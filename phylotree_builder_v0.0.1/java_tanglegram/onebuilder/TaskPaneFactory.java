package onebuilder;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

final class TaskPaneFactory {
    private TaskPaneFactory() {
    }

    static JPanel createBlueTaskPane(String title, JPanel content, boolean collapsedByDefault) {
        JXTaskPane taskPane = new JXTaskPane();
        taskPane.setTitle(title);
        taskPane.setCollapsed(collapsedByDefault);
        taskPane.setAnimated(true);
        taskPane.setSpecial(true);
        taskPane.setOpaque(false);
        taskPane.add(content);

        JXTaskPaneContainer container = new JXTaskPaneContainer();
        container.setOpaque(false);
        container.setBackgroundPainter(null);
        container.add(taskPane);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(container, BorderLayout.CENTER);
        return wrapper;
    }
}
