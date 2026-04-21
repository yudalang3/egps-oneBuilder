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
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

final class ThreeDTreeAlignmentView extends JPanel implements ExportableView {
    private static final int RENDER_DELAY_MS = 120;
    private static final int HORIZONTAL_MARGIN = 44;
    private static final int VERTICAL_MARGIN = 34;
    private static final int SHEET_GAP = 34;
    private static final double SHEAR_Y = -0.28d;
    private static final int CORNER_ARC = 28;
    private static final int SHADOW_OFFSET_X = 12;
    private static final int SHADOW_OFFSET_Y = 10;
    private static final int MIN_SHEET_WIDTH = 120;
    private static final int MIN_SHEET_HEIGHT = 220;
    private static final int FLOOR_DEPTH_X = 150;
    private static final int FLOOR_DEPTH_Y = 90;
    private static final int SHEET_TITLE_HEIGHT = 18;
    private static final int LABEL_BAND_HEIGHT = 74;
    private static final int CONTENT_PADDING_X = 20;
    private static final int CONTENT_PADDING_TOP = 20;

    private final List<ImportedTreeSpec> importedTrees;
    private final Timer renderTimer;
    private final AtomicLong renderSequence;
    private volatile List<PreparedLayer> preparedLayers;
    private volatile String errorMessage;
    private volatile boolean loading;

