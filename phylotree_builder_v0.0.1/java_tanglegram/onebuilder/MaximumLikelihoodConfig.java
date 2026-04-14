package onebuilder;

import java.util.List;

public final class MaximumLikelihoodConfig {
    private final boolean enabled;
    private final int bootstrapReplicates;
    private final String modelStrategy;
    private final String modelSet;
    private final String threads;
    private final Integer threadsMax;
    private final Integer seed;
    private final boolean safe;
    private final boolean keepIdent;
    private final boolean quiet;
    private final boolean verbose;
    private final boolean redo;
    private final String memoryLimit;
    private final String outgroup;
    private final String sequenceType;
    private final Integer alrt;
    private final boolean abayes;
    private final List<String> extraArgs;

    public MaximumLikelihoodConfig(boolean enabled, int bootstrapReplicates, String modelStrategy, String modelSet) {
        this(
                enabled,
                bootstrapReplicates,
                modelStrategy,
                modelSet,
                null,
                null,
                null,
                false,
                false,
                true,
                false,
                true,
                null,
                null,
                null,
                null,
                false,
                List.of());
    }

    public MaximumLikelihoodConfig(
            boolean enabled,
            int bootstrapReplicates,
            String modelStrategy,
            String modelSet,
            String threads,
            Integer threadsMax,
            Integer seed,
            boolean safe,
            boolean keepIdent,
            boolean quiet,
            boolean verbose,
            boolean redo,
            String memoryLimit,
            String outgroup,
            String sequenceType,
            Integer alrt,
            boolean abayes,
            List<String> extraArgs) {
        this.enabled = enabled;
        this.bootstrapReplicates = bootstrapReplicates;
        this.modelStrategy = modelStrategy;
        this.modelSet = modelSet;
        this.threads = threads;
        this.threadsMax = threadsMax;
        this.seed = seed;
        this.safe = safe;
        this.keepIdent = keepIdent;
        this.quiet = quiet;
        this.verbose = verbose;
        this.redo = redo;
        this.memoryLimit = memoryLimit;
        this.outgroup = outgroup;
        this.sequenceType = sequenceType;
        this.alrt = alrt;
        this.abayes = abayes;
        this.extraArgs = List.copyOf(extraArgs == null ? List.of() : extraArgs);
    }

    public boolean enabled() {
        return enabled;
    }

    public int bootstrapReplicates() {
        return bootstrapReplicates;
    }

    public String modelStrategy() {
        return modelStrategy;
    }

    public String modelSet() {
        return modelSet;
    }

    public String threads() {
        return threads;
    }

    public Integer threadsMax() {
        return threadsMax;
    }

    public Integer seed() {
        return seed;
    }

    public boolean safe() {
        return safe;
    }

    public boolean keepIdent() {
        return keepIdent;
    }

    public boolean quiet() {
        return quiet;
    }

    public boolean verbose() {
        return verbose;
    }

    public boolean redo() {
        return redo;
    }

    public String memoryLimit() {
        return memoryLimit;
    }

    public String outgroup() {
        return outgroup;
    }

    public String sequenceType() {
        return sequenceType;
    }

    public Integer alrt() {
        return alrt;
    }

    public boolean abayes() {
        return abayes;
    }

    public List<String> extraArgs() {
        return extraArgs;
    }
}
