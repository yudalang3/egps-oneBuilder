# wrap_protein_pairwise_distance 计划、实现、评估与测试记录

## 目标

为 oneBuilder 增加一个蛋白质两两结构相似性/距离计算模块，用于在 Protein 输入流程中额外生成结构相似性矩阵。

目标输入有两类：

- 只有蛋白质 FASTA 序列。
- 用户提供 FASTA 序列，并通过 TSV 指定每条序列对应的 PDB 或 mmCIF 结构文件。

本任务的重点不是传统序列相似性，而是尽量从序列或结构出发，得到结构层面的相似性或距离。

## 工具选型结论

第一版只包装一个工具：`Foldseek`。

选择 Foldseek 的原因：

- 支持 Linux x86_64，当前 CPU 支持 AVX2。
- 已可通过 repo-local Pixi 环境安装和调用。
- 支持 PDB/mmCIF 结构输入。
- 支持 all-vs-all 风格的批量结构搜索，比逐对调用 US-align 更适合矩阵输出。
- 输出 TSV，便于统一整理为 pairwise table、similarity matrix 和 distance matrix。
- 后续可扩展 FASTA-only ProstT5/3Di 路径。

未进入第一版的工具：

- `US-align`：适合后续作为小规模坐标级 TM-score 复核后端，但不能直接处理 FASTA。
- `TM-align`：可作为 fallback，但同样不能直接处理 FASTA，且需要逐对枚举。
- `DALI v5`、`FATCAT 2`、`DeepAlign`：安装和维护成本较高，不适合作为第一版默认实现。

## 本机环境评估

当前机器适合优先使用免编译或 Pixi/conda 可安装的 Linux 工具。

- 系统：WSL2 Linux，`x86_64`
- CPU：Intel 13th Gen Core i5-1340P，当前 Linux 中可见 4 个 CPU 线程
- CPU 指令集：支持 `AVX2`
- glibc：`2.35`
- 内存：约 `9.7 GiB`
- GPU：未发现 `nvidia-smi`
- 可用包管理器：`conda`、`mamba`、`pixi`、`apt`
- 容器运行时：Docker、Singularity、Apptainer 均不在 PATH 中
- 编译工具链：`gcc`、`g++`、`make`、`cmake` 均不在 PATH 中

APT 检查结论：

- `tm-align` 可由 Ubuntu jammy universe 安装，候选版本为 `20190822+dfsg-2build1`。
- `foldseek`、`usalign`、DALI、蛋白结构版 FATCAT、DeepAlign 未在 APT 缓存中直接命中。
- APT 中的 `fatcat` 是 FAT 文件系统取证工具，不是蛋白结构比对 FATCAT。

实际安装方式：

- 在 `phylotree_builder_v0.0.1/pixi.toml` 中加入 `foldseek = "*"`。
- 运行 `pixi install --manifest-path phylotree_builder_v0.0.1/pixi.toml` 更新环境和 `pixi.lock`。
- 实测 Pixi 环境中 Foldseek 版本为 `10.941cd33`。

## 核心分数定义

`TM-score` 是常用蛋白三维结构相似性分数，通常在 `0` 到 `1` 之间。

- 接近 `1`：结构几乎完全相同。
- 大于 `0.5`：通常认为两个蛋白有相同或相近 fold。
- 小于 `0.2`：通常接近随机结构相似性。

第一版默认相似度规则：

```text
如果有 qtmscore 和 ttmscore：similarity = (qtmscore + ttmscore) / 2
否则如果有 alntmscore：similarity = alntmscore
否则如果有 prob：similarity = prob
否则如果只有 fident：similarity = fident
```

距离定义：

```text
distance = 1 - similarity
```

矩阵输出规则：

- Foldseek 结果是有向记录，`query -> target` 和 `target -> query` 可能略有差别。
- 第一版将矩阵值改为双向平均，使 `similarity_matrix.tsv` 和 `distance_matrix.tsv` 对称。
- 对角线固定为 `similarity = 1`、`distance = 0`。

## 输入格式

### Wrapper CLI

实际实现的入口：

```bash
python3.13 phylotree_builder_v0.0.1/wrap_protein_pairwise_distance.py \
  --input-fasta proteins.fasta \
  --output-dir protein_structure \
  --structure-manifest structure_mapping.tsv \
  --threads 4
```

参数：

- `--input-fasta`：蛋白 FASTA 或已比对 FASTA。
- `--output-dir`：输出目录。
- `--structure-manifest`：可选 TSV，指定 FASTA ID 到结构文件的映射。
- `--threads`：可选，传给 Foldseek。
- `--foldseek`：可选，Foldseek 可执行文件路径；默认从 `FOLDSEEK_EXE` 或 PATH 解析。