    ThreeDTreeAlignmentView(List<ImportedTreeSpec> importedTrees) {
        this.importedTrees = List.copyOf(importedTrees);
        this.renderTimer = new Timer(RENDER_DELAY_MS, event -> renderForCurrentSize());
        this.renderSequence = new AtomicLong();
        this.preparedLayers = List.of();
        this.loading = true;
        this.renderTimer.setRepeats(false);
        setOpaque(true);
        setBackground(Color.WHITE);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                scheduleRender();
            }
        });
        SwingUtilities.invokeLater(this::scheduleRender);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D graphics2d = (Graphics2D) graphics.create();
        graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (loading) {
            paintCenteredMessage(graphics2d, "Preparing 3D tree alignment...", new Color(94, 112, 137));
            graphics2d.dispose();
            return;
        }
        if (errorMessage != null) {
            paintCenteredMessage(graphics2d, errorMessage, new Color(184, 41, 41));
            graphics2d.dispose();
            return;
        }

        paintFloorShadow(graphics2d, preparedLayers);
        for (PreparedLayer preparedLayer : preparedLayers) {
            paintLayer(graphics2d, preparedLayer);
        }
        graphics2d.dispose();
    }

    private void scheduleRender() {
        renderTimer.restart();
    }

    private void renderForCurrentSize() {
        final long renderId = renderSequence.incrementAndGet();
        final Dimension viewportSize = currentViewportSize();
        loading = true;
        errorMessage = null;
        repaint();
        Thread renderThread = new Thread(() -> {
            try {
                List<PreparedLayer> layers = prepareLayers(viewportSize);
                SwingUtilities.invokeLater(() -> {
                    if (renderId != renderSequence.get()) {
                        return;
                    }
                    preparedLayers = layers;
                    errorMessage = null;
                    loading = false;
                    repaint();
                });
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> {
                    if (renderId != renderSequence.get()) {
                        return;
                    }
                    preparedLayers = List.of();
                    errorMessage = exception.getMessage() == null ? "3D tree alignment could not be rendered." : exception.getMessage();
                    loading = false;
                    repaint();
                });
            }
        }, "tanglegram-3d-renderer");
        renderThread.setDaemon(true);
        renderThread.start();
    }

    private Dimension currentViewportSize() {
        Dimension size = getSize();
        if (size.width <= 0 || size.height <= 0) {
            return new Dimension(1320, 920);
        }
        return new Dimension(size);
    }

    private List<PreparedLayer> prepareLayers(Dimension viewportSize) {
        if (importedTrees.isEmpty()) {
            throw new IllegalStateException("No trees are available for 3D alignment.");
        }

        int layerCount = importedTrees.size();
        int availableWidth = Math.max(1, viewportSize.width - (HORIZONTAL_MARGIN * 2) - (SHEET_GAP * Math.max(0, layerCount - 1)));
        int availableHeight = Math.max(1, viewportSize.height - (VERTICAL_MARGIN * 2) - FLOOR_DEPTH_Y);
        int sheetWidth = Math.max(1, availableWidth / Math.max(1, layerCount));
        int shearRise = (int) Math.ceil(Math.abs(SHEAR_Y * sheetWidth));
        int maxHeight = Math.max(1, availableHeight - shearRise);
        int preferredHeight = (int) Math.round(sheetWidth * 2.0d);
        int sheetHeight = Math.min(maxHeight, preferredHeight);
        if (sheetWidth < MIN_SHEET_WIDTH || sheetHeight < MIN_SHEET_HEIGHT) {
            throw new IllegalStateException("The current window is too small to render the 3D tree alignment.");
        }

        int totalWidth = (sheetWidth * layerCount) + (SHEET_GAP * Math.max(0, layerCount - 1));
        int startX = HORIZONTAL_MARGIN + Math.max(0, (viewportSize.width - (HORIZONTAL_MARGIN * 2) - totalWidth) / 2);
        int startY = VERTICAL_MARGIN + shearRise + Math.max(0, (availableHeight - shearRise - sheetHeight) / 2 - 8);

        int contentX = CONTENT_PADDING_X;
        int contentY = CONTENT_PADDING_TOP + SHEET_TITLE_HEIGHT;
        int treeWidth = Math.max(70, sheetWidth - (CONTENT_PADDING_X * 2));
        int treeHeight = Math.max(90, sheetHeight - contentY - LABEL_BAND_HEIGHT - 12);
        int labelBaseY = treeHeight + LABEL_BAND_HEIGHT - 10;

        List<PreparedLayer> layers = new ArrayList<>(layerCount);
        for (int index = 0; index < layerCount; index++) {
            ImportedTreeSpec importedTree = importedTrees.get(index);
            if (importedTree.root() == null) {
                throw new IllegalStateException("The tree '" + importedTree.label() + "' is not loaded in memory yet.");
            }
            EvolNode copiedRoot = TreeDataLoader.copyTree(importedTree.root());
            ReflectGraphicNode<EvolNode> graphicRoot = new ReflectGraphicNode<>(copiedRoot);
            SingleTreeLayoutCalculator calculator = new SingleTreeLayoutCalculator();
            calculator.calculateTree(graphicRoot, new Dimension(treeHeight, treeWidth));
            int sheetX = startX + (index * (sheetWidth + SHEET_GAP));
            int sheetY = startY;
            layers.add(new PreparedLayer(
                    importedTree.label(),
                    graphicRoot,
                    sheetX,
                    sheetY,
                    sheetWidth,
                    sheetHeight,
                    contentX,
                    contentY,
                    treeWidth,
                    treeHeight,
                    labelBaseY,
                    sheetFillColor(index),
                    sheetBorderColor(index),
                    sheetShadowColor(index)));
        }
        return layers;
    }

    private static void paintFloorShadow(Graphics2D graphics2d, List<PreparedLayer> layers) {
        if (layers.isEmpty()) {
            return;
        }
        PreparedLayer first = layers.get(0);
        PreparedLayer last = layers.get(layers.size() - 1);
        int topLeftX = first.x() - 32;
        int topLeftY = first.y() + first.sheetHeight() - 4;
        int topRightX = last.x() + last.sheetWidth() + 40;
        int topRightY = topLeftY + (int) Math.round(SHEAR_Y * (topRightX - topLeftX));

        Polygon floor = new Polygon(
                new int[] { topLeftX, topRightX, topRightX + FLOOR_DEPTH_X, topLeftX + FLOOR_DEPTH_X },
                new int[] { topLeftY, topRightY, topRightY + FLOOR_DEPTH_Y, topLeftY + FLOOR_DEPTH_Y },
                4);
        graphics2d.setColor(new Color(205, 211, 218, 108));
        graphics2d.fillPolygon(floor);
    }

    private static void paintLayer(Graphics2D graphics2d, PreparedLayer preparedLayer) {
        Graphics2D shadowGraphics = (Graphics2D) graphics2d.create();
        shadowGraphics.translate(preparedLayer.x() + SHADOW_OFFSET_X, preparedLayer.y() + SHADOW_OFFSET_Y);
        shadowGraphics.shear(0.0d, SHEAR_Y);
        shadowGraphics.setColor(preparedLayer.shadowColor());
        shadowGraphics.fill(new RoundRectangle2D.Double(0, 0, preparedLayer.sheetWidth(), preparedLayer.sheetHeight(), CORNER_ARC, CORNER_ARC));
        shadowGraphics.dispose();

        Graphics2D layerGraphics = (Graphics2D) graphics2d.create();
        layerGraphics.translate(preparedLayer.x(), preparedLayer.y());
        layerGraphics.shear(0.0d, SHEAR_Y);

        RoundRectangle2D.Double sheetShape = new RoundRectangle2D.Double(0, 0, preparedLayer.sheetWidth(), preparedLayer.sheetHeight(), CORNER_ARC, CORNER_ARC);
        layerGraphics.setColor(preparedLayer.fillColor());
        layerGraphics.fill(sheetShape);
        layerGraphics.setColor(preparedLayer.borderColor());
        layerGraphics.setStroke(new BasicStroke(1.15f));
        layerGraphics.draw(sheetShape);

        layerGraphics.setFont(resolveTitleFont());
        layerGraphics.setColor(new Color(92, 102, 114, 170));
        layerGraphics.drawString(preparedLayer.label(), preparedLayer.contentX(), CONTENT_PADDING_TOP + 2);

        Graphics2D treeGraphics = (Graphics2D) layerGraphics.create();
        Shape previousClip = treeGraphics.getClip();
        treeGraphics.setClip(sheetShape);
        treeGraphics.translate(preparedLayer.contentX(), preparedLayer.contentY());
        drawStandingTree(
                treeGraphics,
                preparedLayer.root(),
                preparedLayer.treeHeight(),
                preparedLayer.labelBaseY());
        treeGraphics.setClip(previousClip);
        treeGraphics.dispose();
        layerGraphics.dispose();
    }

    private static void paintCenteredMessage(Graphics2D graphics2d, String message, Color color) {
        graphics2d.setColor(color);
        FontMetrics fontMetrics = graphics2d.getFontMetrics();
        String safeMessage = message == null ? " " : message;
        int textWidth = fontMetrics.stringWidth(safeMessage);
        int x = Math.max(12, (graphics2d.getClipBounds().width - textWidth) / 2);
        int y = Math.max(fontMetrics.getHeight(), graphics2d.getClipBounds().height / 2);
        graphics2d.drawString(safeMessage, x, y);
    }

    @SuppressWarnings("unchecked")
    private static void drawStandingTree(
            Graphics2D graphics2d,
            ReflectGraphicNode<EvolNode> node,
            int treeHeight,
            int labelBaseY) {
        int childCount = node.getChildCount();
        Stroke originalStroke = graphics2d.getStroke();
        graphics2d.setStroke(new BasicStroke(1.1f));
        graphics2d.setColor(treeLineColor());

        double xSelf = node.getYSelf();
        double ySelf = node.getXSelf();
        double xParent = node.getYParent();
        double yParent = node.getXParent();

        Line2D.Double line = new Line2D.Double(xSelf, ySelf, xParent, yParent);
        graphics2d.draw(line);
        if (childCount > 0) {
            ReflectGraphicNode<EvolNode> firstChild = (ReflectGraphicNode<EvolNode>) node.getFirstChild();
            ReflectGraphicNode<EvolNode> lastChild = (ReflectGraphicNode<EvolNode>) node.getLastChild();
            line.setLine(firstChild.getYParent(), firstChild.getXParent(), lastChild.getYParent(), lastChild.getXParent());
            graphics2d.draw(line);
        }

        if (node.getChildCount() == 0) {
            String name = node.getReflectNode().getName();
            Graphics2D labelGraphics = (Graphics2D) graphics2d.create();
            labelGraphics.setFont(resolveLeafLabelFont());
            labelGraphics.setColor(treeLabelColor());
            labelGraphics.translate(node.getYSelf() - 2.0d, labelBaseY);
            labelGraphics.rotate(-Math.PI / 2.0d);
            labelGraphics.drawString(name, 0, 0);
            labelGraphics.dispose();

            graphics2d.setColor(treeLineColor());
            line.setLine(node.getYSelf(), treeHeight + 4.0d, node.getYSelf(), node.getXSelf());
            graphics2d.draw(line);
        }

        graphics2d.setStroke(originalStroke);

        for (int index = 0; index < childCount; index++) {
            ReflectGraphicNode<EvolNode> child = (ReflectGraphicNode<EvolNode>) node.getChildAt(index);
            drawStandingTree(graphics2d, child, treeHeight, labelBaseY);
        }
    }

    private static Font resolveLeafLabelFont() {
        Font uiFont = UIManager.getFont("Label.font");
        int fontSize = Math.max(9, UiPreferenceStore.load().defaultTanglegramLabelFontSize() - 2);
        if (uiFont != null) {
            return uiFont.deriveFont((float) fontSize);
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
    }

    private static Font resolveTitleFont() {
        Font uiFont = UIManager.getFont("Label.font");
        int fontSize = Math.max(11, UiPreferenceStore.load().defaultTanglegramLabelFontSize() - 1);
        if (uiFont != null) {
            return uiFont.deriveFont(Font.BOLD, (float) fontSize);
        }
        return new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
    }

    private static Color treeLineColor() {
        return new Color(88, 96, 107, 180);
    }

    private static Color treeLabelColor() {
        return new Color(96, 103, 113, 170);
    }

    private static Color sheetFillColor(int index) {
        return new Color(255, 255, 255, 28);
    }

    private static Color sheetBorderColor(int index) {
        return new Color(130, 138, 149, 138);
    }

    private static Color sheetShadowColor(int index) {
        return new Color(125, 135, 146, 18);
    }

    @Override
    public JComponent getExportComponent() {
        return this;
    }

    @Override
    public boolean canExport() {
        return !loading && errorMessage == null;
    }

    private record PreparedLayer(
            String label,
            ReflectGraphicNode<EvolNode> root,
            int x,
            int y,
            int sheetWidth,
            int sheetHeight,
            int contentX,
            int contentY,
            int treeWidth,
            int treeHeight,
            int labelBaseY,
            Color fillColor,
            Color borderColor,
            Color shadowColor) {
    }

    private static final class SingleTreeLayoutCalculator {
        private static final int BLANK_LENGTH = 30;
        private double heightUnit;
        private int heightIndex;

        <T extends EvolNode> void calculateTree(ReflectGraphicNode<T> root, Dimension dimension) {
            heightIndex = 0;
            double maxDepth = maxDepth(root, 0.0d);
            if (maxDepth <= 0.0d) {
                EvolNodeUtil.recursiveIterateTreeIF(root, node -> node.setLength(1.0d));
                root.setLength(0.0d);
                maxDepth = maxDepth(root, 0.0d);
            }

            int leafCount = Math.max(1, EvolNodeUtil.getLeaves(root).size());
            double usableWidth = Math.max(1.0d, dimension.getWidth() - (BLANK_LENGTH * 2.0d));
            double usableHeight = Math.max(1.0d, dimension.getHeight() - (BLANK_LENGTH * 2.0d));
            heightUnit = usableHeight / leafCount;
            double widthRatio = maxDepth <= 0.0d ? 1.0d : usableWidth / maxDepth;
            iterateTree2assignLocation(root, widthRatio, -root.getLength());
        }

        @SuppressWarnings("unchecked")
        private <T extends EvolNode> void iterateTree2assignLocation(
                ReflectGraphicNode<T> node,
                double widthRatio,
                double depth) {
            depth += node.getLength();
            double x = (widthRatio * depth) + BLANK_LENGTH;
            node.setXSelf(x);

            ReflectGraphicNode<T> parent = (ReflectGraphicNode<T>) node.getParent();
            if (parent == null) {
                node.setXParent(x - 10.0d);
            } else {
                node.setXParent(parent.getXSelf());
            }

            int childCount = node.getChildCount();
            if (childCount > 0) {
                double y = 0.0d;
                for (int index = 0; index < childCount; index++) {
                    ReflectGraphicNode<T> child = EvolNodeUtil.getChildrenAt(node, index);
                    iterateTree2assignLocation(child, widthRatio, depth);
                    y += child.getYSelf();
                }
                y /= childCount;
                node.setYSelf(y);
                node.setYParent(y);
                return;
            }

            double y = BLANK_LENGTH + (heightIndex * heightUnit);
            node.setYSelf(y);
            node.setYParent(y);
            heightIndex++;
        }

        @SuppressWarnings("unchecked")
        private <T extends EvolNode> double maxDepth(ReflectGraphicNode<T> node, double depth) {
            double nextDepth = depth + node.getReflectNode().getLength();
            int childCount = node.getChildCount();
            if (childCount == 0) {
                return nextDepth;
            }

            double maxDepth = nextDepth;
            for (int index = 0; index < childCount; index++) {
                ReflectGraphicNode<T> child = (ReflectGraphicNode<T>) node.getChildAt(index);
                maxDepth = Math.max(maxDepth, maxDepth(child, nextDepth));
            }
            return maxDepth;
        }
    }
}
