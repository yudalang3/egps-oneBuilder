package tanglegram;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

final class TanglegramFrame extends JFrame implements PreferenceAware {
    private static final String WINDOW_KEY = "tanglegram";
    private static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(1400, 900);
    private static final Color CANVAS_BACKGROUND = new Color(243, 247, 252);
    private static final Color SURFACE_BACKGROUND = Color.WHITE;
    private static final Color ACCENT = new Color(39, 102, 191);
    private static final Color ACCENT_BORDER = new Color(193, 211, 242);
    private final TanglegramPanelFactory panelFactory;
    private final JTabbedPane tabs;
    private final JLabel summaryLabel;
    private Path lastOpenedTreeSummaryDir;
    private volatile long loadSequence;

    TanglegramFrame() {
        super("Tanglegram");
        this.panelFactory = new TanglegramPanelFactory();
        this.tabs = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
        this.summaryLabel = createSubtitleLabel("Open a tree_summary directory to load the six fixed pairwise comparisons.");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setJMenuBar(createMenuBar());
        getContentPane().setBackground(CANVAS_BACKGROUND);
        add(buildContentPanel(), BorderLayout.CENTER);
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
        final long requestId = ++loadSequence;
        summaryLabel.setText("Loading tree summary: " + treeSummaryDir + " ...");
        Thread loadThread = new Thread(() -> {
            try {
                TreeSummaryLoadResult loadResult = TreeSummaryLoader.load(treeSummaryDir);
                SwingUtilities.invokeLater(() -> applyLoadResult(requestId, treeSummaryDir, loadResult));
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> applyLoadFailure(requestId, exception));
            }
        }, "tanglegram-summary-loader");
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void applyLoadResult(long requestId, Path requestedDir, TreeSummaryLoadResult loadResult) {
        if (requestId != loadSequence) {
            return;
        }
        if (loadResult.resolvedTrees().size() < 2) {
            showError("At least two tree methods must be readable from " + requestedDir);
            return;
        }

        replaceTabs(loadResult.availablePairs());
        lastOpenedTreeSummaryDir = loadResult.treeSummaryDir();
        summaryLabel.setText("Loaded " + loadResult.availablePairs().size() + " comparisons from " + loadResult.treeSummaryDir());

        if (!loadResult.missingMethods().isEmpty()) {
            summaryLabel.setText("Loaded partial result from " + loadResult.treeSummaryDir()
                    + ". Missing: " + formatMethods(loadResult.missingMethods()));
            showWarning("Skipped methods: " + formatMethods(loadResult.missingMethods()));
        }
    }

    private void applyLoadFailure(long requestId, Exception exception) {
        if (requestId != loadSequence) {
            return;
        }
        showError(exception.getMessage());
    }

    private void replaceTabs(List<TreePairSpec> pairSpecs) {
        tabs.removeAll();
        for (TreePairSpec pairSpec : pairSpecs) {
            tabs.addTab(pairSpec.tabName(), new ResizableTanglegramView(pairSpec, panelFactory));
        }
        tabs.revalidate();
        tabs.repaint();
    }

    private JPanel buildContentPanel() {
        JPanel canvas = new JPanel(new BorderLayout(0, 16));
        canvas.setOpaque(true);
        canvas.setBackground(CANVAS_BACKGROUND);
        canvas.setBorder(new EmptyBorder(20, 20, 20, 20));
        canvas.add(buildHeaderCard(), BorderLayout.NORTH);

        JPanel tabCard = new JPanel(new BorderLayout());
        applyCard(tabCard);
        tabCard.add(tabs, BorderLayout.CENTER);
        canvas.add(tabCard, BorderLayout.CENTER);
        return canvas;
    }

    private JPanel buildHeaderCard() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        applyCard(header);

        JPanel textBlock = new JPanel(new BorderLayout(0, 4));
        textBlock.setOpaque(false);
        textBlock.add(createTitleLabel("Tanglegram Viewer"), BorderLayout.NORTH);
        textBlock.add(summaryLabel, BorderLayout.CENTER);

        JPanel chipPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        chipPanel.setOpaque(false);
        chipPanel.add(createChip("Files > Open"));
        chipPanel.add(createChip("6 fixed pairs"));

        header.add(textBlock, BorderLayout.CENTER);
        header.add(chipPanel, BorderLayout.EAST);
        return header;
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

    private static void applyCard(JPanel panel) {
        panel.setOpaque(true);
        panel.setBackground(SURFACE_BACKGROUND);
        Border border = new CompoundBorder(
                new LineBorder(ACCENT_BORDER, 1, true),
                new EmptyBorder(18, 18, 18, 18));
        panel.setBorder(border);
    }

    private static JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text);
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont != null) {
            label.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize2D() + 8f));
        }
        return label;
    }

    private static JLabel createSubtitleLabel(String text) {
        JLabel label = new JLabel(text);
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont != null) {
            label.setFont(baseFont.deriveFont(Font.PLAIN, Math.max(11f, baseFont.getSize2D() - 1f)));
        }
        label.setForeground(new Color(94, 112, 137));
        return label;
    }

    private static JLabel createChip(String text) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(new Color(231, 239, 252));
        label.setForeground(ACCENT);
        label.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_BORDER, 1, true),
                new EmptyBorder(4, 10, 4, 10)));
        return label;
    }
}
