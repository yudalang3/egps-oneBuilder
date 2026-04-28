package onebuilder;

import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

final class HowToCitePanel extends JPanel {
    private final Path scriptDirectory;
    private final JTextArea citationArea;

    HowToCitePanel(Path scriptDirectory) {
        super(new BorderLayout(0, 16));
        this.scriptDirectory = scriptDirectory;
        WorkbenchStyles.applyCanvas(this);

        JPanel header = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 6));
        header.add(WorkbenchStyles.createSectionTitle("How to cite"), BorderLayout.NORTH);
        header.add(WorkbenchStyles.createSubtitleLabel("Citation guidance for eGPS-onebuilder and bundled analysis software."), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        citationArea = new JTextArea();
        citationArea.setEditable(false);
        citationArea.setLineWrap(true);
        citationArea.setWrapStyleWord(true);
        WorkbenchStyles.styleMonospaceLog(citationArea);

        JPanel body = WorkbenchStyles.createSurfacePanel(new BorderLayout(0, 8));
        body.add(WorkbenchStyles.createNoteArea(
                "This page loads phylotree_builder_v0.0.1/how_to_cite.md. The repository root how_to_cite.md is a symlink to the same file."),
                BorderLayout.NORTH);
        body.add(new JScrollPane(citationArea), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        loadCitationDocument();
    }

    static Path resolveCitationDocument(Path scriptDirectory) {
        return scriptDirectory.resolve("how_to_cite.md").toAbsolutePath().normalize();
    }

    Path citationDocumentPathForTest() {
        return resolveCitationDocument(scriptDirectory);
    }

    private void loadCitationDocument() {
        citationArea.setText("Loading citation information...");
        Thread worker = new Thread(() -> {
            Path citationDocument = resolveCitationDocument(scriptDirectory);
            String text;
            try {
                text = Files.readString(citationDocument, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                text = "Citation document could not be loaded: " + citationDocument
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + exception.getMessage();
            }
            String finalText = text;
            SwingUtilities.invokeLater(() -> {
                citationArea.setText(finalText);
                citationArea.setCaretPosition(0);
            });
        }, "onebuilder-citation-loader");
        worker.setDaemon(true);
        worker.start();
    }
}
