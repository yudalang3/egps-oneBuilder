# eGPS oneBuilder

A Linux-based phylogenetic tree pipeline. It supports protein and DNA/CDS sequence alignment, tree inference, visualization, and comparison across multiple methods.

## Highlights

- The repository provides one workflow for protein input and one for DNA/CDS input.
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

### Protein Input

```bash
zsh phylotree_builder_v0.0.1/s2_phylo_4prot.zsh \
  input_demo/simu/gold_standard_protein_aligned.fasta demo_protein
```

### DNA/CDS Input

```bash
zsh phylotree_builder_v0.0.1/s2_phylo_4dna.zsh \
  input_demo/simu/gold_standard_cds_aligned.fasta demo_dna
```

### Unaligned Input

If your input FASTA is not aligned yet, run MAFFT first:

```bash
zsh phylotree_builder_v0.0.1/s1_quick_align.zsh input.fasta
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
- MAD rerooting defaults to `phylotree_builder_v0.0.1/third_party/mad/mad`

## Example Data

- `input_demo/simu/`: simulated aligned input data
- `test1/`: example output directory layout

## Other Modules

- `phylo_pipeline_4dna.py`: DNA/CDS pipeline kept for compatibility and extension; some helper paths are external to this repository
- `java_tanglegram/`: Java Swing tanglegram module for the broader eGPS platform; it is not a standalone Java application here

## Notes

- Both the protein and DNA/CDS pipelines temporarily rename sequence IDs to `seqN` before PHYLIP conversion, then restore the original names in the output trees.
- Prefer the repository wrapper scripts over assembling `pixi run ... python3.13 ...` commands by hand.

## Known Gotchas

- PHYLIP uses the strict format here, so sequence IDs traditionally need to stay within 10 characters. Both repository pipelines handle this automatically by renaming IDs to `seqN`, but the limitation still matters if you invoke PHYLIP tools manually.
- Use the protein workflow for protein input and the DNA/CDS workflow for DNA or CDS input.
- The wrapper scripts assume that `pixi` is callable. If your shell cannot find it, add Pixi to `PATH` or set `PIXI_EXE` explicitly.
- When invoking the Python pipelines directly, use `python3.13` instead of assuming a plain `python` executable exists inside the Pixi environment.
- If you run the main Python scripts directly rather than using the wrapper scripts, some environments also require `LD_LIBRARY_PATH=phylotree_builder_v0.0.1/.pixi/envs/default/lib`; otherwise dependencies such as NumPy, R, or IQ-TREE may fail to load.
- In the current Pixi environment, IQ-TREE may appear as `iqtree3`; the repository scripts handle this automatically, but manual commands need to account for the binary name.
- MAD rerooting depends on the vendored binary at `phylotree_builder_v0.0.1/third_party/mad/mad`. If it is missing, the pipelines keep the original trees and continue.
- Sample inputs and sample output directories live at the repository root, not inside `phylotree_builder_v0.0.1/`.
