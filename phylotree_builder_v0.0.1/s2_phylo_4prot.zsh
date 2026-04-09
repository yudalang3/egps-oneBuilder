#!/bin/zsh

# Get the absolute path of the directory containing this script
script_dir="${0:a:h}"

# Check number of arguments: accept 1 or 2
if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo "Usage: zsh $0 <input.fasta> [output_prefix]"
    echo "  - If only input.fasta is provided, the output prefix is automatically set to '<input>_tree' (extension removed)."
    echo "  - Examples:"
    echo "      zsh $0 wnt_homo.fa        → output prefix: wnt_homo_tree"
    echo "      zsh $0 wnt_homo.fa mytree → output prefix: mytree"
    exit 1
fi

input="$1"

# Check if input file exists
if [[ ! -f "$input" ]]; then
    echo "Error: Input file '$input' does not exist."
    exit 1
fi

# Determine output prefix
if [[ $# -eq 2 ]]; then
    output="$2"
else
    # Auto mode: remove extension and append '_tree'
    if [[ "$input" == *.* ]]; then
        base="${input%.*}"
    else
        base="$input"
    fi
    output="${base}_tree"
fi

# Run the phylogenetic pipeline via pixi
pixi run --manifest-path "$script_dir" python "$script_dir/phylo_pipeline_4prot.py" "$input" -o "$output"

echo "Pipeline completed! Output prefix: $output"