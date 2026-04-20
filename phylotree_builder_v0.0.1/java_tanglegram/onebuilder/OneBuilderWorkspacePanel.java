package onebuilder;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import tanglegram.UiPreferences;

final class OneBuilderWorkspacePanel extends JPanel {
    private final NavigationRailPanel navigationRail;
    private final JPanel headerPanel;
    private final JLabel headerStatusChip;
    private final JLabel headerContextLabel;
    private final JPanel headerRightPanel;
    private final JPanel contentPanel;
    private final CardLayout contentLayout;
    private final InputAlignPanel inputAlignPanel;
    private final TreeBuildPanel treeBuildPanel;
    private final CurrentRunTanglegramPanel currentRunTanglegramPanel;
    private final PipelineRunner pipelineRunner;
    private final PipelineConfigWriter pipelineConfigWriter;
    private final PlatformSupport platformSupport;
    private WorkflowTabsState workflowTabsState;
    private InputType selectedInputType;
    private WorkspaceSection selectedSection;

    OneBuilderWorkspacePanel(Path scriptDirectory) {
        this(scriptDirectory, PlatformSupport.current());
    }

    OneBuilderWorkspacePanel(Path scriptDirectory, PlatformSupport platformSupport) {
        super(new BorderLayout());
        this.platformSupport = platformSupport;
        this.workflowTabsState = WorkflowTabsState.initial();
        this.selectedInputType = InputType.PROTEIN;
        this.selectedSection = WorkspaceSection.INPUT_ALIGN;

        WorkbenchStyles.applyCanvas(this);

        treeBuildPanel = new TreeBuildPanel(selectedInputType);
        currentRunTanglegramPanel = new CurrentRunTanglegramPanel();
        pipelineRunner = new PipelineRunner(scriptDirectory, new RunnerListener());
        pipelineConfigWriter = new PipelineConfigWriter();
        inputAlignPanel = new InputAlignPanel(
                platformSupport,
                this::handleInputChanged,
                this::handleRunRequested,
                this::handleExportRequested,
                this::handleStopRequested,
                treeBuildPanel::runtimeConfig);

        headerContextLabel = WorkbenchStyles.createSubtitleLabel(
                "Prepare one alignment, tune the four tree methods, then inspect the tanglegram.");
        headerStatusChip = WorkbenchStyles.createStatusChip("Setup");
        navigationRail = new NavigationRailPanel(this::selectSection);
        headerPanel = buildHeaderPanel();
        headerRightPanel = (JPanel) headerPanel.getComponent(1);
        headerRightPanel.add(headerStatusChip);

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        WorkbenchStyles.applyCanvas(contentPanel);
        contentPanel.add(inputAlignPanel, WorkspaceSection.INPUT_ALIGN.name());
        contentPanel.add(treeBuildPanel, WorkspaceSection.TREE_BUILD.name());
        contentPanel.add(currentRunTanglegramPanel, WorkspaceSection.TANGLEGRAM.name());

        JPanel centerPanel = WorkbenchStyles.createCanvasPanel(new BorderLayout(0, 16));
        centerPanel.setBorder(WorkbenchStyles.PAGE_PADDING);
        centerPanel.add(headerPanel, BorderLayout.NORTH);
        centerPanel.add(contentPanel, BorderLayout.CENTER);

        add(navigationRail, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);

        handleInputChanged();
        syncWorkflowTabs();
        selectSection(WorkspaceSection.INPUT_ALIGN);
    }

    List<String> navigationLabels() {
        return navigationRail.labels();
    }

    String selectedSectionLabel() {
        return selectedSection.label();
    }

    boolean isSectionEnabled(String label) {
        return navigationRail.isSectionEnabled(label);
    }

    boolean hasHeaderPanel() {
        return headerPanel != null;
    }

    TreeBuildPanel treeBuildPanel() {
        return treeBuildPanel;
    }

    InputAlignPanel inputAlignPanel() {
        return inputAlignPanel;
    }

    void applyPreferences(UiPreferences preferences) {
        currentRunTanglegramPanel.applyPreferences(preferences);
        treeBuildPanel.applyPreferences();
        revalidate();
        repaint();
    }

