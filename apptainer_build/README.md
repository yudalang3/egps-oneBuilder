# oneBuilder Apptainer Image

This directory contains the tracked Apptainer build recipe for oneBuilder.

The large local build workspace is intentionally separate from this tracked recipe directory. In this workspace, the repository path `apptainer` is a local symlink to `/home/dell/project/oneBuilder_apptainer`, so the large `stage/` directory and final `.sif` image stay on the Linux home filesystem instead of slow `/mnt/c` storage.

## Build

From the repository root:

```bash
bash apptainer_build/build.sh
```

The default build is incremental. It always recompiles Java and syncs current
source files into `apptainer/stage/onebuilder`, but it reuses the expensive
staged Pixi runtime and staged JRE when they already exist. This is the normal
command for most oneBuilder GUI/script edits.

The build script writes by default to:

```text
apptainer/stage/
apptainer/onebuilder-0.0.1-linux64.sif
```

Override the local workspace if needed:

```bash
ONEBUILDER_APPTAINER_DIR=/home/dell/project/oneBuilder_apptainer bash apptainer_build/build.sh
```

If running the script outside the repository root, set the source repository explicitly:

```bash
ONEBUILDER_REPO_ROOT=/mnt/c/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-oneBuilder \
  bash /path/to/apptainer_build/build.sh
```

The build script:

- Compiles Java classes with the local Java 25 JDK.
- Reuses the staged JRE by default, or creates a JRE-only Java runtime with `jlink` when requested.
- Syncs `phylotree_builder_v0.0.1/` into the local Apptainer workspace stage.
- Reuses the staged `.pixi/envs/default/` runtime by default, or refreshes it when requested.
- Removes any `pixi` executable from the staged runtime because the image runs binaries directly.
- Rewrites text references from host software prefixes to `/opt/onebuilder/runtime` only when the runtime is refreshed, unless explicitly requested.
- Builds `onebuilder-0.0.1-linux64.sif` in the local Apptainer workspace.

## Incremental vs Full Builds

Most development changes are Java, zsh, Python, docs, or bundled GUI resources.
For those, use the default incremental build:

```bash
bash apptainer_build/build.sh
```

This skips the slowest local staging work:

- does not recopy the 3GB Pixi runtime if `apptainer/stage/onebuilder/runtime` already exists
- does not rerun `jlink` if `apptainer/stage/jre25/bin/java` already exists
- does not rescan tens of thousands of runtime files for prefix rewriting

Use this when you changed files such as:

```text
phylotree_builder_v0.0.1/java_tanglegram/**/*.java
phylotree_builder_v0.0.1/*.zsh
phylotree_builder_v0.0.1/*.py
phylotree_builder_v0.0.1/help_utils.py
phylotree_builder_v0.0.1/cal_pair_wise_tree_dist.R
```

Refresh the Pixi runtime only when dependencies or runtime binaries changed:

```bash
REFRESH_RUNTIME=1 bash apptainer_build/build.sh
```

Use this after changing or rebuilding:

```text
phylotree_builder_v0.0.1/.pixi/envs/default/
phylotree_builder_v0.0.1/pixi.toml
phylotree_builder_v0.0.1/pixi.lock
```

Refresh the staged JRE only when the Java installation or desired `jlink`
modules changed:

```bash
REFRESH_JRE=1 bash apptainer_build/build.sh
```

Force prefix rewriting without recopying the runtime:

```bash
FORCE_PREFIX_REWRITE=1 bash apptainer_build/build.sh
```

By default prefix rewriting scans only the staged runtime. To reproduce the old
broader scan, use:

```bash
FORCE_PREFIX_REWRITE=1 PREFIX_REWRITE_SCOPE=all bash apptainer_build/build.sh
```

Do a full clean rebuild when you need to discard all staged cache:

```bash
FULL_REBUILD=1 bash apptainer_build/build.sh
```

`FULL_REBUILD=1` removes `apptainer/stage/`, recopies the Pixi runtime, rewrites
runtime prefixes, recreates the JRE, and then builds the image.

Chinese quick reference:

- 平时只改 Java/zsh/Python：`bash apptainer_build/build.sh`
- 改了 Pixi runtime 或依赖：`REFRESH_RUNTIME=1 bash apptainer_build/build.sh`
- 改了 Java JDK/JRE 打包模块：`REFRESH_JRE=1 bash apptainer_build/build.sh`
- 只想重跑路径替换：`FORCE_PREFIX_REWRITE=1 bash apptainer_build/build.sh`
- 完全清空 stage 后重来：`FULL_REBUILD=1 bash apptainer_build/build.sh`

## Runtime Commands

```bash
apptainer exec apptainer/onebuilder-0.0.1-linux64.sif onebuilder-run-config --help
apptainer exec apptainer/onebuilder-0.0.1-linux64.sif onebuilder-align --help
apptainer exec apptainer/onebuilder-0.0.1-linux64.sif onebuilder-trim --help
apptainer exec apptainer/onebuilder-0.0.1-linux64.sif onebuilder-protein --help
apptainer exec apptainer/onebuilder-0.0.1-linux64.sif onebuilder-dna --help
```

GUI examples:

```bash
apptainer exec --bind "$PWD:/work" apptainer/onebuilder-0.0.1-linux64.sif \
  java -cp "/opt/onebuilder/java_tanglegram:/opt/onebuilder/lib/*" onebuilder.launcher

apptainer exec --bind "$PWD:/work" apptainer/onebuilder-0.0.1-linux64.sif \
  java -cp "/opt/onebuilder/java_tanglegram:/opt/onebuilder/lib/*" tanglegram.launcher -dir /work/test1/tree_summary
```

The image sets `FONTCONFIG_FILE` and `FONTCONFIG_PATH` so Java and fontconfig can find the system font configuration.
