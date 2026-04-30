package tanglegram;

import evoltree.struct.EvolNode;
import evoltree.struct.util.EvolNodeUtil;
import evoltree.txtdisplay.ReflectGraphicNode;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

final class CustomTanglegramPanel extends JPanel {
    private static final int TOP_BOTTOM_MARGIN = 18;
    private static final int LEAF_LABEL_GAP = 8;
    private static final int CONNECTOR_LABEL_GAP = 14;
    private static final int MIN_BRANCH_WIDTH = 110;
    private static final Stroke TREE_STROKE = new BasicStroke(1.0f);

    private final TanglegramPanelFactory.PreparedPair preparedPair;
    private final TanglegramRenderOptions renderOptions;
    private final Font labelFont;
    private final Font branchLengthFont;

    CustomTanglegramPanel(
            TanglegramPanelFactory.PreparedPair preparedPair,
            TanglegramRenderOptions renderOptions,
            Font labelFont) {
        this.preparedPair = preparedPair;
        this.renderOptions = renderOptions;
        this.labelFont = labelFont;
        this.branchLengthFont = labelFont.deriveFont(Math.max(9.0f, labelFont.getSize2D() - 2.0f));
        setOpaque(true);
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D graphics2d = (Graphics2D) graphics.create();
        graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Layout layout = buildLayout(graphics2d);
        drawTreeStructure(graphics2d, layout.leftRoot());
        drawTreeStructure(graphics2d, layout.rightRoot());
        drawConnectors(graphics2d, layout.leftLeafEndpoints(), layout.rightLeafEndpoints());
        drawLeafLabels(graphics2d, layout.leftLeafEndpoints(), true);
        drawLeafLabels(graphics2d, layout.rightLeafEndpoints(), false);
        graphics2d.dispose();
    }

    private Layout buildLayout(Graphics2D graphics2d) {
        EvolNode leftCopy = TreeDataLoader.copyTree(preparedPair.leftTree());
        EvolNode rightCopy = TreeDataLoader.copyTree(preparedPair.rightTree());
        ReflectGraphicNode<EvolNode> leftRoot = new ReflectGraphicNode<>(leftCopy);
        ReflectGraphicNode<EvolNode> rightRoot = new ReflectGraphicNode<>(rightCopy);

        FontMetrics labelMetrics = graphics2d.getFontMetrics(labelFont);
        boolean showLeafLabels = renderOptions.showLeafLabels();
        int maxLeftLabelWidth = showLeafLabels ? maxLeafLabelWidth(leftCopy, labelMetrics) : 0;
        int maxRightLabelWidth = showLeafLabels ? maxLeafLabelWidth(rightCopy, labelMetrics) : 0;

        int panelWidth = Math.max(getWidth(), 640);
        int panelHeight = Math.max(getHeight(), 480);
        int horizontalPadding = renderOptions.horizontalPadding();
        int verticalPadding = renderOptions.verticalPadding();
        int availableWidth = Math.max(320, panelWidth - (horizontalPadding * 2));
        int connectorGap = Math.max(80, renderOptions.connectorGap());
        int labelReserve = showLeafLabels
                ? maxLeftLabelWidth + maxRightLabelWidth + (LEAF_LABEL_GAP * 2) + (CONNECTOR_LABEL_GAP * 2)
                : 12;
        int reservedWidth = labelReserve + connectorGap;
        int branchWidth = Math.max(MIN_BRANCH_WIDTH, (availableWidth - reservedWidth) / 2);
        int totalUsedWidth = (branchWidth * 2) + reservedWidth;
        if (totalUsedWidth > availableWidth) {
            branchWidth = Math.max(80, branchWidth - (totalUsedWidth - availableWidth + 1) / 2);
        }

        int leftTreeStartX = horizontalPadding;
        int leftTreeEndX = leftTreeStartX + branchWidth;
        int rightTreeEndX = panelWidth - horizontalPadding;
        int rightTreeStartX = rightTreeEndX - branchWidth;
        int treeTopY = verticalPadding + TOP_BOTTOM_MARGIN;
        int treeHeight = Math.max(120, panelHeight - (verticalPadding * 2) - (TOP_BOTTOM_MARGIN * 2));

        HorizontalTreeLayoutCalculator leftCalculator = new HorizontalTreeLayoutCalculator(false, leftTreeStartX, leftTreeEndX, treeTopY, treeHeight);
        HorizontalTreeLayoutCalculator rightCalculator = new HorizontalTreeLayoutCalculator(true, rightTreeStartX, rightTreeEndX, treeTopY, treeHeight);
        leftCalculator.calculate(leftRoot);
        rightCalculator.calculate(rightRoot);

        Map<String, LeafEndpoint> leftLeafEndpoints = buildLeafEndpoints(leftRoot, labelMetrics, true);
        Map<String, LeafEndpoint> rightLeafEndpoints = buildLeafEndpoints(rightRoot, labelMetrics, false);
        return new Layout(leftRoot, rightRoot, leftLeafEndpoints, rightLeafEndpoints);
    }

