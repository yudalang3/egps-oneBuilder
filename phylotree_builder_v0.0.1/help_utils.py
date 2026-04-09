#!/usr/bin/env python3
"""
解析 MrBayes 生成的 nexus 树文件并转换为 newick 格式
使用 ete4 库进行树的处理
"""

import re
from ete4 import Tree

def parse_nexus_tree(nexus_file_path, output_nwk_path=None):
    """
    解析 nexus 格式的树文件并转换为 newick 格式

    Args:
        nexus_file_path (str): nexus 文件路径
        output_nwk_path (str): 输出的 newick 文件路径，如果为 None 则自动生成

    Returns:
        Tree: ete4 Tree 对象
    """

    # 读取 nexus 文件
    with open(nexus_file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 查找树的定义部分
    # MrBayes 输出通常在 begin trees; 和 end; 之间（不区分大小写）
    tree_block_pattern = r'begin\s+trees;(.*?)end;'
    tree_block_match = re.search(tree_block_pattern, content, re.DOTALL | re.IGNORECASE)

    if not tree_block_match:
        raise ValueError("未找到 TREES 块")

    tree_block = tree_block_match.group(1)

    # 提取 TRANSLATE 信息
    translate_dict = {}
    translate_pattern = r'translate\s+(.*?);'
    translate_match = re.search(translate_pattern, tree_block, re.DOTALL | re.IGNORECASE)

    if translate_match:
        translate_content = translate_match.group(1)
        # 解析每一行的映射关系
        for line in translate_content.split(','):
            line = line.strip()
            if line:
                # 移除可能的制表符和多余空格
                parts = re.split(r'\s+', line.strip())
                if len(parts) >= 2:
                    translate_dict[parts[0]] = parts[1]

    # 查找树的定义行
    # 格式为: tree tree_name = [&U] (newick_string);
    tree_pattern = r'tree\s+\w+\s*=\s*(?:\[&[^\]]*\])?\s*(\([^;]+\));'
    tree_match = re.search(tree_pattern, tree_block, re.DOTALL | re.IGNORECASE)

    if not tree_match:
        raise ValueError("未找到树的定义")

    # 获取 newick 字符串
    newick_string = tree_match.group(1).strip()

    # 清理 newick 字符串，移除可能的注释
    # MrBayes 会在节点上添加支持值等信息，格式如 [&prob=0.95,length_mean=0.15]
    newick_string = re.sub(r'\[&[^\]]*\]', '', newick_string)

    # 替换数字标识符为实际物种名
    # 需要确保只替换作为节点标识符的数字，不替换分支长度中的数字
    for num, name in translate_dict.items():
        # 匹配模式：数字前面是 ( 或 , 或开头，后面是 [ 或 : 或 , 或 )
        pattern = r'(?<=[\(,])' + re.escape(num) + r'(?=[\[\:,\)])'
        newick_string = re.sub(pattern, name, newick_string)

    # 确保字符串以分号结尾
    if not newick_string.endswith(';'):
        newick_string += ';'

    # 创建 ete4 Tree 对象
    tree = Tree(newick_string)

    # 如果没有指定输出路径，自动生成
    if output_nwk_path is None:
        output_nwk_path = nexus_file_path + '.nwk'

    # 导出为 newick 格式
    tree.write(outfile=output_nwk_path)

    print(f"成功解析 nexus 文件: {nexus_file_path}")
    print(f"树已导出为 newick 格式: {output_nwk_path}")
    print(f"树包含 {len(list(tree.leaves()))} 个叶节点")

    return tree


if __name__ == "__main__":
    # experiments/trial/main.py
    import sys
    from pathlib import Path

    # 将 ref_fun 所在目录（即项目根目录下的 ref_fun）加入 Python 路径
    for i in range(0, 5):
        print("i:", i)
        yy = Path(__file__).resolve().parents[i]
        print(yy)
