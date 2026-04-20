package onebuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.SwingUtilities;

final class PipelineRunner {
    private static final int OUTPUT_BATCH_LINES = 20;
    private static final long OUTPUT_BATCH_NANOS = 50_000_000L;

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
    private volatile Process currentProcess;
    private volatile Thread workerThread;
    private volatile boolean stopRequested;

    PipelineRunner(Path scriptDirectory, Listener listener) {
        this.scriptDirectory = scriptDirectory;
        this.listener = listener;
        this.progressInterpreter = new PipelineProgressInterpreter();
        this.pipelineConfigWriter = new PipelineConfigWriter();
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
        if (process != null) {
            ProcessHandle handle = process.toHandle();
            handle.descendants().forEach(ProcessHandle::destroy);
            handle.destroy();
            if (handle.isAlive()) {
                handle.destroyForcibly();
            }
        }
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
                configPath = Files.createTempFile("onebuilder-runtime-", ".json");
                deleteConfigPath = true;
            }
            pipelineConfigWriter.write(configPath, request);

            ExecutionPlan executionPlan = new ExecutionPlanBuilder(scriptDirectory).build(request, configPath);
            dispatch(() -> listener.onPlanReady(executionPlan));

            if (executionPlan.alignCommand().isPresent()) {
                runCommand("Alignment", executionPlan.alignCommand().orElseThrow(), request.inputType());
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
                } catch (IOException ignored) {
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

        Process process = processBuilder.start();
        currentProcess = process;
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
                    throw new InterruptedException("Run stopped during " + stageName);
                }
            }
        }

        flushPendingOutput(pendingOutput);

        int exitCode = process.waitFor();
        currentProcess = null;
        if (stopRequested) {
            throw new InterruptedException("Run stopped during " + stageName);
        }
        if (exitCode != 0) {
            throw new IOException(stageName + " failed with exit code " + exitCode);
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

    private static void dispatch(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }
}
