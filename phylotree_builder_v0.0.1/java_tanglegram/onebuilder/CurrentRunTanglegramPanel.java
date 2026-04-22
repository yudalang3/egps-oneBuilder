package onebuilder;

import com.jidesoft.swing.JideTabbedPane;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import tanglegram.ResizableTanglegramView;
import tanglegram.TanglegramPanelFactory;
import tanglegram.TanglegramRenderOptions;
import tanglegram.TreePairSpec;
import tanglegram.TreeSummaryLoadResult;
import tanglegram.TreeSummaryLoader;
import tanglegram.UiPreferenceStore;
import tanglegram.UiPreferences;
import tanglegram.UiText;

final class CurrentRunTanglegramPanel extends JPanel {
    private JSpinner labelFontSizeSpinner;
    private JSpinner horizontalPaddingSpinner;
    private JSpinner verticalPaddingSpinner;
    private JCheckBox autoFitCheckBox;
    private final JLabel summaryLabel;
    private final JideTabbedPane comparisonTabs;
    private final Object loadMonitor;
    private JPanel controlsPanel;
    private JLabel labelFontLabel;
    private JLabel horizontalPaddingLabel;
    private JLabel verticalPaddingLabel;
    private JLabel autoFitLabel;
    private javax.swing.JButton reloadButton;
    private Path currentOutputDirectory;
    private volatile long loadSequence;
    private volatile boolean loading;

    CurrentRunTanglegramPanel() {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        loadMonitor = new Object();

        add(buildControlsPanel(), BorderLayout.NORTH);

        comparisonTabs = new JideTabbedPane(JideTabbedPane.LEFT);
        comparisonTabs.setTabLayoutPolicy(JideTabbedPane.SCROLL_TAB_LAYOUT);
        comparisonTabs.setShowCloseButton(false);
        comparisonTabs.setShowCloseButtonOnTab(false);
        add(comparisonTabs, BorderLayout.CENTER);

        summaryLabel = new JLabel(UiText.text("No current run loaded.", "当前没有已加载的运行结果。"));
        add(summaryLabel, BorderLayout.SOUTH);
    }

