package tanglegram;

public final class UiPreferences {
    private final String uiFontFamily;
    private final int uiFontSize;
    private final boolean restoreLastWindowSize;
    private final int defaultTanglegramLabelFontSize;

    public UiPreferences(
            String uiFontFamily,
            int uiFontSize,
            boolean restoreLastWindowSize,
            int defaultTanglegramLabelFontSize) {
        this.uiFontFamily = uiFontFamily;
        this.uiFontSize = uiFontSize;
        this.restoreLastWindowSize = restoreLastWindowSize;
        this.defaultTanglegramLabelFontSize = defaultTanglegramLabelFontSize;
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
}
