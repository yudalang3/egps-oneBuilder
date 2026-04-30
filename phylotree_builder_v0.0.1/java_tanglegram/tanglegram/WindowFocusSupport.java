package tanglegram;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class WindowFocusSupport {
    private WindowFocusSupport() {
    }

    public static void requestFrameFocus(JFrame frame) {
        if (frame == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            frame.toFront();
            frame.requestFocus();
        });
    }
}
