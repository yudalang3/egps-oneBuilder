package onebuilder;

public final class MethodProgressEvent {
    public enum Lifecycle {
        RUNNING,
        COMPLETED,
        SKIPPED,
        FAILED
    }

    private final TreeMethodKey methodKey;
    private final Lifecycle lifecycle;

    private MethodProgressEvent(TreeMethodKey methodKey, Lifecycle lifecycle) {
        this.methodKey = methodKey;
        this.lifecycle = lifecycle;
    }

    public static MethodProgressEvent running(TreeMethodKey methodKey) {
        return new MethodProgressEvent(methodKey, Lifecycle.RUNNING);
    }

    public static MethodProgressEvent completed(TreeMethodKey methodKey) {
        return new MethodProgressEvent(methodKey, Lifecycle.COMPLETED);
    }

    public static MethodProgressEvent failed(TreeMethodKey methodKey) {
        return new MethodProgressEvent(methodKey, Lifecycle.FAILED);
    }

    public static MethodProgressEvent skipped(TreeMethodKey methodKey) {
        return new MethodProgressEvent(methodKey, Lifecycle.SKIPPED);
    }

    public TreeMethodKey methodKey() {
        return methodKey;
    }

    public Lifecycle lifecycle() {
        return lifecycle;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MethodProgressEvent)) {
            return false;
        }
        MethodProgressEvent event = (MethodProgressEvent) other;
        return methodKey == event.methodKey && lifecycle == event.lifecycle;
    }

    @Override
    public int hashCode() {
        return methodKey.hashCode() * 31 + lifecycle.hashCode();
    }

    @Override
    public String toString() {
        return methodKey + ":" + lifecycle;
    }
}
