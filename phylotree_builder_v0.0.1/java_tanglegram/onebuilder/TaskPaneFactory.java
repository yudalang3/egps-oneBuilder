package onebuilder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.UIManager;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

final class TaskPaneFactory {
    private TaskPaneFactory() {
    }

    static JPanel createBlueTaskPane(String title, JPanel content, boolean collapsedByDefault) {
        JXTaskPane taskPane = new JXTaskPane();
        Font labelFont = UIManager.getFont("Label.font");
        taskPane.setTitle(title);
        taskPane.setCollapsed(collapsedByDefault);
        taskPane.setAnimated(true);
        // Keep the default task-pane header treatment so the collapse arrow stays easy to see.
        taskPane.setSpecial(true);
        taskPane.setOpaque(false);
        if (labelFont != null) {
            taskPane.setFont(labelFont.deriveFont(Font.BOLD, Math.max(12f, labelFont.getSize2D())));
        }
        if (taskPane.getContentPane() instanceof JPanel panel) {
            panel.setOpaque(true);
            panel.setBackground(WorkbenchStyles.SURFACE_BACKGROUND);
            panel.setForeground(WorkbenchStyles.TEXT_PRIMARY);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(WorkbenchStyles.ACCENT_BORDER, 1, true),
                    BorderFactory.createEmptyBorder(8, 10, 10, 10)));
        }
        taskPane.add(content);

        JXTaskPaneContainer container = new JXTaskPaneContainer();
        container.setOpaque(true);
        container.setBackground(Color.WHITE);
        container.setBackgroundPainter(null);
        container.setBorder(null);
        container.add(taskPane);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        wrapper.add(container, BorderLayout.CENTER);
        return wrapper;
    }
}
