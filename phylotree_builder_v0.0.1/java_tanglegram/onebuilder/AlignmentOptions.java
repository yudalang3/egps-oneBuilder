package onebuilder;

public final class AlignmentOptions {
    private final String strategy;
    private final int maxiterate;
    private final boolean reorder;

    public AlignmentOptions(String strategy, int maxiterate, boolean reorder) {
        this.strategy = strategy;
        this.maxiterate = maxiterate;
        this.reorder = reorder;
    }

    public static AlignmentOptions defaults() {
        return new AlignmentOptions("localpair", 1000, true);
    }

    public String strategy() {
        return strategy;
    }

    public int maxiterate() {
        return maxiterate;
    }

    public boolean reorder() {
        return reorder;
    }
}
