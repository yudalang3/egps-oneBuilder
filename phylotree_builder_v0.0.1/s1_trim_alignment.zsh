#!/bin/zsh

set -euo pipefail

script_dir="${0:a:h}"
runtime_env="${ONEBUILDER_RUNTIME_ENV:-$script_dir/runtime}"
if [[ ! -d "$runtime_env" ]]; then
    runtime_env="$script_dir/.pixi/envs/default"
fi
if [[ -d "$runtime_env/bin" ]]; then
    export PATH="$runtime_env/bin:$PATH"
fi
if [[ -d "/opt/bioinfor/trimAI/bin" ]]; then
    export PATH="/opt/bioinfor/trimAI/bin:$PATH"
fi

pixi_exe="${PIXI_EXE:-pixi}"
json_python_cmd=(python3.13)

if ! command -v python3.13 >/dev/null 2>&1; then
    if command -v "$pixi_exe" >/dev/null 2>&1; then
        json_python_cmd=("$pixi_exe" run --manifest-path "$script_dir" python3.13)
    else
        echo "Error: python3.13 is not available. Activate the runtime environment or set ONEBUILDER_RUNTIME_ENV."
        exit 1
    fi
fi

usage() {
    echo "Usage: zsh $0 [--config runtime.json] [trimAl options...] <input.fasta>"
    echo "The output file is written beside the input with .trim inserted before the extension."
}

trim_args=()
config_path=""
input=""
positionals=()

while [[ $# -gt 0 ]]; do
    case "$1" in
        --config)
            [[ $# -ge 2 ]] || { usage; exit 1; }
            config_path="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            positionals+=("$1")
            shift
            ;;
    esac
done

if [[ "${#positionals[@]}" -lt 1 ]]; then
    usage
    exit 1
fi
input="${positionals[-1]}"
if [[ "${#positionals[@]}" -gt 1 ]]; then
    trim_args=()
    for ((i = 1; i < ${#positionals[@]}; i++)); do
        trim_args+=("${positionals[$i]}")
    done
fi

if [[ -n "$config_path" ]]; then
    if [[ ! -f "$config_path" ]]; then
        echo "Error: config file '$config_path' does not exist."
        exit 1
    fi

    config_values=("${(@f)$(
        "${json_python_cmd[@]}" -c '
import json, sys
with open(sys.argv[1], encoding="utf-8") as handle:
    payload = json.load(handle)
trim = payload.get("trim_alignment", {}) if isinstance(payload, dict) else {}
enabled = bool(trim.get("enabled", False)) if isinstance(trim, dict) else False
trimal = trim.get("trimal", {}) if isinstance(trim, dict) else {}
common = trimal.get("common", {}) if isinstance(trimal, dict) else {}
args = common.get("args", []) if isinstance(common, dict) else []
print("1" if enabled else "0")
for arg in args:
    print(str(arg))
' "$config_path"
    )}")

    if [[ "${config_values[1]:-0}" != "1" ]]; then
        echo "Error: trim_alignment.enabled is false in '$config_path'."
        exit 1
    fi
    trim_args=()
    if [[ "${#config_values[@]}" -gt 1 ]]; then
        for ((i = 2; i <= ${#config_values[@]}; i++)); do
            trim_args+=("${config_values[$i]}")
        done
    fi
fi

if [[ "${#trim_args[@]}" -eq 0 ]]; then
    echo "Error: no trimAl arguments were provided."
    exit 1
fi

for arg in "${trim_args[@]}"; do
    if [[ "$arg" == "-in" || "$arg" == "-out" ]]; then
        echo "Error: do not pass -in or -out; this wrapper supplies input and output paths."
        exit 1
    fi
done

if [[ ! -f "$input" ]]; then
    echo "Error: input file '$input' does not exist."
    exit 1
fi

if [[ "$input" == *.* ]]; then
    base="${input%.*}"
    ext="${input##*.}"
    output="${base}.trim.${ext}"
else
    output="${input}.trim"
fi

trimal_exe="${TRIMAL_EXE:-}"
if [[ -z "$trimal_exe" ]]; then
    if [[ -x "/opt/bioinfor/trimAI/bin/trimal" ]]; then
        trimal_exe="/opt/bioinfor/trimAI/bin/trimal"
    elif command -v trimal >/dev/null 2>&1; then
        trimal_exe="$(command -v trimal)"
    else
        echo "Error: trimal was not found. Set TRIMAL_EXE or install trimAI at /opt/bioinfor/trimAI."
        exit 1
    fi
fi

"$trimal_exe" -in "$input" -out "$output" "${trim_args[@]}" > "${output}.trimal.log" 2>&1

echo "Trim alignment completed. Output file: $output"
