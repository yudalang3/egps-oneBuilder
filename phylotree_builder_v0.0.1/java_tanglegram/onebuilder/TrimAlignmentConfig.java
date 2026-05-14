package onebuilder;

import java.util.ArrayList;
import java.util.List;

public final class TrimAlignmentConfig {
    private final boolean enabled;
    private final TrimAlignmentPreset preset;
    private final List<String> customArgs;

    public TrimAlignmentConfig(boolean enabled, TrimAlignmentPreset preset, List<String> customArgs) {
        this.enabled = enabled;
        this.preset = preset == null ? TrimAlignmentPreset.GAP_THRESHOLD_CONSERVE : preset;
        this.customArgs = List.copyOf(customArgs == null ? List.of() : customArgs);
    }

    public static TrimAlignmentConfig defaults() {
        return new TrimAlignmentConfig(false, TrimAlignmentPreset.GAP_THRESHOLD_CONSERVE, List.of());
    }

    public boolean enabled() {
        return enabled;
    }

    public TrimAlignmentPreset preset() {
        return preset;
    }

    public List<String> customArgs() {
        return customArgs;
    }

    public List<String> effectiveArgs() {
        if (!enabled) {
            return List.of();
        }
        if (preset == TrimAlignmentPreset.CUSTOMIZED) {
            return customArgs;
        }
        return preset.arguments();
    }

    public String displayText() {
        if (!enabled) {
            return "Disabled";
        }
        if (preset == TrimAlignmentPreset.CUSTOMIZED) {
            return customArgs.isEmpty() ? "customized" : "customized: " + String.join(" ", customArgs);
        }
        return preset.commandPreview();
    }

    public void validate() {
        if (!enabled) {
            return;
        }
        List<String> args = effectiveArgs();
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Trim alignment is enabled, but no trimAl arguments are configured.");
        }
        for (String arg : args) {
            if ("-in".equals(arg) || "-out".equals(arg)) {
                throw new IllegalArgumentException("Trim alignment custom arguments must not include -in or -out; oneBuilder supplies input and output paths.");
            }
        }
    }

    public static List<String> splitCustomArgs(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }
        String[] pieces = text.trim().split("\\s+");
        List<String> values = new ArrayList<>();
        for (String piece : pieces) {
            if (!piece.isBlank()) {
                values.add(piece);
            }
        }
        return values;
    }
}
