package tanglegram;

import java.awt.Dimension;
import java.awt.Cursor;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

final class TreeViewportNavigationSupport {
    private static final double ZOOM_STEP = 1.12d;
    private static final double ZOOM_TO_NODE_FACTOR = 1.60d;
    private static final double ZOOM_AREA_FACTOR = 1.45d;
    private static final double MIN_VIEWPORT_SCALE = 0.50d;
    private static final double MAX_VIEWPORT_SCALE = 24.0d;
    private static final int ABSOLUTE_MAX_SIZE = 24000;

    private TreeViewportNavigationSupport() {
    }

    static Controller install(
            JComponent target,
            JScrollPane scrollPane,
            HitTester hitTester,
            Runnable sizeChangedCallback) {
        Controller controller = new Controller(target, scrollPane, hitTester, sizeChangedCallback);
        target.addMouseListener(controller);
        target.addMouseMotionListener(controller);
        target.addMouseWheelListener(controller);
        return controller;
    }

    interface HitTester {
        TreeHit hitAt(Point point);
    }

    record TreeHit(
            String viewType,
            String layerName,
            String nodeName,
            boolean leafNode,
            double branchLength,
            int childCount,
            List<String> leafNames,
            Point center) {
        TreeHit {
            viewType = cleanText(viewType, "Tree view");
            layerName = cleanText(layerName, "Tree");
            nodeName = cleanText(nodeName, "(internal node)");
            leafNames = leafNames == null ? List.of() : List.copyOf(leafNames);
            center = center == null ? new Point(0, 0) : new Point(center);
        }

        String detailsText() {
            String leafNamesText = String.join(",", leafNames);
            return "View type: " + viewType
                    + "\nTree/layer: " + layerName
                    + "\nNode name: " + nodeName
                    + "\nNode kind: " + (leafNode ? "Leaf node" : "Internal node")
                    + "\nBranch length: " + formatBranchLength(branchLength)
                    + "\nChild nodes: " + childCount
                    + "\nLeaf names: " + leafNamesText
                    + "\nCopy value for Consistency annotation: " + leafNamesText;
        }

        String summaryText() {
            return viewType + " | " + layerName + " | " + nodeName + " | "
                    + (leafNode ? "Leaf node" : "Internal node") + " | children=" + childCount;
        }

        Point centerPoint() {
            return new Point(center);
        }

        private static String cleanText(String text, String fallback) {
            if (text == null || text.trim().isEmpty()) {
                return fallback;
            }
            return text.trim();
        }
    }

    static final class Controller extends MouseAdapter {
        private final JComponent target;
        private final JScrollPane scrollPane;
        private final HitTester hitTester;
        private final Runnable sizeChangedCallback;
        private Point dragStartPoint;
        private Point dragStartScreenPoint;
        private Point dragStartViewPosition;
        private Cursor previousCursor;
        private boolean popupShownOnPress;

        private Controller(
                JComponent target,
                JScrollPane scrollPane,
                HitTester hitTester,
                Runnable sizeChangedCallback) {
            this.target = target;
            this.scrollPane = scrollPane;
            this.hitTester = hitTester;
            this.sizeChangedCallback = sizeChangedCallback;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent event) {
            int rotation = event.getWheelRotation();
            if (rotation == 0) {
                return;
            }
            zoomAt(event.getPoint(), Math.pow(ZOOM_STEP, -rotation));
            event.consume();
        }

