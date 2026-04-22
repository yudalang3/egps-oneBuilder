# Tree Build 参数参考与 JSON 设计

## 范围

这份文档只覆盖 `Tree Build` 阶段真正涉及的软件与参数：

- `MAFFT`
- `PHYLIP protdist / dnadist / neighbor / protpars / dnapars`
- `IQ-TREE`
- `MrBayes`

不包含：

- `MAD` 定根
- `TreeDist / Robinson-Foulds` 距离统计
- 热图和其他后处理可视化

平台边界也要先说清楚：

- 真正执行比对和建树，只支持 Linux。
- Windows 下的 `onebuilder.launcher` 只负责导出 JSON。
- Windows 下查看已有结果，请用 `tanglegram.launcher`。

## 官方来源

以下是本次参数整理使用的主来源，都是官方文档或官方站点：

- MAFFT manual: <https://mafft.cbrc.jp/alignment/software/manual/manual.html>
- MAFFT home / usage: <https://mafft.cbrc.jp/alignment/software/index.html>
- IQ-TREE command reference: <https://iqtree.github.io/doc/Command-Reference>
- PHYLIP main documentation: <https://phylipweb.github.io/phylip/doc/main.html>
- PHYLIP `protdist`: <https://phylipweb.github.io/phylip/doc/protdist.html>
- PHYLIP `dnadist`: <https://phylipweb.github.io/phylip/doc/dnadist.html>
- PHYLIP `neighbor`: <https://phylipweb.github.io/phylip/doc/neighbor.html>
- PHYLIP `protpars`: <https://phylipweb.github.io/phylip/doc/protpars.html>
- PHYLIP `dnapars`: <https://phylipweb.github.io/phylip/doc/dnapars.html>
- MrBayes manual: <https://nbisweden.github.io/MrBayes/manual.html>

## 当前真实调用链

当前仓库里，Tree Build 的真实执行链是：

1. `onebuilder.launcher` 导出运行时 JSON。
2. 如果勾选 `Run alignment first`，先调用 `s1_quick_align.zsh`。
3. 再调用 `s2_phylo_4prot.zsh` 或 `s2_phylo_4dna.zsh`。
4. wrapper 把 `--config` 传入 `phylo_pipeline_4prot.py` 或 `phylo_pipeline_4dna.py`。
5. Python 管线根据 JSON 组装底层软件调用。

其中当前仓库的参数消费方式是：

- MAFFT：`s1_quick_align.zsh` 读取 `alignment` 段。
- PHYLIP：Python 管线从 `menu_overrides` 读取交互式菜单输入序列。
- IQ-TREE：Python 管线消费结构化字段，并把 `extra_args` 直接追加到命令尾部。
- MrBayes：Python 管线消费结构化字段；如果 `command_block` 非空，则改为使用原生命令块。

## JSON 设计规则

JSON 设计分两档：

- `common`: 常用参数，适合直接放在 GUI 里。
- `advanced`: 高级参数，适合少量高级用户调整。

同时保留 3 个原生透传通道，保证官方参数面不被 GUI 结构化字段限制住：

- `extra_args`: 适用于命令行软件，当前用于 `MAFFT` 和 `IQ-TREE`
- `menu_overrides`: 适用于 PHYLIP 这类菜单式程序
- `command_block`: 适用于 `MrBayes` 的原生命令块

优先级与行为：

- `enabled=false` 会直接跳过该方法。
- `MAFFT extra_args` 会在常用参数之后、输入文件之前追加。
- `IQ-TREE extra_args` 会在结构化参数之后追加；如果你手动追加了会覆盖前面语义的 flag，以 IQ-TREE 自身解析结果为准。
- `PHYLIP menu_overrides` 一旦提供，就由你自己控制交互菜单序列；程序只会在末尾缺少 `Y` 时自动补一个 `Y`。
- `MrBayes command_block` 一旦提供，就不再自动生成默认的 `lset/mcmcp/mcmc/sumt` 组合；你需要自己在 block 里写完整。

## 常用参数

