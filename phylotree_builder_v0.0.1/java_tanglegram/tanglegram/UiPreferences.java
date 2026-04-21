package tanglegram;

public final class UiPreferences {
    private final String uiFontFamily;
    private final int uiFontSize;
    private final boolean restoreLastWindowSize;
    private final int defaultTanglegramLabelFontSize;
    private final boolean showWindowsOneBuilderWarning;

    public UiPreferences(
            String uiFontFamily,
            int uiFontSize,
            boolean restoreLastWindowSize,
            int defaultTanglegramLabelFontSize,
            boolean showWindowsOneBuilderWarning) {
        this.uiFontFamily = uiFontFamily;
        this.uiFontSize = uiFontSize;
        this.restoreLastWindowSize = restoreLastWindowSize;
        this.defaultTanglegramLabelFontSize = defaultTanglegramLabelFontSize;
        this.showWindowsOneBuilderWarning = showWindowsOneBuilderWarning;
    }

    public String uiFontFamily() {
        return uiFontFamily;
    }

    public int uiFontSize() {
        return uiFontSize;
    }

    public boolean restoreLastWindowSize() {
        return restoreLastWindowSize;
    }

    public int defaultTanglegramLabelFontSize() {
        return defaultTanglegramLabelFontSize;
    }

    public boolean showWindowsOneBuilderWarning() {
        return showWindowsOneBuilderWarning;
    }
}
