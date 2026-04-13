package onebuilder;

public final class SimpleMethodConfig {
    private final boolean enabled;

    public SimpleMethodConfig(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }
}
