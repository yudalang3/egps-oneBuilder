package onebuilder;

import java.util.List;

public final class AlignmentOptions {
    private final String strategy;
    private final int maxiterate;
    private final Integer threads;
    private final boolean reorder;
    private final List<String> extraArgs;

    public AlignmentOptions(String strategy, int maxiterate, boolean reorder) {
        this(strategy, maxiterate, null, reorder, List.of());
    }

    public AlignmentOptions(String strategy, int maxiterate, boolean reorder, List<String> extraArgs) {
        this(strategy, maxiterate, null, reorder, extraArgs);
    }

    public AlignmentOptions(String strategy, int maxiterate, Integer threads, boolean reorder, List<String> extraArgs) {
        this.strategy = strategy;
        this.maxiterate = maxiterate;
        this.threads = threads;
        this.reorder = reorder;
        this.extraArgs = List.copyOf(extraArgs == null ? List.of() : extraArgs);
    }

    public static AlignmentOptions defaults() {
        return new AlignmentOptions("localpair", 1000, null, true, List.of());
    }

    public String strategy() {
        return strategy;
    }

    public int maxiterate() {
        return maxiterate;
    }

    public Integer threads() {
        return threads;
    }

    public boolean reorder() {
        return reorder;
    }

    public List<String> extraArgs() {
        return extraArgs;
    }
}
