package onebuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

final class PipelineRunner {
    private static final int OUTPUT_BATCH_LINES = 20;
    private static final long OUTPUT_BATCH_NANOS = 50_000_000L;
    private static final long DEFAULT_STAGE_TIMEOUT_SECONDS = TimeUnit.HOURS.toSeconds(24);
    private static final String STAGE_TIMEOUT_PROPERTY = "onebuilder.stageTimeoutSeconds";
    private static final String STAGE_TIMEOUT_ENV = "ONEBUILDER_STAGE_TIMEOUT_SECONDS";

    interface Listener {
        void onPlanReady(ExecutionPlan executionPlan);

        void onStageStarted(String stageName, List<String> command);

        void onProcessOutput(String line);

        void onMethodProgress(MethodProgressEvent event);

        void onRunCompleted(Path outputDirectory, InputType inputType);

        void onRunFailed(String message);

        void onRunStopped();
    }

    private final Path scriptDirectory;
    private final Listener listener;
    private final PipelineProgressInterpreter progressInterpreter;
    private final PipelineConfigWriter pipelineConfigWriter;
    private final long stageTimeoutSeconds;
    private volatile Process currentProcess;
    private volatile Thread workerThread;
    private volatile boolean stopRequested;

    PipelineRunner(Path scriptDirectory, Listener listener) {
        this(scriptDirectory, listener, configuredStageTimeoutSeconds());
    }

    PipelineRunner(Path scriptDirectory, Listener listener, long stageTimeoutSeconds) {
        this.scriptDirectory = scriptDirectory;
        this.listener = listener;
        this.progressInterpreter = new PipelineProgressInterpreter();
        this.pipelineConfigWriter = new PipelineConfigWriter();
        this.stageTimeoutSeconds = stageTimeoutSeconds;
    }

    synchronized boolean isRunning() {
        return workerThread != null && workerThread.isAlive();
    }

