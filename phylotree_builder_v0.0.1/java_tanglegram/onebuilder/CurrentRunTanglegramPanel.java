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

final class CurrentRunTanglegramPanel extends JPanel {
    private JSpinner labelFontSizeSpinner;
    private JSpinner horizontalPaddingSpinner;
    private JSpinner verticalPaddingSpinner;
    private JCheckBox autoFitCheckBox;
    private final JLabel summaryLabel;
    private final JideTabbedPane comparisonTabs;
    private final Object loadMonitor;
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

        summaryLabel = new JLabel("No current run loaded.");
        add(summaryLabel, BorderLayout.SOUTH);
    }

    private JPanel buildControlsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Tanglegram Options"));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 8, 8);
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(new JLabel("Label font"), constraints);

        constraints.gridx = 1;
        labelFontSizeSpinner = new JSpinner(new SpinnerNumberModel(UiPreferenceStore.load().defaultTanglegramLabelFontSize(), 8, 48, 1));
        panel.add(labelFontSizeSpinner, constraints);

        constraints.gridx = 2;
        panel.add(new JLabel("Horizontal padding"), constraints);

        constraints.gridx = 3;
        horizontalPaddingSpinner = new JSpinner(new SpinnerNumberModel(24, 0, 240, 4));
        panel.add(horizontalPaddingSpinner, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(new JLabel("Vertical padding"), constraints);

        constraints.gridx = 1;
        verticalPaddingSpinner = new JSpinner(new SpinnerNumberModel(16, 0, 240, 4));
        panel.add(verticalPaddingSpinner, constraints);

        constraints.gridx = 2;
        panel.add(new JLabel("Auto fit"), constraints);

        constraints.gridx = 3;
        autoFitCheckBox = new JCheckBox("Fit viewport", true);
        panel.add(autoFitCheckBox, constraints);

        javax.swing.JButton reloadButton = new javax.swing.JButton("Reload from current run");
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
        if (currentOutputDirectory != null) {
            loadRunResults(currentOutputDirectory);
        }
    }

    void loadRunResults(Path outputDirectory) {
        final long requestId = ++loadSequence;
        currentOutputDirectory = outputDirectory == null ? null : outputDirectory.toAbsolutePath().normalize();
        comparisonTabs.removeAll();
        if (currentOutputDirectory == null) {
            summaryLabel.setText("No current run loaded.");
            markLoadFinished();
            return;
        }

        Path treeSummaryDir = CurrentRunArtifacts.resolveTreeSummaryDir(currentOutputDirectory);
        summaryLabel.setText("Loading current run tanglegrams from " + treeSummaryDir + " ...");
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
            summaryLabel.setText("Current run does not contain at least two readable trees: " + treeSummaryDir);
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
                : " Missing methods: " + loadResult.missingMethods();
        summaryLabel.setText("Loaded " + loadResult.availablePairs().size() + " pair tabs from " + treeSummaryDir + "." + warningSuffix);
        markLoadFinished();
    }

    private void applyLoadFailure(long requestId, Exception exception) {
        if (requestId != loadSequence) {
            return;
        }
        comparisonTabs.removeAll();
        summaryLabel.setText("Failed to load current run tanglegrams: " + exception.getMessage());
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
