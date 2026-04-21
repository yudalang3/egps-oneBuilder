package tanglegram;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class ResizableTanglegramView extends JPanel {
    private static final int RENDER_DELAY_MS = 120;

    private final TreePairSpec pairSpec;
    private final TanglegramPanelFactory panelFactory;
    private final TanglegramRenderOptions renderOptions;
    private final Timer renderTimer;
    private final AtomicLong renderSequence;
    private final JPanel contentPanel;

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
        this.contentPanel = new JPanel(new BorderLayout());
        this.renderTimer.setRepeats(false);
        setLayout(new BorderLayout());
        setBorder(null);
        contentPanel.setBorder(null);
        add(contentPanel, BorderLayout.CENTER);
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
        Dimension currentSize = getSize();
        if (currentSize.width <= 0 || currentSize.height <= 0) {
            return new Dimension(1200, 800);
        }
        return new Dimension(Math.max(1, currentSize.width), Math.max(1, currentSize.height));
    }

    private void setContent(Component component) {
        contentPanel.removeAll();
        if (component != null) {
            contentPanel.add(component, BorderLayout.CENTER);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private static JPanel errorPanel(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(message == null ? "Failed to render tanglegram." : message, SwingConstants.CENTER),
                BorderLayout.CENTER);
        return panel;
    }

    private static JPanel loadingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Loading tanglegram...", SwingConstants.CENTER), BorderLayout.CENTER);
        return panel;
    }
}