    synchronized void start(RunRequest request) {
        if (isRunning()) {
            throw new IllegalStateException("A pipeline run is already in progress.");
        }

        stopRequested = false;
        workerThread = new Thread(() -> runRequest(request), "onebuilder-pipeline-runner");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    synchronized void stop() {
        stopRequested = true;
        Process process = currentProcess;
        destroyProcessTree(process);
        Thread thread = workerThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void runRequest(RunRequest request) {
        Path configPath = null;
        boolean deleteConfigPath = false;
        try {
            Files.createDirectories(request.outputDirectory());
            if (request.exportConfigFile()) {
                configPath = request.exportConfigPath();
                Files.createDirectories(configPath.getParent());
            } else {
                configPath = Files.createTempFile(request.outputDirectory(), "onebuilder-runtime-", ".json");
                deleteConfigPath = true;
            }
            pipelineConfigWriter.write(configPath, request);

            ExecutionPlan executionPlan = new ExecutionPlanBuilder(scriptDirectory).build(request, configPath);
            dispatch(() -> listener.onPlanReady(executionPlan));

            if (executionPlan.alignCommand().isPresent()) {
                runCommand("Alignment", executionPlan.alignCommand().orElseThrow(), request.inputType());
            }
            if (executionPlan.trimCommand().isPresent()) {
                runCommand("Trim alignment", executionPlan.trimCommand().orElseThrow(), request.inputType());
            }
            runCommand("Tree Build", executionPlan.buildCommand(), request.inputType());

            if (stopRequested) {
                dispatch(listener::onRunStopped);
            } else {
                dispatch(() -> listener.onRunCompleted(executionPlan.pipelineOutputDir(), request.inputType()));
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            dispatch(listener::onRunStopped);
        } catch (Exception exception) {
            dispatch(() -> listener.onRunFailed(exception.getMessage() == null ? exception.toString() : exception.getMessage()));
        } finally {
            currentProcess = null;
            synchronized (this) {
                workerThread = null;
            }
            if (deleteConfigPath && configPath != null) {
                try {
                    Files.deleteIfExists(configPath);
                } catch (IOException exception) {
                    String message = "Warning: could not remove temporary oneBuilder config file: "
                            + configPath + " (" + exception.getMessage() + ")" + System.lineSeparator();
                    dispatch(() -> listener.onProcessOutput(message));
                }
            }
        }
    }

    private void runCommand(String stageName, List<String> command, InputType inputType) throws Exception {
        if (stopRequested) {
            throw new InterruptedException("Run stopped before " + stageName);
        }

        dispatch(() -> listener.onStageStarted(stageName, command));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(scriptDirectory.toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("PYTHONUNBUFFERED", "1");

        Process process = processBuilder.start();
        currentProcess = process;
        if (stopRequested) {
            destroyProcessTree(process);
            throw new InterruptedException("Run stopped during " + stageName);
        }

        AtomicReference<IOException> outputFailure = new AtomicReference<>();
        Thread outputReader = new Thread(
                () -> readProcessOutput(process, inputType, outputFailure),
                "onebuilder-process-output-reader");
        outputReader.setDaemon(true);
        outputReader.start();

        boolean completed;
        try {
            if (stageTimeoutSeconds > 0L) {
                completed = process.waitFor(stageTimeoutSeconds, TimeUnit.SECONDS);
            } else {
                process.waitFor();
                completed = true;
            }
        } catch (InterruptedException interruptedException) {
            destroyProcessTree(process);
            throw interruptedException;
        } finally {
            currentProcess = null;
        }

        if (!completed) {
            destroyProcessTree(process);
            waitForOutputReader(outputReader);
            throw new IOException(stageName + " timed out after " + stageTimeoutSeconds + " seconds");
        }

        waitForOutputReader(outputReader);
        if (stopRequested) {
            throw new InterruptedException("Run stopped during " + stageName);
        }
        IOException outputException = outputFailure.get();
        if (outputException != null) {
            throw outputException;
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException(stageName + " failed with exit code " + exitCode);
        }
    }

    private void readProcessOutput(Process process, InputType inputType, AtomicReference<IOException> outputFailure) {
        StringBuilder pendingOutput = new StringBuilder();
        int pendingLineCount = 0;
        long lastOutputDispatch = System.nanoTime();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                pendingOutput.append(line).append(System.lineSeparator());
                pendingLineCount++;
                long now = System.nanoTime();
                if (pendingLineCount >= OUTPUT_BATCH_LINES || now - lastOutputDispatch >= OUTPUT_BATCH_NANOS) {
                    flushPendingOutput(pendingOutput);
                    pendingLineCount = 0;
                    lastOutputDispatch = now;
                }
                MethodProgressEvent event = progressInterpreter.interpret(inputType, line);
                if (event != null) {
                    dispatch(() -> listener.onMethodProgress(event));
                }
                if (stopRequested) {
                    destroyProcessTree(process);
                    break;
                }
            }
        } catch (IOException exception) {
            outputFailure.compareAndSet(null, exception);
        } finally {
            flushPendingOutput(pendingOutput);
        }
    }

    private static void waitForOutputReader(Thread outputReader) throws InterruptedException {
        outputReader.join(5000L);
        if (outputReader.isAlive()) {
            outputReader.interrupt();
        }
    }

    private void flushPendingOutput(StringBuilder pendingOutput) {
        if (pendingOutput.length() == 0) {
            return;
        }
        String capturedOutput = pendingOutput.toString();
        pendingOutput.setLength(0);
        dispatch(() -> listener.onProcessOutput(capturedOutput));
    }

    static void destroyProcessTreeForTest(Process process) {
        destroyProcessTree(process);
    }

    private static void destroyProcessTree(Process process) {
        if (process == null) {
            return;
        }
        ProcessHandle handle = process.toHandle();
        List<ProcessHandle> descendants = handle.descendants().toList();
        descendants.forEach(ProcessHandle::destroy);
        handle.destroy();
        descendants.stream()
                .filter(ProcessHandle::isAlive)
                .forEach(ProcessHandle::destroyForcibly);
        if (handle.isAlive()) {
            handle.destroyForcibly();
        }
    }

    private static void dispatch(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private static long configuredStageTimeoutSeconds() {
        String configured = System.getProperty(STAGE_TIMEOUT_PROPERTY);
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv(STAGE_TIMEOUT_ENV);
        }
        if (configured == null || configured.trim().isEmpty()) {
            return DEFAULT_STAGE_TIMEOUT_SECONDS;
        }
        try {
            long parsed = Long.parseLong(configured.trim());
            return Math.max(0L, parsed);
        } catch (NumberFormatException exception) {
            return DEFAULT_STAGE_TIMEOUT_SECONDS;
        }
    }
}
