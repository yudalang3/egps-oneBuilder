# eGPS oneBuilder

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Chinese documentation: [`README_zh.md`](README_zh.md)

A Linux-based phylogenetic workflow that combines scriptable CLI wrappers with Java Swing GUIs. The actual alignment and tree-building pipeline is Linux-only. On Windows, the Swing tools are limited to GUI configuration export and standalone tanglegram viewing for existing results.

## 0. Highlights

- The repository provides one workflow for protein input and one for DNA/CDS input.
- Supports MAFFT alignment, PHYLIP distance/parsimony methods, IQ-TREE maximum likelihood, and MrBayes Bayesian inference.
- Adds `onebuilder.launcher`, a Java Swing workflow GUI with `Input / Align`, `Tree Parameters`, `Tree Build`, and `Tanglegram`. On Linux it can run the existing pipeline; on Windows it is limited to workflow setup, config export, and standalone result viewing.
- Keeps the CLI wrappers for scripted and batch-style runs, so the same workflow can be driven either from the GUI or from shell automation.
- Lets the GUI pass MAFFT, method enable/disable, maximum-likelihood, Bayesian, and raw passthrough settings into the existing shell wrappers and Python pipelines through `--config` runtime JSON.
- Generates tree visualizations plus TreeDist / Robinson-Foulds matrices and a combined summary heatmap in `tree_summary/`.
- Provides a standalone Java tanglegram viewer for interactive comparison of the four inferred trees across six fixed pair views on both Linux and Windows.
- `tanglegram.launcher` can directly load one `tree_summary/` result and render the four trees as six pairwise comparison tabs.

## 0.1 New Features

- `onebuilder.launcher` adds an interactive Java Swing workflow GUI with a four-step workspace: `Input / Align`, `Tree Parameters`, `Tree Build`, and `Tanglegram`.
- The parameter editor is now separated from the run page: method settings live in `Tree Parameters`, while `Tree Build` focuses on draft review, run/export controls, live logs, and compact method-status indicators.
- The existing CLI wrappers remain first-class entrypoints, so the same workflow can still be scripted for repeated or batch-style runs.
- GUI and CLI now work together through a shared `--config` runtime JSON bridge instead of maintaining separate execution logic.
- `tanglegram.launcher` loads one `tree_summary/` result and renders the four inferred trees as six fixed pairwise comparison tabs.
- `tree_summary/` now includes a combined TreeDist + Robinson-Foulds heatmap figure in addition to the raw distance matrices.
- On Windows, `onebuilder.launcher` now shows a startup warning explaining that pipeline execution is disabled there, with a preference toggle to hide or re-enable that notice later.

## 1. Input and Output

### 1.1 Input

- Multi-sequence protein FASTA, or multi-sequence DNA/CDS FASTA
- You can provide either aligned or unaligned multi-sequence FASTA
- If the input is already aligned, run the tree pipeline directly
- If the input is not aligned yet, you can either align it yourself first or let the GUI / CLI run MAFFT automatically before tree inference

### 1.2 Output

- `distance_method/`
- `maximum_likelihood/`
- `bayesian_method/`
- `parsimony_method/`
- `visualizations/`
- `tree_summary/`

## 2. Quick Start

### 2.1 Pure GUI

Launch the workflow GUI and configure everything interactively:

```bash
java -cp "java_tanglegram:lib/*" onebuilder.launcher
```

Use this path when you want to set alignment, method selection, and ML/Bayesian options in the window and run from the same place.

View already-run results:

```bash
java -cp "java_tanglegram:lib/*" tanglegram.launcher
```

### 2.2 Pure CLI

Run the pipeline directly from the shell, which is convenient for scripting and batch jobs.

#### 2.2.1 Protein Sequence

```bash
zsh phylotree_builder_v0.0.1/s2_phylo_4prot.zsh \
  input_demo/simu/gold_standard_protein_aligned.fasta demo_protein
```

#### 2.2.2 DNA Sequence

```bash
zsh phylotree_builder_v0.0.1/s2_phylo_4dna.zsh \
  input_demo/simu/gold_standard_cds_aligned.fasta demo_dna
```

#### 2.2.3 Unaligned Sequence

If your input FASTA is not aligned yet, run MAFFT first, then pass the aligned result into the tree pipeline:

