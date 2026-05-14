package onebuilder;

public final class WorkflowTabsState {
    private final boolean inputEnabled;
    private final boolean trimAlignmentEnabled;
    private final boolean treeParametersEnabled;
    private final boolean rerootTreeEnabled;
    private final boolean treeBuildEnabled;
    private final boolean tanglegramEnabled;
    private final boolean visLaunchingEnabled;
    private final boolean runInProgress;

    private WorkflowTabsState(
            boolean inputEnabled,
            boolean trimAlignmentEnabled,
            boolean treeParametersEnabled,
            boolean rerootTreeEnabled,
            boolean treeBuildEnabled,
            boolean tanglegramEnabled,
            boolean visLaunchingEnabled,
            boolean runInProgress) {
        this.inputEnabled = inputEnabled;
        this.trimAlignmentEnabled = trimAlignmentEnabled;
        this.treeParametersEnabled = treeParametersEnabled;
        this.rerootTreeEnabled = rerootTreeEnabled;
        this.treeBuildEnabled = treeBuildEnabled;
        this.tanglegramEnabled = tanglegramEnabled;
        this.visLaunchingEnabled = visLaunchingEnabled;
        this.runInProgress = runInProgress;
    }

    public static WorkflowTabsState initial() {
        return new WorkflowTabsState(true, false, false, false, false, false, false, false);
    }

    public WorkflowTabsState markInputConfigured() {
        return new WorkflowTabsState(true, true, true, true, true, tanglegramEnabled, visLaunchingEnabled, runInProgress);
    }

    public WorkflowTabsState markRunStarted() {
        return new WorkflowTabsState(true, trimAlignmentEnabled, treeParametersEnabled, rerootTreeEnabled, treeBuildEnabled, false, false, true);
    }

    public WorkflowTabsState markTanglegramReady() {
        return new WorkflowTabsState(true, true, true, true, true, true, true, false);
    }

    public WorkflowTabsState markRunFinished() {
        return new WorkflowTabsState(true, trimAlignmentEnabled, treeParametersEnabled, rerootTreeEnabled, treeBuildEnabled, tanglegramEnabled, visLaunchingEnabled, false);
    }

    public boolean inputEnabled() {
        return inputEnabled;
    }

    public boolean treeBuildEnabled() {
        return treeBuildEnabled;
    }

    public boolean trimAlignmentEnabled() {
        return trimAlignmentEnabled;
    }

    public boolean treeParametersEnabled() {
        return treeParametersEnabled;
    }

    public boolean rerootTreeEnabled() {
        return rerootTreeEnabled;
    }

    public boolean tanglegramEnabled() {
        return tanglegramEnabled;
    }

    public boolean visLaunchingEnabled() {
        return visLaunchingEnabled;
    }

    public boolean runInProgress() {
        return runInProgress;
    }
}
