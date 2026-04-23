package onebuilder;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

final class NavigationRailPanel extends JComponent {
    private final Map<WorkspaceSection, JToggleButton> buttons;
    private final Map<WorkspaceSection, Boolean> enabledStates;
    private WorkspaceSection selectedSection;

    NavigationRailPanel(Function<WorkspaceSection, String> selectionHandler) {
        buttons = new EnumMap<>(WorkspaceSection.class);
        enabledStates = new EnumMap<>(WorkspaceSection.class);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
        setBackground(WorkbenchStyles.CANVAS_BACKGROUND);
        setBorder(WorkbenchStyles.PAGE_PADDING);

        ButtonGroup group = new ButtonGroup();
        for (WorkspaceSection section : WorkspaceSection.values()) {
            JToggleButton button = new JToggleButton(section.navigationLabel());
            WorkbenchStyles.styleRailButton(button);
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.addActionListener(event -> handleSectionClick(section, selectionHandler));
            group.add(button);
            buttons.put(section, button);
            enabledStates.put(section, Boolean.TRUE);
            add(button);
            add(Box.createVerticalStrut(8));
        }
        normalizeButtonSizes();
        add(Box.createVerticalGlue());
    }

    void setSectionEnabled(WorkspaceSection section, boolean enabled) {
        setSectionEnabled(section, enabled, null);
    }

    void setSectionEnabled(WorkspaceSection section, boolean enabled, String tooltipText) {
        enabledStates.put(section, Boolean.valueOf(enabled));
        JToggleButton button = buttons.get(section);
        if (button != null) {
            button.setEnabled(true);
            button.setToolTipText(enabled ? null : tooltipText);
        }
        refreshSelectionStyles(selectedSection);
    }

    void select(WorkspaceSection section) {
        selectedSection = section;
        JToggleButton button = buttons.get(section);
        if (button != null) {
            button.setSelected(true);
        }
        refreshSelectionStyles(section);
    }

    List<String> labels() {
        List<String> labels = new ArrayList<>();
        for (WorkspaceSection section : WorkspaceSection.values()) {
            labels.add(section.label());
        }
        return labels;
    }

    boolean isSectionEnabled(String label) {
        for (Map.Entry<WorkspaceSection, JToggleButton> entry : buttons.entrySet()) {
            if (entry.getKey().label().equals(label)) {
                return Boolean.TRUE.equals(enabledStates.get(entry.getKey()));
            }
        }
        return false;
    }

    void clickSection(String label) {
        JToggleButton button = buttonForLabel(label);
        if (button != null) {
            button.doClick();
        }
    }

    String toolTipText(String label) {
        JToggleButton button = buttonForLabel(label);
        return button == null ? null : button.getToolTipText();
    }

    private void handleSectionClick(WorkspaceSection section, Function<WorkspaceSection, String> selectionHandler) {
        String blockingMessage = selectionHandler.apply(section);
        if (blockingMessage == null || blockingMessage.isBlank()) {
            return;
        }
        restoreSelection();
        showBlockingTooltip(buttons.get(section), blockingMessage);
    }

    private void restoreSelection() {
        if (selectedSection != null) {
            select(selectedSection);
        }
    }

    private void showBlockingTooltip(JToggleButton button, String message) {
        if (button == null) {
            return;
        }
        button.setToolTipText(message);
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        int originalDelay = toolTipManager.getInitialDelay();
        toolTipManager.setInitialDelay(0);
        MouseEvent hoverEvent = new MouseEvent(
                button,
                MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0,
                Math.max(1, button.getWidth() / 2),
                Math.max(1, button.getHeight() / 2),
                0,
                false);
        toolTipManager.mouseMoved(hoverEvent);

        Timer restoreDelayTimer = new Timer(750, event -> toolTipManager.setInitialDelay(originalDelay));
        restoreDelayTimer.setRepeats(false);
        restoreDelayTimer.start();
    }

    private JToggleButton buttonForLabel(String label) {
        for (Map.Entry<WorkspaceSection, JToggleButton> entry : buttons.entrySet()) {
            if (entry.getKey().label().equals(label)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void refreshSelectionStyles(WorkspaceSection selectedSection) {
        for (Map.Entry<WorkspaceSection, JToggleButton> entry : buttons.entrySet()) {
            JToggleButton button = entry.getValue();
            boolean enabled = Boolean.TRUE.equals(enabledStates.get(entry.getKey()));
            boolean selected = entry.getKey() == selectedSection;
            button.setBackground(selected ? WorkbenchStyles.ACCENT_SOFT : WorkbenchStyles.CANVAS_BACKGROUND);
            button.setForeground(enabled
                    ? (selected ? WorkbenchStyles.ACCENT : WorkbenchStyles.TEXT_PRIMARY)
                    : WorkbenchStyles.TEXT_SECONDARY);
            button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(
                            selected || !enabled ? WorkbenchStyles.ACCENT_BORDER : WorkbenchStyles.CANVAS_BACKGROUND,
                            1,
                            true),
                    javax.swing.BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        }
    }

    private void normalizeButtonSizes() {
        int maxWidth = 0;
        int maxHeight = 0;
        for (JToggleButton button : buttons.values()) {
            Dimension preferredSize = button.getPreferredSize();
            maxWidth = Math.max(maxWidth, preferredSize.width);
            maxHeight = Math.max(maxHeight, preferredSize.height);
        }
        Dimension uniformSize = new Dimension(maxWidth + 24, maxHeight);
        for (JToggleButton button : buttons.values()) {
            button.setPreferredSize(uniformSize);
            button.setMinimumSize(uniformSize);
            button.setMaximumSize(uniformSize);
        }
    }
}
