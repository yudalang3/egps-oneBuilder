package tanglegram;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

final class ThreeDVisualPropertiesDialog extends JDialog {
    private final JSpinner metricsFontSizeSpinner;
    private final JSpinner treeTitleFontSizeSpinner;
    private final JSpinner treeLineThicknessSpinner;
    private final JSpinner leafLabelFontSizeSpinner;
    private final JSpinner legendFontSizeSpinner;
    private final JSpinner scaleBarFontSizeSpinner;
    private final Consumer<ThreeDVisualOptions> applyCallback;

    private ThreeDVisualPropertiesDialog(
            Window owner,
            ThreeDVisualOptions currentOptions,
            Consumer<ThreeDVisualOptions> applyCallback) {
        super(owner, "Visual properties", Dialog.ModalityType.DOCUMENT_MODAL);
        this.applyCallback = applyCallback;
        ThreeDVisualOptions safeOptions = currentOptions == null ? ThreeDVisualOptions.defaults() : currentOptions;
        WindowIconSupport.apply(this);
        setLayout(new BorderLayout(12, 12));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("3D Alignment visual properties"),
                BorderFactory.createEmptyBorder(10, 10, 8, 10)));
        GridBagConstraints constraints = baseConstraints();

        metricsFontSizeSpinner = fontSizeSpinner(safeOptions.metricsFontSize());
        treeTitleFontSizeSpinner = fontSizeSpinner(safeOptions.treeTitleFontSize());
        treeLineThicknessSpinner = new JSpinner(new SpinnerNumberModel(
                Double.valueOf(safeOptions.treeLineThickness()), Double.valueOf(0.5d), Double.valueOf(8.0d), Double.valueOf(0.1d)));
        leafLabelFontSizeSpinner = fontSizeSpinner(safeOptions.leafLabelFontSize());
        legendFontSizeSpinner = fontSizeSpinner(safeOptions.legendFontSize());
        scaleBarFontSizeSpinner = fontSizeSpinner(safeOptions.scaleBarFontSize());

        JLabel preferenceHint = new JLabel("Font family follows global Preferences; this dialog adjusts sizes only.");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        formPanel.add(preferenceHint, constraints);
        constraints.gridwidth = 1;

        addRow(formPanel, constraints, 1, "Top-left metrics font size", metricsFontSizeSpinner);
        addRow(formPanel, constraints, 2, "Tree title font size", treeTitleFontSizeSpinner);
        addRow(formPanel, constraints, 3, "Tree line thickness", treeLineThicknessSpinner);
        addRow(formPanel, constraints, 4, "Leaf label font size", leafLabelFontSizeSpinner);
        addRow(formPanel, constraints, 5, "Legend font size", legendFontSizeSpinner);
        addRow(formPanel, constraints, 6, "Scale bar font size", scaleBarFontSizeSpinner);

        add(formPanel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        setPreferredSize(new java.awt.Dimension(500, 360));
        pack();
        setMinimumSize(new java.awt.Dimension(460, 330));
        setLocationRelativeTo(owner);
    }

    static void showDialog(
            Window owner,
            ThreeDVisualOptions currentOptions,
            Consumer<ThreeDVisualOptions> applyCallback) {
        new ThreeDVisualPropertiesDialog(owner, currentOptions, applyCallback).setVisible(true);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("Apply");
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        applyButton.addActionListener(event -> applyValues());
        okButton.addActionListener(event -> {
            applyValues();
            dispose();
        });
        cancelButton.addActionListener(event -> dispose());
        buttonPanel.add(applyButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        return buttonPanel;
    }

    private void applyValues() {
        applyCallback.accept(new ThreeDVisualOptions(
                ((Integer) metricsFontSizeSpinner.getValue()).intValue(),
                ((Integer) treeTitleFontSizeSpinner.getValue()).intValue(),
                ((Double) treeLineThicknessSpinner.getValue()).floatValue(),
                ((Integer) leafLabelFontSizeSpinner.getValue()).intValue(),
                ((Integer) legendFontSizeSpinner.getValue()).intValue(),
                ((Integer) scaleBarFontSizeSpinner.getValue()).intValue()));
    }

    private static JSpinner fontSizeSpinner(int value) {
        return new JSpinner(new SpinnerNumberModel(Integer.valueOf(value), Integer.valueOf(6), Integer.valueOf(72), Integer.valueOf(1)));
    }

    private static GridBagConstraints baseConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 8, 8);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        return constraints;
    }

    private static void addRow(JPanel panel, GridBagConstraints constraints, int row, String label, java.awt.Component component) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0.0d;
        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0d;
        panel.add(component, constraints);
    }
}
