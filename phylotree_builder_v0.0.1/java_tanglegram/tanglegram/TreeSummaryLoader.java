package tanglegram;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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

        Map<TreeMethod, Path> resolvedTrees = new EnumMap<>(TreeMethod.class);
        List<TreeMethod> missingMethods = new ArrayList<>();
        for (TreeMethod method : TreeMethod.DISPLAY_ORDER) {
            Path resolvedTree = resolveTree(method, metadataEntries.get(method), normalizedTreeSummaryDir, outputRootDir);
            if (resolvedTree == null) {
                missingMethods.add(method);
            } else {
                resolvedTrees.put(method, resolvedTree);
            }
        }

        if (!missingMethods.isEmpty()) {
            warnings.add("Missing methods: " + formatMethods(missingMethods));
        }

        return new TreeSummaryLoadResult(normalizedTreeSummaryDir, outputRootDir, resolvedTrees, missingMethods, warnings);
    }

    public static TreeSummaryLoadResult loadRunResult(Path outputRootDir) throws IOException {
        Path normalizedOutputRootDir = outputRootDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedOutputRootDir)) {
            throw new IOException("Directory does not exist: " + normalizedOutputRootDir);
        }

        List<String> missingDirectories = new ArrayList<>();
        for (String directoryName : Arrays.asList(
                "bayesian_method",
                "distance_method",
                "maximum_likelihood",
                "parsimony_method",
                "tree_summary")) {
            if (!Files.isDirectory(normalizedOutputRootDir.resolve(directoryName))) {
                missingDirectories.add(directoryName);
            }
        }
        if (!missingDirectories.isEmpty()) {
            throw new IOException("Missing required directories: " + missingDirectories);
        }

        List<String> missingTrees = new ArrayList<>();
        for (TreeMethod method : TreeMethod.DISPLAY_ORDER) {
            if (resolveTree(method, null, normalizedOutputRootDir.resolve("tree_summary"), normalizedOutputRootDir) == null) {
                missingTrees.add(method.shortLabel());
            }
        }
        if (!missingTrees.isEmpty()) {
            throw new IOException("Missing required tree files for: " + missingTrees);
        }

        return load(normalizedOutputRootDir.resolve("tree_summary"));
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
