package onebuilder;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class TrimAlignmentPanel extends JPanel {
    private final JCheckBox enabledCheckBox;
    private final Map<TrimAlignmentPreset, JRadioButton> presetButtons;
    private final JTextArea customArgsArea;
    private final Runnable changeCallback;
    private boolean running;

    TrimAlignmentPanel(Runnable changeCallback) {
        super(new BorderLayout(0, 16));
        this.changeCallback = changeCallback == null ? () -> { } : changeCallback;
        this.presetButtons = new EnumMap<>(TrimAlignmentPreset.class);
        WorkbenchStyles.applyCanvas(this);

        JPanel header = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 6));
        header.add(WorkbenchStyles.createSectionTitle("Trim alignment"), BorderLayout.NORTH);
        header.add(
                WorkbenchStyles.createSubtitleLabel("Optionally trim alignment columns or sequences with trimAl before tree construction."),
                BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel form = WorkbenchStyles.createSurfacePanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 10, 10);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        enabledCheckBox = new JCheckBox("Run Trim alignment", false);
        enabledCheckBox.addActionListener(event -> {
            refreshEnabledStates();
            notifyChanged();
        });
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0d;
        form.add(enabledCheckBox, constraints);

        constraints.gridy = 1;
        form.add(WorkbenchStyles.createNoteArea(
                "When enabled, oneBuilder runs trimAl after MAFFT if alignment is selected, otherwise directly on the input MSA. The trimmed file is passed to all tree-building methods."),
                constraints);

        ButtonGroup group = new ButtonGroup();
        int row = 2;
        for (TrimAlignmentPreset preset : TrimAlignmentPreset.values()) {
            JRadioButton button = new JRadioButton(preset.toString());
            button.setOpaque(false);
            button.addActionListener(event -> {
                refreshEnabledStates();
                notifyChanged();
            });
            group.add(button);
            presetButtons.put(preset, button);

            constraints.gridx = 0;
            constraints.gridy = row++;
            constraints.gridwidth = 2;
            constraints.weightx = 1.0d;
            form.add(button, constraints);
        }
        presetButtons.get(TrimAlignmentPreset.GAP_THRESHOLD_CONSERVE).setSelected(true);

        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 1;
        constraints.weightx = 0.0d;
        form.add(new JLabel("Custom trimAl arguments"), constraints);

        customArgsArea = new JTextArea(5, 36);
        customArgsArea.setLineWrap(true);
        customArgsArea.setWrapStyleWord(true);
        customArgsArea.setText("-gappyout");
        customArgsArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                notifyChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                notifyChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                notifyChanged();
            }
        });
        constraints.gridx = 1;
        constraints.weightx = 1.0d;
        form.add(new JScrollPane(customArgsArea), constraints);

        add(form, BorderLayout.CENTER);
        refreshEnabledStates();
        WorkbenchStyles.applyPanelTreeBackground(this);
    }

    TrimAlignmentConfig toConfig() {
        TrimAlignmentPreset preset = selectedPreset();
        return new TrimAlignmentConfig(
                enabledCheckBox.isSelected(),
                preset,
                preset == TrimAlignmentPreset.CUSTOMIZED
                        ? TrimAlignmentConfig.splitCustomArgs(customArgsArea.getText())
                        : java.util.List.of());
    }

    void apply(TrimAlignmentConfig config) {
        TrimAlignmentConfig effectiveConfig = config == null ? TrimAlignmentConfig.defaults() : config;
        enabledCheckBox.setSelected(effectiveConfig.enabled());
        JRadioButton selectedButton = presetButtons.get(effectiveConfig.preset());
        if (selectedButton == null) {
            selectedButton = presetButtons.get(TrimAlignmentPreset.GAP_THRESHOLD_CONSERVE);
        }
        selectedButton.setSelected(true);
        if (!effectiveConfig.customArgs().isEmpty()) {
            customArgsArea.setText(String.join(" ", effectiveConfig.customArgs()));
        }
        refreshEnabledStates();
    }

    void setRunning(boolean running) {
        this.running = running;
        refreshEnabledStates();
    }

    boolean isEnabledForTest() {
        return enabledCheckBox.isSelected();
    }

    TrimAlignmentPreset selectedPresetForTest() {
        return selectedPreset();
    }

    boolean isCustomArgsEditableForTest() {
        return customArgsArea.isEditable();
    }

    String customArgsTextForTest() {
        return customArgsArea.getText();
    }

    void setEnabledForTest(boolean enabled) {
        if (enabledCheckBox.isSelected() != enabled) {
            enabledCheckBox.doClick();
        }
    }

    void selectPresetForTest(TrimAlignmentPreset preset) {
        JRadioButton button = presetButtons.get(preset);
        if (button != null && !button.isSelected()) {
            button.doClick();
        }
    }

    void setCustomArgsForTest(String customArgs) {
        customArgsArea.setText(customArgs == null ? "" : customArgs);
    }

    private TrimAlignmentPreset selectedPreset() {
        for (Map.Entry<TrimAlignmentPreset, JRadioButton> entry : presetButtons.entrySet()) {
            if (entry.getValue().isSelected()) {
                return entry.getKey();
            }
        }
        return TrimAlignmentPreset.GAP_THRESHOLD_CONSERVE;
    }

    private void refreshEnabledStates() {
        boolean trimEnabled = enabledCheckBox.isSelected();
        enabledCheckBox.setEnabled(!running);
        for (JRadioButton button : presetButtons.values()) {
            button.setEnabled(!running && trimEnabled);
        }
        boolean customEditable = !running && trimEnabled && selectedPreset() == TrimAlignmentPreset.CUSTOMIZED;
        customArgsArea.setEnabled(customEditable);
        customArgsArea.setEditable(customEditable);
    }

    private void notifyChanged() {
        changeCallback.run();
    }
}