| 软件 | JSON 路径 | 官方参数 / 命令 | 适用范围 | 当前状态 | 说明 |
| --- | --- | --- | --- | --- | --- |
| MAFFT | `alignment.mafft.common.strategy` | `--auto` / `--localpair` / `--genafpair` / `--globalpair` | Both | 已支持 | 当前 GUI 已暴露 |
| MAFFT | `alignment.mafft.common.maxiterate` | `--maxiterate` | Both | 已支持 | 当前 GUI 已暴露 |
| MAFFT | `alignment.mafft.common.threads` | `--thread` | Both | 已支持 | 当前 GUI 已暴露 |
| MAFFT | `alignment.mafft.common.reorder` | `--reorder` | Both | 已支持 | `false` 时相当于不加 `--reorder` |
| Distance | `methods.distance.enabled` | 方法开关 | Both | 已支持 | 仅控制是否运行 |
| IQ-TREE | `methods.maximum_likelihood.enabled` | 方法开关 | Both | 已支持 | 仅控制是否运行 |
| IQ-TREE | `methods.maximum_likelihood.iqtree.common.bootstrap_replicates` | `-bb` | Both | 已支持 | 当前 GUI 已暴露 |
| IQ-TREE | `methods.maximum_likelihood.iqtree.common.model_strategy` | `-m` | Both | 已支持 | 当前 GUI 已暴露 |
| IQ-TREE | `methods.maximum_likelihood.iqtree.common.model_set` | `-mset` | Protein 优先 | 已支持 | DNA 也可传，但通常留空 |
| MrBayes | `methods.bayesian.enabled` | 方法开关 | Both | 已支持 | 仅控制是否运行 |
| MrBayes | `methods.bayesian.mrbayes.common.rates` | `lset rates=` | Both | 已支持 | 当前 GUI 已暴露 |
| MrBayes | `methods.bayesian.mrbayes.common.ngen` | `mcmcp ngen=` | Both | 已支持 | 当前 GUI 已暴露 |
| MrBayes | `methods.bayesian.mrbayes.common.samplefreq` | `mcmcp samplefreq=` | Both | 已支持 | 当前 GUI 已暴露 |
| MrBayes | `methods.bayesian.mrbayes.common.printfreq` | `mcmcp printfreq=` | Both | 已支持 | 当前 GUI 已暴露 |
| MrBayes | `methods.bayesian.mrbayes.common.diagnfreq` | `mcmcp diagnfreq=` | Both | 已支持 | 当前 GUI 已暴露 |
| MrBayes | `methods.bayesian.mrbayes.common.protein_model_prior` | `prset aamodelpr=` | Protein | 已支持 | 当前 GUI 已暴露 |
| MrBayes | `methods.bayesian.mrbayes.common.nst` | `lset nst=` | DNA/CDS | 已支持 | 当前 GUI 已暴露 |
| Parsimony | `methods.parsimony.enabled` | 方法开关 | Both | 已支持 | 仅控制是否运行 |

## 高级参数

