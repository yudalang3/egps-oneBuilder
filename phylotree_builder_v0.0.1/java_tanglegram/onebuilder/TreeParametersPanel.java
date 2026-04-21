package onebuilder;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

final class TreeParametersPanel extends JPanel {
    private final DistanceMethodPanel distancePanel;
    private final MaximumLikelihoodPanel maximumLikelihoodPanel;
    private final BayesianPanel bayesianPanel;
    private final ParsimonyMethodPanel parsimonyPanel;
    private final JTree sectionTree;
    private final JPanel cardPanel;
    private final CardLayout cardLayout;
    private final JTextArea proteinStructureNote;
    private final Map<ParameterSection, DefaultMutableTreeNode> nodesBySection;
    private final DefaultTreeCellRenderer treeRenderer;
    private InputType inputType;
    private ParameterSection selectedSection;

    TreeParametersPanel(InputType inputType) {
        super(new BorderLayout(0, 16));
        this.inputType = inputType;
        this.nodesBySection = new EnumMap<>(ParameterSection.class);
        WorkbenchStyles.applyCanvas(this);

        distancePanel = new DistanceMethodPanel(inputType);
        maximumLikelihoodPanel = new MaximumLikelihoodPanel();
        bayesianPanel = new BayesianPanel();
        parsimonyPanel = new ParsimonyMethodPanel(inputType);

        add(buildHeader(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardPanel = WorkbenchStyles.createCanvasPanel(cardLayout);
        proteinStructureNote = WorkbenchStyles.createNoteArea("");
        buildCards();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Tree Parameters");
        for (ParameterSection section : ParameterSection.values()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(section);
            nodesBySection.put(section, node);
            root.add(node);
        }

        sectionTree = new JTree(new DefaultTreeModel(root)) {
            @Override
            public String getToolTipText(MouseEvent event) {
                ParameterSection section = sectionAt(event.getX(), event.getY());
                return proteinOnlyMessage(section);
            }
        };
        sectionTree.setRootVisible(false);
        sectionTree.setShowsRootHandles(false);
        sectionTree.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        sectionTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        ToolTipManager.sharedInstance().registerComponent(sectionTree);

        treeRenderer = new DefaultTreeCellRenderer();
        treeRenderer.setLeafIcon(null);
        treeRenderer.setClosedIcon(null);
        treeRenderer.setOpenIcon(null);
        treeRenderer.setBackgroundSelectionColor(WorkbenchStyles.ACCENT_SOFT);
        treeRenderer.setBorderSelectionColor(WorkbenchStyles.ACCENT_BORDER);
        treeRenderer.setTextSelectionColor(WorkbenchStyles.ACCENT);
        treeRenderer.setTextNonSelectionColor(WorkbenchStyles.TEXT_PRIMARY);
        treeRenderer.setBackgroundNonSelectionColor(WorkbenchStyles.SURFACE_BACKGROUND);
        sectionTree.setCellRenderer((tree, value, selected, expanded, leaf, row, hasFocus) -> {
            JLabel label = (JLabel) treeRenderer.getTreeCellRendererComponent(
                    tree,
                    value,
                    selected,
                    expanded,
                    leaf,
                    row,
                    hasFocus);
            Object userObject = value instanceof DefaultMutableTreeNode ? ((DefaultMutableTreeNode) value).getUserObject() : null;
            if (userObject instanceof ParameterSection) {
                ParameterSection section = (ParameterSection) userObject;
                boolean enabled = isSectionEnabled(section);
                label.setText(section.label());
                if (!enabled) {
                    label.setForeground(WorkbenchStyles.TEXT_SECONDARY);
                }
            }
            return label;
        });

        sectionTree.addTreeSelectionListener(event -> {
            TreePath selectionPath = event.getNewLeadSelectionPath();
            if (selectionPath == null) {
                return;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            Object userObject = node.getUserObject();
            if (!(userObject instanceof ParameterSection)) {
                return;
            }
            ParameterSection requestedSection = (ParameterSection) userObject;
            if (!isSectionEnabled(requestedSection)) {
                restoreSelection();
                showProteinOnlyTooltip(requestedSection);
                return;
            }
            selectedSection = requestedSection;
            cardLayout.show(cardPanel, requestedSection.cardKey());
        });

        JScrollPane treeScrollPane = new JScrollPane(sectionTree);
        treeScrollPane.setBorder(null);
        treeScrollPane.getViewport().setBackground(WorkbenchStyles.SURFACE_BACKGROUND);

        JPanel treeCard = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 8));
        treeCard.add(WorkbenchStyles.createSectionTitle("Method Tree"), BorderLayout.NORTH);
        treeCard.add(treeScrollPane, BorderLayout.CENTER);
        treeCard.setPreferredSize(new Dimension(260, 420));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeCard, cardPanel);
        splitPane.setBorder(null);
        splitPane.setResizeWeight(0.24d);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(10);
        splitPane.setOpaque(false);
        add(splitPane, BorderLayout.CENTER);

        applyRuntimeConfig(PipelineRuntimeConfig.defaultsFor(inputType));
        selectSection(ParameterSection.DISTANCE_METHOD);
    }

