package tanglegram;

enum ImportSourceKind {
    MANUAL("manual"),
    RUNNING_RESULT("running result"),
    TSV("tsv config");

    private final String displayName;

    ImportSourceKind(String displayName) {
        this.displayName = displayName;
    }

    String displayName() {
        return displayName;
    }
}
