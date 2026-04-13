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

final class BayesianPanel extends JPanel {
    private final JCheckBox enabledCheckBox;
    private final JComboBox<String> ratesCombo;
    private final JSpinner ngenSpinner;
    private final JSpinner samplefreqSpinner;
    private final JSpinner printfreqSpinner;
    private final JSpinner diagnfreqSpinner;
    private final JLabel proteinModelLabel;
    private final JTextField proteinModelField;
    private final JLabel nstLabel;
    private final JSpinner nstSpinner;
    private final JLabel statusValue;
    private final JLabel outputValue;

    BayesianPanel() {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titleLabel = new JLabel("Bayesian");
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
        formPanel.add(new JLabel("Rates"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        ratesCombo = new JComboBox<>(new String[] {"invgamma", "gamma", "equal"});
        formPanel.add(ratesCombo, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("ngen"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        ngenSpinner = new JSpinner(new SpinnerNumberModel(50000, 1000, 10000000, 1000));
        formPanel.add(ngenSpinner, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("samplefreq"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        samplefreqSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 1000000, 10));
        formPanel.add(samplefreqSpinner, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("printfreq"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        printfreqSpinner = new JSpinner(new SpinnerNumberModel(1000, 1, 1000000, 10));
        formPanel.add(printfreqSpinner, constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("diagnfreq"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        diagnfreqSpinner = new JSpinner(new SpinnerNumberModel(5000, 1, 1000000, 10));
        formPanel.add(diagnfreqSpinner, constraints);

        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.weightx = 0.0;
        proteinModelLabel = new JLabel("Protein prior");
        formPanel.add(proteinModelLabel, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        proteinModelField = new JTextField();
        formPanel.add(proteinModelField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.weightx = 0.0;
        nstLabel = new JLabel("nst");
        formPanel.add(nstLabel, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        nstSpinner = new JSpinner(new SpinnerNumberModel(6, 1, 6, 1));
        formPanel.add(nstSpinner, constraints);

        JTextArea note = new JTextArea(
                "This panel exposes the MrBayes settings that the GUI can safely bridge into the existing scripts and Python pipelines.");
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

    void apply(BayesianConfig config, InputType inputType) {
        enabledCheckBox.setSelected(config.enabled());
        ratesCombo.setSelectedItem(config.rates());
        ngenSpinner.setValue(Integer.valueOf(config.ngen()));
        samplefreqSpinner.setValue(Integer.valueOf(config.samplefreq()));
        printfreqSpinner.setValue(Integer.valueOf(config.printfreq()));
        diagnfreqSpinner.setValue(Integer.valueOf(config.diagnfreq()));
        proteinModelField.setText(config.proteinModelPrior() == null ? "" : config.proteinModelPrior());
        nstSpinner.setValue(Integer.valueOf(config.nst() == null ? 6 : config.nst().intValue()));
        setInputType(inputType);
    }

    BayesianConfig toConfig(InputType inputType) {
        if (inputType == InputType.PROTEIN) {
            return new BayesianConfig(
                    enabledCheckBox.isSelected(),
                    proteinModelField.getText().trim(),
                    String.valueOf(ratesCombo.getSelectedItem()).trim(),
                    ((Integer) ngenSpinner.getValue()).intValue(),
                    ((Integer) samplefreqSpinner.getValue()).intValue(),
                    ((Integer) printfreqSpinner.getValue()).intValue(),
                    ((Integer) diagnfreqSpinner.getValue()).intValue());
        }
        return new BayesianConfig(
                enabledCheckBox.isSelected(),
                null,
                String.valueOf(ratesCombo.getSelectedItem()).trim(),
                ((Integer) ngenSpinner.getValue()).intValue(),
                ((Integer) samplefreqSpinner.getValue()).intValue(),
                ((Integer) printfreqSpinner.getValue()).intValue(),
                ((Integer) diagnfreqSpinner.getValue()).intValue(),
                Integer.valueOf(((Integer) nstSpinner.getValue()).intValue()));
    }

    void setInputType(InputType inputType) {
        boolean protein = inputType == InputType.PROTEIN;
        proteinModelLabel.setVisible(protein);
        proteinModelField.setVisible(protein);
        nstLabel.setVisible(!protein);
        nstSpinner.setVisible(!protein);
        revalidate();
        repaint();
    }

    void setStatusText(String statusText) {
        statusValue.setText(statusText == null ? "-" : statusText);
    }

    void setOutputPath(Path outputPath) {
        outputValue.setText(outputPath == null ? "-" : outputPath.toString());
    }
}
