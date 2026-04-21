package tanglegram;

import java.nio.file.Path;

public final class TreePairSpec {
    private final TreeMethod leftMethod;
    private final TreeMethod rightMethod;
    private final String leftLabel;
    private final String rightLabel;
    private final Path leftTree;
    private final Path rightTree;

    public TreePairSpec(TreeMethod leftMethod, TreeMethod rightMethod, Path leftTree, Path rightTree) {
        this(leftMethod, rightMethod, leftMethod.shortLabel(), rightMethod.shortLabel(), leftTree, rightTree);
    }

    public TreePairSpec(String leftLabel, String rightLabel, Path leftTree, Path rightTree) {
        this(null, null, leftLabel, rightLabel, leftTree, rightTree);
    }

    private TreePairSpec(
            TreeMethod leftMethod,
            TreeMethod rightMethod,
            String leftLabel,
            String rightLabel,
            Path leftTree,
            Path rightTree) {
        this.leftMethod = leftMethod;
        this.rightMethod = rightMethod;
        this.leftLabel = leftLabel;
        this.rightLabel = rightLabel;
        this.leftTree = leftTree;
        this.rightTree = rightTree;
    }

    public TreeMethod leftMethod() {
        return leftMethod;
    }

    public TreeMethod rightMethod() {
        return rightMethod;
    }

    public Path leftTree() {
        return leftTree;
    }

    public Path rightTree() {
        return rightTree;
    }

    public String leftLabel() {
        return leftLabel;
    }

    public String rightLabel() {
        return rightLabel;
    }

    public String tabName() {
        return leftLabel + "-" + rightLabel;
    }
}
