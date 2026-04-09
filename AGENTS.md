# eGPS oneBuilder

- Real code lives in `phylotree_builder_v0.0.1/`. Root `Readme.md` is empty; start from `Readme_zh.md`, `pixi.toml`, and the pipeline entrypoints instead.
- Ignore `phylotree_builder_v0.0.1/.pixi/` unless you are debugging the environment itself. It is a checked-in vendored env and creates massive search noise. `pixi.lock` is marked generated/binary in `.gitattributes`; do not hand-edit it.
- This repo is Linux-only in practice: `pixi.toml` pins `platforms = ["linux-64"]`.

## Entry Points

- `phylotree_builder_v0.0.1/s1_quick_align.zsh <input.fasta>` runs MAFFT and writes `<input>.aligned.<ext>`.
- `phylotree_builder_v0.0.1/s2_phylo_4prot.zsh <input.fasta> [output_prefix]` is the main protein wrapper; it shells into `phylo_pipeline_4prot.py`.
- `phylotree_builder_v0.0.1/phylo_pipeline_4prot.py` and `phylo_pipeline_4dna.py` are the real orchestrators.
- `phylotree_builder_v0.0.1/cal_pair_wise_tree_dist.R <tree_meta_data.tsv>` computes TreeDist and Robinson-Foulds matrices.
- `phylotree_builder_v0.0.1/help_utils.py` is the local MrBayes NEXUS -> Newick helper used by the protein pipeline.

## Pipeline Boundaries

- Prefer the protein pipeline when possible. It is the more self-contained path in this repo: it uses the local `help_utils.py` and local `cal_pair_wise_tree_dist.R`.
- Treat the DNA pipeline as fragile/legacy unless you are fixing it intentionally:
- It hardcodes external helper paths that are not in this repo: `/home/dell/projects/evol_homo_genes_db/scripts/ref_fun/cal_pair_wise_tree_dist.R` and `ref_fun/parse_mrb_tree`.
- Its `__main__` block deletes `phylo_results/` and all `*.log` files in the current working directory before running.
- Its dependency check may try to `conda install` missing tools.

## Protein Pipeline Gotchas

- `phylo_pipeline_4prot.py` rewrites FASTA IDs to `seqN` before PHYLIP conversion (`name_convention`) and restores original names near the end. Do not break that mapping flow when changing sequence or tree handling.
- Expected output layout is method-scoped: `distance_method/`, `maximum_likelihood/`, `bayesian_method/`, `parsimony_method/`, plus `visualizations/` and `tree_summary/`.
- MAD rerooting is hardcoded to `/opt/BioInfo/MAD/mad/mad`. If that binary is absent, the protein pipeline warns and keeps the original trees.

## Data And Verification

- Demo aligned inputs live in `phylotree_builder_v0.0.1/input_demo/simu/`.
- `phylotree_builder_v0.0.1/test1/` is a checked-in sample output bundle that shows the expected result layout.
- No repo-local CI workflows, lint config, formatter config, or automated test harness were found. Verification here is manual and command-based.

## Environment Notes

- Repo scripts assume `pixi` is installed and callable as `pixi run --manifest-path ...`; `pixi.toml` defines dependencies only, not named tasks.
- In this workspace, `pixi` was not on `PATH`, so the wrapper scripts could print usage but not actually run their Pixi commands.
- Directly invoking the vendored `.pixi` binaries is not a reliable fallback here: `.pixi/envs/default/bin/python3.13` fails importing NumPy because `libcblas.so.3` is unresolved, and `.pixi/envs/default/bin/Rscript` only worked after setting `LD_LIBRARY_PATH=.pixi/envs/default/lib`.

## Java Module

- `phylotree_builder_v0.0.1/java_tanglegram/` is not a standalone Java app in this repo. It is packaged as `egps2.module.treetanglegram` and imports `egps2.*` / `egps3.*` classes from the larger eGPS codebase. No local Java build file is present.
