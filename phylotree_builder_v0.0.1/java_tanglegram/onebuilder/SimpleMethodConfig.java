package onebuilder;

import java.util.List;

public final class SimpleMethodConfig {
    private final boolean enabled;
    private final List<String> protdistMenuOverrides;
    private final List<String> dnadistMenuOverrides;
    private final List<String> neighborMenuOverrides;
    private final List<String> protparsMenuOverrides;
    private final List<String> dnaparsMenuOverrides;

    public SimpleMethodConfig(boolean enabled) {
        this(enabled, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public SimpleMethodConfig(
            boolean enabled,
            List<String> protdistMenuOverrides,
            List<String> dnadistMenuOverrides,
            List<String> neighborMenuOverrides,
            List<String> protparsMenuOverrides,
            List<String> dnaparsMenuOverrides) {
        this.enabled = enabled;
        this.protdistMenuOverrides = immutableCopy(protdistMenuOverrides);
        this.dnadistMenuOverrides = immutableCopy(dnadistMenuOverrides);
        this.neighborMenuOverrides = immutableCopy(neighborMenuOverrides);
        this.protparsMenuOverrides = immutableCopy(protparsMenuOverrides);
        this.dnaparsMenuOverrides = immutableCopy(dnaparsMenuOverrides);
    }

    public boolean enabled() {
        return enabled;
    }

    public List<String> protdistMenuOverrides() {
        return protdistMenuOverrides;
    }

    public List<String> dnadistMenuOverrides() {
        return dnadistMenuOverrides;
    }

    public List<String> neighborMenuOverrides() {
        return neighborMenuOverrides;
    }

    public List<String> protparsMenuOverrides() {
        return protparsMenuOverrides;
    }

    public List<String> dnaparsMenuOverrides() {
        return dnaparsMenuOverrides;
    }

    private static List<String> immutableCopy(List<String> values) {
        return List.copyOf(values == null ? List.of() : values);
    }
}
