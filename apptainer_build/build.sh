#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
if [[ -n "${ONEBUILDER_REPO_ROOT:-}" ]]; then
  repo_root="$(cd "$ONEBUILDER_REPO_ROOT" && pwd -P)"
elif [[ -d "$PWD/phylotree_builder_v0.0.1" ]]; then
  repo_root="$(pwd -P)"
elif [[ -d "$script_dir/../phylotree_builder_v0.0.1" ]]; then
  repo_root="$(cd "$script_dir/.." && pwd -P)"
else
  echo "Error: could not locate repository root. Run this script from the repo root or set ONEBUILDER_REPO_ROOT." >&2
  exit 1
fi

source_dir="$repo_root/phylotree_builder_v0.0.1"
build_workspace="${ONEBUILDER_APPTAINER_DIR:-$repo_root/apptainer}"
stage_dir="$build_workspace/stage"
stage_app="$stage_dir/onebuilder"
stage_jre="$stage_dir/jre25"
image_name="${IMAGE_NAME:-onebuilder-0.0.1-linux64.sif}"
image_path="$build_workspace/$image_name"
java_home="${JAVA_HOME:-}"
full_rebuild="${FULL_REBUILD:-0}"
clean_stage="${CLEAN_STAGE:-$full_rebuild}"
refresh_runtime="${REFRESH_RUNTIME:-$clean_stage}"
refresh_jre="${REFRESH_JRE:-$clean_stage}"
force_prefix_rewrite="${FORCE_PREFIX_REWRITE:-$refresh_runtime}"
prefix_rewrite_scope="${PREFIX_REWRITE_SCOPE:-runtime}"

if [[ ! -d "$source_dir" ]]; then
  echo "Error: source directory not found: $source_dir" >&2
  exit 1
fi

source_runtime="$source_dir/.pixi/envs/default"

if [[ ! -d "$source_runtime" ]]; then
  echo "Error: local runtime software snapshot not found: $source_runtime" >&2
  exit 1
fi

if [[ ! -d "$source_dir/lib" ]]; then
  echo "Error: local Java lib directory not found: $source_dir/lib" >&2
  exit 1
fi

if [[ ! -x "$source_dir/third_party/mad/mad" ]]; then
  echo "Error: MAD executable not found: $source_dir/third_party/mad/mad" >&2
  exit 1
fi

if [[ ! -x "/opt/bioinfor/trimAI/bin/trimal" ]]; then
  echo "Error: trimAl executable not found: /opt/bioinfor/trimAI/bin/trimal" >&2
  exit 1
fi

if [[ -z "$java_home" ]]; then
  java_bin="$(command -v java || true)"
  if [[ -z "$java_bin" ]]; then
    echo "Error: java was not found on PATH." >&2
    exit 1
  fi
  java_home="$(cd "$(dirname "$java_bin")/.." && pwd)"
fi

javac_exe="${JAVAC_EXE:-$java_home/bin/javac}"
jlink_exe="${JLINK_EXE:-$java_home/bin/jlink}"

if [[ ! -x "$javac_exe" ]]; then
  echo "Error: javac not found: $javac_exe" >&2
  exit 1
fi

if [[ ! -x "$jlink_exe" ]]; then
  echo "Error: jlink not found: $jlink_exe" >&2
  exit 1
fi

echo "Compiling Java classes with $javac_exe"
(
  cd "$source_dir"
  "$javac_exe" -cp "lib/*:java_tanglegram" -d java_tanglegram \
    java_tanglegram/onebuilder/*.java \
    java_tanglegram/tanglegram/*.java \
    java_tanglegram/tests/OneBuilderStandaloneTest.java \
    java_tanglegram/tests/TanglegramStandaloneTest.java
)

echo "Preparing stage directory: $stage_dir"
if [[ "$clean_stage" == "1" ]]; then
  echo "FULL_REBUILD/CLEAN_STAGE requested; removing staged workspace."
  rm -rf "$stage_dir"
fi
mkdir -p "$stage_app" "$stage_jre"

echo "Syncing application files into stage"
rsync -a \
  --delete \
  --exclude='.pixi/' \
  --exclude='runtime/' \
  --exclude='__pycache__/' \
  --exclude='*.pyc' \
  --exclude='*.log' \
  --exclude='test*/' \
  --exclude='tests/tmp*/' \
  --exclude='third_party/mad2.2.zip' \
  "$source_dir/" "$stage_app/"

mkdir -p "$stage_app/runtime"
if [[ "$refresh_runtime" != "1" && -d "$stage_app/runtime/bin" ]]; then
  echo "Reusing staged runtime snapshot: $stage_app/runtime"
else
  echo "Refreshing staged runtime snapshot from $source_runtime"
  rm -rf "$stage_app/runtime"
  mkdir -p "$stage_app/runtime"
  rsync -a "$source_runtime/" "$stage_app/runtime/"
fi

rm -f "$stage_app/runtime/bin/pixi" "$stage_app/runtime/bin/pixi-global" 2>/dev/null || true

if [[ "$force_prefix_rewrite" == "1" ]]; then
  new_prefix="/opt/onebuilder/runtime"
  old_prefixes=(
    "$source_runtime"
    "/opt/BioInfo/phylotree_builder_v0.0.1/.pixi/envs/default"
    "/opt/onebuilder/.pixi/envs/default"
  )
  if [[ "$prefix_rewrite_scope" == "all" ]]; then
    prefix_rewrite_root="$stage_app"
  else
    prefix_rewrite_root="$stage_app/runtime"
  fi
  echo "Rewriting text prefixes under $prefix_rewrite_root"
  while IFS= read -r -d '' file_path; do
    if grep -Iq . "$file_path"; then
      for old_prefix in "${old_prefixes[@]}"; do
        OLD_PREFIX="$old_prefix" NEW_PREFIX="$new_prefix" perl -0pi -e 's/\Q$ENV{OLD_PREFIX}\E/$ENV{NEW_PREFIX}/g' "$file_path"
      done
    fi
  done < <(find "$prefix_rewrite_root" -type f -size -10M -print0)
else
  echo "Reusing existing staged prefix rewrite; set FORCE_PREFIX_REWRITE=1 to rescan text files."
fi

echo "Removing broken symlinks from staged runtime"
find "$stage_app" -xtype l -print -delete

if [[ "$refresh_jre" != "1" && -x "$stage_jre/bin/java" ]]; then
  echo "Reusing staged Java runtime: $stage_jre"
else
  echo "Creating Java 25 runtime with jlink"
  rm -rf "$stage_jre"
  "$jlink_exe" \
    --add-modules java.se,jdk.crypto.ec,jdk.localedata \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --output "$stage_jre"
fi

build_runner="${APPTAINER_EXE:-apptainer}"
if ! command -v "$build_runner" >/dev/null 2>&1; then
  echo "Error: apptainer executable not found. Set APPTAINER_EXE if needed." >&2
  exit 1
fi

build_flags=()
if [[ -n "${APPTAINER_BUILD_FLAGS:-}" ]]; then
  read -r -a build_flags <<<"$APPTAINER_BUILD_FLAGS"
else
  build_flags=(--fakeroot)
fi

mkdir -p "$build_workspace"
echo "Building image: $image_path"
rm -f "$image_path"
(
  cd "$build_workspace"
  "$build_runner" build "${build_flags[@]}" "$image_path" "$script_dir/onebuilder.def"
)

echo "Built image: $image_path"
