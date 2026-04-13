# eGPS oneBuilder

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

English documentation: [`README.md`](README.md)

一个运行在 Linux 下的系统发育树构建管线。它支持蛋白质序列和 DNA/CDS 序列的比对、建树、可视化，以及不同方法结果的对比。

## 项目特点

- 按输入类型分别提供蛋白质流程和 DNA/CDS 流程。
- 支持 MAFFT 比对、PHYLIP 距离法/简约法、IQ-TREE 极大似然、MrBayes 贝叶斯分析。
- 提供树图可视化，以及 TreeDist / Robinson-Foulds 距离统计。
- 保留 DNA 管线，并提供一个独立的 Java 纠缠树（Tanglegram）查看器，用来交互式比较四种方法得到的系统发育树。
- 另外还提供一个适合 Linux/X11 桌面或 MobaXTerm X11 转发场景的 Java Swing 全流程 GUI。

## 输入与输出

### 输入

- 多序列蛋白质 FASTA，或多序列 DNA/CDS FASTA
- 建议先完成多序列比对，再进入建树流程

### 输出

- `distance_method/`
- `maximum_likelihood/`
- `bayesian_method/`
- `parsimony_method/`
- `visualizations/`
- `tree_summary/`

## Quick Start

### 蛋白质输入

```bash
zsh phylotree_builder_v0.0.1/s2_phylo_4prot.zsh \
  input_demo/simu/gold_standard_protein_aligned.fasta demo_protein
```

### DNA/CDS 输入

```bash
zsh phylotree_builder_v0.0.1/s2_phylo_4dna.zsh \
  input_demo/simu/gold_standard_cds_aligned.fasta demo_dna
```

### 非对齐输入

如果你的输入还没有完成多序列比对，可以先运行：

```bash
zsh phylotree_builder_v0.0.1/s1_quick_align.zsh input.fasta
```

## 运行耗时预期

下面的时间来自当前仓库自带示例数据在当前开发机上的实际运行日志，只能作为量级参考，不代表所有数据集的固定耗时。

- 蛋白质示例 `gold_standard_protein_aligned.fasta`：整条流程约 8 到 9 分钟。
- 蛋白质流程里最慢的是 MrBayes 贝叶斯步骤；当前默认参数是 `ngen=50000`，单这一步大约用了 7.5 到 8.5 分钟。
- 同一条蛋白质流程中，IQ-TREE 极大似然步骤大约 35 到 40 秒，距离法和简约法通常只要几秒内，后处理（MAD 定根、可视化、树距离统计）大约还需要 20 到 30 秒。
- DNA/CDS 示例 `gold_standard_cds_aligned.fasta`：整条流程约 40 到 45 秒。
- 当前 DNA/CDS 示例里，IQ-TREE 大约 10 到 12 秒，MrBayes 大约 9 到 10 秒，MAD 定根、可视化和树距离统计加起来大约 15 到 25 秒。
- 如果你的数据更多、序列更长、bootstrap 更高、或把 MrBayes 代数调大，贝叶斯法通常会首先成为最慢步骤。

## 工作流程

1. 输入比对后的 FASTA 文件。
2. 转换为 PHYLIP 格式。
3. 用四种方法构建系统发育树。
4. 对树进行重新定根和 ladderize 处理。
5. 生成树图、总结报告和树距离统计。
6. 如需进一步比较不同结果，可使用 Java 纠缠树（Tanglegram）模块。

## Java GUI 工具

仓库现在提供两个独立的 Java Swing 入口：

- `onebuilder.launcher`：全流程 GUI，包含 `Input / Align`、`Tree Build`、`Tanglegram` 三个固定标签页
- `tanglegram.launcher`：专门用于加载已有 `tree_summary/` 的纠缠树查看器

在 `phylotree_builder_v0.0.1/` 目录下编译：

```bash
javac -cp "lib/*:java_tanglegram" -d java_tanglegram \
  java_tanglegram/tanglegram/*.java \
  java_tanglegram/onebuilder/*.java
```

如果你只是想在 Windows PowerShell 里验证 Swing 窗口能否启动，需要把 classpath 分隔符从 `:` 改成 `;`：

```powershell
javac -cp "lib/*;java_tanglegram" -d java_tanglegram `
  java_tanglegram/tanglegram/*.java `
  java_tanglegram/onebuilder/*.java
```

### 全流程 GUI

在 Linux 图形桌面，或带 X11 转发的终端里启动：

```bash
java -cp "java_tanglegram:lib/*" onebuilder.launcher
```

```powershell
java -cp "java_tanglegram;lib/*" onebuilder.launcher
```

使用说明：

- 顶层标签固定为 `Input / Align`、`Tree Build`、`Tanglegram`。
- `Tree Build` 内部使用左侧标签页，固定显示 `Distance`、`ML`、`Bayesian`、`Parsimony`。
- 如果输入 FASTA 还没有完成多序列比对，就勾选 `Run alignment first`；GUI 会把参数转发给 `s1_quick_align.zsh`。
- GUI 会生成一个临时运行时 JSON 配置，并把它传给 `s2_phylo_4prot.zsh` 或 `s2_phylo_4dna.zsh`。
- `Tanglegram` 页面只显示当前这次 GUI run 的结果，只有在当前 run 产出了可用的 `tree_summary/` 之后才会解锁。
- 两个 launcher 都会显式设置 `flatlaf.useNativeLibrary=false`，所以在 JDK 24+ 下不会再打印 `--enable-native-access=ALL-UNNAMED` 的警告。

### Tanglegram 查看器

不预加载任何结果，直接启动：

```bash
java -cp "java_tanglegram:lib/*" tanglegram.launcher
```

```powershell
java -cp "java_tanglegram;lib/*" tanglegram.launcher
```

启动时直接加载一个流程输出的 `tree_summary/`：

```bash
java -cp "java_tanglegram:lib/*" tanglegram.launcher -dir /path/to/tree_summary
```

```powershell
java -cp "java_tanglegram;lib/*" tanglegram.launcher -dir C:\path\to\tree_summary
```

使用说明：

- 主窗口标题固定为 `Tanglegram`。
- 菜单栏只有 `Files > Open`。
- `Open` 需要选择一次流程输出里的 `tree_summary/` 目录。
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
