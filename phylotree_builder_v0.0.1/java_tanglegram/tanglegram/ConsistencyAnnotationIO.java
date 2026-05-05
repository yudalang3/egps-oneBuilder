package tanglegram;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ConsistencyAnnotationIO {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ConsistencyAnnotationIO() {
    }

    static List<ConsistencyAnnotation> readTsv(Path path) throws IOException {
        List<ConsistencyAnnotation> annotations = new ArrayList<>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
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
                throw new IOException("Invalid annotation TSV row " + (index + 1) + ": expected clade/cluster and color columns.");
            }
            String widthText = pieces.length >= 3 ? pieces[2] : "";
            annotations.add(parseRow(pieces[0], pieces[1], widthText, index + 1));
        }
        return annotations;
    }

    static void writeTsv(Path path, List<ConsistencyAnnotation> annotations) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# clade_or_cluster\tcolor_with_alpha\tribbon_width");
        lines.add("# exported at " + TIMESTAMP_FORMAT.format(LocalDateTime.now()));
        for (ConsistencyAnnotation annotation : annotations) {
            lines.add(annotation.leafNamesText() + "\t" + annotation.colorText() + "\t" + annotation.ribbonWidthText());
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    static List<ConsistencyAnnotation> parseTableRows(List<String[]> rows) throws IOException {
        List<ConsistencyAnnotation> annotations = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            String[] row = rows.get(index);
            String cladeText = row.length > 0 ? row[0] : "";
            String colorText = row.length > 1 ? row[1] : "";
            String widthText = row.length > 2 ? row[2] : "";
            if ((cladeText == null || cladeText.trim().isEmpty())
                    && (colorText == null || colorText.trim().isEmpty())
                    && (widthText == null || widthText.trim().isEmpty())) {
                continue;
            }
            annotations.add(parseRow(cladeText, colorText, widthText, index + 1));
        }
        return annotations;
    }

    static ConsistencyAnnotation parseRow(String cladeText, String colorText, int rowNumber) throws IOException {
        return parseRow(cladeText, colorText, "", rowNumber);
    }

    static ConsistencyAnnotation parseRow(String cladeText, String colorText, String widthText, int rowNumber) throws IOException {
        List<String> leafNames = parseLeafNames(cladeText, rowNumber);
        Color color = parseColor(colorText, rowNumber);
        double ribbonWidth = parseRibbonWidth(widthText, rowNumber);
        return new ConsistencyAnnotation(leafNames, color, ribbonWidth);
    }

    static List<String> parseLeafNames(String cladeText, int rowNumber) throws IOException {
        Set<String> leafNames = new LinkedHashSet<>();
        String safeText = cladeText == null ? "" : cladeText;
        for (String piece : safeText.split(",")) {
            String leafName = piece.trim();
            if (!leafName.isEmpty()) {
                leafNames.add(leafName);
            }
        }
        if (leafNames.isEmpty()) {
            throw new IOException("Invalid annotation row " + rowNumber + ": clade/cluster leaf list is empty.");
        }
        return List.copyOf(leafNames);
    }

    static Color parseColor(String colorText, int rowNumber) throws IOException {
        String normalized = colorText == null ? "" : colorText.trim();
        if (!normalized.startsWith("#")) {
            throw new IOException("Invalid annotation row " + rowNumber + ": color must start with #.");
        }
        String hex = normalized.substring(1);
        if (hex.length() != 6 && hex.length() != 8) {
            throw new IOException("Invalid annotation row " + rowNumber + ": color must be #RRGGBB or #RRGGBBAA.");
        }
        try {
            int red = Integer.parseInt(hex.substring(0, 2), 16);
            int green = Integer.parseInt(hex.substring(2, 4), 16);
            int blue = Integer.parseInt(hex.substring(4, 6), 16);
            int alpha = hex.length() == 8 ? Integer.parseInt(hex.substring(6, 8), 16) : 255;
            return new Color(red, green, blue, alpha);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid annotation row " + rowNumber + ": color contains non-hex characters.");
        }
    }

    static double parseRibbonWidth(String widthText, int rowNumber) throws IOException {
        String normalized = widthText == null ? "" : widthText.trim();
        if (normalized.isEmpty()) {
            return ConsistencyAnnotation.DEFAULT_RIBBON_WIDTH;
        }
        try {
            double width = Double.parseDouble(normalized);
            if (!Double.isFinite(width) || width <= 0.0d) {
                throw new IOException("Invalid annotation row " + rowNumber + ": ribbon width must be greater than 0.");
            }
            return width;
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid annotation row " + rowNumber + ": ribbon width must be a number.");
        }
    }
}