    private static int maxLeafLabelWidth(EvolNode root, FontMetrics labelMetrics) {
        int maxWidth = 0;
        for (EvolNode leaf : EvolNodeUtil.getLeaves(root)) {
            maxWidth = Math.max(maxWidth, labelMetrics.stringWidth(leaf.getName()));
        }
        return maxWidth;
    }

    private Map<String, LeafEndpoint> buildLeafEndpoints(
            ReflectGraphicNode<EvolNode> root,
            FontMetrics labelMetrics,
            boolean leftSide) {
        Map<String, LeafEndpoint> endpoints = new LinkedHashMap<>();
        collectLeafEndpoints(root, labelMetrics, leftSide, endpoints);
        return endpoints;
    }

    @SuppressWarnings("unchecked")
    private void collectLeafEndpoints(
            ReflectGraphicNode<EvolNode> node,
            FontMetrics labelMetrics,
            boolean leftSide,
            Map<String, LeafEndpoint> endpoints) {
        if (node.getChildCount() == 0) {
            String name = node.getReflectNode().getName();
            int textWidth = renderOptions.showLeafLabels() ? labelMetrics.stringWidth(name) : 0;
            double xSelf = node.getXSelf();
            double ySelf = node.getYSelf();
            int labelX = leftSide
                    ? (int) Math.round(xSelf) + LEAF_LABEL_GAP
                    : (int) Math.round(xSelf) - LEAF_LABEL_GAP - textWidth;
            int baselineY = (int) Math.round(ySelf + (labelMetrics.getAscent() * 0.35d));
            double connectorX;
            if (renderOptions.showLeafLabels()) {
                connectorX = leftSide
                        ? labelX + textWidth + CONNECTOR_LABEL_GAP
                        : labelX - CONNECTOR_LABEL_GAP;
            } else {
                connectorX = leftSide ? xSelf + 6.0d : xSelf - 6.0d;
            }
            endpoints.put(name, new LeafEndpoint(name, connectorX, ySelf, labelX, baselineY));
            return;
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            collectLeafEndpoints((ReflectGraphicNode<EvolNode>) node.getChildAt(index), labelMetrics, leftSide, endpoints);
        }
    }

