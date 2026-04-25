#!/usr/bin/env python3

from __future__ import annotations

from dataclasses import dataclass


ENGLISH = "english"
CHINESE = "chinese"


def normalize_language(value) -> str:
    text = str(value or "").strip().lower()
    if text in {"zh", "zh-cn", "zh_cn", "chinese", "中文", "cn"}:
        return CHINESE
    return ENGLISH


def runtime_language(runtime_config) -> str:
    if not isinstance(runtime_config, dict):
        return ENGLISH
    run_config = runtime_config.get("run")
    if not isinstance(run_config, dict):
        return ENGLISH
    return normalize_language(run_config.get("language"))


def translate_runtime_text(message: str, language: str) -> str:
    if not isinstance(message, str) or not message:
        return message

    replacements = ENGLISH_TO_CHINESE if normalize_language(language) == CHINESE else CHINESE_TO_ENGLISH
    translated = message
    for source, target in replacements:
        translated = translated.replace(source, target)
    return translated


@dataclass(frozen=True)
class RuntimeTranslator:
    language: str = ENGLISH

    @classmethod
    def from_runtime_config(cls, runtime_config):
        return cls(runtime_language(runtime_config))

    def is_chinese(self) -> bool:
        return normalize_language(self.language) == CHINESE

    def text(self, english: str, chinese: str) -> str:
        return chinese if self.is_chinese() else english

    def method_label(self, method_key: str) -> str:
        labels = {
            "distance": self.text("Distance method", "距离法"),
            "maximum_likelihood": self.text("Maximum likelihood method", "极大似然法"),
            "bayesian": self.text("Bayesian method", "贝叶斯法"),
            "parsimony": self.text("Parsimony method", "简约法"),
            "protein_structure": self.text("Protein structure", "蛋白质结构"),
        }
        return labels[method_key]

    def marker_complete(self, method_key: str) -> str:
        return self.text(
            f"===={self.method_label(method_key)} complete===========================================================",
            f"===={self.method_label(method_key)}完成===========================================================",
        )

    def marker_failed(self, method_key: str) -> str:
        return self.text(
            f"===={self.method_label(method_key)} failed===========================================================",
            f"===={self.method_label(method_key)}失败===========================================================",
        )

    def marker_skipped(self, method_key: str) -> str:
        return self.text(
            f"===={self.method_label(method_key)} skipped by runtime config====",
            f"===={self.method_label(method_key)}已按运行时配置跳过====",
        )


class LocalizedLogger:
    def __init__(self, logger, translator: RuntimeTranslator):
        self._logger = logger
        self._translator = translator

    def __getattr__(self, name):
        return getattr(self._logger, name)

    def debug(self, message, *args, **kwargs):
        self._logger.debug(self._translate(message), *args, **kwargs)

    def info(self, message, *args, **kwargs):
        self._logger.info(self._translate(message), *args, **kwargs)

    def warning(self, message, *args, **kwargs):
        self._logger.warning(self._translate(message), *args, **kwargs)

    def error(self, message, *args, **kwargs):
        self._logger.error(self._translate(message), *args, **kwargs)

    def _translate(self, message):
        if isinstance(message, str):
            return translate_runtime_text(message, self._translator.language)
        return message


ENGLISH_TO_CHINESE = [
    ("Changed to work_dir: ", "当前所在目录： "),
    ("Starting distance-based inference...", "开始距离法建树..."),
    ("Distance-based inference completed", "距离法建树完成"),
    ("Distance-based method failed: ", "距离法建树失败: "),
    ("Starting maximum likelihood inference (IQ-TREE)...", "开始极大似然法建树..."),
    ("Maximum likelihood inference completed", "极大似然法建树完成"),
    ("Maximum likelihood method failed: ", "极大似然法建树失败: "),
    ("Starting Bayesian inference (MrBayes)...", "开始贝叶斯法建树..."),
    ("Bayesian inference completed", "贝叶斯法建树完成"),
    ("MrBayes consensus tree output not found", "未找到贝叶斯树输出文件"),
    ("MrBayes path not explicitly set; will try to use system 'mb' executable", "MrBayes路径未显式指定，将尝试使用系统中的 `mb`"),
    ("Bayesian method failed: ", "贝叶斯法建树失败: "),
    ("Starting parsimony inference...", "开始简约法建树..."),
    ("Parsimony inference completed", "简约法建树完成"),
    ("Parsimony method failed: ", "简约法建树失败: "),
    ("Generating tree visualizations...", "生成进化树可视化图片..."),
    ("Visualization completed", "可视化完成"),
    ("Tree distance calculation completed", "完成计算树的距离"),
    ("Tree distance calculation failed with command: ", "树距离计算失败，命令: "),
    ("Tree distance script execution failed with command ", "树距离脚本执行失败，命令 "),
    ("Tree distance heatmaps saved: ", "树距离热图已保存: "),
    (" and ", " 和 "),
    ("Failed to generate tree distance heatmaps: ", "生成树距离热图失败: "),
    ("Summary saved: ", "结果总结已保存: "),
    ("MAD executable not found: ", "MAD 工具未找到: "),
    ("MAD rerooting failed: ", "MAD 定根失败: "),
    ("Rerooting completed: ", "定根完成，"),
    ("Restored names in tree: ", "已恢复树文件名称: "),
    ("Failed to restore names in ", "恢复树文件名称失败 "),
    ("Restoring original sequence names in trees...", "恢复树文件中的原始序列名称..."),
    ("Protein phylogenetic pipeline completed!", "进化树构建管道完成!"),
    ("Phylogenetic pipeline completed!", "进化树构建管道完成!"),
    ("Results saved to: ", "结果保存在: "),
    ("Starting protein phylogenetic pipeline...This is: ", "开始进化树构建管道...This is: "),
    ("Starting phylogenetic pipeline...This is: ", "开始进化树构建管道...This is: "),
    ("Checking required software...", "检查所需软件..."),
    ("Input file does not exist: ", "输入文件不存在: "),
    ("At least 3 sequences are required to build a phylogenetic tree", "至少需要3条序列才能构建进化树"),
    ("Input file validation succeeded: ", "输入文件验证成功: "),
    (" sequences", "条序列"),
    ("Input file format error: ", "输入文件格式错误: "),
    ("Creating output directory: ", "创建输出目录: "),
    ("Converting to PHYLIP format: ", "转换为PHYLIP格式: "),
    ("Setting temporary naming convention for input sequences...", "为输入序列设置临时命名规则..."),
    ("IQ-TREE not found", "IQ-TREE未找到"),
    ("MrBayes not found", "MrBayes未找到"),
    ("Required PHYLIP commands not found: ", "未找到必需的 PHYLIP 命令: "),
    ("Tree file not found, skipping: ", "树文件不存在，跳过: "),
    ("No name mapping available; skipping name restoration in trees", "没有名称映射，跳过树文件中的名称恢复"),
    ("Standard error:", "标准错误:"),
]


CHINESE_TO_ENGLISH = [(target, source) for source, target in ENGLISH_TO_CHINESE]
