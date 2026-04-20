package tanglegram;

import evoltree.struct.EvolNode;
import evoltree.struct.TreeDecoder;
import evoltree.swingvis.OneNodeDrawer;
import evoltree.tanglegram.QuickPairwiseTreeComparator;
import evoltree.txtdisplay.ReflectGraphicNode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.UIManager;

public final class TanglegramPanelFactory {
    private static final Dimension DEFAULT_DIMENSION = new Dimension(1200, 800);
    private static final int MIN_WIDTH = 640;
    private static final int MIN_HEIGHT = 480;

    private final TanglegramRenderOptions renderOptions;
    private final OneNodeDrawer<EvolNode> leftDrawer = (graphics2d, node) -> drawRightFacingLeafLabels(graphics2d, node);
    private final OneNodeDrawer<EvolNode> rightDrawer = (graphics2d, node) -> drawLeftFacingLeafLabels(graphics2d, node);

    public TanglegramPanelFactory() {
        this(TanglegramRenderOptions.defaults());
    }

    public TanglegramPanelFactory(TanglegramRenderOptions renderOptions) {
        this.renderOptions = renderOptions == null ? TanglegramRenderOptions.defaults() : renderOptions;
    }

    public JPanel createPanel(TreePairSpec pairSpec, Dimension requestedSize) throws Exception {
        return createPanel(preparePair(pairSpec), requestedSize);
    }

    public PreparedPair preparePair(TreePairSpec pairSpec) throws Exception {
        TreeDecoder decoder = new TreeDecoder();
        EvolNode leftTree = decoder.decode(readTree(pairSpec.leftTree()));
        EvolNode rightTree = decoder.decode(readTree(pairSpec.rightTree()));
        return new PreparedPair(leftTree, rightTree);
    }

    public JPanel createPanel(PreparedPair preparedPair, Dimension requestedSize) {
        Dimension effectiveSize = renderOptions.autoFit() ? sanitizeSize(requestedSize) : new Dimension(DEFAULT_DIMENSION);
        Font labelFont = resolveLabelFont();
        JPanel innerPanel = QuickPairwiseTreeComparator.plotTree(
                preparedPair.leftTree(),
                preparedPair.rightTree(),
                labelFont,
                effectiveSize,
                leftDrawer,
                rightDrawer);
        innerPanel.setPreferredSize(effectiveSize);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(
                renderOptions.verticalPadding(),
                renderOptions.horizontalPadding(),
                renderOptions.verticalPadding(),
                renderOptions.horizontalPadding()));
        panel.add(innerPanel, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(
                effectiveSize.width + (renderOptions.horizontalPadding() * 2),
                effectiveSize.height + (renderOptions.verticalPadding() * 2)));
        return panel;
    }

    private static String readTree(Path treeFile) throws IOException {
        return new String(Files.readAllBytes(treeFile), StandardCharsets.UTF_8).trim();
    }

    public record PreparedPair(EvolNode leftTree, EvolNode rightTree) {
    }

    private Font resolveLabelFont() {
        Font uiFont = UIManager.getFont("Label.font");
        if (uiFont != null) {
            return uiFont.deriveFont((float) renderOptions.labelFontSize());
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, renderOptions.labelFontSize());
    }

    private static Dimension sanitizeSize(Dimension requestedSize) {
        if (requestedSize == null) {
            return new Dimension(DEFAULT_DIMENSION);
        }

        int width = Math.max(requestedSize.width, MIN_WIDTH);
        int height = Math.max(requestedSize.height, MIN_HEIGHT);
        return new Dimension(width, height);
    }

    private static void drawRightFacingLeafLabels(Graphics2D graphics2d, ReflectGraphicNode<EvolNode> node) {
        int xSelf = (int) node.getXSelf();
        int ySelf = (int) node.getYSelf();
        int xParent = (int) node.getXParent();
        if (node.getChildCount() == 0) {
            graphics2d.drawString(node.getReflectNode().getName(), xSelf + 5, ySelf + 5);
        }

        String lengthString = String.valueOf(node.getLength());
        int centerX = (xSelf + xParent - graphics2d.getFontMetrics().stringWidth(lengthString)) / 2;
        int centerY = ySelf - 5;
        graphics2d.drawString(lengthString, centerX, centerY);
    }

    private static void drawLeftFacingLeafLabels(Graphics2D graphics2d, ReflectGraphicNode<EvolNode> node) {
        int xSelf = (int) node.getXSelf();
        int ySelf = (int) node.getYSelf();
        int xParent = (int) node.getXParent();
        if (node.getChildCount() == 0) {
            FontMetrics fontMetrics = graphics2d.getFontMetrics();
            String name = node.getReflectNode().getName();
            int stringWidth = fontMetrics.stringWidth(name);
            graphics2d.drawString(name, xSelf - 5 - stringWidth, ySelf + 5);
        }

        String lengthString = String.valueOf(node.getLength());
        int centerX = (xSelf + xParent - graphics2d.getFontMetrics().stringWidth(lengthString)) / 2;
        int centerY = ySelf - 5;
        graphics2d.drawString(lengthString, centerX, centerY);
    }
}
