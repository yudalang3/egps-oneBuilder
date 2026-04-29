package onebuilder;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import tanglegram.UiPreferences;

final class OneBuilderWorkspacePanel extends JPanel {
    private final NavigationRailPanel navigationRail;
    private final JPanel headerPanel;
    private final JLabel headerSnapshotChip;
    private final JLabel headerStatusChip;
    private final JLabel headerContextLabel;
    private final JPanel headerRightPanel;
    private final JPanel contentPanel;
    private final CardLayout contentLayout;
    private final InputAlignPanel inputAlignPanel;
    private final TreeParametersPanel treeParametersPanel;
    private final RerootTreePanel rerootTreePanel;
    private final TreeBuildPanel treeBuildPanel;
    private final CurrentRunTanglegramPanel currentRunTanglegramPanel;
    private final VisLaunchingPanel visLaunchingPanel;
    private final HowToCitePanel howToCitePanel;
    private final PipelineRunner pipelineRunner;
    private final PipelineConfigWriter pipelineConfigWriter;
    private final PlatformSupport platformSupport;
    private WorkflowTabsState workflowTabsState;
    private InputType selectedInputType;
    private WorkspaceSection selectedSection;
    private Path latestCompletedOutputDirectory;

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

        treeParametersPanel = new TreeParametersPanel(selectedInputType);
        rerootTreePanel = new RerootTreePanel();
        rerootTreePanel.apply(RerootConfig.defaults());
        treeBuildPanel = new TreeBuildPanel(
                platformSupport,
                this::handleRunButtonPressed,
                this::handleExportButtonPressed,
                this::handleStopRequested);
        currentRunTanglegramPanel = new CurrentRunTanglegramPanel();
        visLaunchingPanel = new VisLaunchingPanel(scriptDirectory, () -> latestCompletedOutputDirectory);
        howToCitePanel = new HowToCitePanel(scriptDirectory);
        pipelineRunner = new PipelineRunner(scriptDirectory, new RunnerListener());
        pipelineConfigWriter = new PipelineConfigWriter();
        inputAlignPanel = new InputAlignPanel(
                platformSupport,
                this::handleInputChanged,
                this::currentRuntimeConfig);

        headerContextLabel = WorkbenchStyles.createSubtitleLabel(
                "Prepare one alignment, review the method tree, run the pipeline, then inspect the tanglegram.");
        headerSnapshotChip = WorkbenchStyles.createStatusChip("Quick Snapshot of the Tanglegram");
        headerSnapshotChip.setVisible(false);
        headerStatusChip = WorkbenchStyles.createStatusChip("Setup");
        navigationRail = new NavigationRailPanel(this::handleSectionSelectionRequest);
        headerPanel = buildHeaderPanel();
        headerRightPanel = (JPanel) headerPanel.getComponent(1);
        headerRightPanel.add(headerSnapshotChip);
        headerRightPanel.add(headerStatusChip);

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        WorkbenchStyles.applyCanvas(contentPanel);
        contentPanel.add(inputAlignPanel, WorkspaceSection.INPUT_ALIGN.name());
        contentPanel.add(treeParametersPanel, WorkspaceSection.TREE_PARAMETERS.name());
        contentPanel.add(rerootTreePanel, WorkspaceSection.REROOT_TREE.name());
        contentPanel.add(treeBuildPanel, WorkspaceSection.TREE_BUILD.name());
        contentPanel.add(currentRunTanglegramPanel, WorkspaceSection.TANGLEGRAM.name());
        contentPanel.add(visLaunchingPanel, WorkspaceSection.VIS_LAUNCHING.name());
        contentPanel.add(howToCitePanel, WorkspaceSection.HOW_TO_CITE.name());

        JPanel centerPanel = WorkbenchStyles.createCanvasPanel(new BorderLayout(0, 16));
        centerPanel.setBorder(WorkbenchStyles.PAGE_PADDING);
        centerPanel.add(headerPanel, BorderLayout.NORTH);
        centerPanel.add(contentPanel, BorderLayout.CENTER);

        add(navigationRail, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);

        handleInputChanged();
        syncWorkflowTabs();
        refreshTreeBuildDraft();
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

    void clickNavigationSection(String label) {
        navigationRail.clickSection(label);
    }

    String navigationTooltipText(String label) {
        return navigationRail.toolTipText(label);
    }

    boolean hasHeaderPanel() {
        return headerPanel != null;
    }

