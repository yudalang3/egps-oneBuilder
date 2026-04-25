package onebuilder;

public final class ProteinStructureConfig {
    private final boolean enabled;
    private final boolean useStructureManifest;
    private final String structureManifestFile;

    public ProteinStructureConfig(boolean enabled, boolean useStructureManifest, String structureManifestFile) {
        this.enabled = enabled;
        this.useStructureManifest = useStructureManifest;
        this.structureManifestFile = normalizeText(structureManifestFile);
    }

    public static ProteinStructureConfig defaults() {
        return new ProteinStructureConfig(false, false, null);
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

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