### 结构 TSV

第一版采用两列 TSV，而不是更复杂的 manifest。

格式：

```text
# sequence_id<TAB>structure_file
FZD1_HUMAN_Q9UP38<TAB>structures/FZD1_HUMAN_Q9UP38.cif
```

规则：

- UTF-8 文本。
- Tab 分隔。
- 空行忽略。
- `#` 开头的行视为注释。
- 第一列必须匹配 FASTA header 的 `record.id`。
- 第二列支持 `.pdb`、`.ent`、`.cif`、`.mmcif` 及对应 `.gz`。
- 相对路径按 TSV 文件所在目录解析。
- 重复 ID 会报错。
- TSV ID 不在 FASTA 中会报错。
- 启用结构 TSV 时，每条 FASTA 序列都必须有结构映射，缺失会报错。

这个约束是有意保守的：第一版避免把坐标级结构分数和 ProstT5/3Di sequence-only 分数混在同一矩阵中，降低解释风险。

## 输出格式

输出目录：

```text
protein_structure/
├── pairwise_scores.tsv
├── similarity_matrix.tsv
├── distance_matrix.tsv
├── normalized_structure_manifest.tsv
├── run_config.json
├── foldseek_raw.tsv
├── foldseek_tmp/
└── structure_inputs/
```

`pairwise_scores.tsv` 字段：

```text
query	target	pair_type	backend	score_type	similarity	distance	fident	alnlen	evalue	bits	alntmscore	qtmscore	ttmscore	lddt	prob
```

字段说明：

- `query`、`target`：恢复后的 FASTA ID。
- `pair_type`：当前结构 TSV 模式为 `structure_vs_structure`。
- `backend`：当前固定为 `foldseek`。
- `score_type`：当前主路径为 `foldseek_tmscore_mean`。
- `similarity`：wrapper 归一化后的主相似性分数。
- `distance`：`1 - similarity`。
- `alntmscore`、`qtmscore`、`ttmscore`、`lddt`、`prob`：Foldseek 原始输出字段。

`normalized_structure_manifest.tsv` 用于记录 wrapper 实际传给 Foldseek 的结构输入 ID 与原始 FASTA ID 的对应关系。

## 实现内容

### Python wrapper

新增文件：

```text
phylotree_builder_v0.0.1/wrap_protein_pairwise_distance.py
```

已实现功能：

- 解析 FASTA ID 并检查重复 ID。
- 解析两列结构 TSV。
- 校验 TSV 中结构文件是否存在、扩展名是否支持。
- 将结构文件链接或复制到 `structure_inputs/`。
- 为 Foldseek 生成安全 ID，避免原始 FASTA ID 中特殊字符影响 Foldseek 文件名和结果映射。
- 调用 `foldseek easy-search` 进行结构 all-vs-all 搜索。
- 解析 Foldseek TSV 输出。
- 用 `(qtmscore + ttmscore) / 2` 生成主 `similarity`。
- 生成 `distance = 1 - similarity`。
- 输出 pairwise table、对称 similarity matrix、对称 distance matrix 和 run config。
- 支持 `--threads` 和自定义 `--foldseek`。

已写入但未作为本次主要端到端验收目标的能力：

- 无 `--structure-manifest` 时走 `sequence_prostt5` 分支，调用 `foldseek databases ProstT5`、`foldseek createdb --prostt5-model`、`foldseek search`、`foldseek convertalis`。
- 该分支属于后续 FASTA-only 工作流基础，未用本次 FZD demo 做完整耗时评估。

### Protein pipeline 集成

修改文件：

```text
phylotree_builder_v0.0.1/phylo_pipeline_4prot.py
```

集成方式：

- 在 protein pipeline 的四种树构建方法之后增加 `protein_structure_method()`。
- 当 runtime config 中 `methods.protein_structure.enabled = true` 时调用 wrapper。
- 输出目录固定为 pipeline 输出目录下的 `protein_structure/`。
- 运行完成后写入进度 marker：`====Protein structure complete====`。
- 未启用时写入 skipped marker。

### oneBuilder runtime config

修改文件：

```text
phylotree_builder_v0.0.1/onebuilder_runtime_config.py
```

已实现：

- 新增 `protein_structure` 默认配置。
- 只允许 Protein 输入启用 Protein Structure。
- 固定后端为 `foldseek`。
- 解析 `use_structure_manifest`、`structure_manifest_file`、`similarity_rule`。
- 修复相对路径解析：`structure_manifest_file` 现在按 `.onebuilder.json` 所在目录解析，而不是按当前工作目录解析。

