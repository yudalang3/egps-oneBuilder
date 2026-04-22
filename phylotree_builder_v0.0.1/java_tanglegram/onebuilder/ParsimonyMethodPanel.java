package onebuilder;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

final class ParsimonyMethodPanel extends JPanel {
    private final JCheckBox enabledCheckBox;
    private final JLabel outgroupLabel;
    private final JSpinner outgroupSpinner;
    private final JCheckBox dnaparsTransversionCheckBox;
    private final JLabel methodOverrideLabel;
    private final JTextArea methodOverrideArea;
    private InputType inputType;

    ParsimonyMethodPanel(InputType inputType) {
        super(new BorderLayout(12, 12));
        this.inputType = inputType;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));

        enabledCheckBox = new JCheckBox("Enable parsimony method", true);
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(enabledCheckBox, BorderLayout.WEST);
        header.add(
                WorkbenchStyles.createSubtitleLabel("Keep routine runs simple and expose raw PHYLIP menu responses only on demand."),
                BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JTextArea descriptionArea = WorkbenchStyles.createNoteArea(
                "Parsimony mode runs the existing PHYLIP parsimony workflow. Advanced users can override protpars/dnapars menu choices directly.");

        outgroupLabel = new JLabel("Outgroup index");
        outgroupSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 999999, 1));
        dnaparsTransversionCheckBox = new JCheckBox("Use transversion parsimony");
        methodOverrideLabel = new JLabel();
        methodOverrideArea = new JTextArea(6, 28);

        JPanel commonForm = new JPanel(new GridBagLayout());
        commonForm.setOpaque(false);
        GridBagConstraints commonConstraints = new GridBagConstraints();
        commonConstraints.insets = new Insets(0, 0, 8, 8);
        commonConstraints.anchor = GridBagConstraints.WEST;
        commonConstraints.fill = GridBagConstraints.HORIZONTAL;
        commonConstraints.gridx = 0;
        commonConstraints.gridy = 0;
        commonConstraints.weightx = 0.0;
        commonForm.add(outgroupLabel, commonConstraints);

        commonConstraints.gridx = 1;
        commonConstraints.weightx = 1.0;
        commonForm.add(outgroupSpinner, commonConstraints);

        commonConstraints.gridx = 0;
        commonConstraints.gridy = 1;
        commonConstraints.gridwidth = 2;
        commonForm.add(dnaparsTransversionCheckBox, commonConstraints);

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
        setInputType(inputType);
        WorkbenchStyles.applyPanelTreeBackground(this);
    }

    void setInputType(InputType inputType) {
        this.inputType = inputType;
        methodOverrideLabel.setText(inputType == InputType.PROTEIN ? "protpars menu overrides" : "dnapars menu overrides");
        dnaparsTransversionCheckBox.setVisible(inputType == InputType.DNA_CDS);
    }

    void apply(SimpleMethodConfig config) {
        enabledCheckBox.setSelected(config.enabled());
        outgroupSpinner.setValue(Integer.valueOf(inputType == InputType.PROTEIN
                ? (config.protparsOutgroupIndex() == null ? 0 : config.protparsOutgroupIndex().intValue())
                : (config.dnaparsOutgroupIndex() == null ? 0 : config.dnaparsOutgroupIndex().intValue())));
        dnaparsTransversionCheckBox.setSelected(config.dnaparsTransversionParsimony());
        methodOverrideArea.setText(TextListCodec.joinLines(inputType == InputType.PROTEIN
                ? config.protparsMenuOverrides()
                : config.dnaparsMenuOverrides()));
    }

    SimpleMethodConfig toConfig() {
        return new SimpleMethodConfig(
                enabledCheckBox.isSelected(),
                "F84",
                Double.valueOf(2.0d),
                true,
                "NJ",
                null,
                inputType == InputType.PROTEIN ? integerOrNull((Integer) outgroupSpinner.getValue()) : null,
                inputType == InputType.DNA_CDS ? integerOrNull((Integer) outgroupSpinner.getValue()) : null,
                inputType == InputType.DNA_CDS && dnaparsTransversionCheckBox.isSelected(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                inputType == InputType.PROTEIN ? TextListCodec.splitLines(methodOverrideArea.getText()) : java.util.List.of(),
                inputType == InputType.DNA_CDS ? TextListCodec.splitLines(methodOverrideArea.getText()) : java.util.List.of());
    }

    private static Integer integerOrNull(Integer value) {
        return value == null || value.intValue() <= 0 ? null : Integer.valueOf(value.intValue());
    }

}
