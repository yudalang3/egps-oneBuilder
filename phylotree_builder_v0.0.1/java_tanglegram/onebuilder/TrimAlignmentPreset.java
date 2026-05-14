package onebuilder;

import java.util.Arrays;
import java.util.List;

public enum TrimAlignmentPreset {
    GAP_THRESHOLD_CONSERVE(
            "Gap threshold + conserve 60%",
            "-gt 0.9 -cons 60",
            "-gt",
            "0.9",
            "-cons",
            "60"),
    GAP_THRESHOLD_WINDOW(
            "Gap threshold + conserve 60% + window 3",
            "-gt 0.9 -cons 60 -w 3",
            "-gt",
            "0.9",
            "-cons",
            "60",
            "-w",
            "3"),
    GAPPYOUT(
            "Gappyout automatic",
            "-gappyout",
            "-gappyout"),
    STRICTPLUS(
            "Strictplus automatic",
            "-strictplus",
            "-strictplus"),
    AUTOMATED1(
            "Automated1 heuristic",
            "-automated1",
            "-automated1"),
    RES_SEQ_OVERLAP(
            "Residue/sequence overlap",
            "-resoverlap 0.8 -seqoverlap 75",
            "-resoverlap",
            "0.8",
            "-seqoverlap",
            "75"),
    CUSTOMIZED(
            "customized",
            "",
            new String[0]);

    private final String label;
    private final String commandPreview;
    private final List<String> arguments;

    TrimAlignmentPreset(String label, String commandPreview, String... arguments) {
        this.label = label;
        this.commandPreview = commandPreview;
        this.arguments = List.copyOf(Arrays.asList(arguments));
    }

    public String label() {
        return label;
    }

    public String commandPreview() {
        return commandPreview;
    }

    public List<String> arguments() {
        return arguments;
    }

    public static TrimAlignmentPreset fromStorageValue(String value) {
        if (value == null || value.isBlank()) {
            return GAP_THRESHOLD_CONSERVE;
        }
        for (TrimAlignmentPreset preset : values()) {
            if (preset.name().equalsIgnoreCase(value.trim())) {
                return preset;
            }
        }
        return GAP_THRESHOLD_CONSERVE;
    }

    @Override
    public String toString() {
        if (commandPreview.isBlank()) {
            return label;
        }
        return label + " (" + commandPreview + ")";
    }
}