    private void drawConnectors(
            Graphics2D graphics2d,
            Map<String, LeafEndpoint> leftLeafEndpoints,
            Map<String, LeafEndpoint> rightLeafEndpoints) {
        Graphics2D connectorGraphics = (Graphics2D) graphics2d.create();
        connectorGraphics.setColor(new Color(115, 123, 136, 180));
        connectorGraphics.setStroke(createConnectorStroke());
        List<LeafEndpoint> orderedLeftLeaves = new ArrayList<>(leftLeafEndpoints.values());
        orderedLeftLeaves.sort((left, right) -> Double.compare(left.y(), right.y()));
        for (LeafEndpoint leftLeaf : orderedLeftLeaves) {
            LeafEndpoint rightLeaf = rightLeafEndpoints.get(leftLeaf.name());
            if (rightLeaf == null) {
                continue;
            }
            double deltaX = Math.max(36.0d, (rightLeaf.connectorX() - leftLeaf.connectorX()) * 0.24d);
            CubicCurve2D.Double curve = new CubicCurve2D.Double(
                    leftLeaf.connectorX(),
                    leftLeaf.y(),
                    leftLeaf.connectorX() + deltaX,
                    leftLeaf.y(),
                    rightLeaf.connectorX() - deltaX,
                    rightLeaf.y(),
                    rightLeaf.connectorX(),
                    rightLeaf.y());
            connectorGraphics.draw(curve);
        }
        connectorGraphics.dispose();
    }

    private void drawLeafLabels(Graphics2D graphics2d, Map<String, LeafEndpoint> leafEndpoints, boolean leftSide) {
        if (!renderOptions.showLeafLabels()) {
            return;
        }
        Graphics2D labelGraphics = (Graphics2D) graphics2d.create();
        labelGraphics.setFont(labelFont);
        labelGraphics.setColor(new Color(42, 47, 54));
        for (LeafEndpoint endpoint : leafEndpoints.values()) {
            labelGraphics.drawString(endpoint.name(), endpoint.labelX(), endpoint.baselineY());
            double guideStartX = leftSide ? endpoint.connectorX() - CONNECTOR_LABEL_GAP + 2.0d : endpoint.connectorX() + CONNECTOR_LABEL_GAP - 2.0d;
            double guideEndX = leftSide ? endpoint.connectorX() - 3.0d : endpoint.connectorX() + 3.0d;
            labelGraphics.setColor(new Color(165, 173, 182, 120));
            labelGraphics.draw(new Line2D.Double(guideStartX, endpoint.y(), guideEndX, endpoint.y()));
            labelGraphics.setColor(new Color(42, 47, 54));
        }
        labelGraphics.dispose();
    }

    @SuppressWarnings("unchecked")
    private void drawTreeStructure(Graphics2D graphics2d, ReflectGraphicNode<EvolNode> node) {
        Graphics2D treeGraphics = (Graphics2D) graphics2d.create();
        treeGraphics.setColor(new Color(20, 24, 30));
        treeGraphics.setStroke(TREE_STROKE);
        treeGraphics.setFont(branchLengthFont);

        double xSelf = node.getXSelf();
        double ySelf = node.getYSelf();
        double xParent = node.getXParent();
        int childCount = node.getChildCount();
        if (node.getParent() != null) {
            treeGraphics.draw(new Line2D.Double(xParent, ySelf, xSelf, ySelf));
            String lengthString = formatBranchLength(node.getReflectNode().getLength());
            int textWidth = treeGraphics.getFontMetrics().stringWidth(lengthString);
            int textX = (int) Math.round(((xSelf + xParent) / 2.0d) - (textWidth / 2.0d));
            int textY = (int) Math.round(ySelf - 4.0d);
            treeGraphics.drawString(lengthString, textX, textY);
        }
        if (childCount > 0) {
            ReflectGraphicNode<EvolNode> firstChild = (ReflectGraphicNode<EvolNode>) node.getFirstChild();
            ReflectGraphicNode<EvolNode> lastChild = (ReflectGraphicNode<EvolNode>) node.getLastChild();
            treeGraphics.draw(new Line2D.Double(xSelf, firstChild.getYSelf(), xSelf, lastChild.getYSelf()));
            for (int index = 0; index < childCount; index++) {
                drawTreeStructure(graphics2d, (ReflectGraphicNode<EvolNode>) node.getChildAt(index));
            }
        }
        treeGraphics.dispose();
    }

