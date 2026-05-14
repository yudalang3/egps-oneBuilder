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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

final class ThreeDGuideLineEffectsDialog extends JDialog {
    private final JRadioButton phylogramWithDashLineRadioButton;
    private final JRadioButton semiCladogramRadioButton;
    private final JCheckBox showLeafNamesCheckBox;
    private final JSpinner strokeWidthSpinner;
    private final JSpinner dashLengthSpinner;
    private final JSpinner dashGapSpinner;
    private final JButton colorButton;
    private final JLabel strokeWidthLabel;
    private final JLabel dashLengthLabel;
    private final JLabel dashGapLabel;
    private final JLabel colorLabel;
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

        showLeafNamesCheckBox = new JCheckBox("Show leaf names", safeOptions.showLeafNames());
        phylogramWithDashLineRadioButton = new JRadioButton("Phylogram with dash line", safeOptions.showDashLine());
        phylogramWithDashLineRadioButton.setToolTipText(
                "Use dashed base guide lines to show leaf projection from the phylogram tree tips to the shared base plane.");
        semiCladogramRadioButton = new JRadioButton("Semi-cladogram", !safeOptions.showDashLine());
        semiCladogramRadioButton.setToolTipText(
                "Use a simplified semi-cladogram style without dashed guide-line styling controls.");
        ButtonGroup guideLineModeGroup = new ButtonGroup();
        guideLineModeGroup.add(phylogramWithDashLineRadioButton);
        guideLineModeGroup.add(semiCladogramRadioButton);
        strokeWidthSpinner = new JSpinner(new SpinnerNumberModel(
                Double.valueOf(safeOptions.strokeWidth()), Double.valueOf(0.5d), Double.valueOf(8.0d), Double.valueOf(0.1d)));
        dashLengthSpinner = new JSpinner(new SpinnerNumberModel(
                Double.valueOf(safeOptions.dashLength()), Double.valueOf(1.0d), Double.valueOf(30.0d), Double.valueOf(0.5d)));
        dashGapSpinner = new JSpinner(new SpinnerNumberModel(
                Double.valueOf(safeOptions.dashGap()), Double.valueOf(1.0d), Double.valueOf(30.0d), Double.valueOf(0.5d)));
        strokeWidthLabel = new JLabel("Dashed line width");
        dashLengthLabel = new JLabel("Dash length");
        dashGapLabel = new JLabel("Dash gap");
        colorLabel = new JLabel("Guide line color");
        colorButton = new JButton("Pick color...");
        colorButton.setBackground(selectedColor);
        colorButton.setForeground(contrastingTextColor(selectedColor));
        colorButton.addActionListener(event -> chooseColor());
        phylogramWithDashLineRadioButton.addActionListener(event -> updateDashStylingControls());
        semiCladogramRadioButton.addActionListener(event -> updateDashStylingControls());

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        formPanel.add(showLeafNamesCheckBox, constraints);
        constraints.gridy = 1;
        formPanel.add(new JSeparator(SwingConstants.HORIZONTAL), constraints);
        constraints.gridy = 2;
        formPanel.add(phylogramWithDashLineRadioButton, constraints);
        constraints.gridy = 3;
        formPanel.add(semiCladogramRadioButton, constraints);
        constraints.gridwidth = 1;

        addRow(formPanel, constraints, 4, strokeWidthLabel, strokeWidthSpinner);
        addRow(formPanel, constraints, 5, dashLengthLabel, dashLengthSpinner);
        addRow(formPanel, constraints, 6, dashGapLabel, dashGapSpinner);
        addRow(formPanel, constraints, 7, colorLabel, colorButton);

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

        setPreferredSize(new java.awt.Dimension(560, 360));
        pack();
        setMinimumSize(new java.awt.Dimension(520, 320));
        updateDashStylingControls();
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
                phylogramWithDashLineRadioButton.isSelected(),
                showLeafNamesCheckBox.isSelected(),
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

    private void updateDashStylingControls() {
        boolean useDashStyling = phylogramWithDashLineRadioButton.isSelected();
        strokeWidthLabel.setEnabled(useDashStyling);
        strokeWidthSpinner.setEnabled(useDashStyling);
        dashLengthLabel.setEnabled(useDashStyling);
        dashLengthSpinner.setEnabled(useDashStyling);
        dashGapLabel.setEnabled(useDashStyling);
        dashGapSpinner.setEnabled(useDashStyling);
        colorLabel.setEnabled(useDashStyling);
        colorButton.setEnabled(useDashStyling);
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

    private static void addRow(JPanel panel, GridBagConstraints constraints, int row, JLabel label, java.awt.Component component) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0.0d;
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0d;
        panel.add(component, constraints);
    }
}
