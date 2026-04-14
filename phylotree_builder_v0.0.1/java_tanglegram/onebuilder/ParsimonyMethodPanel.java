package onebuilder;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class ParsimonyMethodPanel extends JPanel {
    private final JCheckBox enabledCheckBox;
    private final JLabel methodOverrideLabel;
    private final JTextArea methodOverrideArea;
    private JLabel statusValue;
    private JLabel outputValue;
    private InputType inputType;

    ParsimonyMethodPanel(InputType inputType) {
        super(new BorderLayout(12, 12));
        this.inputType = inputType;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titleLabel = new JLabel("Parsimony");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D() + 1f));
        enabledCheckBox = new JCheckBox("Enable", true);

        JPanel header = new JPanel(new BorderLayout());
        header.add(titleLabel, BorderLayout.WEST);
        header.add(enabledCheckBox, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JTextArea descriptionArea = new JTextArea(
                "Parsimony mode runs the existing PHYLIP parsimony workflow. Common usage is usually just enable or disable the method. Advanced users can provide raw PHYLIP menu responses below.");
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setOpaque(false);
        descriptionArea.setBorder(null);

        methodOverrideLabel = new JLabel();
        methodOverrideArea = new JTextArea(6, 28);

        JPanel advancedForm = new JPanel(new GridBagLayout());
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

        JTextArea advancedNote = new JTextArea(
                "One menu response per line. These values are sent directly to PHYLIP. If the last line is not Y, the pipeline appends Y automatically.");
        advancedNote.setEditable(false);
        advancedNote.setLineWrap(true);
        advancedNote.setWrapStyleWord(true);
        advancedNote.setOpaque(false);
        advancedNote.setBorder(null);

        JPanel advancedContent = new JPanel(new BorderLayout(0, 8));
        advancedContent.add(advancedForm, BorderLayout.NORTH);
        advancedContent.add(advancedNote, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.add(descriptionArea, BorderLayout.NORTH);
        centerPanel.add(new CollapsibleSectionPanel("Advanced Parameters", advancedContent, true), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
        add(buildStatusPanel(), BorderLayout.SOUTH);
        setInputType(inputType);
    }

    void setInputType(InputType inputType) {
        this.inputType = inputType;
        methodOverrideLabel.setText(inputType == InputType.PROTEIN ? "protpars menu overrides" : "dnapars menu overrides");
    }

    void apply(SimpleMethodConfig config) {
        enabledCheckBox.setSelected(config.enabled());
        methodOverrideArea.setText(TextListCodec.joinLines(inputType == InputType.PROTEIN
                ? config.protparsMenuOverrides()
                : config.dnaparsMenuOverrides()));
    }

    SimpleMethodConfig toConfig() {
        return new SimpleMethodConfig(
                enabledCheckBox.isSelected(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                inputType == InputType.PROTEIN ? TextListCodec.splitLines(methodOverrideArea.getText()) : java.util.List.of(),
                inputType == InputType.DNA_CDS ? TextListCodec.splitLines(methodOverrideArea.getText()) : java.util.List.of());
    }

    void setStatusText(String statusText) {
        statusValue.setText(statusText == null ? "-" : statusText);
    }

    void setOutputPath(Path outputPath) {
        outputValue.setText(outputPath == null ? "-" : outputPath.toString());
    }

    private JPanel buildStatusPanel() {
        JPanel statusPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 6, 8);
        statusPanel.add(new JLabel("Status"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        statusValue = new JLabel("Idle");
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
