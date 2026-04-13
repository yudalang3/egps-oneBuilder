package onebuilder;

public final class MaximumLikelihoodConfig {
    private final boolean enabled;
    private final int bootstrapReplicates;
    private final String modelStrategy;
    private final String modelSet;

    public MaximumLikelihoodConfig(boolean enabled, int bootstrapReplicates, String modelStrategy, String modelSet) {
        this.enabled = enabled;
        this.bootstrapReplicates = bootstrapReplicates;
        this.modelStrategy = modelStrategy;
        this.modelSet = modelSet;
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
}
