package tanglegram;

import evoltree.struct.EvolNode;
import evoltree.struct.util.EvolNodeUtil;
import evoltree.txtdisplay.ReflectGraphicNode;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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
    private static final int SHEET_TITLE_HEIGHT = 18;
    private static final int LABEL_BAND_HEIGHT = 74;
    private static final int CONTENT_PADDING_X = 20;
    private static final int CONTENT_PADDING_TOP = 20;

    private final AlignmentCanvas canvas;
    private final Timer renderTimer;
    private final AtomicLong renderSequence;
    private List<ImportedTreeSpec> displayedTrees;
    private List<ConsistencyAnnotation> consistencyAnnotations;
    private boolean usingDefaultRootAnnotation;
    private volatile List<PreparedLayer> preparedLayers;
    private volatile String errorMessage;
    private volatile boolean loading;

    ThreeDTreeAlignmentView(List<ImportedTreeSpec> importedTrees) {
        super(new BorderLayout(0, 8));
        this.displayedTrees = new ArrayList<>(importedTrees == null ? List.of() : importedTrees);
        this.consistencyAnnotations = defaultRootAnnotations(displayedTrees);
        this.usingDefaultRootAnnotation = true;
        this.canvas = new AlignmentCanvas();
        this.renderTimer = new Timer(RENDER_DELAY_MS, event -> renderForCurrentSize());
        this.renderSequence = new AtomicLong();
        this.preparedLayers = List.of();
        this.loading = true;
        this.renderTimer.setRepeats(false);
        setOpaque(false);
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                scheduleRender();
            }
        });
        add(canvas, BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.SOUTH);
        SwingUtilities.invokeLater(this::scheduleRender);
    }

    private void scheduleRender() {
        renderTimer.restart();
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        JButton treeOrderButton = new JButton("Tree order");
        treeOrderButton.setToolTipText("Reorder the trees in this 3D alignment view without changing the imported data or tree files.");
        treeOrderButton.addActionListener(event -> openTreeOrderDialog());

        JButton consistencyAnnotationButton = new JButton("Consistency annotation");
        consistencyAnnotationButton.setToolTipText(
                "Connect clades or clusters that contain exactly the same leaf set across the aligned trees using translucent Sankey ribbons.");
        consistencyAnnotationButton.addActionListener(event -> openConsistencyAnnotationDialog());

        panel.add(treeOrderButton);
        panel.add(consistencyAnnotationButton);
        return panel;
    }

    List<ConsistencyAnnotation> consistencyAnnotationsForTest() {
        return List.copyOf(consistencyAnnotations);
    }

    List<String> displayedTreeLabelsForTest() {
        List<String> labels = new ArrayList<>();
        for (ImportedTreeSpec tree : displayedTrees) {
            labels.add(tree.label());
        }
        return labels;
    }

    private void openTreeOrderDialog() {
        ThreeDTreeOrderDialog.showDialog(
                SwingUtilities.getWindowAncestor(this),
                displayedTrees,
                updatedOrder -> {
                    displayedTrees = new ArrayList<>(updatedOrder);
                    if (usingDefaultRootAnnotation) {
                        consistencyAnnotations = defaultRootAnnotations(displayedTrees);
                    }
                    scheduleRender();
                });
    }

    private void openConsistencyAnnotationDialog() {
        ConsistencyAnnotationDialog.showDialog(
                SwingUtilities.getWindowAncestor(this),
                consistencyAnnotations,
                updatedAnnotations -> {
                    consistencyAnnotations = List.copyOf(updatedAnnotations);
                    usingDefaultRootAnnotation = false;
                    canvas.repaint();
                });
    }

    private void renderForCurrentSize() {
        final long renderId = renderSequence.incrementAndGet();
        final Dimension viewportSize = currentViewportSize();
        loading = true;
        errorMessage = null;
        canvas.repaint();
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
                    canvas.repaint();
                });
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> {
                    if (renderId != renderSequence.get()) {
                        return;
                    }
                    preparedLayers = List.of();
                    errorMessage = exception.getMessage() == null ? "3D tree alignment could not be rendered." : exception.getMessage();
                    loading = false;
                    canvas.repaint();
                });
            }
        }, "tanglegram-3d-renderer");
        renderThread.setDaemon(true);
        renderThread.start();
    }

    private Dimension currentViewportSize() {
        Dimension size = canvas.getSize();
        if (size.width <= 0 || size.height <= 0) {
            return new Dimension(1320, 920);
        }
        return new Dimension(size);
    }

    private List<PreparedLayer> prepareLayers(Dimension viewportSize) {
        if (displayedTrees.isEmpty()) {
            throw new IllegalStateException("No trees are available for 3D alignment.");
        }

        int layerCount = displayedTrees.size();
        int availableWidth = Math.max(1, viewportSize.width - (HORIZONTAL_MARGIN * 2) - (SHEET_GAP * Math.max(0, layerCount - 1)));
        int availableHeight = Math.max(1, viewportSize.height - (VERTICAL_MARGIN * 2) - 90);
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
            ImportedTreeSpec importedTree = displayedTrees.get(index);
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
        int leftBottomLeftX = first.x();
        int leftBottomLeftY = first.y() + first.sheetHeight();
        int rightBottomLeftX = last.x();
        int rightBottomLeftY = last.y() + last.sheetHeight();

        int rightBottomRightX = last.x() + last.sheetWidth();
        int rightBottomRightY = last.y() + last.sheetHeight() + (int) Math.round(SHEAR_Y * last.sheetWidth());
        int leftBottomRightX = first.x() + first.sheetWidth();
        int leftBottomRightY = first.y() + first.sheetHeight() + (int) Math.round(SHEAR_Y * first.sheetWidth());

        Polygon floor = new Polygon(
                new int[] { leftBottomLeftX, rightBottomLeftX, rightBottomRightX, leftBottomRightX },
                new int[] { leftBottomLeftY, rightBottomLeftY, rightBottomRightY, leftBottomRightY },
                4);
        graphics2d.setColor(new Color(205, 211, 218, 88));
        graphics2d.fillPolygon(floor);
    }

    private static void paintLayerShadow(Graphics2D graphics2d, PreparedLayer preparedLayer) {
        Graphics2D shadowGraphics = (Graphics2D) graphics2d.create();
        shadowGraphics.translate(preparedLayer.x() + SHADOW_OFFSET_X, preparedLayer.y() + SHADOW_OFFSET_Y);
        shadowGraphics.shear(0.0d, SHEAR_Y);
        shadowGraphics.setColor(preparedLayer.shadowColor());
        shadowGraphics.fill(new RoundRectangle2D.Double(0, 0, preparedLayer.sheetWidth(), preparedLayer.sheetHeight(), CORNER_ARC, CORNER_ARC));
        shadowGraphics.dispose();
    }

    private static void paintLayerSheet(Graphics2D graphics2d, PreparedLayer preparedLayer) {
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

        layerGraphics.dispose();
    }

    private static void paintLayerTree(Graphics2D graphics2d, PreparedLayer preparedLayer) {
        Graphics2D layerGraphics = (Graphics2D) graphics2d.create();
        layerGraphics.translate(preparedLayer.x(), preparedLayer.y());
        layerGraphics.shear(0.0d, SHEAR_Y);

        RoundRectangle2D.Double sheetShape = new RoundRectangle2D.Double(0, 0, preparedLayer.sheetWidth(), preparedLayer.sheetHeight(), CORNER_ARC, CORNER_ARC);

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

    private static void paintConsistencyAnnotations(
            Graphics2D graphics2d,
            List<PreparedLayer> layers,
            List<ConsistencyAnnotation> annotations) {
        if (layers.size() < 2 || annotations == null || annotations.isEmpty()) {
            return;
        }
        Graphics2D ribbonGraphics = (Graphics2D) graphics2d.create();
        ribbonGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (ConsistencyAnnotation annotation : annotations) {
            List<Point2D.Double> anchors = new ArrayList<>(layers.size());
            Set<String> targetLeafNames = new HashSet<>(annotation.leafNames());
            for (PreparedLayer layer : layers) {
                ReflectGraphicNode<EvolNode> matchingNode = findExactCladeNode(layer.root(), targetLeafNames);
                anchors.add(matchingNode == null ? null : globalAnchorPoint(layer, matchingNode));
            }
            paintAnnotationRibbonSegments(ribbonGraphics, anchors, annotation.color(), annotation.ribbonWidth());
        }
        ribbonGraphics.dispose();
    }

    private static void paintConsistencyAnnotationMarkers(
            Graphics2D graphics2d,
            List<PreparedLayer> layers,
            List<ConsistencyAnnotation> annotations) {
        if (layers.isEmpty() || annotations == null || annotations.isEmpty()) {
            return;
        }
        Graphics2D markerGraphics = (Graphics2D) graphics2d.create();
        markerGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (ConsistencyAnnotation annotation : annotations) {
            Set<String> targetLeafNames = new HashSet<>(annotation.leafNames());
            for (PreparedLayer layer : layers) {
                ReflectGraphicNode<EvolNode> matchingNode = findExactCladeNode(layer.root(), targetLeafNames);
                if (matchingNode != null) {
                    paintAnnotationMarker(markerGraphics, globalAnchorPoint(layer, matchingNode), annotation.color(), annotation.ribbonWidth());
                }
            }
        }
        markerGraphics.dispose();
    }

    private static void paintAnnotationRibbonSegments(
            Graphics2D graphics2d,
            List<Point2D.Double> anchors,
            Color color,
            double width) {
        for (int index = 0; index < anchors.size() - 1; index++) {
            Point2D.Double left = anchors.get(index);
            Point2D.Double right = anchors.get(index + 1);
            if (left == null || right == null) {
                continue;
            }
            paintSankeyRibbon(graphics2d, left, right, color, width);
        }
    }

    private static void paintSankeyRibbon(
            Graphics2D graphics2d,
            Point2D.Double left,
            Point2D.Double right,
            Color color,
            double width) {
        double halfWidth = width / 2.0d;
        double controlDelta = Math.max(44.0d, Math.abs(right.x - left.x) * 0.36d);
        double leftControlX = left.x + controlDelta;
        double rightControlX = right.x - controlDelta;

        Path2D.Double ribbon = new Path2D.Double();
        ribbon.moveTo(left.x, left.y - halfWidth);
        ribbon.curveTo(leftControlX, left.y - halfWidth, rightControlX, right.y - halfWidth, right.x, right.y - halfWidth);
        ribbon.lineTo(right.x, right.y + halfWidth);
        ribbon.curveTo(rightControlX, right.y + halfWidth, leftControlX, left.y + halfWidth, left.x, left.y + halfWidth);
        ribbon.closePath();

        graphics2d.setColor(color);
        graphics2d.fill(ribbon);
        graphics2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(210, color.getAlpha() + 50)));
        graphics2d.setStroke(new BasicStroke(0.8f));
        graphics2d.draw(ribbon);
    }

    private static void paintAnnotationMarker(
            Graphics2D graphics2d,
            Point2D.Double anchor,
            Color color,
            double width) {
        double radius = Math.max(3.5d, Math.min(9.0d, width * 0.85d));
        Ellipse2D.Double marker = new Ellipse2D.Double(
                anchor.x - radius,
                anchor.y - radius,
                radius * 2.0d,
                radius * 2.0d);
        graphics2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(210, color.getAlpha() + 35)));
        graphics2d.fill(marker);
        graphics2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, color.getAlpha() + 85)));
        graphics2d.setStroke(new BasicStroke(1.2f));
        graphics2d.draw(marker);
    }

    private static Point2D.Double globalAnchorPoint(PreparedLayer layer, ReflectGraphicNode<EvolNode> node) {
        double localX = layer.contentX() + node.getYSelf();
        double localY = layer.contentY() + node.getXSelf();
        return new Point2D.Double(
                layer.x() + localX,
                layer.y() + localY + (SHEAR_Y * localX));
    }

    @SuppressWarnings("unchecked")
    private static ReflectGraphicNode<EvolNode> findExactCladeNode(
            ReflectGraphicNode<EvolNode> node,
            Set<String> targetLeafNames) {
        Set<String> nodeLeafNames = collectLeafNames(node);
        if (nodeLeafNames.equals(targetLeafNames)) {
            return node;
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            ReflectGraphicNode<EvolNode> match = findExactCladeNode(
                    (ReflectGraphicNode<EvolNode>) node.getChildAt(index),
                    targetLeafNames);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> collectLeafNames(ReflectGraphicNode<EvolNode> node) {
        Set<String> leafNames = new HashSet<>();
        if (node.getChildCount() == 0) {
            String name = node.getReflectNode().getName();
            if (name != null && !name.trim().isEmpty()) {
                leafNames.add(name.trim());
            }
            return leafNames;
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            leafNames.addAll(collectLeafNames((ReflectGraphicNode<EvolNode>) node.getChildAt(index)));
        }
        return leafNames;
    }

    private static List<ConsistencyAnnotation> defaultRootAnnotations(List<ImportedTreeSpec> importedTrees) {
        if (importedTrees == null || importedTrees.isEmpty() || importedTrees.get(0).root() == null) {
            return List.of();
        }
        List<String> leafNames = collectLeafNames(importedTrees.get(0).root());
        if (leafNames.isEmpty()) {
            return List.of();
        }
        return List.of(new ConsistencyAnnotation(leafNames, new Color(79, 140, 255, 160), ConsistencyAnnotation.DEFAULT_RIBBON_WIDTH));
    }

    private static List<String> collectLeafNames(EvolNode node) {
        List<String> leafNames = new ArrayList<>();
        collectLeafNames(node, leafNames);
        return List.copyOf(leafNames);
    }

    private static void collectLeafNames(EvolNode node, List<String> leafNames) {
        if (node == null) {
            return;
        }
        if (node.getChildCount() == 0) {
            String name = node.getName();
            if (name != null && !name.trim().isEmpty()) {
                leafNames.add(name.trim());
            }
            return;
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            collectLeafNames(EvolNodeUtil.getChildrenAt(node, index), leafNames);
        }
    }

    private final class AlignmentCanvas extends JPanel {
        private AlignmentCanvas() {
            setOpaque(true);
            setBackground(Color.WHITE);
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
                paintLayerShadow(graphics2d, preparedLayer);
            }
            for (PreparedLayer preparedLayer : preparedLayers) {
                paintLayerSheet(graphics2d, preparedLayer);
            }
            paintConsistencyAnnotations(graphics2d, preparedLayers, consistencyAnnotations);
            for (PreparedLayer preparedLayer : preparedLayers) {
                paintLayerTree(graphics2d, preparedLayer);
            }
            paintConsistencyAnnotationMarkers(graphics2d, preparedLayers, consistencyAnnotations);
            graphics2d.dispose();
        }
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
        return Color.BLACK;
    }

    private static Color treeLabelColor() {
        return Color.BLACK;
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
        return canvas;
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
