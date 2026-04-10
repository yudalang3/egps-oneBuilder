# eGPS oneBuilder

一个运行在 Linux 下的系统发育树构建管线。它支持蛋白质序列和 DNA/CDS 序列的比对、建树、可视化，以及不同方法结果的对比。

## 项目特点

- 按输入类型分别提供蛋白质流程和 DNA/CDS 流程。
- 支持 MAFFT 比对、PHYLIP 距离法/简约法、IQ-TREE 极大似然、MrBayes 贝叶斯分析。
- 提供树图可视化，以及 TreeDist / Robinson-Foulds 距离统计。
- 保留 DNA 管线和 Java 纠缠树（Tanglegram）模块，方便和 eGPS 其他组件衔接。

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
- `java_tanglegram/`：Java Swing 纠缠树（Tanglegram）模块，属于更大的 eGPS 平台代码，不是一个独立 Java 应用

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