    private Stroke createConnectorStroke() {
        return new BasicStroke(
                Math.max(0.5f, renderOptions.connectorStrokeWidth()),
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND,
                1.0f,
                new float[] {
                    Math.max(1.0f, renderOptions.connectorDashLength()),
                    Math.max(1.0f, renderOptions.connectorDashGap())
                },
                0.0f);
    }

    private static String formatBranchLength(double length) {
        if (Double.isNaN(length) || Double.isInfinite(length)) {
            return "0.0";
        }
        String raw = String.valueOf(length);
        return raw.length() > 8 ? raw.substring(0, 8) : raw;
    }

    private record LeafEndpoint(String name, double connectorX, double y, int labelX, int baselineY) {
    }

    private record Layout(
            ReflectGraphicNode<EvolNode> leftRoot,
            ReflectGraphicNode<EvolNode> rightRoot,
            Map<String, LeafEndpoint> leftLeafEndpoints,
            Map<String, LeafEndpoint> rightLeafEndpoints) {
    }

    private static final class HorizontalTreeLayoutCalculator {
        private final boolean mirror;
        private final int startX;
        private final int endX;
        private final int topY;
        private final int treeHeight;
        private double maxDepth;
        private double yStep;
        private int leafIndex;

        private HorizontalTreeLayoutCalculator(boolean mirror, int startX, int endX, int topY, int treeHeight) {
            this.mirror = mirror;
            this.startX = startX;
            this.endX = endX;
            this.topY = topY;
            this.treeHeight = treeHeight;
        }

        <T extends EvolNode> void calculate(ReflectGraphicNode<T> root) {
            leafIndex = 0;
            maxDepth = maxDepth(root, 0.0d);
            if (maxDepth <= 0.0d) {
                EvolNodeUtil.recursiveIterateTreeIF(root, node -> node.getReflectNode().setLength(node.getParent() == null ? 0.0d : 1.0d));
                maxDepth = maxDepth(root, 0.0d);
            }
            int leafCount = Math.max(1, EvolNodeUtil.getLeaves(root).size());
            yStep = leafCount == 1 ? 0.0d : (double) treeHeight / (leafCount - 1);
            assign(root, 0.0d);
        }

        @SuppressWarnings("unchecked")
        private <T extends EvolNode> void assign(ReflectGraphicNode<T> node, double depthBeforeNode) {
            double nextDepth = depthBeforeNode + Math.max(0.0d, node.getReflectNode().getLength());
            double ratio = maxDepth <= 0.0d ? 0.0d : nextDepth / maxDepth;
            double x = mirror
                    ? endX - ((endX - startX) * ratio)
                    : startX + ((endX - startX) * ratio);
            node.setXSelf(x);
            ReflectGraphicNode<T> parent = (ReflectGraphicNode<T>) node.getParent();
            node.setXParent(parent == null ? x : parent.getXSelf());

            int childCount = node.getChildCount();
            if (childCount == 0) {
                double y = topY + (leafIndex * yStep);
                node.setYSelf(y);
                node.setYParent(y);
                leafIndex++;
                return;
            }

            double y = 0.0d;
            for (int index = 0; index < childCount; index++) {
                ReflectGraphicNode<T> child = (ReflectGraphicNode<T>) node.getChildAt(index);
                assign(child, nextDepth);
                y += child.getYSelf();
            }
            y /= childCount;
            node.setYSelf(y);
            node.setYParent(y);
        }

        @SuppressWarnings("unchecked")
        private <T extends EvolNode> double maxDepth(ReflectGraphicNode<T> node, double depth) {
            double nextDepth = depth + Math.max(0.0d, node.getReflectNode().getLength());
            if (node.getChildCount() == 0) {
                return nextDepth;
            }
            double localMaxDepth = nextDepth;
            for (int index = 0; index < node.getChildCount(); index++) {
                localMaxDepth = Math.max(localMaxDepth, maxDepth((ReflectGraphicNode<T>) node.getChildAt(index), nextDepth));
            }
            return localMaxDepth;
        }
    }
}
