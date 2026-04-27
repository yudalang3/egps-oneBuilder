package onebuilder;

public final class ProteinStructureConfig {
    private final boolean enabled;
    private final boolean useStructureManifest;
    private final String structureManifestFile;
    private final String treeBuilderMethod;

    public ProteinStructureConfig(boolean enabled, boolean useStructureManifest, String structureManifestFile) {
        this(enabled, useStructureManifest, structureManifestFile, "NJ");
    }

    public ProteinStructureConfig(
            boolean enabled,
            boolean useStructureManifest,
            String structureManifestFile,
            String treeBuilderMethod) {
        this.enabled = enabled;
        this.useStructureManifest = useStructureManifest;
        this.structureManifestFile = normalizeText(structureManifestFile);
        this.treeBuilderMethod = normalizeTreeBuilderMethod(treeBuilderMethod);
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

    public String treeBuilderMethod() {
        return treeBuilderMethod;
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
}
