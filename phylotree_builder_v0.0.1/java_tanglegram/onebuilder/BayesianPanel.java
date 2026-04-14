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
    private final JSpinner nrunsSpinner;
    private final JSpinner nchainsSpinner;
    private final JSpinner tempSpinner;
    private final JCheckBox stopruleCheckBox;
    private final JSpinner stopvalSpinner;
    private final JSpinner burninSpinner;
    private final JSpinner burninfracSpinner;
    private final JCheckBox relburninCheckBox;
    private final JTextArea commandBlockArea;
    private JLabel statusValue;
    private JLabel outputValue;

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

        ratesCombo = new JComboBox<>(new String[] {"invgamma", "gamma", "equal", "propinv", "adgamma"});
        ngenSpinner = new JSpinner(new SpinnerNumberModel(50000, 1000, 100000000, 1000));
        samplefreqSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 1000000, 10));
        printfreqSpinner = new JSpinner(new SpinnerNumberModel(1000, 1, 1000000, 10));
        diagnfreqSpinner = new JSpinner(new SpinnerNumberModel(5000, 1, 1000000, 10));
        proteinModelLabel = new JLabel("Protein prior");
        proteinModelField = new JTextField();
        nstLabel = new JLabel("nst");
        nstSpinner = new JSpinner(new SpinnerNumberModel(6, 1, 6, 1));
        nrunsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 128, 1));
        nchainsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 128, 1));
        tempSpinner = new JSpinner(new SpinnerNumberModel(0.0d, 0.0d, 100.0d, 0.05d));
        stopruleCheckBox = new JCheckBox("Use stoprule");
        stopvalSpinner = new JSpinner(new SpinnerNumberModel(0.0d, 0.0d, 100.0d, 0.001d));
        burninSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100000000, 100));
        burninfracSpinner = new JSpinner(new SpinnerNumberModel(0.0d, 0.0d, 1.0d, 0.01d));
        relburninCheckBox = new JCheckBox("Use relburnin");
        commandBlockArea = new JTextArea(7, 28);

        JPanel commonForm = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = baseConstraints();
        addRow(commonForm, constraints, 0, "Rates", ratesCombo);
        addRow(commonForm, constraints, 1, "ngen", ngenSpinner);
        addRow(commonForm, constraints, 2, "samplefreq", samplefreqSpinner);
        addRow(commonForm, constraints, 3, "printfreq", printfreqSpinner);
        addRow(commonForm, constraints, 4, "diagnfreq", diagnfreqSpinner);
        addRow(commonForm, constraints, 5, proteinModelLabel, proteinModelField);
        addRow(commonForm, constraints, 6, nstLabel, nstSpinner);

        JTextArea note = new JTextArea(
                "Common MrBayes settings cover the standard GUI workflow. Protein mode uses aamodelpr; DNA/CDS mode uses nst.");
        note.setEditable(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setOpaque(false);
        note.setBorder(null);

        JPanel advancedForm = new JPanel(new GridBagLayout());
        GridBagConstraints advancedConstraints = baseConstraints();
        addRow(advancedForm, advancedConstraints, 0, "nruns", nrunsSpinner);
        addRow(advancedForm, advancedConstraints, 1, "nchains", nchainsSpinner);
        addRow(advancedForm, advancedConstraints, 2, "temp", tempSpinner);

        advancedConstraints.gridx = 0;
        advancedConstraints.gridy = 3;
        advancedConstraints.gridwidth = 2;
        advancedForm.add(stopruleCheckBox, advancedConstraints);

        advancedConstraints.gridy = 4;
        advancedConstraints.gridwidth = 1;
        advancedForm.add(new JLabel("stopval"), advancedConstraints);
        advancedConstraints.gridx = 1;
        advancedForm.add(stopvalSpinner, advancedConstraints);

        advancedConstraints.gridx = 0;
        advancedConstraints.gridy = 5;
        advancedForm.add(new JLabel("burnin"), advancedConstraints);
        advancedConstraints.gridx = 1;
        advancedForm.add(burninSpinner, advancedConstraints);

        advancedConstraints.gridx = 0;
        advancedConstraints.gridy = 6;
        advancedForm.add(new JLabel("burninfrac"), advancedConstraints);
        advancedConstraints.gridx = 1;
        advancedForm.add(burninfracSpinner, advancedConstraints);

        advancedConstraints.gridx = 0;
        advancedConstraints.gridy = 7;
        advancedConstraints.gridwidth = 2;
        advancedForm.add(relburninCheckBox, advancedConstraints);

        advancedConstraints.gridy = 8;
        advancedConstraints.gridwidth = 1;
        advancedForm.add(new JLabel("Command block"), advancedConstraints);
        advancedConstraints.gridx = 1;
        advancedConstraints.weightx = 1.0;
        advancedConstraints.fill = GridBagConstraints.BOTH;
        advancedForm.add(new JScrollPane(commandBlockArea), advancedConstraints);

        JTextArea advancedNote = new JTextArea(
                "If command_block is not empty, the pipeline treats it as the authoritative MrBayes command sequence. Enter one command per line.");
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

    void apply(BayesianConfig config, InputType inputType) {
        enabledCheckBox.setSelected(config.enabled());
        ratesCombo.setSelectedItem(config.rates());
        ngenSpinner.setValue(Integer.valueOf(config.ngen()));
        samplefreqSpinner.setValue(Integer.valueOf(config.samplefreq()));
        printfreqSpinner.setValue(Integer.valueOf(config.printfreq()));
        diagnfreqSpinner.setValue(Integer.valueOf(config.diagnfreq()));
        proteinModelField.setText(config.proteinModelPrior() == null ? "" : config.proteinModelPrior());
        nstSpinner.setValue(Integer.valueOf(config.nst() == null ? 6 : config.nst().intValue()));
        nrunsSpinner.setValue(Integer.valueOf(config.nruns() == null ? 0 : config.nruns().intValue()));
        nchainsSpinner.setValue(Integer.valueOf(config.nchains() == null ? 0 : config.nchains().intValue()));
        tempSpinner.setValue(Double.valueOf(config.temp() == null ? 0.0d : config.temp().doubleValue()));
        stopruleCheckBox.setSelected(Boolean.TRUE.equals(config.stoprule()));
        stopvalSpinner.setValue(Double.valueOf(config.stopval() == null ? 0.0d : config.stopval().doubleValue()));
        burninSpinner.setValue(Integer.valueOf(config.burnin() == null ? 0 : config.burnin().intValue()));
        burninfracSpinner.setValue(Double.valueOf(config.burninfrac() == null ? 0.0d : config.burninfrac().doubleValue()));
        relburninCheckBox.setSelected(Boolean.TRUE.equals(config.relburnin()));
        commandBlockArea.setText(TextListCodec.joinLines(config.commandBlock()));
        setInputType(inputType);
    }

    BayesianConfig toConfig(InputType inputType) {
        return new BayesianConfig(
                enabledCheckBox.isSelected(),
                inputType == InputType.PROTEIN ? blankToNull(proteinModelField.getText()) : null,
                String.valueOf(ratesCombo.getSelectedItem()).trim(),
                ((Integer) ngenSpinner.getValue()).intValue(),
                ((Integer) samplefreqSpinner.getValue()).intValue(),
                ((Integer) printfreqSpinner.getValue()).intValue(),
                ((Integer) diagnfreqSpinner.getValue()).intValue(),
                inputType == InputType.DNA_CDS ? Integer.valueOf(((Integer) nstSpinner.getValue()).intValue()) : null,
                integerOrNull((Integer) nrunsSpinner.getValue()),
                integerOrNull((Integer) nchainsSpinner.getValue()),
                doubleOrNull((Double) tempSpinner.getValue()),
                stopruleCheckBox.isSelected() ? Boolean.TRUE : null,
                doubleOrNull((Double) stopvalSpinner.getValue()),
                integerOrNull((Integer) burninSpinner.getValue()),
                doubleOrNull((Double) burninfracSpinner.getValue()),
                relburninCheckBox.isSelected() ? Boolean.TRUE : null,
                TextListCodec.splitLines(commandBlockArea.getText()));
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

    private static Double doubleOrNull(Double value) {
        return value == null || value.doubleValue() <= 0.0d ? null : Double.valueOf(value.doubleValue());
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
