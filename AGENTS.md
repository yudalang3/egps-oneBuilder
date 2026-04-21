# eGPS oneBuilder

- Real code lives in `phylotree_builder_v0.0.1/`. Sample data and sample outputs now live at the repo root in `input_demo/` and `test1/`; do not assume they are still nested under `phylotree_builder_v0.0.1/`.
- Ignore `phylotree_builder_v0.0.1/.pixi/` unless you are debugging the environment itself. It is a checked-in vendored env and creates massive search noise. `pixi.lock` is marked generated/binary in `.gitattributes`; do not hand-edit it.
- This repo is Linux-only in practice: `pixi.toml` pins `platforms = ["linux-64"]`.

## Entry Points

- `phylotree_builder_v0.0.1/s1_quick_align.zsh <input.fasta>` runs MAFFT and writes `<input>.aligned.<ext>`.
- `phylotree_builder_v0.0.1/s2_phylo_4prot.zsh <input.fasta> [output_prefix]` is the main protein wrapper; it shells into `phylo_pipeline_4prot.py`.
- `phylotree_builder_v0.0.1/s2_phylo_4dna.zsh <input.fasta> [output_prefix]` is the main DNA/CDS wrapper; it shells into `phylo_pipeline_4dna.py`.
- `phylotree_builder_v0.0.1/phylo_pipeline_4prot.py` and `phylo_pipeline_4dna.py` are the real orchestrators.
- `phylotree_builder_v0.0.1/cal_pair_wise_tree_dist.R <tree_meta_data.tsv>` computes TreeDist and Robinson-Foulds matrices.
- `phylotree_builder_v0.0.1/help_utils.py` is the local MrBayes NEXUS -> Newick helper used by the protein pipeline.

## Pipeline Boundaries

- The protein and DNA/CDS pipelines are both self-contained for the main run path now: they use the local `help_utils.py`, local `cal_pair_wise_tree_dist.R`, and the vendored MAD binary.
- The remaining legacy DNA-only path is `ktreedist_method_path`, which still points outside the repo and is not part of the main `run_pipeline()` flow.

## Protein Pipeline Gotchas

- `phylo_pipeline_4prot.py` rewrites FASTA IDs to `seqN` before PHYLIP conversion (`name_convention`) and restores original names near the end. Do not break that mapping flow when changing sequence or tree handling.
- Expected output layout is method-scoped: `distance_method/`, `maximum_likelihood/`, `bayesian_method/`, `parsimony_method/`, plus `visualizations/` and `tree_summary/`.
- MAD rerooting now expects the vendored binary at `phylotree_builder_v0.0.1/third_party/mad/mad`. If that binary is absent, the protein pipeline warns and keeps the original trees.

## DNA Pipeline Gotchas

- `phylo_pipeline_4dna.py` now mirrors the protein pipeline's PHYLIP name-safety behavior: it rewrites FASTA IDs to `seqN` before PHYLIP conversion and restores the original names after tree processing.
- DNA outputs follow the same method-scoped layout as the protein pipeline: `distance_method/`, `maximum_likelihood/`, `bayesian_method/`, `parsimony_method/`, plus `visualizations/` and `tree_summary/`.
- The DNA wrapper also relies on the vendored MAD binary at `phylotree_builder_v0.0.1/third_party/mad/mad`.

## Data And Verification

- Demo aligned inputs live in `input_demo/simu/`.
- `test1/` is a checked-in sample output bundle that shows the expected result layout.
- No repo-local CI workflows, lint config, formatter config, or automated test harness were found. Verification here is manual and command-based.

## Environment Notes

- Repo scripts assume `pixi` is installed and callable as `pixi run --manifest-path ...`; `pixi.toml` defines dependencies only, not named tasks.
- In this workspace, `pixi` exists at `/home/dell/.pixi/bin/pixi` but is not on `PATH` in non-interactive shell sessions. Use the absolute path or export `PATH="/home/dell/.pixi/bin:$PATH"` before trying the wrapper scripts.
- Wrapper scripts call `python3.13`, not `python`, because the vendored Pixi env does not expose a plain `python` entrypoint here.
- The vendored Pixi env contains `iqtree3`, not `iqtree`; if the protein pipeline cannot find IQ-TREE, check binary naming before changing anything else.
- Directly invoking the vendored `.pixi` binaries is not a reliable fallback here: `.pixi/envs/default/bin/python3.13` fails importing NumPy because `libcblas.so.3` is unresolved, and `.pixi/envs/default/bin/Rscript` only worked after setting `LD_LIBRARY_PATH=.pixi/envs/default/lib`.

## Java Module

- `phylotree_builder_v0.0.1/java_tanglegram/` is not a standalone Java app in this repo. It is packaged as `egps2.module.treetanglegram` and imports `egps2.*` / `egps3.*` classes from the larger eGPS codebase. No local Java build file is present.
- Do not delete locally generated `.class` files under `phylotree_builder_v0.0.1/java_tanglegram/`; the user may need them to launch the GUI directly. Keep them locally, but do not add or commit them because `.gitignore` already excludes `*.class`.

