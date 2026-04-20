package onebuilder;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

final class TreeBuildPanel extends JPanel {
    private final DistanceMethodPanel distancePanel;
    private final MaximumLikelihoodPanel maximumLikelihoodPanel;
    private final BayesianPanel bayesianPanel;
    private final ParsimonyMethodPanel parsimonyPanel;
    private final LogDrawerPanel logDrawerPanel;
    private JLabel runStatusValue;
    private JLabel currentStageValue;
    private JLabel alignedOutputValue;
    private JLabel outputRootValue;
    private InputType inputType;

    TreeBuildPanel(InputType inputType) {
        super(new BorderLayout(0, 16));
        this.inputType = inputType;
        WorkbenchStyles.applyCanvas(this);

        distancePanel = new DistanceMethodPanel(inputType);
        maximumLikelihoodPanel = new MaximumLikelihoodPanel();
        bayesianPanel = new BayesianPanel();
        parsimonyPanel = new ParsimonyMethodPanel(inputType);
        logDrawerPanel = new LogDrawerPanel();

        add(buildSummaryCard(), BorderLayout.NORTH);
        add(buildMethodGrid(), BorderLayout.CENTER);
        add(logDrawerPanel, BorderLayout.SOUTH);

        applyRuntimeConfig(PipelineRuntimeConfig.defaultsFor(inputType));
    }

    List<String> methodCardTitles() {
        return Arrays.asList("Distance", "ML", "Bayesian", "Parsimony");
    }

    boolean hasBottomLogDrawer() {
        return true;
    }

    boolean isLogDrawerCollapsed() {
        return logDrawerPanel.isCollapsed();
    }

    void applyPreferences() {
        WorkbenchStyles.styleMonospaceLog(logDrawerPanel.logArea());
        revalidate();
        repaint();
    }

