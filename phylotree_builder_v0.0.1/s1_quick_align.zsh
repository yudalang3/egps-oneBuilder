#!/bin/zsh

set -euo pipefail

usage() {
    echo "用法: zsh $0 [--strategy localpair|auto|globalpair] [--maxiterate N] [--reorder|--no-reorder] <input.fasta>"
}

strategy="localpair"
maxiterate="1000"
reorder_enabled=1
input=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --strategy)
            [[ $# -ge 2 ]] || { usage; exit 1; }
            strategy="$2"
            shift 2
            ;;
        --maxiterate)
            [[ $# -ge 2 ]] || { usage; exit 1; }
            maxiterate="$2"
            shift 2
            ;;
        --reorder)
            reorder_enabled=1
            shift
            ;;
        --no-reorder)
            reorder_enabled=0
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        -*)
            echo "错误: 未知参数 '$1'"
            usage
            exit 1
            ;;
        *)
            if [[ -n "$input" ]]; then
                echo "错误: 只能提供一个输入文件。"
                usage
                exit 1
            fi
            input="$1"
            shift
            ;;
    esac
done

if [[ -z "$input" ]]; then
    usage
    exit 1
fi

# 检查输入文件是否存在
if [[ ! -f "$input" ]]; then
    echo "错误: 文件 '$input' 不存在。"
    exit 1
fi

# 自动构造输出文件名：在扩展名前插入 ".aligned"
# 例如：all_wnt_gents_in_human_outgroup.fa -> all_wnt_gents_in_human_outgroup.aligned.fa
if [[ "$input" == *.* ]]; then
    base="${input%.*}"
    ext="${input##*.}"
    output="${base}.aligned.${ext}"
else
    # 如果没有扩展名，直接加 .aligned
    output="${input}.aligned"
fi

# 执行 mafft 命令
# 获取脚本所在目录的绝对路径
script_dir="${0:a:h}"
pixi_exe="${PIXI_EXE:-/home/dell/.pixi/bin/pixi}"

if [[ ! -x "$pixi_exe" ]]; then
    echo "错误: 找不到 pixi。请设置 PIXI_EXE，或安装到 /home/dell/.pixi/bin/pixi。"
    exit 1
fi

mafft_cmd=("$pixi_exe" run --manifest-path "$script_dir" mafft)
if [[ "$reorder_enabled" -eq 1 ]]; then
    mafft_cmd+=(--reorder)
fi
mafft_cmd+=("--$strategy" --maxiterate "$maxiterate" "$input")

"${mafft_cmd[@]}" > "$output"

echo "比对完成！输出文件: $output"
