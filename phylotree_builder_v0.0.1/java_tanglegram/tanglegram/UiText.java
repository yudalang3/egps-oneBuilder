package tanglegram;

public final class UiText {
    private UiText() {
    }

    public static String text(UiPreferences preferences, String english, String chinese) {
        if (preferences != null && preferences.uiLanguage().isChinese()) {
            return chinese;
        }
        return english;
    }

    public static String text(String english, String chinese) {
        return text(UiPreferenceStore.load(), english, chinese);
    }
}
