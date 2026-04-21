package onebuilder;

enum WorkspaceSection {
    INPUT_ALIGN("Input / Align"),
    TREE_PARAMETERS("Tree Parameters"),
    TREE_BUILD("Tree Build"),
    TANGLEGRAM("Tanglegram");

    private final String label;

    WorkspaceSection(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }
}
