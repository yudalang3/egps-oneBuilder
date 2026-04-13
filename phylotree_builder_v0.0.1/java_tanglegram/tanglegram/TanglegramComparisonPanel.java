package tanglegram;

import com.jidesoft.swing.JideTabbedPane;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public final class TanglegramComparisonPanel extends JPanel {
    private final JideTabbedPane pairTabs;
    private TanglegramRenderOptions renderOptions;
    private TreeSummaryLoadResult currentLoadResult;
    private Path currentTreeSummaryDir;

    public TanglegramComparisonPanel() {
        this.renderOptions = TanglegramRenderOptions.defaults();
        this.pairTabs = createPairTabs();

        setLayout(new BorderLayout());
        add(pairTabs, BorderLayout.CENTER);
    }

    public String loadTreeSummary(Path treeSummaryDir) throws Exception {
        currentTreeSummaryDir = treeSummaryDir;
        currentLoadResult = TreeSummaryLoader.load(treeSummaryDir);
        if (currentLoadResult.availablePairs().size() < 1) {
            throw new IllegalStateException("No renderable tree pairs found in " + treeSummaryDir);
        }
        rebuildTabs();
        return buildStatusMessage(currentLoadResult);
    }

    public void reload() throws Exception {
        if (currentTreeSummaryDir != null) {
            loadTreeSummary(currentTreeSummaryDir);
        }
    }

    public void clearPairs() {
        currentLoadResult = null;
        currentTreeSummaryDir = null;
        pairTabs.removeAll();
    }

    public void setRenderOptions(TanglegramRenderOptions renderOptions) {
        this.renderOptions = renderOptions == null ? TanglegramRenderOptions.defaults() : renderOptions;
        rebuildTabs();
    }

    private void rebuildTabs() {
        pairTabs.removeAll();
        if (currentLoadResult == null) {
            return;
        }

        TanglegramPanelFactory panelFactory = new TanglegramPanelFactory(renderOptions);
        List<TreePairSpec> pairSpecs = currentLoadResult.availablePairs();
        for (int index = 0; index < pairSpecs.size(); index++) {
            TreePairSpec pairSpec = pairSpecs.get(index);
            pairTabs.addTab(pairSpec.tabName(), new ResizableTanglegramView(pairSpec, panelFactory, renderOptions));
            pairTabs.setTabClosableAt(index, false);
        }
        pairTabs.revalidate();
        pairTabs.repaint();
    }

    private static JideTabbedPane createPairTabs() {
        JideTabbedPane tabs = new JideTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setShowCloseButton(false);
        tabs.setShowCloseButtonOnTab(false);
        tabs.setShowTabButtons(false);
        tabs.setTabShape(JideTabbedPane.SHAPE_FLAT);
        tabs.setBoldActiveTab(true);
        tabs.setTabResizeMode(JideTabbedPane.RESIZE_MODE_FIT);
        return tabs;
    }

    private static String buildStatusMessage(TreeSummaryLoadResult loadResult) {
        List<String> fragments = new ArrayList<>();
        fragments.add("Loaded " + loadResult.availablePairs().size() + " pair views");
        if (!loadResult.missingMethods().isEmpty()) {
            fragments.add("Missing methods: " + formatMissingMethods(loadResult));
        }
        return String.join(" | ", fragments);
    }

    private static String formatMissingMethods(TreeSummaryLoadResult loadResult) {
        List<String> labels = new ArrayList<>();
        for (TreeMethod method : loadResult.missingMethods()) {
            labels.add(method.shortLabel());
        }
        return labels.toString();
    }
}
