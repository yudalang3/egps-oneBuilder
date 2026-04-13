package tanglegram;

import javax.swing.SwingUtilities;

public final class launcher {
    private launcher() {
    }

    public static void main(String[] args) {
        LauncherOptions options = LauncherOptions.parse(args);
        SwingUtilities.invokeLater(() -> {
            FlatLafBootstrap.setupFlatLaf();
            TanglegramFrame frame = new TanglegramFrame();
            frame.setVisible(true);
            frame.handleStartup(options);
        });
    }
}
