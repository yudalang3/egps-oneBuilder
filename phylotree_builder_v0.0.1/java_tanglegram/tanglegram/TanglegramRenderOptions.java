package tanglegram;

import java.awt.Font;
import java.util.Objects;
import javax.swing.UIManager;

public final class TanglegramRenderOptions {
    private final int labelFontSize;
    private final String labelFontFamily;
    private final int labelFontStyle;
    private final boolean showLeafLabels;
    private final int horizontalPadding;
    private final int verticalPadding;
    private final int connectorGap;
    private final float connectorStrokeWidth;
    private final float connectorDashLength;
    private final float connectorDashGap;
    private final boolean autoFit;

    public TanglegramRenderOptions(
            int labelFontSize,
            String labelFontFamily,
            int labelFontStyle,
            boolean showLeafLabels,
            int horizontalPadding,
            int verticalPadding,
            int connectorGap,
            float connectorStrokeWidth,
            float connectorDashLength,
            float connectorDashGap,
            boolean autoFit) {
        this.labelFontSize = labelFontSize;
        this.labelFontFamily = labelFontFamily;
        this.labelFontStyle = labelFontStyle;
        this.showLeafLabels = showLeafLabels;
        this.horizontalPadding = horizontalPadding;
        this.verticalPadding = verticalPadding;
        this.connectorGap = connectorGap;
        this.connectorStrokeWidth = connectorStrokeWidth;
        this.connectorDashLength = connectorDashLength;
        this.connectorDashGap = connectorDashGap;
        this.autoFit = autoFit;
    }

    public static TanglegramRenderOptions defaults() {
        Font uiFont = UIManager.getFont("Label.font");
        String fontFamily = uiFont == null ? Font.SANS_SERIF : uiFont.getFamily();
        int fontStyle = uiFont == null ? Font.PLAIN : uiFont.getStyle();
        return new TanglegramRenderOptions(
                UiPreferenceStore.load().defaultTanglegramLabelFontSize(),
                fontFamily,
                fontStyle,
                true,
                24,
                16,
                160,
                1.0f,
                6.0f,
                5.0f,
                true);
    }

    public int labelFontSize() {
        return labelFontSize;
    }

    public String labelFontFamily() {
        return labelFontFamily;
    }

    public int labelFontStyle() {
        return labelFontStyle;
    }

    public boolean showLeafLabels() {
        return showLeafLabels;
    }

    public int horizontalPadding() {
        return horizontalPadding;
    }

    public int verticalPadding() {
        return verticalPadding;
    }

    public int connectorGap() {
        return connectorGap;
    }

    public float connectorStrokeWidth() {
        return connectorStrokeWidth;
    }

    public float connectorDashLength() {
        return connectorDashLength;
    }

    public float connectorDashGap() {
        return connectorDashGap;
    }

    public boolean autoFit() {
        return autoFit;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TanglegramRenderOptions other)) {
            return false;
        }
        return labelFontSize == other.labelFontSize
                && labelFontStyle == other.labelFontStyle
                && showLeafLabels == other.showLeafLabels
                && horizontalPadding == other.horizontalPadding
                && verticalPadding == other.verticalPadding
                && connectorGap == other.connectorGap
                && Float.compare(connectorStrokeWidth, other.connectorStrokeWidth) == 0
                && Float.compare(connectorDashLength, other.connectorDashLength) == 0
                && Float.compare(connectorDashGap, other.connectorDashGap) == 0
                && autoFit == other.autoFit
                && Objects.equals(labelFontFamily, other.labelFontFamily);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                labelFontSize, labelFontFamily, labelFontStyle,
                showLeafLabels, horizontalPadding, verticalPadding,
                connectorGap, connectorStrokeWidth, connectorDashLength,
                connectorDashGap, autoFit);
    }
}
