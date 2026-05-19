package tree.alignment.indices;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import tanglegram.TreeDifferenceMetricsCalculator;

public final class CLI {
    private CLI() {
    }

    public static void main(String[] args) {
        if (args.length != 1 || "--help".equals(args[0]) || "-h".equals(args[0])) {
            printUsage();
            System.exit(args.length == 1 ? 0 : 2);
        }

        Path inputFile = Path.of(args[0]).toAbsolutePath().normalize();
        try {
            List<String> lines = Files.readAllLines(inputFile, StandardCharsets.UTF_8);
            TreeDifferenceMetricsCalculator.Metrics metrics =
                    TreeDifferenceMetricsCalculator.calculateNewickLines(lines);
            System.out.println("tree_count\t" + metrics.treeCount());
            System.out.println("reference_clade_count\t" + metrics.referenceCladeCount());
            System.out.println("recovered_reference_clade_count\t" + metrics.recoveredReferenceCladeCount());
            System.out.println("TDI\t" + format(metrics.topologyDifferenceIndex()));
            System.out.println("BDI\t" + format(metrics.branchLengthDifferenceIndex()));
        } catch (Exception exception) {
            System.err.println("Failed to calculate TDI/BDI: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -cp \"java_tanglegram:lib/*\" tree.alignment.indices.CLI inputFile.txt");
        System.out.println();
        System.out.println("inputFile.txt format:");
        System.out.println("  One Newick tree per non-empty line. Lines starting with # are ignored.");
    }

    private static String format(double value) {
        if (Double.isNaN(value)) {
            return "NA";
        }
        if (Double.isInfinite(value)) {
            return value > 0.0d ? "Inf" : "-Inf";
        }
        return String.format(java.util.Locale.ROOT, "%.10g", value);
    }
}
