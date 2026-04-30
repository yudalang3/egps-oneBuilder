package tanglegram;

import com.jidesoft.swing.JideTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTabbedPane;

final class TanglegramResultTabPanel extends JPanel implements ExportableView {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JideTabbedPane pairTabs;
    private final List<TreePairSpec> pairSpecs;
    private final JButton visualPropertiesButton;
    private TanglegramRenderOptions renderOptions;

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

        this.pairSpecs = List.copyOf(pairSpecs);
        this.pairTabs = createPairTabs();
        this.renderOptions = initialRenderOptions();
        this.visualPropertiesButton = new JButton(UiText.text("Visual properties", "可视化属性"));
        this.visualPropertiesButton.addActionListener(event -> openVisualPropertiesDialog());

        JButton treeAlignmentButton = new JButton(UiText.text("3D Tree Alignment", "3D 树对齐"));
        treeAlignmentButton.addActionListener(event -> openThreeDAlignmentAction.run());
        this.pairTabs.setTabTrailingComponent(createTrailingActions(treeAlignmentButton));
        rebuildPairTabs(0);

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

    boolean hasVisualPropertiesButtonForTest() {
        return visualPropertiesButton != null;
    }

    int pairTabCountForTest() {
        return pairTabs.getTabCount();
    }

    TanglegramRenderOptions renderOptionsForTest() {
        return renderOptions;
    }

    void applyRenderOptionsForTest(TanglegramRenderOptions updatedOptions) {
        applyRenderOptions(updatedOptions);
    }

    static List<String> standaloneActionLabelsForTest() {
        return Arrays.asList(
                UiText.text("Visual properties", "可视化属性"),
                UiText.text("3D Tree Alignment", "3D 树对齐"));
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

    private JPanel createTrailingActions(JButton treeAlignmentButton) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(visualPropertiesButton);
        buttonPanel.add(javax.swing.Box.createHorizontalStrut(6));
        buttonPanel.add(treeAlignmentButton);
        return buttonPanel;
    }

    private TanglegramRenderOptions initialRenderOptions() {
        TanglegramRenderOptions defaults = TanglegramRenderOptions.defaults();
        return new TanglegramRenderOptions(
                defaults.labelFontSize(),
                defaults.labelFontFamily(),
                defaults.labelFontStyle(),
                defaults.showLeafLabels(),
                4,
                2,
                160,
                defaults.connectorStrokeWidth(),
                defaults.connectorDashLength(),
                defaults.connectorDashGap(),
                true);
    }

    private void openVisualPropertiesDialog() {
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        TanglegramVisualPropertiesDialog.showDialog(owner, renderOptions, this::applyRenderOptions);
    }

    private void applyRenderOptions(TanglegramRenderOptions updatedOptions) {
        if (updatedOptions == null) {
            return;
        }
        int selectedIndex = pairTabs.getSelectedIndex();
        renderOptions = updatedOptions;
        rebuildPairTabs(selectedIndex);
    }

    private void rebuildPairTabs(int selectedIndex) {
        pairTabs.removeAll();
        TanglegramPanelFactory panelFactory = new TanglegramPanelFactory(renderOptions);
        for (TreePairSpec pairSpec : pairSpecs) {
            pairTabs.addTab(pairSpec.tabName(), new ResizableTanglegramView(pairSpec, panelFactory, renderOptions));
        }
        if (pairTabs.getTabCount() > 0) {
            pairTabs.setSelectedIndex(Math.max(0, Math.min(selectedIndex, pairTabs.getTabCount() - 1)));
        }
        pairTabs.revalidate();
        pairTabs.repaint();
    }

    private static String buildSummary(
            String sourceName,
            ImportSourceKind sourceKind,
            List<ImportedTreeSpec> importedTrees,
            List<TreePairSpec> pairSpecs,
            List<String> warnings) {
        StringBuilder summary = new StringBuilder();
        summary.append(UiText.text("Source type: ", "来源类型: ")).append(sourceKind.displayName()).append('\n');
        summary.append(UiText.text("Source name: ", "来源名称: ")).append(sourceName).append('\n');
        summary.append(UiText.text("Created at: ", "创建时间: ")).append(TIMESTAMP_FORMAT.format(LocalDateTime.now())).append('\n');
        summary.append(UiText.text("Tree count: ", "树数量: ")).append(importedTrees.size()).append('\n');
        summary.append(UiText.text("Pair count: ", "比较对数量: ")).append(pairSpecs.size()).append('\n');
        summary.append('\n').append(UiText.text("Imported trees:", "已导入的树:")).append('\n');
        for (ImportedTreeSpec importedTree : importedTrees) {
            summary.append("- ").append(importedTree.label()).append(" -> ").append(importedTree.path()).append('\n');
        }
        if (!warnings.isEmpty()) {
            summary.append('\n').append(UiText.text("Warnings:", "警告:")).append('\n');
            for (String warning : warnings) {
                summary.append("- ").append(warning).append('\n');
            }
        }
        return summary.toString();
    }
}
