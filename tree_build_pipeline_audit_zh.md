# Tree Build Pipeline 参数与 GUI 审计

这份文档是对当前实现的审计结论，不是另一份参数百科。  
参数总表和 JSON 模板仍以 [tree_build_pipeline_parameter_reference_zh.md](./tree_build_pipeline_parameter_reference_zh.md) 和 [tree_build_full_config_template.json](./tree_build_full_config_template.json) 为准；本文件只回答 4 个问题：

1. 仓库里实际打包的软件版本是什么
2. 当前 GUI 暴露的基础参数 / 高级参数是否合理
3. GUI -> JSON -> wrapper / Python pipeline -> 底层软件 的解析链是否正确
4. 当前有哪些必须修的参数问题

更新说明：

- 下面的原始审计结论已经部分落实。
- 当前代码已经补上 IQ-TREE `Bootstrap` 的推荐/耗时提示。
- 当前代码也已经把一部分 PHYLIP 常用项结构化，尤其是 `dnadist / neighbor / protpars / dnapars` 的高频字段。

## 1. 版本基线

当前版本基线来自 `phylotree_builder_v0.0.1/pixi.toml`：

- `MAFFT 7.525`
- `PHYLIP 3.697`
- `IQ-TREE 3.0.1`
- `MrBayes 3.2.7`

`pixi.lock` 与之匹配：

- `mafft-7.525`
- `iqtree-3.0.1`
- `mrbayes-3.2.7`

说明：

- `PHYLIP` 官方网页文档标题通常写成 `version 3.69`，与这里锁定的 `3.697` 属于同一代文档体系。
- `MrBayes` 官方站点当前公开的网页版本信息更接近 `3.2.7a`，而本仓库锁的是 `3.2.7`。因此 MrBayes 的网页手册可用于核对参数名称和大方向，但更细的子命令语义仍应以程序内 `help` / `manual` 为最终依据。

## 2. 官方手册来源

本次审计使用的主来源：

- MAFFT manual: <https://mafft.cbrc.jp/alignment/software/manual/manual.html>
- IQ-TREE command reference: <https://iqtree.github.io/doc/Command-Reference>
- PHYLIP `protdist`: <https://phylipweb.github.io/phylip/doc/protdist.html>
- PHYLIP `dnadist`: <https://phylipweb.github.io/phylip/doc/dnadist.html>
- PHYLIP `neighbor`: <https://phylipweb.github.io/phylip/doc/neighbor.html>
- PHYLIP `protpars`: <https://phylipweb.github.io/phylip/doc/protpars.html>
- PHYLIP `dnapars`: <https://phylipweb.github.io/phylip/doc/dnapars.html>
- MrBayes manual landing page: <https://nbisweden.github.io/MrBayes/manual.html>

结论范围：

- `MAFFT`、`PHYLIP`、`IQ-TREE` 的 CLI / 菜单参数，当前网页文档足够支撑本次审计。
- `MrBayes` 的 `lset` / `prset` / `mcmcp` / `sumt` 具体细则，网页 landing page 只提供 manual / online help 入口，不是完整命令表。因此本次对 MrBayes 的判断，重点放在“当前代码生成逻辑是否自洽、是否存在明显组合错误”，而不是穷举所有官方选项。

## 3. 当前真实参数链

### 3.1 对齐

链路：

1. `onebuilder` GUI 在 `Input / Align` 页收集输入。
2. `PipelineConfigWriter.java` 把它写进 `alignment` 段。
3. `s1_quick_align.zsh` 读取 `alignment.mafft.common.*` 和 `alignment.mafft.extra_args[]`。
4. wrapper 组装 `mafft` 命令。

当前实现位置：

- `phylotree_builder_v0.0.1/java_tanglegram/onebuilder/InputAlignPanel.java`
- `phylotree_builder_v0.0.1/java_tanglegram/onebuilder/PipelineConfigWriter.java`
- `phylotree_builder_v0.0.1/s1_quick_align.zsh`

### 3.2 建树

链路：

1. GUI 在 `Tree Parameters` 页收集四大类方法参数。
2. `PipelineConfigWriter.java` 写入 `methods.distance / maximum_likelihood / bayesian / parsimony`。
3. `onebuilder_runtime_config.py` 读取 JSON，并把新旧 schema 合并成运行时 settings。
4. `phylo_pipeline_4prot.py` / `phylo_pipeline_4dna.py` 把这些 settings 翻译成：
   - PHYLIP 菜单输入
   - IQ-TREE CLI 参数
   - MrBayes 命令块

