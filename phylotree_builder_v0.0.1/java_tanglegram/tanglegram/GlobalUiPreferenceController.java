package tanglegram;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Window;
import java.util.Enumeration;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

public final class GlobalUiPreferenceController {
    private GlobalUiPreferenceController() {
    }

    public static void applyStoredPreferencesToLookAndFeel() {
        UiPreferenceStore.captureLookAndFeelDefaults();
        applyLookAndFeelPreferences(UiPreferenceStore.load());
    }

    public static void applyToOpenWindows(UiPreferences preferences) {
        applyLookAndFeelPreferences(preferences);
        for (Window window : Window.getWindows()) {
            if (window == null) {
                continue;
            }
            applyRecursively(window, preferences);
            if (window instanceof PreferenceAware aware) {
                aware.applyPreferences(preferences);
            }
            SwingUtilities.updateComponentTreeUI(window);
            window.invalidate();
            window.validate();
            window.repaint();
        }
    }

    static void applyLookAndFeelPreferences(UiPreferences preferences) {
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        for (Enumeration<Object> keys = defaults.keys(); keys.hasMoreElements();) {
            Object key = keys.nextElement();
            Object value = defaults.get(key);
            if (value instanceof Font font) {
                defaults.put(key, asUiFontResource(font, preferences));
            }
        }
        UIManager.put("defaultFont", new FontUIResource(preferences.uiFontFamily(), Font.PLAIN, preferences.uiFontSize()));
    }

    private static void applyRecursively(Component component, UiPreferences preferences) {
        if (component == null) {
            return;
        }
        Font currentFont = component.getFont();
        if (currentFont != null) {
            component.setFont(asRuntimeFont(currentFont, preferences));
        }
        if (component instanceof JMenuBar menuBar) {
            for (Component menuComponent : menuBar.getComponents()) {
                applyRecursively(menuComponent, preferences);
            }
        }
        if (component instanceof JMenu menu) {
            for (Component menuComponent : menu.getMenuComponents()) {
                applyRecursively(menuComponent, preferences);
            }
        }
        if (component instanceof JComponent jComponent) {
            JPopupMenu popupMenu = jComponent.getComponentPopupMenu();
            if (popupMenu != null) {
                applyRecursively(popupMenu, preferences);
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyRecursively(child, preferences);
            }
        }
    }

    private static FontUIResource asUiFontResource(Font baseFont, UiPreferences preferences) {
        return new FontUIResource(resolveFontFamily(baseFont, preferences), baseFont.getStyle(), preferences.uiFontSize());
    }

    private static Font asRuntimeFont(Font baseFont, UiPreferences preferences) {
        return new Font(resolveFontFamily(baseFont, preferences), baseFont.getStyle(), preferences.uiFontSize());
    }

    private static String resolveFontFamily(Font baseFont, UiPreferences preferences) {
        String family = baseFont.getFamily();
        if (Font.MONOSPACED.equalsIgnoreCase(family) || "Monospaced".equalsIgnoreCase(family)) {
            return Font.MONOSPACED;
        }
        return preferences.uiFontFamily();
    }
}
