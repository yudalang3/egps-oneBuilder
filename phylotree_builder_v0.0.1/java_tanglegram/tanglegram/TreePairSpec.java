package tanglegram;

import java.nio.file.Path;

final class TreePairSpec {
    private final TreeMethod leftMethod;
    private final TreeMethod rightMethod;
    private final Path leftTree;
    private final Path rightTree;

    TreePairSpec(TreeMethod leftMethod, TreeMethod rightMethod, Path leftTree, Path rightTree) {
        this.leftMethod = leftMethod;
        this.rightMethod = rightMethod;
        this.leftTree = leftTree;
        this.rightTree = rightTree;
    }

    TreeMethod leftMethod() {
        return leftMethod;
    }

    TreeMethod rightMethod() {
        return rightMethod;
    }

    Path leftTree() {
        return leftTree;
    }

    Path rightTree() {
        return rightTree;
    }

    String tabName() {
        return leftMethod.shortLabel() + "-" + rightMethod.shortLabel();
    }
}