### oneBuilder GUI 集成

新增文件：

```text
phylotree_builder_v0.0.1/java_tanglegram/onebuilder/ProteinStructureConfig.java
phylotree_builder_v0.0.1/java_tanglegram/onebuilder/ProteinStructurePanel.java
```

修改文件：

```text
phylotree_builder_v0.0.1/java_tanglegram/onebuilder/TreeParametersPanel.java
phylotree_builder_v0.0.1/java_tanglegram/onebuilder/PipelineRuntimeConfig.java
phylotree_builder_v0.0.1/java_tanglegram/onebuilder/PipelineConfigWriter.java
phylotree_builder_v0.0.1/java_tanglegram/onebuilder/InputAlignPanel.java
phylotree_builder_v0.0.1/java_tanglegram/onebuilder/TreeMethodKey.java
phylotree_builder_v0.0.1/java_tanglegram/onebuilder/TreeBuildPanel.java
phylotree_builder_v0.0.1/java_tanglegram/onebuilder/PipelineProgressInterpreter.java
phylotree_builder_v0.0.1/java_tanglegram/tests/OneBuilderStandaloneTest.java
```

已实现：

- `Tree Parameters` 中 `Protein Structure` 节点从占位区改为 Foldseek 配置面板。
- GUI 提供启用 checkbox、结构 TSV 路径输入框和 Browse 按钮。
- 非 Protein 输入时保留 `Protein Structure` 节点，但禁用并显示 `Protein only` 语义。
- `Input / Align` draft summary 显示 Foldseek 状态。
- 启用结构 TSV 时，GUI 校验 TSV 路径存在。
- `Export Config` 输出 `methods.protein_structure` JSON。
- `Tree Build` 第五个状态槽变成真实 Protein Structure 运行状态。
- 进度解释器识别 Protein Structure complete/failed/skipped marker。

### MAFFT wrapper 修复

修改文件：

```text
phylotree_builder_v0.0.1/s1_quick_align.zsh
```

端到端测试中发现两个环境问题，并已修复：

- Pixi 中的 `mafft` wrapper 写死了旧安装前缀 `/opt/BioInfo/...`，repo 移动后找不到正确的 `libexec/mafft`。
- 当前非交互工具环境中 MAFFT 直接写 `/dev/stderr` 会失败。

修复方式：

- 在 `s1_quick_align.zsh` 中，如果存在 repo-local `.pixi/envs/default/libexec/mafft`，显式导出 `MAFFT_BINARIES` 指向该目录。
- 将 MAFFT stderr 重定向到 `${output}.mafft.log`，避免 `/dev/stderr` 失败导致 alignment 退出。

## Demo 数据集

新增目录：

```text
input_demo/prot_seqs_with_structure/
```

内容：

```text
human_FZD_paralogs.fasta
human_FZD_structure_mapping.tsv
human_FZD_metadata.tsv
human_FZD_with_structure.onebuilder.json
structures/
demo_outputs/
```

下载的人类 FZD paralogs：

| Gene | UniProt | Entry | Length |
|---|---|---|---:|
| FZD1 | Q9UP38 | FZD1_HUMAN | 647 |
| FZD2 | Q14332 | FZD2_HUMAN | 565 |
| FZD3 | Q9NPG1 | FZD3_HUMAN | 666 |
| FZD4 | Q9ULV1 | FZD4_HUMAN | 537 |
| FZD5 | Q13467 | FZD5_HUMAN | 585 |
| FZD6 | O60353 | FZD6_HUMAN | 706 |
| FZD7 | O75084 | FZD7_HUMAN | 574 |
| FZD8 | Q9H461 | FZD8_HUMAN | 694 |
| FZD9 | O00144 | FZD9_HUMAN | 591 |
| FZD10 | Q9ULW2 | FZD10_HUMAN | 581 |

数据来源：

- 蛋白序列：UniProt reviewed human entries。
- 结构文件：AlphaFoldDB full-length mmCIF model v6。

选择 AlphaFoldDB mmCIF 的原因：

- FZD 是膜蛋白，实验 PDB 常常只覆盖 CRD 或复合物片段。
- Demo 需要每条序列稳定对应一个结构文件。
- AlphaFoldDB 提供 full-length mmCIF，适合 Foldseek all-vs-all 验证。

FASTA header 设计：

```text
>FZD1_HUMAN_Q9UP38 FZD1 Q9UP38 reviewed human Frizzled paralog
```

TSV 设计：