    private JPanel buildControlsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        controlsPanel = panel;
        panel.setBorder(BorderFactory.createTitledBorder(UiText.text("Tanglegram Options", "缠结图选项")));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 8, 8);
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        constraints.gridy = 0;
        labelFontLabel = new JLabel(UiText.text("Label font", "标签字号"));
        panel.add(labelFontLabel, constraints);

        constraints.gridx = 1;
        labelFontSizeSpinner = new JSpinner(new SpinnerNumberModel(UiPreferenceStore.load().defaultTanglegramLabelFontSize(), 8, 48, 1));
        panel.add(labelFontSizeSpinner, constraints);

        constraints.gridx = 2;
        horizontalPaddingLabel = new JLabel(UiText.text("Horizontal padding", "水平留白"));
        panel.add(horizontalPaddingLabel, constraints);

        constraints.gridx = 3;
        horizontalPaddingSpinner = new JSpinner(new SpinnerNumberModel(24, 0, 240, 4));
        panel.add(horizontalPaddingSpinner, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        verticalPaddingLabel = new JLabel(UiText.text("Vertical padding", "垂直留白"));
        panel.add(verticalPaddingLabel, constraints);

        constraints.gridx = 1;
        verticalPaddingSpinner = new JSpinner(new SpinnerNumberModel(16, 0, 240, 4));
        panel.add(verticalPaddingSpinner, constraints);

        constraints.gridx = 2;
        autoFitLabel = new JLabel(UiText.text("Auto fit", "自动适配"));
        panel.add(autoFitLabel, constraints);

        constraints.gridx = 3;
        autoFitCheckBox = new JCheckBox(UiText.text("Fit viewport", "适配视口"), true);
        panel.add(autoFitCheckBox, constraints);

        reloadButton = new javax.swing.JButton(UiText.text("Reload from current run", "从当前运行结果重新加载"));
        reloadButton.addActionListener(event -> {
            if (currentOutputDirectory != null) {
                loadRunResults(currentOutputDirectory);
            }
        });

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 4;
        constraints.insets = new Insets(4, 0, 0, 0);
        panel.add(reloadButton, constraints);

        return panel;
    }

    JideTabbedPane comparisonTabs() {
        return comparisonTabs;
    }

    int labelFontSizeValueForTest() {
        return ((Integer) labelFontSizeSpinner.getValue()).intValue();
    }

    void applyPreferences(UiPreferences preferences) {
        labelFontSizeSpinner.setValue(Integer.valueOf(preferences.defaultTanglegramLabelFontSize()));
        controlsPanel.setBorder(BorderFactory.createTitledBorder(UiText.text(preferences, "Tanglegram Options", "缠结图选项")));
        labelFontLabel.setText(UiText.text(preferences, "Label font", "标签字号"));
        horizontalPaddingLabel.setText(UiText.text(preferences, "Horizontal padding", "水平留白"));
        verticalPaddingLabel.setText(UiText.text(preferences, "Vertical padding", "垂直留白"));
        autoFitLabel.setText(UiText.text(preferences, "Auto fit", "自动适配"));
        autoFitCheckBox.setText(UiText.text(preferences, "Fit viewport", "适配视口"));
        reloadButton.setText(UiText.text(preferences, "Reload from current run", "从当前运行结果重新加载"));
        if (currentOutputDirectory == null) {
            summaryLabel.setText(UiText.text(preferences, "No current run loaded.", "当前没有已加载的运行结果。"));
        }
        if (currentOutputDirectory != null) {
            loadRunResults(currentOutputDirectory);
        }
    }

    void loadRunResults(Path outputDirectory) {
        final long requestId = ++loadSequence;
        currentOutputDirectory = outputDirectory == null ? null : outputDirectory.toAbsolutePath().normalize();
        comparisonTabs.removeAll();
        if (currentOutputDirectory == null) {
            summaryLabel.setText(UiText.text("No current run loaded.", "当前没有已加载的运行结果。"));
            markLoadFinished();
            return;
        }

        Path treeSummaryDir = CurrentRunArtifacts.resolveTreeSummaryDir(currentOutputDirectory);
        summaryLabel.setText(UiText.text("Loading current run tanglegrams from ", "正在从以下目录加载当前运行的缠结图: ") + treeSummaryDir + " ...");
        markLoadStarted();
        Thread loadThread = new Thread(() -> {
            try {
                TreeSummaryLoadResult loadResult = TreeSummaryLoader.load(treeSummaryDir);
                SwingUtilities.invokeLater(() -> applyLoadResult(requestId, treeSummaryDir, loadResult));
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> applyLoadFailure(requestId, exception));
            }
        }, "onebuilder-current-run-tanglegram-loader");
        loadThread.setDaemon(true);
        loadThread.start();
    }

    boolean waitForLoadCompletionForTest(long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        synchronized (loadMonitor) {
            while (loading) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    return false;
                }
                loadMonitor.wait(remaining);
            }
            return true;
        }
    }

    private void applyLoadResult(long requestId, Path treeSummaryDir, TreeSummaryLoadResult loadResult) {
        if (requestId != loadSequence) {
            return;
        }
        comparisonTabs.removeAll();
        if (loadResult.resolvedTrees().size() < 2) {
            summaryLabel.setText(UiText.text("Current run does not contain at least two readable trees: ", "当前运行结果中少于两棵可读取的树: ") + treeSummaryDir);
            markLoadFinished();
            return;
        }

        TanglegramRenderOptions renderOptions = new TanglegramRenderOptions(
                ((Integer) labelFontSizeSpinner.getValue()).intValue(),
                ((Integer) horizontalPaddingSpinner.getValue()).intValue(),
                ((Integer) verticalPaddingSpinner.getValue()).intValue(),
                autoFitCheckBox.isSelected());
        TanglegramPanelFactory panelFactory = new TanglegramPanelFactory(renderOptions);
        for (TreePairSpec pairSpec : loadResult.availablePairs()) {
            comparisonTabs.addTab(pairSpec.tabName(), new ResizableTanglegramView(pairSpec, panelFactory, renderOptions));
        }

        String warningSuffix = loadResult.missingMethods().isEmpty()
                ? ""
                : UiText.text(" Missing methods: ", " 缺失方法: ") + loadResult.missingMethods();
        summaryLabel.setText(UiText.text("Loaded ", "已加载 ") + loadResult.availablePairs().size()
                + UiText.text(" pair tabs from ", " 个成对比较标签页，来源: ") + treeSummaryDir + "." + warningSuffix);
        markLoadFinished();
    }

    private void applyLoadFailure(long requestId, Exception exception) {
        if (requestId != loadSequence) {
            return;
        }
        comparisonTabs.removeAll();
        summaryLabel.setText(UiText.text("Failed to load current run tanglegrams: ", "加载当前运行的缠结图失败: ") + exception.getMessage());
        markLoadFinished();
    }

    private void markLoadStarted() {
        synchronized (loadMonitor) {
            loading = true;
        }
    }

    private void markLoadFinished() {
        synchronized (loadMonitor) {
            loading = false;
            loadMonitor.notifyAll();
        }
    }
}
