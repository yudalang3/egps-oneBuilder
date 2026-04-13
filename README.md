# eGPS oneBuilder

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Chinese documentation: [`README_zh.md`](README_zh.md)

A Linux-based phylogenetic workflow that combines scriptable CLI wrappers with Java Swing GUIs. It supports protein and DNA/CDS sequence alignment, tree inference, visualization, interactive parameter tuning, and cross-method comparison.

## Highlights

- The repository provides one workflow for protein input and one for DNA/CDS input.
- Supports MAFFT alignment, PHYLIP distance/parsimony methods, IQ-TREE maximum likelihood, and MrBayes Bayesian inference.
- Adds `onebuilder.launcher`, a Java Swing workflow GUI for interactive parameter setting and in-window execution of the existing pipeline.
- Keeps the CLI wrappers for scripted and batch-style runs, so the same workflow can be driven either from the GUI or from shell automation.
- Lets the GUI pass MAFFT, method enable/disable, maximum-likelihood, and Bayesian settings into the existing shell wrappers and Python pipelines through `--config` runtime JSON.
- Provides a standalone Java tanglegram viewer for interactive comparison of the four inferred trees across six fixed pair views.
- Generates tree visualizations plus TreeDist / Robinson-Foulds matrices and a combined summary heatmap in `tree_summary/`.

## New Features

- `onebuilder.launcher` adds an interactive Java Swing workflow GUI for configuring alignment, tree-building methods, and exposed ML / Bayesian parameters before running the pipeline.
- The existing CLI wrappers remain first-class entrypoints, so the same workflow can still be scripted for repeated or batch-style runs.
- GUI and CLI now work together through a shared `--config` runtime JSON bridge instead of maintaining separate execution logic.
- `tanglegram.launcher` loads one `tree_summary/` result and renders the four inferred trees as six fixed pairwise comparison tabs.
- `tree_summary/` now includes a combined TreeDist + Robinson-Foulds heatmap figure in addition to the raw distance matrices.

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

### Wrapper Options

The wrappers now expose the options that the GUI uses internally:

- `s1_quick_align.zsh [--strategy localpair|auto|globalpair] [--maxiterate N] [--reorder|--no-reorder] <input.fasta>`
- `s2_phylo_4prot.zsh [--config runtime.json] <input.fasta> [output_prefix]`
- `s2_phylo_4dna.zsh [--config runtime.json] <input.fasta> [output_prefix]`

The optional `--config` JSON file is mainly intended for `onebuilder.launcher`. It lets the GUI pass method enable/disable state plus ML and Bayesian parameters into the existing shell wrappers and Python pipelines.

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

## Java GUI Tools

The repository now includes two standalone Java Swing entrypoints:

- `onebuilder.launcher`: the full workflow GUI with `Input / Align`, `Tree Build`, and `Tanglegram` tabs
- `tanglegram.launcher`: the focused pairwise tanglegram viewer for loading an existing `tree_summary/`

Compile from `phylotree_builder_v0.0.1/`:

```bash
javac -cp "lib/*:java_tanglegram" -d java_tanglegram \
  java_tanglegram/tanglegram/*.java \
  java_tanglegram/onebuilder/*.java
```

If you only want to validate the Swing windows on Windows PowerShell, use `;` instead of `:` in the classpath:

```powershell
javac -cp "lib/*;java_tanglegram" -d java_tanglegram `
  java_tanglegram/tanglegram/*.java `
  java_tanglegram/onebuilder/*.java
```

### Full Workflow GUI

Launch from a Linux desktop or an X11-forwarded terminal session:

```bash
java -cp "java_tanglegram:lib/*" onebuilder.launcher
```

```powershell
java -cp "java_tanglegram;lib/*" onebuilder.launcher
```

Usage notes:

- The top-level tabs are fixed to `Input / Align`, `Tree Build`, and `Tanglegram`.
- `Tree Build` uses a left-side tab set with `Distance`, `ML`, `Bayesian`, and `Parsimony`.
- Enable `Run alignment first` if the input FASTA is not aligned yet; the GUI forwards MAFFT settings into `s1_quick_align.zsh`.
- The input page includes `Run` and `Stop`, output directory/prefix fields, MAFFT strategy, `maxiterate`, and sequence reorder control.
- The tree-building page keeps live run status, current stage, aligned-input path, output-root path, and a scrolling stdout/stderr log panel in the same window.
- The GUI lets you disable individual tree-building methods and tune the exposed ML and Bayesian parameters before starting a run.
- The GUI writes a temporary runtime JSON config and passes it into `s2_phylo_4prot.zsh` or `s2_phylo_4dna.zsh`.
- The `Tanglegram` page is current-run only. It unlocks after the current GUI run creates a usable `tree_summary/`.
- Inside the GUI, the `Tanglegram` page also exposes label-font size, horizontal/vertical padding, auto-fit, and a `Reload from current run` action.
- The launchers explicitly disable FlatLaf's native library integration with `flatlaf.useNativeLibrary=false`, so JDK 24+ does not print the `--enable-native-access=ALL-UNNAMED` warning.

### Tanglegram Viewer

Launch without loading data first:

```bash
java -cp "java_tanglegram:lib/*" tanglegram.launcher
```

```powershell
java -cp "java_tanglegram;lib/*" tanglegram.launcher
```

Launch and immediately load one pipeline result:

```bash
java -cp "java_tanglegram:lib/*" tanglegram.launcher -dir /path/to/tree_summary
```

```powershell
java -cp "java_tanglegram;lib/*" tanglegram.launcher -dir C:\path\to\tree_summary
```

Usage notes:

- The window title is `Tanglegram`.
- The menu bar contains only `Files > Open`.
- `Open` expects a `tree_summary/` directory from a pipeline run.
- The viewer always lays out the available comparisons as fixed pair tabs in this order: `NJ-ML`, `NJ-BI`, `NJ-MP`, `ML-BI`, `ML-MP`, `BI-MP`.
- If one method is missing but at least two trees can still be resolved, the viewer loads only the valid pair tabs instead of failing the whole window.
- If `tree_meta_data.tsv` contains stale or machine-specific paths, the viewer falls back to the standard sibling folders `distance_method/`, `maximum_likelihood/`, `bayesian_method/`, and `parsimony_method/`.
- If no `-dir` argument is provided, the app starts empty and waits for the user to import a `tree_summary/` directory from the menu.

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
- `java_tanglegram/`: standalone Java Swing tanglegram viewer for comparing the four pipeline trees in six pairwise tabs

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

## License

Apache-2.0. See `LICENSE`.