| 软件 | JSON 路径 | 官方参数 / 命令 | 适用范围 | 当前状态 | 说明 |
| --- | --- | --- | --- | --- | --- |
| MAFFT | `alignment.mafft.extra_args[]` | 任意官方 CLI 参数 | Both | 已支持 | 完整透传 |
| PHYLIP protdist | `methods.distance.protdist.menu_overrides[]` | 交互菜单序列 | Protein | 已支持 | 完整透传 |
| PHYLIP dnadist | `methods.distance.dnadist.menu_overrides[]` | 交互菜单序列 | DNA/CDS | 已支持 | 完整透传 |
| PHYLIP neighbor | `methods.distance.neighbor.menu_overrides[]` | 交互菜单序列 | Both | 已支持 | 完整透传 |
| IQ-TREE | `methods.maximum_likelihood.iqtree.advanced.threads` | `-nt` | Both | 已支持 | 线程数，支持 `AUTO` |
| IQ-TREE | `methods.maximum_likelihood.iqtree.advanced.threads_max` | `-ntmax` | Both | 已支持 | 最大线程上限 |
| IQ-TREE | `methods.maximum_likelihood.iqtree.advanced.seed` | `-seed` | Both | 已支持 | 随机种子 |
| IQ-TREE | `methods.maximum_likelihood.iqtree.advanced.safe` | `-safe` | Both | 已支持 | 安全模式 |
| IQ-TREE | `methods.maximum_likelihood.iqtree.advanced.keep_ident` | `-keep-ident` | Both | 已支持 | 保留重复序列 |
| IQ-TREE | `methods.maximum_likelihood.iqtree.advanced.quiet` | `-quiet` | Both | 已支持 | 默认为 `true` |
| IQ-TREE | `methods.maximum_likelihood.iqtree.advanced.verbose` | `-v` | Both | 已支持 | 仅在 `quiet=false` 时才有意义 |
| IQ-TREE | `methods.maximum_likelihood.iqtree.advanced.redo` | `-redo` | Both | 已支持 | 默认为 `true` |
| IQ-TREE | `methods.maximum_likelihood.iqtree.advanced.memory_limit` | `-mem` | Both | 已支持 | 例如 `8G` |
| IQ-TREE | `methods.maximum_likelihood.iqtree.advanced.outgroup` | `-o` | Both | 已支持 | 指定外群 |
| IQ-TREE | `methods.maximum_likelihood.iqtree.advanced.sequence_type` | `-st` | Both | 已支持 | 手动指定序列类型 |
| IQ-TREE | `methods.maximum_likelihood.iqtree.advanced.alrt` | `-alrt` | Both | 已支持 | SH-aLRT 支持度 |
| IQ-TREE | `methods.maximum_likelihood.iqtree.advanced.abayes` | `-abayes` | Both | 已支持 | aBayes 支持度 |
| IQ-TREE | `methods.maximum_likelihood.iqtree.extra_args[]` | 任意官方 CLI 参数 | Both | 已支持 | 完整透传 |
| MrBayes | `methods.bayesian.mrbayes.advanced.nruns` | `mcmcp nruns=` | Both | 已支持 | 并行运行数 |
| MrBayes | `methods.bayesian.mrbayes.advanced.nchains` | `mcmcp nchains=` | Both | 已支持 | 每个 run 的链数 |
| MrBayes | `methods.bayesian.mrbayes.advanced.temp` | `mcmcp temp=` | Both | 已支持 | heated chains 温度 |
| MrBayes | `methods.bayesian.mrbayes.advanced.stoprule` | `mcmcp stoprule=` | Both | 已支持 | 是否启用自动停止 |
| MrBayes | `methods.bayesian.mrbayes.advanced.stopval` | `mcmcp stopval=` | Both | 已支持 | 自动停止阈值 |
| MrBayes | `methods.bayesian.mrbayes.advanced.burnin` | `sumt burnin=` | Both | 已支持 | 固定 burnin 样本数 |
| MrBayes | `methods.bayesian.mrbayes.advanced.burninfrac` | `sumt burninfrac=` | Both | 已支持 | burnin 比例 |
| MrBayes | `methods.bayesian.mrbayes.advanced.relburnin` | `sumt relburnin=` | Both | 已支持 | 是否按比例解释 burnin |
| MrBayes | `methods.bayesian.mrbayes.command_block[]` | 任意 MrBayes 命令 | Both | 已支持 | 完整透传，并覆盖默认命令生成 |
| PHYLIP protpars | `methods.parsimony.protpars.menu_overrides[]` | 交互菜单序列 | Protein | 已支持 | 完整透传 |
| PHYLIP dnapars | `methods.parsimony.dnapars.menu_overrides[]` | 交互菜单序列 | DNA/CDS | 已支持 | 完整透传 |

## 软件分项说明

### 1. MAFFT

当前仓库直接结构化支持的常用参数：

| JSON 路径 | 官方参数 | 档位 | 说明 |
| --- | --- | --- | --- |
| `alignment.mafft.common.strategy` | `--auto` / `--localpair` / `--genafpair` / `--globalpair` | 常用 | 当前 GUI 已给出常用预设 |
| `alignment.mafft.common.maxiterate` | `--maxiterate` | 常用 | 迭代次数 |
| `alignment.mafft.common.threads` | `--thread` | 常用 | 并行线程数，`0`/空值表示用 MAFFT 默认 |
| `alignment.mafft.common.reorder` | `--reorder` | 常用 | 是否允许重排序 |

建议归入高级档但不逐个结构化的官方参数，当前统一走 `alignment.mafft.extra_args[]`：

