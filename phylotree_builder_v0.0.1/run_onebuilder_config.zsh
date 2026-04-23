#!/bin/zsh

set -euo pipefail

script_dir="${0:a:h}"
pixi_exe="${PIXI_EXE:-/home/dell/.pixi/bin/pixi}"
json_python_cmd=(python3.13)
force_overwrite=0

if ! command -v python3.13 >/dev/null 2>&1; then
    if [[ -x "$pixi_exe" ]]; then
        json_python_cmd=("$pixi_exe" run --manifest-path "$script_dir" python3.13)
    else
        echo "Error: python3.13 is not available and pixi was not found at '$pixi_exe'."
        exit 1
    fi
fi

usage() {
    echo "Usage: zsh $0 [--force-overwrite] [--config runtime.json] <runtime.json>"
    echo "  or:  zsh $0 [--force-overwrite] --config <runtime.json>"
    echo ""
    echo "The config file must be a JSON exported by onebuilder.launcher."
    echo "The script reads run.input_type, run.input_file, run.output_base_dir,"
    echo "run.output_prefix, and alignment.run_alignment_first from that JSON."
    echo "If the pipeline output directory already exists, the called build wrapper asks whether to overwrite it unless --force-overwrite is supplied."
}

aligned_output_path() {
    local input_file="$1"
    if [[ "$input_file" == *.* ]]; then
        local base="${input_file%.*}"
        local ext="${input_file##*.}"
        echo "${base}.aligned.${ext}"
    else
        echo "${input_file}.aligned"
    fi
}

config_path=""
positionals=()

while [[ $# -gt 0 ]]; do
    case "$1" in
        --config)
            [[ $# -ge 2 ]] || { usage; exit 1; }
            config_path="$2"
            shift 2
            ;;
        --force-overwrite)
            force_overwrite=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        -*)
            echo "Error: unknown option '$1'"
            usage
            exit 1
            ;;
        *)
            positionals+=("$1")
            shift
            ;;
    esac
done

if [[ -n "$config_path" && ${#positionals[@]} -gt 0 ]]; then
    echo "Error: provide the config JSON either as a positional argument or with --config, not both."
    usage
    exit 1
fi

if [[ -z "$config_path" && ${#positionals[@]} -eq 1 ]]; then
    config_path="${positionals[1]}"
fi

if [[ -z "$config_path" || ${#positionals[@]} -gt 1 ]]; then
    usage
    exit 1
fi

config_path="${config_path:A}"

if [[ ! -f "$config_path" ]]; then
    echo "Error: config file '$config_path' does not exist."
    exit 1
fi

config_values=("${(@f)$(
    "${json_python_cmd[@]}" -c '
import json
import sys
from pathlib import Path

config_path = Path(sys.argv[1]).expanduser().resolve()
with config_path.open(encoding="utf-8") as handle:
    payload = json.load(handle)

run = payload.get("run", {}) if isinstance(payload, dict) else {}
alignment = payload.get("alignment", {}) if isinstance(payload, dict) else {}

def resolve_path(value: object) -> str:
    if value is None:
        return ""
    text = str(value).strip()
    if not text:
        return ""
    candidate = Path(text).expanduser()
    if not candidate.is_absolute():
        candidate = (config_path.parent / candidate).resolve()
    else:
        candidate = candidate.resolve()
    return str(candidate)

input_type = str(run.get("input_type", "")).strip().upper()
if input_type in {"DNA", "DNA/CDS", "DNA-CDS"}:
    input_type = "DNA_CDS"

print(input_type)
print(resolve_path(run.get("input_file")))
print(resolve_path(run.get("output_base_dir")))
print(str(run.get("output_prefix", "")).strip())
print("1" if bool(alignment.get("run_alignment_first", False)) else "0")
' "$config_path"
)}")

input_type="${config_values[1]:-}"
input_file="${config_values[2]:-}"
output_base_dir="${config_values[3]:-}"
output_prefix="${config_values[4]:-}"
run_alignment_first="${config_values[5]:-0}"

if [[ "$input_type" != "PROTEIN" && "$input_type" != "DNA_CDS" ]]; then
    echo "Error: unsupported run.input_type '$input_type' in '$config_path'. Expected PROTEIN or DNA_CDS."
    exit 1
fi

if [[ -z "$input_file" ]]; then
    echo "Error: run.input_file is missing in '$config_path'."
    exit 1
fi

if [[ ! -f "$input_file" ]]; then
    echo "Error: input file '$input_file' from '$config_path' does not exist."
    exit 1
fi

if [[ -z "$output_base_dir" ]]; then
    echo "Error: run.output_base_dir is missing in '$config_path'."
    exit 1
fi

if [[ -z "$output_prefix" ]]; then
    echo "Error: run.output_prefix is missing in '$config_path'."
    exit 1
fi

mkdir -p "$output_base_dir"

align_script="$script_dir/s1_quick_align.zsh"
protein_script="$script_dir/s2_phylo_4prot.zsh"
dna_script="$script_dir/s2_phylo_4dna.zsh"

build_script="$protein_script"
if [[ "$input_type" == "DNA_CDS" ]]; then
    build_script="$dna_script"
fi

if [[ "$run_alignment_first" -eq 1 && ! -f "$align_script" ]]; then
    echo "Error: alignment script '$align_script' does not exist."
    exit 1
fi

if [[ ! -f "$build_script" ]]; then
    echo "Error: build script '$build_script' does not exist."
    exit 1
fi

effective_input_file="$input_file"
if [[ "$run_alignment_first" -eq 1 ]]; then
    effective_input_file="$(aligned_output_path "$input_file")"
fi

pipeline_output_prefix="${output_base_dir}/${output_prefix}"

echo "Running oneBuilder config: $config_path"
echo "  input type: $input_type"
echo "  input file: $input_file"
echo "  run alignment first: $([[ "$run_alignment_first" -eq 1 ]] && echo yes || echo no)"
echo "  effective build input: $effective_input_file"
echo "  output prefix: $pipeline_output_prefix"

if [[ "$run_alignment_first" -eq 1 ]]; then
    echo "Step 1/2: running MAFFT wrapper..."
    zsh "$align_script" --config "$config_path" "$input_file"
fi

echo "Step $([[ "$run_alignment_first" -eq 1 ]] && echo "2/2" || echo "1/1"): running phylogeny wrapper..."
build_cmd=(zsh "$build_script")
if [[ "$force_overwrite" -eq 1 ]]; then
    build_cmd+=(--force-overwrite)
fi
build_cmd+=(--config "$config_path" "$effective_input_file" "$pipeline_output_prefix")
"${build_cmd[@]}"

echo "Completed oneBuilder config run."
