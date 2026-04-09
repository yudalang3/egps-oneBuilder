# eGPS oneBuilder

一个运行在 Linux 下的系统发育树构建管线。它支持蛋白质序列和 DNA/CDS 序列的比对、建树、可视化，以及不同方法结果的对比。

## 项目特点

- 蛋白质流程更完整，推荐优先使用。
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

### 蛋白质流程

```bash
zsh s1_quick_align.zsh input.fasta
zsh s2_phylo_4prot.zsh input.aligned.fasta
```

如果你已经有对齐好的 FASTA，也可以直接运行主管线：

```bash
pixi run --manifest-path phylotree_builder_v0.0.1 python phylotree_builder_v0.0.1/phylo_pipeline_4prot.py input.aligned.fasta -o phylo_results_protein
```

### DNA/CDS 流程

```bash
pixi run --manifest-path phylotree_builder_v0.0.1 python phylotree_builder_v0.0.1/phylo_pipeline_4dna.py input.aligned.fasta -o phylo_results
```

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
- MAD 重新定根工具默认路径为 `/opt/BioInfo/MAD/mad/mad`，如果不存在，程序会保留原树

## 示例数据

- `phylotree_builder_v0.0.1/input_demo/simu/`：模拟的对齐数据示例
- `phylotree_builder_v0.0.1/test1/`：示例输出目录结构

## 其他模块

- `phylo_pipeline_4dna.py`：DNA/CDS 管线，保留用于兼容和扩展；其中部分辅助路径不在本仓库内
- `java_tanglegram/`：Java Swing 纠缠树（Tanglegram）模块，属于更大的 eGPS 平台代码，不是一个独立 Java 应用

## 说明

- 蛋白质管线会在 PHYLIP 转换前把序列 ID 临时重命名为 `seqN`，并在输出阶段恢复原名。
- 如果序列 ID 很长，优先使用蛋白质管线提供的包装脚本。
