package onebuilder;

final class RerootConfig {
    private final RerootMethod method;
    private final LadderizeDirection ladderizeDirection;
    private final boolean sortByCladeSize;
    private final boolean sortByBranchLength;

    RerootConfig(RerootMethod method) {
        this(method, LadderizeDirection.UP, true, true);
    }

    RerootConfig(
            RerootMethod method,
            LadderizeDirection ladderizeDirection,
            boolean sortByCladeSize,
            boolean sortByBranchLength) {
        this.method = method == null ? RerootMethod.MAD : method;
        this.ladderizeDirection = ladderizeDirection == null ? LadderizeDirection.UP : ladderizeDirection;
        this.sortByCladeSize = sortByCladeSize;
        this.sortByBranchLength = sortByBranchLength;
    }

    static RerootConfig defaults() {
        return new RerootConfig(RerootMethod.MAD, LadderizeDirection.UP, true, true);
    }

    RerootMethod method() {
        return method;
    }

    LadderizeDirection ladderizeDirection() {
        return ladderizeDirection;
    }

    boolean sortByCladeSize() {
        return sortByCladeSize;
    }

    boolean sortByBranchLength() {
        return sortByBranchLength;
    }
}