    private JPanel buildHeaderPanel() {
        JPanel panel = WorkbenchStyles.createSurfacePanel(new BorderLayout(16, 0));

        JPanel textBlock = new JPanel(new BorderLayout(0, 4));
        textBlock.setOpaque(false);
        textBlock.add(WorkbenchStyles.createPageTitle("Galaxy-style oneBuilder Workbench"), BorderLayout.NORTH);
        textBlock.add(headerContextLabel, BorderLayout.CENTER);

        JPanel rightBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBlock.setOpaque(false);

        panel.add(textBlock, BorderLayout.CENTER);
        panel.add(rightBlock, BorderLayout.EAST);
        return panel;
    }

    private void selectSection(WorkspaceSection section) {
        if (!isSectionEnabled(section.label())) {
            return;
        }
        selectedSection = section;
        navigationRail.select(section);
        contentLayout.show(contentPanel, section.name());
        updateHeaderForSection(section);
    }

    private void updateHeaderForSection(WorkspaceSection section) {
        switch (section) {
            case INPUT_ALIGN:
                headerContextLabel.setText(platformSupport.supportsPipelineExecution()
                        ? "Set input, output, and alignment behavior before running the Linux-only pipeline."
                        : "Edit pipeline settings and export JSON on Windows. Execution remains Linux-only.");
                break;
            case TREE_BUILD:
                headerContextLabel.setText("Tune the four inference methods in parallel cards and open the log drawer only when needed.");
                break;
            case TANGLEGRAM:
                headerContextLabel.setText("Inspect the current run using the same six fixed tree-pair comparisons as the standalone viewer.");
                break;
            default:
                throw new IllegalStateException("Unexpected section: " + section);
        }
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
        if (!platformSupport.supportsPipelineExecution()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Actual alignment and tree building are Linux-only. On Windows, export a JSON config here and run the pipeline on Linux.",
                    "eGPS oneBuilder",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (pipelineRunner.isRunning()) {
            return;
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

    private void handleExportRequested(RunRequest request) {
        inputAlignPanel.setRunning(true);
        Thread exportThread = new Thread(() -> {
            try {
                Files.createDirectories(request.outputDirectory());
                pipelineConfigWriter.write(request.exportConfigPath(), request);
                SwingUtilities.invokeLater(() -> {
                    inputAlignPanel.setRunning(false);
                    JOptionPane.showMessageDialog(
                            OneBuilderWorkspacePanel.this,
                            "Config exported to: " + request.exportConfigPath(),
                            "eGPS oneBuilder",
                            JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> {
                    inputAlignPanel.setRunning(false);
                    JOptionPane.showMessageDialog(
                            OneBuilderWorkspacePanel.this,
                            "Failed to export config: " + exception.getMessage(),
                            "eGPS oneBuilder",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "onebuilder-config-export");
        exportThread.setDaemon(true);
        exportThread.start();
    }

    private void handleStopRequested() {
        pipelineRunner.stop();
    }

    private void syncWorkflowTabs() {
        navigationRail.setSectionEnabled(WorkspaceSection.INPUT_ALIGN, workflowTabsState.inputEnabled());
        navigationRail.setSectionEnabled(WorkspaceSection.TREE_BUILD, workflowTabsState.treeBuildEnabled());
        navigationRail.setSectionEnabled(
                WorkspaceSection.TANGLEGRAM,
                platformSupport.supportsPipelineExecution() && workflowTabsState.tanglegramEnabled());
    }

    private final class RunnerListener implements PipelineRunner.Listener {
        @Override
        public void onPlanReady(ExecutionPlan executionPlan) {
            treeBuildPanel.resetForRun(executionPlan);
            inputAlignPanel.setAlignedPreview(executionPlan.effectiveInputFile());
            WorkbenchStyles.updateStatusChip(headerStatusChip, "Running");
            selectSection(WorkspaceSection.TREE_BUILD);
        }

        @Override
        public void onStageStarted(String stageName, java.util.List<String> command) {
            treeBuildPanel.setCurrentStage(stageName);
            treeBuildPanel.appendLog("$ " + String.join(" ", command));
            WorkbenchStyles.updateStatusChip(headerStatusChip, stageName);
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
            WorkbenchStyles.updateStatusChip(headerStatusChip, "Completed");
            selectSection(WorkspaceSection.TANGLEGRAM);
        }

        @Override
        public void onRunFailed(String message) {
            inputAlignPanel.setRunning(false);
            treeBuildPanel.finishRun("Failed");
            treeBuildPanel.setCurrentStage("Failed");
            WorkbenchStyles.updateStatusChip(headerStatusChip, "Failed");
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
            WorkbenchStyles.updateStatusChip(headerStatusChip, "Interrupted");
        }
    }
}