当前实现位置：

- `phylotree_builder_v0.0.1/onebuilder_runtime_config.py`
- `phylotree_builder_v0.0.1/phylo_pipeline_4prot.py`
- `phylotree_builder_v0.0.1/phylo_pipeline_4dna.py`
- `phylotree_builder_v0.0.1/java_tanglegram/onebuilder/DistanceMethodPanel.java`
- `phylotree_builder_v0.0.1/java_tanglegram/onebuilder/MaximumLikelihoodPanel.java`
- `phylotree_builder_v0.0.1/java_tanglegram/onebuilder/BayesianPanel.java`
- `phylotree_builder_v0.0.1/java_tanglegram/onebuilder/ParsimonyMethodPanel.java`

## 4. 总体判断

总体结论不是“全错”，而是“主干链路已经通了，但参数层面不均衡”：

- `MAFFT`：基础链路正确，常用参数已经覆盖常见策略、迭代次数、线程和重排序。
- `PHYLIP`：运行链路正确，且常用参数已经补了一层结构化支持，但整体仍明显弱于 IQ-TREE / MrBayes。
- `IQ-TREE`：当前问题最多。部分基础参数是正确的，但 GUI 可选项和 flag 翻译里已经有会影响运行语义的错误。
- `MrBayes`：基本链路可用，默认命令构造逻辑大致合理；`stoprule/stopval` 联动已经修正，剩余问题主要是提示和可用性收紧。

如果只看“能不能跑”，当前设计是能跑的。  
如果看“参数设置是否正确、是否能让 GUI 代表真实软件语义”，当前最薄弱的是 `IQ-TREE`，其次是 `PHYLIP` 的结构化参数层。

## 5. 按软件审计

## 5.1 MAFFT

### 当前 GUI 与运行链

GUI 暴露：

- 基础参数：
  - `strategy`
  - `Maxiterate`
  - `MAFFT threads`
  - `Reorder sequences`
- 高级参数：
  - `extra_args[]`

代码位置：

- GUI：`InputAlignPanel.java`
- JSON：`PipelineConfigWriter.java`
- 运行：`s1_quick_align.zsh`

当前 wrapper 最终命令形态是：

```bash
mafft [--reorder] --<strategy> --maxiterate <N> [--thread <N>] <extra_args...> <input>
```

### 对照官方手册的判断

MAFFT 官方 manual 对这几个核心选项的语义是明确的：

- `--localpair --maxiterate 1000` 对应 L-INS-i
- `--genafpair --maxiterate 1000` 对应 E-INS-i 风格入口
- `--globalpair --maxiterate 1000` 对应 G-INS-i
- `--auto` 自动选策略
- `--reorder` 允许重排序列
- `--maxiterate` 控制迭代轮次
- `--thread` 控制并行线程

### 结论

正确项：

- `strategy / Maxiterate / reorder / extra_args` 的解析链是通的。
- `extra_args` 在命令尾部追加，行为清楚。
- `run_alignment_first=false` 时 MAFFT 不会运行，逻辑正确。

不足项：

- `MAFFT strategy` 仍然是精简版，不是完整策略面。

判断：

- 基础参数：`到位`
- 高级参数：`正确但不完整`
- 运行解析：`正确`

## 5.2 PHYLIP 距离法

### 当前 GUI 与运行链

涉及程序：

- 蛋白：`protdist` + `neighbor`
- DNA/CDS：`dnadist` + `neighbor`

当前 GUI：

- 基础参数只有 `Enable distance method`
- 高级参数只有：
  - `protdist` 或 `dnadist` 的 `menu_overrides[]`
  - `neighbor menu_overrides[]`

当前默认菜单输入：

- `protdist`: `"Y\n"`
- `dnadist`: `"Y\n"`
- `neighbor`: `"Y\n"`

### 对照官方文档的判断

官方文档显示默认语义是：

- `protdist` 默认模型为 JTT
- `dnadist` 默认模型为 F84
- `neighbor` 默认是 Neighbor-Joining，不是 UPGMA

