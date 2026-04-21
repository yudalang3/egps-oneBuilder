package tanglegram;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
    private final JCheckBox showWindowsOneBuilderWarningCheckBox;

    private PreferenceDialog(Frame owner) {
        super(owner, "Preferences", true);
        setLayout(new BorderLayout(12, 12));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));

        fontFamilyCombo = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 48, 1));
        restoreWindowSizeCheckBox = new JCheckBox("Restore last window size", true);
        defaultTanglegramLabelSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 48, 1));
        showWindowsOneBuilderWarningCheckBox = new JCheckBox("Show Windows oneBuilder startup warning", true);

        JPanel generalPanel = createSectionPanel("General");
        GridBagConstraints generalConstraints = baseConstraints();
        addRow(generalPanel, generalConstraints, 0, "UI font family", fontFamilyCombo);
        addRow(generalPanel, generalConstraints, 1, "UI font size", fontSizeSpinner);
        generalConstraints.gridx = 0;
        generalConstraints.gridy = 2;
        generalConstraints.gridwidth = 2;
        generalPanel.add(restoreWindowSizeCheckBox, generalConstraints);
        generalConstraints.gridwidth = 1;
        addRow(generalPanel, generalConstraints, 3, "Default tanglegram label font size", defaultTanglegramLabelSpinner);

        JPanel platformPanel = createSectionPanel("Platform Notices");
        GridBagConstraints platformConstraints = baseConstraints();
        platformConstraints.gridx = 0;
        platformConstraints.gridy = 0;
        platformConstraints.gridwidth = 2;
        platformPanel.add(showWindowsOneBuilderWarningCheckBox, platformConstraints);
        platformConstraints.gridy = 1;
        platformPanel.add(
                noteLabel("Show a startup warning on Windows to explain that oneBuilder can export configs there, but the pipeline itself still needs Linux to run."),
                platformConstraints);

        contentPanel.add(generalPanel);
        contentPanel.add(platformPanel);
        add(contentPanel, BorderLayout.CENTER);

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
        setPreferredSize(new Dimension(760, 420));
        pack();
        setMinimumSize(new Dimension(720, 380));
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
        showWindowsOneBuilderWarningCheckBox.setSelected(preferences.showWindowsOneBuilderWarning());
    }

    private void applyAndKeepOpen() {
        UiPreferences preferences = new UiPreferences(
                String.valueOf(fontFamilyCombo.getSelectedItem()),
                ((Integer) fontSizeSpinner.getValue()).intValue(),
                restoreWindowSizeCheckBox.isSelected(),
                ((Integer) defaultTanglegramLabelSpinner.getValue()).intValue(),
                showWindowsOneBuilderWarningCheckBox.isSelected());
        UiPreferenceStore.save(preferences);
        GlobalUiPreferenceController.applyToOpenWindows(preferences);
    }

    private static GridBagConstraints baseConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 8, 8);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        return constraints;
    }

    private static JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(10, 10, 6, 10)));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    private static JLabel noteLabel(String text) {
        return new JLabel("<html><body style='width:480px'>" + text + "</body></html>");
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
