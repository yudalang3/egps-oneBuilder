#!/bin/zsh

set -euo pipefail

script_dir="${0:a:h}"
pixi_exe="${PIXI_EXE:-/home/dell/.pixi/bin/pixi}"
pixi_lib_dir="$script_dir/.pixi/envs/default/lib"
config_path=""

if [[ ! -x "$pixi_exe" ]]; then
    echo "Error: pixi executable not found. Set PIXI_EXE or install pixi at /home/dell/.pixi/bin/pixi."
    exit 1
fi

usage() {
    echo "Usage: zsh $0 [--config runtime.json] <input.fasta> [output_prefix]"
    echo "  - If only input.fasta is provided, the output prefix is automatically set to '<input>_tree' (extension removed)."
    echo "  - Examples:"
    echo "      zsh $0 cds_aligned.fasta        -> output prefix: cds_aligned_tree"
    echo "      zsh $0 cds_aligned.fasta mytree -> output prefix: mytree"
}

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

if [[ ! -f "$input" ]]; then
    echo "Error: Input file '$input' does not exist."
    exit 1
fi

if [[ ${#positionals[@]} -eq 2 ]]; then
    output="${positionals[2]}"
else
    if [[ "$input" == *.* ]]; then
        base="${input%.*}"
    else
        base="$input"
    fi
    output="${base}_tree"
fi

pipeline_cmd=("$pixi_exe" run --manifest-path "$script_dir" python3.13 "$script_dir/phylo_pipeline_4dna.py")
if [[ -n "$config_path" ]]; then
    pipeline_cmd+=(--config "$config_path")
fi
pipeline_cmd+=("$input" -o "$output")

LD_LIBRARY_PATH="$pixi_lib_dir${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}" "${pipeline_cmd[@]}"

echo "Pipeline completed! Output prefix: $output"