## oneBuilder GUI Gotchas

- Keep the oneBuilder workflow fixed to four sections in this exact order: `Input / Align`, `Tree Parameters`, `Tree Build`, `Tanglegram`.
- `Tree Parameters` is the parameter editor only. Do not reintroduce run-state or output-path widgets inside the per-method parameter panels.
- `Tree Parameters` uses a method tree, not flat tabs/cards. The visible node names are `Distance Method`, `Maximum Likelihood`, `Bayes Method`, `Maximum Parsimony`, and `Protein Structure`.
- `Protein Structure` must stay visible even for non-protein input. For non-protein input it should be disabled and explain `Protein only` rather than disappearing.
- `Tree Build` is the dedicated run page. Keep the configuration/log text area plus `Run`, `Stop`, and `Export Config` there.
- The current run-state indicators belong in `Tree Build`, not `Tree Parameters`. Keep the compact five-item status area there, with `Protein Structure` shown as a reserved/non-running slot.
- Inside oneBuilder, `Tanglegram` should unlock only after a successful Linux run and should auto-load the current run output when opened.
- Windows cannot execute the pipeline from oneBuilder. Keep the startup warning behavior, including the "do not show again" checkbox and the matching Preference toggle to re-enable it later.
- User-facing wording matters here: prefer explicit labels such as `Run multiple sequence alignment first` instead of vague text like `Run alignment first`.
- Keep `Maxiterate` capitalized exactly that way in the GUI.
- `Advanced MAFFT` must remain editable whenever oneBuilder is not actively running; do not disable it just because alignment is currently unchecked.
- Keep `Advanced Parameters` placement consistent across method panels: it should appear directly below the primary controls, not buried at different vertical positions on different pages.
- Preserve clear task-pane affordances. If task-pane styling is changed, make sure the expand/collapse arrow stays easy to see.
- Do not add artificial restrictions against Chinese/Unicode paths in the GUI; the user explicitly wants Unicode-capable path handling.

## Java Verification

- Useful manual verification from `phylotree_builder_v0.0.1/`:
  - `javac -cp "lib/*:java_tanglegram" -d java_tanglegram java_tanglegram/onebuilder/*.java java_tanglegram/tanglegram/*.java java_tanglegram/tests/OneBuilderStandaloneTest.java java_tanglegram/tests/TanglegramStandaloneTest.java`
  - `java -cp "lib/*:java_tanglegram" onebuilder.OneBuilderStandaloneTest`
  - `java -cp "lib/*:java_tanglegram" tanglegram.TanglegramStandaloneTest`



Threading and Event Dispatch Thread (EDT) Best Practices

**CRITICAL RULE: Never perform time-consuming operations on the EDT thread.**

The Event Dispatch Thread (EDT) is responsible for handling all GUI events and rendering. Blocking the EDT causes the UI to freeze and creates a poor user experience.

**What must run on EDT:**

- All GUI component creation and modification (setText, setEnabled, addComponent, etc.)
- Showing dialogs (JOptionPane, JDialog, etc.)
- Updating table models, list models, or any Swing model
- Repainting and revalidating components
- Any Swing component method calls

**What must NOT run on EDT:**

- File I/O operations (reading/writing files)
- Network operations (HTTP requests, socket operations)
- Database queries
- Complex computations or data processing
- Module scanning, class loading, or reflection operations
- Any operation that takes more than ~100ms

**Correct pattern for time-consuming operations:**

```java
// WRONG - Blocks EDT
button.addActionListener(e -> {
    heavyComputation();     // BAD: Freezes UI
    updateTable();          // BAD: Mixed threading
});

// CORRECT - Background thread + EDT for GUI updates
button.addActionListener(e -> {
    // Disable button on EDT
    button.setEnabled(false);

    // Run heavy work in background thread
    new Thread(() -> {
        try {
            // TIME-CONSUMING OPERATIONS - NOT ON EDT
            Object result = heavyComputation();
            Object data = loadFromDatabase();

            // GUI UPDATES - DISPATCH TO EDT
            SwingUtilities.invokeLater(() -> {
                updateTable(result, data);
                button.setEnabled(true);
            });
        } catch (Exception ex) {
            // Error handling also needs EDT
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(parent, "Error: " + ex.getMessage());
                button.setEnabled(true);
            });
        }
    }, "BackgroundWorkerThread").start();
});
```

**Checking if you're on EDT:**

```java
if (SwingUtilities.isEventDispatchThread()) {
    // Safe to update GUI directly
    label.setText("Updated");
} else {
    // Must dispatch to EDT
    SwingUtilities.invokeLater(() -> label.setText("Updated"));
}
```
