package onebuilder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.jdesktop.swingx.JXCollapsiblePane;

final class LogDrawerPanel extends JPanel {
    private final JLabel stageValue;
    private final JButton toggleButton;
    private final JXCollapsiblePane collapsiblePane;
    private final JTextArea logArea;
    private final JProgressBar progressBar;
    private Consumer<Boolean> collapseStateListener;

    LogDrawerPanel() {
        super(new BorderLayout(0, 10));
        setOpaque(false);

        JPanel header = WorkbenchStyles.createSurfacePanel(new BorderLayout(12, 0));
        JLabel title = WorkbenchStyles.createSectionTitle("Execution Log");
        JLabel subtitle = WorkbenchStyles.createSubtitleLabel("Keep stdout/stderr out of the main work area until you need it.");

        JPanel titleBlock = new JPanel(new BorderLayout(0, 4));
        titleBlock.setOpaque(false);
        titleBlock.add(title, BorderLayout.NORTH);
        titleBlock.add(subtitle, BorderLayout.CENTER);

        stageValue = WorkbenchStyles.createStatusChip("Idle");
        toggleButton = new JButton("Show log");
        toggleButton.setFocusPainted(false);

        JPanel rightActions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);
        rightActions.add(stageValue);
        rightActions.add(toggleButton);

        header.add(titleBlock, BorderLayout.CENTER);
        header.add(rightActions, BorderLayout.EAST);

        collapsiblePane = new JXCollapsiblePane();
        collapsiblePane.setAnimated(true);
        collapsiblePane.setCollapsed(true);

        JPanel drawerContent = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 10));
        drawerContent.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WorkbenchStyles.ACCENT_BORDER, 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        logArea = new JTextArea(12, 80);
        logArea.setEditable(false);
        WorkbenchStyles.styleMonospaceLog(logArea);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Idle");

        drawerContent.add(new JScrollPane(logArea), BorderLayout.CENTER);
        drawerContent.add(progressBar, BorderLayout.SOUTH);
        drawerContent.setPreferredSize(new Dimension(100, 260));

        collapsiblePane.add(drawerContent);
        toggleButton.addActionListener(event -> setCollapsed(!collapsiblePane.isCollapsed()));

        add(header, BorderLayout.NORTH);
        add(collapsiblePane, BorderLayout.CENTER);
    }

    JTextArea logArea() {
        return logArea;
    }

    void setProgressText(String text) {
        progressBar.setString(text == null || text.isBlank() ? "Idle" : text);
    }

    void setProgressRunning(boolean running) {
        progressBar.setIndeterminate(running);
    }

    void setStageText(String text) {
        WorkbenchStyles.updateStatusChip(stageValue, text == null || text.isBlank() ? "Idle" : text);
    }

    boolean isCollapsed() {
        return collapsiblePane.isCollapsed();
    }

    void setCollapsed(boolean collapsed) {
        collapsiblePane.setCollapsed(collapsed);
        toggleButton.setText(collapsed ? "Show log" : "Hide log");
        if (collapseStateListener != null) {
            collapseStateListener.accept(Boolean.valueOf(collapsed));
        }
    }

    void setCollapseStateListener(Consumer<Boolean> collapseStateListener) {
        this.collapseStateListener = collapseStateListener;
    }
}
