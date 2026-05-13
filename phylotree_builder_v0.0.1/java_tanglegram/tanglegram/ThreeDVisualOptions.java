package tanglegram;

import java.awt.Color;

record ThreeDVisualOptions(
        int metricsFontSize,
        Color metricsColor,
        int treeTitleFontSize,
        Color treeTitleColor,
        float treeLineThickness,
        Color treeLineColor,
        int leafLabelFontSize,
        Color leafLabelColor,
        int legendFontSize,
        Color legendColor,
        int scaleBarFontSize,
        float scaleBarLineThickness,
        Color scaleBarColor) {
    private static final float DEFAULT_TREE_LINE_THICKNESS = 1.1f;
    private static final float DEFAULT_SCALE_BAR_LINE_THICKNESS = 1.1f;
    private static final Color DEFAULT_TEXT_COLOR = Color.BLACK;
    private static final Color DEFAULT_SCALE_BAR_COLOR = new Color(65, 72, 82, 210);

    ThreeDVisualOptions {
        metricsFontSize = clampFontSize(metricsFontSize);
        metricsColor = safeColor(metricsColor, DEFAULT_TEXT_COLOR);
        treeTitleFontSize = clampFontSize(treeTitleFontSize);
        treeTitleColor = safeColor(treeTitleColor, DEFAULT_TEXT_COLOR);
        treeLineThickness = Math.max(0.5f, Math.min(8.0f, treeLineThickness));
        treeLineColor = safeColor(treeLineColor, DEFAULT_TEXT_COLOR);
        leafLabelFontSize = clampFontSize(leafLabelFontSize);
        leafLabelColor = safeColor(leafLabelColor, DEFAULT_TEXT_COLOR);
        legendFontSize = clampFontSize(legendFontSize);
        legendColor = safeColor(legendColor, DEFAULT_TEXT_COLOR);
        scaleBarFontSize = clampFontSize(scaleBarFontSize);
        scaleBarLineThickness = Math.max(0.5f, Math.min(8.0f, scaleBarLineThickness));
        scaleBarColor = safeColor(scaleBarColor, DEFAULT_SCALE_BAR_COLOR);
    }

    static ThreeDVisualOptions defaults() {
        int preferenceLabelSize = UiPreferenceStore.load().defaultTanglegramLabelFontSize();
        return new ThreeDVisualOptions(
                Math.max(12, preferenceLabelSize),
                DEFAULT_TEXT_COLOR,
                18,
                DEFAULT_TEXT_COLOR,
                DEFAULT_TREE_LINE_THICKNESS,
                DEFAULT_TEXT_COLOR,
                Math.max(9, preferenceLabelSize - 2),
                DEFAULT_TEXT_COLOR,
                10,
                DEFAULT_TEXT_COLOR,
                Math.max(9, preferenceLabelSize - 3),
                DEFAULT_SCALE_BAR_LINE_THICKNESS,
                DEFAULT_SCALE_BAR_COLOR);
    }

    int previousDefaultTitleFontSizeForTest() {
        return previousDefaultTitleFontSize(UiPreferenceStore.load().defaultTanglegramLabelFontSize());
    }

    private static int previousDefaultTitleFontSize(int preferenceLabelSize) {
        return Math.max(11, preferenceLabelSize - 1);
    }

    private static int clampFontSize(int value) {
        return Math.max(6, Math.min(72, value));
    }

    private static Color safeColor(Color color, Color fallback) {
        return color == null ? fallback : color;
    }
}