```bash
zsh phylotree_builder_v0.0.1/s1_quick_align.zsh input.fasta
```

### 2.3 GUI plus CLI

This is the main hybrid workflow: use the GUI to set parameters, export one runtime JSON, then replay the whole flow from the shell with a single command.

```bash
java -cp "java_tanglegram:lib/*" onebuilder.launcher
```

Then run that exported JSON on Linux:

```bash
zsh phylotree_builder_v0.0.1/run_onebuilder_config.zsh /path/to/demo.onebuilder.json
```

This script reads `run.input_type`, `run.input_file`, `run.output_base_dir`, `run.output_prefix`, and `alignment.run_alignment_first` from the JSON, optionally runs MAFFT first, then dispatches to the correct protein or DNA/CDS wrapper automatically.

The lower-level wrappers remain available, but `run_onebuilder_config.zsh` is now the intended CLI entrypoint for replaying a GUI-exported configuration.

### 2.4 Full Config Import Run

You can also start from one complete runtime JSON directly, without reopening the GUI first. This is useful when you want to keep one checked or shared config file as the single source of truth for repeated Linux runs.

```bash
zsh phylotree_builder_v0.0.1/run_onebuilder_config.zsh tree_build_full_config_template.json
```

In practice you should first copy `tree_build_full_config_template.json`, then fill in the `run` paths and the method settings you care about.

The full template may include both protein-oriented and DNA/CDS-oriented method blocks at the same time. The runtime loader now resolves that by reading `run.input_type` first, then selecting the matching method settings automatically during execution.

For example, with `run.input_type` set to `DNA_CDS`, the run will use `dnadist`, `dnapars`, DNA MrBayes settings, and DNA-safe IQ-TREE settings even if the same JSON still contains protein-only blocks for another job.

### 2.5 Wrapper Options

The wrappers now expose the options that the GUI uses internally:

- `run_onebuilder_config.zsh <runtime.json>` or `run_onebuilder_config.zsh --config <runtime.json>`
- `s1_quick_align.zsh [--config runtime.json] [--strategy localpair|genafpair|auto|globalpair] [--maxiterate N] [--reorder|--no-reorder] <input.fasta>`
- `s2_phylo_4prot.zsh [--config runtime.json] <input.fasta> [output_prefix]`
- `s2_phylo_4dna.zsh [--config runtime.json] <input.fasta> [output_prefix]`

`run_onebuilder_config.zsh` is Linux-only and is the highest-level CLI wrapper. It reuses the JSON exported by `onebuilder.launcher` directly, so you do not need to repeat the input file or output prefix on the command line.

The lower-level `--config` JSON support on `s1_quick_align.zsh`, `s2_phylo_4prot.zsh`, and `s2_phylo_4dna.zsh` remains available. It lets the GUI pass alignment, method enable/disable state, ML parameters, Bayesian parameters, and raw passthrough blocks into the existing shell wrappers and Python pipelines.

Detailed parameter reference:

- Chinese reference: [`tree_build_pipeline_parameter_reference_zh.md`](tree_build_pipeline_parameter_reference_zh.md)
- Full JSON template: [`tree_build_full_config_template.json`](tree_build_full_config_template.json)

## 3. Runtime Expectations

The timings below come from real runs of the bundled demo inputs on the current development machine. Treat them as order-of-magnitude guidance, not fixed guarantees.

- Protein demo `gold_standard_protein_aligned.fasta`: about 8 to 9 minutes end to end.
- In the protein workflow, the slowest step is MrBayes. With the current default `ngen=50000`, that step alone took about 7.5 to 8.5 minutes.
- In the same protein run, IQ-TREE took about 35 to 40 seconds, distance and parsimony steps finished within seconds, and post-processing (MAD rerooting, visualization, and tree-distance summaries) took another roughly 20 to 30 seconds.
- DNA/CDS demo `gold_standard_cds_aligned.fasta`: about 40 to 45 seconds end to end.
- In the current DNA/CDS demo, IQ-TREE took about 10 to 12 seconds, MrBayes took about 9 to 10 seconds, and MAD rerooting plus visualization plus tree-distance summaries added another roughly 15 to 25 seconds.
- On larger datasets, with longer sequences, more bootstrap replicates, or more MrBayes generations, the Bayesian step is usually the first one that becomes the bottleneck.