因此当前默认 `"Y"` 的行为是明确的，不是随机行为。

### 结论

正确项：

- 当前默认输入确实等价于“接受 PHYLIP 的默认设置”。
- `menu_overrides[]` 每行一个响应，末尾自动补 `Y` 的处理是合理的。

不足项：

- GUI 几乎没有结构化参数，用户无法直接设置高频项，例如：
  - `protdist` 的模型
  - `dnadist` 的距离模型、transition/transversion ratio、base frequency
  - `neighbor` 的 outgroup / UPGMA / jumble
- 这意味着当前“高级参数”本质上只是菜单脚本输入，不是良好的参数化设计。

判断：

- 基础参数：`严重不足`
- 高级参数：`能用，但本质是原始菜单透传`
- 运行解析：`正确`

## 5.3 PHYLIP 简约法

### 当前 GUI 与运行链

涉及程序：

- 蛋白：`protpars`
- DNA/CDS：`dnapars`

当前 GUI：

- 基础参数包括：
  - `Enable parsimony method`
  - `protpars_outgroup_index`
  - `protpars_print_steps`
  - `protpars_print_sequences`
  - `dnapars_outgroup_index`
  - `dnapars_transversion_parsimony`
- 高级参数仍保留：
  - `protpars menu_overrides[]`
  - `dnapars menu_overrides[]`

当前默认菜单输入：

- `protpars`: `"4\n5\nY\n"`
- `dnapars`: `"Y\n"`

### 对照官方文档的判断

`protpars` 菜单中的 `4` / `5` 控制的是输出内容：

- `4`：打印每个位点的 steps
- `5`：打印节点序列

它们不改变树搜索本身，只改变输出详细度。  
因此当前蛋白简约法默认不是“算法错”，而是“默认开了更多输出”。

`dnapars` 默认 `"Y"` 等价于接受：

- thorough search
- ordinary parsimony
- count all steps

这也是明确的默认行为。

### 结论

正确项：

- 默认菜单输入本身是可解释的，不是错误参数。
- `menu_overrides[]` 的处理链是通的。

不足项：

- 和距离法一样，结构化支持仍然不完整。
- 当前已经补了 `protpars` 输出开关、`protpars/dnapars` 外群和 `dnapars transversion parsimony`，但 `threshold`、`search option` 等仍然没有结构化字段。

判断：

- 基础参数：`已有改进，但仍不完整`
- 高级参数：`能用，但只是菜单透传`
- 运行解析：`正确`

## 5.4 IQ-TREE

### 当前 GUI 与运行链

GUI 暴露：

- 基础参数：
  - `Model strategy`
  - `Bootstrap`
  - `Model set`（仅 protein 启用）
- 高级参数：
  - `threads`
  - `threads_max`
  - `seed`
  - `safe`
  - `keep_ident`
  - `quiet`
  - `verbose`
  - `redo`
  - `memory_limit`
  - `outgroup`
  - `sequence_type`
  - `alrt`
  - `abayes`
  - `extra_args[]`

当前主命令形态：

```bash
iqtree -s <input> -m <model_strategy> -bb <bootstrap_replicates> -pre ml_tree [optional flags...]
```

### 对照官方文档的判断

#### 正确项

- `-nt / -ntmax / -seed / -safe / -keep-ident / -mem / -o / -st / -abayes / -redo` 的映射方向是对的。
- `-mset` 作为模型筛选集合用于蛋白是合理的。
- `-alrt` 与 `-bb` 可以组合，这一点与官方文档一致。

#### 明确问题

1. `quiet` flag 写错

当前代码追加的是 `--quiet`。  
IQ-TREE 官方文档写的是 `-quiet`，不是双横线版本。

这意味着：

- 当前实现至少和官方文档不一致
- 在不同版本上可能被忽略，也可能直接报错

2. `Model strategy` 下拉里包含 `MF` 和 `TESTONLY`

这在 tree-building GUI 里是错误设计。  
官方文档明确：

- `TESTONLY`：只做模型选择
- `MF`：扩展模型选择，不做后续 tree reconstruction
- `MFP`：模型选择后继续建树

而当前 pipeline 不论用户选什么，都会继续期待 `ml_tree.treefile`。  
因此：

- 选 `MF` 或 `TESTONLY` 时，GUI 语义和 pipeline 预期不一致
- 这不是“可选增强”，而是当前可触发的错误配置

