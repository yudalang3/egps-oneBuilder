package onebuilder;

public final class WorkflowTabsState {
    private final boolean inputEnabled;
    private final boolean treeBuildEnabled;
    private final boolean tanglegramEnabled;
    private final boolean runInProgress;

    private WorkflowTabsState(boolean inputEnabled, boolean treeBuildEnabled, boolean tanglegramEnabled, boolean runInProgress) {
        this.inputEnabled = inputEnabled;
        this.treeBuildEnabled = treeBuildEnabled;
        this.tanglegramEnabled = tanglegramEnabled;
        this.runInProgress = runInProgress;
    }

    public static WorkflowTabsState initial() {
        return new WorkflowTabsState(true, false, false, false);
    }

    public WorkflowTabsState markInputConfigured() {
        return new WorkflowTabsState(true, true, tanglegramEnabled, runInProgress);
    }

    public WorkflowTabsState markRunStarted() {
        return new WorkflowTabsState(true, true, tanglegramEnabled, true);
    }

    public WorkflowTabsState markTanglegramReady() {
        return new WorkflowTabsState(true, true, true, false);
    }

    public WorkflowTabsState markRunFinished() {
        return new WorkflowTabsState(true, treeBuildEnabled, tanglegramEnabled, false);
    }

    public boolean inputEnabled() {
        return inputEnabled;
    }

    public boolean treeBuildEnabled() {
        return treeBuildEnabled;
    }

    public boolean tanglegramEnabled() {
        return tanglegramEnabled;
    }

    public boolean runInProgress() {
        return runInProgress;
    }
}