    boolean headerSnapshotChipVisibleForTest() {
        return headerSnapshotChip.isVisible();
    }

    List<String> headerRightLabelsForTest() {
        List<String> labels = new ArrayList<>();
        for (java.awt.Component component : headerRightPanel.getComponents()) {
            if (component instanceof JLabel && component.isVisible()) {
                labels.add(((JLabel) component).getText());
            }
        }
        return labels;
    }

    TreeParametersPanel treeParametersPanel() {
        return treeParametersPanel;
    }

    TreeBuildPanel treeBuildPanel() {
        return treeBuildPanel;
    }

    RerootTreePanel rerootTreePanel() {
        return rerootTreePanel;
    }

    HowToCitePanel howToCitePanel() {
        return howToCitePanel;
    }

    InputAlignPanel inputAlignPanel() {
        return inputAlignPanel;
    }

    void applyPreferences(UiPreferences preferences) {
        currentRunTanglegramPanel.applyPreferences(preferences);
        howToCitePanel.applyPreferences();
        treeParametersPanel.applyPreferences();
        treeBuildPanel.applyPreferences();
        revalidate();
        repaint();
    }

    private JPanel buildHeaderPanel() {
        JPanel panel = WorkbenchStyles.createSurfacePanel(new BorderLayout(16, 0));

        JPanel textBlock = new JPanel(new BorderLayout(0, 4));
        textBlock.setOpaque(false);
        textBlock.add(WorkbenchStyles.createPageTitle("User-friendly oneBuilder Workbench"), BorderLayout.NORTH);
        textBlock.add(headerContextLabel, BorderLayout.CENTER);

        JPanel rightBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBlock.setOpaque(false);

        panel.add(textBlock, BorderLayout.CENTER);
        panel.add(rightBlock, BorderLayout.EAST);
        return panel;
    }

    private String handleSectionSelectionRequest(WorkspaceSection section) {
        if (section == selectedSection) {
            selectSection(section);
            return null;
        }
        String blockingMessage = blockingMessageFor(section);
        if (blockingMessage != null) {
            return blockingMessage;
        }
        if (section == WorkspaceSection.TREE_BUILD) {
            refreshTreeBuildDraft();
        }
        selectSection(section);
        return null;
    }

    private void selectSection(WorkspaceSection section) {
        if (!isSectionEnabled(section.label())) {
            return;
        }
        if (section == WorkspaceSection.TANGLEGRAM && latestCompletedOutputDirectory != null && !pipelineRunner.isRunning()) {
            currentRunTanglegramPanel.loadRunResults(latestCompletedOutputDirectory);
        }
        if (section == WorkspaceSection.TREE_BUILD) {
            refreshTreeBuildDraft();
        }
        selectedSection = section;
        navigationRail.select(section);
        contentLayout.show(contentPanel, section.name());
        updateHeaderForSection(section);
    }

    private void updateHeaderForSection(WorkspaceSection section) {
        headerSnapshotChip.setVisible(section == WorkspaceSection.TANGLEGRAM);
        headerRightPanel.revalidate();
        headerRightPanel.repaint();
        switch (section) {
            case INPUT_ALIGN:
                headerContextLabel.setText(platformSupport.supportsPipelineExecution()
                        ? "Set input, output, and alignment behavior before moving deeper into the Linux-only pipeline."
                        : "Edit input and alignment settings here, then export the config from Tree Build on Windows.");
                break;
            case TREE_PARAMETERS:
                headerContextLabel.setText("Use the method tree to configure Distance, Maximum Likelihood, Bayes, Parsimony, and protein-only Foldseek structure similarity.");
                break;
            case REROOT_TREE:
                headerContextLabel.setText("Choose the rerooting method applied after tree inference and before visualization.");
                break;
            case TREE_BUILD:
                headerContextLabel.setText("Review the current configuration, export it for command-line reuse, or launch the pipeline from here.");
                break;
            case TANGLEGRAM:
                headerContextLabel.setText("Inspect the current run using the same fixed tree-pair comparisons as the standalone viewer.");
                break;
            case VIS_LAUNCHING:
                headerContextLabel.setText("Open the standalone interactive Tanglegram viewer for the latest completed run.");
                break;
            case HOW_TO_CITE:
                headerContextLabel.setText("Review citation guidance for eGPS-onebuilder and the software used by this workflow.");
                break;
            default:
                throw new IllegalStateException("Unexpected section: " + section);
        }
    }