    List<String> parameterTreeLabels() {
        List<String> labels = new ArrayList<>();
        for (ParameterSection section : ParameterSection.values()) {
            labels.add(section.label());
        }
        return labels;
    }

    boolean isProteinStructureEnabledForTest() {
        return isSectionEnabled(ParameterSection.PROTEIN_STRUCTURE);
    }

    void applyPreferences() {
        revalidate();
        repaint();
    }

    void setInputType(InputType inputType) {
        this.inputType = inputType;
        distancePanel.setInputType(inputType);
        maximumLikelihoodPanel.setInputType(inputType);
        bayesianPanel.setInputType(inputType);
        parsimonyPanel.setInputType(inputType);
        proteinStructureNote.setText(proteinStructureText());
        sectionTree.repaint();
        if (selectedSection != null && !isSectionEnabled(selectedSection)) {
            selectSection(ParameterSection.DISTANCE_METHOD);
        }
    }

    void applyRuntimeConfig(PipelineRuntimeConfig runtimeConfig) {
        setInputType(runtimeConfig.inputType());
        distancePanel.apply(runtimeConfig.distance());
        maximumLikelihoodPanel.apply(runtimeConfig.maximumLikelihood(), runtimeConfig.inputType());
        bayesianPanel.apply(runtimeConfig.bayesian(), runtimeConfig.inputType());
        parsimonyPanel.apply(runtimeConfig.parsimony());
    }

    PipelineRuntimeConfig runtimeConfig() {
        return new PipelineRuntimeConfig(
                inputType,
                distancePanel.toConfig(),
                maximumLikelihoodPanel.toConfig(),
                bayesianPanel.toConfig(inputType),
                parsimonyPanel.toConfig());
    }

