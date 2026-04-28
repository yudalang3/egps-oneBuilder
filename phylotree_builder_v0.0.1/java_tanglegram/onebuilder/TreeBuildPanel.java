package onebuilder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;

final class TreeBuildPanel extends JPanel {
    private final PlatformSupport platformSupport;
    private final JTextArea detailsArea;
    private final JButton runButton;
    private final JButton stopButton;
    private final JButton exportConfigButton;
    private final StringBuilder logBuffer;
    private final Map<TreeMethodKey, String> methodStatuses;
    private final Map<TreeMethodKey, Path> methodOutputs;
    private final Map<TreeMethodKey, JLabel> methodStatusLabels;
    private final Map<TreeMethodKey, StatusDot> methodStatusDots;
    private final JProgressBar overallProgressBar;
    private final EnumSet<TreeMethodKey> enabledProgressMethods;
    private final EnumSet<TreeMethodKey> completedProgressMethods;
    private final EnumSet<PostBuildStep> completedPostBuildSteps;
    private String draftSummary;
    private String overallStatus;
    private String currentStage;
    private Path alignedOutputPath;
    private Path outputDirectoryPath;
    private int totalProgressSteps;
    private final Timer waitIndicatorTimer;
    private String waitIndicatorMessage;
    private int waitIndicatorDotCount;

    TreeBuildPanel(
            PlatformSupport platformSupport,
            Runnable runRequestedCallback,
            Runnable exportRequestedCallback,
            Runnable stopRequestedCallback) {
        super(new BorderLayout(0, 16));
        this.platformSupport = platformSupport;
        this.logBuffer = new StringBuilder();
        this.methodStatuses = new EnumMap<>(TreeMethodKey.class);
        this.methodOutputs = new EnumMap<>(TreeMethodKey.class);
        this.methodStatusLabels = new EnumMap<>(TreeMethodKey.class);
        this.methodStatusDots = new EnumMap<>(TreeMethodKey.class);
        this.enabledProgressMethods = EnumSet.noneOf(TreeMethodKey.class);
        this.completedProgressMethods = EnumSet.noneOf(TreeMethodKey.class);
        this.completedPostBuildSteps = EnumSet.noneOf(PostBuildStep.class);
        this.draftSummary = "Input type: -" + System.lineSeparator() + "Input FASTA/MSA: -";
        this.overallStatus = "Idle";
        this.currentStage = "-";
        this.totalProgressSteps = 0;
        this.waitIndicatorMessage = null;
        this.waitIndicatorDotCount = 0;
        this.waitIndicatorTimer = new Timer(550, event -> advanceWaitIndicator());
        WorkbenchStyles.applyCanvas(this);

        add(buildHeader(), BorderLayout.NORTH);

        detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(false);
        JScrollPane scrollPane = new JScrollPane(detailsArea);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(WorkbenchStyles.SURFACE_BACKGROUND);

        JPanel detailsCard = WorkbenchStyles.createSurfacePanel(new BorderLayout());
        detailsCard.add(scrollPane, BorderLayout.CENTER);
        overallProgressBar = new JProgressBar();
        overallProgressBar.setStringPainted(true);
        overallProgressBar.setMinimum(0);
        overallProgressBar.setMaximum(1);
        overallProgressBar.setValue(0);
        overallProgressBar.setString("Idle");
        overallProgressBar.setForeground(WorkbenchStyles.ACCENT);
        overallProgressBar.setBackground(WorkbenchStyles.SURFACE_BACKGROUND);
        overallProgressBar.setBorder(BorderFactory.createLineBorder(WorkbenchStyles.ACCENT_BORDER, 1, true));

        JPanel bodyPanel = WorkbenchStyles.createCanvasPanel(new BorderLayout(0, 12));
        bodyPanel.add(detailsCard, BorderLayout.CENTER);
        bodyPanel.add(buildStatusSection(), BorderLayout.SOUTH);
        add(bodyPanel, BorderLayout.CENTER);

        runButton = new JButton("Run");
        exportConfigButton = new JButton("Export Config");
        stopButton = new JButton("Stop");
        runButton.addActionListener(event -> runRequestedCallback.run());
        exportConfigButton.addActionListener(event -> exportRequestedCallback.run());
        stopButton.addActionListener(event -> stopRequestedCallback.run());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(runButton);
        buttonRow.add(exportConfigButton);
        buttonRow.add(stopButton);

        JPanel actionCard = WorkbenchStyles.createSurfacePanel(new BorderLayout());
        actionCard.add(buttonRow, BorderLayout.WEST);
        add(actionCard, BorderLayout.SOUTH);

        resetMethodStatusesToIdle();
        updateOverallProgressDisplay("Idle");
        setRunning(false);
        refreshDetailsText();
    }

