package onebuilder;

import evoltree.phylogeny.DefaultPhyNode;
import evoltree.struct.util.EvolNodeUtil;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class TreeLeafSanitizer {
    private static final double ZERO_LENGTH_TOLERANCE = 1e-12d;

    private TreeLeafSanitizer() {
    }

    static int removeBlankZeroLengthLeaves(DefaultPhyNode root) {
        if (root == null) {
            throw new IllegalArgumentException("Tree root is null");
        }
        return removeBlankZeroLengthLeaves(root, true);
    }

    static void validateNamedLeaves(DefaultPhyNode root) {
        Set<String> leafNames = new LinkedHashSet<>();
        for (DefaultPhyNode leaf : EvolNodeUtil.getLeaves(root)) {
            String name = normalizedName(leaf);
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Invalid tree: blank leaf name");
            }
            if (!leafNames.add(name)) {
                throw new IllegalArgumentException("Invalid tree: duplicate leaf name '" + name + "'");
            }
        }
    }

    private static int removeBlankZeroLengthLeaves(DefaultPhyNode node, boolean rootNode) {
        if (node.getChildCount() == 0) {
            if (!rootNode) {
                rejectBlankNonzeroLeaf(node);
            }
            return 0;
        }

        int removedCount = 0;
        List<DefaultPhyNode> keptChildren = new ArrayList<>(node.getChildCount());
        for (int index = 0; index < node.getChildCount(); index++) {
            DefaultPhyNode child = (DefaultPhyNode) node.getChildAt(index);
            removedCount += removeBlankZeroLengthLeaves(child, false);
            if (child.getChildCount() == 0 && normalizedName(child).isEmpty()) {
                if (isZeroLength(child)) {
                    removedCount++;
                    continue;
                }
                rejectBlankNonzeroLeaf(child);
            }
            keptChildren.add(child);
        }

        node.removeAllChild();
        for (DefaultPhyNode child : keptChildren) {
            node.addChild(child);
            if (child.getParentCount() == 0) {
                child.setParent(node);
            }
        }
        return removedCount;
    }

    private static void rejectBlankNonzeroLeaf(DefaultPhyNode leaf) {
        if (normalizedName(leaf).isEmpty() && !isZeroLength(leaf)) {
            throw new IllegalArgumentException(
                    "Invalid tree: blank leaf name with nonzero branch length " + leaf.getLength());
        }
    }

    private static String normalizedName(DefaultPhyNode node) {
        String name = node.getName();
        return name == null ? "" : name.trim();
    }

    private static boolean isZeroLength(DefaultPhyNode node) {
        return Math.abs(node.getLength()) <= ZERO_LENGTH_TOLERANCE;
    }
}
