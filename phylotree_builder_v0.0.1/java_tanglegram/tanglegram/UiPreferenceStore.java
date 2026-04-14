package tanglegram;

import java.awt.Dimension;
import java.awt.Font;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.UIManager;

public final class UiPreferenceStore {
    private static final String KEY_FONT_FAMILY = "ui.font.family";
    private static final String KEY_FONT_SIZE = "ui.font.size";
    private static final String KEY_RESTORE_WINDOW_SIZE = "ui.restoreWindowSize";
    private static final String KEY_TANGLEGRAM_LABEL_FONT_SIZE = "ui.tanglegram.labelFontSize";
    private static final String KEY_WINDOW_PREFIX = "window.";
    private static final String KEY_WIDTH_SUFFIX = ".width";
    private static final String KEY_HEIGHT_SUFFIX = ".height";

    private static Preferences preferencesNode = Preferences.userNodeForPackage(UiPreferenceStore.class).node("ui");
    private static String defaultFontFamily;
    private static Integer defaultFontSize;

    private UiPreferenceStore() {
    }

    public static void captureLookAndFeelDefaults() {
        if (defaultFontFamily != null && defaultFontSize != null) {
            return;
        }
        Font labelFont = UIManager.getFont("Label.font");
        if (labelFont == null) {
            labelFont = new javax.swing.JLabel().getFont();
        }
        defaultFontFamily = labelFont.getFamily();
        defaultFontSize = Integer.valueOf(labelFont.getSize());
    }

    public static UiPreferences defaultPreferences() {
        captureLookAndFeelDefaults();
        return new UiPreferences(defaultFontFamily, defaultFontSize.intValue(), true, 12);
    }

    public static UiPreferences load() {
        UiPreferences defaults = defaultPreferences();
        return new UiPreferences(
                preferencesNode.get(KEY_FONT_FAMILY, defaults.uiFontFamily()),
                preferencesNode.getInt(KEY_FONT_SIZE, defaults.uiFontSize()),
                preferencesNode.getBoolean(KEY_RESTORE_WINDOW_SIZE, defaults.restoreLastWindowSize()),
                preferencesNode.getInt(KEY_TANGLEGRAM_LABEL_FONT_SIZE, defaults.defaultTanglegramLabelFontSize()));
    }

    public static void save(UiPreferences preferences) {
        preferencesNode.put(KEY_FONT_FAMILY, preferences.uiFontFamily());
        preferencesNode.putInt(KEY_FONT_SIZE, preferences.uiFontSize());
        preferencesNode.putBoolean(KEY_RESTORE_WINDOW_SIZE, preferences.restoreLastWindowSize());
        preferencesNode.putInt(KEY_TANGLEGRAM_LABEL_FONT_SIZE, preferences.defaultTanglegramLabelFontSize());
        flushQuietly();
    }

    public static Dimension resolveWindowSize(String windowKey, Dimension defaultSize) {
        UiPreferences preferences = load();
        if (!preferences.restoreLastWindowSize()) {
            return new Dimension(defaultSize);
        }
        int width = preferencesNode.getInt(windowPreferenceKey(windowKey) + KEY_WIDTH_SUFFIX, -1);
        int height = preferencesNode.getInt(windowPreferenceKey(windowKey) + KEY_HEIGHT_SUFFIX, -1);
        if (width > 0 && height > 0) {
            return new Dimension(width, height);
        }
        return new Dimension(defaultSize);
    }

    public static void saveWindowSize(String windowKey, Dimension size) {
        if (size == null || size.width <= 0 || size.height <= 0) {
            return;
        }
        preferencesNode.putInt(windowPreferenceKey(windowKey) + KEY_WIDTH_SUFFIX, size.width);
        preferencesNode.putInt(windowPreferenceKey(windowKey) + KEY_HEIGHT_SUFFIX, size.height);
        flushQuietly();
    }

    public static void useTestNode(String nodePath) {
        preferencesNode = Preferences.userRoot().node(nodePath);
    }

    public static void resetNodeForTests() {
        preferencesNode = Preferences.userNodeForPackage(UiPreferenceStore.class).node("ui");
    }

    public static void clearNodeForTests() {
        try {
            preferencesNode.clear();
            for (String childName : preferencesNode.childrenNames()) {
                preferencesNode.node(childName).removeNode();
            }
            flushQuietly();
        } catch (BackingStoreException ignored) {
        }
        defaultFontFamily = null;
        defaultFontSize = null;
    }

    private static String windowPreferenceKey(String windowKey) {
        return KEY_WINDOW_PREFIX + windowKey;
    }

    private static void flushQuietly() {
        try {
            preferencesNode.flush();
        } catch (BackingStoreException ignored) {
        }
    }
}
