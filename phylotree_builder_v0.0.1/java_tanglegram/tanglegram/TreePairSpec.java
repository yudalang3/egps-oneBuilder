package tanglegram;

import java.nio.file.Path;

public final class TreePairSpec {
    private final TreeMethod leftMethod;
    private final TreeMethod rightMethod;
    private final Path leftTree;
    private final Path rightTree;

    public TreePairSpec(TreeMethod leftMethod, TreeMethod rightMethod, Path leftTree, Path rightTree) {
        this.leftMethod = leftMethod;
        this.rightMethod = rightMethod;
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

    public String tabName() {
        return leftMethod.shortLabel() + "-" + rightMethod.shortLabel();
    }
}
