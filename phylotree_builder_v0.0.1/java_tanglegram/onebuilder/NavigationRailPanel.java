package onebuilder;

import java.awt.Component;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JToggleButton;

final class NavigationRailPanel extends JComponent {
    private final Map<WorkspaceSection, JToggleButton> buttons;

    NavigationRailPanel(Consumer<WorkspaceSection> selectionListener) {
        buttons = new EnumMap<>(WorkspaceSection.class);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
        setBackground(WorkbenchStyles.CANVAS_BACKGROUND);
        setBorder(WorkbenchStyles.PAGE_PADDING);

        ButtonGroup group = new ButtonGroup();
        for (WorkspaceSection section : WorkspaceSection.values()) {
            JToggleButton button = new JToggleButton(section.label());
            WorkbenchStyles.styleRailButton(button);
            button.addActionListener(event -> selectionListener.accept(section));
            group.add(button);
            buttons.put(section, button);
            add(button);
            add(Box.createVerticalStrut(8));
        }
        add(Box.createVerticalGlue());
    }

    void setSectionEnabled(WorkspaceSection section, boolean enabled) {
        buttons.get(section).setEnabled(enabled);
    }

    void select(WorkspaceSection section) {
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
                return entry.getValue().isEnabled();
            }
        }
        return false;
    }

    private void refreshSelectionStyles(WorkspaceSection selectedSection) {
        for (Map.Entry<WorkspaceSection, JToggleButton> entry : buttons.entrySet()) {
            JToggleButton button = entry.getValue();
            boolean selected = entry.getKey() == selectedSection;
            button.setBackground(selected ? WorkbenchStyles.ACCENT_SOFT : WorkbenchStyles.CANVAS_BACKGROUND);
            button.setForeground(selected ? WorkbenchStyles.ACCENT : WorkbenchStyles.TEXT_PRIMARY);
            button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(
                            selected ? WorkbenchStyles.ACCENT_BORDER : WorkbenchStyles.CANVAS_BACKGROUND,
                            1,
                            true),
                    javax.swing.BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        }
    }
}
