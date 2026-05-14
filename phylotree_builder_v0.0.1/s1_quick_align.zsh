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
pixi_exe="${PIXI_EXE:-pixi}"
json_python_cmd=(python3.13)

if ! command -v python3.13 >/dev/null 2>&1; then
    if command -v "$pixi_exe" >/dev/null 2>&1; then
        json_python_cmd=("$pixi_exe" run --manifest-path "$script_dir" python3.13)
    else
        echo "错误: 找不到 python3.13。请激活运行环境，或设置 ONEBUILDER_RUNTIME_ENV。"
        exit 1
    fi
fi

usage() {
    echo "用法: zsh $0 [--config runtime.json] [--strategy localpair|auto|globalpair] [--maxiterate N] [--reorder|--no-reorder] <input.fasta>"
}

strategy="localpair"
maxiterate="1000"
thread_count=""
reorder_enabled=1
config_path=""
strategy_from_cli=0
maxiterate_from_cli=0
reorder_from_cli=0
mafft_extra_args=()
input=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --config)
            [[ $# -ge 2 ]] || { usage; exit 1; }
            config_path="$2"
            shift 2
            ;;
        --strategy)
            [[ $# -ge 2 ]] || { usage; exit 1; }
            strategy="$2"
            strategy_from_cli=1
            shift 2
            ;;
        --maxiterate)
            [[ $# -ge 2 ]] || { usage; exit 1; }
            maxiterate="$2"
            maxiterate_from_cli=1
            shift 2
            ;;
        --reorder)
            reorder_enabled=1
            reorder_from_cli=1
            shift
            ;;
        --no-reorder)
            reorder_enabled=0
            reorder_from_cli=1
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

if [[ -n "$config_path" ]]; then
    if [[ ! -f "$config_path" ]]; then
        echo "错误: 配置文件 '$config_path' 不存在。"
        exit 1
    fi

    config_values=("${(@f)$(
        "${json_python_cmd[@]}" -c '
import json, sys
with open(sys.argv[1], encoding="utf-8") as handle:
    payload = json.load(handle)
alignment = payload.get("alignment", {})
mafft = alignment.get("mafft", {}) if isinstance(alignment, dict) else {}
common = mafft.get("common", {}) if isinstance(mafft, dict) else {}
extra_args = mafft.get("extra_args", []) if isinstance(mafft, dict) else []
strategy = common.get("strategy", alignment.get("strategy", "localpair"))
maxiterate = common.get("maxiterate", alignment.get("maxiterate", 1000))
reorder = common.get("reorder", alignment.get("reorder", True))
threads = common.get("threads")
print(strategy)
print(maxiterate)
print("" if threads is None else threads)
print("1" if reorder else "0")
for arg in extra_args:
    print(arg)
' "$config_path"
    )}")

    if [[ "$strategy_from_cli" -eq 0 && "${#config_values[@]}" -ge 1 && -n "${config_values[1]}" ]]; then
        strategy="${config_values[1]}"
    fi
    if [[ "$maxiterate_from_cli" -eq 0 && "${#config_values[@]}" -ge 2 && -n "${config_values[2]}" ]]; then
        maxiterate="${config_values[2]}"
    fi
    if [[ "${#config_values[@]}" -ge 3 && -n "${config_values[3]}" ]]; then
        thread_count="${config_values[3]}"
    fi
    if [[ "$reorder_from_cli" -eq 0 && "${#config_values[@]}" -ge 4 && -n "${config_values[4]}" ]]; then
        reorder_enabled="${config_values[4]}"
    fi
    if [[ "${#config_values[@]}" -gt 4 ]]; then
        mafft_extra_args=()
        for ((i = 5; i <= ${#config_values[@]}; i++)); do
            mafft_extra_args+=("${config_values[$i]}")
        done
    fi
fi

# 检查输入文件是否存在
if [[ ! -f "$input" ]]; then
    echo "错误: 文件 '$input' 不存在。"
    exit 1
fi

# 自动构造输出文件名：在扩展名前插入 ".aligned"
# 例如：all_wnt_gents_in_human_outgroup.fa -> all_wnt_gents_in_human_outgroup.aligned.fa
input_dir="${input:h}"
input_name="${input:t}"
if [[ "$input_name" == *.* ]]; then
    base="${input_name%.*}"
    ext="${input_name##*.}"
    output_name="${base}.aligned.${ext}"
else
    # 如果没有扩展名，直接加 .aligned
    output_name="${input_name}.aligned"
fi
if [[ "$input_dir" == "." ]]; then
    output="$output_name"
else
    output="${input_dir}/${output_name}"
fi

# 执行 mafft 命令
if ! command -v mafft >/dev/null 2>&1 && ! command -v "$pixi_exe" >/dev/null 2>&1; then
    echo "错误: 找不到 mafft。请激活运行环境，或设置 ONEBUILDER_RUNTIME_ENV。"
    exit 1
fi

mafft_binaries_dir="$runtime_env/libexec/mafft"
if [[ -d "$mafft_binaries_dir" ]]; then
    export MAFFT_BINARIES="$mafft_binaries_dir"
else
    unset MAFFT_BINARIES
fi

if command -v mafft >/dev/null 2>&1; then
    mafft_cmd=(mafft)
else
    mafft_cmd=("$pixi_exe" run --manifest-path "$script_dir" mafft)
fi
if [[ "$reorder_enabled" -eq 1 ]]; then
    mafft_cmd+=(--reorder)
fi
mafft_cmd+=("--$strategy" --maxiterate "$maxiterate")
if [[ -n "$thread_count" && "$thread_count" != "0" ]]; then
    mafft_cmd+=(--thread "$thread_count")
fi
if [[ "${#mafft_extra_args[@]}" -gt 0 ]]; then
    mafft_cmd+=("${mafft_extra_args[@]}")
fi
mafft_cmd+=("$input")

"${mafft_cmd[@]}" > "$output" 2> "${output}.mafft.log"

echo "比对完成！输出文件: $output"
