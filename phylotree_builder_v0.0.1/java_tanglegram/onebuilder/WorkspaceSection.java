package onebuilder;

enum WorkspaceSection {
    INPUT_ALIGN("Input / Align", 1),
    TRIM_ALIGNMENT("Trim alignment", 2),
    TREE_PARAMETERS("Tree Parameters", 3),
    REROOT_TREE("Reroot Tree", 4),
    TREE_BUILD("Tree Build", 5),
    TANGLEGRAM("Tanglegram", 6),
    VIS_LAUNCHING("Vis. Launching", 7),
    HOW_TO_CITE("How to cite", 8);

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
