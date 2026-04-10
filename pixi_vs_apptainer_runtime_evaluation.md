# Runtime Evaluation: Pixi vs Apptainer

## Question

Should this repository keep relying on the current host-level Pixi workflow, or should it add a container image such as Apptainer to make the pipeline easier to move across machines?

## Short Answer

Yes, this repository would benefit from adding an Apptainer image for runtime reproducibility.

My recommendation is not "replace Pixi with Apptainer everywhere", but:

- keep `pixi.toml` as the dependency manifest for development and package resolution
- add an Apptainer recipe as the supported runtime for users and reproducible testing
- stop treating the checked-in `.pixi/` environment as the portability mechanism

For this project, a hybrid model is the best technical fit.

## Why This Came Up

During real debugging on this repository, the following environment issues showed up:

- `pixi` was installed, but not on `PATH` in non-interactive shells
- the checked-in `.pixi/` environment was not reliably runnable as-is
- the vendored environment had missing library symlinks such as `libcblas.so.3`
- `Rscript` in the vendored environment still had a stale hardcoded prefix from an old install path
- binary names differed from script assumptions, for example `iqtree3` vs `iqtree`
- PHYLIP helper executables were present in non-obvious locations under `.pixi/.../share/phylip-*/exe/`
- local MAD had to be vendored manually to make rerooting reproducible

None of these problems are algorithmic. They are deployment problems.

## What The Current Pixi-Based Model Does Well

Pixi still provides real value here:

- it is a good declarative dependency manifest
- it is much easier to edit than a handwritten environment bootstrap script
- it works well for iterative development inside one known Linux environment
- it gives you a lockable software stack without having to manually list every binary dependency in bash scripts

If a single developer is working on one machine, Pixi is fine.

## What The Current Model Does Poorly

The current portability story is weak.

### 1. The checked-in `.pixi/` directory is not a reliable artifact

In practice, this repository has already shown that a copied or vendored `.pixi/` environment can become invalid because of:

- absolute-path assumptions
- wrapper scripts compiled or generated against a different prefix
- broken symlink chains
- dynamic library lookup differences between machines

This means the repo currently carries a large environment directory without actually getting reliable portability from it.

### 2. The runtime depends on shell details

At runtime, success depended on details such as:

- whether `pixi` is on `PATH`
- whether `LD_LIBRARY_PATH` is set
- whether the command is invoked as `python3.13` rather than `python`

That is manageable for debugging, but not a good default for users.

### 3. Multi-language stacks are exactly where containers help

This project mixes:

- Python
- R
- PHYLIP
- IQ-TREE
- MrBayes
- MAFFT
- a vendored MAD binary

Once you cross Python plus native bioinformatics binaries plus R, host-level reproducibility gets much harder. This is the classic point where containerized runtime starts paying off.

## Why Apptainer Fits This Repo

Apptainer is a strong fit for this project for several reasons.

### 1. This repo is already Linux-only

The repo is explicitly Linux-oriented. That removes one of the biggest objections to Apptainer.

### 2. Bioinformatics users often run on HPC

Apptainer is widely accepted on shared Linux servers and HPC clusters where Docker is often unavailable. For phylogenetics workflows, that matters.

### 3. It freezes the runtime, not just the dependency list

An Apptainer image can capture:

- exact binary names
- shared libraries
- wrapper scripts
- filesystem layout
- the vendored MAD location

That is the level of reproducibility this repo is currently missing.

### 4. It reduces machine-to-machine drift

With Apptainer, the command surface can become something like:

```bash
apptainer exec egps-onebuilder.sif zsh phylotree_builder_v0.0.1/s2_phylo_4prot.zsh ...
```

That is much less fragile than asking every machine to reproduce a correct Pixi shell with the right dynamic library behavior.

## Why Apptainer Should Not Fully Replace Pixi

I would not recommend throwing Pixi away.

Reasons:

- Pixi is still the easiest editable source of truth for dependencies
- developers need a maintainable way to add or update tools
- debugging inside a container-only workflow is slower and more awkward
- a container recipe by itself is not a pleasant day-to-day dependency management format

So the better architecture is:

- Pixi for environment definition and local development
- Apptainer for runtime distribution and reproducible execution

## Recommendation

### Recommended Direction

Adopt a hybrid deployment model.

#### Development mode

- keep `pixi.toml`
- keep wrapper scripts for local iterative work
- stop relying on the checked-in `.pixi/` directory as the main portability mechanism

#### Runtime mode

- add an Apptainer definition file
- build a single runtime image containing the validated toolchain
- document Apptainer as the preferred execution path for users and reproducible tests

### Recommendation Strength

Strong yes for Apptainer as a runtime artifact.

The current environment history is exactly the sort of evidence that justifies moving from "dependency manifest only" to "reproducible runtime image".

## Expected Benefits

If you add Apptainer, you should expect:

- fewer machine-specific startup failures
- fewer missing-library issues
- no dependence on user shell startup files
- no dependence on host `PATH` layout
- less time spent debugging installation drift
- a cleaner story for HPC and server deployment

## Expected Costs

You also take on some cost:

- maintaining an Apptainer recipe
- image rebuild time when dependencies change
- larger published artifact size than a plain source repo
- some friction for developers who want to inspect tools directly on the host

These costs are real, but for this repository they are smaller than the operational cost of repeatedly repairing broken runtime environments.

## Risks And Caveats

### 1. Apptainer will not fix workflow bugs

It solves packaging and reproducibility problems. It does not solve logical bugs in the pipelines.

### 2. Large example data should stay outside the image

The image should contain software and small required assets, not large demo outputs.

### 3. Java tanglegram support may need separate thought

The Java module is not standalone in this repository. It should not block the containerization of the core phylogenetics pipeline.

### 4. Build host matters

Because this repo is Linux-only in practice, the Apptainer build and test path should be standardized on Linux as well.

## Practical Migration Plan

### Phase 1: Clean runtime contract

- keep using the current wrapper scripts as the public entrypoints
- ensure both wrappers work without relying on interactive shell state
- remove any remaining hardcoded external paths from the main run path

### Phase 2: Add Apptainer recipe

- create an Apptainer definition file at the repo root or under `container/`
- install all runtime tools inside the image
- place MAD at the same path expected by the wrappers or pipeline
- make the image run the same wrapper scripts already used locally

### Phase 3: Verify one protein and one DNA demo inside the image

- protein demo should produce the method directories and `tree_summary/`
- DNA demo should do the same
- verify tree distance summary generation inside the image as part of smoke testing

### Phase 4: Update README

- keep local Pixi instructions for developers
- add Apptainer commands as the recommended reproducible execution path
- clearly separate "developer setup" from "run the pipeline"

## Suggested Final Position

If the goal is:

- convenient local development: keep Pixi
- reproducible runtime across machines: add Apptainer

If you must choose only one mechanism for users, choose Apptainer over a checked-in `.pixi/` environment.

That is the more robust operational choice for this repository.
