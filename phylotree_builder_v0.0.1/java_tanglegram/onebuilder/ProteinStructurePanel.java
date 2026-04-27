package onebuilder;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

final class ProteinStructurePanel extends JPanel {
    private final JCheckBox enabledCheckBox;
    private final JCheckBox useStructureManifestCheckBox;
    private final JTextField structureManifestField;
    private final JButton browseButton;
    private final JRadioButton njTreeBuilderButton;
    private final JRadioButton swiftNjTreeBuilderButton;
    private final JSpinner threadsSpinner;
    private final JSpinner sensitivitySpinner;
    private final JSpinner evalueSpinner;
    private final JSpinner maxSeqsSpinner;
    private final JSpinner coverageThresholdSpinner;
    private final JSpinner coverageModeSpinner;
    private final JSpinner alignmentTypeSpinner;
    private final JSpinner tmscoreThresholdSpinner;
    private final JCheckBox exhaustiveSearchCheckBox;
    private final JCheckBox exactTmscoreCheckBox;
    private final JCheckBox gpuCheckBox;
    private final JSpinner verbositySpinner;
    private final JTextArea extraArgsArea;
    private InputType inputType;

    ProteinStructurePanel(InputType inputType) {
        super(new BorderLayout(12, 12));
        this.inputType = inputType;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));

        enabledCheckBox = new JCheckBox("Enable Foldseek protein structure similarity", false);
        useStructureManifestCheckBox = new JCheckBox("Use protein structure mapping TSV", false);
        structureManifestField = new JTextField();
        browseButton = new JButton("Browse");
        browseButton.addActionListener(event -> browseForStructureManifest());
        njTreeBuilderButton = new JRadioButton("NJ", true);
        swiftNjTreeBuilderButton = new JRadioButton("Swift NJ", false);
        ButtonGroup treeBuilderGroup = new ButtonGroup();
        treeBuilderGroup.add(njTreeBuilderButton);
        treeBuilderGroup.add(swiftNjTreeBuilderButton);
        threadsSpinner = new JSpinner(new SpinnerNumberModel(ProteinStructureConfig.DEFAULT_THREADS, 0, 512, 1));
        sensitivitySpinner = new JSpinner(new SpinnerNumberModel(ProteinStructureConfig.DEFAULT_SENSITIVITY, 1.0d, 20.0d, 0.5d));
        evalueSpinner = new JSpinner(new SpinnerNumberModel(ProteinStructureConfig.DEFAULT_EVALUE, 0.0d, Double.MAX_VALUE, 1.0d));
        maxSeqsSpinner = new JSpinner(new SpinnerNumberModel(ProteinStructureConfig.DEFAULT_MAX_SEQS, 1, 10000000, 100));
        coverageThresholdSpinner = new JSpinner(new SpinnerNumberModel(ProteinStructureConfig.DEFAULT_COVERAGE_THRESHOLD, 0.0d, 1.0d, 0.05d));
        coverageModeSpinner = new JSpinner(new SpinnerNumberModel(ProteinStructureConfig.DEFAULT_COVERAGE_MODE, 0, 5, 1));
        alignmentTypeSpinner = new JSpinner(new SpinnerNumberModel(ProteinStructureConfig.DEFAULT_ALIGNMENT_TYPE, 0, 2, 1));
        tmscoreThresholdSpinner = new JSpinner(new SpinnerNumberModel(ProteinStructureConfig.DEFAULT_TMSCORE_THRESHOLD, 0.0d, 1.0d, 0.05d));
        exhaustiveSearchCheckBox = new JCheckBox("Use --exhaustive-search");
        exactTmscoreCheckBox = new JCheckBox("Use --exact-tmscore");
        gpuCheckBox = new JCheckBox("Use --gpu");
        verbositySpinner = new JSpinner(new SpinnerNumberModel(ProteinStructureConfig.DEFAULT_VERBOSITY, 0, 3, 1));
        extraArgsArea = new JTextArea(4, 28);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 8, 8);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.weightx = 1.0;
        formPanel.add(enabledCheckBox, constraints);

        constraints.gridy = 1;
        formPanel.add(useStructureManifestCheckBox, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Protein structure TSV"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        formPanel.add(structureManifestField, constraints);

        constraints.gridx = 2;
        constraints.weightx = 0.0;
        formPanel.add(browseButton, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 3;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(WorkbenchStyles.createNoteArea(
                "When no TSV is selected, Foldseek uses the input FASTA with ProstT5/3Di for structure-like similarity. "
                        + "When a TSV is selected, each non-comment row must contain: sequence_id<TAB>structure_file. "
                        + "Lines beginning with # are ignored, and relative structure paths are resolved from the TSV file location."),
                constraints);

        JPanel advancedForm = new JPanel(new GridBagLayout());
        advancedForm.setOpaque(false);
        GridBagConstraints advancedConstraints = new GridBagConstraints();
        advancedConstraints.insets = new Insets(0, 0, 8, 8);
        advancedConstraints.anchor = GridBagConstraints.WEST;
        advancedConstraints.fill = GridBagConstraints.HORIZONTAL;
        addAdvancedRow(advancedForm, advancedConstraints, 0, "Threads (--threads)", threadsSpinner);
        addAdvancedRow(advancedForm, advancedConstraints, 1, "Sensitivity (-s)", sensitivitySpinner);
        addAdvancedRow(advancedForm, advancedConstraints, 2, "E-value (-e)", evalueSpinner);
        addAdvancedRow(advancedForm, advancedConstraints, 3, "Max seqs (--max-seqs)", maxSeqsSpinner);
        addAdvancedRow(advancedForm, advancedConstraints, 4, "Coverage threshold (-c)", coverageThresholdSpinner);
        addAdvancedRow(advancedForm, advancedConstraints, 5, "Coverage mode (--cov-mode)", coverageModeSpinner);
        addAdvancedRow(advancedForm, advancedConstraints, 6, "Alignment type (--alignment-type)", alignmentTypeSpinner);
        addAdvancedRow(advancedForm, advancedConstraints, 7, "TM-score threshold", tmscoreThresholdSpinner);
        addAdvancedRow(advancedForm, advancedConstraints, 8, "Verbosity (-v)", verbositySpinner);

        advancedConstraints.gridx = 0;
        advancedConstraints.gridy = 9;
        advancedConstraints.gridwidth = 2;
        advancedForm.add(exhaustiveSearchCheckBox, advancedConstraints);
        advancedConstraints.gridy = 10;
        advancedForm.add(exactTmscoreCheckBox, advancedConstraints);
        advancedConstraints.gridy = 11;
        advancedForm.add(gpuCheckBox, advancedConstraints);

        advancedConstraints.gridy = 12;
        advancedConstraints.gridwidth = 1;
        advancedForm.add(new JLabel("Extra Foldseek args"), advancedConstraints);
        advancedConstraints.gridx = 1;
        advancedConstraints.weightx = 1.0;
        advancedConstraints.fill = GridBagConstraints.BOTH;
        advancedForm.add(new JScrollPane(extraArgsArea), advancedConstraints);

        JPanel advancedContent = new JPanel(new BorderLayout(0, 8));
        advancedContent.setOpaque(false);
        advancedContent.add(advancedForm, BorderLayout.NORTH);
        advancedContent.add(
                WorkbenchStyles.createNoteArea("Advanced fields map directly to Foldseek search flags. Extra args are appended last; enter one token per line."),
                BorderLayout.CENTER);

        constraints.gridy = 4;
        formPanel.add(TaskPaneFactory.createBlueTaskPane("Advanced Parameters", advancedContent, true), constraints);

        constraints.gridy = 5;
        formPanel.add(new JSeparator(), constraints);

        constraints.gridy = 6;
        formPanel.add(new JLabel("<html>After Foldseek produces a pair-wise distance matrix, eGPS will build a "
                + "structure-similarity tree with the selected method.</html>"), constraints);

        JPanel treeBuilderPanel = new JPanel(new GridBagLayout());
        treeBuilderPanel.setOpaque(false);
        GridBagConstraints radioConstraints = new GridBagConstraints();
        radioConstraints.anchor = GridBagConstraints.WEST;
        radioConstraints.insets = new Insets(0, 0, 0, 8);
        radioConstraints.gridx = 0;
        radioConstraints.gridy = 0;
        treeBuilderPanel.add(new JLabel("Structure tree builder"), radioConstraints);
        radioConstraints.gridx = 1;
        treeBuilderPanel.add(njTreeBuilderButton, radioConstraints);
        radioConstraints.gridy = 1;
        treeBuilderPanel.add(swiftNjTreeBuilderButton, radioConstraints);
        radioConstraints.gridx = 2;
        radioConstraints.gridy = 0;
        radioConstraints.gridheight = 2;
        radioConstraints.weightx = 1.0;
        radioConstraints.fill = GridBagConstraints.HORIZONTAL;
        JPanel horizontalFiller = new JPanel();
        horizontalFiller.setOpaque(false);
        treeBuilderPanel.add(horizontalFiller, radioConstraints);

        constraints.gridy = 7;
        formPanel.add(treeBuilderPanel, constraints);

        add(formPanel, BorderLayout.NORTH);

        enabledCheckBox.addActionListener(event -> updateControlState());
        useStructureManifestCheckBox.addActionListener(event -> updateControlState());
        setInputType(inputType);
        WorkbenchStyles.applyPanelTreeBackground(this);
    }

    void setInputType(InputType inputType) {
        this.inputType = inputType;
        updateControlState();
    }

    void apply(ProteinStructureConfig config) {
        ProteinStructureConfig safeConfig = config == null ? ProteinStructureConfig.defaults() : config;
        enabledCheckBox.setSelected(safeConfig.enabled());
        useStructureManifestCheckBox.setSelected(safeConfig.useStructureManifest());
        structureManifestField.setText(safeConfig.structureManifestFile() == null ? "" : safeConfig.structureManifestFile());
        if ("SwiftNJ".equals(safeConfig.treeBuilderMethod())) {
            swiftNjTreeBuilderButton.setSelected(true);
        } else {
            njTreeBuilderButton.setSelected(true);
        }
        threadsSpinner.setValue(Integer.valueOf(safeConfig.threads()));
        sensitivitySpinner.setValue(Double.valueOf(safeConfig.sensitivity()));
        evalueSpinner.setValue(Double.valueOf(safeConfig.evalue()));
        maxSeqsSpinner.setValue(Integer.valueOf(safeConfig.maxSeqs()));
        coverageThresholdSpinner.setValue(Double.valueOf(safeConfig.coverageThreshold()));
        coverageModeSpinner.setValue(Integer.valueOf(safeConfig.coverageMode()));
        alignmentTypeSpinner.setValue(Integer.valueOf(safeConfig.alignmentType()));
        tmscoreThresholdSpinner.setValue(Double.valueOf(safeConfig.tmscoreThreshold()));
        exhaustiveSearchCheckBox.setSelected(safeConfig.exhaustiveSearch());
        exactTmscoreCheckBox.setSelected(safeConfig.exactTmscore());
        gpuCheckBox.setSelected(safeConfig.gpu());
        verbositySpinner.setValue(Integer.valueOf(safeConfig.verbosity()));
        extraArgsArea.setText(TextListCodec.joinLines(safeConfig.extraArgs()));
        updateControlState();
    }

    ProteinStructureConfig toConfig() {
        return new ProteinStructureConfig(
                inputType == InputType.PROTEIN && enabledCheckBox.isSelected(),
                useStructureManifestCheckBox.isSelected(),
                structureManifestField.getText(),
                swiftNjTreeBuilderButton.isSelected() ? "SwiftNJ" : "NJ",
                ((Integer) threadsSpinner.getValue()).intValue(),
                ((Double) sensitivitySpinner.getValue()).doubleValue(),
                ((Double) evalueSpinner.getValue()).doubleValue(),
                ((Integer) maxSeqsSpinner.getValue()).intValue(),
                ((Double) coverageThresholdSpinner.getValue()).doubleValue(),
                ((Integer) coverageModeSpinner.getValue()).intValue(),
                ((Integer) alignmentTypeSpinner.getValue()).intValue(),
                ((Double) tmscoreThresholdSpinner.getValue()).doubleValue(),
                exhaustiveSearchCheckBox.isSelected(),
                exactTmscoreCheckBox.isSelected(),
                gpuCheckBox.isSelected(),
                ((Integer) verbositySpinner.getValue()).intValue(),
                TextListCodec.splitLines(extraArgsArea.getText()));
    }

    private void browseForStructureManifest() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int selection = chooser.showOpenDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            Path selectedPath = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
            structureManifestField.setText(selectedPath.toString());
        }
    }

    private void updateControlState() {
        boolean proteinInput = inputType == InputType.PROTEIN;
        boolean enabled = proteinInput && enabledCheckBox.isSelected();
        enabledCheckBox.setEnabled(proteinInput);
        useStructureManifestCheckBox.setEnabled(enabled);
        structureManifestField.setEnabled(enabled && useStructureManifestCheckBox.isSelected());
        browseButton.setEnabled(enabled && useStructureManifestCheckBox.isSelected());
        njTreeBuilderButton.setEnabled(enabled);
        swiftNjTreeBuilderButton.setEnabled(enabled);
        threadsSpinner.setEnabled(enabled);
        sensitivitySpinner.setEnabled(enabled);
        evalueSpinner.setEnabled(enabled);
        maxSeqsSpinner.setEnabled(enabled);
        coverageThresholdSpinner.setEnabled(enabled);
        coverageModeSpinner.setEnabled(enabled);
        alignmentTypeSpinner.setEnabled(enabled);
        tmscoreThresholdSpinner.setEnabled(enabled);
        exhaustiveSearchCheckBox.setEnabled(enabled);
        exactTmscoreCheckBox.setEnabled(enabled);
        gpuCheckBox.setEnabled(enabled);
        verbositySpinner.setEnabled(enabled);
        extraArgsArea.setEnabled(enabled);
        extraArgsArea.setEditable(enabled);
    }

    private static void addAdvancedRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            String label,
            java.awt.Component component) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        panel.add(component, constraints);
    }
}
