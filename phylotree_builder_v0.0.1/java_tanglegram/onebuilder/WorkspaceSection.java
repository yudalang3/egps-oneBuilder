package onebuilder;

enum WorkspaceSection {
    INPUT_ALIGN("Input / Align", 1),
    TREE_PARAMETERS("Tree Parameters", 2),
    REROOT_TREE("Reroot Tree", 3),
    TREE_BUILD("Tree Build", 4),
    TANGLEGRAM("Tanglegram", 5),
    VIS_LAUNCHING("Vis. Launching", 6),
    HOW_TO_CITE("How to cite", 7);

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