    boolean hasRunButtonForTest() {
        return runButton != null;
    }

    boolean hasExportConfigButtonForTest() {
        return exportConfigButton != null;
    }

    boolean isExportConfigButtonEnabledForTest() {
        return exportConfigButton.isEnabled();
    }

    int caretPositionForTest() {
        return detailsArea.getCaretPosition();
    }

    int documentLengthForTest() {
        return detailsArea.getDocument().getLength();
    }

    int overallProgressValueForTest() {
        return overallProgressBar.getValue();
    }

    int overallProgressMaximumForTest() {
        return overallProgressBar.getMaximum();
    }

    String overallProgressTextForTest() {
        return overallProgressBar.getString();
    }

    boolean overallProgressIndeterminateForTest() {
        return overallProgressBar.isIndeterminate();
    }

    boolean waitIndicatorRunningForTest() {
        return waitIndicatorTimer.isRunning();
    }

    void applyPreferences() {
        revalidate();
        repaint();
    }

    void configureOverallProgress(PipelineRuntimeConfig runtimeConfig) {
        enabledProgressMethods.clear();
        completedProgressMethods.clear();
        completedPostBuildSteps.clear();
        if (runtimeConfig != null) {
            if (runtimeConfig.distance().enabled()) {
                enabledProgressMethods.add(TreeMethodKey.DISTANCE);
            }
            if (runtimeConfig.maximumLikelihood().enabled()) {
                enabledProgressMethods.add(TreeMethodKey.MAXIMUM_LIKELIHOOD);
            }
            if (runtimeConfig.bayesian().enabled()) {
                enabledProgressMethods.add(TreeMethodKey.BAYESIAN);
            }
            if (runtimeConfig.parsimony().enabled()) {
                enabledProgressMethods.add(TreeMethodKey.PARSIMONY);
            }
            if (runtimeConfig.inputType() == InputType.PROTEIN && runtimeConfig.proteinStructure().enabled()) {
                enabledProgressMethods.add(TreeMethodKey.PROTEIN_STRUCTURE);
            }
        }
        totalProgressSteps = enabledProgressMethods.size() + PostBuildStep.values().length;
        updateOverallProgressDisplay("Ready to run");
    }

    void setDraftSummary(String draftSummary) {
        this.draftSummary = draftSummary == null || draftSummary.isBlank() ? "Input type: -" : draftSummary;
        refreshDetailsText();
    }

    void resetForRun(ExecutionPlan executionPlan) {
        logBuffer.setLength(0);
        overallStatus = "Running";
        currentStage = "Preparing";
        alignedOutputPath = executionPlan == null ? null : executionPlan.effectiveInputFile();
        outputDirectoryPath = executionPlan == null ? null : executionPlan.pipelineOutputDir();
        completedProgressMethods.clear();
        completedPostBuildSteps.clear();
        stopWaitIndicator();
        if (totalProgressSteps == 0) {
            totalProgressSteps = TreeMethodKey.values().length + PostBuildStep.values().length;
        }
        showPreparingProgress();
        for (TreeMethodKey methodKey : TreeMethodKey.values()) {
            String initialStatus = enabledProgressMethods.contains(methodKey) ? "Queued" : "Skipped";
            methodStatuses.put(methodKey, initialStatus);
            methodOutputs.put(methodKey, null);
            updateMethodIndicator(methodKey, initialStatus);
        }
        setRunning(true);
        refreshDetailsText();
    }

    void appendLog(String line) {
        if (line == null) {
            return;
        }
        logBuffer.append(line);
        if (!line.endsWith("\n") && !line.endsWith("\r")) {
            logBuffer.append(System.lineSeparator());
        }
        refreshDetailsText();
    }

    void setCurrentStage(String stageText) {
        currentStage = stageText == null || stageText.isBlank() ? "-" : stageText;
        refreshDetailsText();
    }

