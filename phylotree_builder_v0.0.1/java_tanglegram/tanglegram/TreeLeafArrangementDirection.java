package tanglegram;

enum TreeLeafArrangementDirection {
    UP("UP"),
    DOWN("DOWN");

    private final String label;

    TreeLeafArrangementDirection(String label) {
        this.label = label;
    }

    boolean ascending() {
        return this == UP;
    }

    @Override
    public String toString() {
        return label;
    }
}
