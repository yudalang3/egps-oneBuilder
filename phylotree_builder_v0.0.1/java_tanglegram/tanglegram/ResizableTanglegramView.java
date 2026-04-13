package tanglegram;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class ResizableTanglegramView extends JScrollPane {
    private static final int RENDER_DELAY_MS = 120;

    private final TreePairSpec pairSpec;
    private final TanglegramPanelFactory panelFactory;
    private final TanglegramRenderOptions renderOptions;
    private final Timer renderTimer;

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
        this.renderTimer.setRepeats(false);
        setBorder(null);
        getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent event) {
                scheduleRender();
            }
        });

        SwingUtilities.invokeLater(this::scheduleRender);
    }

    public void renderNowForTest(Dimension dimension) throws Exception {
        setViewportView(panelFactory.createPanel(pairSpec, dimension));
    }

    private void scheduleRender() {
        renderTimer.restart();
    }

    private void renderToViewport() {
        try {
            setViewportView(panelFactory.createPanel(pairSpec, getViewportSize()));
        } catch (Exception exception) {
            setViewportView(errorPanel(exception.getMessage()));
        }
    }

    private Dimension getViewportSize() {
        if (!renderOptions.autoFit()) {
            return new Dimension(1200, 800);
        }
        Dimension extentSize = getViewport().getExtentSize();
        if (extentSize.width <= 0 || extentSize.height <= 0) {
            return new Dimension(1200, 800);
        }
        Insets insets = getInsets();
        int width = Math.max(1, extentSize.width - insets.left - insets.right);
        int height = Math.max(1, extentSize.height - insets.top - insets.bottom);
        return new Dimension(width, height);
    }

    private static JPanel errorPanel(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(message == null ? "Failed to render tanglegram." : message, SwingConstants.CENTER),
                BorderLayout.CENTER);
        return panel;
    }
}
