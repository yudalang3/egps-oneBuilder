# eGPS oneBuilder

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

English documentation: [`README.md`](README.md)

一个结合 GUI 和 CLI 的系统发生树构建工作流。用户可以在 Windows 下配置工作流、导出配置并查看已有结果；真正的比对和建树执行只支持 Linux。



## 0. 项目特点

- 按输入类型分别提供蛋白质流程和 DNA/CDS 流程。
- 支持 MAFFT 比对、PHYLIP 距离法/简约法、IQ-TREE 极大似然、MrBayes 贝叶斯分析。（即支持四大类构建进化树的方法）
- GUI 可以通过 `--config` 运行时 JSON，把 MAFFT、方法启停、极大似然参数和贝叶斯参数传入现有 shell wrapper 与 Python 管线。
- 保留 CLI wrapper，方便脚本化和批量式运行，因此同一套流程既能走 GUI，也能走命令行自动化。GUI 与 CLI 现在通过共享的 `--config` 运行时 JSON 串联起来，而不是各自维护独立执行逻辑。（现有 CLI wrapper 继续保留为一等入口，因此同一套流程仍然可以用于脚本化、重复性和批量式运行。）
- 在 `tree_summary/` 中输出树图可视化、TreeDist / Robinson-Foulds 距离矩阵，以及合并热图。
- `onebuilder.launcher` 新增了交互式 Java Swing 全流程 GUI，并整理成四步工作流：`Input / Align`、`Tree Parameters`、`Tree Build`、`Tanglegram`。
- 参数编辑页和运行页已经拆开：方法参数集中在 `Tree Parameters`，`Tree Build` 专门负责配置摘要、运行/导出按钮、日志和方法状态指示灯。

- 借助于强大的GUI功能，`tanglegram.launcher` 可以直接加载一次流程输出的 `tree_summary/`，并把四棵树固定渲染成 6 个两两比较标签页。
- 提供独立的 Java 纠缠树（Tanglegram）查看器，可在 Linux 或 Windows 下把四种方法生成的树固定组合成 6 个两两配对视图进行交互比较。
- `tree_summary/` 现在除了原始距离矩阵，还会输出一张合并的 TreeDist + Robinson-Foulds 热图。
- Windows 下的 `onebuilder.launcher` 现在会在启动时给出说明提示，明确告知该平台只能配置和导出，不能直接执行管线；这个提示可以在 Preference 里关闭或重新打开。

## 1. 输入与输出

### 1.1 输入

- 多序列蛋白质 FASTA，或多序列 DNA/CDS FASTA
- 输入可以是已经比对好的多序列 FASTA，也可以是尚未比对的多序列 FASTA

### 1.2 输出

- `distance_method/`
- `maximum_likelihood/`
- `bayesian_method/`
- `parsimony_method/`
- `visualizations/`
- `tree_summary/`

## 2. Quick Start

### 2.1 纯 GUI

启动工作流 GUI，在界面里交互式配置参数并直接运行：

```bash
java -cp "java_tanglegram:lib/*" onebuilder.launcher
```

适合在窗口里设置比对、方法启停、ML / 贝叶斯参数，并在同一界面里启动运行。



查看已经运行的结果：

```java
java -cp "java_tanglegram:lib/*" tanglegram.launcher
```

### 2.2 纯 CLI

直接在命令行运行流程，适合脚本化和批量任务。

#### 2.2.1 蛋白质序列

```bash
zsh phylotree_builder_v0.0.1/s2_phylo_4prot.zsh \
  input_demo/simu/gold_standard_protein_aligned.fasta demo_protein
```

#### 2.2.2 DNA 序列

```bash
zsh phylotree_builder_v0.0.1/s2_phylo_4dna.zsh \
  input_demo/simu/gold_standard_cds_aligned.fasta demo_dna
```

#### 2.2.3 未比对序列

如果输入还没有完成多序列比对，可以先运行 MAFFT，然后把比对结果交给建树流程：

```bash
zsh phylotree_builder_v0.0.1/s1_quick_align.zsh input.fasta
```

### 2.3 GUI + CLI

这是推荐的混合式流程：先在 GUI 里设置参数并导出运行时 JSON，然后在 Linux 命令行里用这一份 JSON 直接重放整条流程。

```bash
java -cp "java_tanglegram:lib/*" onebuilder.launcher
```

然后在 Linux 下执行导出的 JSON：

```bash
zsh phylotree_builder_v0.0.1/run_onebuilder_config.zsh /path/to/demo.onebuilder.json
```

