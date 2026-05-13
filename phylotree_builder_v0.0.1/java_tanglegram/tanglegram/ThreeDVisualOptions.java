package tanglegram;

record ThreeDVisualOptions(
        int metricsFontSize,
        int treeTitleFontSize,
        float treeLineThickness,
        int leafLabelFontSize,
        int legendFontSize,
        int scaleBarFontSize) {
    private static final float DEFAULT_TREE_LINE_THICKNESS = 1.1f;

    ThreeDVisualOptions {
        metricsFontSize = clampFontSize(metricsFontSize);
        treeTitleFontSize = clampFontSize(treeTitleFontSize);
        treeLineThickness = Math.max(0.5f, Math.min(8.0f, treeLineThickness));
        leafLabelFontSize = clampFontSize(leafLabelFontSize);
        legendFontSize = clampFontSize(legendFontSize);
        scaleBarFontSize = clampFontSize(scaleBarFontSize);
    }

    static ThreeDVisualOptions defaults() {
        int preferenceLabelSize = UiPreferenceStore.load().defaultTanglegramLabelFontSize();
        int previousTitleSize = previousDefaultTitleFontSize(preferenceLabelSize);
        return new ThreeDVisualOptions(
                Math.max(12, preferenceLabelSize),
                previousTitleSize * 2,
                DEFAULT_TREE_LINE_THICKNESS,
                Math.max(9, preferenceLabelSize - 2),
                Math.max(9, preferenceLabelSize - 3),
                Math.max(9, preferenceLabelSize - 3));
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
}