## 4. Workflow

1. Start from an aligned FASTA file.
2. Convert it to PHYLIP format.
3. Build trees with four inference methods.
4. Reroot and ladderize the trees.
5. Generate plots, summary reports, and tree distance statistics.
6. Use the Java tanglegram module if you want to compare results interactively.

## 5. Java GUI Tools

The repository now includes two standalone Java Swing entrypoints:

- `onebuilder.launcher`: the full workflow GUI with `Input / Align`, `Tree Parameters`, `Tree Build`, and `Tanglegram`. Linux can run the pipeline; Windows is for setup and JSON config export only.
- `tanglegram.launcher`: the focused pairwise tanglegram viewer for loading an existing `tree_summary/` on Linux or Windows.
- `run_onebuilder_config.zsh`: the Linux-only JSON runner that replays a GUI-exported `.onebuilder.json` without re-entering the input file or output prefix.

Compile from the repository root:

```bash
javac -cp "phylotree_builder_v0.0.1/lib/*:phylotree_builder_v0.0.1/java_tanglegram" -d phylotree_builder_v0.0.1/java_tanglegram \
  phylotree_builder_v0.0.1/java_tanglegram/tanglegram/*.java \
  phylotree_builder_v0.0.1/java_tanglegram/onebuilder/*.java \
  phylotree_builder_v0.0.1/java_tanglegram/tests/*.java
```

```powershell
javac -cp "phylotree_builder_v0.0.1\lib/*;phylotree_builder_v0.0.1\java_tanglegram" -d phylotree_builder_v0.0.1\java_tanglegram `
  phylotree_builder_v0.0.1\java_tanglegram\tanglegram\*.java `
  phylotree_builder_v0.0.1\java_tanglegram\onebuilder\*.java `
  phylotree_builder_v0.0.1\java_tanglegram\tests\*.java
