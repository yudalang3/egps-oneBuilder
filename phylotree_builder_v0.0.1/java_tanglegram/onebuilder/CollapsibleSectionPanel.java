package onebuilder;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.jdesktop.swingx.JXCollapsiblePane;

final class CollapsibleSectionPanel extends JPanel {
    private final JButton toggleButton;
    private final JXCollapsiblePane collapsiblePane;
    private final String expandedLabel;
    private final String collapsedLabel;

    CollapsibleSectionPanel(String title, JPanel content, boolean collapsedByDefault) {
        super(new BorderLayout(0, 6));
        this.expandedLabel = "Hide " + title;
        this.collapsedLabel = "Show " + title;
        this.toggleButton = new JButton();
        this.collapsiblePane = new JXCollapsiblePane();
        this.collapsiblePane.setAnimated(false);
        this.collapsiblePane.getContentPane().setLayout(new BorderLayout());
        this.collapsiblePane.getContentPane().add(content, BorderLayout.CENTER);
        this.collapsiblePane.setCollapsed(collapsedByDefault);

        toggleButton.addActionListener(event -> {
            collapsiblePane.setCollapsed(!collapsiblePane.isCollapsed());
            syncButtonLabel();
        });
        syncButtonLabel();

        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        add(toggleButton, BorderLayout.NORTH);
        add(collapsiblePane, BorderLayout.CENTER);
    }

    private void syncButtonLabel() {
        toggleButton.setText(collapsiblePane.isCollapsed() ? collapsedLabel : expandedLabel);
    }
}
