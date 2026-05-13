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
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

final class ThreeDVisualPropertiesDialog extends JDialog {
    private final JSpinner metricsFontSizeSpinner;
    private final JButton metricsColorButton;
    private final JSpinner treeTitleFontSizeSpinner;
    private final JButton treeTitleColorButton;
    private final JSpinner treeLineThicknessSpinner;
    private final JButton treeLineColorButton;
    private final JSpinner leafLabelFontSizeSpinner;
    private final JButton leafLabelColorButton;
    private final JSpinner legendFontSizeSpinner;
    private final JButton legendColorButton;
    private final JSpinner scaleBarFontSizeSpinner;
    private final JSpinner scaleBarLineThicknessSpinner;
    private final JButton scaleBarColorButton;
    private final Consumer<ThreeDVisualOptions> applyCallback;
    private Color metricsColor;
    private Color treeTitleColor;
    private Color treeLineColor;
    private Color leafLabelColor;
    private Color legendColor;
    private Color scaleBarColor;

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
        metricsColor = safeOptions.metricsColor();
        metricsColorButton = colorButton(metricsColor, "Metrics color");
        treeTitleFontSizeSpinner = fontSizeSpinner(safeOptions.treeTitleFontSize());
        treeTitleColor = safeOptions.treeTitleColor();
        treeTitleColorButton = colorButton(treeTitleColor, "Tree title color");
        treeLineThicknessSpinner = new JSpinner(new SpinnerNumberModel(
                Double.valueOf(safeOptions.treeLineThickness()), Double.valueOf(0.5d), Double.valueOf(8.0d), Double.valueOf(0.1d)));
        treeLineColor = safeOptions.treeLineColor();
        treeLineColorButton = colorButton(treeLineColor, "Tree line color");
        leafLabelFontSizeSpinner = fontSizeSpinner(safeOptions.leafLabelFontSize());
        leafLabelColor = safeOptions.leafLabelColor();
        leafLabelColorButton = colorButton(leafLabelColor, "Leaf label color");
        legendFontSizeSpinner = fontSizeSpinner(safeOptions.legendFontSize());
        legendColor = safeOptions.legendColor();
        legendColorButton = colorButton(legendColor, "Legend color");
        scaleBarFontSizeSpinner = fontSizeSpinner(safeOptions.scaleBarFontSize());
        scaleBarLineThicknessSpinner = new JSpinner(new SpinnerNumberModel(
                Double.valueOf(safeOptions.scaleBarLineThickness()), Double.valueOf(0.5d), Double.valueOf(8.0d), Double.valueOf(0.1d)));
        scaleBarColor = safeOptions.scaleBarColor();
        scaleBarColorButton = colorButton(scaleBarColor, "Scale bar color");

        JLabel preferenceHint = new JLabel("Font family follows global Preferences; this dialog adjusts sizes only.");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        formPanel.add(preferenceHint, constraints);
        constraints.gridwidth = 1;

        addSection(formPanel, constraints, 1, "Metrics");
        addRow(formPanel, constraints, 2, "Top-left metrics font size", metricsFontSizeSpinner);
        addRow(formPanel, constraints, 3, "Top-left metrics color", metricsColorButton);
        addSection(formPanel, constraints, 4, "Tree");
        addRow(formPanel, constraints, 5, "Tree title font size", treeTitleFontSizeSpinner);
        addRow(formPanel, constraints, 6, "Tree title color", treeTitleColorButton);
        addRow(formPanel, constraints, 7, "Tree line thickness", treeLineThicknessSpinner);
        addRow(formPanel, constraints, 8, "Tree line color", treeLineColorButton);
        addSection(formPanel, constraints, 9, "Leaf labels");
        addRow(formPanel, constraints, 10, "Leaf label font size", leafLabelFontSizeSpinner);
        addRow(formPanel, constraints, 11, "Leaf label color", leafLabelColorButton);
        addSection(formPanel, constraints, 12, "Legend and scale");
        addRow(formPanel, constraints, 13, "Legend font size", legendFontSizeSpinner);
        addRow(formPanel, constraints, 14, "Legend color", legendColorButton);
        addRow(formPanel, constraints, 15, "Scale bar font size", scaleBarFontSizeSpinner);
        addRow(formPanel, constraints, 16, "Scale bar line thickness", scaleBarLineThicknessSpinner);
        addRow(formPanel, constraints, 17, "Scale bar color", scaleBarColorButton);

        add(formPanel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        setPreferredSize(new java.awt.Dimension(540, 620));
        pack();
        setMinimumSize(new java.awt.Dimension(500, 540));
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
                metricsColor,
                ((Integer) treeTitleFontSizeSpinner.getValue()).intValue(),
                treeTitleColor,
                ((Double) treeLineThicknessSpinner.getValue()).floatValue(),
                treeLineColor,
                ((Integer) leafLabelFontSizeSpinner.getValue()).intValue(),
                leafLabelColor,
                ((Integer) legendFontSizeSpinner.getValue()).intValue(),
                legendColor,
                ((Integer) scaleBarFontSizeSpinner.getValue()).intValue(),
                ((Double) scaleBarLineThicknessSpinner.getValue()).floatValue(),
                scaleBarColor));
    }

    private static JSpinner fontSizeSpinner(int value) {
        return new JSpinner(new SpinnerNumberModel(Integer.valueOf(value), Integer.valueOf(6), Integer.valueOf(72), Integer.valueOf(1)));
    }

    private JButton colorButton(Color initialColor, String title) {
        JButton button = new JButton("Pick color...");
        updateColorButton(button, initialColor);
        button.addActionListener(event -> {
            Color updatedColor = JColorChooser.showDialog(this, title, currentColorFor(title));
            if (updatedColor == null) {
                return;
            }
            setCurrentColor(title, updatedColor);
            updateColorButton(button, updatedColor);
        });
        return button;
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

    private static void addSection(JPanel panel, GridBagConstraints constraints, int row, String title) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 1;
        constraints.weightx = 1.0d;
        panel.add(new JLabel(title), constraints);

        constraints.gridx = 1;
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), constraints);
    }

    private Color currentColorFor(String title) {
        if ("Metrics color".equals(title)) {
            return metricsColor;
        }
        if ("Tree title color".equals(title)) {
            return treeTitleColor;
        }
        if ("Tree line color".equals(title)) {
            return treeLineColor;
        }
        if ("Leaf label color".equals(title)) {
            return leafLabelColor;
        }
        if ("Legend color".equals(title)) {
            return legendColor;
        }
        return scaleBarColor;
    }

    private void setCurrentColor(String title, Color color) {
        if ("Metrics color".equals(title)) {
            metricsColor = color;
        } else if ("Tree title color".equals(title)) {
            treeTitleColor = color;
        } else if ("Tree line color".equals(title)) {
            treeLineColor = color;
        } else if ("Leaf label color".equals(title)) {
            leafLabelColor = color;
        } else if ("Legend color".equals(title)) {
            legendColor = color;
        } else {
            scaleBarColor = color;
        }
    }

    private static void updateColorButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(contrastingTextColor(color));
    }

    private static Color contrastingTextColor(Color background) {
        int brightness = ((background.getRed() * 299) + (background.getGreen() * 587) + (background.getBlue() * 114)) / 1000;
        return brightness < 140 ? Color.WHITE : Color.BLACK;
    }
}
