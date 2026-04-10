package tanglegram;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

enum TreeMethod {
    NJ_PHYLIP(
            "NJ_phylip",
            "NJ",
            Arrays.asList(
                    "distance_method/distance_tree.nwk.rooted.renamed.ladderize",
                    "distance_method/distance_tree.nwk.rooted.ladderize",
                    "distance_method/distance_tree.nwk.rooted",
                    "distance_method/distance_tree.nwk")),
    ML_IQTREE(
            "ML_iqtree",
            "ML",
            Arrays.asList(
                    "maximum_likelihood/ml_tree.treefile.rooted.renamed.ladderize",
                    "maximum_likelihood/ml_tree.treefile.rooted.ladderize",
                    "maximum_likelihood/ml_tree.treefile.rooted",
                    "maximum_likelihood/ml_tree.treefile")),
    BI_MRBAYES(
            "BI_mrbayes",
            "BI",
            Arrays.asList(
                    "bayesian_method/alignment.nex.con.tre.nwk.rooted.renamed.ladderize",
                    "bayesian_method/alignment.nex.con.tre.nwk.rooted.ladderize",
                    "bayesian_method/alignment.nex.con.tre.nwk.rooted",
                    "bayesian_method/alignment.nex.con.tre.nwk",
                    "bayesian_method/alignment.nex.con.tre")),
    MP_PHYLIP(
            "MP_phylip",
            "MP",
            Arrays.asList(
                    "parsimony_method/parsimony_tree.nwk.rooted.renamed.ladderize",
                    "parsimony_method/parsimony_tree.nwk.renamed.ladderize",
                    "parsimony_method/parsimony_tree.nwk.rooted.ladderize",
                    "parsimony_method/parsimony_tree.nwk.ladderize",
                    "parsimony_method/parsimony_tree.nwk"));

    static final List<TreeMethod> DISPLAY_ORDER = Collections.unmodifiableList(
            Arrays.asList(NJ_PHYLIP, ML_IQTREE, BI_MRBAYES, MP_PHYLIP));

    private final String metadataKey;
    private final String shortLabel;
    private final List<String> fallbackCandidates;

    TreeMethod(String metadataKey, String shortLabel, List<String> fallbackCandidates) {
        this.metadataKey = metadataKey;
        this.shortLabel = shortLabel;
        this.fallbackCandidates = fallbackCandidates;
    }

    String metadataKey() {
        return metadataKey;
    }

    String shortLabel() {
        return shortLabel;
    }

    List<Path> fallbackCandidates(Path outputRootDir) {
        List<Path> resolvedCandidates = new ArrayList<>();
        for (String fallbackCandidate : fallbackCandidates) {
            resolvedCandidates.add(outputRootDir.resolve(fallbackCandidate).normalize());
        }
        return resolvedCandidates;
    }

    static TreeMethod fromMetadataKey(String metadataKey) {
        for (TreeMethod method : values()) {
            if (method.metadataKey.equals(metadataKey)) {
                return method;
            }
        }
        return null;
    }
}
