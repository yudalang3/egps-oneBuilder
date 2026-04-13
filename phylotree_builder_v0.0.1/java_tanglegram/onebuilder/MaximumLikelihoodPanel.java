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
    private final JLabel statusValue;
    private final JLabel outputValue;

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

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 8, 8);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.gridx = 0;
        constraints.gridy = 0;
        formPanel.add(new JLabel("Model strategy"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        modelStrategyCombo = new JComboBox<>(new String[] {"MFP", "TESTONLY", "LG", "GTR"});
        formPanel.add(modelStrategyCombo, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Bootstrap"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        bootstrapSpinner = new JSpinner(new SpinnerNumberModel(1000, 0, 1000000, 100));
        formPanel.add(bootstrapSpinner, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.0;
        modelSetLabel = new JLabel("Model set");
        formPanel.add(modelSetLabel, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        modelSetField = new JTextField();
        formPanel.add(modelSetField, constraints);

        JTextArea note = new JTextArea(
                "IQ-TREE runs through the existing wrapper. Protein mode can restrict the candidate model set; DNA/CDS keeps this field disabled.");
        note.setEditable(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setOpaque(false);
        note.setBorder(null);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(note, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

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

        add(statusPanel, BorderLayout.SOUTH);
    }

    void apply(MaximumLikelihoodConfig config, InputType inputType) {
        enabledCheckBox.setSelected(config.enabled());
        bootstrapSpinner.setValue(Integer.valueOf(config.bootstrapReplicates()));
        modelStrategyCombo.setSelectedItem(config.modelStrategy());
        modelSetField.setText(config.modelSet());
        setInputType(inputType);
    }

    MaximumLikelihoodConfig toConfig() {
        return new MaximumLikelihoodConfig(
                enabledCheckBox.isSelected(),
                ((Integer) bootstrapSpinner.getValue()).intValue(),
                String.valueOf(modelStrategyCombo.getSelectedItem()).trim(),
                modelSetField.isEnabled() ? modelSetField.getText().trim() : "");
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
}
