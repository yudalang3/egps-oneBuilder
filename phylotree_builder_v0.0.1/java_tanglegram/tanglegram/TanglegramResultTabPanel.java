package tanglegram;

import com.jidesoft.swing.JideTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTabbedPane;

final class TanglegramResultTabPanel extends JPanel implements ExportableView {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JideTabbedPane pairTabs;

    TanglegramResultTabPanel(
            String sourceName,
            ImportSourceKind sourceKind,
            List<ImportedTreeSpec> importedTrees,
            List<TreePairSpec> pairSpecs,
            List<String> warnings,
            Runnable openThreeDAlignmentAction) {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        setOpaque(false);

        this.pairTabs = createPairTabs();

        TanglegramPanelFactory panelFactory = new TanglegramPanelFactory(
                new TanglegramRenderOptions(UiPreferenceStore.load().defaultTanglegramLabelFontSize(), 4, 2, true));
        for (TreePairSpec pairSpec : pairSpecs) {
            this.pairTabs.addTab(pairSpec.tabName(), new ResizableTanglegramView(pairSpec, panelFactory));
        }

        JButton treeAlignmentButton = new JButton("3D Tree Alignment");
        treeAlignmentButton.addActionListener(event -> openThreeDAlignmentAction.run());
        this.pairTabs.setTabTrailingComponent(treeAlignmentButton);

        JTextArea summaryArea = new JTextArea(buildSummary(sourceName, sourceKind, importedTrees, pairSpecs, warnings));
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setBackground(new Color(248, 251, 255));
        summaryArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 241)));
        summaryPanel.add(summaryArea, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.pairTabs, summaryPanel);
        splitPane.setResizeWeight(0.94d);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);
    }

    @Override
    public JComponent getExportComponent() {
        java.awt.Component selectedComponent = pairTabs.getSelectedComponent();
        if (selectedComponent instanceof JComponent exportComponent) {
            return exportComponent;
        }
        return null;
    }

    @Override
    public Class<?> getExportContextClass() {
        return TanglegramResultTabPanel.class;
    }

    private static JideTabbedPane createPairTabs() {
        JideTabbedPane tabs = new JideTabbedPane(JTabbedPane.BOTTOM, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setBorder(BorderFactory.createEmptyBorder());
        tabs.setShowCloseButton(false);
        tabs.setShowCloseButtonOnTab(false);
        tabs.setShowTabButtons(false);
        tabs.setTabShape(JideTabbedPane.SHAPE_OFFICE2003);
        tabs.setColorTheme(JideTabbedPane.COLOR_THEME_WINXP);
        tabs.setTabResizeMode(JideTabbedPane.RESIZE_MODE_FIT);
        return tabs;
    }

    private static String buildSummary(
            String sourceName,
            ImportSourceKind sourceKind,
            List<ImportedTreeSpec> importedTrees,
            List<TreePairSpec> pairSpecs,
            List<String> warnings) {
        StringBuilder summary = new StringBuilder();
        summary.append("Source type: ").append(sourceKind.displayName()).append('\n');
        summary.append("Source name: ").append(sourceName).append('\n');
        summary.append("Created at: ").append(TIMESTAMP_FORMAT.format(LocalDateTime.now())).append('\n');
        summary.append("Tree count: ").append(importedTrees.size()).append('\n');
        summary.append("Pair count: ").append(pairSpecs.size()).append('\n');
        summary.append('\n').append("Imported trees:").append('\n');
        for (ImportedTreeSpec importedTree : importedTrees) {
            summary.append("- ").append(importedTree.label()).append(" -> ").append(importedTree.path()).append('\n');
        }
        if (!warnings.isEmpty()) {
            summary.append('\n').append("Warnings:").append('\n');
            for (String warning : warnings) {
                summary.append("- ").append(warning).append('\n');
            }
        }
        return summary.toString();
    }
}
