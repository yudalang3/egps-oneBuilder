# input_demo 数据说明

本目录保存 oneBuilder 的示例输入、配置和真实生物数据演示集。当前与蛋白结构树相关的两个真实数据集是：

- `prot_seqs_fzd_human/`：10 条人源 Frizzled/FZD 受体旁系同源蛋白。
- `prot_seqs_wnt_human/`：19 条人源 WNT 蛋白，加上 `NDP`/Norrin，共 20 条序列。

## FZD 示例集

目录：`prot_seqs_fzd_human/`

主要文件：

- `human_FZD_paralogs_protein.fasta`：FZD1-FZD10 的 reviewed UniProt 蛋白序列。
- `human_FZD_paralogs_cds.fasta`：与上述蛋白对应的 Ensembl CDS 序列。
- `human_FZD_metadata.tsv`：基因名、UniProt accession、序列 ID、长度、结构文件和结构来源。
- `human_FZD_structure_mapping.tsv`：oneBuilder 蛋白结构模块使用的序列 ID 到 mmCIF 文件映射。
- `human_FZD_with_structure.onebuilder.json`：可直接导入 oneBuilder 的蛋白树和结构树配置。
- `structures/*.cif`：AlphaFold DB full-length mmCIF model_v6 结构文件。

处理过程：

1. 原目录 `prot_seqs_with_structure/` 重命名为 `prot_seqs_fzd_human/`，避免名称过泛。
2. 原蛋白 FASTA `human_FZD_paralogs.fasta` 重命名为 `human_FZD_paralogs_protein.fasta`。
3. 更新 `human_FZD_with_structure.onebuilder.json`，使 `run.input_file` 指向 `human_FZD_paralogs_protein.fasta`。
4. 保留原有 FZD metadata、结构映射和 AlphaFold mmCIF 文件。
5. 补充 `human_FZD_paralogs_cds.fasta`。CDS 通过 UniProt reviewed entry 中的 Ensembl cross-reference 选择对应 transcript，再用 Ensembl REST `sequence/id/<transcript>?type=cds` 获取。

为什么之前 FZD 没有 CDS：早先的 FZD 示例集只服务于蛋白序列树和蛋白结构树，所以没有 CDS FASTA。为了和 WNT 示例集保持一致，现在已经补齐 FZD CDS 序列。

## WNT/Norrin 示例集

目录：`prot_seqs_wnt_human/`

主要文件：

- `human_WNT_paralogs_protein.fasta`：19 条人源 WNT reviewed UniProt 蛋白序列，加 `NDP`/Norrin。
- `human_WNT_paralogs_cds.fasta`：与上述蛋白对应的 Ensembl CDS 序列。
- `human_WNT_metadata.tsv`：基因名、UniProt accession、序列 ID、长度、结构文件和结构来源。
- `human_WNT_structure_mapping.tsv`：oneBuilder 蛋白结构模块使用的序列 ID 到 mmCIF 文件映射。
- `human_WNT_with_structure.onebuilder.json`：可直接导入 oneBuilder 的蛋白树和结构树配置。
- `structures/*.cif`：AlphaFold DB full-length mmCIF model_v6 结构文件。

处理过程：

1. 保留目录名 `prot_seqs_wnt_human/`。
2. 将旧文件 `Wnt.and.norrin.fa` 规范化为 `human_WNT_paralogs_protein.fasta`。
3. 用 UniProt reviewed human entries 作为蛋白序列和 accession 来源。
4. FASTA header 统一为 oneBuilder/FZD 风格：`<UniProtEntry>_<Accession> <Gene> <Accession> ...`。
5. 用 AlphaFold DB URL 模式下载结构：`https://alphafold.ebi.ac.uk/files/AF-<accession>-F1-model_v6.cif`。
6. 用 UniProt entry 中的 Ensembl cross-reference 找到对应 transcript，再用 Ensembl REST 获取 CDS FASTA。
7. 生成 metadata、结构映射和 oneBuilder JSON 配置。

注意：UniProt reviewed entry 中 WNT10A/WNT10B 的 entry ID 是 `WN10A_HUMAN` 和 `WN10B_HUMAN`，所以对应 sequence ID 和结构文件名也保留该官方写法。

## 数据来源

- UniProt reviewed human entries：蛋白序列、accession、entry ID、Ensembl cross-reference。
- AlphaFold DB：full-length mmCIF model_v6 结构文件。
- Ensembl REST：CDS FASTA。

## 生成文件与忽略规则

源数据和配置文件应保留在版本库中，包括 FASTA、TSV、JSON 和 `structures/*.cif`。

运行 oneBuilder 或 MAFFT 后产生的结果不作为源数据维护，已在仓库根目录 `.gitignore` 中忽略：

- `input_demo/prot_seqs_fzd_human/demo_outputs/`
- `input_demo/prot_seqs_fzd_human/foldseek_structure_similarity/`
- `input_demo/prot_seqs_fzd_human/*.aligned.*`
- `input_demo/prot_seqs_wnt_human/demo_outputs/`
- `input_demo/prot_seqs_wnt_human/foldseek_structure_similarity/`
- `input_demo/prot_seqs_wnt_human/*.aligned.*`

## 快速验证

可在仓库根目录运行以下检查：

```powershell
(Select-String -Path input_demo\prot_seqs_fzd_human\human_FZD_paralogs_protein.fasta -Pattern '^>').Count
(Select-String -Path input_demo\prot_seqs_fzd_human\human_FZD_paralogs_cds.fasta -Pattern '^>').Count
(Select-String -Path input_demo\prot_seqs_wnt_human\human_WNT_paralogs_protein.fasta -Pattern '^>').Count
(Select-String -Path input_demo\prot_seqs_wnt_human\human_WNT_paralogs_cds.fasta -Pattern '^>').Count
```

预期结果分别为 `10`、`10`、`20`、`20`。