    private void handleInputChanged() {
        InputType newInputType = inputAlignPanel.selectedInputType();
        if (newInputType != selectedInputType) {
            selectedInputType = newInputType;
            treeParametersPanel.applyRuntimeConfig(PipelineRuntimeConfig.defaultsFor(selectedInputType));
        }
        if (inputAlignPanel.navigationBlockingMessage() == null) {
            workflowTabsState = workflowTabsState.markInputConfigured();
        }
        refreshTreeBuildDraft();
        syncWorkflowTabs();
    }

    private void handleRunButtonPressed() {
        refreshTreeBuildDraft();
        try {
            RunRequest request = inputAlignPanel.buildRunRequestForExecution();
            if (!confirmSingleMethodRunIfNeeded(request)) {
                return;
            }
            handleRunRequested(request);
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "eGPS oneBuilder", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleExportButtonPressed() {
        refreshTreeBuildDraft();
        try {
            RunRequest request = inputAlignPanel.buildRunRequestForExport();
            if (!confirmSingleMethodRunIfNeeded(request)) {
                return;
            }
            handleExportRequested(request);
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "eGPS oneBuilder", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRunRequested(RunRequest request) {
        if (!platformSupport.supportsPipelineExecution()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Actual alignment and tree building are Linux-only. Use Export Config here and run the pipeline on Linux.",
                    "eGPS oneBuilder",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (pipelineRunner.isRunning()) {
            return;
        }

        RunRequest preparedRequest = confirmOverwriteIfNeeded(request);
        if (preparedRequest == null) {
            return;
        }

        latestCompletedOutputDirectory = null;
        currentRunTanglegramPanel.loadRunResults(null);
        workflowTabsState = workflowTabsState.markInputConfigured().markRunStarted();
        syncWorkflowTabs();
        inputAlignPanel.setRunning(true);
        rerootTreePanel.setRunning(true);
        treeBuildPanel.setDraftSummary(inputAlignPanel.buildRunDraftSummary(preparedRequest.runtimeConfig()));
        treeBuildPanel.configureOverallProgress(preparedRequest.runtimeConfig());

        try {
            pipelineRunner.start(preparedRequest);
        } catch (IllegalStateException exception) {
            inputAlignPanel.setRunning(false);
            rerootTreePanel.setRunning(false);
            treeBuildPanel.setRunning(false);
            JOptionPane.showMessageDialog(this, exception.getMessage(), "eGPS oneBuilder", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleExportRequested(RunRequest request) {
        if (Files.exists(request.exportConfigPath())) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Config file already exists:\n" + request.exportConfigPath()
                            + "\n\nOverwrite it, or cancel this export?",
                    "Overwrite Existing Config?",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }
        }
        inputAlignPanel.setRunning(true);
        rerootTreePanel.setRunning(true);
        treeBuildPanel.setExporting(true);
        Thread exportThread = new Thread(() -> {
            try {
                Files.createDirectories(request.outputDirectory());
                pipelineConfigWriter.write(request.exportConfigPath(), request);
                SwingUtilities.invokeLater(() -> {
                    inputAlignPanel.setRunning(false);
                    rerootTreePanel.setRunning(false);
                    treeBuildPanel.setExporting(false);
                    JOptionPane.showMessageDialog(
                            OneBuilderWorkspacePanel.this,
                            "Config exported to: " + request.exportConfigPath(),
                            "eGPS oneBuilder",
                            JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> {
                    inputAlignPanel.setRunning(false);
                    rerootTreePanel.setRunning(false);
                    treeBuildPanel.setExporting(false);
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

    private void refreshTreeBuildDraft() {
        treeBuildPanel.setDraftSummary(inputAlignPanel.buildRunDraftSummary(currentRuntimeConfig()));
    }

    private PipelineRuntimeConfig currentRuntimeConfig() {
        return treeParametersPanel.runtimeConfig().withReroot(rerootTreePanel.toConfig());
    }

    private RunRequest confirmOverwriteIfNeeded(RunRequest request) {
        Path pipelineOutputDir = request.pipelineOutputDir();
        if (!Files.exists(pipelineOutputDir)) {
            return request;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Output directory already exists:\n" + pipelineOutputDir
                        + "\n\nOverwrite its current contents, or cancel this run?",
                "Overwrite Existing Output?",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }
        return request.withOverwriteExistingOutput(true);
    }

    private boolean confirmSingleMethodRunIfNeeded(RunRequest request) {
        if (request == null || InputAlignPanel.countEnabledTreeMethods(request.runtimeConfig(), request.inputType()) != 1) {
            return true;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Only one tree-building method is enabled. The run can produce a single tree, but Tanglegram and TreeDist comparisons require at least two readable trees.\n\nContinue anyway?",
                "Only One Method Enabled",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.OK_OPTION;
    }

    private void syncWorkflowTabs() {
        navigationRail.setSectionEnabled(WorkspaceSection.INPUT_ALIGN, workflowTabsState.inputEnabled());
        boolean inputReady = inputAlignPanel.navigationBlockingMessage() == null;
        navigationRail.setSectionEnabled(
                WorkspaceSection.TREE_PARAMETERS,
                workflowTabsState.treeParametersEnabled() && inputReady,
                inputAlignPanel.navigationBlockingMessage());
        navigationRail.setSectionEnabled(
                WorkspaceSection.REROOT_TREE,
                workflowTabsState.rerootTreeEnabled() && inputReady,
                inputAlignPanel.navigationBlockingMessage());
        navigationRail.setSectionEnabled(
                WorkspaceSection.TREE_BUILD,
                workflowTabsState.treeBuildEnabled() && inputReady,
                inputAlignPanel.navigationBlockingMessage());
        boolean tanglegramEnabled = platformSupport.supportsPipelineExecution() && workflowTabsState.tanglegramEnabled();
        navigationRail.setSectionEnabled(
                WorkspaceSection.TANGLEGRAM,
                tanglegramEnabled,
                lockedSectionMessage(WorkspaceSection.TANGLEGRAM));
        navigationRail.setSectionEnabled(
                WorkspaceSection.VIS_LAUNCHING,
                tanglegramEnabled && workflowTabsState.visLaunchingEnabled(),
                lockedSectionMessage(WorkspaceSection.VIS_LAUNCHING));
        navigationRail.setSectionEnabled(WorkspaceSection.HOW_TO_CITE, true);
        visLaunchingPanel.setReady(tanglegramEnabled && workflowTabsState.visLaunchingEnabled());
    }

    private String blockingMessageFor(WorkspaceSection targetSection) {
        String currentSectionMessage = currentSectionBlockingMessage();
        if (currentSectionMessage != null) {
            return currentSectionMessage;
        }
        if (!isSectionEnabled(targetSection.label())) {
            return lockedSectionMessage(targetSection);
        }
        return null;
    }

    private String currentSectionBlockingMessage() {
        if (selectedSection == WorkspaceSection.INPUT_ALIGN) {
            return inputAlignPanel.navigationBlockingMessage();
        }
        return null;
    }

    private String lockedSectionMessage(WorkspaceSection section) {
        switch (section) {
            case TREE_PARAMETERS:
            case REROOT_TREE:
            case TREE_BUILD:
                return inputAlignPanel.navigationBlockingMessage();
            case TANGLEGRAM:
            case VIS_LAUNCHING:
                if (!platformSupport.supportsPipelineExecution()) {
                    return "Tanglegram in oneBuilder opens only after a Linux run generates results. Use the standalone viewer to inspect an existing tree_summary output on this platform.";
                }
                return "Finish Tree Build first. Run the pipeline and generate the current run results before opening Tanglegram.";
            case INPUT_ALIGN:
            case HOW_TO_CITE:
                return null;
            default:
                throw new IllegalStateException("Unexpected section: " + section);
        }
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
            treeBuildPanel.notePipelineOutput(line);
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
                case SKIPPED:
                    treeBuildPanel.setMethodStatus(event.methodKey(), "Skipped");
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
            latestCompletedOutputDirectory = outputDirectory;
            workflowTabsState = workflowTabsState.markTanglegramReady().markRunFinished();
            syncWorkflowTabs();
            inputAlignPanel.setRunning(false);
            rerootTreePanel.setRunning(false);
            treeBuildPanel.finishRun("Completed");
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
            rerootTreePanel.setRunning(false);
            treeBuildPanel.finishRun("Failed");
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
            rerootTreePanel.setRunning(false);
            treeBuildPanel.finishRun("Interrupted");
            WorkbenchStyles.updateStatusChip(headerStatusChip, "Interrupted");
        }
    }
}
