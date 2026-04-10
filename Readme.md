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

## Runtime Expectations

The timings below come from real runs of the bundled demo inputs on the current development machine. Treat them as order-of-magnitude guidance, not fixed guarantees.

- Protein demo `gold_standard_protein_aligned.fasta`: about 8 to 9 minutes end to end.
- In the protein workflow, the slowest step is MrBayes. With the current default `ngen=50000`, that step alone took about 7.5 to 8.5 minutes.
- In the same protein run, IQ-TREE took about 35 to 40 seconds, distance and parsimony steps finished within seconds, and post-processing (MAD rerooting, visualization, and tree-distance summaries) took another roughly 20 to 30 seconds.
- DNA/CDS demo `gold_standard_cds_aligned.fasta`: about 40 to 45 seconds end to end.
- In the current DNA/CDS demo, IQ-TREE took about 10 to 12 seconds, MrBayes took about 9 to 10 seconds, and MAD rerooting plus visualization plus tree-distance summaries added another roughly 15 to 25 seconds.
- On larger datasets, with longer sequences, more bootstrap replicates, or more MrBayes generations, the Bayesian step is usually the first one that becomes the bottleneck.

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

- `phylo_pipeline_4dna.py`: DNA/CDS pipeline entrypoint, now aligned with the protein workflow's local helper scripts, MAD rerooting, and name-restoration flow
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
- With the current defaults, the protein workflow is much slower than the DNA/CDS workflow mainly because the protein pipeline runs MrBayes with more generations (`50000` vs `10000`), not because the other tree-building methods are inherently different in kind.
- Sample inputs and sample output directories live at the repository root, not inside `phylotree_builder_v0.0.1/`.
