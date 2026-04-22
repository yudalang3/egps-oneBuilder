package tanglegram;

public enum UiLanguage {
    ENGLISH("english", "English"),
    CHINESE("chinese", "中文");

    private final String storageValue;
    private final String displayName;

    UiLanguage(String storageValue, String displayName) {
        this.storageValue = storageValue;
        this.displayName = displayName;
    }

    public String storageValue() {
        return storageValue;
    }

    public boolean isChinese() {
        return this == CHINESE;
    }

    public static UiLanguage fromStoredValue(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if (normalized.equals("zh") || normalized.equals("zh-cn") || normalized.equals("zh_cn")
                || normalized.equals("cn") || normalized.equals("chinese") || normalized.equals("中文")) {
            return CHINESE;
        }
        return ENGLISH;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