这个总入口脚本会直接读取 JSON 里的 `run.input_type`、`run.input_file`、`run.output_base_dir`、`run.output_prefix` 和 `alignment.run_alignment_first`。如果需要，它会先跑 MAFFT，再自动分发到蛋白质或 DNA/CDS 的建树 wrapper。

底层的 `s1_quick_align.zsh`、`s2_phylo_4prot.zsh`、`s2_phylo_4dna.zsh` 仍然保留，但现在推荐的“GUI 导出后命令行复现”方式，就是直接使用 `run_onebuilder_config.zsh`。

### 2.4 整份 Config 直接运行

现在也可以直接从一整份运行时 JSON 启动，而不必每次都先重新打开 GUI。这个用法适合把一份完整配置文件当作重复运行、共享或审计时的唯一真源。

```bash
zsh phylotree_builder_v0.0.1/run_onebuilder_config.zsh tree_build_full_config_template.json
```

实际使用时，建议先复制 `tree_build_full_config_template.json`，再把其中 `run` 段的路径和你关心的方法参数补全。

完整模板现在允许同时保留 protein 和 DNA/CDS 两套方法块。运行时会先读取 `run.input_type`，然后自动选择匹配的那一套设置。

例如当 `run.input_type` 为 `DNA_CDS` 时，即使同一份 JSON 里仍然保留了蛋白质专用块，实际运行也会只使用 `dnadist`、`dnapars`、DNA 的 MrBayes 参数，以及 DNA 安全的 IQ-TREE 设置。

### 2.5 包装脚本参数

现在这几个包装脚本已经把 GUI 会用到的参数接口公开出来了：

- `run_onebuilder_config.zsh <runtime.json>` 或 `run_onebuilder_config.zsh --config <runtime.json>`
- `s1_quick_align.zsh [--config runtime.json] [--strategy localpair|genafpair|auto|globalpair] [--maxiterate N] [--reorder|--no-reorder] <input.fasta>`
- `s2_phylo_4prot.zsh [--config runtime.json] <input.fasta> [output_prefix]`
- `s2_phylo_4dna.zsh [--config runtime.json] <input.fasta> [output_prefix]`

其中 `run_onebuilder_config.zsh` 是 Linux-only 的最高层命令行入口。它可以直接复用 `onebuilder.launcher` 导出的 JSON，因此用户不需要再手工重复填写输入文件和输出前缀。

底层三个 wrapper 上的 `--config` JSON 仍然保留，主要用于让 GUI 把比对参数、方法启用状态、ML 参数、贝叶斯参数以及原生透传块传进现有 shell wrapper 和 Python 管线。

详细参数说明：

- 中文参数参考：[`tree_build_pipeline_parameter_reference_zh.md`](tree_build_pipeline_parameter_reference_zh.md)
- 完整 JSON 模板：[`tree_build_full_config_template.json`](tree_build_full_config_template.json)

## 3. 运行耗时预期

下面的时间来自当前仓库自带示例数据在当前开发机上的实际运行日志，只能作为量级参考，不代表所有数据集的固定耗时。

- 蛋白质示例 `gold_standard_protein_aligned.fasta`：整条流程约 8 到 9 分钟。
- 蛋白质流程里最慢的是 MrBayes 贝叶斯步骤；当前默认参数是 `ngen=50000`，单这一步大约用了 7.5 到 8.5 分钟。
- 同一条蛋白质流程中，IQ-TREE 极大似然步骤大约 35 到 40 秒，距离法和简约法通常只要几秒内，后处理（MAD 定根、可视化、树距离统计）大约还需要 20 到 30 秒。
- DNA/CDS 示例 `gold_standard_cds_aligned.fasta`：整条流程约 40 到 45 秒。
- 当前 DNA/CDS 示例里，IQ-TREE 大约 10 到 12 秒，MrBayes 大约 9 到 10 秒，MAD 定根、可视化和树距离统计加起来大约 15 到 25 秒。
- 如果你的数据更多、序列更长、bootstrap 更高、或把 MrBayes 代数调大，贝叶斯法通常会首先成为最慢步骤。

## 4. 工作流程

1. 输入比对后的 FASTA 文件。
2. 转换为 PHYLIP 格式。
3. 用四种方法构建系统发育树。
4. 对树进行重新定根和 ladderize 处理。
5. 生成树图、总结报告和树距离统计。
6. 如需进一步比较不同结果，可使用 Java 纠缠树（Tanglegram）模块。

## 5. Java GUI 工具

仓库现在提供两个独立的 Java Swing 入口：

