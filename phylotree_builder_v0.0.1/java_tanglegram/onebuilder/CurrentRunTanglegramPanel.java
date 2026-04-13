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
import tanglegram.ResizableTanglegramView;
import tanglegram.TanglegramPanelFactory;
import tanglegram.TanglegramRenderOptions;
import tanglegram.TreePairSpec;
import tanglegram.TreeSummaryLoadResult;
import tanglegram.TreeSummaryLoader;

final class CurrentRunTanglegramPanel extends JPanel {
    private JSpinner labelFontSizeSpinner;
    private JSpinner horizontalPaddingSpinner;
    private JSpinner verticalPaddingSpinner;
    private JCheckBox autoFitCheckBox;
    private final JLabel summaryLabel;
    private final JideTabbedPane comparisonTabs;
    private Path currentOutputDirectory;

    CurrentRunTanglegramPanel() {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

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
        labelFontSizeSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 48, 1));
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

    void loadRunResults(Path outputDirectory) {
        currentOutputDirectory = outputDirectory == null ? null : outputDirectory.toAbsolutePath().normalize();
        comparisonTabs.removeAll();
        if (currentOutputDirectory == null) {
            summaryLabel.setText("No current run loaded.");
            return;
        }

        Path treeSummaryDir = CurrentRunArtifacts.resolveTreeSummaryDir(currentOutputDirectory);
        try {
            TreeSummaryLoadResult loadResult = TreeSummaryLoader.load(treeSummaryDir);
            if (loadResult.resolvedTrees().size() < 2) {
                summaryLabel.setText("Current run does not contain at least two readable trees: " + treeSummaryDir);
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
        } catch (Exception exception) {
            summaryLabel.setText("Failed to load current run tanglegrams: " + exception.getMessage());
        }
    }
}
