package onebuilder;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

final class RerootTreePanel extends JPanel {
    private final JComboBox<RerootMethod> methodCombo;

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

        add(form, BorderLayout.CENTER);
    }

    void apply(RerootConfig config) {
        methodCombo.setSelectedItem((config == null ? RerootConfig.defaults() : config).method());
    }

    RerootConfig toConfig() {
        Object selected = methodCombo.getSelectedItem();
        return new RerootConfig(selected instanceof RerootMethod ? (RerootMethod) selected : RerootMethod.MAD);
    }

    void setRunning(boolean running) {
        methodCombo.setEnabled(!running);
    }
}
