#!/bin/zsh

set -euo pipefail

# 检查是否提供了输入文件
if [[ $# -ne 1 ]]; then
    echo "用法: zsh $0 <input.fasta>"
    exit 1
fi

# 获取输入文件路径
input="$1"

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

"$pixi_exe" run --manifest-path "$script_dir" mafft --reorder --localpair --maxiterate 1000 "$input" > "$output"

echo "比对完成！输出文件: $output"
