package onebuilder;

import com.jidesoft.swing.JideTabbedPane;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class TreeBuildPanel extends JPanel {
    private final JideTabbedPane methodTabs;
    private final DistanceMethodPanel distancePanel;
    private final MaximumLikelihoodPanel maximumLikelihoodPanel;
    private final BayesianPanel bayesianPanel;
    private final ParsimonyMethodPanel parsimonyPanel;
    private JLabel runStatusValue;
    private JLabel currentStageValue;
    private JLabel alignedOutputValue;
    private JLabel outputRootValue;
    private final JProgressBar progressBar;
    private final JTextArea logArea;
    private InputType inputType;

    TreeBuildPanel(InputType inputType) {
        super(new BorderLayout(12, 12));
        this.inputType = inputType;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        distancePanel = new DistanceMethodPanel(inputType);
        maximumLikelihoodPanel = new MaximumLikelihoodPanel();
        bayesianPanel = new BayesianPanel();
        parsimonyPanel = new ParsimonyMethodPanel(inputType);

        methodTabs = new JideTabbedPane(JideTabbedPane.LEFT);
        methodTabs.setTabLayoutPolicy(JideTabbedPane.SCROLL_TAB_LAYOUT);
        methodTabs.setShowCloseButton(false);
        methodTabs.setShowCloseButtonOnTab(false);
        methodTabs.addTab("Distance", distancePanel);
        methodTabs.addTab("ML", maximumLikelihoodPanel);
        methodTabs.addTab("Bayesian", bayesianPanel);
        methodTabs.addTab("Parsimony", parsimonyPanel);

        JPanel activityPanel = new JPanel(new BorderLayout(0, 12));
        activityPanel.add(buildSummaryPanel(), BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        activityPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Idle");
        activityPanel.add(progressBar, BorderLayout.SOUTH);

        add(methodTabs, BorderLayout.WEST);
        add(activityPanel, BorderLayout.CENTER);

        applyRuntimeConfig(PipelineRuntimeConfig.defaultsFor(inputType));
    }

    private JPanel buildSummaryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Run Activity"));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 6, 8);
        panel.add(new JLabel("Overall status"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        runStatusValue = new JLabel("Idle");
        panel.add(runStatusValue, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Current stage"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        currentStageValue = new JLabel("-");
        panel.add(currentStageValue, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Aligned input"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        alignedOutputValue = new JLabel("-");
        panel.add(alignedOutputValue, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Output root"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        outputRootValue = new JLabel("-");
        panel.add(outputRootValue, constraints);

        return panel;
    }

    JideTabbedPane methodTabs() {
        return methodTabs;
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
        logArea.setText("");
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
        progressBar.setIndeterminate(true);
        progressBar.setString("Running");
    }

    void appendLog(String line) {
        if (line == null) {
            return;
        }
        logArea.append(line);
        logArea.append(System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    void setOverallStatus(String statusText) {
        runStatusValue.setText(statusText == null ? "-" : statusText);
    }

    void setCurrentStage(String stageText) {
        currentStageValue.setText(stageText == null ? "-" : stageText);
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
        progressBar.setIndeterminate(false);
        progressBar.setString(finalStatus == null ? "Idle" : finalStatus);
    }
}
