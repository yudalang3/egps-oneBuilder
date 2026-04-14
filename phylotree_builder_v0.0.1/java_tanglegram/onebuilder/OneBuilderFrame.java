package onebuilder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import tanglegram.PreferenceAware;
import tanglegram.PreferenceDialog;
import tanglegram.UiPreferenceStore;
import tanglegram.UiPreferences;

final class OneBuilderFrame extends JFrame implements PreferenceAware {
    private static final String WINDOW_KEY = "onebuilder";
    private static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(1520, 960);
    private final OneBuilderWorkspacePanel workspacePanel;

    OneBuilderFrame(Path scriptDirectory) {
        super("eGPS oneBuilder");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        workspacePanel = new OneBuilderWorkspacePanel(scriptDirectory);
        setJMenuBar(createMenuBar());
        add(workspacePanel, BorderLayout.CENTER);
        setSize(UiPreferenceStore.resolveWindowSize(WINDOW_KEY, DEFAULT_WINDOW_SIZE));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                UiPreferenceStore.saveWindowSize(WINDOW_KEY, getSize());
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu preferenceMenu = new JMenu("Preference");
        JMenuItem settingsItem = new JMenuItem("Settings...");
        settingsItem.addActionListener(event -> PreferenceDialog.showDialog(this));
        preferenceMenu.add(settingsItem);
        menuBar.add(preferenceMenu);
        return menuBar;
    }

    @Override
    public void applyPreferences(UiPreferences preferences) {
        if (preferences.restoreLastWindowSize()) {
            UiPreferenceStore.saveWindowSize(WINDOW_KEY, getSize());
        }
        workspacePanel.applyPreferences(preferences);
    }
}
