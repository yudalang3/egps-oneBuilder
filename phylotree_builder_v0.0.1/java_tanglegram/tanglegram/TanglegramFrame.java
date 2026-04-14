package tanglegram;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

final class TanglegramFrame extends JFrame implements PreferenceAware {
    private static final String WINDOW_KEY = "tanglegram";
    private static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(1400, 900);
    private final TanglegramPanelFactory panelFactory;
    private final JTabbedPane tabs;
    private Path lastOpenedTreeSummaryDir;

    TanglegramFrame() {
        super("Tanglegram");
        this.panelFactory = new TanglegramPanelFactory();
        this.tabs = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setJMenuBar(createMenuBar());
        add(tabs, BorderLayout.CENTER);
        setSize(UiPreferenceStore.resolveWindowSize(WINDOW_KEY, DEFAULT_WINDOW_SIZE));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                UiPreferenceStore.saveWindowSize(WINDOW_KEY, getSize());
            }
        });
    }

    void handleStartup(LauncherOptions options) {
        if (options.startupError() != null) {
            showError(options.startupError());
            return;
        }

        options.treeSummaryDir().ifPresent(this::loadTreeSummary);
    }

    void loadTreeSummary(Path treeSummaryDir) {
        try {
            TreeSummaryLoadResult loadResult = TreeSummaryLoader.load(treeSummaryDir);
            if (loadResult.resolvedTrees().size() < 2) {
                showError("At least two tree methods must be readable from " + treeSummaryDir);
                return;
            }

            replaceTabs(loadResult.availablePairs());
            lastOpenedTreeSummaryDir = loadResult.treeSummaryDir();

            if (!loadResult.missingMethods().isEmpty()) {
                showWarning("Skipped methods: " + formatMethods(loadResult.missingMethods()));
            }
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void replaceTabs(List<TreePairSpec> pairSpecs) {
        tabs.removeAll();
        for (TreePairSpec pairSpec : pairSpecs) {
            tabs.addTab(pairSpec.tabName(), new ResizableTanglegramView(pairSpec, panelFactory));
        }
        tabs.revalidate();
        tabs.repaint();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu filesMenu = new JMenu("Files");
        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(event -> openTreeSummaryDirectory());
        filesMenu.add(openItem);
        JMenu preferenceMenu = new JMenu("Preference");
        JMenuItem settingsItem = new JMenuItem("Settings...");
        settingsItem.addActionListener(event -> PreferenceDialog.showDialog(this));
        preferenceMenu.add(settingsItem);
        menuBar.add(filesMenu);
        menuBar.add(preferenceMenu);
        return menuBar;
    }

    private void openTreeSummaryDirectory() {
        JFileChooser fileChooser = new JFileChooser(lastOpenedTreeSummaryDir == null ? null : lastOpenedTreeSummaryDir.toFile());
        fileChooser.setDialogTitle("Open tree_summary directory");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int selection = fileChooser.showOpenDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            loadTreeSummary(fileChooser.getSelectedFile().toPath());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Tanglegram", JOptionPane.ERROR_MESSAGE);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Tanglegram", JOptionPane.WARNING_MESSAGE);
    }

    private static String formatMethods(List<TreeMethod> methods) {
        List<String> labels = new ArrayList<>();
        for (TreeMethod method : methods) {
            labels.add(method.shortLabel());
        }
        return labels.toString();
    }

    @Override
    public void applyPreferences(UiPreferences preferences) {
        if (preferences.restoreLastWindowSize()) {
            UiPreferenceStore.saveWindowSize(WINDOW_KEY, getSize());
        }
        if (lastOpenedTreeSummaryDir != null) {
            loadTreeSummary(lastOpenedTreeSummaryDir);
        }
    }
}