    private JPanel buildHeader() {
        JPanel headerCard = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 6));
        headerCard.add(WorkbenchStyles.createSectionTitle("Tree Parameters"), BorderLayout.NORTH);
        headerCard.add(
                WorkbenchStyles.createSubtitleLabel(
                        "Browse the parameter tree, adjust each method, and keep Protein Structure reserved for future protein-only work."),
                BorderLayout.CENTER);
        return headerCard;
    }

    private void buildCards() {
        cardPanel.add(createMethodCard(
                "Distance Method",
                "PHYLIP distance plus neighbor settings.",
                distancePanel),
                ParameterSection.DISTANCE_METHOD.cardKey());
        cardPanel.add(createMethodCard(
                "Maximum Likelihood",
                "IQ-TREE likelihood inference settings.",
                maximumLikelihoodPanel),
                ParameterSection.MAXIMUM_LIKELIHOOD.cardKey());
        cardPanel.add(createMethodCard(
                "Bayes Method",
                "MrBayes posterior sampling and convergence settings.",
                bayesianPanel),
                ParameterSection.BAYES_METHOD.cardKey());
        cardPanel.add(createMethodCard(
                "Maximum Parsimony",
                "PHYLIP parsimony settings.",
                parsimonyPanel),
                ParameterSection.MAXIMUM_PARSIMONY.cardKey());
        cardPanel.add(createPlaceholderCard(), ParameterSection.PROTEIN_STRUCTURE.cardKey());
    }

    private JPanel createMethodCard(String title, String subtitle, JPanel content) {
        JPanel card = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 14));
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(WorkbenchStyles.createSectionTitle(title), BorderLayout.NORTH);
        header.add(WorkbenchStyles.createSubtitleLabel(subtitle), BorderLayout.CENTER);
        card.add(header, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(WorkbenchStyles.SURFACE_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private JPanel createPlaceholderCard() {
        JPanel card = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 12));
        card.add(WorkbenchStyles.createSectionTitle("Protein Structure"), BorderLayout.NORTH);
        proteinStructureNote.setText(proteinStructureText());
        card.add(proteinStructureNote, BorderLayout.CENTER);
        return card;
    }

    private void selectSection(ParameterSection section) {
        selectedSection = section;
        DefaultMutableTreeNode node = nodesBySection.get(section);
        if (node != null) {
            TreePath path = new TreePath(node.getPath());
            sectionTree.setSelectionPath(path);
            sectionTree.scrollPathToVisible(path);
        }
        cardLayout.show(cardPanel, section.cardKey());
    }

    private void restoreSelection() {
        if (selectedSection != null) {
            DefaultMutableTreeNode node = nodesBySection.get(selectedSection);
            if (node != null) {
                sectionTree.setSelectionPath(new TreePath(node.getPath()));
            }
        }
    }

    private ParameterSection sectionAt(int x, int y) {
        TreePath path = sectionTree.getPathForLocation(x, y);
        if (path == null) {
            return null;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        return userObject instanceof ParameterSection ? (ParameterSection) userObject : null;
    }

    private boolean isSectionEnabled(ParameterSection section) {
        return section != null && (!section.proteinOnly() || inputType == InputType.PROTEIN);
    }

    private String proteinOnlyMessage(ParameterSection section) {
        if (section != null && section.proteinOnly() && inputType != InputType.PROTEIN) {
            return "Protein only";
        }
        return null;
    }

    private String proteinStructureText() {
        return inputType == InputType.PROTEIN
                ? "Reserved for future protein structure workflow. This placeholder stays visible so protein projects can grow into structure-aware analysis later."
                : "Protein only. Switch the input type to Protein to enable this future structure workflow placeholder.";
    }

    private void showProteinOnlyTooltip(ParameterSection section) {
        if (section == null) {
            return;
        }
        DefaultMutableTreeNode node = nodesBySection.get(section);
        if (node == null) {
            return;
        }
        TreePath path = new TreePath(node.getPath());
        Rectangle bounds = sectionTree.getPathBounds(path);
        if (bounds == null) {
            return;
        }
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        int originalDelay = toolTipManager.getInitialDelay();
        toolTipManager.setInitialDelay(0);
        MouseEvent event = new MouseEvent(
                sectionTree,
                MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0,
                bounds.x + Math.max(4, bounds.width / 3),
                bounds.y + Math.max(4, bounds.height / 2),
                0,
                false);
        toolTipManager.mouseMoved(event);
        Timer restoreDelayTimer = new Timer(750, restoreEvent -> toolTipManager.setInitialDelay(originalDelay));
        restoreDelayTimer.setRepeats(false);
        restoreDelayTimer.start();
    }

    private enum ParameterSection {
        DISTANCE_METHOD("Distance Method", "distanceMethod", false),
        MAXIMUM_LIKELIHOOD("Maximum Likelihood", "maximumLikelihood", false),
        BAYES_METHOD("Bayes Method", "bayesMethod", false),
        MAXIMUM_PARSIMONY("Maximum Parsimony", "maximumParsimony", false),
        PROTEIN_STRUCTURE("Protein Structure", "proteinStructure", true);

        private final String label;
        private final String cardKey;
        private final boolean proteinOnly;

        ParameterSection(String label, String cardKey, boolean proteinOnly) {
            this.label = label;
            this.cardKey = cardKey;
            this.proteinOnly = proteinOnly;
        }

        String label() {
            return label;
        }

        String cardKey() {
            return cardKey;
        }

        boolean proteinOnly() {
            return proteinOnly;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
