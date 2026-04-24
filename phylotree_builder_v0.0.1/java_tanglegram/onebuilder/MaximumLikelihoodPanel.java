package onebuilder;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

final class MaximumLikelihoodPanel extends JPanel {
    private static final List<String> PROTEIN_MODEL_STRATEGIES = List.of("MFP", "TEST", "LG", "WAG", "JTT");
    private static final List<String> DNA_MODEL_STRATEGIES = List.of("MFP", "TEST", "GTR", "HKY", "JC");

    private final JCheckBox enabledCheckBox;
    private final JSpinner bootstrapSpinner;
    private final JComboBox<String> modelStrategyCombo;
    private final JTextField modelSetField;
    private final JLabel modelSetLabel;
    private final JTextField threadsField;
    private final JSpinner threadsMaxSpinner;
    private final JSpinner seedSpinner;
    private final JCheckBox safeCheckBox;
    private final JCheckBox keepIdentCheckBox;
    private final JCheckBox quietCheckBox;
    private final JCheckBox verboseCheckBox;
    private final JCheckBox redoCheckBox;
    private final JTextField memoryLimitField;
    private final JTextField outgroupField;
    private final JComboBox<String> sequenceTypeCombo;
    private final JCheckBox alrtCheckBox;
    private final JSpinner alrtSpinner;
    private final JCheckBox abayesCheckBox;
    private final JTextArea extraArgsArea;
    private final JTextArea bootstrapGuidanceArea;

