package tanglegram;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import onebuilder.ProteinStructureTreeCommand;

public final class TreeSummaryLoader {
    private TreeSummaryLoader() {
    }

    public static TreeSummaryLoadResult load(Path treeSummaryDir) throws IOException {
        Path normalizedTreeSummaryDir = treeSummaryDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedTreeSummaryDir)) {
            throw new IOException("Directory does not exist: " + normalizedTreeSummaryDir);
        }

        Path outputRootDir = normalizedTreeSummaryDir.getParent();
        if (outputRootDir == null) {
            outputRootDir = normalizedTreeSummaryDir;
        }

        Path metadataFile = normalizedTreeSummaryDir.resolve("tree_meta_data.tsv");
        Map<TreeMethod, String> metadataEntries = readMetadata(metadataFile);
        List<String> warnings = new ArrayList<>();
        if (!Files.exists(metadataFile)) {
            warnings.add("tree_meta_data.tsv not found, using standard output layout fallback.");
        }

        Map<TreeMethod, Path> resolvedTrees = resolveAvailableTrees(
                metadataEntries,
                normalizedTreeSummaryDir,
                outputRootDir,
                warnings);
        List<TreeMethod> missingMethods = missingMethods(resolvedTrees);
        Map<TreeMethod, Map<TreeMethod, Double>> treeDistances = readDoubleMatrix(
                normalizedTreeSummaryDir.resolve("tree_distance_matrix.tsv"));
        Map<TreeMethod, Map<TreeMethod, Integer>> robinsonFouldsDistances = readIntegerMatrix(
                normalizedTreeSummaryDir.resolve("rf_distance_matrix.tsv"));

        return new TreeSummaryLoadResult(
                normalizedTreeSummaryDir,
                outputRootDir,
                resolvedTrees,
                treeDistances,
                robinsonFouldsDistances,
                missingMethods,
                warnings);
    }

    public static TreeSummaryLoadResult loadRunResult(Path outputRootDir) throws IOException {
        Path normalizedOutputRootDir = outputRootDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedOutputRootDir)) {
            throw new IOException("Directory does not exist: " + normalizedOutputRootDir);
        }

        Path treeSummaryDir = normalizedOutputRootDir.resolve("tree_summary");
        Map<TreeMethod, String> metadataEntries = readMetadata(treeSummaryDir.resolve("tree_meta_data.tsv"));
        List<String> warnings = new ArrayList<>();
        if (!Files.isDirectory(treeSummaryDir)) {
            warnings.add("tree_summary not found, using method output directories directly.");
        } else if (metadataEntries.isEmpty()) {
            warnings.add("tree_meta_data.tsv not found, using standard output layout fallback.");
        }

        Map<TreeMethod, Path> resolvedTrees = resolveAvailableTrees(
                metadataEntries,
                treeSummaryDir,
                normalizedOutputRootDir,
                warnings);
        if (resolvedTrees.size() < 2) {
            throw new IOException("At least two readable tree files are required in the selected result folder.");
        }

        return new TreeSummaryLoadResult(
                treeSummaryDir.toAbsolutePath().normalize(),
                normalizedOutputRootDir,
                resolvedTrees,
                readDoubleMatrix(treeSummaryDir.resolve("tree_distance_matrix.tsv")),
                readIntegerMatrix(treeSummaryDir.resolve("rf_distance_matrix.tsv")),
                missingMethods(resolvedTrees),
                warnings);
    }

    private static Map<TreeMethod, Path> resolveAvailableTrees(
            Map<TreeMethod, String> metadataEntries,
            Path treeSummaryDir,
            Path outputRootDir,
            List<String> warnings) {
        Map<TreeMethod, Path> resolvedTrees = new EnumMap<>(TreeMethod.class);
        List<TreeMethod> missingMethods = new ArrayList<>();
        for (TreeMethod method : TreeMethod.DISPLAY_ORDER) {
            Path resolvedTree = resolveTree(method, metadataEntries.get(method), treeSummaryDir, outputRootDir);
            if (resolvedTree == null && method == TreeMethod.PROTEIN_STRUCTURE) {
                resolvedTree = resolveOrBuildProteinStructureTree(outputRootDir, warnings);
            }
            if (resolvedTree == null) {
                missingMethods.add(method);
            } else {
                resolvedTrees.put(method, resolvedTree);
            }
        }
        if (!missingMethods.isEmpty()) {
            warnings.add("Missing methods: " + formatMethods(missingMethods));
        }
        return resolvedTrees;
    }

    private static List<TreeMethod> missingMethods(Map<TreeMethod, Path> resolvedTrees) {
        List<TreeMethod> missingMethods = new ArrayList<>();
        for (TreeMethod method : TreeMethod.DISPLAY_ORDER) {
            if (method.optional()) {
                continue;
            }
            if (!resolvedTrees.containsKey(method)) {
                missingMethods.add(method);
            }
        }
        return missingMethods;
    }

    private static Path resolveOrBuildProteinStructureTree(Path outputRootDir, List<String> warnings) {
        Path structureTree = outputRootDir.resolve("protein_structure").resolve("structure_tree.nwk").normalize();
        if (Files.isRegularFile(structureTree)) {
            return structureTree.toAbsolutePath().normalize();
        }

        Path distanceMatrix = outputRootDir.resolve("protein_structure").resolve("distance_matrix.tsv").normalize();
        if (!Files.isRegularFile(distanceMatrix)) {
            return null;
        }

        try {
            ProteinStructureTreeCommand.buildTree("NJ", distanceMatrix, structureTree);
            warnings.add("ProteinCluster tree was generated from protein_structure/distance_matrix.tsv.");
            return structureTree.toAbsolutePath().normalize();
        } catch (Exception exception) {
            warnings.add("ProteinCluster tree could not be generated: " + exception.getMessage());
            return null;
        }
    }

    private static Map<TreeMethod, String> readMetadata(Path metadataFile) throws IOException {
        Map<TreeMethod, String> metadataEntries = new EnumMap<>(TreeMethod.class);
        if (!Files.exists(metadataFile)) {
            return metadataEntries;
        }

        for (String line : Files.readAllLines(metadataFile, StandardCharsets.UTF_8)) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String[] pieces = line.split("\t", 2);
            if (pieces.length != 2) {
                continue;
            }

            TreeMethod method = TreeMethod.fromMetadataKey(pieces[0].trim());
            if (method != null) {
                metadataEntries.put(method, pieces[1].trim());
            }
        }

        return metadataEntries;
    }

    private static Map<TreeMethod, Map<TreeMethod, Double>> readDoubleMatrix(Path matrixFile) throws IOException {
        Map<TreeMethod, Map<TreeMethod, Double>> matrix = new EnumMap<>(TreeMethod.class);
        if (!Files.exists(matrixFile)) {
            return matrix;
        }

        List<String> lines = Files.readAllLines(matrixFile, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return matrix;
        }
        List<TreeMethod> columns = parseMatrixHeader(lines.get(0));
        for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
            String[] pieces = lines.get(lineIndex).split("\t", -1);
            if (pieces.length == 0) {
                continue;
            }
            TreeMethod rowMethod = TreeMethod.fromMetadataKey(pieces[0].trim());
            if (rowMethod == null) {
                continue;
            }
            EnumMap<TreeMethod, Double> row = new EnumMap<>(TreeMethod.class);
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                int pieceIndex = columnIndex + 1;
                if (pieceIndex >= pieces.length || columns.get(columnIndex) == null) {
                    continue;
                }
                try {
                    row.put(columns.get(columnIndex), Double.valueOf(pieces[pieceIndex].trim()));
                } catch (NumberFormatException ignored) {
                    // Keep loading available matrix entries even if one cell is malformed.
                }
            }
            matrix.put(rowMethod, row);
        }
        return matrix;
    }

    private static Map<TreeMethod, Map<TreeMethod, Integer>> readIntegerMatrix(Path matrixFile) throws IOException {
        Map<TreeMethod, Map<TreeMethod, Integer>> matrix = new EnumMap<>(TreeMethod.class);
        Map<TreeMethod, Map<TreeMethod, Double>> doubleMatrix = readDoubleMatrix(matrixFile);
        for (Map.Entry<TreeMethod, Map<TreeMethod, Double>> rowEntry : doubleMatrix.entrySet()) {
            EnumMap<TreeMethod, Integer> row = new EnumMap<>(TreeMethod.class);
            for (Map.Entry<TreeMethod, Double> cellEntry : rowEntry.getValue().entrySet()) {
                row.put(cellEntry.getKey(), Integer.valueOf((int) Math.round(cellEntry.getValue().doubleValue())));
            }
            matrix.put(rowEntry.getKey(), row);
        }
        return matrix;
    }

    private static List<TreeMethod> parseMatrixHeader(String headerLine) {
        List<TreeMethod> columns = new ArrayList<>();
        String[] pieces = headerLine.split("\t", -1);
        int startIndex = matrixHeaderHasRowNameColumn(headerLine) ? 1 : 0;
        for (int index = startIndex; index < pieces.length; index++) {
            columns.add(TreeMethod.fromMetadataKey(pieces[index].trim()));
        }
        return columns;
    }

    private static boolean matrixHeaderHasRowNameColumn(String headerLine) {
        String[] pieces = headerLine.split("\t", -1);
        return pieces.length > 0 && TreeMethod.fromMetadataKey(pieces[0].trim()) == null;
    }

    private static Path resolveTree(
            TreeMethod method,
            String metadataValue,
            Path treeSummaryDir,
            Path outputRootDir) {
        for (Path candidate : metadataCandidates(metadataValue, treeSummaryDir, outputRootDir)) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        for (Path fallbackCandidate : method.fallbackCandidates(outputRootDir)) {
            if (Files.isRegularFile(fallbackCandidate)) {
                return fallbackCandidate.toAbsolutePath().normalize();
            }
        }

        return null;
    }

    private static List<Path> metadataCandidates(String metadataValue, Path treeSummaryDir, Path outputRootDir) {
        List<Path> candidates = new ArrayList<>();
        if (metadataValue == null || metadataValue.isBlank() || "NULL".equalsIgnoreCase(metadataValue)) {
            return candidates;
        }

        try {
            Path metadataPath = java.nio.file.Paths.get(metadataValue);
            if (metadataPath.isAbsolute()) {
                candidates.add(metadataPath.normalize());
            } else {
                candidates.add(treeSummaryDir.resolve(metadataPath).normalize());
                candidates.add(outputRootDir.resolve(metadataPath).normalize());
            }
        } catch (InvalidPathException ignored) {
            return candidates;
        }

        return candidates;
    }

    private static String formatMethods(List<TreeMethod> methods) {
        List<String> labels = new ArrayList<>();
        for (TreeMethod method : methods) {
            labels.add(method.shortLabel());
        }
        return labels.toString();
    }
}
