package tanglegram;

import evoltree.struct.EvolNode;
import evoltree.struct.TreeDecoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TreeDifferenceMetricsCalculator {
    private TreeDifferenceMetricsCalculator() {
    }

    public static Metrics calculateNewickLines(List<String> newickLines) throws Exception {
        if (newickLines == null) {
            throw new IllegalArgumentException("Input tree lines must not be null.");
        }
        TreeDecoder decoder = new TreeDecoder();
        List<ImportedTreeSpec> trees = new ArrayList<>();
        int treeIndex = 1;
        for (String line : newickLines) {
            String newick = line == null ? "" : line.trim();
            if (newick.isEmpty() || newick.startsWith("#")) {
                continue;
            }
            EvolNode root = decoder.decode(newick);
            trees.add(new ImportedTreeSpec(Path.of("tree-" + treeIndex + ".nwk"), "Tree " + treeIndex, root));
            treeIndex++;
        }
        if (trees.size() < 2) {
            throw new IllegalArgumentException("At least two non-empty Newick tree lines are required.");
        }
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics =
                ThreeDTreeAlignmentView.calculateTreeDifferenceMetricsForTest(trees);
        return new Metrics(
                metrics.topologyDifferenceIndex(),
                metrics.branchLengthDifferenceIndex(),
                metrics.referenceCladeCount(),
                metrics.recoveredReferenceCladeCount(),
                trees.size());
    }

    public record Metrics(
            double topologyDifferenceIndex,
            double branchLengthDifferenceIndex,
            int referenceCladeCount,
            int recoveredReferenceCladeCount,
            int treeCount) {
    }
}
