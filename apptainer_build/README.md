# oneBuilder Apptainer Image

This directory contains the tracked Apptainer build recipe for oneBuilder.

The large local build workspace is intentionally separate from this tracked recipe directory. In this workspace, the repository path `apptainer` is a local symlink to `/home/dell/project/oneBuilder_apptainer`, so the large `stage/` directory and final `.sif` image stay on the Linux home filesystem instead of slow `/mnt/c` storage.

## Build

From the repository root:

```bash
bash apptainer_build/build.sh
```

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
- Creates a JRE-only Java runtime with `jlink`.
- Copies `phylotree_builder_v0.0.1/` into the local Apptainer workspace stage.
- Copies the verified local `.pixi/envs/default/` software snapshot into `/opt/onebuilder/runtime`.
- Removes any `pixi` executable from the staged runtime because the image runs binaries directly.
- Rewrites text references from host software prefixes to `/opt/onebuilder/runtime`.
- Builds `onebuilder-0.0.1-linux64.sif` in the local Apptainer workspace.

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
