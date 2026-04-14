package onebuilder;

import java.util.List;

public final class AlignmentOptions {
    private final String strategy;
    private final int maxiterate;
    private final boolean reorder;
    private final List<String> extraArgs;

    public AlignmentOptions(String strategy, int maxiterate, boolean reorder) {
        this(strategy, maxiterate, reorder, List.of());
    }

    public AlignmentOptions(String strategy, int maxiterate, boolean reorder, List<String> extraArgs) {
        this.strategy = strategy;
        this.maxiterate = maxiterate;
        this.reorder = reorder;
        this.extraArgs = List.copyOf(extraArgs == null ? List.of() : extraArgs);
    }

    public static AlignmentOptions defaults() {
        return new AlignmentOptions("localpair", 1000, true, List.of());
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

    public List<String> extraArgs() {
        return extraArgs;
    }
}
