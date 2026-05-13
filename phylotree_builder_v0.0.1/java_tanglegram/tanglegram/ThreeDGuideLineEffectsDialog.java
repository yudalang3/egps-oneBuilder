package tanglegram;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

final class ThreeDGuideLineEffectsDialog extends JDialog {
    private final JCheckBox showDashLineCheckBox;
    private final JSpinner strokeWidthSpinner;
    private final JSpinner dashLengthSpinner;
    private final JSpinner dashGapSpinner;
    private final JButton colorButton;
    private final Consumer<ThreeDGuideLineOptions> applyCallback;
    private Color selectedColor;

    private ThreeDGuideLineEffectsDialog(
            Window owner,
            ThreeDGuideLineOptions currentOptions,
            Consumer<ThreeDGuideLineOptions> applyCallback) {
        super(owner, "Base guide line effects", Dialog.ModalityType.DOCUMENT_MODAL);
        this.applyCallback = applyCallback;
        ThreeDGuideLineOptions safeOptions = currentOptions == null ? ThreeDGuideLineOptions.defaults() : currentOptions;
        selectedColor = safeOptions.color();
        WindowIconSupport.apply(this);
        setLayout(new BorderLayout(12, 12));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("3D Alignment base guide lines"),
                BorderFactory.createEmptyBorder(10, 10, 8, 10)));
        GridBagConstraints constraints = baseConstraints();

        showDashLineCheckBox = new JCheckBox("Show dash line", safeOptions.showDashLine());
        strokeWidthSpinner = new JSpinner(new SpinnerNumberModel(
                Double.valueOf(safeOptions.strokeWidth()), Double.valueOf(0.5d), Double.valueOf(8.0d), Double.valueOf(0.1d)));
        dashLengthSpinner = new JSpinner(new SpinnerNumberModel(
                Double.valueOf(safeOptions.dashLength()), Double.valueOf(1.0d), Double.valueOf(30.0d), Double.valueOf(0.5d)));
        dashGapSpinner = new JSpinner(new SpinnerNumberModel(
                Double.valueOf(safeOptions.dashGap()), Double.valueOf(1.0d), Double.valueOf(30.0d), Double.valueOf(0.5d)));
        colorButton = new JButton("Pick color...");
        colorButton.setBackground(selectedColor);
        colorButton.setForeground(contrastingTextColor(selectedColor));
        colorButton.addActionListener(event -> chooseColor());

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        formPanel.add(showDashLineCheckBox, constraints);
        constraints.gridwidth = 1;

        addRow(formPanel, constraints, 1, "Dashed line width", strokeWidthSpinner);
        addRow(formPanel, constraints, 2, "Dash length", dashLengthSpinner);
        addRow(formPanel, constraints, 3, "Dash gap", dashGapSpinner);
        addRow(formPanel, constraints, 4, "Guide line color", colorButton);

        add(formPanel, BorderLayout.CENTER);

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
        add(buttonPanel, BorderLayout.SOUTH);

        setPreferredSize(new java.awt.Dimension(460, 270));
        pack();
        setMinimumSize(new java.awt.Dimension(420, 240));
        setLocationRelativeTo(owner);
    }

    static void showDialog(
            Window owner,
            ThreeDGuideLineOptions currentOptions,
            Consumer<ThreeDGuideLineOptions> applyCallback) {
        new ThreeDGuideLineEffectsDialog(owner, currentOptions, applyCallback).setVisible(true);
    }

    private void applyValues() {
        applyCallback.accept(new ThreeDGuideLineOptions(
                showDashLineCheckBox.isSelected(),
                ((Double) strokeWidthSpinner.getValue()).floatValue(),
                ((Double) dashLengthSpinner.getValue()).floatValue(),
                ((Double) dashGapSpinner.getValue()).floatValue(),
                selectedColor));
    }

    private void chooseColor() {
        Color updatedColor = JColorChooser.showDialog(this, "Guide line color", selectedColor);
        if (updatedColor == null) {
            return;
        }
        selectedColor = updatedColor;
        colorButton.setBackground(selectedColor);
        colorButton.setForeground(contrastingTextColor(selectedColor));
    }

    private static Color contrastingTextColor(Color background) {
        int brightness = ((background.getRed() * 299) + (background.getGreen() * 587) + (background.getBlue() * 114)) / 1000;
        return brightness < 140 ? Color.WHITE : Color.BLACK;
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
