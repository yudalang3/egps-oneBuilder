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
import javax.swing.JTextArea;

final class SimpleMethodPanel extends JPanel {
    private final JCheckBox enabledCheckBox;
    private final JLabel statusValue;
    private final JLabel outputValue;

    SimpleMethodPanel(String title, String description) {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D() + 1f));
        enabledCheckBox = new JCheckBox("Enable", true);

        JPanel header = new JPanel(new BorderLayout());
        header.add(titleLabel, BorderLayout.WEST);
        header.add(enabledCheckBox, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JTextArea descriptionArea = new JTextArea(description);
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setOpaque(false);
        descriptionArea.setBorder(null);
        add(descriptionArea, BorderLayout.CENTER);

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

        add(statusPanel, BorderLayout.SOUTH);
    }

    void apply(SimpleMethodConfig config) {
        enabledCheckBox.setSelected(config.enabled());
    }

    SimpleMethodConfig toConfig() {
        return new SimpleMethodConfig(enabledCheckBox.isSelected());
    }

    void setStatusText(String statusText) {
        statusValue.setText(statusText == null ? "-" : statusText);
    }

    void setOutputPath(Path outputPath) {
        outputValue.setText(outputPath == null ? "-" : outputPath.toString());
    }
}
