package onebuilder;

import java.awt.BorderLayout;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import tanglegram.FlatLafBootstrap;
import tanglegram.GlobalUiPreferenceController;
import tanglegram.UiPreferenceStore;
import tanglegram.UiPreferences;

public final class launcher {
    private launcher() {
    }

    public static void main(String[] args) {
        Path scriptDirectory = resolveScriptDirectory();
        SwingUtilities.invokeLater(() -> {
            FlatLafBootstrap.setupFlatLaf();
            UiPreferenceStore.captureLookAndFeelDefaults();
            GlobalUiPreferenceController.applyStoredPreferencesToLookAndFeel();
            OneBuilderFrame frame = new OneBuilderFrame(scriptDirectory);
            frame.setVisible(true);
            showWindowsStartupWarningIfNeeded(frame);
        });
    }

    private static void showWindowsStartupWarningIfNeeded(OneBuilderFrame frame) {
        if (PlatformSupport.current() != PlatformSupport.WINDOWS) {
            return;
        }
        UiPreferences preferences = UiPreferenceStore.load();
        if (!preferences.showWindowsOneBuilderWarning()) {
            return;
        }

        JCheckBox suppressCheckBox = new JCheckBox("Do not show this warning again");
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.add(
                new JLabel(
                        "<html>oneBuilder is running on Windows.<br>Pipeline execution is disabled on this platform, but you can still prepare the workflow and export a reusable config for Linux.</html>"),
                BorderLayout.CENTER);
        content.add(suppressCheckBox, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(
                frame,
                content,
                "Windows Startup Notice",
                JOptionPane.WARNING_MESSAGE);
        if (suppressCheckBox.isSelected()) {
            UiPreferenceStore.save(new UiPreferences(
                    preferences.uiFontFamily(),
                    preferences.uiFontSize(),
                    preferences.restoreLastWindowSize(),
                    preferences.defaultTanglegramLabelFontSize(),
                    false));
        }
    }

    private static Path resolveScriptDirectory() {
        try {
            Path codeSource = Paths.get(launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (java.nio.file.Files.isDirectory(codeSource)) {
                Path parent = codeSource.getParent();
                if (parent != null) {
                    return parent.toAbsolutePath().normalize();
                }
            }
        } catch (URISyntaxException ignored) {
        }
        return Paths.get("").toAbsolutePath().normalize();
    }
}