        @Override
        public void mousePressed(MouseEvent event) {
            popupShownOnPress = false;
            if (isPopupEvent(event)) {
                showPopup(event);
                popupShownOnPress = true;
                return;
            }
            if (SwingUtilities.isLeftMouseButton(event)) {
                dragStartPoint = event.getPoint();
                dragStartScreenPoint = screenPoint(event);
                dragStartViewPosition = scrollPane.getViewport().getViewPosition();
                previousCursor = target.getCursor();
                target.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            if (dragStartPoint == null || dragStartViewPosition == null) {
                return;
            }
            Point currentScreenPoint = screenPoint(event);
            int deltaX = currentScreenPoint.x - dragStartScreenPoint.x;
            int deltaY = currentScreenPoint.y - dragStartScreenPoint.y;
            target.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            setViewPosition(new Point(
                    dragStartViewPosition.x - deltaX,
                    dragStartViewPosition.y - deltaY));
            event.consume();
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            dragStartPoint = null;
            dragStartScreenPoint = null;
            dragStartViewPosition = null;
            target.setCursor(previousCursor == null ? Cursor.getDefaultCursor() : previousCursor);
            previousCursor = null;
            if (isPopupEvent(event) && !popupShownOnPress) {
                showPopup(event);
            }
            popupShownOnPress = false;
        }

        JPopupMenu createPopupMenu(Point point) {
            Point safePoint = point == null ? new Point(0, 0) : new Point(point);
            TreeHit hit = hitTester == null ? null : hitTester.hitAt(safePoint);
            JPopupMenu popupMenu = new JPopupMenu();
            popupMenu.setLightWeightPopupEnabled(false);
            if (hit != null) {
                JMenuItem informationItem = new JMenuItem("Display more information");
                informationItem.addActionListener(event -> showInformation(hit));
                JMenuItem zoomNodeItem = new JMenuItem("Zoom to see node");
                zoomNodeItem.addActionListener(event -> zoomToPoint(hit.centerPoint(), ZOOM_TO_NODE_FACTOR));
                popupMenu.add(informationItem);
                popupMenu.add(zoomNodeItem);
                return popupMenu;
            }

            JMenuItem fitItem = new JMenuItem("Refresh (Fit frame)");
            fitItem.addActionListener(event -> fitFrame());
            JMenuItem zoomAreaItem = new JMenuItem("Zoom the area");
            zoomAreaItem.addActionListener(event -> zoomAt(safePoint, ZOOM_AREA_FACTOR));
            popupMenu.add(fitItem);
            popupMenu.add(zoomAreaItem);
            return popupMenu;
        }

        void fitFrame() {
            Dimension extent = effectiveViewportExtent();
            resizeTarget(new Dimension(Math.max(1, extent.width), Math.max(1, extent.height)));
            setViewPosition(new Point(0, 0));
        }

        void zoomAt(Point point, double factor) {
            zoomAtInternal(point, factor);
        }

        private void zoomToPoint(Point point, double factor) {
            Dimension before = currentViewSize();
            Dimension after = zoomAtInternal(point, factor);
            Point centerAfterZoom = scalePoint(point, before, after);
            scrollToCenter(centerAfterZoom);
        }

        private Dimension zoomAtInternal(Point point, double factor) {
            if (!Double.isFinite(factor) || factor <= 0.0d) {
                return currentViewSize();
            }
            Dimension before = currentViewSize();
            Dimension extent = effectiveViewportExtent();
            Dimension after = constrainedZoomSize(before, extent, factor);
            if (before.equals(after)) {
                return before;
            }

            Rectangle viewRect = scrollPane.getViewport().getViewRect();
            Point focus = point == null
                    ? new Point(viewRect.x + (viewRect.width / 2), viewRect.y + (viewRect.height / 2))
                    : new Point(point);
            double focusRatioX = before.width <= 0 ? 0.5d : clampRatio((double) focus.x / (double) before.width);
            double focusRatioY = before.height <= 0 ? 0.5d : clampRatio((double) focus.y / (double) before.height);
            int cursorX = focus.x - viewRect.x;
            int cursorY = focus.y - viewRect.y;

            resizeTarget(after);
            setViewPosition(new Point(
                    (int) Math.round((focusRatioX * after.width) - cursorX),
                    (int) Math.round((focusRatioY * after.height) - cursorY)));
            return after;
        }

        private void scrollToCenter(Point center) {
            Dimension extent = effectiveViewportExtent();
            setViewPosition(new Point(
                    center.x - (extent.width / 2),
                    center.y - (extent.height / 2)));
        }

        private void resizeTarget(Dimension size) {
            target.setPreferredSize(size);
            target.setSize(size);
            target.revalidate();
            target.repaint();
            if (sizeChangedCallback != null) {
                sizeChangedCallback.run();
            }
        }

        private Dimension constrainedZoomSize(Dimension current, Dimension extent, double factor) {
            int minWidth = Math.max(1, (int) Math.round(extent.width * MIN_VIEWPORT_SCALE));
            int minHeight = Math.max(1, (int) Math.round(extent.height * MIN_VIEWPORT_SCALE));
            int maxWidth = Math.max(minWidth, Math.min(ABSOLUTE_MAX_SIZE, (int) Math.round(extent.width * MAX_VIEWPORT_SCALE)));
            int maxHeight = Math.max(minHeight, Math.min(ABSOLUTE_MAX_SIZE, (int) Math.round(extent.height * MAX_VIEWPORT_SCALE)));
            return new Dimension(
                    clamp((int) Math.round(current.width * factor), minWidth, maxWidth),
                    clamp((int) Math.round(current.height * factor), minHeight, maxHeight));
        }

        private Dimension currentViewSize() {
            Dimension preferredSize = target.getPreferredSize();
            if (preferredSize != null && preferredSize.width > 0 && preferredSize.height > 0) {
                return new Dimension(preferredSize);
            }
            Dimension size = target.getSize();
            if (size.width > 0 && size.height > 0) {
                return new Dimension(size);
            }
            return effectiveViewportExtent();
        }

        private Dimension effectiveViewportExtent() {
            JViewport viewport = scrollPane.getViewport();
            Dimension extent = viewport.getExtentSize();
            if (extent.width > 0 && extent.height > 0) {
                return new Dimension(extent);
            }
            Dimension size = scrollPane.getSize();
            if (size.width > 0 && size.height > 0) {
                return new Dimension(size);
            }
            return new Dimension(1200, 800);
        }

        private void setViewPosition(Point requestedPosition) {
            JViewport viewport = scrollPane.getViewport();
            Dimension extent = effectiveViewportExtent();
            Dimension viewSize = currentViewSize();
            int maxX = Math.max(0, viewSize.width - extent.width);
            int maxY = Math.max(0, viewSize.height - extent.height);
            viewport.setViewPosition(new Point(
                    clamp(requestedPosition.x, 0, maxX),
                    clamp(requestedPosition.y, 0, maxY)));
        }

        private void showPopup(MouseEvent event) {
            JPopupMenu popupMenu = createPopupMenu(event.getPoint());
            if (target.isShowing()) {
                JViewport viewport = scrollPane.getViewport();
                Point viewportPoint = SwingUtilities.convertPoint(target, event.getPoint(), viewport);
                popupMenu.show(viewport, viewportPoint.x, viewportPoint.y);
            }
            event.consume();
        }

        private boolean isPopupEvent(MouseEvent event) {
            return event.isPopupTrigger() || SwingUtilities.isRightMouseButton(event);
        }

        private static Point screenPoint(MouseEvent event) {
            return new Point(event.getXOnScreen(), event.getYOnScreen());
        }

        private void showInformation(TreeHit hit) {
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            javax.swing.JTextArea textArea = new javax.swing.JTextArea(hit.detailsText(), 10, 48);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setCaretPosition(0);
            javax.swing.JScrollPane informationScrollPane = new javax.swing.JScrollPane(textArea);
            Object[] options = new Object[] { "Copy all", "Close" };
            int selectedOption = JOptionPane.showOptionDialog(
                    target,
                    informationScrollPane,
                    "Node information",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    options,
                    options[1]);
            if (selectedOption == 0) {
                Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new StringSelection(hit.detailsText()), null);
            }
        }

        private static Point scalePoint(Point point, Dimension before, Dimension after) {
            if (point == null || before.width <= 0 || before.height <= 0) {
                return new Point(after.width / 2, after.height / 2);
            }
            return new Point(
                    (int) Math.round(((double) point.x / (double) before.width) * after.width),
                    (int) Math.round(((double) point.y / (double) before.height) * after.height));
        }

        private static double clampRatio(double value) {
            if (!Double.isFinite(value)) {
                return 0.5d;
            }
            return Math.max(0.0d, Math.min(1.0d, value));
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private static String formatBranchLength(double value) {
        if (!Double.isFinite(value)) {
            return "0.0";
        }
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
