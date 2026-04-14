package onebuilder;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
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

final class MaximumLikelihoodPanel extends JPanel {
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
    private final JSpinner alrtSpinner;
    private final JCheckBox abayesCheckBox;
    private final JTextArea extraArgsArea;
    private JLabel statusValue;
    private JLabel outputValue;

    MaximumLikelihoodPanel() {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titleLabel = new JLabel("Maximum Likelihood");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D() + 1f));
        enabledCheckBox = new JCheckBox("Enable", true);

        JPanel header = new JPanel(new BorderLayout());
        header.add(titleLabel, BorderLayout.WEST);
        header.add(enabledCheckBox, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        bootstrapSpinner = new JSpinner(new SpinnerNumberModel(1000, 0, 1000000, 100));
        modelStrategyCombo = new JComboBox<>(new String[] {"MFP", "MF", "TESTONLY", "LG", "GTR"});
        modelSetField = new JTextField();
        modelSetLabel = new JLabel("Model set");
        threadsField = new JTextField();
        threadsMaxSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 512, 1));
        seedSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        safeCheckBox = new JCheckBox("Use -safe");
        keepIdentCheckBox = new JCheckBox("Use -keep-ident");
        quietCheckBox = new JCheckBox("Use --quiet", true);
        verboseCheckBox = new JCheckBox("Use -v");
        redoCheckBox = new JCheckBox("Use -redo", true);
        memoryLimitField = new JTextField();
        outgroupField = new JTextField();
        sequenceTypeCombo = new JComboBox<>(new String[] {"", "AA", "DNA", "CODON", "NT2AA", "BIN", "MORPH"});
        alrtSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000000, 100));
        abayesCheckBox = new JCheckBox("Use -abayes");
        extraArgsArea = new JTextArea(5, 28);

        JPanel commonForm = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = baseConstraints();
        addRow(commonForm, constraints, 0, "Model strategy", modelStrategyCombo);
        addRow(commonForm, constraints, 1, "Bootstrap", bootstrapSpinner);
        addRow(commonForm, constraints, 2, modelSetLabel, modelSetField);

        JTextArea note = new JTextArea(
                "Common ML settings cover the usual IQ-TREE workflow. Protein mode can restrict the candidate model set; DNA/CDS keeps this field disabled.");
        note.setEditable(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setOpaque(false);
        note.setBorder(null);

        JPanel advancedForm = new JPanel(new GridBagLayout());
        GridBagConstraints advancedConstraints = baseConstraints();
        addRow(advancedForm, advancedConstraints, 0, "Threads (-nt)", threadsField);
        addRow(advancedForm, advancedConstraints, 1, "Thread cap (-ntmax)", threadsMaxSpinner);
        addRow(advancedForm, advancedConstraints, 2, "Seed (-seed)", seedSpinner);
        addRow(advancedForm, advancedConstraints, 3, "Memory limit (-mem)", memoryLimitField);
        addRow(advancedForm, advancedConstraints, 4, "Outgroup (-o)", outgroupField);
        addRow(advancedForm, advancedConstraints, 5, "Sequence type (-st)", sequenceTypeCombo);
        addRow(advancedForm, advancedConstraints, 6, "SH-aLRT (-alrt)", alrtSpinner);

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

        JTextArea advancedNote = new JTextArea(
                "Advanced fields map directly to IQ-TREE flags. For extra_args, enter one token per line.");
        advancedNote.setEditable(false);
        advancedNote.setLineWrap(true);
        advancedNote.setWrapStyleWord(true);
        advancedNote.setOpaque(false);
        advancedNote.setBorder(null);

        JPanel advancedContent = new JPanel(new BorderLayout(0, 8));
        advancedContent.add(advancedForm, BorderLayout.NORTH);
        advancedContent.add(advancedNote, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.add(commonForm, BorderLayout.NORTH);
        centerPanel.add(note, BorderLayout.CENTER);
        centerPanel.add(new CollapsibleSectionPanel("Advanced Parameters", advancedContent, true), BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        add(buildStatusPanel(), BorderLayout.SOUTH);
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
                integerOrNull((Integer) alrtSpinner.getValue()),
                abayesCheckBox.isSelected(),
                TextListCodec.splitLines(extraArgsArea.getText()));
    }

    void setInputType(InputType inputType) {
        boolean protein = inputType == InputType.PROTEIN;
        modelSetField.setEnabled(protein);
        modelSetLabel.setEnabled(protein);
    }

    void setStatusText(String statusText) {
        statusValue.setText(statusText == null ? "-" : statusText);
    }

    void setOutputPath(Path outputPath) {
        outputValue.setText(outputPath == null ? "-" : outputPath.toString());
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
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0.0;
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        panel.add(component, constraints);
    }

    private JPanel buildStatusPanel() {
        JPanel statusPanel = new JPanel(new GridBagLayout());
        GridBagConstraints statusConstraints = new GridBagConstraints();
        statusConstraints.gridx = 0;
        statusConstraints.gridy = 0;
        statusConstraints.insets = new Insets(0, 0, 6, 8);
        statusConstraints.anchor = GridBagConstraints.WEST;
        statusPanel.add(new JLabel("Status"), statusConstraints);

        statusConstraints.gridx = 1;
        statusConstraints.weightx = 1.0;
        statusConstraints.fill = GridBagConstraints.HORIZONTAL;
        statusValue = new JLabel("Idle");
        statusPanel.add(statusValue, statusConstraints);

        statusConstraints.gridx = 0;
        statusConstraints.gridy = 1;
        statusConstraints.weightx = 0.0;
        statusConstraints.fill = GridBagConstraints.NONE;
        statusPanel.add(new JLabel("Output"), statusConstraints);

        statusConstraints.gridx = 1;
        statusConstraints.weightx = 1.0;
        statusConstraints.fill = GridBagConstraints.HORIZONTAL;
        outputValue = new JLabel("-");
        statusPanel.add(outputValue, statusConstraints);
        return statusPanel;
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
