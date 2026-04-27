package onebuilder;

import module.remnant.treeoperator.NodeEGPSv1;
import module.remnant.treeoperator.io.TreeCoder;
import module.remnant.treeoperator.reconAlgo.NJ;
import module.remnant.treeoperator.reconAlgo.SwiftNJ;
import module.remnant.treeoperator.reconAlgo.TreeReconMethod;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class ProteinStructureTreeCommand {
    private ProteinStructureTreeCommand() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        try {
            CommandOptions options = CommandOptions.parse(args);
            DistanceMatrix matrix = readDistanceMatrix(options.inputFile);
            TreeReconMethod method = createMethod(options.method);
            NodeEGPSv1 root = method.tree(matrix.distances, matrix.names);
            String newick = TreeCoder.code(root).trim();
            if (!newick.endsWith(";")) {
                newick += ";";
            }
            if (options.outputFile.getParent() != null) {
                Files.createDirectories(options.outputFile.getParent());
            }
            Files.writeString(options.outputFile, newick + System.lineSeparator(), StandardCharsets.UTF_8);
            return 0;
        } catch (Exception exception) {
            System.err.println("Failed to build protein structure tree: " + exception.getMessage());
            return 1;
        }
    }

    private static TreeReconMethod createMethod(String method) {
        if ("SwiftNJ".equals(method)) {
            return new SwiftNJ();
        }
        return new NJ();
    }

    private static DistanceMatrix readDistanceMatrix(Path inputFile) throws Exception {
        if (!Files.isRegularFile(inputFile)) {
            throw new IllegalArgumentException("Distance matrix does not exist: " + inputFile);
        }
        List<String> rawLines = Files.readAllLines(inputFile, StandardCharsets.UTF_8);
        List<String[]> rows = new ArrayList<>();
        for (String rawLine : rawLines) {
            if (rawLine == null || rawLine.isBlank()) {
                continue;
            }
            rows.add(rawLine.split("\t", -1));
        }
        if (rows.size() < 3) {
            throw new IllegalArgumentException("Distance matrix must contain at least two sequences.");
        }

        String[] header = rows.get(0);
        if (header.length < 3) {
            throw new IllegalArgumentException("Distance matrix header must contain sequence IDs.");
        }
        int size = header.length - 1;
        if (rows.size() != size + 1) {
            throw new IllegalArgumentException("Distance matrix row count does not match header size.");
        }

        String[] names = new String[size];
        for (int index = 0; index < size; index++) {
            names[index] = header[index + 1].trim();
            if (names[index].isEmpty()) {
                throw new IllegalArgumentException("Distance matrix header contains an empty sequence ID.");
            }
        }

        double[][] squareDistances = new double[size][size];
        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            String[] row = rows.get(rowIndex + 1);
            if (row.length != size + 1) {
                throw new IllegalArgumentException("Distance matrix row has wrong column count: " + (rowIndex + 2));
            }
            String rowName = row[0].trim();
            if (!names[rowIndex].equals(rowName)) {
                throw new IllegalArgumentException("Distance matrix row ID does not match header: " + rowName);
            }
            for (int columnIndex = 0; columnIndex < size; columnIndex++) {
                squareDistances[rowIndex][columnIndex] = Double.parseDouble(row[columnIndex + 1].trim());
            }
        }

        double[][] lowerTriangleDistances = new double[size - 1][];
        for (int rowIndex = 1; rowIndex < size; rowIndex++) {
            lowerTriangleDistances[rowIndex - 1] = new double[rowIndex];
            for (int columnIndex = 0; columnIndex < rowIndex; columnIndex++) {
                lowerTriangleDistances[rowIndex - 1][columnIndex] = squareDistances[rowIndex][columnIndex];
            }
        }

        return new DistanceMatrix(names, lowerTriangleDistances);
    }

    private static final class DistanceMatrix {
        private final String[] names;
        private final double[][] distances;

        private DistanceMatrix(String[] names, double[][] distances) {
            this.names = names;
            this.distances = distances;
        }
    }

    private static final class CommandOptions {
        private final String method;
        private final Path inputFile;
        private final Path outputFile;

        private CommandOptions(String method, Path inputFile, Path outputFile) {
            this.method = method;
            this.inputFile = inputFile;
            this.outputFile = outputFile;
        }

        private static CommandOptions parse(String[] args) {
            String method = "NJ";
            Path inputFile = null;
            Path outputFile = null;
            for (int index = 0; index < args.length; index++) {
                String arg = args[index];
                if ("--method".equals(arg)) {
                    method = normalizeMethod(requireValue(args, ++index, arg));
                } else if ("--input".equals(arg)) {
                    inputFile = Paths.get(requireValue(args, ++index, arg)).toAbsolutePath().normalize();
                } else if ("--output".equals(arg)) {
                    outputFile = Paths.get(requireValue(args, ++index, arg)).toAbsolutePath().normalize();
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (inputFile == null || outputFile == null) {
                throw new IllegalArgumentException(
                        "Usage: onebuilder.ProteinStructureTreeCommand --method NJ|SwiftNJ --input distance_matrix.tsv --output structure_tree.nwk");
            }
            return new CommandOptions(method, inputFile, outputFile);
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }

        private static String normalizeMethod(String rawValue) {
            String value = rawValue == null ? "" : rawValue.trim();
            if ("SwiftNJ".equalsIgnoreCase(value) || "Swift NJ".equalsIgnoreCase(value)) {
                return "SwiftNJ";
            }
            if (value.isEmpty() || "NJ".equalsIgnoreCase(value)) {
                return "NJ";
            }
            throw new IllegalArgumentException("Unsupported structure tree builder method: " + rawValue);
        }
    }
}
