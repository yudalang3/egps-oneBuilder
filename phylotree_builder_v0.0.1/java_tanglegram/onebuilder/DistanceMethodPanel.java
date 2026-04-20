package onebuilder;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class DistanceMethodPanel extends JPanel {
    private final JCheckBox enabledCheckBox;
    private final JLabel methodOverrideLabel;
    private final JTextArea methodOverrideArea;
    private final JTextArea neighborOverrideArea;
    private JLabel statusValue;
    private JLabel outputValue;
    private InputType inputType;

    DistanceMethodPanel(InputType inputType) {
        super(new BorderLayout(12, 12));
        this.inputType = inputType;
        setOpaque(false);

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

        methodOverrideLabel = new JLabel();
        methodOverrideArea = new JTextArea(5, 28);
        neighborOverrideArea = new JTextArea(4, 28);

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
        centerPanel.add(descriptionArea, BorderLayout.NORTH);
        centerPanel.add(TaskPaneFactory.createBlueTaskPane("Advanced Parameters", advancedContent, true), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
        add(buildStatusPanel(), BorderLayout.SOUTH);
        setInputType(inputType);
        WorkbenchStyles.applyPanelTreeBackground(this);
    }

    void setInputType(InputType inputType) {
        this.inputType = inputType;
        methodOverrideLabel.setText(inputType == InputType.PROTEIN ? "protdist menu overrides" : "dnadist menu overrides");
    }

    void apply(SimpleMethodConfig config) {
        enabledCheckBox.setSelected(config.enabled());
        methodOverrideArea.setText(TextListCodec.joinLines(inputType == InputType.PROTEIN
                ? config.protdistMenuOverrides()
                : config.dnadistMenuOverrides()));
        neighborOverrideArea.setText(TextListCodec.joinLines(config.neighborMenuOverrides()));
    }

    SimpleMethodConfig toConfig() {
        return new SimpleMethodConfig(
                enabledCheckBox.isSelected(),
                inputType == InputType.PROTEIN ? TextListCodec.splitLines(methodOverrideArea.getText()) : java.util.List.of(),
                inputType == InputType.DNA_CDS ? TextListCodec.splitLines(methodOverrideArea.getText()) : java.util.List.of(),
                TextListCodec.splitLines(neighborOverrideArea.getText()),
                java.util.List.of(),
                java.util.List.of());
    }

    void setStatusText(String statusText) {
        WorkbenchStyles.updateStatusChip(statusValue, statusText);
    }

    void setOutputPath(Path outputPath) {
        outputValue.setText(outputPath == null ? "-" : outputPath.toString());
    }

    private JPanel buildStatusPanel() {
        JPanel statusPanel = new JPanel(new GridBagLayout());
        statusPanel.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 6, 8);
        statusPanel.add(new JLabel("Status"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        statusValue = WorkbenchStyles.createStatusChip("Idle");
        statusPanel.add(statusValue, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        statusPanel.add(new JLabel("Output"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        outputValue = new JLabel("-");
        statusPanel.add(outputValue, constraints);
        return statusPanel;
    }
}
