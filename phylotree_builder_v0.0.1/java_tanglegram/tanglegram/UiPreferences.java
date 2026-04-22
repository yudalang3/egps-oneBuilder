package tanglegram;

public final class UiPreferences {
    private final String uiFontFamily;
    private final int uiFontSize;
    private final boolean restoreLastWindowSize;
    private final int defaultTanglegramLabelFontSize;
    private final boolean showWindowsOneBuilderWarning;
    private final UiLanguage uiLanguage;

    public UiPreferences(
            String uiFontFamily,
            int uiFontSize,
            boolean restoreLastWindowSize,
            int defaultTanglegramLabelFontSize,
            boolean showWindowsOneBuilderWarning) {
        this(uiFontFamily, uiFontSize, restoreLastWindowSize, defaultTanglegramLabelFontSize, showWindowsOneBuilderWarning,
                UiLanguage.ENGLISH);
    }

    public UiPreferences(
            String uiFontFamily,
            int uiFontSize,
            boolean restoreLastWindowSize,
            int defaultTanglegramLabelFontSize,
            boolean showWindowsOneBuilderWarning,
            UiLanguage uiLanguage) {
        this.uiFontFamily = uiFontFamily;
        this.uiFontSize = uiFontSize;
        this.restoreLastWindowSize = restoreLastWindowSize;
        this.defaultTanglegramLabelFontSize = defaultTanglegramLabelFontSize;
        this.showWindowsOneBuilderWarning = showWindowsOneBuilderWarning;
        this.uiLanguage = uiLanguage == null ? UiLanguage.ENGLISH : uiLanguage;
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

    public UiLanguage uiLanguage() {
        return uiLanguage;
    }
}