- `--retree`
- `--ep`
- `--op`
- `--lexp`
- `--bl`
- 以及 MAFFT 官方手册中其他命令行参数

说明：

- 当前 `onebuilder.launcher` 导出的 JSON 已可驱动对齐步骤读取这些常用参数。
- `extra_args[]` 也会被 `s1_quick_align.zsh --config ...` 读取并透传给 MAFFT。

### 2. PHYLIP 距离法

当前距离法分两步：

- 蛋白：`protdist` -> `neighbor`
- DNA/CDS：`dnadist` -> `neighbor`

当前仓库对 PHYLIP 的策略是：

- `enabled` 负责控制是否运行。
- 具体菜单项不做大量 Java 表单化。
- 需要完整控制时，直接写 `menu_overrides[]`。

默认菜单序列：

- `protdist`: `Y`
- `dnadist`: `Y`
- `neighbor`: `Y`

建议：

- 如果你只是想用仓库默认行为，不要写 `menu_overrides[]`。
- 如果你要改模型、随机种子、jumble、outgroup、距离修正方式等，请直接按 PHYLIP 官方交互顺序写进 `menu_overrides[]`。

### 3. IQ-TREE

当前结构化支持如下：

| JSON 路径 | 官方参数 | 档位 | 说明 |
| --- | --- | --- | --- |
| `...iqtree.common.bootstrap_replicates` | `-bb` | 常用 | UFBoot 次数 |
| `...iqtree.common.model_strategy` | `-m` | 常用 | 如 `MFP` |
| `...iqtree.common.model_set` | `-mset` | 常用 | 蛋白流程更常见 |
| `...iqtree.advanced.threads` | `-nt` | 高级 | 线程数或 `AUTO` |
| `...iqtree.advanced.threads_max` | `-ntmax` | 高级 | 最大线程数 |
| `...iqtree.advanced.seed` | `-seed` | 高级 | 随机种子 |
| `...iqtree.advanced.safe` | `-safe` | 高级 | 安全模式 |
| `...iqtree.advanced.keep_ident` | `-keep-ident` | 高级 | 保留相同序列 |
| `...iqtree.advanced.quiet` | `-quiet` | 高级 | 静默输出 |
| `...iqtree.advanced.verbose` | `-v` | 高级 | 详细输出 |
| `...iqtree.advanced.redo` | `-redo` | 高级 | 重跑现有前缀 |
| `...iqtree.advanced.memory_limit` | `-mem` | 高级 | 内存上限 |
| `...iqtree.advanced.outgroup` | `-o` | 高级 | 指定外群 |
| `...iqtree.advanced.sequence_type` | `-st` | 高级 | 手动指定序列类型 |
| `...iqtree.advanced.alrt` | `-alrt` | 高级 | SH-aLRT |
| `...iqtree.advanced.abayes` | `-abayes` | 高级 | aBayes |
| `...iqtree.extra_args[]` | 任意官方参数 | 高级 | 完整透传 |

这意味着：

- 日常常用参数已经有稳定 JSON key。
- 其余 IQ-TREE 官方参数仍然建议直接放到 `extra_args[]` 里。
- 例如 `-bnni`、`-wbtl`、`-ninit`、`-nstop`、`-czb`、`-pers`、`-fast` 等，都可以原样追加。

### 4. MrBayes

当前结构化支持如下：

