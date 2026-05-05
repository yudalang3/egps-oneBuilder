package tanglegram;

enum TreeLeafArrangementRule {
    CLADE_SIZE("Clade size"),
    LEAF_NAME_STRING("Leaf name string"),
    BRANCH_LENGTH("Branch length");

    private final String label;

    TreeLeafArrangementRule(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
