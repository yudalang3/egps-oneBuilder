package tanglegram;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

final class TanglegramVisualPropertiesDialog extends JDialog {
    private final JSpinner connectorGapSpinner;
    private final JSpinner connectorWidthSpinner;
    private final JSpinner connectorDashLengthSpinner;
    private final JSpinner connectorDashGapSpinner;
    private final JCheckBox showLeafLabelsCheckBox;
    private final JComboBox<String> fontFamilyCombo;
    private final JComboBox<FontStyleOption> fontStyleCombo;
    private final JSpinner fontSizeSpinner;
    private final Consumer<TanglegramRenderOptions> applyCallback;
    private final TanglegramRenderOptions baseOptions;

    private TanglegramVisualPropertiesDialog(
            Window owner,
            TanglegramRenderOptions currentOptions,
            Consumer<TanglegramRenderOptions> applyCallback) {
        super(owner, UiText.text("Visual properties", "可视化属性"), Dialog.ModalityType.DOCUMENT_MODAL);
        this.applyCallback = applyCallback;
        this.baseOptions = currentOptions;
        setLayout(new BorderLayout(12, 12));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(UiText.text("Tanglegram view", "缠结图视图")),
                BorderFactory.createEmptyBorder(10, 10, 8, 10)));
        GridBagConstraints constraints = baseConstraints();

        connectorGapSpinner = new JSpinner(new SpinnerNumberModel(currentOptions.connectorGap(), 80, 420, 10));
        connectorWidthSpinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(currentOptions.connectorStrokeWidth()), Double.valueOf(0.5d), Double.valueOf(8.0d), Double.valueOf(0.1d)));
        connectorDashLengthSpinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(currentOptions.connectorDashLength()), Double.valueOf(1.0d), Double.valueOf(30.0d), Double.valueOf(0.5d)));
        connectorDashGapSpinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(currentOptions.connectorDashGap()), Double.valueOf(1.0d), Double.valueOf(30.0d), Double.valueOf(0.5d)));
        showLeafLabelsCheckBox = new JCheckBox(UiText.text("Show leaf labels", "显示叶节点标签"), currentOptions.showLeafLabels());
        fontFamilyCombo = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        fontFamilyCombo.setSelectedItem(currentOptions.labelFontFamily());
        fontStyleCombo = new JComboBox<>(new FontStyleOption[] {
                new FontStyleOption(java.awt.Font.PLAIN, UiText.text("Plain", "常规")),
                new FontStyleOption(java.awt.Font.BOLD, UiText.text("Bold", "粗体")),
                new FontStyleOption(java.awt.Font.ITALIC, UiText.text("Italic", "斜体")),
                new FontStyleOption(java.awt.Font.BOLD | java.awt.Font.ITALIC, UiText.text("Bold Italic", "粗斜体"))
        });
        fontStyleCombo.setSelectedItem(FontStyleOption.fromStyle(currentOptions.labelFontStyle()));
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(currentOptions.labelFontSize(), 8, 48, 1));

        addRow(formPanel, constraints, 0, UiText.text("Tree spacing", "树间距"), connectorGapSpinner);
        addRow(formPanel, constraints, 1, UiText.text("Dashed line width", "虚线宽度"), connectorWidthSpinner);
        addRow(formPanel, constraints, 2, UiText.text("Dash length", "虚线长度"), connectorDashLengthSpinner);
        addRow(formPanel, constraints, 3, UiText.text("Dash gap", "虚线间隔"), connectorDashGapSpinner);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        formPanel.add(showLeafLabelsCheckBox, constraints);
        constraints.gridwidth = 1;

        addRow(formPanel, constraints, 5, UiText.text("Leaf label font family", "叶节点字体"), fontFamilyCombo);
        addRow(formPanel, constraints, 6, UiText.text("Leaf label font style", "叶节点字形"), fontStyleCombo);
        addRow(formPanel, constraints, 7, UiText.text("Leaf label font size", "叶节点字号"), fontSizeSpinner);

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton(UiText.text("Apply", "应用"));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton(UiText.text("Cancel", "取消"));
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

        setPreferredSize(new java.awt.Dimension(620, 430));
        pack();
        setMinimumSize(new java.awt.Dimension(560, 380));
        setLocationRelativeTo(owner);
    }

    static void showDialog(
            Window owner,
            TanglegramRenderOptions currentOptions,
            Consumer<TanglegramRenderOptions> applyCallback) {
        new TanglegramVisualPropertiesDialog(owner, currentOptions, applyCallback).setVisible(true);
    }

    private void applyValues() {
        FontStyleOption selectedStyle = (FontStyleOption) fontStyleCombo.getSelectedItem();
        TanglegramRenderOptions updatedOptions = new TanglegramRenderOptions(
                ((Integer) fontSizeSpinner.getValue()).intValue(),
                String.valueOf(fontFamilyCombo.getSelectedItem()),
                selectedStyle == null ? java.awt.Font.PLAIN : selectedStyle.style(),
                showLeafLabelsCheckBox.isSelected(),
                baseOptions.horizontalPadding(),
                baseOptions.verticalPadding(),
                ((Integer) connectorGapSpinner.getValue()).intValue(),
                ((Double) connectorWidthSpinner.getValue()).floatValue(),
                ((Double) connectorDashLengthSpinner.getValue()).floatValue(),
                ((Double) connectorDashGapSpinner.getValue()).floatValue(),
                baseOptions.autoFit());
        applyCallback.accept(updatedOptions);
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

    private record FontStyleOption(int style, String label) {
        private static FontStyleOption fromStyle(int style) {
            if (style == java.awt.Font.BOLD) {
                return new FontStyleOption(java.awt.Font.BOLD, UiText.text("Bold", "粗体"));
            }
            if (style == java.awt.Font.ITALIC) {
                return new FontStyleOption(java.awt.Font.ITALIC, UiText.text("Italic", "斜体"));
            }
            if (style == (java.awt.Font.BOLD | java.awt.Font.ITALIC)) {
                return new FontStyleOption(java.awt.Font.BOLD | java.awt.Font.ITALIC, UiText.text("Bold Italic", "粗斜体"));
            }
            return new FontStyleOption(java.awt.Font.PLAIN, UiText.text("Plain", "常规"));
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
