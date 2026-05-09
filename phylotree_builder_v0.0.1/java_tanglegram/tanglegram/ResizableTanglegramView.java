package tanglegram;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class ResizableTanglegramView extends JPanel implements ExportableView {
    private static final int RENDER_DELAY_MS = 120;

    private final TreePairSpec pairSpec;
    private final TanglegramPanelFactory panelFactory;
    private final TanglegramRenderOptions renderOptions;
    private final Timer renderTimer;
    private final AtomicLong renderSequence;
    private final JScrollPane scrollPane;
    private JComponent exportComponent;
    private TreeViewportNavigationSupport.Controller navigationController;

    public ResizableTanglegramView(TreePairSpec pairSpec, TanglegramPanelFactory panelFactory) {
        this(pairSpec, panelFactory, TanglegramRenderOptions.defaults());
    }

    public ResizableTanglegramView(
            TreePairSpec pairSpec,
            TanglegramPanelFactory panelFactory,
            TanglegramRenderOptions renderOptions) {
        this.pairSpec = pairSpec;
        this.panelFactory = panelFactory;
        this.renderOptions = renderOptions == null ? TanglegramRenderOptions.defaults() : renderOptions;
        this.renderTimer = new Timer(RENDER_DELAY_MS, event -> renderToViewport());
        this.renderSequence = new AtomicLong();
        this.scrollPane = new JScrollPane();
        this.renderTimer.setRepeats(false);
        setLayout(new BorderLayout());
        setBorder(null);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(java.awt.Color.WHITE);
        scrollPane.getViewport().setScrollMode(javax.swing.JViewport.SIMPLE_SCROLL_MODE);
        add(scrollPane, BorderLayout.CENTER);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent event) {
                scheduleRender();
            }
        });

        SwingUtilities.invokeLater(this::scheduleRender);
    }

    public void renderNowForTest(Dimension dimension) throws Exception {
        setContent(panelFactory.createPanel(pairSpec, dimension));
    }

    JScrollPane scrollPaneForTest() {
        return scrollPane;
    }

    void fitFrameForTest() {
        if (navigationController != null) {
            navigationController.fitFrame();
        }
    }

    Point firstNodePointForTest() {
        if (exportComponent instanceof CustomTanglegramPanel customTanglegramPanel) {
            return customTanglegramPanel.firstNodePointForTest();
        }
        return new Point(0, 0);
    }

    JPopupMenu popupMenuForTest(Point point) {
        if (navigationController == null) {
            return new JPopupMenu();
        }
        return navigationController.createPopupMenu(point);
    }

    private void scheduleRender() {
        renderTimer.restart();
    }

    private void renderToViewport() {
        final long renderId = renderSequence.incrementAndGet();
        final Dimension viewportSize = getViewportSize();
        setContent(loadingPanel());
        Thread renderThread = new Thread(() -> {
            try {
                TanglegramPanelFactory.PreparedPair preparedPair = panelFactory.preparePair(pairSpec);
                SwingUtilities.invokeLater(() -> {
                    if (renderId != renderSequence.get()) {
                        return;
                    }
                    try {
                        setContent(panelFactory.createPanel(preparedPair, viewportSize));
                    } catch (Exception exception) {
                        setContent(errorPanel(exception.getMessage()));
                    }
                });
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> {
                    if (renderId == renderSequence.get()) {
                        setContent(errorPanel(exception.getMessage()));
                    }
                });
            }
        }, "tanglegram-renderer");
        renderThread.setDaemon(true);
        renderThread.start();
    }

    private Dimension getViewportSize() {
        if (!renderOptions.autoFit()) {
            return new Dimension(1200, 800);
        }
        Dimension viewportSize = scrollPane.getViewport().getExtentSize();
        if (viewportSize.width > 0 && viewportSize.height > 0) {
            return new Dimension(Math.max(1, viewportSize.width), Math.max(1, viewportSize.height));
        }
        Dimension currentSize = scrollPane.getSize();
        if (currentSize.width <= 0 || currentSize.height <= 0) {
            currentSize = getSize();
        }
        if (currentSize.width <= 0 || currentSize.height <= 0) {
            return new Dimension(1200, 800);
        }
        return new Dimension(Math.max(1, currentSize.width), Math.max(1, currentSize.height));
    }

    private void setContent(Component component) {
        exportComponent = null;
        navigationController = null;
        if (component instanceof CustomTanglegramPanel customTanglegramPanel) {
            exportComponent = customTanglegramPanel;
            navigationController = TreeViewportNavigationSupport.install(
                    customTanglegramPanel,
                    scrollPane,
                    customTanglegramPanel::hitAt,
                    null);
        }
        scrollPane.setViewportView(component);
        if (getWidth() > 0 && getHeight() > 0) {
            scrollPane.setSize(getWidth(), getHeight());
            scrollPane.doLayout();
        }
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    @Override
    public JComponent getExportComponent() {
        return exportComponent;
    }

    @Override
    public boolean canExport() {
        return exportComponent != null;
    }

    @Override
    public Class<?> getExportContextClass() {
        return ResizableTanglegramView.class;
    }

    private static JPanel errorPanel(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(message == null ? UiText.text("Failed to render tanglegram.", "缠结图渲染失败。") : message, SwingConstants.CENTER),
                BorderLayout.CENTER);
        return panel;
    }

    private static JPanel loadingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(UiText.text("Loading tanglegram...", "正在加载缠结图..."), SwingConstants.CENTER), BorderLayout.CENTER);
        return panel;
    }
}
