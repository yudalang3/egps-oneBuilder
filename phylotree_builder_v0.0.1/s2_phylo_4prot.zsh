#!/bin/zsh

set -euo pipefail

# Get the absolute path of the directory containing this script
script_dir="${0:a:h}"
pixi_exe="${PIXI_EXE:-/home/dell/.pixi/bin/pixi}"
pixi_lib_dir="$script_dir/.pixi/envs/default/lib"
config_path=""
force_overwrite=0

if [[ ! -x "$pixi_exe" ]]; then
    echo "Error: pixi executable not found. Set PIXI_EXE or install pixi at /home/dell/.pixi/bin/pixi."
    exit 1
fi

usage() {
    echo "Usage: zsh $0 [--force-overwrite] [--config runtime.json] <input.fasta> [output_prefix]"
    echo "  - If only input.fasta is provided, the output prefix is automatically set to '<input>_tree' (extension removed)."
    echo "  - If the output path already exists, the script asks whether to overwrite it unless --force-overwrite is supplied."
    echo "  - Examples:"
    echo "      zsh $0 wnt_homo.fa        → output prefix: wnt_homo_tree"
    echo "      zsh $0 wnt_homo.fa mytree → output prefix: mytree"
}

confirm_overwrite_if_needed() {
    local output_path="$1"
    if [[ ! -e "$output_path" ]]; then
        return 0
    fi
    if [[ "$force_overwrite" -eq 1 ]]; then
        rm -rf -- "$output_path"
        return 0
    fi
    if [[ ! -t 0 ]]; then
        echo "Error: output path '$output_path' already exists. Re-run with --force-overwrite to replace it."
        exit 1
    fi

    local reply
    printf "Output path '%s' already exists. Overwrite it? [y/N]: " "$output_path"
    read -r reply
    case "$reply" in
        [Yy]|[Yy][Ee][Ss])
            rm -rf -- "$output_path"
            ;;
        *)
            echo "Cancelled. Existing output was left unchanged."
            exit 1
            ;;
    esac
}

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

if [[ ${#positionals[@]} -lt 1 || ${#positionals[@]} -gt 2 ]]; then
    usage
    exit 1
fi

input="${positionals[1]}"

# Check if input file exists
if [[ ! -f "$input" ]]; then
    echo "Error: Input file '$input' does not exist."
    exit 1
fi

# Determine output prefix
if [[ ${#positionals[@]} -eq 2 ]]; then
    output="${positionals[2]}"
else
    # Auto mode: remove extension and append '_tree'
    if [[ "$input" == *.* ]]; then
        base="${input%.*}"
    else
        base="$input"
    fi
    output="${base}_tree"
fi

confirm_overwrite_if_needed "$output"

pipeline_cmd=("$pixi_exe" run --manifest-path "$script_dir" python3.13 "$script_dir/phylo_pipeline_4prot.py")
if [[ -n "$config_path" ]]; then
    pipeline_cmd+=(--config "$config_path")
fi
pipeline_cmd+=("$input" -o "$output")

LD_LIBRARY_PATH="$pixi_lib_dir${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}" "${pipeline_cmd[@]}"

echo "Pipeline completed! Output prefix: $output"
