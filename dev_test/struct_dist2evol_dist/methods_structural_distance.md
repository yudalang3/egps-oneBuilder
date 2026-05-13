# 2. Materials and Methods (材料与方法)

## 2.X Protein Structure-Based Evolutionary Distance Calculation

### English Version
To infer phylogenetic relationships from protein structures, structural similarities must be transformed into distances with care. In the eGPS-oneBuilder pipeline, pairwise structural alignments are performed using Foldseek (van Kempen et al., 2023), and the structural similarity between two proteins is quantified using the Template Modeling score (TM-score). The TM-score is a length-independent metric for assessing topological similarity, where a value of 1.0 indicates a perfect match and values below ~0.17 are generally indistinguishable from random structural similarity (Zhang and Skolnick, 2004; Xu and Zhang, 2010; Zhang et al., 2022). To ensure symmetry, we compute the pairwise similarity $S$ as the arithmetic mean of the query-normalized TM-score (qTM) and the target-normalized TM-score (tTM).

A naive linear transformation from structural similarity to distance ($d = 1 - S$) fails to account for the saturation effect of structural divergence over long evolutionary timescales, which can severely compress the branch lengths of distantly related taxa and distort the inferred tree topology. To address this, we implemented a logarithmic transformation analogous to the Jukes-Cantor model of nucleotide substitution. By incorporating the empirical random baseline of TM-score ($TM_{random} = 0.17$), the structural evolutionary distance $d$ is calculated as:

$$ d = -\ln \left( \frac{S - 0.17}{1.0 - 0.17} \right) $$

For numerical stability, the argument of the logarithm is clamped to a minimum of $10^{-6}$. This produces a finite saturated maximum distance of:

$$ d_{max} = -\ln(10^{-6}) = 13.81551056 $$

The constants in this transformation have different interpretations. The value 0.17 is the empirical TM-score random-similarity baseline supported by the TM-score literature; 0.83 is not an independent biological parameter, but simply the normalization term $1.0 - 0.17$; and 13.81551056 is not a literature-derived evolutionary coefficient, but the numerical cap implied by the $10^{-6}$ floor. Missing Foldseek hits are assigned this saturated distance by default, so they are treated as maximally unresolved or randomly similar pairs rather than as intermediate-distance pairs. This transformation stretches distances at the low-similarity end, but it remains a pragmatic structural-distance correction rather than a fully validated generative model of protein structural evolution. The resulting distance matrix is then used to construct a structural phylogenetic tree using the Neighbor-Joining (NJ) or SwiftNJ algorithms (Saitou and Nei, 1987).

### 中文版本
为了从蛋白质结构推断系统发育关系，必须谨慎地将结构相似性转换为距离。在 eGPS-oneBuilder 流程中，我们使用 Foldseek (van Kempen et al., 2023) 进行双序列结构比对，并使用 TM-score (Template Modeling score) 量化两个蛋白质之间的结构相似性。TM-score 是一种独立于长度的拓扑相似性评估指标，其值为 1.0 时表示完美匹配，而低于 ~0.17 的值通常难以区别于随机结构相似性 (Zhang and Skolnick, 2004; Xu and Zhang, 2010; Zhang et al., 2022)。为了保证矩阵的对称性，我们计算 query 归一化的 TM-score (qTM) 和 target 归一化的 TM-score (tTM) 的算术平均值作为成对结构相似度 $S$。

从结构相似性到距离的简单线性转换 ($d = 1 - S$) 无法解释长时间进化尺度下结构散度的饱和效应。这会严重压缩远缘类群的分支长度，并扭曲推断出的建树拓扑结构。为了解决这一问题，我们实现了一种类似于核苷酸替代中 Jukes-Cantor 模型的对数转换方法。通过引入 TM-score 的经验随机基线 ($TM_{random} = 0.17$)，结构进化距离 $d$ 的计算公式如下：

$$ d = -\ln \left( \frac{S - 0.17}{1.0 - 0.17} \right) $$

为了保证数值计算的稳定性，对数函数的自变量被限制在不小于 $10^{-6}$ 的范围内。因此饱和距离上限为：

$$ d_{max} = -\ln(10^{-6}) = 13.81551056 $$

这些常数的含义并不相同。0.17 是 TM-score 文献支持的随机相似性经验基线；0.83 不是独立的生物学参数，而只是归一化项 $1.0 - 0.17$；13.81551056 也不是文献给出的进化系数，而是由 $10^{-6}$ 数值下限推导出的有限截断上限。默认情况下，Foldseek 未命中的配对会被赋予该饱和距离，因此它们被视为最大程度未解析或接近随机相似性的配对，而不是中等距离配对。该转换在低相似度的一端有效地拉伸了距离，但它仍是一个实用的结构距离校正，而不是已经完全验证的蛋白质结构进化生成模型。生成的距离矩阵随后被用于基于邻接法 (Neighbor-Joining, NJ) 或 SwiftNJ 算法构建结构系统发育树 (Saitou and Nei, 1987)。

---

## References / 参考文献

- **Foldseek**: van Kempen, M., Kim, S. S., Tumescheit, C., Mirdita, M., Lee, J., Gilchrist, C. L., ... & Steinegger, M. (2023). "Fast and accurate protein structure search with Foldseek." *Nature Biotechnology*, 41(2), 224-232.
- **TM-score & Random Baseline**: Zhang, Y., & Skolnick, J. (2004). "Scoring function for automated assessment of protein structure template quality." *Proteins: Structure, Function, and Bioinformatics*, 57(4), 702-710.
- **TM-score Significance**: Xu, J., & Zhang, Y. (2010). "How significant is a protein structure similarity with TM-score = 0.5?" *Bioinformatics*, 26(7), 889-895.
- **US-align**: Zhang, C., Shine, M., Pyle, A. M., & Zhang, Y. (2022). "US-align: universal structure alignments of proteins, nucleic acids, and macromolecular complexes." *Nature Methods*, 19(9), 1109-1115.
- **Neighbor-Joining**: Saitou, N., & Nei, M. (1987). "The neighbor-joining method: a new method for reconstructing phylogenetic trees." *Molecular Biology and Evolution*, 4(4), 406-425.
