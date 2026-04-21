package tanglegram;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

final class TreeImportConfigIO {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TreeImportConfigIO() {
    }

    static List<ImportedTreeSpec> readTsv(Path configPath) throws IOException {
        List<ImportedTreeSpec> importedTrees = new ArrayList<>();
        List<String> lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line == null) {
                continue;
            }
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }
            String[] pieces = line.split("\t", 3);
            if (pieces.length < 2) {
                throw new IOException("Invalid TSV row " + (index + 1) + ": expected label and path columns.");
            }
            String label = pieces[0].trim();
            String pathText = normalizePathText(pieces[1]);
            if (label.isEmpty()) {
                throw new IOException("Invalid TSV row " + (index + 1) + ": label is empty.");
            }
            if (pathText.isEmpty()) {
                throw new IOException("Invalid TSV row " + (index + 1) + ": path is empty.");
            }
            Path treePath = Path.of(pathText).toAbsolutePath().normalize();
            if (!Files.isRegularFile(treePath)) {
                throw new IOException("Tree file does not exist at TSV row " + (index + 1) + ": " + treePath);
            }
            importedTrees.add(new ImportedTreeSpec(treePath, label));
        }
        return importedTrees;
    }

    static void writeTsv(Path configPath, List<ImportedTreeSpec> importedTrees) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# label\tpath");
        lines.add("# exported at " + TIMESTAMP_FORMAT.format(LocalDateTime.now()));
        for (ImportedTreeSpec importedTree : importedTrees) {
            lines.add(importedTree.label() + "\t" + importedTree.path());
        }
        Files.write(configPath, lines, StandardCharsets.UTF_8);
    }

    static String normalizePathText(String pathText) {
        if (pathText == null) {
            return "";
        }
        String normalized = pathText.trim();
        if (normalized.length() >= 2) {
            boolean quotedWithDouble = normalized.startsWith("\"") && normalized.endsWith("\"");
            boolean quotedWithSingle = normalized.startsWith("'") && normalized.endsWith("'");
            if (quotedWithDouble || quotedWithSingle) {
                normalized = normalized.substring(1, normalized.length() - 1).trim();
            }
        }
        return normalized;
    }
}
