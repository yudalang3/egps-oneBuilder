#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
自动化进化树构建管道
功能：输入比对的FASTA文件，自动生成四种不同方法的进化树

作者：自动化脚本
版本：1.0
"""

import os, glob
import sys
import subprocess
import shutil
from pathlib import Path
from typing import List, Dict, Optional
import logging
from datetime import datetime
import argparse
from Bio import SeqIO, Phylo
import matplotlib.pyplot as plt
import matplotlib as mpl
from ete4 import Tree
from onebuilder_localization import LocalizedLogger, RuntimeTranslator
from onebuilder_runtime_config import dna_runtime_settings, load_runtime_config
from tree_summary_plot import create_tree_distance_heatmaps

mpl.set_loglevel("warning")  # 或 "error"
mpl.use("Agg", force=True)
import warnings

warnings.filterwarnings("ignore")


MRBAYES_WRAP_WIDTH = 70


def _normalize_phylip_menu_input(menu_overrides, default_input):
    if not menu_overrides:
        return default_input
    lines = [str(item).strip() for item in menu_overrides if str(item).strip()]
    if not lines:
        return default_input
    if lines[-1].upper() != "Y":
        lines.append("Y")
    return "\n".join(lines) + "\n"


def _build_dnadist_menu_input(settings):
    overrides = settings.get("dnadist_menu_overrides")
    if overrides:
        return _normalize_phylip_menu_input(overrides, "Y\n")

    lines = []
    model = str(settings.get("dnadist_model", "F84")).strip().upper()
    model_steps = {
        "F84": 0,
        "KIMURA": 1,
        "JUKES-CANTOR": 2,
        "LOGDET": 3,
    }
    for _ in range(model_steps.get(model, 0)):
        lines.append("D")

    if model in {"F84", "KIMURA"}:
        ratio = settings.get("dnadist_transition_transversion_ratio")
        if ratio is not None and float(ratio) != 2.0:
            lines.extend(["T", str(ratio)])
    if model == "F84" and settings.get("dnadist_empirical_base_frequencies") is False:
        lines.append("F")
    return _normalize_phylip_menu_input(lines, "Y\n")


def _build_neighbor_menu_input(settings):
    overrides = settings.get("neighbor_menu_overrides")
    if overrides:
        return _normalize_phylip_menu_input(overrides, "Y\n")

    lines = []
    neighbor_method = str(settings.get("neighbor_method", "NJ")).strip().upper()
    if neighbor_method == "UPGMA":
        lines.append("N")
    else:
        outgroup_index = settings.get("neighbor_outgroup_index")
        if outgroup_index is not None:
            lines.extend(["O", str(outgroup_index)])
    return _normalize_phylip_menu_input(lines, "Y\n")


def _build_dnapars_menu_input(settings):
    overrides = settings.get("dnapars_menu_overrides")
    if overrides:
        return _normalize_phylip_menu_input(overrides, "Y\n")

    lines = []
    outgroup_index = settings.get("dnapars_outgroup_index")
    if outgroup_index is not None:
        lines.extend(["O", str(outgroup_index)])
    if settings.get("dnapars_transversion_parsimony"):
        lines.append("N")
    return _normalize_phylip_menu_input(lines, "Y\n")


def _append_iqtree_args(cmd, settings):
    option_pairs = [
        ("sequence_type", "-st"),
        ("outgroup", "-o"),
        ("threads", "-nt"),
        ("threads_max", "-ntmax"),
        ("seed", "-seed"),
        ("memory_limit", "-mem"),
        ("alrt", "-alrt"),
    ]
    for setting_key, cli_flag in option_pairs:
        value = settings.get(setting_key)
        if value is not None and str(value).strip():
            cmd.extend([cli_flag, str(value)])

    if settings.get("safe"):
        cmd.append("-safe")
    if settings.get("keep_ident"):
        cmd.append("-keep-ident")
    if settings.get("abayes"):
        cmd.append("-abayes")
    if settings.get("verbose") and not settings.get("quiet", True):
        cmd.append("-v")
    if settings.get("quiet", True):
        cmd.append("-quiet")
    if settings.get("redo", True):
        cmd.append("-redo")
    for extra_arg in settings.get("extra_args", []):
        cmd.append(str(extra_arg))


def _normalize_mrbayes_lines(command_block):
    lines = []
    for raw_line in command_block or []:
        text = str(raw_line).strip()
        if not text:
            continue
        if not text.endswith(";"):
            text += ";"
        lines.append(text + "\n")
    return lines


def _build_sumt_command(settings):
    options = []
    if settings.get("burnin") is not None:
        options.append(f"burnin={settings['burnin']}")
    if settings.get("burninfrac") is not None:
        options.append(f"burninfrac={settings['burninfrac']}")
    if settings.get("relburnin") is not None:
        options.append(f"relburnin={'yes' if settings['relburnin'] else 'no'}")
    if options:
        return "sumt " + " ".join(options) + ";\n"
    return "sumt;\n"


def _build_dna_mrbayes_commands(nexus_file_name, settings):
    custom_block = _normalize_mrbayes_lines(settings.get("command_block"))
    commands = ["set autoclose=yes nowarn=yes;\n", f"execute {nexus_file_name}\n"]
    if custom_block:
        commands.extend(custom_block)
        if not custom_block[-1].strip().lower().startswith("quit"):
            commands.append("quit;\n")
        return commands

    commands.append(f"lset nst={settings['nst']} rates={settings['rates']};\n")

    mcmcp_options = [
        f"ngen={settings['ngen']}",
        f"samplefreq={settings['samplefreq']}",
        f"printfreq={settings['printfreq']}",
        f"diagnfreq={settings['diagnfreq']}",
    ]
    for option_key in ("nruns", "nchains", "temp"):
        option_value = settings.get(option_key)
        if option_value is not None:
            mcmcp_options.append(f"{option_key}={option_value}")
    stoprule_value = settings.get("stoprule")
    if stoprule_value is not None:
        mcmcp_options.append(
            f"stoprule={'yes' if stoprule_value else 'no'}"
        )
        if stoprule_value and settings.get("stopval") is not None:
            mcmcp_options.append(f"stopval={settings['stopval']}")
    commands.append("mcmcp " + " ".join(mcmcp_options) + ";\n")
    commands.append("mcmc;\n")
    commands.append(_build_sumt_command(settings))
    commands.append("quit;\n")
    return commands


class PhylogeneticPipeline:
    def __init__(self, input_file, output_dir="phylo_results", runtime_config=None):
        """
        初始化进化树构建管道

        Args:
            input_file (str): 输入的比对FASTA文件路径
            output_dir (str): 输出目录路径
        """
        self.input_file = Path(input_file).resolve()
        self.original_file = None
        self.new_name2ori_name_dict: Dict[str, str] = None
        self.output_dir = Path(output_dir).resolve()
        self.script_dir = Path(__file__).parent.resolve()
        self.translator = RuntimeTranslator.from_runtime_config(runtime_config)
        self.runtime_settings = dna_runtime_settings(runtime_config)
        self.setup_logging()
        self.create_directories()

        # 软件路径（将在检查时设置）
        self.phylip_commands: Dict[str, str] = {}
        self.iqtree_path = None
        self.mrbayes_path = None
        self.mad_method_path = str(self.script_dir / "third_party" / "mad" / "mad")
        self.ktreedist_method_path = "/opt/BioInfo/Ktreedist/Ktreedist_v1/Ktreedist.pl"
        self.cal_treedist_method_path = str(
            self.script_dir / "cal_pair_wise_tree_dist.R"
        )

    def _resolve_command(
        self, command_names: List[str], bundled_glob: Optional[str] = None
    ) -> Optional[str]:
        for command_name in command_names:
            resolved_path = shutil.which(command_name)
            if resolved_path:
                return resolved_path

        if bundled_glob:
            bundled_matches = sorted(self.script_dir.glob(bundled_glob))
            for bundled_match in bundled_matches:
                if bundled_match.exists():
                    return str(bundled_match)

        return None

    def name_convention(self):
        """临时缩短序列 ID，避免 strict PHYLIP 格式限制。"""
        self.input_file = Path(self.input_file).resolve()
        records = list(SeqIO.parse(self.input_file, "fasta"))

        new_name2ori_name_dict: Dict[str, str] = {}
        for index, record in enumerate(records, start=1):
            new_name = f"seq{index}"
            new_name2ori_name_dict[new_name] = record.id
            record.id = new_name
            record.name = new_name
            record.description = new_name

        self.new_name2ori_name_dict = new_name2ori_name_dict
        self.original_file = self.input_file
        self.input_file = self.output_dir / (
            self.input_file.stem + "_tmp" + self.input_file.suffix
        )

        SeqIO.write(records, self.input_file, "fasta")
        self.logger.info("为输入序列设置临时命名规则...")

    def setup_logging(self):
        """设置日志系统"""
        log_file = f"phylo_pipeline_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"
        logging.basicConfig(
            level=logging.DEBUG,
            format="%(asctime)s - %(levelname)s - %(message)s",
            handlers=[logging.FileHandler(log_file), logging.StreamHandler()],
        )
        self.logger = LocalizedLogger(logging.getLogger(__name__), self.translator)

    def create_directories(self):
        """创建输出目录结构"""
        directories = [
            self.output_dir,
            self.output_dir / "distance_method",
            self.output_dir / "parsimony_method",
            self.output_dir / "maximum_likelihood",
            self.output_dir / "bayesian_method",
            self.output_dir / "visualizations",
        ]

        for dir_path in directories:
            dir_path.mkdir(parents=True, exist_ok=True)

        self.logger.info(f"创建输出目录: {self.output_dir}")

    def check_and_install_software(self):
        """检查并安装所需软件"""
        self.logger.info("检查所需软件...")

        self.iqtree_path = self._resolve_command(["iqtree", "iqtree2", "iqtree3"])
        self.mrbayes_path = self._resolve_command(["mb", "mrbayes"])
        self.phylip_commands = {
            "dnadist": self._resolve_command(
                ["dnadist"], ".pixi/envs/default/share/phylip-*/exe/dnadist"
            ),
            "neighbor": self._resolve_command(
                ["neighbor"], ".pixi/envs/default/share/phylip-*/exe/neighbor"
            ),
            "dnapars": self._resolve_command(
                ["dnapars"], ".pixi/envs/default/share/phylip-*/exe/dnapars"
            ),
        }

        if not self.iqtree_path:
            self.logger.error("IQ-TREE未找到")
            sys.exit(1)
        if not self.mrbayes_path:
            self.logger.error("MrBayes未找到")
            sys.exit(1)

        missing_phylip_commands = [
            command_name
            for command_name, resolved_path in self.phylip_commands.items()
            if not resolved_path
        ]
        if missing_phylip_commands:
            self.logger.error(
                f"未找到必需的 PHYLIP 命令: {', '.join(missing_phylip_commands)}"
            )
            sys.exit(1)

        if not Path(self.mad_method_path).exists():
            self.logger.error(
                f"MAD rerooting method is not found: {self.mad_method_path}"
            )
            sys.exit(1)

    def validate_input(self):
        """验证输入文件"""
        if not self.input_file.exists():
            self.logger.error(f"输入文件不存在: {self.input_file}")
            sys.exit(1)

        # 检查是否为有效的FASTA文件
        try:
            sequences = list(SeqIO.parse(self.input_file, "fasta"))
            if len(sequences) < 3:
                self.logger.error("至少需要3条序列才能构建进化树")
                sys.exit(1)

            self.logger.info(f"输入文件验证成功: {len(sequences)}条序列")
            return sequences

        except Exception as e:
            self.logger.error(f"输入文件格式错误: {e}")
            sys.exit(1)

    def convert_to_phylip(self):
        """将FASTA格式转换为PHYLIP格式"""
        phylip_file = self.output_dir / "alignment.phy"

        records = SeqIO.parse(self.input_file, "fasta")
        # ⚠️ 注意：
        # phylip 格式有两种常见变体：
        # strict (经典版)：序列 ID 限制在 10 个字符以内。
        # relaxed (扩展版)：允许更长的 ID。
        # Biopython 默认使用 strict 格式，如果 ID 太长会报错或被截断。
        # 👉 如果你的 ID 很长，建议用：
        # SeqIO.write(records, phylip_file, "phylip-relaxed")
        SeqIO.write(records, phylip_file, "phylip")

        self.logger.info(f"转换为PHYLIP格式: {phylip_file}")
        return phylip_file

    def distance_method(self, phylip_file):
        """距离法建树（使用PHYLIP）"""
        self.logger.info("开始距离法建树...")

        work_dir = self.output_dir / "distance_method"
        self.logger.info(f"当前所在目录： {os.getcwd()}")
        shutil.copy(phylip_file, work_dir / "infile")
        os.chdir(work_dir)
        self.logger.info(f"当前所在目录： {os.getcwd()}")

        try:
            # 1. 计算距离矩阵 (dnadist)
            dnadist_input = _normalize_phylip_menu_input(
                self.runtime_settings["distance"].get("dnadist_menu_overrides"),
                _build_dnadist_menu_input(self.runtime_settings["distance"]),
            )
            proc = subprocess.Popen(
                [self.phylip_commands["dnadist"]],
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            stdout, stderr = proc.communicate(input=dnadist_input)
            # 现在你可以使用输出内容了
            self.logger.debug("==Stdout of PHYLIP==")
            self.logger.debug(stdout)
            if proc.returncode != 0:
                print(self.translator.text("Standard error:", "标准错误:"))
                print(stderr)
                sys.exit(1)
            # 2. 邻接法建树 (neighbor)
            if os.path.exists("outfile"):
                shutil.copy2("outfile", "distance_matrix.txt")
                shutil.move("outfile", "infile")

            neighbor_input = _normalize_phylip_menu_input(
                self.runtime_settings["distance"].get("neighbor_menu_overrides"),
                _build_neighbor_menu_input(self.runtime_settings["distance"]),
            )
            proc = subprocess.Popen(
                [self.phylip_commands["neighbor"]],
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            stdout, stderr = proc.communicate(input=neighbor_input)
            self.logger.debug("==Stdout of PHYLIP==")
            self.logger.debug(stdout)
            if proc.returncode != 0:
                print(self.translator.text("Standard error:", "标准错误:"))
                print(stderr)
                sys.exit(1)

            # 重命名输出文件
            if os.path.exists("outtree"):
                shutil.move("outtree", "distance_tree.nwk")
            if os.path.exists("outfile"):
                shutil.move("outfile", "distance_tree.nwk.txt")

            self.logger.info("距离法建树完成")
            return work_dir / "distance_tree.nwk"

        except Exception as e:
            self.logger.error(f"距离法建树失败: {e}")
            return None
        finally:
            os.remove("infile")
            os.chdir(self.output_dir.parent)

    def parsimony_method(self, phylip_file):
        """简约法建树（使用PHYLIP）"""
        self.logger.info("开始简约法建树...")

        work_dir = self.output_dir / "parsimony_method"
        shutil.copy(phylip_file, work_dir / "infile")
        os.chdir(work_dir)

        self.logger.info(f"当前所在目录： {os.getcwd()}")

        try:
            # 使用dnapars进行简约法分析
            dnapars_input = _normalize_phylip_menu_input(
                self.runtime_settings["parsimony"].get("dnapars_menu_overrides"),
                _build_dnapars_menu_input(self.runtime_settings["parsimony"]),
            )
            proc = subprocess.Popen(
                [self.phylip_commands["dnapars"]],
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            stdout, stderr = proc.communicate(input=dnapars_input)
            self.logger.debug("==Stdout of PHYLIP==")
            self.logger.debug(stdout)
            if proc.returncode != 0:
                print(self.translator.text("Standard error:", "标准错误:"))
                print(stderr)
                sys.exit(1)
            # 重命名输出文件
            if os.path.exists("outtree"):
                shutil.move("outtree", "parsimony_tree.nwk")
            if os.path.exists("outfile"):
                shutil.move("outfile", "parsimony_output.txt")

            self.logger.info("简约法建树完成")
            return work_dir / "parsimony_tree.nwk"

        except Exception as e:
            self.logger.error(f"简约法建树失败: {e}")
            return None
        finally:
            os.remove("infile")
            os.chdir(self.output_dir.parent)

    def maximum_likelihood_method(self):
        """极大似然法建树（使用IQ-TREE）"""
        self.logger.info("开始极大似然法建树...")

        if not self.iqtree_path:
            self.logger.error("IQ-TREE未找到")
            return None

        work_dir = self.output_dir / "maximum_likelihood"
        # input_file = work_dir / "alignment.fasta"
        input_file = self.input_file
        ml_settings = self.runtime_settings["maximum_likelihood"]

        os.chdir(work_dir)
        self.logger.info(f"当前所在目录： {os.getcwd()}")
        try:
            # 运行IQ-TREE
            cmd = [
                self.iqtree_path,
                "-s",
                str(input_file),
                "-m",
                str(ml_settings["model_strategy"]),
                "-bb",
                str(ml_settings["bootstrap_replicates"]),
                "-pre",
                "ml_tree",  # 重新运行
            ]
            if str(ml_settings.get("model_set", "")).strip():
                cmd[5:5] = ["-mset", str(ml_settings["model_set"])]
            _append_iqtree_args(cmd, ml_settings)
            self.logger.info(cmd)

            proc = subprocess.Popen(
                cmd,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            stdout, stderr = proc.communicate()
            self.logger.debug("==Stdout of IQ-Tree==")
            self.logger.debug(stdout)
            if proc.returncode != 0:
                print("标准错误:")
                print(stderr)
                sys.exit(1)
            else:
                self.logger.info("极大似然法建树完成")
                # 如何你要共识树，那么可以用 ml_tree.contree
                return work_dir / "ml_tree.treefile"
        except Exception as e:
            self.logger.error(f"极大似然法建树失败: {e}")
            return None
        finally:
            # os.remove("infile")
            os.chdir(self.output_dir.parent)

    def bayesian_method(self):
        """贝叶斯法建树（使用MrBayes）"""
        self.logger.info("开始贝叶斯法建树...")

        if not self.mrbayes_path:
            self.logger.warning(
                self.translator.text(
                    "MrBayes path not explicitly set; will try to use system 'mb' executable",
                    "MrBayes路径未显式指定，将尝试使用系统中的 `mb`",
                )
            )
            mb_exec = "mb"
        else:
            mb_exec = str(self.mrbayes_path)

        work_dir = self.output_dir / "bayesian_method"
        work_dir.mkdir(parents=True, exist_ok=True)  # FIX: 确保目录存在
        bayesian_settings = self.runtime_settings["bayesian"]

        self.logger.info(f"当前所在目录： {os.getcwd()}")

        try:
            # 创建MrBayes输入文件（NEXUS）
            nexus_file = work_dir / "alignment.nex"
            self.create_nexus_file(nexus_file)

            # 创建MrBayes命令文件
            mb_script = work_dir / "mb_commands.txt"
            mb_input = _build_dna_mrbayes_commands(nexus_file.name, bayesian_settings)

            # 写入脚本文件（便于复现/调试）
            mb_input_str = "".join(mb_input)  # FIX: communicate 需要字符串
            mb_script.write_text(mb_input_str, encoding="utf-8")

            # 运行MrBayes
            os.chdir(work_dir)
            proc = subprocess.Popen(
                [mb_exec],
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            stdout, stderr = proc.communicate(input=mb_input_str)  # FIX: 传字符串
            self.logger.debug("==Stdout of MrBayes==")
            self.logger.debug(stdout)
            if proc.returncode != 0:
                print(self.translator.text("Standard error:", "标准错误:"))
                print(stderr)
                sys.exit(1)

            # 查找输出的树文件（*.con.tre）
            consensus_tree = None
            for file in Path.cwd().glob("*.con.tre"):
                consensus_tree = file
                from help_utils import parse_nexus_tree

                out_file = Path(str(consensus_tree) + ".nwk")
                parse_nexus_tree(
                    consensus_tree,
                    out_file,
                    language=self.translator.language,
                )
                consensus_tree = out_file
                break

            if consensus_tree:
                self.logger.info("贝叶斯法建树完成")
                return consensus_tree
            else:
                self.logger.error("未找到贝叶斯树输出文件")
                return None

        except Exception as e:
            self.logger.error(f"贝叶斯法建树失败: {e}")
            return None
        finally:
            os.chdir(self.output_dir.parent)

    def create_nexus_file(self, nexus_file):
        """创建NEXUS格式文件供MrBayes使用"""
        sequences = list(SeqIO.parse(self.input_file, "fasta"))

        with open(nexus_file, "w", encoding="utf-8") as f:
            f.write("#NEXUS\n\n")
            f.write("begin data;\n")
            f.write(
                f"    dimensions ntax={len(sequences)} nchar={len(sequences[0].seq)};\n"
            )
            f.write("    format datatype=DNA gap=- missing=?;\n")
            f.write("    matrix\n")

            for seq in sequences:
                wrapped_sequence = str(seq.seq)
                chunks = [
                    wrapped_sequence[index : index + MRBAYES_WRAP_WIDTH]
                    for index in range(0, len(wrapped_sequence), MRBAYES_WRAP_WIDTH)
                ]
                for index, chunk in enumerate(chunks):
                    prefix = f"    {seq.id:<20} " if index == 0 else " " * 25
                    f.write(f"{prefix}{chunk}\n")

            f.write("    ;\n")
            f.write("end;\n")

    def visualize_trees(self, tree_files):
        """可视化生成的进化树"""
        self.logger.info("生成进化树可视化图片...")

        vis_dir = self.output_dir / "visualizations"
        vis_dir.mkdir(parents=True, exist_ok=True)
        methods = ["Distance", "Maximum Likelihood", "Bayesian", "Parsimony"]

        # 设置中文字体（如果可用）
        try:
            plt.rcParams["font.sans-serif"] = ["DejaVu Sans", "SimHei"]
            plt.rcParams["axes.unicode_minus"] = False
        except:
            self.logger.error("plt.rcParams setting error.")
            pass

        fig, axes = plt.subplots(2, 2, figsize=(16, 12))
        axes = axes.flatten()

        for i, (tree_file, method) in enumerate(zip(tree_files, methods)):
            if tree_file and tree_file.exists():
                try:
                    tree = Phylo.read(tree_file, "newick")
                    Phylo.draw(tree, axes=axes[i], do_show=False)
                    axes[i].set_title(
                        self.translator.text(f"{method} Method", f"{method}法") ,
                        fontsize=14,
                    )
                except Exception as e:
                    axes[i].text(
                        0.5,
                        0.5,
                        self.translator.text(
                            f"Failed to read tree\n{method}",
                            f"读取树失败\n{method}",
                        ),
                        ha="center",
                        va="center",
                        transform=axes[i].transAxes,
                    )
            else:
                axes[i].text(
                    0.5,
                    0.5,
                    self.translator.text(
                        f"Tree file not found\n{method}",
                        f"树文件不存在\n{method}",
                    ),
                    ha="center",
                    va="center",
                    transform=axes[i].transAxes,
                )

        plt.tight_layout()
        plt.savefig(vis_dir / "all_trees_comparison.png", dpi=300, bbox_inches="tight")
        plt.savefig(vis_dir / "all_trees_comparison.pdf", bbox_inches="tight")
        plt.close()

        self.logger.info("可视化完成")

    def generate_summary(self, tree_files, sequences):
        """生成结果总结报告"""

        path_trees_summary = self.output_dir / "tree_summary"
        path_trees_summary.mkdir(parents=True, exist_ok=True)

        os.chdir(path_trees_summary)

        path_tree_info = path_trees_summary / "tree_meta_data.tsv"
        with open(path_tree_info, "w") as f:
            f.write(f"NJ_phylip\t{tree_files[0] if tree_files[0] else 'NULL'}\n")
            f.write(f"ML_iqtree\t{tree_files[1] if tree_files[1] else 'NULL'}\n")
            f.write(f"BI_mrbayes\t{tree_files[2] if tree_files[2] else 'NULL'}\n")
            f.write(f"MP_phylip\t{tree_files[3] if tree_files[3] else 'NULL'}\n")

        heatmap_paths = []
        r_commands = [
            ["Rscript", self.cal_treedist_method_path, str(path_tree_info), self.translator.language],
            [
                "R",
                "--slave",
                "-f",
                self.cal_treedist_method_path,
                "--args",
                str(path_tree_info),
                self.translator.language,
            ],
        ]

        tree_distance_completed = False
        for cmd in r_commands:
            self.logger.info(cmd)

            try:
                proc = subprocess.Popen(
                    cmd,
                    stdin=subprocess.PIPE,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    text=True,
                )
                stdout, stderr = proc.communicate()
                self.logger.debug("==Stdout of tree distance calculation==")
                self.logger.debug(stdout)
                if proc.returncode == 0:
                    self.logger.info("完成计算树的距离")
                    tree_distance_completed = True
                    break

                print(self.translator.text("Standard error:", "标准错误:"))
                print(stderr)
                self.logger.warning(f"树距离计算失败，命令: {cmd[0]}")
            except Exception as e:
                self.logger.warning(f"树距离脚本执行失败，命令 {cmd[0]}: {e}")

        if not tree_distance_completed:
            sys.exit(1)
        try:
            heatmap_paths = create_tree_distance_heatmaps(
                path_trees_summary / "tree_distance_matrix.tsv",
                path_trees_summary / "rf_distance_matrix.tsv",
                path_trees_summary / "tree_distance_heatmaps.png",
                path_trees_summary / "tree_distance_heatmaps.pdf",
                figure_title=self.translator.text(
                    "Tree Distance Comparison Across Inference Methods",
                    "不同建树方法的树距离比较",
                ),
                tree_title=self.translator.text("TreeDist Matrix", "TreeDist 矩阵"),
                rf_title=self.translator.text("Robinson-Foulds Matrix", "Robinson-Foulds 矩阵"),
                tree_colorbar_label=self.translator.text("TreeDist", "TreeDist"),
                rf_colorbar_label=self.translator.text("RF Distance", "RF 距离"),
            )
            self.logger.info(
                f"树距离热图已保存: {heatmap_paths[0]} 和 {heatmap_paths[1]}"
            )
        except Exception as e:
            self.logger.warning(f"生成树距离热图失败: {e}")

        summary_file = path_trees_summary / "analysis_summary.txt"

        with open(summary_file, "w", encoding="utf-8") as f:
            f.write("=" * 60 + "\n")
            f.write(
                self.translator.text(
                    "          Phylogenetic analysis summary\n",
                    "          进化树分析结果总结\n",
                )
            )
            f.write("=" * 60 + "\n\n")

            f.write(
                self.translator.text(
                    f"Analysis time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n",
                    f"分析时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n",
                )
            )
            f.write(self.translator.text(f"Input file: {self.input_file}\n", f"输入文件: {self.input_file}\n"))
            f.write(self.translator.text(f"Number of sequences: {len(sequences)}\n", f"序列数量: {len(sequences)}\n"))
            f.write(self.translator.text(f"Sequence length: {len(sequences[0].seq)} nucleotides\n\n", f"序列长度: {len(sequences[0].seq)}\n\n"))

            f.write(self.translator.text("Methods and results:\n", "分析方法和结果:\n"))
            f.write("-" * 40 + "\n")

            methods = [
                (self.translator.text("Distance method (DNA/CDS)", "距离法 (Distance Method)"), tree_files[0]),
                (self.translator.text("Maximum likelihood (DNA/CDS)", "极大似然法 (Maximum Likelihood)"), tree_files[1]),
                (self.translator.text("Bayesian inference (DNA/CDS)", "贝叶斯法 (Bayesian Method)"), tree_files[2]),
                (self.translator.text("Parsimony (DNA/CDS)", "简约法 (Parsimony Method)"), tree_files[3]),
            ]

            for method, tree_file in methods:
                status = self.translator.text("OK", "✓ 成功") if tree_file and tree_file.exists() else self.translator.text("FAILED", "✗ 失败")
                f.write(f"{method:<30} {status}\n")
                if tree_file and tree_file.exists():
                    f.write(
                        self.translator.text(
                            f"    Output file: {tree_file.resolve().relative_to(self.output_dir.resolve())}\n",
                            f"    输出文件: {tree_file.resolve().relative_to(self.output_dir.resolve())}\n",
                        )
                    )
                f.write("\n")

            f.write(self.translator.text("Output directory structure:\n", "输出目录结构:\n"))
            f.write("-" * 40 + "\n")
            f.write(f"{self.output_dir}/\n")
            f.write(self.translator.text("├── distance_method/        # distance-based results\n", "├── distance_method/        # 距离法结果\n"))
            f.write(self.translator.text("├── maximum_likelihood/     # maximum likelihood results\n", "├── maximum_likelihood/     # 极大似然法结果\n"))
            f.write(self.translator.text("├── bayesian_method/        # Bayesian inference results\n", "├── bayesian_method/        # 贝叶斯法结果\n"))
            f.write(self.translator.text("├── parsimony_method/       # parsimony results\n", "├── parsimony_method/       # 简约法结果\n"))
            f.write(self.translator.text("├── visualizations/         # tree visualizations\n", "├── visualizations/         # 树图可视化\n"))
            f.write(self.translator.text("└── tree_summary/           # tree distance analyses\n\n", "└── tree_summary/           # 树距离矩阵与热图\n\n"))

            f.write(self.translator.text("Tree summary outputs:\n", "tree_summary 目录内容:\n"))
            f.write("-" * 40 + "\n")
            f.write("- tree_meta_data.tsv\n")
            f.write("- tree_distance_matrix.tsv\n")
            f.write("- rf_distance_matrix.tsv\n")
            if heatmap_paths:
                f.write("- tree_distance_heatmaps.png\n")
                f.write("- tree_distance_heatmaps.pdf\n")
            f.write("- analysis_summary.txt\n\n")

            f.write(self.translator.text("Notes for DNA/CDS analyses:\n", "注意事项:\n"))
            f.write("-" * 40 + "\n")
            f.write(self.translator.text("1. All tree files are in Newick format (.nwk)\n", "1. 所有树文件均为Newick格式(.nwk)\n"))
            f.write(self.translator.text("2. Use FigTree, iTOL, or other viewers to inspect and edit trees further\n", "2. 可使用FigTree、iTOL等工具进一步查看和编辑\n"))
            f.write(self.translator.text("3. Maximum likelihood results include bootstrap support values\n", "3. 极大似然法结果包含bootstrap支持值\n"))
            f.write(self.translator.text("4. Bayesian results include posterior probability support values\n", "4. 贝叶斯法结果包含后验概率支持值\n"))
            f.write(self.translator.text("5. tree_summary includes both TSV distance matrices and a combined heatmap figure\n", "5. tree_summary 目录同时包含距离矩阵 TSV 和合并热图\n"))

        self.logger.info(f"结果总结已保存: {summary_file}")

    def reroot_tree_by_MAD(self, tree_files):
        ret_paths: List[Path] = []

        for tree_file in tree_files:
            if tree_file is None or not Path(tree_file).exists():
                ret_paths.append(tree_file)
                continue

            tree_file = str(tree_file)

            path_output = tree_file + ".rooted"

            if not Path(self.mad_method_path).exists():
                self.logger.warning(f"MAD executable not found: {self.mad_method_path}")
                ret_paths.append(Path(tree_file))
                continue

            cmd = [
                self.mad_method_path,
                str(tree_file),
            ]
            self.logger.info(cmd)

            proc = subprocess.Popen(
                cmd,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            stdout, stderr = proc.communicate()
            self.logger.debug("==Stdout of MAD rerooting==")
            self.logger.debug(stdout)
            if proc.returncode != 0:
                print(self.translator.text("Standard error:", "标准错误:"))
                print(stderr)
                self.logger.error(f"MAD 定根失败: {tree_file}")
                ret_paths.append(Path(tree_file))
            else:
                self.logger.info(f"定根完成，{tree_file}")
                ret_paths.append(Path(path_output))
        return ret_paths

    def restore_names_in_trees(self, tree_files):
        self.logger.info("Restoring original sequence names in trees...")
        if self.new_name2ori_name_dict is None:
            self.logger.warning("No name mapping available; skipping name restoration in trees")
            return tree_files

        ret_paths: List[Path] = []
        for tree_file in tree_files:
            if tree_file is None or not Path(tree_file).exists():
                ret_paths.append(tree_file)
                continue

            tree_file = Path(tree_file)
            path_output = tree_file.with_name(
                tree_file.stem + ".renamed" + tree_file.suffix
            )

            try:
                t = Tree(str(tree_file))
                for leaf in t.leaves():
                    if leaf.name in self.new_name2ori_name_dict:
                        leaf.name = self.new_name2ori_name_dict[leaf.name]

                t.write(outfile=str(path_output))
                ret_paths.append(path_output)
                self.logger.info(f"已恢复树文件名称: {tree_file}")
            except Exception as e:
                self.logger.error(f"恢复树文件名称失败 {tree_file}: {e}")
                ret_paths.append(tree_file)

        return ret_paths

    def compare_tree_ktreedist(self, tree_files):
        ret_paths: List[Path] = []

        lst_of_contents = []
        for tree_file in tree_files:
            if not isinstance(tree_file, Path):
                tree_file = Path(tree_file)
            content = tree_file.read_text().rstrip()
            if not content.endswith(";"):
                content += ";"
            lst_of_contents.append(content)
            # 未完成，需要计算两两之间的距离

            tree_file = str(tree_file)

            # 运行IQ-TREE
            cmd = [self.ktreedist_method_path, "-rt", tree_file, "-ct", tree_file, "-t"]
            self.logger.info(cmd)

            proc = subprocess.Popen(
                cmd,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            stdout, stderr = proc.communicate()
            self.logger.debug("==Stdout of Ktree dist==")
            self.logger.debug(stdout)
            if proc.returncode != 0:
                print(self.translator.text("Standard error:", "标准错误:"))
                print(stderr)
                sys.exit(1)
            else:
                self.logger.info(f"定根完成，{tree_file}")

    def ladderize_tree_by_ete4(self, tree_files) -> List[Path]:
        from ete4 import Tree

        ret_paths: List[Path] = []
        for tree_file in tree_files:
            if tree_file is None or not Path(tree_file).exists():
                ret_paths.append(tree_file)
                continue

            # 保证是 Path
            tree_file = Path(tree_file)
            # 输出路径：原文件名后面加 .ladderize
            path_output = tree_file.with_name(tree_file.name + ".ladderize")

            # 读入树
            t = Tree(str(tree_file))

            # 阶梯化（默认递增，reverse=True 为递减）
            t.ladderize()

            # 保存到新文件
            t.write(outfile=str(path_output))

            ret_paths.append(path_output)

        return ret_paths

    def run_pipeline(self):
        """运行完整的分析管道"""
        self.logger.info(f"Starting phylogenetic pipeline...This is: {Path.cwd()}")
        """
        Statement: 注意你的fasta序列最好所有的字符串长度不大于10，这样才能得到成功让phylip转为它能够识别的格式
        """

        # 1. 检查软件依赖
        self.check_and_install_software()

        # 2. 验证输入文件
        sequences = self.validate_input()

        self.name_convention()

        # 3. 格式转换
        phylip_file = self.convert_to_phylip()

        # 4. 四种方法建树
        tree_files = [None, None, None, None]

        entry_path = Path.cwd()

        if self.runtime_settings["distance"]["enabled"]:
            os.chdir(entry_path)
            tree_files[0] = self.distance_method(phylip_file)
            self.logger.info(
                self.translator.marker_complete("distance")
                if tree_files[0]
                else self.translator.marker_failed("distance")
            )
        else:
            self.logger.info(self.translator.marker_skipped("distance"))

        if self.runtime_settings["maximum_likelihood"]["enabled"]:
            os.chdir(entry_path)
            tree_files[1] = self.maximum_likelihood_method()
            self.logger.info(
                self.translator.marker_complete("maximum_likelihood")
                if tree_files[1]
                else self.translator.marker_failed("maximum_likelihood")
            )
        else:
            self.logger.info(self.translator.marker_skipped("maximum_likelihood"))

        if self.runtime_settings["bayesian"]["enabled"]:
            os.chdir(entry_path)
            tree_files[2] = self.bayesian_method()
            self.logger.info(
                self.translator.marker_complete("bayesian")
                if tree_files[2]
                else self.translator.marker_failed("bayesian")
            )
        else:
            self.logger.info(self.translator.marker_skipped("bayesian"))

        if self.runtime_settings["parsimony"]["enabled"]:
            os.chdir(entry_path)
            tree_files[3] = self.parsimony_method(phylip_file)
            self.logger.info(
                self.translator.marker_complete("parsimony")
                if tree_files[3]
                else self.translator.marker_failed("parsimony")
            )
        else:
            self.logger.info(self.translator.marker_skipped("parsimony"))

        os.chdir(entry_path)
        rooted_tree_files = self.reroot_tree_by_MAD(tree_files)
        os.chdir(entry_path)
        rooted_ladd_tree_files = self.ladderize_tree_by_ete4(rooted_tree_files)

        result_trees = self.restore_names_in_trees(rooted_ladd_tree_files)

        os.chdir(entry_path)
        self.visualize_trees(result_trees)

        self.generate_summary(result_trees, sequences)

        self.logger.info("Phylogenetic pipeline completed!")
        self.logger.info(f"Results saved to: {self.output_dir}")


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="自动化进化树构建管道")
    parser.add_argument("input_file", help="输入的比对FASTA文件")
    parser.add_argument(
        "-o", "--output", default="phylo_results", help="输出目录 (默认: phylo_results)"
    )
    parser.add_argument(
        "--config",
        help="由 oneBuilder GUI 生成的可选运行时 JSON 配置文件",
    )

    args = parser.parse_args()
    runtime_config = load_runtime_config(args.config)

    # 创建并运行管道
    pipeline = PhylogeneticPipeline(args.input_file, args.output, runtime_config=runtime_config)
    pipeline.run_pipeline()


if __name__ == "__main__":
    main()