    void setMethodStatus(TreeMethodKey methodKey, String statusText) {
        String normalizedStatus = statusText == null || statusText.isBlank() ? "-" : statusText;
        methodStatuses.put(methodKey, normalizedStatus);
        updateMethodIndicator(methodKey, normalizedStatus);
        updateOverallProgressForMethod(methodKey, normalizedStatus);
        refreshDetailsText();
    }

    void setMethodOutput(TreeMethodKey methodKey, Path outputPath) {
        methodOutputs.put(methodKey, outputPath);
        refreshDetailsText();
    }

    void finishRun(String finalStatus) {
        overallStatus = finalStatus == null || finalStatus.isBlank() ? "Idle" : finalStatus;
        stopWaitIndicator();
        if ("Completed".equalsIgnoreCase(overallStatus)) {
            currentStage = "Complete";
            markAllProgressUnitsComplete();
            updateOverallProgressDisplay("Completed");
        } else if ("Failed".equalsIgnoreCase(overallStatus)) {
            currentStage = "Failed";
            replaceRemainingMethodStates("Running", "Failed");
            replaceRemainingMethodStates("Queued", "Stopped");
            updateOverallProgressDisplay("Failed");
        } else if ("Interrupted".equalsIgnoreCase(overallStatus) || "Stopped".equalsIgnoreCase(overallStatus)) {
            currentStage = "Stopped";
            replaceRemainingMethodStates("Running", "Stopped");
            replaceRemainingMethodStates("Queued", "Stopped");
            updateOverallProgressDisplay("Stopped");
        }
        setRunning(false);
        refreshDetailsText();
    }

    void notePipelineOutput(String outputChunk) {
        if (outputChunk == null || outputChunk.isBlank()) {
            return;
        }
        for (String line : outputChunk.split("\\R")) {
            notePipelineOutputLine(line);
        }
    }

    void setRunning(boolean running) {
        runButton.setEnabled(platformSupport.supportsPipelineExecution() && !running);
        stopButton.setEnabled(platformSupport.supportsPipelineExecution() && running);
        exportConfigButton.setEnabled(!running);
    }

    void setExporting(boolean exporting) {
        runButton.setEnabled(platformSupport.supportsPipelineExecution() && !exporting);
        stopButton.setEnabled(false);
        exportConfigButton.setEnabled(!exporting);
    }

