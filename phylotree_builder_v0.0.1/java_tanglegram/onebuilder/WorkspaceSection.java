package onebuilder;

enum WorkspaceSection {
    INPUT_ALIGN("Input / Align", 1),
    TREE_PARAMETERS("Tree Parameters", 2),
    TREE_BUILD("Tree Build", 3),
    TANGLEGRAM("Tanglegram", 4);

    private final String label;
    private final int stepNumber;

    WorkspaceSection(String label, int stepNumber) {
        this.label = label;
        this.stepNumber = stepNumber;
    }

    String label() {
        return label;
    }

    String navigationLabel() {
        return stepNumber + ". " + label;
    }
}
