package onebuilder;

public enum InputType {
    PROTEIN("Protein", "s2_phylo_4prot.zsh"),
    DNA_CDS("DNA/CDS", "s2_phylo_4dna.zsh");

    private final String displayName;
    private final String buildScriptName;

    InputType(String displayName, String buildScriptName) {
        this.displayName = displayName;
        this.buildScriptName = buildScriptName;
    }

    public String displayName() {
        return displayName;
    }

    public String buildScriptName() {
        return buildScriptName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
