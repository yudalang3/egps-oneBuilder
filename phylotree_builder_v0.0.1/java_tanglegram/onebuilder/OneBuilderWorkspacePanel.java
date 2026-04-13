package onebuilder;

import com.jidesoft.swing.JideTabbedPane;
import java.awt.BorderLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

final class OneBuilderWorkspacePanel extends JPanel {
    private final JideTabbedPane workflowTabs;
    private final InputAlignPanel inputAlignPanel;
    private final TreeBuildPanel treeBuildPanel;
    private final CurrentRunTanglegramPanel currentRunTanglegramPanel;
    private final PipelineRunner pipelineRunner;
    private WorkflowTabsState workflowTabsState;
    private InputType selectedInputType;

    OneBuilderWorkspacePanel(Path scriptDirectory) {
        super(new BorderLayout());
        this.workflowTabsState = WorkflowTabsState.initial();
        this.selectedInputType = InputType.PROTEIN;

        treeBuildPanel = new TreeBuildPanel(selectedInputType);
        currentRunTanglegramPanel = new CurrentRunTanglegramPanel();
        pipelineRunner = new PipelineRunner(scriptDirectory, new RunnerListener());
        inputAlignPanel = new InputAlignPanel(
                this::handleInputChanged,
                this::handleRunRequested,
                this::handleStopRequested,
                treeBuildPanel::runtimeConfig);

        workflowTabs = new JideTabbedPane(JideTabbedPane.TOP);
        workflowTabs.setTabLayoutPolicy(JideTabbedPane.SCROLL_TAB_LAYOUT);
        workflowTabs.setShowCloseButton(false);
        workflowTabs.setShowCloseButtonOnTab(false);
        workflowTabs.addTab("Input / Align", inputAlignPanel);
        workflowTabs.addTab("Tree Build", treeBuildPanel);
        workflowTabs.addTab("Tanglegram", currentRunTanglegramPanel);
        add(workflowTabs, BorderLayout.CENTER);

        handleInputChanged();
        syncWorkflowTabs();
    }

    JideTabbedPane workflowTabs() {
        return workflowTabs;
    }

    TreeBuildPanel treeBuildPanel() {
        return treeBuildPanel;
    }

    private void handleInputChanged() {
        InputType newInputType = inputAlignPanel.selectedInputType();
        if (newInputType != selectedInputType) {
            selectedInputType = newInputType;
            treeBuildPanel.applyRuntimeConfig(PipelineRuntimeConfig.defaultsFor(selectedInputType));
        }
        if (inputAlignPanel.hasCompleteInput()) {
            workflowTabsState = workflowTabsState.markInputConfigured();
        }
        syncWorkflowTabs();
    }

    private void handleRunRequested(RunRequest request) {
        if (pipelineRunner.isRunning()) {
            return;
        }

        if (!Files.isDirectory(request.outputDirectory())) {
            try {
                Files.createDirectories(request.outputDirectory());
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to create output directory: " + exception.getMessage(),
                        "eGPS oneBuilder",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        workflowTabsState = workflowTabsState.markInputConfigured().markRunStarted();
        syncWorkflowTabs();
        inputAlignPanel.setRunning(true);
        treeBuildPanel.applyRuntimeConfig(request.runtimeConfig());

        try {
            pipelineRunner.start(request);
        } catch (IllegalStateException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "eGPS oneBuilder", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleStopRequested() {
        pipelineRunner.stop();
    }

    private void syncWorkflowTabs() {
        workflowTabs.setEnabledAt(0, workflowTabsState.inputEnabled());
        workflowTabs.setEnabledAt(1, workflowTabsState.treeBuildEnabled());
        workflowTabs.setEnabledAt(2, workflowTabsState.tanglegramEnabled());
    }

    private final class RunnerListener implements PipelineRunner.Listener {
        @Override
        public void onPlanReady(ExecutionPlan executionPlan) {
            treeBuildPanel.resetForRun(executionPlan);
            inputAlignPanel.setAlignedPreview(executionPlan.effectiveInputFile());
            workflowTabs.setSelectedIndex(1);
        }

        @Override
        public void onStageStarted(String stageName, java.util.List<String> command) {
            treeBuildPanel.setCurrentStage(stageName);
            treeBuildPanel.appendLog("$ " + String.join(" ", command));
        }

        @Override
        public void onProcessOutput(String line) {
            treeBuildPanel.appendLog(line);
        }

        @Override
        public void onMethodProgress(MethodProgressEvent event) {
            switch (event.lifecycle()) {
                case RUNNING:
                    treeBuildPanel.setMethodStatus(event.methodKey(), "Running");
                    break;
                case COMPLETED:
                    treeBuildPanel.setMethodStatus(event.methodKey(), "Completed");
                    break;
                case FAILED:
                    treeBuildPanel.setMethodStatus(event.methodKey(), "Failed");
                    break;
                default:
                    throw new IllegalStateException("Unexpected lifecycle: " + event.lifecycle());
            }
        }

        @Override
        public void onRunCompleted(Path outputDirectory, InputType inputType) {
            workflowTabsState = workflowTabsState.markTanglegramReady().markRunFinished();
            syncWorkflowTabs();
            inputAlignPanel.setRunning(false);
            treeBuildPanel.finishRun("Completed");
            treeBuildPanel.setCurrentStage("Complete");
            for (TreeMethodKey methodKey : TreeMethodKey.values()) {
                Path outputPath = methodKey.expectedOutputPath(outputDirectory, inputType);
                treeBuildPanel.setMethodOutput(methodKey, Files.exists(outputPath) ? outputPath : null);
            }
            currentRunTanglegramPanel.loadRunResults(outputDirectory);
            workflowTabs.setSelectedIndex(2);
        }

        @Override
        public void onRunFailed(String message) {
            inputAlignPanel.setRunning(false);
            treeBuildPanel.finishRun("Failed");
            treeBuildPanel.setCurrentStage("Failed");
            JOptionPane.showMessageDialog(
                    OneBuilderWorkspacePanel.this,
                    message,
                    "eGPS oneBuilder",
                    JOptionPane.ERROR_MESSAGE);
        }

        @Override
        public void onRunStopped() {
            inputAlignPanel.setRunning(false);
            treeBuildPanel.finishRun("Interrupted");
            treeBuildPanel.setCurrentStage("Stopped");
        }
    }
}