3. `Model strategy` 选项没有按输入类型收口

当前固定选项是：

- `MFP`
- `MF`
- `TESTONLY`
- `LG`
- `GTR`

问题：

- `LG` 是蛋白模型，不应直接给 DNA/CDS 输入类型当常规固定模型
- `GTR` 是 DNA 模型，不应直接给蛋白输入类型当常规固定模型

这会让 GUI 生成输入类型不匹配的模型配置。

4. `alrt=0` 无法表达

官方文档明确：

- `-alrt 0` 不是“关闭”
- 而是执行 parametric aLRT

当前 GUI 用 `0` 作为 spinner 的“空值”，再在 `toConfig()` 中把 `<=0` 转成 `null`。  
结果是：

- `alrt 0` 的官方语义丢失

5. `Bootstrap` 缺少基本验证

官方文档对 `-bb` 的说明是“`>=1000`”。  
当前 GUI 允许填 `0`，代码也会照样组装 `-bb 0`。

这至少说明当前 GUI 没有按照官方推荐范围做基本约束。

### 结论

判断：

- 基础参数：`部分到位，但包含错误选项`
- 高级参数：`大部分方向正确，但有关键 flag / 约束问题`
- 运行解析：`存在真实错误`

## 5.5 MrBayes

### 当前 GUI 与运行链

GUI 暴露：

- 基础参数：
  - `rates`
  - `ngen`
  - `samplefreq`
  - `printfreq`
  - `diagnfreq`
  - protein 时 `protein_model_prior`
  - DNA/CDS 时 `nst`
- 高级参数：
  - `nruns`
  - `nchains`
  - `temp`
  - `stoprule`
  - `stopval`
  - `burnin`
  - `burninfrac`
  - `relburnin`
  - `command_block[]`

当前默认命令生成：

- 蛋白：
  - `prset aamodelpr=...;`
  - `lset rates=...;`
  - `mcmcp ...;`
  - `mcmc;`
  - `sumt ...;`
- DNA/CDS：
  - `lset nst=... rates=...;`
  - `mcmcp ...;`
  - `mcmc;`
  - `sumt ...;`

如果 `command_block[]` 非空，则当前 pipeline 把它视为权威输入，不再生成默认命令块。

### 对照官方手册入口与当前实现的判断

#### 正确项

- 蛋白把 `protein_model_prior` 翻译成 `prset aamodelpr=...`，方向正确。
- DNA/CDS 把 `nst` 和 `rates` 放进 `lset`，方向正确。
- `ngen / samplefreq / printfreq / diagnfreq / nruns / nchains / temp / stoprule / stopval` 放进 `mcmcp`，结构是自洽的。
- `burnin / burninfrac / relburnin` 放进 `sumt`，结构方向正确。
- `command_block[]` 非空即接管默认命令生成，这个行为清楚且合理。

#### 当前剩余问题

1. `burnin`、`burninfrac`、`relburnin` 仍然缺少更明确的互斥或依赖提示

当前 `sumt` 生成逻辑允许它们同时存在。  
本次网页资料不足以直接宣称“这一定非法”，但从 GUI 设计角度，这组参数至少缺少联动说明。

2. `protein_model_prior` 虽然已改成可编辑下拉，但仍允许自由文本

这不一定错误，因为 MrBayes 的 `aamodelpr=` 本来就有多种值。  
但它会降低 GUI 可用性，也增加拼写错误风险。

### 结论

判断：

- 基础参数：`到位`
- 高级参数：`基本到位，但仍可继续收紧`
- 运行解析：`主链正确，组合约束不足`

## 6. GUI 合理性审计

## 6.1 整体结构

从产品结构上看，当前 oneBuilder 把参数设置和运行页分开，这个方向是对的：

- `Input / Align`
- `Tree Parameters`
- `Tree Build`
- `Tanglegram`

这比把所有参数和运行状态混在一个页面里更清楚。

## 6.2 基础参数是否到位

结论按模块分：

- `MAFFT`：`够用，但不是完整常用面`
- `PHYLIP` 距离法 / 简约法：`不够`
- `IQ-TREE`：`覆盖面够，但选项收口错误`
- `MrBayes`：`基本到位`

换句话说：

