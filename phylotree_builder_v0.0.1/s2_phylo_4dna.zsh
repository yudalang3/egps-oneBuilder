#!/bin/zsh

set -euo pipefail

script_dir="${0:a:h}"
pixi_exe="${PIXI_EXE:-/home/dell/.pixi/bin/pixi}"
pixi_lib_dir="$script_dir/.pixi/envs/default/lib"

if [[ ! -x "$pixi_exe" ]]; then
    echo "Error: pixi executable not found. Set PIXI_EXE or install pixi at /home/dell/.pixi/bin/pixi."
    exit 1
fi

if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo "Usage: zsh $0 <input.fasta> [output_prefix]"
    echo "  - If only input.fasta is provided, the output prefix is automatically set to '<input>_tree' (extension removed)."
    echo "  - Examples:"
    echo "      zsh $0 cds_aligned.fasta        -> output prefix: cds_aligned_tree"
    echo "      zsh $0 cds_aligned.fasta mytree -> output prefix: mytree"
    exit 1
fi

input="$1"

if [[ ! -f "$input" ]]; then
    echo "Error: Input file '$input' does not exist."
    exit 1
fi

if [[ $# -eq 2 ]]; then
    output="$2"
else
    if [[ "$input" == *.* ]]; then
        base="${input%.*}"
    else
        base="$input"
    fi
    output="${base}_tree"
fi

LD_LIBRARY_PATH="$pixi_lib_dir${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}" \
    "$pixi_exe" run --manifest-path "$script_dir" python3.13 "$script_dir/phylo_pipeline_4dna.py" "$input" -o "$output"

echo "Pipeline completed! Output prefix: $output"
