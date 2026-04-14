package tanglegram;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public final class PreferenceDialog extends JDialog {
    private final JComboBox<String> fontFamilyCombo;
    private final JSpinner fontSizeSpinner;
    private final JCheckBox restoreWindowSizeCheckBox;
    private final JSpinner defaultTanglegramLabelSpinner;

    private PreferenceDialog(Frame owner) {
        super(owner, "Preferences", true);
        setLayout(new BorderLayout(12, 12));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 8, 8);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        fontFamilyCombo = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 48, 1));
        restoreWindowSizeCheckBox = new JCheckBox("Restore last window size", true);
        defaultTanglegramLabelSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 48, 1));

        addRow(formPanel, constraints, 0, "UI font family", fontFamilyCombo);
        addRow(formPanel, constraints, 1, "UI font size", fontSizeSpinner);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        formPanel.add(restoreWindowSizeCheckBox, constraints);
        constraints.gridwidth = 1;
        addRow(formPanel, constraints, 3, "Default tanglegram label font size", defaultTanglegramLabelSpinner);
        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton resetButton = new JButton("Reset to Defaults");
        JButton applyButton = new JButton("Apply");
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        resetButton.addActionListener(event -> populate(UiPreferenceStore.defaultPreferences()));
        applyButton.addActionListener(event -> applyAndKeepOpen());
        okButton.addActionListener(event -> {
            applyAndKeepOpen();
            dispose();
        });
        cancelButton.addActionListener(event -> dispose());

        buttonPanel.add(resetButton);
        buttonPanel.add(applyButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        populate(UiPreferenceStore.load());
        pack();
        setLocationRelativeTo(owner);
    }

    public static void showDialog(Frame owner) {
        new PreferenceDialog(owner).setVisible(true);
    }

    private void populate(UiPreferences preferences) {
        fontFamilyCombo.setSelectedItem(preferences.uiFontFamily());
        fontSizeSpinner.setValue(Integer.valueOf(preferences.uiFontSize()));
        restoreWindowSizeCheckBox.setSelected(preferences.restoreLastWindowSize());
        defaultTanglegramLabelSpinner.setValue(Integer.valueOf(preferences.defaultTanglegramLabelFontSize()));
    }

    private void applyAndKeepOpen() {
        UiPreferences preferences = new UiPreferences(
                String.valueOf(fontFamilyCombo.getSelectedItem()),
                ((Integer) fontSizeSpinner.getValue()).intValue(),
                restoreWindowSizeCheckBox.isSelected(),
                ((Integer) defaultTanglegramLabelSpinner.getValue()).intValue());
        UiPreferenceStore.save(preferences);
        GlobalUiPreferenceController.applyToOpenWindows(preferences);
    }

    private static void addRow(JPanel panel, GridBagConstraints constraints, int row, String label, java.awt.Component component) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0.0;
        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        panel.add(component, constraints);
    }
}