    MaximumLikelihoodPanel() {
        super(new BorderLayout(12, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));

        enabledCheckBox = new JCheckBox("Enable maximum likelihood", true);
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(enabledCheckBox, BorderLayout.WEST);
        header.add(
                WorkbenchStyles.createNoteArea("Keep the common IQ-TREE workflow visible and tuck deep search/runtime flags into the advanced drawer."),
                BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        bootstrapSpinner = new JSpinner(new SpinnerNumberModel(1000, 0, 1000000, 100));
        modelStrategyCombo = new JComboBox<>();
        modelSetField = new JTextField();
        modelSetLabel = new JLabel("Model set");
        threadsField = new JTextField();
        threadsMaxSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 512, 1));
        seedSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        safeCheckBox = new JCheckBox("Use -safe");
        keepIdentCheckBox = new JCheckBox("Use -keep-ident");
        quietCheckBox = new JCheckBox("Use -quiet", true);
        verboseCheckBox = new JCheckBox("Use -v");
        redoCheckBox = new JCheckBox("Use -redo", true);
        memoryLimitField = new JTextField();
        outgroupField = new JTextField();
        sequenceTypeCombo = new JComboBox<>(new String[] {"", "AA", "DNA", "CODON", "NT2AA", "BIN", "MORPH"});
        alrtCheckBox = new JCheckBox("Enable -alrt");
        alrtSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000000, 100));
        abayesCheckBox = new JCheckBox("Use -abayes");
        extraArgsArea = new JTextArea(5, 28);
        bootstrapGuidanceArea = WorkbenchStyles.createNoteArea("");

        JPanel commonForm = new JPanel(new GridBagLayout());
        commonForm.setOpaque(false);
        GridBagConstraints constraints = baseConstraints();
        addRow(commonForm, constraints, 0, "Model strategy", modelStrategyCombo);
        addRow(commonForm, constraints, 1, "Bootstrap", bootstrapSpinner);
        addRow(commonForm, constraints, 2, modelSetLabel, modelSetField);

        JPanel advancedForm = new JPanel(new GridBagLayout());
        advancedForm.setOpaque(false);
        GridBagConstraints advancedConstraints = baseConstraints();
        addRow(advancedForm, advancedConstraints, 0, "Threads (-nt)", threadsField);
        addRow(advancedForm, advancedConstraints, 1, "Thread cap (-ntmax)", threadsMaxSpinner);
        addRow(advancedForm, advancedConstraints, 2, "Seed (-seed)", seedSpinner);
        addRow(advancedForm, advancedConstraints, 3, "Memory limit (-mem)", memoryLimitField);
        addRow(advancedForm, advancedConstraints, 4, "Outgroup (-o)", outgroupField);
        addRow(advancedForm, advancedConstraints, 5, "Sequence type (-st)", sequenceTypeCombo);
        addRow(advancedForm, advancedConstraints, 6, alrtCheckBox, alrtSpinner);

        advancedConstraints.gridx = 0;
        advancedConstraints.gridy = 7;
        advancedConstraints.gridwidth = 2;
        advancedForm.add(safeCheckBox, advancedConstraints);

        advancedConstraints.gridy = 8;
        advancedForm.add(keepIdentCheckBox, advancedConstraints);

        advancedConstraints.gridy = 9;
        advancedForm.add(quietCheckBox, advancedConstraints);

        advancedConstraints.gridy = 10;
        advancedForm.add(verboseCheckBox, advancedConstraints);

        advancedConstraints.gridy = 11;
        advancedForm.add(redoCheckBox, advancedConstraints);

        advancedConstraints.gridy = 12;
        advancedForm.add(abayesCheckBox, advancedConstraints);

        advancedConstraints.gridy = 13;
        advancedConstraints.gridwidth = 1;
        advancedForm.add(new JLabel("Extra args"), advancedConstraints);

        advancedConstraints.gridx = 1;
        advancedConstraints.weightx = 1.0;
        advancedConstraints.fill = GridBagConstraints.BOTH;
        advancedForm.add(new JScrollPane(extraArgsArea), advancedConstraints);

        JPanel advancedContent = new JPanel(new BorderLayout(0, 8));
        advancedContent.setOpaque(false);
        advancedContent.add(advancedForm, BorderLayout.NORTH);
        advancedContent.add(
                WorkbenchStyles.createNoteArea("Advanced fields map directly to IQ-TREE flags. For extra_args, enter one token per line."),
                BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        centerPanel.add(commonForm, BorderLayout.NORTH);
        JPanel lowerPanel = new JPanel(new BorderLayout(0, 8));
        lowerPanel.setOpaque(false);
        lowerPanel.add(TaskPaneFactory.createBlueTaskPane("Advanced Parameters", advancedContent, true), BorderLayout.CENTER);
        JPanel notePanel = new JPanel(new BorderLayout(0, 6));
        notePanel.setOpaque(false);
        notePanel.add(bootstrapGuidanceArea, BorderLayout.NORTH);
        notePanel.add(
                WorkbenchStyles.createNoteArea("Common ML settings cover the usual IQ-TREE workflow. The model set field stays available for MFP and TEST, and is disabled for fixed-model runs."),
                BorderLayout.CENTER);
        lowerPanel.add(notePanel, BorderLayout.SOUTH);
        centerPanel.add(lowerPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
        WorkbenchStyles.applyPanelTreeBackground(this);
        modelStrategyCombo.addActionListener(event -> updateModelSetControls());
        alrtCheckBox.addActionListener(event -> updateAlrtControls());
        ChangeListener bootstrapListener = event -> updateBootstrapGuidance();
        bootstrapSpinner.addChangeListener(bootstrapListener);
        setInputType(InputType.PROTEIN);
        updateAlrtControls();
        updateBootstrapGuidance();
    }

    void apply(MaximumLikelihoodConfig config, InputType inputType) {
        enabledCheckBox.setSelected(config.enabled());
        bootstrapSpinner.setValue(Integer.valueOf(config.bootstrapReplicates()));
        modelStrategyCombo.setSelectedItem(config.modelStrategy());
        modelSetField.setText(config.modelSet());
        threadsField.setText(nullToEmpty(config.threads()));
        threadsMaxSpinner.setValue(Integer.valueOf(config.threadsMax() == null ? 0 : config.threadsMax().intValue()));
        seedSpinner.setValue(Integer.valueOf(config.seed() == null ? 0 : config.seed().intValue()));
        safeCheckBox.setSelected(config.safe());
        keepIdentCheckBox.setSelected(config.keepIdent());
        quietCheckBox.setSelected(config.quiet());
        verboseCheckBox.setSelected(config.verbose());
        redoCheckBox.setSelected(config.redo());
        memoryLimitField.setText(nullToEmpty(config.memoryLimit()));
        outgroupField.setText(nullToEmpty(config.outgroup()));
        sequenceTypeCombo.setSelectedItem(config.sequenceType() == null ? "" : config.sequenceType());
        alrtSpinner.setValue(Integer.valueOf(config.alrt() == null ? 0 : config.alrt().intValue()));
        abayesCheckBox.setSelected(config.abayes());
        extraArgsArea.setText(TextListCodec.joinLines(config.extraArgs()));
        setInputType(inputType);
        alrtCheckBox.setSelected(config.alrt() != null);
        updateAlrtControls();
        updateBootstrapGuidance();
    }

    MaximumLikelihoodConfig toConfig() {
        return new MaximumLikelihoodConfig(
                enabledCheckBox.isSelected(),
                ((Integer) bootstrapSpinner.getValue()).intValue(),
                String.valueOf(modelStrategyCombo.getSelectedItem()).trim(),
                modelSetField.isEnabled() ? modelSetField.getText().trim() : "",
                blankToNull(threadsField.getText()),
                integerOrNull((Integer) threadsMaxSpinner.getValue()),
                integerOrNull((Integer) seedSpinner.getValue()),
                safeCheckBox.isSelected(),
                keepIdentCheckBox.isSelected(),
                quietCheckBox.isSelected(),
                verboseCheckBox.isSelected(),
                redoCheckBox.isSelected(),
                blankToNull(memoryLimitField.getText()),
                blankToNull(outgroupField.getText()),
                blankToNull(String.valueOf(sequenceTypeCombo.getSelectedItem())),
                alrtCheckBox.isSelected() ? Integer.valueOf(((Integer) alrtSpinner.getValue()).intValue()) : null,
                abayesCheckBox.isSelected(),
                TextListCodec.splitLines(extraArgsArea.getText()));
    }

    void setInputType(InputType inputType) {
        String currentStrategy = String.valueOf(modelStrategyCombo.getSelectedItem());
        List<String> strategies = inputType == InputType.PROTEIN ? PROTEIN_MODEL_STRATEGIES : DNA_MODEL_STRATEGIES;
        repopulateModelStrategies(strategies, currentStrategy);
        updateModelSetControls();
    }

    List<String> modelStrategyOptionsForTest() {
        List<String> options = new ArrayList<>();
        for (int index = 0; index < modelStrategyCombo.getItemCount(); index++) {
            options.add(String.valueOf(modelStrategyCombo.getItemAt(index)));
        }
        return options;
    }

    boolean isModelSetEnabledForTest() {
        return modelSetField.isEnabled();
    }

    void setModelStrategyForTest(String strategy) {
        modelStrategyCombo.setSelectedItem(strategy);
    }

    String bootstrapGuidanceTextForTest() {
        return bootstrapGuidanceArea.getText();
    }

    void setBootstrapReplicatesForTest(int value) {
        bootstrapSpinner.setValue(Integer.valueOf(value));
        updateBootstrapGuidance();
    }

    void setAlrtEnabledForTest(boolean enabled) {
        alrtCheckBox.setSelected(enabled);
        updateAlrtControls();
        updateBootstrapGuidance();
    }

    private void repopulateModelStrategies(List<String> strategies, String currentStrategy) {
        modelStrategyCombo.removeAllItems();
        for (String strategy : strategies) {
            modelStrategyCombo.addItem(strategy);
        }
        String normalizedSelection = normalizeStrategy(strategies, currentStrategy);
        modelStrategyCombo.setSelectedItem(normalizedSelection);
    }

    private void updateAlrtControls() {
        alrtSpinner.setEnabled(alrtCheckBox.isSelected());
        updateBootstrapGuidance();
    }

    private void updateBootstrapGuidance() {
        int bootstrapReplicates = ((Integer) bootstrapSpinner.getValue()).intValue();
        boolean warning = false;
        String text;
        if (bootstrapReplicates == 0) {
            warning = true;
            text = "Bootstrap 0 skips IQ-TREE ultrafast bootstrap (-bb). Use this only when you do not want bootstrap support values.";
        } else if (bootstrapReplicates < 100) {
            warning = true;
            text = "Bootstrap below 100 is allowed, but support summaries are usually too weak for routine runs. 1000 remains the recommended default.";
        } else if (bootstrapReplicates > 5000) {
            warning = true;
            text = "Large bootstrap counts can increase IQ-TREE runtime substantially. 1000 is the recommended default unless you need a heavier support analysis.";
        } else {
            text = "Bootstrap 1000 is the recommended default for routine IQ-TREE runs. Increase it only when you explicitly want more support resampling.";
        }
        if (alrtCheckBox.isSelected()) {
            text += " -alrt is a separate support metric and does not replace bootstrap.";
        }
        bootstrapGuidanceArea.setText(text);
        bootstrapGuidanceArea.setForeground(warning ? WorkbenchStyles.WARNING : WorkbenchStyles.TEXT_SECONDARY);
    }

    private void updateModelSetControls() {
        String strategy = String.valueOf(modelStrategyCombo.getSelectedItem());
        boolean enableModelSet = Arrays.asList("MFP", "TEST").contains(strategy);
        modelSetField.setEnabled(enableModelSet);
        modelSetLabel.setEnabled(enableModelSet);
        if (!enableModelSet) {
            modelSetField.setText("");
        }
    }

    private static String normalizeStrategy(List<String> allowedStrategies, String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return allowedStrategies.get(0);
        }
        String trimmed = strategy.trim();
        if ("MF".equals(trimmed) || "TESTNEWONLY".equals(trimmed) || "TESTNEW".equals(trimmed)) {
            trimmed = "MFP";
        } else if ("TESTONLY".equals(trimmed)) {
            trimmed = "TEST";
        }
        return allowedStrategies.contains(trimmed) ? trimmed : allowedStrategies.get(0);
    }

    private static GridBagConstraints baseConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 8, 8);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        return constraints;
    }

    private static void addRow(JPanel panel, GridBagConstraints constraints, int row, String label, java.awt.Component component) {
        addRow(panel, constraints, row, new JLabel(label), component);
    }

    private static void addRow(JPanel panel, GridBagConstraints constraints, int row, JLabel label, java.awt.Component component) {
        addRow(panel, constraints, row, (java.awt.Component) label, component);
    }

    private static void addRow(JPanel panel, GridBagConstraints constraints, int row, java.awt.Component label, java.awt.Component component) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0.0;
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        panel.add(component, constraints);
    }

    private static Integer integerOrNull(Integer value) {
        return value == null || value.intValue() <= 0 ? null : Integer.valueOf(value.intValue());
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
