package onebuilder;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

final class DistanceMethodPanel extends JPanel {
    private final JCheckBox enabledCheckBox;
    private final JLabel dnadistModelLabel;
    private final JComboBox<String> dnadistModelCombo;
    private final JLabel dnadistRatioLabel;
    private final JSpinner dnadistRatioSpinner;
    private final JCheckBox dnadistEmpiricalBaseFrequenciesCheckBox;
    private final JLabel neighborMethodLabel;
    private final JComboBox<String> neighborMethodCombo;
    private final JLabel neighborOutgroupLabel;
    private final JSpinner neighborOutgroupSpinner;
    private final JLabel methodOverrideLabel;
    private final JTextArea methodOverrideArea;
    private final JTextArea neighborOverrideArea;
    private InputType inputType;

    DistanceMethodPanel(InputType inputType) {
        super(new BorderLayout(12, 12));
        this.inputType = inputType;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));

        enabledCheckBox = new JCheckBox("Enable distance method", true);

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(enabledCheckBox, BorderLayout.WEST);
        header.add(
                WorkbenchStyles.createSubtitleLabel("Default use is usually just enable/disable plus optional raw PHYLIP menu responses."),
                BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JTextArea descriptionArea = WorkbenchStyles.createNoteArea(
                "Distance mode runs the existing PHYLIP distance workflow. Advanced users can override protdist/dnadist and neighbor menus directly.");

        dnadistModelLabel = new JLabel("DNA distance model");
        dnadistModelCombo = new JComboBox<>(new String[] {"F84", "Kimura", "Jukes-Cantor", "LogDet"});
        dnadistRatioLabel = new JLabel("Transition/transversion ratio");
        dnadistRatioSpinner = new JSpinner(new SpinnerNumberModel(2.0d, 0.1d, 100.0d, 0.1d));
        dnadistEmpiricalBaseFrequenciesCheckBox = new JCheckBox("Use empirical base frequencies", true);
        neighborMethodLabel = new JLabel("Neighbor tree type");
        neighborMethodCombo = new JComboBox<>(new String[] {"Neighbor-joining", "UPGMA"});
        neighborOutgroupLabel = new JLabel("Neighbor outgroup index");
        neighborOutgroupSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 999999, 1));
        methodOverrideLabel = new JLabel();
        methodOverrideArea = new JTextArea(5, 28);
        neighborOverrideArea = new JTextArea(4, 28);

        JPanel commonForm = new JPanel(new GridBagLayout());
        commonForm.setOpaque(false);
        GridBagConstraints commonConstraints = new GridBagConstraints();
        commonConstraints.insets = new Insets(0, 0, 8, 8);
        commonConstraints.anchor = GridBagConstraints.WEST;
        commonConstraints.fill = GridBagConstraints.HORIZONTAL;

        addCommonRow(commonForm, commonConstraints, 0, dnadistModelLabel, dnadistModelCombo);
        addCommonRow(commonForm, commonConstraints, 1, dnadistRatioLabel, dnadistRatioSpinner);
        commonConstraints.gridx = 0;
        commonConstraints.gridy = 2;
        commonConstraints.gridwidth = 2;
        commonForm.add(dnadistEmpiricalBaseFrequenciesCheckBox, commonConstraints);
        commonConstraints.gridwidth = 1;
        addCommonRow(commonForm, commonConstraints, 3, neighborMethodLabel, neighborMethodCombo);
        addCommonRow(commonForm, commonConstraints, 4, neighborOutgroupLabel, neighborOutgroupSpinner);

        JPanel advancedForm = new JPanel(new GridBagLayout());
        advancedForm.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 8, 8);
        advancedForm.add(methodOverrideLabel, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        advancedForm.add(new JScrollPane(methodOverrideArea), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.0;
        advancedForm.add(new JLabel("neighbor menu overrides"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        advancedForm.add(new JScrollPane(neighborOverrideArea), constraints);

        JPanel advancedContent = new JPanel(new BorderLayout(0, 8));
        advancedContent.setOpaque(false);
        advancedContent.add(advancedForm, BorderLayout.NORTH);
        advancedContent.add(
                WorkbenchStyles.createNoteArea("Enter one menu response per line. The pipeline appends Y automatically if you omit a final confirmation."),
                BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        centerPanel.add(commonForm, BorderLayout.NORTH);
        centerPanel.add(TaskPaneFactory.createBlueTaskPane("Advanced Parameters", advancedContent, true), BorderLayout.CENTER);
        centerPanel.add(descriptionArea, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);
        dnadistModelCombo.addActionListener(event -> updateDnadistControls());
        neighborMethodCombo.addActionListener(event -> updateNeighborControls());
        setInputType(inputType);
        WorkbenchStyles.applyPanelTreeBackground(this);
    }

    void setInputType(InputType inputType) {
        this.inputType = inputType;
        methodOverrideLabel.setText(inputType == InputType.PROTEIN ? "protdist menu overrides" : "dnadist menu overrides");
        boolean dnaInput = inputType == InputType.DNA_CDS;
        dnadistModelLabel.setVisible(dnaInput);
        dnadistModelCombo.setVisible(dnaInput);
        dnadistRatioLabel.setVisible(dnaInput);
        dnadistRatioSpinner.setVisible(dnaInput);
        dnadistEmpiricalBaseFrequenciesCheckBox.setVisible(dnaInput);
        updateDnadistControls();
        updateNeighborControls();
    }

    void apply(SimpleMethodConfig config) {
        enabledCheckBox.setSelected(config.enabled());
        dnadistModelCombo.setSelectedItem(config.dnadistModel());
        dnadistRatioSpinner.setValue(Double.valueOf(
                config.dnadistTransitionTransversionRatio() == null ? 2.0d : config.dnadistTransitionTransversionRatio().doubleValue()));
        dnadistEmpiricalBaseFrequenciesCheckBox.setSelected(config.dnadistEmpiricalBaseFrequencies());
        neighborMethodCombo.setSelectedItem("UPGMA".equalsIgnoreCase(config.neighborMethod()) ? "UPGMA" : "Neighbor-joining");
        neighborOutgroupSpinner.setValue(Integer.valueOf(config.neighborOutgroupIndex() == null ? 0 : config.neighborOutgroupIndex().intValue()));
        methodOverrideArea.setText(TextListCodec.joinLines(inputType == InputType.PROTEIN
                ? config.protdistMenuOverrides()
                : config.dnadistMenuOverrides()));
        neighborOverrideArea.setText(TextListCodec.joinLines(config.neighborMenuOverrides()));
        updateDnadistControls();
        updateNeighborControls();
    }

    SimpleMethodConfig toConfig() {
        return new SimpleMethodConfig(
                enabledCheckBox.isSelected(),
                inputType == InputType.DNA_CDS ? String.valueOf(dnadistModelCombo.getSelectedItem()) : "F84",
                inputType == InputType.DNA_CDS ? Double.valueOf(((Double) dnadistRatioSpinner.getValue()).doubleValue()) : Double.valueOf(2.0d),
                inputType != InputType.DNA_CDS || dnadistEmpiricalBaseFrequenciesCheckBox.isSelected(),
                "UPGMA".equals(String.valueOf(neighborMethodCombo.getSelectedItem())) ? "UPGMA" : "NJ",
                "UPGMA".equals(String.valueOf(neighborMethodCombo.getSelectedItem()))
                        ? null
                        : integerOrNull((Integer) neighborOutgroupSpinner.getValue()),
                null,
                null,
                false,
                inputType == InputType.PROTEIN ? TextListCodec.splitLines(methodOverrideArea.getText()) : java.util.List.of(),
                inputType == InputType.DNA_CDS ? TextListCodec.splitLines(methodOverrideArea.getText()) : java.util.List.of(),
                TextListCodec.splitLines(neighborOverrideArea.getText()),
                java.util.List.of(),
                java.util.List.of());
    }

    private void updateDnadistControls() {
        boolean dnaInput = inputType == InputType.DNA_CDS;
        String model = String.valueOf(dnadistModelCombo.getSelectedItem());
        boolean ratioApplicable = dnaInput && ("F84".equals(model) || "Kimura".equals(model));
        boolean baseFrequencyApplicable = dnaInput && "F84".equals(model);
        dnadistRatioLabel.setEnabled(ratioApplicable);
        dnadistRatioSpinner.setEnabled(ratioApplicable);
        dnadistEmpiricalBaseFrequenciesCheckBox.setEnabled(baseFrequencyApplicable);
    }

    private void updateNeighborControls() {
        boolean outgroupAllowed = !"UPGMA".equals(String.valueOf(neighborMethodCombo.getSelectedItem()));
        neighborOutgroupLabel.setEnabled(outgroupAllowed);
        neighborOutgroupSpinner.setEnabled(outgroupAllowed);
    }

    private static void addCommonRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            JLabel label,
            java.awt.Component component) {
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

}
