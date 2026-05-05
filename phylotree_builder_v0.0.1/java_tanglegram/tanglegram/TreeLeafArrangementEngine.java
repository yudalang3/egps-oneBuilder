package tanglegram;

import evoltree.struct.EvolNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class TreeLeafArrangementEngine {
    private TreeLeafArrangementEngine() {
    }

    static void arrange(EvolNode root, TreeLeafArrangementOptions options) {
        if (root == null || options == null || !options.enabled()) {
            return;
        }
        Map<EvolNode, NodeMetrics> metrics = new IdentityHashMap<>();
        collectMetrics(root, metrics);
        arrangeNode(root, options, metrics);
    }

    private static NodeMetrics collectMetrics(EvolNode node, Map<EvolNode, NodeMetrics> metrics) {
        int childCount = node.getChildCount();
        if (childCount == 0) {
            String name = normalizeName(node.getName());
            NodeMetrics leafMetrics = new NodeMetrics(1, List.of(name));
            metrics.put(node, leafMetrics);
            return leafMetrics;
        }

        int leafCount = 0;
        List<String> sortedLeafNames = new ArrayList<>();
        for (int index = 0; index < childCount; index++) {
            NodeMetrics childMetrics = collectMetrics(node.getChildAt(index), metrics);
            leafCount += childMetrics.leafCount();
            sortedLeafNames.addAll(childMetrics.sortedLeafNames());
        }
        Collections.sort(sortedLeafNames);
        NodeMetrics nodeMetrics = new NodeMetrics(leafCount, List.copyOf(sortedLeafNames));
        metrics.put(node, nodeMetrics);
        return nodeMetrics;
    }

    private static void arrangeNode(
            EvolNode node,
            TreeLeafArrangementOptions options,
            Map<EvolNode, NodeMetrics> metrics) {
        int childCount = node.getChildCount();
        if (childCount <= 1) {
            return;
        }

        List<EvolNode> children = new ArrayList<>(childCount);
        for (int index = 0; index < childCount; index++) {
            children.add(node.getChildAt(index));
        }
        children.sort((left, right) -> compareChildren(left, right, options, metrics));
        node.removeAllChild();
        for (int index = 0; index < childCount; index++) {
            EvolNode child = children.get(index);
            node.addChild(child);
            if (child.getParent() != node) {
                child.setParent(node);
            }
        }
        for (EvolNode child : children) {
            arrangeNode(child, options, metrics);
        }
    }

    private static int compareChildren(
            EvolNode left,
            EvolNode right,
            TreeLeafArrangementOptions options,
            Map<EvolNode, NodeMetrics> metrics) {
        for (TreeLeafArrangementRule rule : options.ruleOrder()) {
            int comparison = compareByRule(left, right, rule, options.direction(), metrics);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static int compareByRule(
            EvolNode left,
            EvolNode right,
            TreeLeafArrangementRule rule,
            TreeLeafArrangementDirection direction,
            Map<EvolNode, NodeMetrics> metrics) {
        int comparison;
        if (rule == TreeLeafArrangementRule.CLADE_SIZE) {
            comparison = Integer.compare(metrics.get(left).leafCount(), metrics.get(right).leafCount());
        } else if (rule == TreeLeafArrangementRule.LEAF_NAME_STRING) {
            comparison = compareLeafNameLists(metrics.get(left).sortedLeafNames(), metrics.get(right).sortedLeafNames());
        } else if (rule == TreeLeafArrangementRule.BRANCH_LENGTH) {
            comparison = Double.compare(normalizeLength(left.getLength()), normalizeLength(right.getLength()));
        } else {
            comparison = 0;
        }
        return direction.ascending() ? comparison : -comparison;
    }

    private static int compareLeafNameLists(List<String> leftLeafNames, List<String> rightLeafNames) {
        int size = Math.min(leftLeafNames.size(), rightLeafNames.size());
        for (int index = 0; index < size; index++) {
            int comparison = leftLeafNames.get(index).compareTo(rightLeafNames.get(index));
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(leftLeafNames.size(), rightLeafNames.size());
    }

    static int compareLeafNameListsForTest(List<String> leftLeafNames, List<String> rightLeafNames) {
        return compareLeafNameLists(leftLeafNames, rightLeafNames);
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private static double normalizeLength(double length) {
        return Double.isFinite(length) ? length : 0.0d;
    }

    private record NodeMetrics(int leafCount, List<String> sortedLeafNames) {
    }
}
