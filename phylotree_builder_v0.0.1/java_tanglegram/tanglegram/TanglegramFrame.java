package tanglegram;

import java.awt.BorderLayout;
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

final class TanglegramFrame extends JFrame {
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
        setSize(1400, 900);
        setLocationRelativeTo(null);
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
        menuBar.add(filesMenu);
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
}
