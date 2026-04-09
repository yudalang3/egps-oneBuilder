金标准（模拟）基准数据集：单拷贝保守蛋白基因（300 aa）
====================================================

文件列表
--------
1. 蛋白质比对（FASTA，已对齐）：gold_standard_protein_aligned.fasta
2. 编码序列（FASTA，已按密码子对齐）：gold_standard_cds_aligned.fasta
3. 本说明：README_说明.txt

核心特性
--------
- 12 个物种（人、黑猩猩、小鼠、大鼠、牛、犬、鸡、斑胸草雀、爪蟾、斑马鱼、腔棘鱼、果蝇）
- 蛋白长度 300 aa；DNA 长度 900 nt（严格 3 的倍数，无终止密码子）
- 无插入缺失（gapless）但含有充分替换差异（蛋白与人类序列的身份度约 60–95%；DNA 更低）
- DNA 通过同义密码子回译，增加碱基层面的多样性；蛋白/DNA 天然等长，可直接建树
- 因为由已知树形与模型模拟得到，**对齐即为真值**（gold standard）

指南树（Newick）
----------------
(((((Homo_sapiens:0.0065,Pan_troglodytes:0.0065):0.0735,(Mus_musculus:0.014,Rattus_norvegicus:0.014):0.066):0.240,(Bos_taurus:0.082,Canis_lupus_familiaris:0.082):0.238,(Gallus_gallus:0.100,Taeniopygia_guttata:0.100):0.220):0.030,Xenopus_tropicalis:0.350):0.065,Latimeria_chalumnae:0.415):0.015,Danio_rerio:0.430):0.180,Drosophila_melanogaster:0.610);

仿真模型（简要）
----------------
- 祖先蛋白序列按经验氨基酸背景频率随机生成（长度 300）
- 沿上述指南树进行替换：每条分支长度 L 下，位点突变次数 ~ Poisson(μ·L)，每次随机换成不同氨基酸（均匀 19 选 1）
- 全局替换率 μ = 0.7
- 回译采用标准遗传密码表，随机选择同义密码子（引入同义差异）；未模拟插入缺失

快速上手（示例）
----------------
- IQ-TREE（蛋白）： `iqtree2 -s gold_standard_protein_aligned.fasta -m MFP -bb 1000 -nt AUTO`
- IQ-TREE（DNA，密码子分区）：
  `iqtree2 -s gold_standard_cds_aligned.fasta -st CODON -m MFP+MERGE -bb 1000 -nt AUTO`
- RAxML-NG（蛋白）： `raxml-ng --msa gold_standard_protein_aligned.fasta --model JTT+G --bs-trees 200`
- PhyloBayes/MrBayes 等亦可直接使用该比对

与“真实基因”的差异与用途
------------------------
- 本数据集为**可控仿真**，因此适合用于：方法对比、参数调优、脚本联调、管线回归测试
- 若您需要某个**具体真实基因**（例如 FZD8）跨物种的高质量真实比对，我也可以基于公共数据库拉取正交单拷贝并做严格筛选与 MAFFT-L-INS-i / PRANK-codon 对齐

序列摘要（相对于人类序列）
--------------------------
物种	蛋白长度(aa)	蛋白身份度	DNA长度(nt)	DNA身份度
Human	300	100.0%	900	100.0%
Chimp	300	99.7%	900	78.2%
Mouse	300	90.7%	900	71.6%
Rat	300	90.3%	900	71.9%
Cow	300	61.3%	900	56.3%
Dog	300	62.0%	900	56.1%
Chicken	300	66.3%	900	59.3%
Finch	300	65.3%	900	59.1%
Frog	300	60.7%	900	56.6%
Zebrafish	300	57.3%	900	53.3%
Coelacanth	300	61.7%	900	56.2%
Fly	300	45.7%	900	47.6%
