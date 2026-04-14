package tanglegram;

public final class TanglegramRenderOptions {
    private final int labelFontSize;
    private final int horizontalPadding;
    private final int verticalPadding;
    private final boolean autoFit;

    public TanglegramRenderOptions(int labelFontSize, int horizontalPadding, int verticalPadding, boolean autoFit) {
        this.labelFontSize = labelFontSize;
        this.horizontalPadding = horizontalPadding;
        this.verticalPadding = verticalPadding;
        this.autoFit = autoFit;
    }

    public static TanglegramRenderOptions defaults() {
        return new TanglegramRenderOptions(UiPreferenceStore.load().defaultTanglegramLabelFontSize(), 24, 16, true);
    }

    public int labelFontSize() {
        return labelFontSize;
    }

    public int horizontalPadding() {
        return horizontalPadding;
    }

    public int verticalPadding() {
        return verticalPadding;
    }

    public boolean autoFit() {
        return autoFit;
    }
}
