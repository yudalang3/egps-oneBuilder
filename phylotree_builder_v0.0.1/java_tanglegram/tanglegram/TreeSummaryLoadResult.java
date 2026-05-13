package tanglegram;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class TreeSummaryLoadResult {
    private final Path treeSummaryDir;
    private final Path outputRootDir;
    private final Map<TreeMethod, Path> resolvedTrees;
    private final Map<TreeMethod, Map<TreeMethod, Double>> treeDistances;
    private final Map<TreeMethod, Map<TreeMethod, Integer>> robinsonFouldsDistances;
    private final List<TreeMethod> missingMethods;
    private final List<String> warnings;

    public TreeSummaryLoadResult(
            Path treeSummaryDir,
            Path outputRootDir,
            Map<TreeMethod, Path> resolvedTrees,
            Map<TreeMethod, Map<TreeMethod, Double>> treeDistances,
            Map<TreeMethod, Map<TreeMethod, Integer>> robinsonFouldsDistances,
            List<TreeMethod> missingMethods,
            List<String> warnings) {
        this.treeSummaryDir = treeSummaryDir;
        this.outputRootDir = outputRootDir;
        EnumMap<TreeMethod, Path> safeResolvedTrees = new EnumMap<>(TreeMethod.class);
        safeResolvedTrees.putAll(resolvedTrees);
        this.resolvedTrees = Collections.unmodifiableMap(safeResolvedTrees);
        this.treeDistances = copyNestedMap(treeDistances);
        this.robinsonFouldsDistances = copyNestedMap(robinsonFouldsDistances);
        this.missingMethods = Collections.unmodifiableList(new ArrayList<>(missingMethods));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public Path treeSummaryDir() {
        return treeSummaryDir;
    }

    public Path outputRootDir() {
        return outputRootDir;
    }

    public Map<TreeMethod, Path> resolvedTrees() {
        return resolvedTrees;
    }

    public Map<TreeMethod, Map<TreeMethod, Double>> treeDistances() {
        return treeDistances;
    }

    public Map<TreeMethod, Map<TreeMethod, Integer>> robinsonFouldsDistances() {
        return robinsonFouldsDistances;
    }

    public List<TreeMethod> missingMethods() {
        return missingMethods;
    }

    public List<String> warnings() {
        return warnings;
    }

    public List<TreePairSpec> availablePairs() {
        List<TreePairSpec> pairs = new ArrayList<>();
        for (int leftIndex = 0; leftIndex < TreeMethod.DISPLAY_ORDER.size(); leftIndex++) {
            TreeMethod leftMethod = TreeMethod.DISPLAY_ORDER.get(leftIndex);
            Path leftTree = resolvedTrees.get(leftMethod);
            if (leftTree == null) {
                continue;
            }
            for (int rightIndex = leftIndex + 1; rightIndex < TreeMethod.DISPLAY_ORDER.size(); rightIndex++) {
                TreeMethod rightMethod = TreeMethod.DISPLAY_ORDER.get(rightIndex);
                Path rightTree = resolvedTrees.get(rightMethod);
                if (rightTree == null) {
                    continue;
                }
                pairs.add(new TreePairSpec(
                        leftMethod,
                        rightMethod,
                        leftTree,
                        rightTree,
                        treeDistance(leftMethod, rightMethod),
                        robinsonFouldsDistance(leftMethod, rightMethod)));
            }
        }
        return pairs;
    }

    private Double treeDistance(TreeMethod leftMethod, TreeMethod rightMethod) {
        Map<TreeMethod, Double> row = treeDistances.get(leftMethod);
        return row == null ? null : row.get(rightMethod);
    }

    private Integer robinsonFouldsDistance(TreeMethod leftMethod, TreeMethod rightMethod) {
        Map<TreeMethod, Integer> row = robinsonFouldsDistances.get(leftMethod);
        return row == null ? null : row.get(rightMethod);
    }

    private static <T> Map<TreeMethod, Map<TreeMethod, T>> copyNestedMap(Map<TreeMethod, Map<TreeMethod, T>> source) {
        EnumMap<TreeMethod, Map<TreeMethod, T>> outer = new EnumMap<>(TreeMethod.class);
        if (source == null) {
            return Collections.unmodifiableMap(outer);
        }
        for (Map.Entry<TreeMethod, Map<TreeMethod, T>> entry : source.entrySet()) {
            EnumMap<TreeMethod, T> inner = new EnumMap<>(TreeMethod.class);
            inner.putAll(entry.getValue());
            outer.put(entry.getKey(), Collections.unmodifiableMap(inner));
        }
        return Collections.unmodifiableMap(outer);
    }
}
