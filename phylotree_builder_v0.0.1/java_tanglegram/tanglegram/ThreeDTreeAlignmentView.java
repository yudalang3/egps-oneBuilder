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
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
    private static final int MAX_QUICK_LABELS = 10;
    private static final int LEGEND_MAX_ITEMS = 10;
    private static final int LEGEND_ITEM_HEIGHT = 24;
    private static final int LEGEND_ITEM_GAP = 8;
    private static final String LEGEND_WIDTH_REFERENCE_LABEL = "Chimp,Coelacanth,Human,Mouse,Rat";
    private static final double NODE_HIT_RADIUS = 10.0d;
    private static final double BRANCH_HIT_TOLERANCE = 3.0d;

    private final AlignmentCanvas canvas;
    private final JScrollPane scrollPane;
    private final TreeViewportNavigationSupport.Controller navigationController;
    private final Timer renderTimer;
    private final AtomicLong renderSequence;
    private List<ImportedTreeSpec> displayedTrees;
    private List<ConsistencyAnnotation> consistencyAnnotations;
    private boolean usingDefaultRootAnnotation;
    private volatile List<PreparedLayer> preparedLayers;
    private volatile List<PreparedAnnotation> preparedAnnotations;
    private volatile TreeDifferenceMetrics treeDifferenceMetrics;
    private volatile String errorMessage;
    private volatile boolean loading;
    private volatile Dimension preparedCanvasSize;

    ThreeDTreeAlignmentView(List<ImportedTreeSpec> importedTrees) {
        super(new BorderLayout(0, 8));
        this.displayedTrees = new ArrayList<>(importedTrees == null ? List.of() : importedTrees);
        this.consistencyAnnotations = defaultRootAnnotations(displayedTrees);
        this.usingDefaultRootAnnotation = true;
        this.canvas = new AlignmentCanvas();
        this.canvas.setPreferredSize(new Dimension(1320, 920));
        this.scrollPane = new JScrollPane(canvas);
        this.navigationController = TreeViewportNavigationSupport.install(
                canvas,
                scrollPane,
                this::hitAt,
                this::scheduleRender);
        this.renderTimer = new Timer(RENDER_DELAY_MS, event -> renderForCurrentSize());
        this.renderSequence = new AtomicLong();
        this.preparedLayers = List.of();
        this.preparedAnnotations = List.of();
        this.treeDifferenceMetrics = TreeDifferenceMetrics.unavailable();
        this.loading = true;
        this.renderTimer.setRepeats(false);
        setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getViewport().setScrollMode(javax.swing.JViewport.SIMPLE_SCROLL_MODE);
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                scheduleRender();
            }
        });
        add(scrollPane, BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.SOUTH);
        SwingUtilities.invokeLater(this::scheduleRender);
    }

    private void scheduleRender() {
        renderSequence.incrementAndGet();
        loading = true;
        errorMessage = null;
        canvas.repaint();
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

        JButton quickLabelButton = new JButton("Quick label consistency");
        quickLabelButton.setToolTipText(
                "Automatically create up to 10 consistency labels from internal clades in the first tree, then connect exact matches across the ordered trees.");
        quickLabelButton.addActionListener(event -> quickLabelConsistency());

        JButton cleanLabelsButton = new JButton("Clean all labels");
        cleanLabelsButton.setToolTipText(
                "Remove all consistency labels and hide all annotation ribbons, markers, and legend entries from this 3D Alignment view.");
        cleanLabelsButton.addActionListener(event -> cleanAllLabels());

        panel.add(treeOrderButton);
        panel.add(consistencyAnnotationButton);
        panel.add(quickLabelButton);
        panel.add(cleanLabelsButton);
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
                    consistencyAnnotations = resolveConsistencyAnnotations(updatedAnnotations);
                    usingDefaultRootAnnotation = false;
                    scheduleRender();
                });
    }

    void applyConsistencyAnnotationsForTest(List<ConsistencyAnnotation> updatedAnnotations) {
        consistencyAnnotations = resolveConsistencyAnnotations(updatedAnnotations);
        usingDefaultRootAnnotation = false;
        scheduleRender();
    }

    void quickLabelConsistencyForTest() {
        quickLabelConsistency();
    }

    void cleanAllLabelsForTest() {
        cleanAllLabels();
    }

    private void quickLabelConsistency() {
        consistencyAnnotations = quickLabelsFromFirstTree(displayedTrees);
        usingDefaultRootAnnotation = false;
        scheduleRender();
    }

    private List<ConsistencyAnnotation> resolveConsistencyAnnotations(List<ConsistencyAnnotation> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return List.of();
        }
        List<ConsistencyAnnotation> resolvedAnnotations = new ArrayList<>(annotations.size());
        for (ConsistencyAnnotation annotation : annotations) {
            if (annotation != null) {
                resolvedAnnotations.add(resolveConsistencyAnnotation(annotation));
            }
        }
        return List.copyOf(resolvedAnnotations);
    }

    private ConsistencyAnnotation resolveConsistencyAnnotation(ConsistencyAnnotation annotation) {
        if (annotation.leafNames().size() != 1) {
            return annotation;
        }
        String nodeName = annotation.leafNames().get(0);
        List<String> resolvedLeafNames = findNamedInternalNodeLeafNames(displayedTrees, nodeName);
        if (resolvedLeafNames.isEmpty()) {
            return annotation;
        }
        return new ConsistencyAnnotation(resolvedLeafNames, annotation.color(), annotation.ribbonWidth());
    }

    private void cleanAllLabels() {
        consistencyAnnotations = List.of();
        preparedAnnotations = List.of();
        usingDefaultRootAnnotation = false;
        scheduleRender();
    }

    private void renderForCurrentSize() {
        final long renderId = renderSequence.incrementAndGet();
        final Dimension viewportSize = currentViewportSize();
        final List<ImportedTreeSpec> treeSnapshot = List.copyOf(displayedTrees);
        final List<ConsistencyAnnotation> annotationSnapshot = List.copyOf(consistencyAnnotations);
        loading = true;
        errorMessage = null;
        canvas.repaint();
        Thread renderThread = new Thread(() -> {
            try {
                PreparedRender preparedRender = prepareRender(viewportSize, treeSnapshot, annotationSnapshot);
                SwingUtilities.invokeLater(() -> {
                    if (renderId != renderSequence.get()) {
                        return;
                    }
                    preparedLayers = preparedRender.layers();
                    preparedAnnotations = preparedRender.annotations();
                    treeDifferenceMetrics = preparedRender.metrics();
                    preparedCanvasSize = new Dimension(viewportSize);
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
                    preparedAnnotations = List.of();
                    treeDifferenceMetrics = TreeDifferenceMetrics.unavailable();
                    preparedCanvasSize = null;
                    errorMessage = exception.getMessage() == null
                            ? exception.getClass().getSimpleName() + ": 3D tree alignment could not be rendered."
                            : exception.getMessage();
                    loading = false;
                    canvas.repaint();
                });
            }
        }, "tanglegram-3d-renderer");
        renderThread.setDaemon(true);
        renderThread.start();
    }

    void renderForCurrentSizeForTest() {
        renderForCurrentSize();
    }

    JScrollPane scrollPaneForTest() {
        return scrollPane;
    }

    Long renderSequenceForTest() {
        return Long.valueOf(renderSequence.get());
    }

    Point firstNodePointForTest() {
        List<PreparedLayer> layers = preparedLayers;
        if (layers.isEmpty()) {
            return new Point(0, 0);
        }
        return roundedPoint(globalAnchorPoint(layers.get(0), layers.get(0).root()));
    }

    String hitSummaryForTest(Point point) {
        TreeViewportNavigationSupport.TreeHit hit = hitAt(point);
        return hit == null ? "" : hit.summaryText();
    }

    String informationTextForTest(Point point) {
        TreeViewportNavigationSupport.TreeHit hit = hitAt(point);
        return hit == null ? "" : hit.detailsText();
    }

    JPopupMenu popupMenuForTest(Point point) {
        return navigationController.createPopupMenu(point);
    }

    int preparedAnnotationCountForTest() {
        return preparedAnnotations.size();
    }

    int preparedAnnotationAnchorCountForTest() {
        int count = 0;
        for (PreparedAnnotation annotation : preparedAnnotations) {
            for (Point2D.Double anchor : annotation.anchors()) {
                if (anchor != null) {
                    count++;
                }
            }
        }
        return count;
    }

    TreeDifferenceMetrics treeDifferenceMetricsForTest() {
        return treeDifferenceMetrics;
    }

    static TreeDifferenceMetrics calculateTreeDifferenceMetricsForTest(List<ImportedTreeSpec> trees) {
        return calculateTreeDifferenceMetrics(trees);
    }

    private Dimension currentViewportSize() {
        Dimension size = canvas.getSize();
        if (size.width <= 0 || size.height <= 0) {
            return new Dimension(1320, 920);
        }
        return new Dimension(size);
    }

    private boolean preparedRenderMatchesCanvasSize() {
        Dimension preparedSize = preparedCanvasSize;
        if (preparedSize == null) {
            return false;
        }
        Dimension currentSize = currentViewportSize();
        return currentSize.width == preparedSize.width && currentSize.height == preparedSize.height;
    }

    private boolean shouldPaintLoadingMessage() {
        return loading && (preparedLayers.isEmpty() || !preparedRenderMatchesCanvasSize());
    }

    private PreparedRender prepareRender(
            Dimension viewportSize,
            List<ImportedTreeSpec> treeSnapshot,
            List<ConsistencyAnnotation> annotationSnapshot) {
        List<PreparedLayer> layers = prepareLayers(viewportSize, treeSnapshot);
        return new PreparedRender(
                layers,
                prepareConsistencyAnnotations(layers, annotationSnapshot),
                calculateTreeDifferenceMetrics(treeSnapshot));
    }

    private List<PreparedLayer> prepareLayers(Dimension viewportSize, List<ImportedTreeSpec> treeSnapshot) {
        if (treeSnapshot.isEmpty()) {
            throw new IllegalStateException("No trees are available for 3D alignment.");
        }

        int layerCount = treeSnapshot.size();
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
            ImportedTreeSpec importedTree = treeSnapshot.get(index);
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
        graphics2d.setColor(new Color(205, 211, 218, 88));
        graphics2d.fillPolygon(floorShadowPolygon(layers));
    }

    private static Polygon floorShadowPolygon(List<PreparedLayer> layers) {
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

        return new Polygon(
                new int[] { leftBottomLeftX, rightBottomLeftX, rightBottomRightX, leftBottomRightX },
                new int[] { leftBottomLeftY, rightBottomLeftY, rightBottomRightY, leftBottomRightY },
                4);
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

    private static List<PreparedAnnotation> prepareConsistencyAnnotations(
            List<PreparedLayer> layers,
            List<ConsistencyAnnotation> annotations) {
        if (layers.isEmpty() || annotations == null || annotations.isEmpty()) {
            return List.of();
        }
        List<PreparedAnnotation> preparedAnnotations = new ArrayList<>(annotations.size());
        for (ConsistencyAnnotation annotation : annotations) {
            List<Point2D.Double> anchors = new ArrayList<>(layers.size());
            Set<String> targetLeafNames = new HashSet<>(annotation.leafNames());
            for (PreparedLayer layer : layers) {
                ReflectGraphicNode<EvolNode> matchingNode = findExactCladeNode(layer.root(), targetLeafNames);
                anchors.add(matchingNode == null ? null : globalAnchorPoint(layer, matchingNode));
            }
            preparedAnnotations.add(new PreparedAnnotation(
                    Collections.unmodifiableList(new ArrayList<>(anchors)),
                    annotation.color(),
                    annotation.ribbonWidth(),
                    annotation.leafNamesText()));
        }
        return List.copyOf(preparedAnnotations);
    }

    private static void paintConsistencyAnnotations(
            Graphics2D graphics2d,
            List<PreparedAnnotation> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return;
        }
        Graphics2D ribbonGraphics = (Graphics2D) graphics2d.create();
        ribbonGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (PreparedAnnotation annotation : annotations) {
            paintAnnotationRibbonSegments(ribbonGraphics, annotation.anchors(), annotation.color(), annotation.ribbonWidth());
        }
        ribbonGraphics.dispose();
    }

    private static void paintConsistencyAnnotationMarkers(
            Graphics2D graphics2d,
            List<PreparedAnnotation> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return;
        }
        Graphics2D markerGraphics = (Graphics2D) graphics2d.create();
        markerGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (PreparedAnnotation annotation : annotations) {
            for (Point2D.Double anchor : annotation.anchors()) {
                if (anchor != null) {
                    paintAnnotationMarker(markerGraphics, anchor, annotation.color(), annotation.ribbonWidth());
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

    private static void paintConsistencyAnnotationLegend(
            Graphics2D graphics2d,
            List<PreparedLayer> layers,
            List<PreparedAnnotation> annotations,
            Dimension canvasSize) {
        if (layers.isEmpty() || annotations == null || annotations.isEmpty()) {
            return;
        }
        Polygon floor = floorShadowPolygon(layers);
        Rectangle floorBounds = floor.getBounds();
        int x = Math.max(HORIZONTAL_MARGIN, floorBounds.x);
        int y = floorBounds.y + floorBounds.height + 14;
        int maxRight = Math.min(canvasSize.width - HORIZONTAL_MARGIN, floorBounds.x + floorBounds.width);
        if (maxRight <= x + 80) {
            maxRight = canvasSize.width - HORIZONTAL_MARGIN;
        }

        Graphics2D legendGraphics = (Graphics2D) graphics2d.create();
        legendGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        legendGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        legendGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        legendGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        legendGraphics.setFont(resolveLegendFont());
        FontMetrics metrics = legendGraphics.getFontMetrics();
        int fixedItemWidth = Math.max(210, metrics.stringWidth(LEGEND_WIDTH_REFERENCE_LABEL) + 34);

        int currentX = x;
        int currentY = y;
        int shownCount = Math.min(LEGEND_MAX_ITEMS, annotations.size());
        for (int index = 0; index < shownCount; index++) {
            PreparedAnnotation annotation = annotations.get(index);
            int itemWidth = fixedItemWidth;
            if (currentX > x && currentX + itemWidth > maxRight) {
                currentX = x;
                currentY += LEGEND_ITEM_HEIGHT + LEGEND_ITEM_GAP;
            }
            paintLegendItem(legendGraphics, annotation, currentX, currentY, itemWidth, metrics);
            currentX += itemWidth + LEGEND_ITEM_GAP;
        }
        if (annotations.size() > LEGEND_MAX_ITEMS) {
            String moreLabel = "+" + (annotations.size() - LEGEND_MAX_ITEMS) + " more";
            int itemWidth = Math.max(76, metrics.stringWidth(moreLabel) + 22);
            if (currentX > x && currentX + itemWidth > maxRight) {
                currentX = x;
                currentY += LEGEND_ITEM_HEIGHT + LEGEND_ITEM_GAP;
            }
            paintMoreLegendItem(legendGraphics, moreLabel, currentX, currentY, itemWidth, metrics);
        }
        legendGraphics.dispose();
    }

    private static void paintTreeDifferenceMetrics(Graphics2D graphics2d, TreeDifferenceMetrics metrics) {
        TreeDifferenceMetrics safeMetrics = metrics == null ? TreeDifferenceMetrics.unavailable() : metrics;
        Graphics2D metricGraphics = (Graphics2D) graphics2d.create();
        metricGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        metricGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        metricGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        metricGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        metricGraphics.setFont(resolveMetricsFont());
        metricGraphics.setColor(Color.BLACK);
        metricGraphics.drawString("Topology difference index : " + formatMetricValue(safeMetrics.topologyDifferenceIndex()), 16, 24);
        metricGraphics.drawString("Branch-length difference index : " + formatMetricValue(safeMetrics.branchLengthDifferenceIndex()), 16, 43);
        metricGraphics.dispose();
    }

    private static String formatMetricValue(double value) {
        if (!Double.isFinite(value)) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%.3f", Math.max(0.0d, Math.min(1.0d, value)));
    }

    private static void paintLegendItem(
            Graphics2D graphics2d,
            PreparedAnnotation annotation,
            int x,
            int y,
            int width,
            FontMetrics metrics) {
        RoundRectangle2D.Double background = new RoundRectangle2D.Double(x, y, width, LEGEND_ITEM_HEIGHT, 12, 12);
        Color color = annotation.color();
        graphics2d.setColor(new Color(255, 255, 255, 220));
        graphics2d.fill(background);
        graphics2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(185, color.getAlpha() + 40)));
        graphics2d.draw(background);

        double markerSize = Math.max(7.0d, Math.min(12.0d, annotation.ribbonWidth() + 3.0d));
        double markerX = x + 12.0d;
        double markerY = y + ((LEGEND_ITEM_HEIGHT - markerSize) / 2.0d);
        Ellipse2D.Double marker = new Ellipse2D.Double(markerX, markerY, markerSize, markerSize);
        graphics2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(210, color.getAlpha() + 35)));
        graphics2d.fill(marker);
        graphics2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, color.getAlpha() + 85)));
        graphics2d.draw(marker);

        graphics2d.setColor(Color.BLACK);
        String clippedLabel = clipLegendLabel(annotation.label(), metrics, width - 34);
        int baseline = y + ((LEGEND_ITEM_HEIGHT - metrics.getHeight()) / 2) + metrics.getAscent();
        graphics2d.drawString(clippedLabel, x + 28, baseline);
    }

    private static void paintMoreLegendItem(
            Graphics2D graphics2d,
            String label,
            int x,
            int y,
            int width,
            FontMetrics metrics) {
        RoundRectangle2D.Double background = new RoundRectangle2D.Double(x, y, width, LEGEND_ITEM_HEIGHT, 12, 12);
        graphics2d.setColor(new Color(246, 248, 252, 230));
        graphics2d.fill(background);
        graphics2d.setColor(new Color(180, 190, 205));
        graphics2d.draw(background);
        graphics2d.setColor(Color.BLACK);
        int baseline = y + ((LEGEND_ITEM_HEIGHT - metrics.getHeight()) / 2) + metrics.getAscent();
        graphics2d.drawString(label, x + 11, baseline);
    }

    private static String clipLegendLabel(String label, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(label) <= maxWidth) {
            return label;
        }
        String suffix = "...";
        int suffixWidth = metrics.stringWidth(suffix);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < label.length(); index++) {
            char next = label.charAt(index);
            if (metrics.stringWidth(builder.toString() + next) + suffixWidth > maxWidth) {
                break;
            }
            builder.append(next);
        }
        return builder + suffix;
    }

    private static Point2D.Double globalAnchorPoint(PreparedLayer layer, ReflectGraphicNode<EvolNode> node) {
        return globalTreePoint(layer, node.getYSelf(), node.getXSelf());
    }

    private static Point2D.Double globalTreePoint(PreparedLayer layer, double treeX, double treeY) {
        double localX = layer.contentX() + treeX;
        double localY = layer.contentY() + treeY;
        return new Point2D.Double(
                layer.x() + localX,
                layer.y() + localY + (SHEAR_Y * localX));
    }

    private TreeViewportNavigationSupport.TreeHit hitAt(Point point) {
        if (point == null || errorMessage != null) {
            return null;
        }
        List<PreparedLayer> layers = preparedLayers;
        for (PreparedLayer layer : layers) {
            TreeViewportNavigationSupport.TreeHit nodeHit = hitLayerNodes(layer, layer.root(), point);
            if (nodeHit != null) {
                return nodeHit;
            }
        }
        for (PreparedLayer layer : layers) {
            TreeViewportNavigationSupport.TreeHit branchHit = hitLayerBranches(layer, layer.root(), point);
            if (branchHit != null) {
                return branchHit;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static TreeViewportNavigationSupport.TreeHit hitLayerNodes(
            PreparedLayer layer,
            ReflectGraphicNode<EvolNode> node,
            Point point) {
        if (globalAnchorPoint(layer, node).distance(point) <= NODE_HIT_RADIUS) {
            return toHit(layer, node);
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            TreeViewportNavigationSupport.TreeHit childHit = hitLayerNodes(
                    layer,
                    (ReflectGraphicNode<EvolNode>) node.getChildAt(index),
                    point);
            if (childHit != null) {
                return childHit;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static TreeViewportNavigationSupport.TreeHit hitLayerBranches(
            PreparedLayer layer,
            ReflectGraphicNode<EvolNode> node,
            Point point) {
        if (node.getParent() != null) {
            Point2D.Double self = globalTreePoint(layer, node.getYSelf(), node.getXSelf());
            Point2D.Double parent = globalTreePoint(layer, node.getYParent(), node.getXParent());
            if (Line2D.ptSegDist(self.x, self.y, parent.x, parent.y, point.x, point.y) <= BRANCH_HIT_TOLERANCE) {
                return toHit(layer, node);
            }
        }

        if (node.getChildCount() > 0) {
            ReflectGraphicNode<EvolNode> firstChild = (ReflectGraphicNode<EvolNode>) node.getFirstChild();
            ReflectGraphicNode<EvolNode> lastChild = (ReflectGraphicNode<EvolNode>) node.getLastChild();
            Point2D.Double first = globalTreePoint(layer, firstChild.getYParent(), firstChild.getXParent());
            Point2D.Double last = globalTreePoint(layer, lastChild.getYParent(), lastChild.getXParent());
            if (Line2D.ptSegDist(first.x, first.y, last.x, last.y, point.x, point.y) <= BRANCH_HIT_TOLERANCE) {
                return toHit(layer, node);
            }
            for (int index = 0; index < node.getChildCount(); index++) {
                TreeViewportNavigationSupport.TreeHit childHit = hitLayerBranches(
                        layer,
                        (ReflectGraphicNode<EvolNode>) node.getChildAt(index),
                        point);
                if (childHit != null) {
                    return childHit;
                }
            }
        }
        return null;
    }

    private static TreeViewportNavigationSupport.TreeHit toHit(
            PreparedLayer layer,
            ReflectGraphicNode<EvolNode> node) {
        EvolNode evolNode = node.getReflectNode();
        return new TreeViewportNavigationSupport.TreeHit(
                "3D Tree Alignment",
                layer.label(),
                evolNode.getName(),
                node.getChildCount() == 0,
                evolNode.getLength(),
                node.getChildCount(),
                leafNameListForNode(node),
                roundedPoint(globalAnchorPoint(layer, node)));
    }

    private static Point roundedPoint(Point2D.Double point) {
        return new Point(
                (int) Math.round(point.x),
                (int) Math.round(point.y));
    }

    @SuppressWarnings("unchecked")
    private static List<String> leafNameListForNode(ReflectGraphicNode<EvolNode> node) {
        if (node.getChildCount() == 0) {
            String name = node.getReflectNode().getName();
            return name == null || name.trim().isEmpty() ? List.of() : List.of(name.trim());
        }
        List<String> leafNames = new ArrayList<>();
        for (int index = 0; index < node.getChildCount(); index++) {
            leafNames.addAll(leafNameListForNode((ReflectGraphicNode<EvolNode>) node.getChildAt(index)));
        }
        return List.copyOf(leafNames);
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

    private static TreeDifferenceMetrics calculateTreeDifferenceMetrics(List<ImportedTreeSpec> trees) {
        if (trees == null || trees.size() < 2 || trees.get(0).root() == null) {
            return TreeDifferenceMetrics.unavailable();
        }

        List<Map<CladeSignature, Double>> cladeMaps = new ArrayList<>(trees.size());
        for (ImportedTreeSpec tree : trees) {
            if (tree.root() == null) {
                return TreeDifferenceMetrics.unavailable();
            }
            Map<CladeSignature, Double> cladeMap = new HashMap<>();
            collectCladeSignatures(tree.root(), true, cladeMap);
            cladeMaps.add(cladeMap);
        }

        Map<CladeSignature, Double> referenceClades = cladeMaps.get(0);
        if (referenceClades.isEmpty()) {
            return TreeDifferenceMetrics.unavailable();
        }

        int otherTreeCount = trees.size() - 1;
        double topologyDifferenceSum = 0.0d;
        double branchDifferenceSum = 0.0d;
        int branchDifferenceCount = 0;
        int recoveredReferenceCladeCount = 0;

        for (Map.Entry<CladeSignature, Double> referenceEntry : referenceClades.entrySet()) {
            CladeSignature signature = referenceEntry.getKey();
            int matchedOtherTreeCount = 0;
            List<Double> branchLengths = new ArrayList<>();
            branchLengths.add(referenceEntry.getValue());

            for (int treeIndex = 1; treeIndex < cladeMaps.size(); treeIndex++) {
                Double branchLength = cladeMaps.get(treeIndex).get(signature);
                if (branchLength != null) {
                    matchedOtherTreeCount++;
                    branchLengths.add(branchLength);
                }
            }

            if (matchedOtherTreeCount > 0) {
                recoveredReferenceCladeCount++;
            }
            topologyDifferenceSum += 1.0d - ((double) matchedOtherTreeCount / (double) otherTreeCount);
            if (branchLengths.size() >= 2) {
                branchDifferenceSum += branchLengthDifference(branchLengths);
                branchDifferenceCount++;
            }
        }

        double branchLengthDifferenceIndex = branchDifferenceCount == 0
                ? Double.NaN
                : branchDifferenceSum / branchDifferenceCount;
        return new TreeDifferenceMetrics(
                topologyDifferenceSum / referenceClades.size(),
                branchLengthDifferenceIndex,
                referenceClades.size(),
                recoveredReferenceCladeCount);
    }

    private static List<String> collectCladeSignatures(
            EvolNode node,
            boolean root,
            Map<CladeSignature, Double> cladeMap) {
        if (node == null) {
            return List.of();
        }
        if (node.getChildCount() == 0) {
            String name = node.getName();
            if (name == null || name.trim().isEmpty()) {
                return List.of();
            }
            return List.of(name.trim());
        }

        List<String> leafNames = new ArrayList<>();
        for (int index = 0; index < node.getChildCount(); index++) {
            leafNames.addAll(collectCladeSignatures(EvolNodeUtil.getChildrenAt(node, index), false, cladeMap));
        }
        leafNames.sort(String::compareTo);
        if (!root && leafNames.size() > 1) {
            cladeMap.putIfAbsent(new CladeSignature(leafNames.size(), List.copyOf(leafNames)), normalizeBranchLength(node.getLength()));
        }
        return leafNames;
    }

    private static double branchLengthDifference(List<Double> branchLengths) {
        double absoluteMean = 0.0d;
        for (Double branchLength : branchLengths) {
            absoluteMean += Math.abs(branchLength == null ? 0.0d : branchLength.doubleValue());
        }
        absoluteMean /= branchLengths.size();

        double mean = 0.0d;
        for (Double branchLength : branchLengths) {
            mean += branchLength == null ? 0.0d : branchLength.doubleValue();
        }
        mean /= branchLengths.size();

        double variance = 0.0d;
        for (Double branchLength : branchLengths) {
            double delta = (branchLength == null ? 0.0d : branchLength.doubleValue()) - mean;
            variance += delta * delta;
        }
        double standardDeviation = Math.sqrt(variance / branchLengths.size());
        if (absoluteMean <= 1.0e-12d) {
            return standardDeviation <= 1.0e-12d ? 0.0d : 1.0d;
        }
        double coefficientOfVariation = standardDeviation / absoluteMean;
        return coefficientOfVariation / (1.0d + coefficientOfVariation);
    }

    private static double normalizeBranchLength(double branchLength) {
        return Double.isFinite(branchLength) ? branchLength : 0.0d;
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

    private static List<ConsistencyAnnotation> quickLabelsFromFirstTree(List<ImportedTreeSpec> importedTrees) {
        if (importedTrees == null || importedTrees.isEmpty() || importedTrees.get(0).root() == null) {
            return List.of();
        }
        List<List<String>> clades = new ArrayList<>();
        collectInternalClades(importedTrees.get(0).root(), true, clades);
        List<ConsistencyAnnotation> annotations = new ArrayList<>();
        int count = Math.min(MAX_QUICK_LABELS, clades.size());
        for (int index = 0; index < count; index++) {
            annotations.add(new ConsistencyAnnotation(
                    clades.get(index),
                    quickLabelColor(index),
                    ConsistencyAnnotation.DEFAULT_RIBBON_WIDTH));
        }
        return List.copyOf(annotations);
    }

    private static List<String> collectInternalClades(EvolNode node, boolean root, List<List<String>> clades) {
        if (node == null) {
            return List.of();
        }
        if (node.getChildCount() == 0) {
            String name = node.getName();
            if (name == null || name.trim().isEmpty()) {
                return List.of();
            }
            return List.of(name.trim());
        }
        List<String> leafNames = new ArrayList<>();
        for (int index = 0; index < node.getChildCount(); index++) {
            leafNames.addAll(collectInternalClades(EvolNodeUtil.getChildrenAt(node, index), false, clades));
        }
        leafNames.sort(String::compareTo);
        if (!root && leafNames.size() > 1 && clades.size() < MAX_QUICK_LABELS) {
            clades.add(List.copyOf(leafNames));
        }
        return leafNames;
    }

    private static List<String> findNamedInternalNodeLeafNames(List<ImportedTreeSpec> importedTrees, String nodeName) {
        if (importedTrees == null || nodeName == null || nodeName.trim().isEmpty()) {
            return List.of();
        }
        String targetName = nodeName.trim();
        for (ImportedTreeSpec importedTree : importedTrees) {
            List<String> leafNames = new ArrayList<>(findNamedInternalNodeLeafNames(importedTree.root(), targetName));
            if (!leafNames.isEmpty()) {
                leafNames.sort(String::compareTo);
                return List.copyOf(leafNames);
            }
        }
        return List.of();
    }

    private static List<String> findNamedInternalNodeLeafNames(EvolNode node, String targetName) {
        if (node == null || node.getChildCount() == 0) {
            return List.of();
        }
        String nodeName = node.getName();
        if (nodeName != null && nodeName.trim().equals(targetName)) {
            return collectLeafNames(node);
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            List<String> leafNames = findNamedInternalNodeLeafNames(EvolNodeUtil.getChildrenAt(node, index), targetName);
            if (!leafNames.isEmpty()) {
                return leafNames;
            }
        }
        return List.of();
    }

    private static Color quickLabelColor(int index) {
        Color[] colors = new Color[] {
                new Color(79, 140, 255, 145),
                new Color(255, 162, 52, 150),
                new Color(68, 190, 132, 145),
                new Color(214, 102, 255, 145),
                new Color(255, 99, 132, 145),
                new Color(55, 185, 210, 145),
                new Color(245, 205, 66, 150),
                new Color(145, 120, 255, 145),
                new Color(255, 128, 82, 145),
                new Color(120, 170, 80, 145)
        };
        return colors[Math.floorMod(index, colors.length)];
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

            if (shouldPaintLoadingMessage()) {
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
            paintConsistencyAnnotations(graphics2d, preparedAnnotations);
            for (PreparedLayer preparedLayer : preparedLayers) {
                paintLayerTree(graphics2d, preparedLayer);
            }
            paintConsistencyAnnotationMarkers(graphics2d, preparedAnnotations);
            paintConsistencyAnnotationLegend(graphics2d, preparedLayers, preparedAnnotations, getSize());
            paintTreeDifferenceMetrics(graphics2d, treeDifferenceMetrics);
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

    private static Font resolveLegendFont() {
        Font uiFont = UIManager.getFont("Label.font");
        int fontSize = Math.max(9, UiPreferenceStore.load().defaultTanglegramLabelFontSize() - 3);
        if (uiFont != null) {
            return uiFont.deriveFont((float) fontSize);
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
    }

    private static Font resolveMetricsFont() {
        Font uiFont = UIManager.getFont("Label.font");
        int fontSize = Math.max(12, UiPreferenceStore.load().defaultTanglegramLabelFontSize());
        if (uiFont != null) {
            return uiFont.deriveFont(Font.PLAIN, (float) fontSize);
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
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
        return !loading && errorMessage == null && preparedRenderMatchesCanvasSize();
    }

    private record PreparedRender(
            List<PreparedLayer> layers,
            List<PreparedAnnotation> annotations,
            TreeDifferenceMetrics metrics) {
    }

    record TreeDifferenceMetrics(
            double topologyDifferenceIndex,
            double branchLengthDifferenceIndex,
            int referenceCladeCount,
            int recoveredReferenceCladeCount) {
        private static TreeDifferenceMetrics unavailable() {
            return new TreeDifferenceMetrics(Double.NaN, Double.NaN, 0, 0);
        }
    }

    private record CladeSignature(int leafCount, List<String> sortedLeafNames) {
        private CladeSignature {
            sortedLeafNames = List.copyOf(sortedLeafNames);
        }
    }

    private record PreparedAnnotation(
            List<Point2D.Double> anchors,
            Color color,
            double ribbonWidth,
            String label) {
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
