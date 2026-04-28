package onebuilder;

import java.util.List;

public final class ProteinStructureConfig {
    public static final int DEFAULT_THREADS = 0;
    public static final double DEFAULT_SENSITIVITY = 9.5d;
    public static final double DEFAULT_EVALUE = 10.0d;
    public static final int DEFAULT_MAX_SEQS = 1000;
    public static final double DEFAULT_COVERAGE_THRESHOLD = 0.0d;
    public static final int DEFAULT_COVERAGE_MODE = 0;
    public static final int DEFAULT_ALIGNMENT_TYPE = 2;
    public static final double DEFAULT_TMSCORE_THRESHOLD = 0.0d;
    public static final int DEFAULT_VERBOSITY = 3;

    private final boolean enabled;
    private final boolean useStructureManifest;
    private final String structureManifestFile;
    private final String prostt5ModelPath;
    private final String treeBuilderMethod;
    private final int threads;
    private final double sensitivity;
    private final double evalue;
    private final int maxSeqs;
    private final double coverageThreshold;
    private final int coverageMode;
    private final int alignmentType;
    private final double tmscoreThreshold;
    private final boolean exhaustiveSearch;
    private final boolean exactTmscore;
    private final boolean gpu;
    private final int verbosity;
    private final List<String> extraArgs;

    public ProteinStructureConfig(boolean enabled, boolean useStructureManifest, String structureManifestFile) {
        this(enabled, useStructureManifest, structureManifestFile, "NJ");
    }

    public ProteinStructureConfig(
            boolean enabled,
            boolean useStructureManifest,
            String structureManifestFile,
            String treeBuilderMethod) {
        this(enabled, useStructureManifest, structureManifestFile, null, treeBuilderMethod);
    }

    public ProteinStructureConfig(
            boolean enabled,
            boolean useStructureManifest,
            String structureManifestFile,
            String prostt5ModelPath,
            String treeBuilderMethod) {
        this(
                enabled,
                useStructureManifest,
                structureManifestFile,
                prostt5ModelPath,
                treeBuilderMethod,
                DEFAULT_THREADS,
                DEFAULT_SENSITIVITY,
                DEFAULT_EVALUE,
                DEFAULT_MAX_SEQS,
                DEFAULT_COVERAGE_THRESHOLD,
                DEFAULT_COVERAGE_MODE,
                DEFAULT_ALIGNMENT_TYPE,
                DEFAULT_TMSCORE_THRESHOLD,
                false,
                false,
                false,
                DEFAULT_VERBOSITY,
                List.of());
    }

    public ProteinStructureConfig(
            boolean enabled,
            boolean useStructureManifest,
            String structureManifestFile,
            String treeBuilderMethod,
            int threads,
            double sensitivity,
            double evalue,
            int maxSeqs,
            double coverageThreshold,
            int coverageMode,
            int alignmentType,
            double tmscoreThreshold,
            boolean exhaustiveSearch,
            boolean exactTmscore,
            boolean gpu,
            int verbosity,
            List<String> extraArgs) {
        this(
                enabled,
                useStructureManifest,
                structureManifestFile,
                null,
                treeBuilderMethod,
                threads,
                sensitivity,
                evalue,
                maxSeqs,
                coverageThreshold,
                coverageMode,
                alignmentType,
                tmscoreThreshold,
                exhaustiveSearch,
                exactTmscore,
                gpu,
                verbosity,
                extraArgs);
    }

    public ProteinStructureConfig(
            boolean enabled,
            boolean useStructureManifest,
            String structureManifestFile,
            String prostt5ModelPath,
            String treeBuilderMethod,
            int threads,
            double sensitivity,
            double evalue,
            int maxSeqs,
            double coverageThreshold,
            int coverageMode,
            int alignmentType,
            double tmscoreThreshold,
            boolean exhaustiveSearch,
            boolean exactTmscore,
            boolean gpu,
            int verbosity,
            List<String> extraArgs) {
        this.enabled = enabled;
        this.useStructureManifest = useStructureManifest;
        this.structureManifestFile = normalizeText(structureManifestFile);
        this.prostt5ModelPath = normalizeText(prostt5ModelPath);
        this.treeBuilderMethod = normalizeTreeBuilderMethod(treeBuilderMethod);
        this.threads = Math.max(0, threads);
        this.sensitivity = Math.max(1.0d, sensitivity);
        this.evalue = Math.max(0.0d, evalue);
        this.maxSeqs = Math.max(1, maxSeqs);
        this.coverageThreshold = clamp(coverageThreshold, 0.0d, 1.0d);
        this.coverageMode = clampInt(coverageMode, 0, 5);
        this.alignmentType = clampInt(alignmentType, 0, 2);
        this.tmscoreThreshold = clamp(tmscoreThreshold, 0.0d, 1.0d);
        this.exhaustiveSearch = exhaustiveSearch;
        this.exactTmscore = exactTmscore;
        this.gpu = gpu;
        this.verbosity = clampInt(verbosity, 0, 3);
        this.extraArgs = extraArgs == null ? List.of() : List.copyOf(extraArgs);
    }

    public static ProteinStructureConfig defaults() {
        return new ProteinStructureConfig(false, false, null, "NJ");
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean useStructureManifest() {
        return useStructureManifest;
    }

    public String structureManifestFile() {
        return structureManifestFile;
    }

    public String prostt5ModelPath() {
        return prostt5ModelPath;
    }

    public String treeBuilderMethod() {
        return treeBuilderMethod;
    }

    public int threads() {
        return threads;
    }

    public double sensitivity() {
        return sensitivity;
    }

    public double evalue() {
        return evalue;
    }

    public int maxSeqs() {
        return maxSeqs;
    }

    public double coverageThreshold() {
        return coverageThreshold;
    }

    public int coverageMode() {
        return coverageMode;
    }

    public int alignmentType() {
        return alignmentType;
    }

    public double tmscoreThreshold() {
        return tmscoreThreshold;
    }

    public boolean exhaustiveSearch() {
        return exhaustiveSearch;
    }

    public boolean exactTmscore() {
        return exactTmscore;
    }

    public boolean gpu() {
        return gpu;
    }

    public int verbosity() {
        return verbosity;
    }

    public List<String> extraArgs() {
        return extraArgs;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeTreeBuilderMethod(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return "NJ";
        }
        if ("Swift NJ".equalsIgnoreCase(normalized) || "SwiftNJ".equalsIgnoreCase(normalized)) {
            return "SwiftNJ";
        }
        return "NJ";
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