- `onebuilder.launcher`：全流程 GUI，包含 `Input / Align`、`Tree Parameters`、`Tree Build`、`Tanglegram` 四个固定步骤。Linux 下可运行管线；Windows 下只负责配置和导出 JSON。
- `tanglegram.launcher`：专门用于加载已有 `tree_summary/` 的纠缠树查看器，Linux 和 Windows 都可用。
- `run_onebuilder_config.zsh`：Linux-only 的 JSON 总入口，用一份 GUI 导出的 `.onebuilder.json` 直接重放整条流程，不需要再次手填输入文件或输出前缀。

从仓库根目录编译：

```bash
javac -cp "phylotree_builder_v0.0.1/lib/*:phylotree_builder_v0.0.1/java_tanglegram" -d phylotree_builder_v0.0.1/java_tanglegram \
  phylotree_builder_v0.0.1/java_tanglegram/tanglegram/*.java \
  phylotree_builder_v0.0.1/java_tanglegram/onebuilder/*.java \
  phylotree_builder_v0.0.1/java_tanglegram/tests/*.java
```

```powershell
javac -cp "phylotree_builder_v0.0.1\lib/*;phylotree_builder_v0.0.1\java_tanglegram" -d phylotree_builder_v0.0.1\java_tanglegram `
  phylotree_builder_v0.0.1\java_tanglegram\tanglegram\*.java `
  phylotree_builder_v0.0.1\java_tanglegram\onebuilder\*.java `
  phylotree_builder_v0.0.1\java_tanglegram\tests\*.java
