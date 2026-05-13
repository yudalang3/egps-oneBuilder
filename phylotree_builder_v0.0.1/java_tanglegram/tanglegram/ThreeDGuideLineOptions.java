package tanglegram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;

record ThreeDGuideLineOptions(
        boolean showDashLine,
        float strokeWidth,
        float dashLength,
        float dashGap,
        Color color) {
    private static final float DEFAULT_STROKE_WIDTH = 1.1f;
    private static final float DEFAULT_DASH_LENGTH = 6.0f;
    private static final float DEFAULT_DASH_GAP = 5.0f;
    private static final Color DEFAULT_COLOR = new Color(80, 80, 80);

    static ThreeDGuideLineOptions defaults() {
        return new ThreeDGuideLineOptions(true, DEFAULT_STROKE_WIDTH, DEFAULT_DASH_LENGTH, DEFAULT_DASH_GAP, DEFAULT_COLOR);
    }

    Stroke stroke() {
        float safeStrokeWidth = Math.max(0.5f, strokeWidth);
        if (!showDashLine) {
            return new BasicStroke(safeStrokeWidth);
        }
        return new BasicStroke(
                safeStrokeWidth,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND,
                1.0f,
                new float[] { Math.max(1.0f, dashLength), Math.max(1.0f, dashGap) },
                0.0f);
    }

    BasicStroke strokeForTest() {
        return (BasicStroke) stroke();
    }
}