| JSON 路径 | MrBayes 命令 | 档位 | 说明 |
| --- | --- | --- | --- |
| `...mrbayes.common.protein_model_prior` | `prset aamodelpr=` | 常用 | 仅蛋白 |
| `...mrbayes.common.nst` | `lset nst=` | 常用 | 仅 DNA/CDS |
| `...mrbayes.common.rates` | `lset rates=` | 常用 | 两条流程都用 |
| `...mrbayes.common.ngen` | `mcmcp ngen=` | 常用 | 代数 |
| `...mrbayes.common.samplefreq` | `mcmcp samplefreq=` | 常用 | 采样频率 |
| `...mrbayes.common.printfreq` | `mcmcp printfreq=` | 常用 | 打印频率 |
| `...mrbayes.common.diagnfreq` | `mcmcp diagnfreq=` | 常用 | 诊断频率 |
| `...mrbayes.advanced.nruns` | `mcmcp nruns=` | 高级 | 独立 run 数 |
| `...mrbayes.advanced.nchains` | `mcmcp nchains=` | 高级 | 链数 |
| `...mrbayes.advanced.temp` | `mcmcp temp=` | 高级 | heated chain 温度 |
| `...mrbayes.advanced.stoprule` | `mcmcp stoprule=` | 高级 | 自动停止 |
| `...mrbayes.advanced.stopval` | `mcmcp stopval=` | 高级 | 停止阈值 |
| `...mrbayes.advanced.burnin` | `sumt burnin=` | 高级 | 固定 burnin |
| `...mrbayes.advanced.burninfrac` | `sumt burninfrac=` | 高级 | 比例 burnin |
| `...mrbayes.advanced.relburnin` | `sumt relburnin=` | 高级 | 是否比例解释 |
| `...mrbayes.command_block[]` | 任意 MrBayes 命令 | 高级 | 完整透传 |

`command_block[]` 的用法要特别注意：

- 只要它非空，程序就不会再自动生成默认 `lset`、`mcmcp`、`mcmc`、`sumt`。
- 这时你应该自己写完整的分析命令。
- 适合需要 `charset`、`partition`、`unlink`、`prset`、更复杂 `mcmcp` 或者自定义 `sumt/sump` 行为的情况。

当前 GUI 已做的收紧：

- `stoprule=false` 时不会导出 `stopval`
- `relburnin` 只有在 `burninfrac > 0` 时才会保留
- `protein_model_prior` 现在是可编辑下拉框，带常见建议值，但仍允许手动输入

### 5. PHYLIP 简约法

当前策略与距离法一致：

- `enabled` 控制是否运行。
- `protpars` / `dnapars` 的详细菜单项统一走 `menu_overrides[]`。

默认菜单序列：

- `protpars`: `4`, `5`, `Y`
- `dnapars`: `Y`

如果你要控制：

- jumble
- outgroup
- threshold
- transversion parsimony
- ancestral states
- 多棵最简树输出策略

就直接按 PHYLIP 官方交互顺序写进 `menu_overrides[]`。

## 当前仓库默认值

### Protein

- MAFFT: `strategy=localpair`, `maxiterate=1000`, `threads=null`, `reorder=true`
- IQ-TREE: `bootstrap_replicates=1000`, `model_strategy=MFP`
- IQ-TREE `model_set`: `WAG,LG,JTT,Dayhoff,DCMut,rtREV,cpREV,VT,Blosum62,mtMam,mtArt,HIVb,HIVw`
- MrBayes: `protein_model_prior=mixed`, `rates=invgamma`, `ngen=50000`, `samplefreq=100`, `printfreq=1000`, `diagnfreq=5000`
- PHYLIP distance: 默认菜单 `Y`
- PHYLIP parsimony: 默认菜单 `4 5 Y`

### DNA/CDS

- MAFFT: `strategy=localpair`, `maxiterate=1000`, `threads=null`, `reorder=true`
- IQ-TREE: `bootstrap_replicates=1000`, `model_strategy=MFP`
- MrBayes: `nst=6`, `rates=invgamma`, `ngen=10000`, `samplefreq=100`, `printfreq=100`, `diagnfreq=1000`
- PHYLIP distance: 默认菜单 `Y`
- PHYLIP parsimony: 默认菜单 `Y`

## 推荐编辑方式

1. 先用 `onebuilder.launcher` 导出一份 JSON。
2. 只改你要改的参数，不要先重排整个文件。
3. 日常使用优先改 `common`。
4. 高级调参优先改 `advanced`。
5. 只有在结构化字段不够时，再用 `extra_args`、`menu_overrides`、`command_block`。

## 配套文件

- 完整模板：[`tree_build_full_config_template.json`](./tree_build_full_config_template.json)
- GUI 导出文件：`<output_base_dir>/<output_prefix>.onebuilder.json`

## 兼容性说明

- 旧版较扁平的 JSON 仍然可以被当前 Python 配置桥读取。
- 新版导出和这份模板采用的是分层 schema。
- 对齐、建树和纠缠图查看三部分的功能边界仍然不变：
  - Linux：可真正执行
  - Windows：导出 JSON + 查看已有结果
