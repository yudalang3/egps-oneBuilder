package onebuilder;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

final class VisLaunchingPanel extends JPanel {
    private final Path scriptDirectory;
    private final Supplier<Path> outputDirectorySupplier;
    private final JButton launchButton;

    VisLaunchingPanel(Path scriptDirectory, Supplier<Path> outputDirectorySupplier) {
        super(new BorderLayout(0, 16));
        this.scriptDirectory = scriptDirectory;
        this.outputDirectorySupplier = outputDirectorySupplier;
        WorkbenchStyles.applyCanvas(this);

        JPanel header = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 6));
        header.add(WorkbenchStyles.createSectionTitle("Vis. Launching"), BorderLayout.NORTH);
        header.add(WorkbenchStyles.createSubtitleLabel("Highly interactive visualization of various trees"), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel body = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 12));
        body.add(WorkbenchStyles.createNoteArea(
                "Open the standalone Tanglegram viewer and load the current run's tree_summary directory automatically."),
                BorderLayout.CENTER);

        launchButton = new JButton("Launch Tanglegram Viewer");
        launchButton.addActionListener(event -> launchViewer());
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(launchButton);
        body.add(buttonRow, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
    }

    static List<String> buildLaunchCommand(Path scriptDirectory, Path outputDirectory, String classPathSeparator) {
        Path javaTanglegram = scriptDirectory.resolve("java_tanglegram");
        String libWildcard = scriptDirectory.resolve("lib").toString() + java.io.File.separator + "*";
        return Arrays.asList(
                "java",
                "-cp",
                javaTanglegram + classPathSeparator + libWildcard,
                "tanglegram.launcher",
                "-dir",
                outputDirectory.resolve("tree_summary").toString());
    }

    void setReady(boolean ready) {
        launchButton.setEnabled(ready);
    }

    private void launchViewer() {
        Path outputDirectory = outputDirectorySupplier.get();
        if (outputDirectory == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Run the pipeline successfully before launching the standalone Tanglegram viewer.",
                    "eGPS oneBuilder",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<String> command = buildLaunchCommand(scriptDirectory, outputDirectory, java.io.File.pathSeparator);
        try {
            new ProcessBuilder(command)
                    .directory(scriptDirectory.toFile())
                    .start();
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to launch Tanglegram viewer: " + exception.getMessage(),
                    "eGPS oneBuilder",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
