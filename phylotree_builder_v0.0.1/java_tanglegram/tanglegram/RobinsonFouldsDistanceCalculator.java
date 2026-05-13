package tanglegram;

import evoltree.struct.EvolNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class RobinsonFouldsDistanceCalculator {
    private RobinsonFouldsDistanceCalculator() {
    }

    static int count(EvolNode leftTree, EvolNode rightTree) {
        Set<String> leftClades = internalCladeSignatures(leftTree);
        Set<String> rightClades = internalCladeSignatures(rightTree);
        int difference = 0;
        for (String leftClade : leftClades) {
            if (!rightClades.contains(leftClade)) {
                difference++;
            }
        }
        for (String rightClade : rightClades) {
            if (!leftClades.contains(rightClade)) {
                difference++;
            }
        }
        return difference;
    }

    private static Set<String> internalCladeSignatures(EvolNode root) {
        Set<String> signatures = new HashSet<>();
        collectInternalClades(root, root, signatures);
        return signatures;
    }

    @SuppressWarnings("unchecked")
    private static void collectInternalClades(EvolNode node, EvolNode root, Set<String> signatures) {
        if (node == null) {
            return;
        }
        if (node != root && node.getChildCount() > 0) {
            List<String> leafNames = leafNames(node);
            if (leafNames.size() > 1) {
                signatures.add(String.join("\u001f", leafNames));
            }
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            collectInternalClades((EvolNode) node.getChildAt(index), root, signatures);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> leafNames(EvolNode node) {
        if (node.getChildCount() == 0) {
            String name = node.getName();
            if (name == null || name.trim().isEmpty()) {
                return List.of();
            }
            return List.of(name.trim());
        }
        List<String> names = new ArrayList<>();
        for (int index = 0; index < node.getChildCount(); index++) {
            names.addAll(leafNames((EvolNode) node.getChildAt(index)));
        }
        Collections.sort(names);
        return names;
    }
}