```text
# sequence_id	structure_file
FZD1_HUMAN_Q9UP38	structures/FZD1_HUMAN_Q9UP38.cif
```

## 运行方式

### 直接运行 wrapper

```bash
/home/dell/.pixi/bin/pixi run --manifest-path phylotree_builder_v0.0.1/pixi.toml \
  python3.13 phylotree_builder_v0.0.1/wrap_protein_pairwise_distance.py \
  --input-fasta input_demo/prot_seqs_with_structure/human_FZD_paralogs.fasta \
  --structure-manifest input_demo/prot_seqs_with_structure/human_FZD_structure_mapping.tsv \
  --output-dir input_demo/prot_seqs_with_structure/foldseek_structure_similarity \
  --threads 4
```

### 通过 oneBuilder 总入口运行完整流程

```bash
zsh phylotree_builder_v0.0.1/run_onebuilder_config.zsh \
  --force-overwrite \
  input_demo/prot_seqs_with_structure/human_FZD_with_structure.onebuilder.json
```

该命令模拟用户选择蛋白质序列 `human_FZD_paralogs.fasta`，启用先 MAFFT 比对，并启用 Protein Structure TSV。

## 测试记录

### Foldseek wrapper 单独测试

命令：

```bash
/home/dell/.pixi/bin/pixi run --manifest-path phylotree_builder_v0.0.1/pixi.toml \
  python3.13 phylotree_builder_v0.0.1/wrap_protein_pairwise_distance.py \
  --input-fasta input_demo/prot_seqs_with_structure/human_FZD_paralogs.fasta \
  --structure-manifest input_demo/prot_seqs_with_structure/human_FZD_structure_mapping.tsv \
  --output-dir input_demo/prot_seqs_with_structure/foldseek_structure_similarity \
  --threads 4
```

结果：

- 成功生成 `pairwise_scores.tsv`。
- 成功生成 `similarity_matrix.tsv`。
- 成功生成 `distance_matrix.tsv`。
- 10 个结构全部被 Foldseek 接受：`Ignore 0 out of 10. Too short: 0, incorrect: 0, not proteins: 0.`
- 生成 100 条有向 pairwise 记录。
- 矩阵已改为双向平均后的 10x10 对称矩阵。

### oneBuilder 完整端到端测试

命令：

```bash
zsh phylotree_builder_v0.0.1/run_onebuilder_config.zsh \
  --force-overwrite \
  input_demo/prot_seqs_with_structure/human_FZD_with_structure.onebuilder.json
```

最终输出目录：

```text
input_demo/prot_seqs_with_structure/demo_outputs/human_FZD_with_structure_demo/
```

已成功生成：

```text
alignment.phy
distance_method/
maximum_likelihood/
bayesian_method/
parsimony_method/
protein_structure/
visualizations/
tree_summary/
```

端到端通过的步骤：

- MAFFT alignment。
- PHYLIP protdist + neighbor distance method。
- IQ-TREE maximum likelihood。
- MrBayes Bayesian inference。
- PHYLIP protpars parsimony。
- Foldseek Protein Structure。
- MAD rerooting。
- ETE ladderize。
- 原始 FASTA ID 恢复。
- 树图可视化。
- R TreeDist / Robinson-Foulds 距离矩阵。
- tree distance heatmap。
- `tree_summary/analysis_summary.txt`。

Protein Structure 成功 marker：

```text
====Protein structure complete===========================================================
```

### 输出存在性和矩阵一致性检查

执行了 Python 检查：

- 必需输出文件存在且非空。
- `protein_structure/pairwise_scores.tsv` 有 100 条 Foldseek pair rows。
- `protein_structure/similarity_matrix.tsv` 是 10x10。
- `similarity_matrix.tsv` 对称。

检查输出：

```text
Verified 9 required outputs, 100 Foldseek pair rows, 10x10 symmetric matrix
```

### 编译和语法检查

已执行：

```bash
python -m py_compile \
  phylotree_builder_v0.0.1/wrap_protein_pairwise_distance.py \
  phylotree_builder_v0.0.1/onebuilder_runtime_config.py
```

结果：通过。

之前已执行并通过的 Java 检查：

```bash
javac -cp "lib/*:java_tanglegram" -d java_tanglegram \
  java_tanglegram/onebuilder/*.java \
  java_tanglegram/tanglegram/*.java \
  java_tanglegram/tests/OneBuilderStandaloneTest.java \
  java_tanglegram/tests/TanglegramStandaloneTest.java
```

```bash
java -cp "lib/*:java_tanglegram" onebuilder.OneBuilderStandaloneTest
java -cp "lib/*:java_tanglegram" tanglegram.TanglegramStandaloneTest
```