```

Or first `cd phylotree_builder_v0.0.1/`, then use:

```bash
javac -cp "lib/*:java_tanglegram" -d java_tanglegram \
  java_tanglegram/tanglegram/*.java \
  java_tanglegram/onebuilder/*.java \
  java_tanglegram/tests/*.java
```

```powershell
javac -cp "lib/*;java_tanglegram" -d java_tanglegram `
  java_tanglegram/tanglegram/*.java `
  java_tanglegram/onebuilder/*.java `
  java_tanglegram/tests/*.java
```

### 5.1 Full Workflow GUI

Linux launch from the repository root:

```bash
java -cp "phylotree_builder_v0.0.1/java_tanglegram:phylotree_builder_v0.0.1/lib/*" onebuilder.launcher
```

Linux launch after `cd phylotree_builder_v0.0.1/`:

```bash
java -cp "java_tanglegram:lib/*" onebuilder.launcher
```

Windows PowerShell launch from the repository root:

```powershell
java -cp "phylotree_builder_v0.0.1\java_tanglegram;phylotree_builder_v0.0.1\lib/*" onebuilder.launcher
```

Windows PowerShell launch after `Set-Location phylotree_builder_v0.0.1`:

```powershell
java -cp "java_tanglegram;lib/*" onebuilder.launcher
```

Usage notes:

- The top-level workflow is fixed to `Input / Align`, `Tree Parameters`, `Tree Build`, and `Tanglegram`.
- `Input / Align` is the only page that must be completed first. If required fields are missing, later sections stay clickable but explain why navigation is blocked.
- `Tree Parameters` uses a method tree with `Distance Method`, `Maximum Likelihood`, `Bayes Method`, `Maximum Parsimony`, and `Protein Structure`.
- `Protein Structure` is always visible in the method tree. It is enabled for protein input and shown as `Protein only` for non-protein input.
- `Tree Build` is now the dedicated run page. It contains the configuration summary/log area plus `Run`, `Stop`, and `Export Config` actions.
- The `Tree Build` page also shows five compact method-status indicators for `Distance Method`, `Maximum Likelihood`, `Bayes Method`, `Maximum Parsimony`, and the reserved `Protein Structure` slot.
- Actual alignment and tree building are Linux-only. Use a Linux desktop session or X11 forwarding for real runs.
- On Windows, `onebuilder.launcher` is limited to parameter editing and JSON config export. It does not execute `s1_quick_align.zsh`, `s2_phylo_4prot.zsh`, or `s2_phylo_4dna.zsh`, and it shows a startup warning unless that notice is disabled in preferences.
- Enable `Run multiple sequence alignment first` if the input FASTA is raw and not aligned yet; on Linux the GUI forwards MAFFT settings into `s1_quick_align.zsh`.
- The input page includes input/output paths, remembered browse locations, MAFFT strategy, `Maxiterate`, sequence reorder control, `Advanced MAFFT`, `Export config file when running`, and `Export JSON`.
- Advanced parameter groups are collapsed by default with a shared task-pane style and now appear in a consistent position directly below the primary controls in each method page.
- The GUI lets you disable individual tree-building methods and tune the exposed ML, Bayesian, and structured PHYLIP common parameters before starting a run.
- The ML page now gives non-blocking bootstrap guidance: `1000` remains the recommended default, `0` explicitly means "skip -bb", and very large values show a runtime warning instead of hard-blocking the run.
- The PHYLIP pages now keep `menu_overrides` as the advanced escape hatch, while exposing stable common controls such as DNA distance model settings, neighbor type/outgroup, protein `protpars` print toggles, and DNA `dnapars` outgroup/transversion options directly in the GUI.
- The window also includes `Preference > Settings...` for shared UI preferences such as global font family, global font size, window-size restore, the default tanglegram label-font size, and `Show Windows oneBuilder startup warning`.
- Preference changes are applied live to currently open Java windows and are reused on the next launch.
- Shared GUI preferences are stored in `~/.egps.onebuilder.prop` instead of the Windows registry, so the same storage model is used on Linux and Windows.
- `Export config file when running` is enabled by default. When it stays enabled, Linux runs save `<output_base_dir>/<output_prefix>.onebuilder.json` and pass that file into the wrappers.
- If `Export config file when running` is disabled, Linux runs still work but use a temporary runtime JSON file for that run only.
- `Export JSON` always writes the current GUI settings to `<output_base_dir>/<output_prefix>.onebuilder.json`.
- The exported `<output_prefix>.onebuilder.json` is now directly executable on Linux through `zsh phylotree_builder_v0.0.1/run_onebuilder_config.zsh /path/to/file.onebuilder.json`.
- Inside `onebuilder.launcher`, the `Tanglegram` page unlocks only after a successful Linux run and then auto-loads the current output directory. On Windows, use the standalone `tanglegram.launcher` to inspect an existing `tree_summary/`.
- Inside the GUI, the `Tanglegram` page also exposes label-font size, horizontal/vertical padding, auto-fit, and a `Reload from current run` action.
- The launchers explicitly disable FlatLaf's native library integration with `flatlaf.useNativeLibrary=false`, so JDK 24+ does not print the `--enable-native-access=ALL-UNNAMED` warning.

### 5.2 Tanglegram Viewer

Launch without loading data first from the repository root:

```bash
java -cp "phylotree_builder_v0.0.1/java_tanglegram:phylotree_builder_v0.0.1/lib/*" tanglegram.launcher
```

```powershell
java -cp "phylotree_builder_v0.0.1\java_tanglegram;phylotree_builder_v0.0.1\lib/*" tanglegram.launcher
```

After `cd phylotree_builder_v0.0.1/`:

```bash
java -cp "java_tanglegram:lib/*" tanglegram.launcher
```

```powershell
java -cp "java_tanglegram;lib/*" tanglegram.launcher
```

Launch and immediately load one pipeline result:

```bash
java -cp "phylotree_builder_v0.0.1/java_tanglegram:phylotree_builder_v0.0.1/lib/*" tanglegram.launcher -dir test1/tree_summary
```

```powershell
java -cp "phylotree_builder_v0.0.1\java_tanglegram;phylotree_builder_v0.0.1\lib/*" tanglegram.launcher -dir test1\tree_summary
```

Usage notes:

- The window title is `Tanglegram`.
- If you already have pipeline output on Windows, this is the viewer to use for the six pairwise comparisons.
- The menu bar contains `Files > Open` and `Preference > Settings...`.
- `Preference` shares the same global UI settings as `onebuilder.launcher`, including the global font and the default tanglegram label-font size.
- Those shared UI settings are stored in `~/.egps.onebuilder.prop`.
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
