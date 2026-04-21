package tanglegram;

import java.awt.Dimension;
import java.awt.Font;
import java.nio.file.Path;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.UIManager;

public final class UiPreferenceStore {
    private static final String KEY_FONT_FAMILY = "ui.font.family";
    private static final String KEY_FONT_SIZE = "ui.font.size";
    private static final String KEY_RESTORE_WINDOW_SIZE = "ui.restoreWindowSize";
    private static final String KEY_TANGLEGRAM_LABEL_FONT_SIZE = "ui.tanglegram.labelFontSize";
    private static final String KEY_SHOW_WINDOWS_ONEBUILDER_WARNING = "ui.onebuilder.showWindowsWarning";
    private static final String KEY_WINDOW_PREFIX = "window.";
    private static final String KEY_WIDTH_SUFFIX = ".width";
    private static final String KEY_HEIGHT_SUFFIX = ".height";
    private static final String KEY_RECENT_RUNNING_RESULT_DIR = "recent.runningResultDir";
    private static final String KEY_RECENT_CONFIG_FILE = "recent.configFile";
    private static final String KEY_RECENT_TREE_FILE_DIR = "recent.treeFileDir";
    private static final String KEY_RECENT_ONEBUILDER_INPUT_DIR = "recent.onebuilder.inputDir";
    private static final String KEY_RECENT_ONEBUILDER_OUTPUT_DIR = "recent.onebuilder.outputDir";

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
        return new UiPreferences(defaultFontFamily, defaultFontSize.intValue(), true, 12, true);
    }

    public static UiPreferences load() {
        UiPreferences defaults = defaultPreferences();
        return new UiPreferences(
                preferencesNode.get(KEY_FONT_FAMILY, defaults.uiFontFamily()),
                preferencesNode.getInt(KEY_FONT_SIZE, defaults.uiFontSize()),
                preferencesNode.getBoolean(KEY_RESTORE_WINDOW_SIZE, defaults.restoreLastWindowSize()),
                preferencesNode.getInt(KEY_TANGLEGRAM_LABEL_FONT_SIZE, defaults.defaultTanglegramLabelFontSize()),
                preferencesNode.getBoolean(KEY_SHOW_WINDOWS_ONEBUILDER_WARNING, defaults.showWindowsOneBuilderWarning()));
    }

    public static void save(UiPreferences preferences) {
        preferencesNode.put(KEY_FONT_FAMILY, preferences.uiFontFamily());
        preferencesNode.putInt(KEY_FONT_SIZE, preferences.uiFontSize());
        preferencesNode.putBoolean(KEY_RESTORE_WINDOW_SIZE, preferences.restoreLastWindowSize());
        preferencesNode.putInt(KEY_TANGLEGRAM_LABEL_FONT_SIZE, preferences.defaultTanglegramLabelFontSize());
        preferencesNode.putBoolean(KEY_SHOW_WINDOWS_ONEBUILDER_WARNING, preferences.showWindowsOneBuilderWarning());
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

    public static Path loadRecentRunningResultDir() {
        return loadPath(KEY_RECENT_RUNNING_RESULT_DIR);
    }

    public static void saveRecentRunningResultDir(Path path) {
        savePath(KEY_RECENT_RUNNING_RESULT_DIR, path);
    }

    public static Path loadRecentConfigFile() {
        return loadPath(KEY_RECENT_CONFIG_FILE);
    }

    public static void saveRecentConfigFile(Path path) {
        savePath(KEY_RECENT_CONFIG_FILE, path);
    }

    public static Path loadRecentTreeFileDir() {
        return loadPath(KEY_RECENT_TREE_FILE_DIR);
    }

    public static void saveRecentTreeFileDir(Path path) {
        savePath(KEY_RECENT_TREE_FILE_DIR, path);
    }

    public static Path loadRecentOneBuilderInputDir() {
        return loadPath(KEY_RECENT_ONEBUILDER_INPUT_DIR);
    }

    public static void saveRecentOneBuilderInputDir(Path path) {
        savePath(KEY_RECENT_ONEBUILDER_INPUT_DIR, path);
    }

    public static Path loadRecentOneBuilderOutputDir() {
        return loadPath(KEY_RECENT_ONEBUILDER_OUTPUT_DIR);
    }

    public static void saveRecentOneBuilderOutputDir(Path path) {
        savePath(KEY_RECENT_ONEBUILDER_OUTPUT_DIR, path);
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

    private static Path loadPath(String key) {
        String value = preferencesNode.get(key, null);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Path.of(value).toAbsolutePath().normalize();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void savePath(String key, Path path) {
        if (path == null) {
            preferencesNode.remove(key);
        } else {
            preferencesNode.put(key, path.toAbsolutePath().normalize().toString());
        }
        flushQuietly();
    }

    private static void flushQuietly() {
        try {
            preferencesNode.flush();
        } catch (BackingStoreException ignored) {
        }
    }
}