- 目前 GUI 最像“正式参数面板”的是 `IQ-TREE` 和 `MrBayes`
- 最像“临时高级输入框”的是 `PHYLIP`

## 6.3 高级参数是否正确

结论：

- `MAFFT extra_args`：正确
- `PHYLIP menu_overrides`：技术上正确，但设计层次太低
- `IQ-TREE advanced`：方向大体对，但有关键错误和缺少约束
- `MrBayes advanced`：主链可用，剩余问题主要是提示和可用性收紧

## 7. JSON 与运行解析审计

## 7.1 正确项

- `PipelineConfigWriter.java` 输出的 JSON 结构与 `onebuilder_runtime_config.py` 的读取方式是对得上的。
- `common / advanced / extra_args / menu_overrides / command_block` 这套 schema 当前已经贯通。
- 旧 schema 兼容逻辑也还在。

## 7.2 解析链问题

### IQ-TREE

- `quiet` 被翻译成 `--quiet`，与官方文档不一致
- `MF` / `TESTONLY` 被当作 tree-building 模式使用
- `alrt=0` 语义丢失

### MrBayes

- `stoprule=false` 时已不再写出 `stopval`

### PHYLIP

- 解析本身没错
- 问题不在解析，而在 GUI 结构化能力过弱

## 8. 问题清单与优先级

## Critical

1. IQ-TREE `Model strategy` 暴露了 `MF` 和 `TESTONLY`
   - 这两者是 model-selection-only 语义
   - 当前 pipeline 却固定把它们当成会产出树的建树模式
   - 必须从 tree-building GUI 中移除，或单独设计成“只做模型选择”的工作流

## High

1. IQ-TREE `quiet` flag 当前写成 `--quiet`
   - 官方文档写的是 `-quiet`
   - 应改为文档一致的单横线形式

2. IQ-TREE `Model strategy` 没有按输入类型收口
   - DNA/CDS 不应暴露 `LG`
   - Protein 不应暴露 `GTR`

## Medium

1. DNA/CDS 模式下 `model_set` 被完全隐藏，虽然运行层支持
2. PHYLIP 距离法 / 简约法仍缺少更多结构化基础参数
3. MrBayes `burnin / burninfrac / relburnin` 缺少组合说明或进一步联动

## Low

1. `protein_model_prior` 虽然已有建议值，但仍允许自由文本，仍可能拼写错
2. PHYLIP 当前的“高级参数”名称虽然正确，但本质仍是原始菜单脚本，不够直观

## 9. 建议的修复顺序

第一批必须先改：

1. 收紧 IQ-TREE `Model strategy`
   - tree-building GUI 只保留真正会建树的选项
   - 按输入类型拆分 protein / DNA 可选模型
2. 把 IQ-TREE `quiet` 从 `--quiet` 改成 `-quiet`
3. 修正 MrBayes `stoprule` / `stopval` 联动

第二批应该补：

1. 继续给 PHYLIP 增加一层真正的结构化常用参数，而不是只靠 `menu_overrides`
2. 继续收紧 MrBayes 的 burnin 相关提示

第三批再做增强：

1. 继续扩 MAFFT 常用策略
2. 对 MrBayes 高级参数增加更明确的互斥 / 依赖提示
3. 对剩余自由文本参数继续改成下拉或带提示的输入

## 10. 最终结论

如果问题是“现在这个 builder GUI 的参数设置是否整体靠谱”，答案是：

- `MAFFT`：靠谱，常用参数面已经比初版完整
- `PHYLIP`：能跑，但参数层设计偏原始
- `IQ-TREE`：当前不完全靠谱，已经有需要尽快修的错误
- `MrBayes`：基本靠谱，主链联动问题已收敛，剩余是可用性层面的收紧

如果问题是“运行解析是否正确”，答案是：

- 大部分链路是正确的
- 当前主要剩余问题已经从 `IQ-TREE` 的明显错误，收缩到：
  - `Bootstrap` 合理性提示不足
  - `PHYLIP` 结构化参数仍不够完整
  - `MrBayes` 的 burnin 系列参数还可以继续收紧

因此当前最准确的总体评价是：

`这套 GUI 已经具备可用且基本自洽的参数驱动能力，但在 PHYLIP 的结构化支持和少量参数提示层面仍有继续打磨空间。`
