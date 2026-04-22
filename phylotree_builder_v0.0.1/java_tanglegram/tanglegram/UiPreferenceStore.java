package tanglegram;

import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import javax.swing.UIManager;

public final class UiPreferenceStore {
    private static final String KEY_FONT_FAMILY = "ui.font.family";
    private static final String KEY_FONT_SIZE = "ui.font.size";
    private static final String KEY_RESTORE_WINDOW_SIZE = "ui.restoreWindowSize";
    private static final String KEY_TANGLEGRAM_LABEL_FONT_SIZE = "ui.tanglegram.labelFontSize";
    private static final String KEY_SHOW_WINDOWS_ONEBUILDER_WARNING = "ui.onebuilder.showWindowsWarning";
    private static final String KEY_UI_LANGUAGE = "ui.language";
    private static final String KEY_WINDOW_PREFIX = "window.";
    private static final String KEY_WIDTH_SUFFIX = ".width";
    private static final String KEY_HEIGHT_SUFFIX = ".height";
    private static final String KEY_RECENT_RUNNING_RESULT_DIR = "recent.runningResultDir";
    private static final String KEY_RECENT_CONFIG_FILE = "recent.configFile";
    private static final String KEY_RECENT_TREE_FILE_DIR = "recent.treeFileDir";
    private static final String KEY_RECENT_ONEBUILDER_INPUT_DIR = "recent.onebuilder.inputDir";
    private static final String KEY_RECENT_ONEBUILDER_OUTPUT_DIR = "recent.onebuilder.outputDir";
    private static final Object STORE_LOCK = new Object();

    private static Path propertiesFile = defaultPropertiesPath();
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
        return new UiPreferences(defaultFontFamily, defaultFontSize.intValue(), true, 12, true, UiLanguage.ENGLISH);
    }

    public static UiPreferences load() {
        Properties properties = loadProperties();
        UiPreferences defaults = defaultPreferences();
        return new UiPreferences(
                properties.getProperty(KEY_FONT_FAMILY, defaults.uiFontFamily()),
                parseInt(properties, KEY_FONT_SIZE, defaults.uiFontSize()),
                parseBoolean(properties, KEY_RESTORE_WINDOW_SIZE, defaults.restoreLastWindowSize()),
                parseInt(properties, KEY_TANGLEGRAM_LABEL_FONT_SIZE, defaults.defaultTanglegramLabelFontSize()),
                parseBoolean(properties, KEY_SHOW_WINDOWS_ONEBUILDER_WARNING, defaults.showWindowsOneBuilderWarning()),
                UiLanguage.fromStoredValue(properties.getProperty(KEY_UI_LANGUAGE, defaults.uiLanguage().storageValue())));
    }

    public static void save(UiPreferences preferences) {
        Properties properties = loadProperties();
        properties.setProperty(KEY_FONT_FAMILY, preferences.uiFontFamily());
        properties.setProperty(KEY_FONT_SIZE, Integer.toString(preferences.uiFontSize()));
        properties.setProperty(KEY_RESTORE_WINDOW_SIZE, Boolean.toString(preferences.restoreLastWindowSize()));
        properties.setProperty(KEY_TANGLEGRAM_LABEL_FONT_SIZE, Integer.toString(preferences.defaultTanglegramLabelFontSize()));
        properties.setProperty(KEY_SHOW_WINDOWS_ONEBUILDER_WARNING, Boolean.toString(preferences.showWindowsOneBuilderWarning()));
        properties.setProperty(KEY_UI_LANGUAGE, preferences.uiLanguage().storageValue());
        saveProperties(properties);
    }

    public static Dimension resolveWindowSize(String windowKey, Dimension defaultSize) {
        UiPreferences preferences = load();
        if (!preferences.restoreLastWindowSize()) {
            return new Dimension(defaultSize);
        }
        Properties properties = loadProperties();
        int width = parseInt(properties, windowPreferenceKey(windowKey) + KEY_WIDTH_SUFFIX, -1);
        int height = parseInt(properties, windowPreferenceKey(windowKey) + KEY_HEIGHT_SUFFIX, -1);
        if (width > 0 && height > 0) {
            return new Dimension(width, height);
        }
        return new Dimension(defaultSize);
    }

    public static void saveWindowSize(String windowKey, Dimension size) {
        if (size == null || size.width <= 0 || size.height <= 0) {
            return;
        }
        Properties properties = loadProperties();
        properties.setProperty(windowPreferenceKey(windowKey) + KEY_WIDTH_SUFFIX, Integer.toString(size.width));
        properties.setProperty(windowPreferenceKey(windowKey) + KEY_HEIGHT_SUFFIX, Integer.toString(size.height));
        saveProperties(properties);
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
        propertiesFile = testPropertiesPath(nodePath);
    }

    public static void resetNodeForTests() {
        propertiesFile = defaultPropertiesPath();
    }

    public static void clearNodeForTests() {
        synchronized (STORE_LOCK) {
            try {
                Files.deleteIfExists(propertiesFile);
            } catch (IOException ignored) {
            }
        }
        defaultFontFamily = null;
        defaultFontSize = null;
    }

    private static String windowPreferenceKey(String windowKey) {
        return KEY_WINDOW_PREFIX + windowKey;
    }

    private static Path loadPath(String key) {
        String value = loadProperties().getProperty(key);
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
        Properties properties = loadProperties();
        if (path == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, path.toAbsolutePath().normalize().toString());
        }
        saveProperties(properties);
    }

    private static Properties loadProperties() {
        synchronized (STORE_LOCK) {
            Properties properties = new Properties();
            if (!Files.isRegularFile(propertiesFile)) {
                return properties;
            }
            try (Reader reader = Files.newBufferedReader(propertiesFile)) {
                properties.load(reader);
            } catch (IOException ignored) {
            }
            return properties;
        }
    }

    private static void saveProperties(Properties properties) {
        synchronized (STORE_LOCK) {
            try {
                Path parent = propertiesFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                try (Writer writer = Files.newBufferedWriter(propertiesFile)) {
                    properties.store(writer, "eGPS oneBuilder UI preferences");
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to save UI preferences to " + propertiesFile, exception);
            }
        }
    }

    private static int parseInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static boolean parseBoolean(Properties properties, String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private static Path defaultPropertiesPath() {
        String userHome = System.getProperty("user.home", ".");
        return Path.of(userHome, ".egps.onebuilder.prop").toAbsolutePath().normalize();
    }

    private static Path testPropertiesPath(String nodePath) {
        String sanitized = nodePath == null || nodePath.isBlank()
                ? "default"
                : nodePath.replaceAll("[^A-Za-z0-9._-]+", "_");
        String tempRoot = System.getProperty("java.io.tmpdir", System.getProperty("user.home", "."));
        return Path.of(tempRoot, "egps-onebuilder-tests", sanitized + ".prop").toAbsolutePath().normalize();
    }
}
