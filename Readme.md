# eGPS oneBuilder

A Linux-based phylogenetic tree pipeline. It supports protein and DNA/CDS sequence alignment, tree inference, visualization, and comparison across multiple methods.

## Highlights

- The protein workflow is the most complete path and is recommended first.
- Supports MAFFT alignment, PHYLIP distance/parsimony methods, IQ-TREE maximum likelihood, and MrBayes Bayesian inference.
- Generates tree visualizations plus TreeDist and Robinson-Foulds distance statistics.
- Keeps the DNA pipeline and Java tanglegram module for compatibility with the wider eGPS ecosystem.

## Input and Output

### Input

- Multi-sequence protein FASTA, or multi-sequence DNA/CDS FASTA
- Aligned FASTA is recommended before tree inference

### Output

- `distance_method/`
- `maximum_likelihood/`
- `bayesian_method/`
- `parsimony_method/`
- `visualizations/`
- `tree_summary/`

## Quick Start

### Protein Workflow

```bash
zsh s1_quick_align.zsh input.fasta
zsh s2_phylo_4prot.zsh input.aligned.fasta
```

If you already have an aligned FASTA, run the main pipeline directly:

```bash
pixi run --manifest-path phylotree_builder_v0.0.1 python phylotree_builder_v0.0.1/phylo_pipeline_4prot.py input.aligned.fasta -o phylo_results_protein
```

### DNA/CDS Workflow

```bash
pixi run --manifest-path phylotree_builder_v0.0.1 python phylotree_builder_v0.0.1/phylo_pipeline_4dna.py input.aligned.fasta -o phylo_results
```

## Workflow

1. Start from an aligned FASTA file.
2. Convert it to PHYLIP format.
3. Build trees with four inference methods.
4. Reroot and ladderize the trees.
5. Generate plots, summary reports, and tree distance statistics.
6. Use the Java tanglegram module if you want to compare results interactively.

## Dependencies and Environment

- Linux
- Pixi environment, declared in `phylotree_builder_v0.0.1/pixi.toml`
- Core dependencies: `phylip`, `iqtree`, `mrbayes`, `biopython`, `ete4`, `r-treedist`, `matplotlib`, `mafft`
- The protein pipeline uses the local `help_utils.py` and `cal_pair_wise_tree_dist.R`
- MAD rerooting defaults to `/opt/BioInfo/MAD/mad/mad`; if it is missing, the original trees are kept

## Example Data

- `phylotree_builder_v0.0.1/input_demo/simu/`: simulated aligned input data
- `phylotree_builder_v0.0.1/test1/`: example output directory layout

## Other Modules

- `phylo_pipeline_4dna.py`: DNA/CDS pipeline kept for compatibility and extension; some helper paths are external to this repository
- `java_tanglegram/`: Java Swing tanglegram module for the broader eGPS platform; it is not a standalone Java application here

## Notes

- The protein pipeline temporarily renames sequence IDs to `seqN` before PHYLIP conversion, then restores the original names in the output trees.
- If your sequence IDs are long, prefer the protein wrapper scripts.
