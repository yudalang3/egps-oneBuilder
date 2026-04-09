# NEXUS 文件格式详解

## 概述

NEXUS 是一种用于存储系统发育数据的标准化文件格式，广泛用于生物信息学中的系统发育分析。该格式由 Maddison 等人在 1997 年提出，支持多种数据类型，包括序列数据、树结构、字符状态等。

## 基本结构

NEXUS 文件采用块状结构，每个块以 `BEGIN blockname;` 开始，以 `END;` 结束：

```
#NEXUS

BEGIN DATA;
    [数据块内容]
END;

BEGIN TREES;
    [树块内容]
END;
```

## 主要数据块类型

### 1. DATA 块
存储序列比对数据：
```
BEGIN DATA;
    DIMENSIONS NTAX=4 NCHAR=10;
    FORMAT DATATYPE=DNA GAP=- MISSING=?;
    MATRIX
        Species1    ATCGATCGAT
        Species2    ATCG-TCGAT
        Species3    ATCGATCG?T
        Species4    ATCGATCGAT
    ;
END;
```

### 2. TREES 块
存储系统发育树：
```
BEGIN TREES;
    TRANSLATE
        1 Species1,
        2 Species2,
        3 Species3,
        4 Species4
    ;
    TREE tree1 = [&U] (1:0.1,2:0.2,(3:0.05,4:0.08):0.15);
END;
```

## 树格式详解

### 基本 Newick 格式
```
(A:0.1,B:0.2,(C:0.05,D:0.08):0.15);
```
- 括号表示分组（内部节点）
- 冒号后的数字表示分支长度
- 逗号分隔同级分支
- 分号结束整个树

### MrBayes 特有的树注释

#### 1. `[&U]` - 无根树标记
```
TREE tree1 = [&U] (1:0.1,2:0.2,(3:0.05,4:0.08):0.15);
```
- `[&U]` 表示这是一个无根树（Unrooted）
- `[&R]` 表示有根树（Rooted）

#### 2. 节点注释 `[&参数=值]`
```
TREE tree1 = [&U] (1:0.1,2:0.2,(3:0.05,4:0.08)[&prob=0.95,length_mean=0.15]:0.15);
```

常见的节点注释参数：

| 参数 | 含义 | 示例 |
|------|------|------|
| `prob` | 后验概率 | `[&prob=0.95]` |
| `prob_stddev` | 后验概率标准差 | `[&prob_stddev=0.02]` |
| `prob_range` | 后验概率范围 | `[&prob_range={0.85,1.00}]` |
| `length_mean` | 分支长度均值 | `[&length_mean=0.15]` |
| `length_median` | 分支长度中位数 | `[&length_median=0.14]` |
| `length_95%HPD` | 分支长度95%置信区间 | `[&length_95%HPD={0.10,0.20}]` |
| `height_mean` | 节点高度均值 | `[&height_mean=0.25]` |
| `height_median` | 节点高度中位数 | `[&height_median=0.24]` |
| `height_95%HPD` | 节点高度95%置信区间 | `[&height_95%HPD={0.20,0.30}]` |

#### 3. 复杂注释示例
```
TREE con_50_majrule = [&U] (
    1[&prob=1.00,prob_stddev=0.00,prob_range={1.00,1.00},prob(percent)="100",prob+-sd="100+-0"]:0.123456,
    2[&prob=1.00,prob_stddev=0.00,prob_range={1.00,1.00},prob(percent)="100",prob+-sd="100+-0"]:0.234567,
    (
        3[&prob=1.00,prob_stddev=0.00,prob_range={1.00,1.00},prob(percent)="100",prob+-sd="100+-0"]:0.045678,
        4[&prob=1.00,prob_stddev=0.00,prob_range={1.00,1.00},prob(percent)="100",prob+-sd="100+-0"]:0.056789
    )[&prob=0.95,prob_stddev=0.02,prob_range={0.85,1.00},prob(percent)="95",prob+-sd="95+-2"]:0.156789
);
```

### TRANSLATE 块
用于将数字标识符映射到物种名称：
```
BEGIN TREES;
    TRANSLATE
        1 Homo_sapiens,
        2 Pan_troglodytes,
        3 Gorilla_gorilla,
        4 Pongo_pygmaeus
    ;
    TREE tree1 = [&U] (1:0.1,2:0.2,(3:0.05,4:0.08):0.15);
END;
```

## MrBayes 输出文件类型

### 1. `.con.tre` - 共识树文件
包含多个共识树：
- 50% majority rule consensus tree
- 其他阈值的共识树

### 2. `.t` - 采样树文件
包含 MCMC 采样过程中的所有树，通常有成千上万棵树。

### 3. `.parts` - 分割文件
包含各个分支的支持度信息。

## 解析注意事项

### 1. 注释清理
解析时需要移除方括号注释：
```python
import re
# 移除所有 [&...] 注释
clean_newick = re.sub(r'\[&[^\]]*\]', '', newick_string)
```

### 2. 数字标识符替换
如果使用了 TRANSLATE，需要将数字替换为实际物种名：
```python
def replace_translate(newick_string, translate_dict):
    for num, name in translate_dict.items():
        newick_string = newick_string.replace(str(num), name)
    return newick_string
```

### 3. 特殊字符处理
- 物种名中的空格通常用下划线替换
- 特殊字符可能需要引号包围

## 常见问题

### Q: 为什么有些节点没有支持值？
A: 在贝叶斯分析中，只有内部节点（分支点）才有后验概率，叶节点（物种）没有。

### Q: `prob=1.00` 是什么意思？
A: 表示该分支在所有采样树中都存在，后验概率为100%。

### Q: 如何理解分支长度？
A: 分支长度通常表示进化距离，单位取决于所使用的进化模型（如每位点替换数）。

### Q: `95%HPD` 是什么？
A: Highest Posterior Density，95%最高后验密度区间，表示参数值的可信区间。

## 实用工具

### Python 解析示例
```python
import re
from ete4 import Tree

def parse_mrbayes_tree(nexus_content):
    # 提取树块
    tree_block = re.search(r'BEGIN\s+TREES;(.*?)END;', nexus_content, re.DOTALL)
    
    # 提取 TRANSLATE 信息
    translate_match = re.search(r'TRANSLATE\s+(.*?);', tree_block.group(1), re.DOTALL)
    translate_dict = {}
    if translate_match:
        for line in translate_match.group(1).split(','):
            parts = line.strip().split()
            if len(parts) >= 2:
                translate_dict[parts[0]] = parts[1]
    
    # 提取树定义
    tree_match = re.search(r'TREE\s+\w+\s*=\s*(?:\[&[^\]]*\])?\s*([^;]+);', tree_block.group(1))
    newick_string = tree_match.group(1)
    
    # 清理注释
    clean_newick = re.sub(r'\[&[^\]]*\]', '', newick_string)
    
    # 替换数字标识符
    for num, name in translate_dict.items():
        clean_newick = clean_newick.replace(num, name)
    
    return Tree(clean_newick)
```

这个文档应该能帮你理解 NEXUS 格式中那些"乱七八糟"的符号了！