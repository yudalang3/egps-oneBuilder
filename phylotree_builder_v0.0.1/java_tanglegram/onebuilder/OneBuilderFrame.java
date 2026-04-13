package onebuilder;

import java.awt.BorderLayout;
import java.nio.file.Path;
import javax.swing.JFrame;

final class OneBuilderFrame extends JFrame {
    OneBuilderFrame(Path scriptDirectory) {
        super("eGPS oneBuilder");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(new OneBuilderWorkspacePanel(scriptDirectory), BorderLayout.CENTER);
        setSize(1520, 960);
        setLocationRelativeTo(null);
    }
}