```

或者先进入 `phylotree_builder_v0.0.1/` 目录，再使用：

```bash
javac -cp "lib/*:java_tanglegram" -d java_tanglegram \
  java_tanglegram/tanglegram/*.java \
  java_tanglegram/onebuilder/*.java \
  java_tanglegram/tests/*.java
```

```powershell
javac -cp "lib/*;java_tanglegram" -d java_tanglegram `
  java_tanglegram/tanglegram/*.java `
  java_tanglegram/onebuilder/*.java `
  java_tanglegram/tests/*.java
```

### 5.1 全流程 GUI

从仓库根目录在 Linux 图形桌面，或带 X11 转发的终端里启动：

```bash
java -cp "phylotree_builder_v0.0.1/java_tanglegram:phylotree_builder_v0.0.1/lib/*" onebuilder.launcher
```

先进入 `phylotree_builder_v0.0.1/` 后再启动：

```bash
java -cp "java_tanglegram:lib/*" onebuilder.launcher
```

从仓库根目录在 Windows PowerShell 里启动：

```powershell
java -cp "phylotree_builder_v0.0.1\java_tanglegram;phylotree_builder_v0.0.1\lib/*" onebuilder.launcher
```

先执行 `Set-Location phylotree_builder_v0.0.1` 后再启动：

```powershell
java -cp "java_tanglegram;lib/*" onebuilder.launcher
```

使用说明：

- 顶层工作流固定为 `Input / Align`、`Tree Parameters`、`Tree Build`、`Tanglegram`。
- `Input / Align` 是必须先完成的入口页。如果必填项还没准备好，后续步骤仍然可以点击，但会解释为什么当前不能跳转。
- `Tree Parameters` 使用方法树来组织参数，包含 `Distance Method`、`Maximum Likelihood`、`Bayes Method`、`Maximum Parsimony`、`Protein Structure`。
- `Protein Structure` 节点会一直显示。蛋白输入时可用；非蛋白输入时保留显示并提示 `Protein only`。
- `Tree Build` 现在是专门的运行页，负责显示配置摘要/日志，并提供 `Run`、`Stop`、`Export Config`。
- `Tree Build` 页面里还有 5 个紧凑的方法状态指示灯，对应 `Distance Method`、`Maximum Likelihood`、`Bayes Method`、`Maximum Parsimony`，以及保留位 `Protein Structure`。
- 真正的比对和建树只支持 Linux。请在 Linux 图形桌面或 X11 转发环境里执行真实 run。
- Windows 下的 `onebuilder.launcher` 只负责参数编辑和 JSON 配置导出，不会执行 `s1_quick_align.zsh`、`s2_phylo_4prot.zsh` 或 `s2_phylo_4dna.zsh`；除非你在 Preference 里关闭，否则启动时会先弹出说明提示。
- 如果输入 FASTA 还是原始序列、尚未完成多序列比对，就勾选 `Run multiple sequence alignment first`；在 Linux 下 GUI 会把参数转发给 `s1_quick_align.zsh`。
- 输入页包含输入/输出路径、最近浏览目录记忆、MAFFT strategy、`Maxiterate`、sequence reorder 控制、`Advanced MAFFT`、`Export config file when running` 以及 `Export JSON`。
- 高级参数区域默认折叠，并且现在在 4 个方法页里都统一放在主要参数区的正下方。
- GUI 支持在启动 run 之前，按方法开启/关闭四种建树步骤，并调整公开出来的 ML、贝叶斯以及结构化 PHYLIP 常用参数。
- `Protein Structure` 是蛋白质专用的 Foldseek 步骤。它既可以直接使用输入 FASTA 的 ProstT5/3Di 模式，也可以读取结构映射 TSV；TSV 中每个非注释行格式为 `sequence_id<TAB>structure_file`，相对结构路径会按 TSV 所在目录解析。
- Foldseek 步骤会输出 `protein_structure/pairwise_scores.tsv`、`protein_structure/similarity_matrix.tsv`、`protein_structure/distance_matrix.tsv` 和 `protein_structure/run_config.json`。运行时生成的 `structure_inputs/` 与 `foldseek_tmp/` 已经被 Git 忽略。
- 默认结构距离策略是 `distance = 1 - similarity`。有真实结构时默认使用 `qtmscore` 和 `ttmscore` 的均值；如果 Foldseek 没返回某个 pair，默认距离写为 `1`，保证距离矩阵仍是数值矩阵。
- ML 页面现在会给出非阻断式的 Bootstrap 提示：`1000` 仍是推荐默认值，`0` 会明确表示跳过 `-bb`，而特别大的值只会提示运行时间风险，不会强制拦截。
- PHYLIP 页面继续保留 `menu_overrides` 作为高级逃生口，同时把已经稳定的常用项直接结构化出来，例如 DNA 距离模型设置、neighbor 类型/外群、蛋白 `protpars` 的输出开关，以及 DNA `dnapars` 的外群/颠换简约法选项。
- 窗口菜单中还提供 `Preference > Settings...`，可设置共享的全局字体族、全局字号、窗口大小恢复、默认的 tanglegram 标签字号，以及 `Show Windows oneBuilder startup warning`。
- 这些 Preference 修改后会立即作用到当前已打开的 Java 窗口，并在下次启动时继续沿用。
- 这些共享 GUI 偏好现在统一保存在 `~/.egps.onebuilder.prop`，不再依赖 Windows 注册表。
- `Export config file when running` 默认勾选。勾选状态下，Linux run 会把配置保存为 `<output_base_dir>/<output_prefix>.onebuilder.json`，然后把这个 JSON 文件传给 wrapper。
- 如果取消勾选，Linux run 仍然可以执行，但只会生成本次运行使用的临时 JSON 配置。
- `Export JSON` 按钮总是会把当前 GUI 参数写入 `<output_base_dir>/<output_prefix>.onebuilder.json`。
- 导出的 `<output_prefix>.onebuilder.json` 现在可以在 Linux 下直接用 `zsh phylotree_builder_v0.0.1/run_onebuilder_config.zsh /path/to/file.onebuilder.json` 执行。
- `onebuilder.launcher` 里的 `Tanglegram` 页面只会在 Linux 成功运行后解锁，并自动载入当前这次 run 的结果。Windows 下请使用独立的 `tanglegram.launcher` 查看已有 `tree_summary/`。
- GUI 内部的 `Tanglegram` 页面还提供 label font size、水平/垂直 padding、auto-fit，以及 `Reload from current run` 按钮。
- 两个 launcher 都会显式设置 `flatlaf.useNativeLibrary=false`，所以在 JDK 24+ 下不会再打印 `--enable-native-access=ALL-UNNAMED` 的警告。

### 5.2 Tanglegram 查看器

从仓库根目录直接启动，不预加载任何结果：

```bash
java -cp "phylotree_builder_v0.0.1/java_tanglegram:phylotree_builder_v0.0.1/lib/*" tanglegram.launcher
```

```powershell
java -cp "phylotree_builder_v0.0.1\java_tanglegram;phylotree_builder_v0.0.1\lib/*" tanglegram.launcher
```

先进入 `phylotree_builder_v0.0.1/` 目录后也可以：

```bash
java -cp "java_tanglegram:lib/*" tanglegram.launcher
```

```powershell
java -cp "java_tanglegram;lib/*" tanglegram.launcher
```

启动时直接加载一个流程输出的 `tree_summary/`：

```bash
java -cp "phylotree_builder_v0.0.1/java_tanglegram:phylotree_builder_v0.0.1/lib/*" tanglegram.launcher -dir test1/tree_summary
```

```powershell
java -cp "phylotree_builder_v0.0.1\java_tanglegram;phylotree_builder_v0.0.1\lib/*" tanglegram.launcher -dir test1\tree_summary
```

使用说明：

- 主窗口标题固定为 `Tanglegram`。
- Windows 下如果你已经有流程输出，应该用这个独立查看器来看 6 个两两比较结果。
- 菜单栏提供 `Files > Open` 和 `Preference > Settings...`。
- `Preference` 与 `onebuilder.launcher` 共享同一套全局 UI 设置，包括全局字体和默认 tanglegram 标签字号。
- 这套共享 UI 设置同样保存在 `~/.egps.onebuilder.prop`。
- `Open` 需要选择一次流程输出里的 `tree_summary/` 目录。
- 查看器会把可用的两两配对固定排成这 6 个标签顺序：`NJ-ML`、`NJ-BI`、`NJ-MP`、`ML-BI`、`ML-MP`、`BI-MP`。
- 如果某一种方法缺失，但仍然能解析出至少两棵树，查看器会只加载有效的配对标签，而不是整窗失败。
- 如果 `tree_meta_data.tsv` 里写的是失效路径或某台机器上的绝对路径，程序会自动回退到标准输出目录 `distance_method/`、`maximum_likelihood/`、`bayesian_method/`、`parsimony_method/` 里查找对应树文件。
- 如果启动时不带 `-dir` 参数，界面会先保持空白，等待用户点击菜单导入 `tree_summary/`。

## 依赖与环境

- Linux
- Pixi 环境，依赖定义在 `phylotree_builder_v0.0.1/pixi.toml`
- 主要依赖：`phylip`、`iqtree`、`mrbayes`、`biopython`、`ete4`、`r-treedist`、`matplotlib`、`mafft`
- 蛋白质管线会使用同目录下的 `help_utils.py` 和 `cal_pair_wise_tree_dist.R`
- MAD 重新定根工具默认路径为 `phylotree_builder_v0.0.1/third_party/mad/mad`

## 示例数据

- `input_demo/simu/`：模拟的对齐数据示例
- `test1/`：示例输出目录结构

## 其他模块

- `phylo_pipeline_4dna.py`：DNA/CDS 管线主入口，对齐了蛋白质流程的本地辅助脚本、MAD 定根和名称恢复逻辑
- `java_tanglegram/`：独立的 Java Swing 纠缠树（Tanglegram）查看器，会把四棵树按 6 种两两组合显示在标签页中

## 说明

- 蛋白质管线和 DNA/CDS 管线都会在 PHYLIP 转换前把序列 ID 临时重命名为 `seqN`，并在输出阶段恢复原名。
- 推荐优先使用仓库内提供的包装脚本，而不是直接手动拼接 `pixi run ... python3.13 ...` 命令。

## 注意事项与常见坑

- PHYLIP 使用的是 strict 格式，传统上序列 ID 最好不要超过 10 个字符。本仓库两条主管线都会自动把 ID 临时改成 `seqN`，但如果你手动调用 PHYLIP 工具，仍要注意这个限制。
- 蛋白质输入请走蛋白质流程，DNA/CDS 输入请走 DNA/CDS 流程。
- 包装脚本依赖 `pixi` 可执行文件。如果你的 shell 里找不到 `pixi`，请先把 Pixi 加到 `PATH`，或者设置环境变量 `PIXI_EXE`。
- 直接调用 Python 管线时，优先使用 `python3.13`，不要假设 Pixi 环境里一定有 `python` 这个命令名。
- 如果直接运行主 Python 脚本而不是用包装脚本，某些环境下还需要设置 `LD_LIBRARY_PATH=phylotree_builder_v0.0.1/.pixi/envs/default/lib`，否则 `numpy`、`Rscript` 或 IQ-TREE 可能无法正常加载依赖。
- IQ-TREE 在当前 Pixi 环境里可能显示为 `iqtree3`；仓库脚本已经做了兼容处理，但如果你手动调命令，需要留意这个命令名差异。
- MAD 重新定根依赖仓库内的 `phylotree_builder_v0.0.1/third_party/mad/mad`。如果缺失，主管线会保留原树继续执行。
- 当前默认参数下，蛋白质流程比 DNA/CDS 流程慢得多，主要原因不是算法类别不同，而是蛋白质流程默认给 MrBayes 设置了更高的迭代代数（`50000` 对 `10000`）。
- 本仓库里的示例数据和示例输出都在仓库根目录，不在 `phylotree_builder_v0.0.1/` 子目录里。

## 开源协议

本项目采用 Apache-2.0 协议，详见 `LICENSE`。
