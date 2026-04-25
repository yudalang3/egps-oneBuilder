package onebuilder;

import java.nio.file.Path;

public enum TreeMethodKey {
    DISTANCE("Distance"),
    MAXIMUM_LIKELIHOOD("Maximum Likelihood"),
    BAYESIAN("Bayesian"),
    PARSIMONY("Parsimony"),
    PROTEIN_STRUCTURE("Protein Structure");

    private final String displayName;

    TreeMethodKey(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public Path expectedOutputPath(Path outputRoot, InputType inputType) {
        switch (this) {
            case DISTANCE:
                return outputRoot.resolve("distance_method").resolve("distance_tree.nwk.rooted.renamed.ladderize");
            case MAXIMUM_LIKELIHOOD:
                return outputRoot.resolve("maximum_likelihood").resolve("ml_tree.treefile.rooted.renamed.ladderize");
            case BAYESIAN:
                return outputRoot.resolve("bayesian_method")
                        .resolve("alignment.nex.con.tre.nwk.rooted.renamed.ladderize");
            case PARSIMONY:
                if (inputType == InputType.PROTEIN) {
                    return outputRoot.resolve("parsimony_method").resolve("parsimony_tree.nwk.renamed.ladderize");
                }
                return outputRoot.resolve("parsimony_method").resolve("parsimony_tree.nwk.rooted.renamed.ladderize");
            case PROTEIN_STRUCTURE:
                return outputRoot.resolve("protein_structure").resolve("pairwise_scores.tsv");
            default:
                throw new IllegalStateException("Unexpected tree method: " + this);
        }
    }
}
