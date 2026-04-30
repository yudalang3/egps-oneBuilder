package onebuilder;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

final class RerootTreePanel extends JPanel {
    private final JComboBox<RerootMethod> methodCombo;
    private final JComboBox<LadderizeDirection> ladderizeDirectionCombo;
    private final JCheckBox sortByCladeSizeCheckBox;
    private final JCheckBox sortByBranchLengthCheckBox;

    RerootTreePanel() {
        super(new BorderLayout(0, 16));
        WorkbenchStyles.applyCanvas(this);

        JPanel header = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 6));
        header.add(WorkbenchStyles.createSectionTitle("Reroot Tree"), BorderLayout.NORTH);
        header.add(
                WorkbenchStyles.createSubtitleLabel("Choose how inferred trees are rerooted before ladderizing, visualization, and tree-distance analysis."),
                BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel form = WorkbenchStyles.createSurfacePanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 12, 12);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        form.add(new JLabel("Reroot method"), constraints);

        constraints.gridx = 1;
        methodCombo = new JComboBox<>(RerootMethod.values());
        methodCombo.setSelectedItem(RerootMethod.MAD);
        form.add(methodCombo, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0d;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        form.add(WorkbenchStyles.createNoteArea(
                "MAD uses the bundled MAD executable. root-at-middle-point uses the eGPS Java tree operator from egps-base to root at the midpoint of the longest leaf-to-leaf path."),
                constraints);

        constraints.gridy = 2;
        constraints.insets = new Insets(4, 0, 12, 0);
        form.add(new JSeparator(), constraints);

        constraints.gridy = 3;
        constraints.insets = new Insets(0, 0, 12, 12);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0d;
        constraints.fill = GridBagConstraints.NONE;
        form.add(new JLabel("ladderization rules"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0d;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        ladderizeDirectionCombo = new JComboBox<>(LadderizeDirection.values());
        ladderizeDirectionCombo.setSelectedItem(LadderizeDirection.UP);
        form.add(ladderizeDirectionCombo, constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0d;
        sortByCladeSizeCheckBox = new JCheckBox("Sorting with clade size", true);
        sortByCladeSizeCheckBox.setEnabled(false);
        form.add(sortByCladeSizeCheckBox, constraints);

        constraints.gridy = 5;
        sortByBranchLengthCheckBox = new JCheckBox("Sorting with branch length", true);
        sortByBranchLengthCheckBox.setEnabled(false);
        form.add(sortByBranchLengthCheckBox, constraints);

        add(form, BorderLayout.CENTER);
    }

    void apply(RerootConfig config) {
        RerootConfig effectiveConfig = config == null ? RerootConfig.defaults() : config;
        methodCombo.setSelectedItem(effectiveConfig.method());
        ladderizeDirectionCombo.setSelectedItem(effectiveConfig.ladderizeDirection());
        sortByCladeSizeCheckBox.setSelected(effectiveConfig.sortByCladeSize());
        sortByBranchLengthCheckBox.setSelected(effectiveConfig.sortByBranchLength());
    }

    RerootConfig toConfig() {
        Object selected = methodCombo.getSelectedItem();
        Object selectedDirection = ladderizeDirectionCombo.getSelectedItem();
        return new RerootConfig(
                selected instanceof RerootMethod ? (RerootMethod) selected : RerootMethod.MAD,
                selectedDirection instanceof LadderizeDirection ? (LadderizeDirection) selectedDirection : LadderizeDirection.UP,
                sortByCladeSizeCheckBox.isSelected(),
                sortByBranchLengthCheckBox.isSelected());
    }

    void setRunning(boolean running) {
        methodCombo.setEnabled(!running);
        ladderizeDirectionCombo.setEnabled(!running);
    }
}