结果：通过。

## 实测结果摘要

Foldseek FZD 结构相似性矩阵显示，FZD paralogs 之间整体结构相似度处于中高区间。

示例值：

- `FZD2` vs `FZD7`：约 `0.735025`。
- `FZD1` vs `FZD7`：约 `0.70145`。
- `FZD9` vs `FZD10`：约 `0.6098`。
- `FZD6` vs `FZD8`：约 `0.446075`。

这些结果符合 FZD 家族同源蛋白预期：整体 fold 相近，但不同 paralog 之间结构相似度有明显层次。

完整矩阵位于：

```text
input_demo/prot_seqs_with_structure/demo_outputs/human_FZD_with_structure_demo/protein_structure/similarity_matrix.tsv
```

## 已发现并修复的问题

### MAFFT Pixi 前缀问题

现象：

```text
v0.000 != v7.525 (2024/Mar/13)
There is a problem in the configuration of your shell.
Check the MAFFT_BINARIES environmental variable
```

原因：

- Pixi 里的 `mafft` wrapper 写死了旧路径 `/opt/BioInfo/phylotree_builder_v0.0.1/.pixi/envs/default/libexec/mafft`。
- 当前 repo 位于 `/mnt/c/Users/.../egps-oneBuilder`，旧前缀不存在。

修复：

- 在 `s1_quick_align.zsh` 中根据当前 `script_dir` 设置 `MAFFT_BINARIES`。

### MAFFT stderr 问题

现象：

```text
line 1291: /dev/stderr: No such device or address
```

原因：

- 当前非交互工具运行环境中，MAFFT wrapper 写 `/dev/stderr` 不稳定。

修复：

- `s1_quick_align.zsh` 将 MAFFT stderr 重定向到 `${output}.mafft.log`。

### IQ-TREE bootstrap 参数问题

现象：

```text
#replicates must be >= 1000
```

原因：

- Demo config 曾将 `bootstrap_replicates` 调低到 `100`，但 IQ-TREE ultrafast bootstrap `-bb` 要求至少 `1000`。

修复：

- `human_FZD_with_structure.onebuilder.json` 将 `bootstrap_replicates` 调回 `1000`。

### structure_manifest_file 相对路径问题

现象：

```text
Protein structure TSV does not exist: .../egps-oneBuilder/human_FZD_structure_mapping.tsv
```

原因：

- Python runtime 按当前工作目录解析 `structure_manifest_file`，而 oneBuilder JSON 中该路径应按 config 文件所在目录解析。

修复：

- `onebuilder_runtime_config.py` 在加载 JSON 时将 `methods.protein_structure.structure_manifest_file` 解析为相对 config 目录的绝对路径。

## 当前实现边界

已完成并验证：

- Protein 输入中启用结构 TSV。
- 每条序列对应一个 PDB/mmCIF 结构文件。
- Foldseek structure-vs-structure all-vs-all。
- oneBuilder GUI 配置、JSON 导出、runtime 解析和 pipeline 集成。
- FZD demo 数据和完整端到端运行。

已实现但仍需进一步评估：

- FASTA-only ProstT5/3Di 分支。
- 大数据集下 ProstT5 权重下载、缓存策略、CPU 耗时和失败恢复。

暂未实现：

- 混合 pair 类型矩阵，即部分序列有结构、部分只有 FASTA。
- chain 选择和链切分。
- US-align 或 TM-align 精确复核后端。
- 将 Protein Structure 结果写入 `analysis_summary.txt` 的方法状态列表；当前结构结果存在于 `protein_structure/`，但 summary 仍只列四种建树方法。

## 后续建议

建议下一步按优先级处理：

1. 在用户文档或 GUI tooltip 中补充结构 TSV 格式说明。
2. 在 `analysis_summary.txt` 中增加 Protein Structure 输出路径摘要。
3. 对 FASTA-only ProstT5 分支做单独耗时和稳定性评估。
4. 如需要坐标级严格 TM-score 复核，再增加 US-align 后端。
5. 如用户需要指定链，再扩展 TSV 第三列 `chain` 并在 wrapper 中做结构预处理。

## 最终结论

第一版选择 Foldseek 是合适的。

本次实现已经完成从用户选择蛋白 FASTA、运行 MAFFT、四种建树方法、Foldseek Protein Structure、可视化和 tree summary 的完整端到端验证。FZD demo 数据也已经加入 `input_demo/prot_seqs_with_structure/`，可作为后续 GUI 和 CLI 回归测试数据。