    private JPanel buildSummaryCard() {
        JPanel card = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 16));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 4));
        titleBlock.setOpaque(false);
        titleBlock.add(WorkbenchStyles.createSectionTitle("Tree Build"), BorderLayout.NORTH);
        titleBlock.add(
                WorkbenchStyles.createSubtitleLabel("Edit the four methods side by side, then open the log drawer only when you need detail."),
                BorderLayout.CENTER);
        card.add(titleBlock, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 10, 18);
        fields.add(new JLabel("Overall status"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        runStatusValue = WorkbenchStyles.createStatusChip("Idle");
        fields.add(runStatusValue, constraints);

        constraints.gridx = 2;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        fields.add(new JLabel("Current stage"), constraints);

        constraints.gridx = 3;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        currentStageValue = new JLabel("-");
        fields.add(currentStageValue, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        fields.add(new JLabel("Aligned input"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.gridwidth = 3;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        alignedOutputValue = new JLabel("-");
        fields.add(alignedOutputValue, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        fields.add(new JLabel("Output root"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.gridwidth = 3;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        outputRootValue = new JLabel("-");
        fields.add(outputRootValue, constraints);

        card.add(fields, BorderLayout.CENTER);
        return card;
    }

    private JScrollPane buildMethodGrid() {
        JPanel grid = WorkbenchStyles.createCanvasPanel(new GridLayout(2, 2, 16, 16));
        grid.add(createMethodCard("Distance", "PHYLIP distance + neighbor workflow.", distancePanel));
        grid.add(createMethodCard("ML", "IQ-TREE likelihood inference.", maximumLikelihoodPanel));
        grid.add(createMethodCard("Bayesian", "MrBayes posterior sampling and consensus.", bayesianPanel));
        grid.add(createMethodCard("Parsimony", "PHYLIP parsimony workflow.", parsimonyPanel));

        JScrollPane scrollPane = new JScrollPane(grid);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(WorkbenchStyles.CANVAS_BACKGROUND);
        return scrollPane;
    }

    private JPanel createMethodCard(String title, String subtitle, JPanel content) {
        JPanel card = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 14));
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(WorkbenchStyles.createSectionTitle(title), BorderLayout.NORTH);
        header.add(WorkbenchStyles.createSubtitleLabel(subtitle), BorderLayout.CENTER);
        card.add(header, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    void setInputType(InputType inputType) {
        this.inputType = inputType;
        distancePanel.setInputType(inputType);
        maximumLikelihoodPanel.setInputType(inputType);
        bayesianPanel.setInputType(inputType);
        parsimonyPanel.setInputType(inputType);
    }

    void applyRuntimeConfig(PipelineRuntimeConfig runtimeConfig) {
        setInputType(runtimeConfig.inputType());
        distancePanel.apply(runtimeConfig.distance());
        maximumLikelihoodPanel.apply(runtimeConfig.maximumLikelihood(), runtimeConfig.inputType());
        bayesianPanel.apply(runtimeConfig.bayesian(), runtimeConfig.inputType());
        parsimonyPanel.apply(runtimeConfig.parsimony());
    }

    PipelineRuntimeConfig runtimeConfig() {
        return new PipelineRuntimeConfig(
                inputType,
                distancePanel.toConfig(),
                maximumLikelihoodPanel.toConfig(),
                bayesianPanel.toConfig(inputType),
                parsimonyPanel.toConfig());
    }

    void resetForRun(ExecutionPlan executionPlan) {
        logDrawerPanel.logArea().setText("");
        setOverallStatus("Running");
        setCurrentStage("Preparing");
        setAlignedOutput(executionPlan.effectiveInputFile());
        setOutputDirectory(executionPlan.pipelineOutputDir());
        setMethodStatus(TreeMethodKey.DISTANCE, "Queued");
        setMethodStatus(TreeMethodKey.MAXIMUM_LIKELIHOOD, "Queued");
        setMethodStatus(TreeMethodKey.BAYESIAN, "Queued");
        setMethodStatus(TreeMethodKey.PARSIMONY, "Queued");
        setMethodOutput(TreeMethodKey.DISTANCE, null);
        setMethodOutput(TreeMethodKey.MAXIMUM_LIKELIHOOD, null);
        setMethodOutput(TreeMethodKey.BAYESIAN, null);
        setMethodOutput(TreeMethodKey.PARSIMONY, null);
        logDrawerPanel.setProgressRunning(true);
        logDrawerPanel.setProgressText("Running");
        logDrawerPanel.setStageText("Running");
    }

    void appendLog(String line) {
        if (line == null) {
            return;
        }
        logDrawerPanel.logArea().append(line);
        logDrawerPanel.logArea().append(System.lineSeparator());
        logDrawerPanel.logArea().setCaretPosition(logDrawerPanel.logArea().getDocument().getLength());
    }

    void setOverallStatus(String statusText) {
        WorkbenchStyles.updateStatusChip(runStatusValue, statusText == null ? "Idle" : statusText);
    }

    void setCurrentStage(String stageText) {
        currentStageValue.setText(stageText == null ? "-" : stageText);
        logDrawerPanel.setStageText(stageText);
    }

    void setAlignedOutput(Path alignedOutput) {
        alignedOutputValue.setText(alignedOutput == null ? "-" : alignedOutput.toString());
    }

    void setOutputDirectory(Path outputDirectory) {
        outputRootValue.setText(outputDirectory == null ? "-" : outputDirectory.toString());
    }

    void setMethodStatus(TreeMethodKey methodKey, String statusText) {
        switch (methodKey) {
            case DISTANCE:
                distancePanel.setStatusText(statusText);
                break;
            case MAXIMUM_LIKELIHOOD:
                maximumLikelihoodPanel.setStatusText(statusText);
                break;
            case BAYESIAN:
                bayesianPanel.setStatusText(statusText);
                break;
            case PARSIMONY:
                parsimonyPanel.setStatusText(statusText);
                break;
            default:
                throw new IllegalStateException("Unexpected method key: " + methodKey);
        }
    }

    void setMethodOutput(TreeMethodKey methodKey, Path outputPath) {
        switch (methodKey) {
            case DISTANCE:
                distancePanel.setOutputPath(outputPath);
                break;
            case MAXIMUM_LIKELIHOOD:
                maximumLikelihoodPanel.setOutputPath(outputPath);
                break;
            case BAYESIAN:
                bayesianPanel.setOutputPath(outputPath);
                break;
            case PARSIMONY:
                parsimonyPanel.setOutputPath(outputPath);
                break;
            default:
                throw new IllegalStateException("Unexpected method key: " + methodKey);
        }
    }

    void finishRun(String finalStatus) {
        setOverallStatus(finalStatus);
        logDrawerPanel.setProgressRunning(false);
        logDrawerPanel.setProgressText(finalStatus == null ? "Idle" : finalStatus);
        logDrawerPanel.setStageText(finalStatus == null ? "Idle" : finalStatus);
    }
}
