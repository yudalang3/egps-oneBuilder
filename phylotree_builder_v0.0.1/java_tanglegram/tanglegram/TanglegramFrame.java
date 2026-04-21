package tanglegram;

import com.jidesoft.swing.JideTabbedPane;
import egps2.utils.common.util.SaveUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

final class TanglegramFrame extends JFrame implements PreferenceAware {
    private static final String WINDOW_KEY = "tanglegram";
    private static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(1400, 900);
    private static final Color CANVAS_BACKGROUND = new Color(243, 247, 252);
    private final JideTabbedPane workspaceTabs;
    private final TanglegramWelcomePanel welcomePanel;
    private final JButton exportButton;
    private final JButton propertiesButton;

    TanglegramFrame() {
        super("Tanglegram");
        this.workspaceTabs = new JideTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        this.welcomePanel = new TanglegramWelcomePanel(this::openImportedSession);
        this.exportButton = createExportButton();
        this.propertiesButton = createPropertiesButton();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
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
            return;
        }
        options.treeSummaryDir().ifPresent(welcomePanel::populateFromStartupTreeSummary);
    }

    private JPanel buildContentPanel() {
        JPanel canvas = new JPanel(new BorderLayout());
        canvas.setOpaque(true);
        canvas.setBackground(CANVAS_BACKGROUND);
        canvas.setBorder(new EmptyBorder(8, 8, 8, 8));
        workspaceTabs.setShowCloseButton(true);
        workspaceTabs.setShowCloseButtonOnTab(true);
        workspaceTabs.setTabShape(JideTabbedPane.SHAPE_OFFICE2003);
        workspaceTabs.setColorTheme(JideTabbedPane.COLOR_THEME_WINXP);
        workspaceTabs.setTabResizeMode(JideTabbedPane.RESIZE_MODE_FIT);
        workspaceTabs.addTab("Welcome", welcomePanel);
        workspaceTabs.setTabClosableAt(0, false);
        workspaceTabs.setBorder(BorderFactory.createEmptyBorder());
        workspaceTabs.setOpaque(false);
        workspaceTabs.setTabTrailingComponent(createTrailingControls());
        workspaceTabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    renameTabAt(event.getX(), event.getY());
                }
            }
        });
        canvas.add(workspaceTabs, BorderLayout.CENTER);
        return canvas;
    }

    @Override
    public void applyPreferences(UiPreferences preferences) {
        if (preferences.restoreLastWindowSize()) {
            UiPreferenceStore.saveWindowSize(WINDOW_KEY, getSize());
        }
        repaint();
    }

    private void openImportedSession(TanglegramWelcomePanel.LoadedImportSession session) {
        TanglegramResultTabPanel resultPanel = new TanglegramResultTabPanel(
                session.sourceName(),
                session.sourceKind(),
                session.importedTrees(),
                session.pairSpecs(),
                session.warnings(),
                () -> openThreeDAlignmentTab(session.sourceName(), session.importedTrees()));
        String tabTitle = "Imported: " + session.sourceName();
        workspaceTabs.addTab(tabTitle, resultPanel);
        int tabIndex = workspaceTabs.getTabCount() - 1;
        workspaceTabs.setTabClosableAt(tabIndex, true);
        workspaceTabs.setSelectedIndex(tabIndex);
    }

    private void openThreeDAlignmentTab(String sourceName, java.util.List<ImportedTreeSpec> importedTrees) {
        ThreeDTreeAlignmentView alignmentView = new ThreeDTreeAlignmentView(importedTrees);
        String tabTitle = "3D Alignment: " + sourceName;
        workspaceTabs.addTab(tabTitle, alignmentView);
        int tabIndex = workspaceTabs.getTabCount() - 1;
        workspaceTabs.setTabClosableAt(tabIndex, true);
        workspaceTabs.setSelectedIndex(tabIndex);
    }

    private JPanel createTrailingControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        panel.setOpaque(false);
        panel.add(exportButton);
        panel.add(propertiesButton);
        return panel;
    }

    private JButton createExportButton() {
        JButton button = new JButton("Export", loadExportIcon());
        button.setFocusable(false);
        button.setMargin(new Insets(0, 6, 0, 6));
        button.setToolTipText("Export the current view as bitmap or vector graphics");
        button.addActionListener(event -> exportCurrentView());
        return button;
    }

    private JButton createPropertiesButton() {
        JButton propertiesButton = new JButton("Preference", loadPropertiesIcon());
        propertiesButton.setFocusable(false);
        propertiesButton.setFocusPainted(false);
        propertiesButton.setMargin(new Insets(0, 6, 0, 6));
        propertiesButton.setToolTipText("Open global properties");
        propertiesButton.addActionListener(event -> PreferenceDialog.showDialog(this));
        return propertiesButton;
    }

    private void exportCurrentView() {
        ExportableView exportableView = currentExportableView();
        if (exportableView == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Nothing to export on the Welcome page. Open a result tab or a 3D Alignment tab first.",
                    "No Export Content",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!exportableView.canExport()) {
            JOptionPane.showMessageDialog(
                    this,
                    "The current view is still rendering or has no exportable content yet.",
                    "Export Unavailable",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new SaveUtil().saveData(exportableView.getExportComponent(), exportableView.getExportContextClass());
    }

    private ExportableView currentExportableView() {
        java.awt.Component selectedComponent = workspaceTabs.getSelectedComponent();
        if (selectedComponent instanceof ExportableView exportableView) {
            return exportableView;
        }
        return null;
    }

    private void renameTabAt(int x, int y) {
        int tabIndex = workspaceTabs.indexAtLocation(x, y);
        if (tabIndex <= 0 || tabIndex >= workspaceTabs.getTabCount()) {
            return;
        }
        String currentTitle = workspaceTabs.getTitleAt(tabIndex);
        String newTitle = JOptionPane.showInputDialog(this, "Rename tab", currentTitle);
        if (newTitle == null) {
            return;
        }
        String trimmedTitle = newTitle.trim();
        if (trimmedTitle.isEmpty()) {
            return;
        }
        workspaceTabs.setTitleAt(tabIndex, trimmedTitle);
    }

    private ImageIcon loadPropertiesIcon() {
        URL resource = getClass().getResource("/tanglegram/properties_button.png");
        if (resource == null) {
            return null;
        }
        Image image = new ImageIcon(resource).getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    private ImageIcon loadExportIcon() {
        URL resource = getClass().getResource("/tanglegram/export_button.png");
        if (resource == null) {
            return null;
        }
        Image image = new ImageIcon(resource).getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

}
