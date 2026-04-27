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
import javax.swing.JSeparator;
import javax.swing.JTextField;

final class ProteinStructurePanel extends JPanel {
    private final JCheckBox enabledCheckBox;
    private final JCheckBox useStructureManifestCheckBox;
    private final JTextField structureManifestField;
    private final JButton browseButton;
    private final JRadioButton njTreeBuilderButton;
    private final JRadioButton swiftNjTreeBuilderButton;
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
        formPanel.add(new JSeparator(), constraints);

        constraints.gridy = 4;
        formPanel.add(new JLabel("<html>After Foldseek produces a pair-wise distance matrix, eGPS will build a "
                + "structure-similarity tree with the selected NJ method.</html>"), constraints);

        JPanel treeBuilderPanel = new JPanel(new GridBagLayout());
        treeBuilderPanel.setOpaque(false);
        GridBagConstraints radioConstraints = new GridBagConstraints();
        radioConstraints.insets = new Insets(0, 0, 0, 12);
        radioConstraints.anchor = GridBagConstraints.WEST;
        radioConstraints.gridx = 0;
        treeBuilderPanel.add(new JLabel("Structure tree builder"), radioConstraints);
        radioConstraints.gridx = 1;
        treeBuilderPanel.add(njTreeBuilderButton, radioConstraints);
        radioConstraints.gridx = 2;
        treeBuilderPanel.add(swiftNjTreeBuilderButton, radioConstraints);

        constraints.gridy = 5;
        formPanel.add(treeBuilderPanel, constraints);

        add(formPanel, BorderLayout.NORTH);
        add(WorkbenchStyles.createNoteArea(
                "When no TSV is selected, Foldseek uses the input FASTA with ProstT5/3Di for structure-like similarity. "
                        + "When a TSV is selected, each non-comment row must contain: sequence_id<TAB>structure_file. "
                        + "Lines beginning with # are ignored, and relative structure paths are resolved from the TSV file location."),
                BorderLayout.CENTER);

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
        updateControlState();
    }

    ProteinStructureConfig toConfig() {
        return new ProteinStructureConfig(
                inputType == InputType.PROTEIN && enabledCheckBox.isSelected(),
                useStructureManifestCheckBox.isSelected(),
                structureManifestField.getText(),
                swiftNjTreeBuilderButton.isSelected() ? "SwiftNJ" : "NJ");
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
    }
}
