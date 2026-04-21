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
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
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
    private final JLabel proteinStructureStatusLabel;
    private final StatusDot proteinStructureStatusDot;
    private String draftSummary;
    private String overallStatus;
    private String currentStage;
    private Path alignedOutputPath;
    private Path outputDirectoryPath;

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
        this.draftSummary = "Input type: -" + System.lineSeparator() + "Input FASTA/MSA: -";
        this.overallStatus = "Idle";
        this.currentStage = "-";
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
        proteinStructureStatusLabel = createStatusValueLabel("Reserved");
        proteinStructureStatusDot = new StatusDot();
        updateStatusVisuals(proteinStructureStatusLabel, proteinStructureStatusDot, "Reserved");

        JPanel bodyPanel = WorkbenchStyles.createCanvasPanel(new BorderLayout(0, 12));
        bodyPanel.add(detailsCard, BorderLayout.CENTER);
        bodyPanel.add(buildMethodStatusCard(), BorderLayout.SOUTH);
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

    void applyPreferences() {
        revalidate();
        repaint();
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
        for (TreeMethodKey methodKey : TreeMethodKey.values()) {
            methodStatuses.put(methodKey, "Queued");
            methodOutputs.put(methodKey, null);
            updateMethodIndicator(methodKey, "Queued");
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
        refreshDetailsText();
    }

    void setMethodOutput(TreeMethodKey methodKey, Path outputPath) {
        methodOutputs.put(methodKey, outputPath);
        refreshDetailsText();
    }

    void finishRun(String finalStatus) {
        overallStatus = finalStatus == null || finalStatus.isBlank() ? "Idle" : finalStatus;
        if ("Completed".equalsIgnoreCase(overallStatus)) {
            currentStage = "Complete";
        } else if ("Failed".equalsIgnoreCase(overallStatus)) {
            currentStage = "Failed";
            replaceRemainingMethodStates("Running", "Failed");
            replaceRemainingMethodStates("Queued", "Stopped");
        } else if ("Interrupted".equalsIgnoreCase(overallStatus) || "Stopped".equalsIgnoreCase(overallStatus)) {
            currentStage = "Stopped";
            replaceRemainingMethodStates("Running", "Stopped");
            replaceRemainingMethodStates("Queued", "Stopped");
        }
        setRunning(false);
        refreshDetailsText();
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
        statusGrid.add(createStatusTile("Protein Structure", proteinStructureStatusDot, proteinStructureStatusLabel));

        statusCard.add(statusGrid, BorderLayout.CENTER);
        return statusCard;
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
        builder.append("- Protein Structure: Reserved").append(System.lineSeparator());

        builder.append(System.lineSeparator()).append("Log").append(System.lineSeparator());
        if (logBuffer.length() == 0) {
            builder.append("(no log yet)");
        } else {
            builder.append(logBuffer);
        }
        detailsArea.setText(builder.toString());
        detailsArea.setCaretPosition(0);
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
            default:
                throw new IllegalStateException("Unexpected method key: " + methodKey);
        }
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