    private JPanel buildHeader() {
        JPanel headerCard = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 6));
        headerCard.add(WorkbenchStyles.createSectionTitle("Tree Build"), BorderLayout.NORTH);
        headerCard.add(
                WorkbenchStyles.createSubtitleLabel(
                        "Review the current configuration, export a reusable config file, or start the Linux pipeline from this page."),
                BorderLayout.CENTER);
        return headerCard;
    }

    private JPanel buildMethodStatusCard() {
        JPanel statusCard = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 8));
        statusCard.add(WorkbenchStyles.createSectionTitle("Method Status"), BorderLayout.NORTH);

        JPanel statusGrid = new JPanel(new GridLayout(1, 5, 8, 0));
        statusGrid.setOpaque(false);
        statusGrid.add(createMethodStatusTile("Distance Method", TreeMethodKey.DISTANCE));
        statusGrid.add(createMethodStatusTile("Maximum Likelihood", TreeMethodKey.MAXIMUM_LIKELIHOOD));
        statusGrid.add(createMethodStatusTile("Bayes Method", TreeMethodKey.BAYESIAN));
        statusGrid.add(createMethodStatusTile("Maximum Parsimony", TreeMethodKey.PARSIMONY));
        statusGrid.add(createMethodStatusTile("Protein Structure", TreeMethodKey.PROTEIN_STRUCTURE));

        statusCard.add(statusGrid, BorderLayout.CENTER);
        return statusCard;
    }

    private JPanel buildStatusSection() {
        JPanel section = WorkbenchStyles.createCanvasPanel(new BorderLayout(0, 10));
        section.setOpaque(false);
        section.add(buildMethodStatusCard(), BorderLayout.NORTH);
        section.add(buildOverallProgressPanel(), BorderLayout.SOUTH);
        return section;
    }

    private JPanel buildOverallProgressPanel() {
        JPanel panel = WorkbenchStyles.createCanvasPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        panel.add(overallProgressBar, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMethodStatusTile(String labelText, TreeMethodKey methodKey) {
        JLabel statusLabel = createStatusValueLabel("Idle");
        StatusDot statusDot = new StatusDot();
        methodStatusLabels.put(methodKey, statusLabel);
        methodStatusDots.put(methodKey, statusDot);
        updateStatusVisuals(statusLabel, statusDot, "Idle");
        return createStatusTile(labelText, statusDot, statusLabel);
    }

    private JPanel createStatusTile(String labelText, StatusDot statusDot, JLabel statusLabel) {
        JPanel tile = new JPanel(new BorderLayout(0, 4));
        WorkbenchStyles.applyInsetCard(tile, 6, 8, 6, 8);

        JLabel titleLabel = new JLabel(labelText);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setForeground(WorkbenchStyles.TEXT_PRIMARY);
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont != null) {
            titleLabel.setFont(baseFont.deriveFont(Font.PLAIN, Math.max(10f, baseFont.getSize2D() - 1f)));
        }
        tile.add(titleLabel, BorderLayout.NORTH);

        JPanel indicatorRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        indicatorRow.setOpaque(false);
        indicatorRow.add(statusDot);
        indicatorRow.add(statusLabel);
        tile.add(indicatorRow, BorderLayout.CENTER);
        return tile;
    }

    private JLabel createStatusValueLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setForeground(WorkbenchStyles.TEXT_PRIMARY);
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont != null) {
            label.setFont(baseFont.deriveFont(Font.BOLD, Math.max(11f, baseFont.getSize2D() - 1f)));
        }
        return label;
    }

    private void resetMethodStatusesToIdle() {
        for (TreeMethodKey methodKey : TreeMethodKey.values()) {
            methodStatuses.put(methodKey, "Idle");
            methodOutputs.put(methodKey, null);
            updateMethodIndicator(methodKey, "Idle");
        }
    }

    private void updateMethodIndicator(TreeMethodKey methodKey, String statusText) {
        JLabel statusLabel = methodStatusLabels.get(methodKey);
        StatusDot statusDot = methodStatusDots.get(methodKey);
        if (statusLabel != null && statusDot != null) {
            updateStatusVisuals(statusLabel, statusDot, statusText);
        }
    }

    private void updateStatusVisuals(JLabel statusLabel, StatusDot statusDot, String statusText) {
        String normalizedStatus = statusText == null || statusText.isBlank() ? "Idle" : statusText.trim();
        statusLabel.setText(normalizedStatus);
        statusLabel.setForeground(WorkbenchStyles.statusForegroundColor(normalizedStatus));
        statusDot.setFillColor(WorkbenchStyles.statusForegroundColor(normalizedStatus));
    }

    private void replaceRemainingMethodStates(String currentValue, String replacementValue) {
        for (TreeMethodKey methodKey : TreeMethodKey.values()) {
            String methodStatus = methodStatuses.get(methodKey);
            if (currentValue.equalsIgnoreCase(methodStatus)) {
                methodStatuses.put(methodKey, replacementValue);
                updateMethodIndicator(methodKey, replacementValue);
            }
        }
    }

    private void updateOverallProgressForMethod(TreeMethodKey methodKey, String statusText) {
        if (methodKey == null || !enabledProgressMethods.contains(methodKey)) {
            return;
        }
        String normalizedStatus = statusText == null || statusText.isBlank() ? "-" : statusText.trim();
        if ("Running".equalsIgnoreCase(normalizedStatus)) {
            String message = "Running " + methodLabel(methodKey);
            if (isSlowMethod(methodKey)) {
                startWaitIndicator(message);
            } else {
                updateOverallProgressDisplay(message);
            }
            return;
        }
        if ("Completed".equalsIgnoreCase(normalizedStatus)) {
            if (isSlowMethod(methodKey)) {
                stopWaitIndicator();
            }
            completedProgressMethods.add(methodKey);
            updateOverallProgressDisplay("Completed " + methodLabel(methodKey));
            return;
        }
        if ("Skipped".equalsIgnoreCase(normalizedStatus)) {
            if (isSlowMethod(methodKey)) {
                stopWaitIndicator();
            }
            completedProgressMethods.add(methodKey);
            updateOverallProgressDisplay("Skipped " + methodLabel(methodKey));
            return;
        }
        if ("Failed".equalsIgnoreCase(normalizedStatus)) {
            if (isSlowMethod(methodKey)) {
                stopWaitIndicator();
            }
            completedProgressMethods.add(methodKey);
            updateOverallProgressDisplay("Failed " + methodLabel(methodKey));
        }
    }

    private void notePipelineOutputLine(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        if (containsAny(line, "Rerooting completed:", "定根完成")) {
            markPostBuildStepComplete(PostBuildStep.REROOTING, "Rerooting trees");
            return;
        }
        if (containsAny(line, "Restoring original sequence names in trees", "恢复原始序列名称")) {
            updateOverallProgressDisplay("Restoring sequence names");
            return;
        }
        if (containsAny(line, "Restored names in tree:", "已恢复树名称")) {
            markPostBuildStepComplete(PostBuildStep.RESTORE_NAMES, "Restoring sequence names");
            return;
        }
        if (containsAny(line, "Generating tree visualizations", "生成进化树可视化")) {
            updateOverallProgressDisplay("Generating tree visualizations");
            return;
        }
        if (containsAny(line, "Visualization completed", "可视化完成")) {
            markPostBuildStepComplete(PostBuildStep.VISUALIZATION, "Generating tree visualizations");
            return;
        }
        if (containsAny(line, "cal_pair_wise_tree_dist.R", "tree distance calculation")) {
            startWaitIndicator("Calculating tree distances");
            return;
        }
        if (containsAny(line, "Tree distance calculation completed", "树距离计算完成")) {
            markPostBuildStepComplete(PostBuildStep.TREE_DISTANCE, "Calculating tree distances");
            return;
        }
        if (containsAny(line, "Tree distance heatmaps saved", "热图已保存")) {
            markPostBuildStepComplete(PostBuildStep.HEATMAPS, "Exporting tree distance heatmaps");
            return;
        }
        if (containsAny(line, "Summary saved", "摘要已保存")) {
            markPostBuildStepComplete(PostBuildStep.SUMMARY, "Finalizing analysis summary");
        }
    }

    private void markPostBuildStepComplete(PostBuildStep step, String message) {
        if (step == null) {
            return;
        }
        if (step == PostBuildStep.TREE_DISTANCE || step == PostBuildStep.HEATMAPS) {
            stopWaitIndicator();
        }
        completedPostBuildSteps.add(step);
        updateOverallProgressDisplay(message);
    }

    private void markAllProgressUnitsComplete() {
        completedProgressMethods.addAll(enabledProgressMethods);
        for (PostBuildStep step : PostBuildStep.values()) {
            completedPostBuildSteps.add(step);
        }
        if (totalProgressSteps == 0) {
            totalProgressSteps = completedProgressMethods.size() + completedPostBuildSteps.size();
        }
    }

    private void updateOverallProgressDisplay(String message) {
        stopWaitIndicator();
        setOverallProgressDisplay(message);
    }

    private void setOverallProgressDisplay(String message) {
        if (overallProgressBar.isIndeterminate()) {
            overallProgressBar.setIndeterminate(false);
        }
        int completedSteps = completedProgressMethods.size() + completedPostBuildSteps.size();
        int safeTotal = Math.max(1, totalProgressSteps);
        overallProgressBar.setMaximum(safeTotal);
        overallProgressBar.setValue(Math.min(completedSteps, safeTotal));
        if (totalProgressSteps <= 0) {
            overallProgressBar.setString(message == null || message.isBlank() ? "Idle" : message.trim());
            return;
        }
        String safeMessage = message == null || message.isBlank() ? "Idle" : message.trim();
        overallProgressBar.setString(safeMessage + " (" + Math.min(completedSteps, totalProgressSteps) + "/" + totalProgressSteps + ")");
    }

    private void startWaitIndicator(String message) {
        waitIndicatorMessage = message == null || message.isBlank() ? "Running" : message.trim();
        waitIndicatorDotCount = 3;
        setWaitIndicatorDisplay();
        if (!waitIndicatorTimer.isRunning()) {
            waitIndicatorTimer.start();
        }
    }

    private void advanceWaitIndicator() {
        if (waitIndicatorMessage == null) {
            waitIndicatorTimer.stop();
            return;
        }
        waitIndicatorDotCount = (waitIndicatorDotCount % 3) + 1;
        setWaitIndicatorDisplay();
    }

    private void setWaitIndicatorDisplay() {
        StringBuilder dots = new StringBuilder();
        for (int index = 0; index < waitIndicatorDotCount; index++) {
            dots.append('.');
        }
        setOverallProgressDisplay(waitIndicatorMessage + ", please wait" + dots);
    }

    private void stopWaitIndicator() {
        if (waitIndicatorTimer.isRunning()) {
            waitIndicatorTimer.stop();
        }
        waitIndicatorMessage = null;
        waitIndicatorDotCount = 0;
    }

    private void showPreparingProgress() {
        overallProgressBar.setMaximum(Math.max(1, totalProgressSteps));
        overallProgressBar.setValue(0);
        overallProgressBar.setIndeterminate(true);
        overallProgressBar.setString("Preparing run");
    }

    private static boolean containsAny(String line, String first, String second) {
        return line.contains(first) || line.contains(second);
    }

    private static boolean isSlowMethod(TreeMethodKey methodKey) {
        return methodKey == TreeMethodKey.BAYESIAN || methodKey == TreeMethodKey.PROTEIN_STRUCTURE;
    }

    private void refreshDetailsText() {
        StringBuilder builder = new StringBuilder();
        builder.append("Configuration").append(System.lineSeparator());
        builder.append(draftSummary == null || draftSummary.isBlank() ? "Input type: -" : draftSummary);
        builder.append(System.lineSeparator()).append(System.lineSeparator());

        builder.append("Run state").append(System.lineSeparator());
        builder.append("Overall status: ").append(overallStatus).append(System.lineSeparator());
        builder.append("Current stage: ").append(currentStage).append(System.lineSeparator());
        builder.append("Aligned input: ").append(alignedOutputPath == null ? "-" : alignedOutputPath).append(System.lineSeparator());
        builder.append("Output root: ").append(outputDirectoryPath == null ? "-" : outputDirectoryPath).append(System.lineSeparator());
        if (!platformSupport.supportsPipelineExecution()) {
            builder.append("Execution note: Linux-only pipeline execution. Export Config remains available on this platform.")
                    .append(System.lineSeparator());
        }

        builder.append(System.lineSeparator()).append("Method progress").append(System.lineSeparator());
        for (TreeMethodKey methodKey : TreeMethodKey.values()) {
            builder.append("- ").append(methodLabel(methodKey)).append(": ")
                    .append(methodStatuses.getOrDefault(methodKey, "-"));
            Path outputPath = methodOutputs.get(methodKey);
            if (outputPath != null) {
                builder.append(" | output=").append(outputPath);
            }
            builder.append(System.lineSeparator());
        }
        builder.append(System.lineSeparator()).append("Log").append(System.lineSeparator());
        if (logBuffer.length() == 0) {
            builder.append("(no log yet)");
        } else {
            builder.append(logBuffer);
        }
        detailsArea.setText(builder.toString());
        detailsArea.setCaretPosition(detailsArea.getDocument().getLength());
    }

    private static String methodLabel(TreeMethodKey methodKey) {
        switch (methodKey) {
            case DISTANCE:
                return "Distance Method";
            case MAXIMUM_LIKELIHOOD:
                return "Maximum Likelihood";
            case BAYESIAN:
                return "Bayes Method";
            case PARSIMONY:
                return "Maximum Parsimony";
            case PROTEIN_STRUCTURE:
                return "Protein Structure";
            default:
                throw new IllegalStateException("Unexpected method key: " + methodKey);
        }
    }

    private enum PostBuildStep {
        REROOTING,
        RESTORE_NAMES,
        VISUALIZATION,
        TREE_DISTANCE,
        HEATMAPS,
        SUMMARY
    }

    private static final class StatusDot extends JPanel {
        private Color fillColor = WorkbenchStyles.TEXT_SECONDARY;

        StatusDot() {
            setOpaque(false);
            setPreferredSize(new Dimension(12, 12));
            setMinimumSize(new Dimension(12, 12));
        }

        void setFillColor(Color fillColor) {
            this.fillColor = fillColor == null ? WorkbenchStyles.TEXT_SECONDARY : fillColor;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2d.setColor(fillColor);
            graphics2d.fillOval(1, 1, Math.max(0, getWidth() - 3), Math.max(0, getHeight() - 3));
            graphics2d.dispose();
        }
    }
}
