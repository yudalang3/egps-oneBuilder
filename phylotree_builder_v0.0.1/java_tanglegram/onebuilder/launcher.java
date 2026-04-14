package onebuilder;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.SwingUtilities;
import tanglegram.FlatLafBootstrap;
import tanglegram.GlobalUiPreferenceController;
import tanglegram.UiPreferenceStore;

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
        });
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
