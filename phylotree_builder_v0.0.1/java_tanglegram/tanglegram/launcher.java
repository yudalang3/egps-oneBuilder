package tanglegram;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.SwingUtilities;

public final class launcher {
    private launcher() {
    }

    public static void main(String[] args) {
        LauncherOptions options = LauncherOptions.parse(args);
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            TanglegramFrame frame = new TanglegramFrame();
            frame.setVisible(true);
            frame.handleStartup(options);
        });
    }
}
