# How to cite

## Cite eGPS-onebuilder first

If you use this workflow, please cite the eGPS-onebuilder software repository first:

eGPS-onebuilder. GitHub repository: <https://github.com/yudalang3/egps-oneBuilder>

## Core workflow software

Please also cite the tools that contributed to the analyses you report.

### MAFFT

Katoh, K., and Standley, D. M. (2013). MAFFT multiple sequence alignment software version 7: improvements in performance and usability. *Molecular Biology and Evolution*, 30(4), 772-780. https://doi.org/10.1093/molbev/mst010

### PHYLIP

Felsenstein, J. (1989). PHYLIP: Phylogeny Inference Package, version 3.2. *Cladistics*, 5, 164-166.

### IQ-TREE

Nguyen, L. T., Schmidt, H. A., von Haeseler, A., and Minh, B. Q. (2015). IQ-TREE: a fast and effective stochastic algorithm for estimating maximum-likelihood phylogenies. *Molecular Biology and Evolution*, 32(1), 268-274. https://doi.org/10.1093/molbev/msu300

Minh, B. Q., Schmidt, H. A., Chernomor, O., Schrempf, D., Woodhams, M. D., von Haeseler, A., and Lanfear, R. (2020). IQ-TREE 2: New models and efficient methods for phylogenetic inference in the genomic era. *Molecular Biology and Evolution*, 37(5), 1530-1534. https://doi.org/10.1093/molbev/msaa015

### MrBayes

Huelsenbeck, J. P., and Ronquist, F. (2001). MRBAYES: Bayesian inference of phylogenetic trees. *Bioinformatics*, 17(8), 754-755. https://doi.org/10.1093/bioinformatics/17.8.754

Ronquist, F., Teslenko, M., van der Mark, P., Ayres, D. L., Darling, A., Hohna, S., Larget, B., Liu, L., Suchard, M. A., and Huelsenbeck, J. P. (2012). MrBayes 3.2: Efficient Bayesian phylogenetic inference and model choice across a large model space. *Systematic Biology*, 61(3), 539-542. https://doi.org/10.1093/sysbio/sys029

### Foldseek

van Kempen, M., Kim, S. S., Tumescheit, C., Mirdita, M., Lee, J., Gilchrist, C. L. M., Soding, J., and Steinegger, M. (2024). Fast and accurate protein structure search with Foldseek. *Nature Biotechnology*, 42, 243-246. https://doi.org/10.1038/s41587-023-01773-0

### TreeDist

Smith, M. R. (2020). Information theoretic generalized Robinson-Foulds metrics for comparing phylogenetic trees. *Bioinformatics*, 36(20), 5007-5013. https://doi.org/10.1093/bioinformatics/btaa614

### MAD rooting

Tria, F. D. K., Landan, G., and Dagan, T. (2017). Phylogenetic rooting using minimal ancestor deviation. *Nature Ecology & Evolution*, 1, 0193. https://doi.org/10.1038/s41559-017-0193

## Python, R, and visualization libraries

### Biopython

Cock, P. J. A., Antao, T., Chang, J. T., Chapman, B. A., Cox, C. J., Dalke, A., Friedberg, I., Hamelryck, T., Kauff, F., Wilczynski, B., and de Hoon, M. J. L. (2009). Biopython: freely available Python tools for computational molecular biology and bioinformatics. *Bioinformatics*, 25(11), 1422-1423. https://doi.org/10.1093/bioinformatics/btp163

### ETE Toolkit

Huerta-Cepas, J., Serra, F., and Bork, P. (2016). ETE 3: Reconstruction, analysis, and visualization of phylogenomic data. *Molecular Biology and Evolution*, 33(6), 1635-1638. https://doi.org/10.1093/molbev/msw046

### Matplotlib

Hunter, J. D. (2007). Matplotlib: A 2D graphics environment. *Computing in Science & Engineering*, 9(3), 90-95. https://doi.org/10.1109/MCSE.2007.55

### R

R Core Team. R: A language and environment for statistical computing. R Foundation for Statistical Computing, Vienna, Austria. <https://www.R-project.org/>

## Runtime and GUI software acknowledgements

The eGPS-onebuilder GUI and runtime environment also rely on additional open-source software. Cite or acknowledge these projects where relevant to your usage and reporting requirements.

- Pixi: <https://pixi.sh/>
- FlatLaf: <https://www.formdev.com/flatlaf/>
- SwingX: <https://github.com/kleopatra/swingx>
- JIDE OSS: <https://github.com/jidesoft/jide-oss>
- Apache Commons libraries: <https://commons.apache.org/>
- org.json Java library: <https://github.com/stleary/JSON-java>

## Suggested methods text

Phylogenetic analyses were configured and executed with eGPS-onebuilder (<https://github.com/yudalang3/egps-oneBuilder>). Multiple sequence alignment was performed with MAFFT when requested. Phylogenetic trees were inferred with PHYLIP distance/parsimony methods, IQ-TREE maximum likelihood, and MrBayes Bayesian inference according to the selected workflow settings. Optional protein-structure similarity analyses used Foldseek, and tree-distance summaries used TreeDist and Robinson-Foulds distances.
