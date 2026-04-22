#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Automated phylogenetic tree construction pipeline - protein sequences version
Purpose: Given an aligned FASTA file, automatically produce phylogenetic trees
using four different inference approaches.

Author: automated script
Version: 1.0 (protein)
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
from onebuilder_runtime_config import load_runtime_config, protein_runtime_settings
from tree_summary_plot import create_tree_distance_heatmaps

mpl.set_loglevel("warning")  # or "error"
mpl.use("Agg", force=True)
import warnings
from ete4 import Tree

warnings.filterwarnings("ignore")


def _normalize_phylip_menu_input(menu_overrides, default_input):
    if not menu_overrides:
        return default_input
    lines = [str(item).strip() for item in menu_overrides if str(item).strip()]
    if not lines:
        return default_input
    if lines[-1].upper() != "Y":
        lines.append("Y")
    return "\n".join(lines) + "\n"


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


def _build_protpars_menu_input(settings):
    overrides = settings.get("protpars_menu_overrides")
    if overrides:
        return _normalize_phylip_menu_input(overrides, "4\n5\nY\n")

    lines = ["4", "5"]
    outgroup_index = settings.get("protpars_outgroup_index")
    if outgroup_index is not None:
        lines.extend(["O", str(outgroup_index)])
    return _normalize_phylip_menu_input(lines, "4\n5\nY\n")


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


def _build_protein_mrbayes_commands(nexus_file_name, settings):
    custom_block = _normalize_mrbayes_lines(settings.get("command_block"))
    commands = ["set autoclose=yes nowarn=yes;\n", f"execute {nexus_file_name}\n"]
    if custom_block:
        commands.extend(custom_block)
        if not custom_block[-1].strip().lower().startswith("quit"):
            commands.append("quit;\n")
        return commands

    protein_model_prior = str(settings.get("protein_model_prior", "mixed")).strip()
    if protein_model_prior:
        commands.append(f"prset aamodelpr={protein_model_prior};\n")
    commands.append(f"lset rates={settings['rates']};\n")

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


class ProteinPhylogeneticPipeline:
    def __init__(self, input_file, output_dir="phylo_results_protein", runtime_config=None):
        """
        Initialize the protein phylogenetic tree construction pipeline

        Args:
            input_file (str): Path to the input aligned FASTA file
            output_dir (str): Path to the output directory
        """
        self.input_file = Path(input_file).resolve()
        self.original_file = None  # original input file path
        self.new_name2ori_name_dict: Dict[str, str] = None
        self.output_dir = Path(output_dir).resolve()
        self.script_dir = Path(__file__).parent.resolve()
        self.runtime_settings = protein_runtime_settings(runtime_config)

        # initialize logging and output directories
        self.setup_logging()
        self.create_directories()

        # software paths (will be set during checks)
        self.phylip_commands: Dict[str, str] = {}
        self.iqtree_path = None
        self.mrbayes_path = None

        # MAD executable for rerooting
        self.mad_method_path = str(self.script_dir / "third_party" / "mad" / "mad")
        # Path to R script for pairwise tree distances (optional)
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

    def setup_logging(self):
        """Configure logging system"""
        log_file = (
            f"phylo_pipeline_protein_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"
        )
        logging.basicConfig(
            level=logging.DEBUG,
            format="%(asctime)s - %(levelname)s - %(message)s",
            handlers=[logging.FileHandler(log_file), logging.StreamHandler()],
        )
        self.logger = logging.getLogger(__name__)

    def name_convention(self):
        """Set name convention for input sequences.

        PHYLIP's traditional format restricts sequence IDs, so we rename
        sequence identifiers to a simple seqN scheme and keep a mapping.
        """
        self.input_file = Path(self.input_file).resolve()
        iterator = SeqIO.parse(self.input_file, "fasta")
        records = list(iterator)
        # If needed, convert sequence IDs to indexed names
        new_name2ori_name_dict: Dict[str, str] = {}
        for index, record in enumerate(records, start=1):
            new_name = f"seq{index}"
            new_name2ori_name_dict[new_name] = record.id
            # DO NOT need to care about record.name, this is not for fasta
            # if record.name:
            #     self.logger.warning(f"Record {record.id} has a name: {record.name}, it will be replaced with ID.")
            record.id = f"seq{index}"
        self.new_name2ori_name_dict = new_name2ori_name_dict
        self.original_file = self.input_file
        self.input_file = self.output_dir / (
            self.input_file.stem + "_tmp" + self.input_file.suffix
        )

        SeqIO.write(records, self.input_file, "fasta")

        self.logger.info("Setting name convention for input sequences...")

    def create_directories(self):
        """Create output directory structure"""
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

        self.logger.info(f"Created output directory: {self.output_dir}")

    def check_and_install_software(self):
        """Check and (optionally) install required external software"""
        self.logger.info("Checking required software...")

        self.iqtree_path = self._resolve_command(["iqtree", "iqtree2", "iqtree3"])
        self.mrbayes_path = self._resolve_command(["mb", "mrbayes"])
        self.phylip_commands = {
            "protdist": self._resolve_command(
                ["protdist"], ".pixi/envs/default/share/phylip-*/exe/protdist"
            ),
            "neighbor": self._resolve_command(
                ["neighbor"], ".pixi/envs/default/share/phylip-*/exe/neighbor"
            ),
            "protpars": self._resolve_command(
                ["protpars"], ".pixi/envs/default/share/phylip-*/exe/protpars"
            ),
        }

        if not self.iqtree_path:
            self.logger.error("Failed to find IQ-TREE executable.")
            sys.exit(1)
        if not self.mrbayes_path:
            self.logger.error("Failed to find MrBayes executable.")
            sys.exit(1)

        missing_phylip_commands = [
            command_name
            for command_name, resolved_path in self.phylip_commands.items()
            if not resolved_path
        ]
        if missing_phylip_commands:
            self.logger.error(
                f"Failed to find required PHYLIP commands: {', '.join(missing_phylip_commands)}"
            )
            sys.exit(1)

        if self.mad_method_path and not Path(self.mad_method_path).exists():
            self.logger.warning(
                f"MAD rerooting executable not found: {self.mad_method_path}"
            )

    def validate_input(self):
        """Validate input FASTA alignment"""
        if not self.input_file.exists():
            self.logger.error(f"Input file not found: {self.input_file}")
            sys.exit(1)

        # Check whether the file is a valid FASTA alignment
        try:
            sequences = list(SeqIO.parse(self.input_file, "fasta"))
            if len(sequences) < 3:
                self.logger.error(
                    "At least 3 sequences are required to infer a phylogeny"
                )
                sys.exit(1)

            # Check for protein alphabet (basic check against standard amino acids)
            protein_chars = set("ACDEFGHIKLMNPQRSTVWY*X-")
            for seq in sequences[:3]:  # inspect a few sequences
                seq_chars = set(str(seq.seq).upper())
                if not seq_chars.issubset(protein_chars):
                    self.logger.warning(
                        "Sequences may not be standard protein sequences; please verify input"
                    )

            self.logger.info(f"Input validation successful: {len(sequences)} sequences")
            return sequences

        except Exception as e:
            self.logger.error(f"Input file format error: {e}")
            sys.exit(1)

    def convert_to_phylip(self):
        """Convert FASTA alignment to PHYLIP format"""
        phylip_file = self.output_dir / "alignment.phy"

        print(f"Converting to PHYLIP format... {self.input_file}")
        records = SeqIO.parse(self.input_file, "fasta")
        # NOTE:
        # PHYLIP has two common variants:
        # strict: sequence IDs limited to 10 characters
        # relaxed: allows longer IDs
        # Biopython's default 'phylip' writer produces strict PHYLIP.
        # If your sequence IDs are long, consider using 'phylip-relaxed'.
        SeqIO.write(records, phylip_file, "phylip")

        self.logger.info(f"Converted to PHYLIP format: {phylip_file}")
        return phylip_file

    def distance_method(self, phylip_file):
        """Distance-based tree inference using PHYLIP protein distance method"""
        self.logger.info("Starting distance-based inference...")

        work_dir = self.output_dir / "distance_method"
        self.logger.info(f"Current working directory: {os.getcwd()}")
        shutil.copy(phylip_file, work_dir / "infile")
        os.chdir(work_dir)
        self.logger.info(f"Changed to work_dir: {os.getcwd()}")

        try:
            # 1. compute protein distance matrix with protdist
            protdist_input = _normalize_phylip_menu_input(
                self.runtime_settings["distance"].get("protdist_menu_overrides"),
                "Y\n",
            )
            proc = subprocess.Popen(
                [self.phylip_commands["protdist"]],
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            stdout, stderr = proc.communicate(input=protdist_input)
            # Now stdout/stderr are available for logging/debugging
            self.logger.debug("==Stdout of PHYLIP protdist==")
            self.logger.debug(stdout)
            if proc.returncode != 0:
                print("Standard error:")
                print(stderr)
                sys.exit(1)

            # 2. infer tree with Neighbor-Joining (neighbor)
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
            self.logger.debug("==Stdout of PHYLIP neighbor==")
            self.logger.debug(stdout)
            if proc.returncode != 0:
                print("Standard error:")
                print(stderr)
                sys.exit(1)

            # rename output files
            if os.path.exists("outtree"):
                shutil.move("outtree", "distance_tree.nwk")
            if os.path.exists("outfile"):
                shutil.move("outfile", "distance_tree.nwk.txt")

            # check branch lengths: protdist/neighbor may produce negative branch lengths
            content = Path("distance_tree.nwk").read_text().replace("\n", "")
            my_tree = Tree(content)
            has_value_less0 = False
            for node in my_tree.traverse():
                # Check whether dist is None; root may have no distance
                if node.dist is not None:
                    if node.dist < 0:
                        node.dist = 0.0
                        has_value_less0 = True
                else:
                    # handle None values
                    node.dist = 0.0
            if has_value_less0:
                self.logger.warning(
                    "Found negative branch lengths; set negatives to 0 and rewrote tree"
                )
                my_tree.write(outfile="distance_tree.nwk")

            self.logger.info("Distance-based inference completed")
            return work_dir / "distance_tree.nwk"

        except Exception as e:
            self.logger.error(f"Distance-based method failed: {e}")
            return None
        finally:
            if os.path.exists("infile"):
                os.remove("infile")

    def parsimony_method(self, phylip_file):
        """Maximum parsimony inference using PHYLIP's protpars (protein version)"""
        self.logger.info("Starting parsimony inference...")

        work_dir = self.output_dir / "parsimony_method"
        shutil.copy(phylip_file, work_dir / "infile")
        os.chdir(work_dir)

        self.logger.info(f"Changed to work_dir: {os.getcwd()}")

        try:
            # Run protpars for protein parsimony analysis
            protpars_input = _normalize_phylip_menu_input(
                self.runtime_settings["parsimony"].get("protpars_menu_overrides"),
                _build_protpars_menu_input(self.runtime_settings["parsimony"]),
            )
            proc = subprocess.Popen(
                [self.phylip_commands["protpars"]],
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            stdout, stderr = proc.communicate(input=protpars_input)
            self.logger.debug("==Stdout of PHYLIP protpars==")
            self.logger.debug(stdout)
            if proc.returncode != 0:
                print("Standard error:")
                print(stderr)
                sys.exit(1)

            # rename output files
            if os.path.exists("outtree"):
                shutil.move("outtree", "parsimony_tree.nwk")
            if os.path.exists("outfile"):
                shutil.move("outfile", "parsimony_output.txt")

            self.logger.info("Parsimony inference completed")
            return work_dir / "parsimony_tree.nwk"

        except Exception as e:
            self.logger.error(f"Parsimony method failed: {e}")
            return None
        finally:
            if os.path.exists("infile"):
                os.remove("infile")
            os.chdir(self.output_dir.parent)

    def maximum_likelihood_method(self):
        """Maximum likelihood inference using IQ-TREE (protein models)"""
        self.logger.info("Starting maximum likelihood inference (IQ-TREE)...")

        if not self.iqtree_path:
            self.logger.error("IQ-TREE not found")
            return None

        work_dir = self.output_dir / "maximum_likelihood"
        input_file = self.input_file
        ml_settings = self.runtime_settings["maximum_likelihood"]

        os.chdir(work_dir)
        self.logger.info(f"Changed to work_dir: {os.getcwd()}")
        try:
            # run IQ-TREE and let it select the best protein substitution model
            cmd = [
                self.iqtree_path,
                "-s",
                str(input_file),
                "-m",
                str(ml_settings["model_strategy"]),
                "-bb",
                str(ml_settings["bootstrap_replicates"]),
                "-pre",
                "ml_tree",  # output prefix
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
                print("Standard error:")
                print(stderr)
                sys.exit(1)
            else:
                self.logger.info("Maximum likelihood inference completed")
                return work_dir / "ml_tree.treefile"

        except Exception as e:
            self.logger.error(f"Maximum likelihood method failed: {e}")
            return None
        finally:
            os.chdir(self.output_dir.parent)

    def bayesian_method(self):
        """Bayesian phylogenetic inference using MrBayes (protein models)"""
        self.logger.info("Starting Bayesian inference (MrBayes)...")

        if not self.mrbayes_path:
            self.logger.warning(
                "MrBayes path not explicitly set; will try to use system 'mb' executable"
            )
            mb_exec = "mb"
        else:
            mb_exec = str(self.mrbayes_path)

        work_dir = self.output_dir / "bayesian_method"
        work_dir.mkdir(parents=True, exist_ok=True)
        bayesian_settings = self.runtime_settings["bayesian"]

        self.logger.info(f"Changed to work_dir: {os.getcwd()}")

        try:
            # create MrBayes input file (NEXUS)
            nexus_file = work_dir / "alignment.nex"
            self.create_nexus_file(nexus_file)

            # prepare MrBayes command/script
            mb_script = work_dir / "mb_commands.txt"
            mb_input = _build_protein_mrbayes_commands(nexus_file.name, bayesian_settings)

            # write script file (for reproducibility/debugging)
            mb_input_str = "".join(mb_input)
            mb_script.write_text(mb_input_str, encoding="utf-8")

            # run MrBayes
            os.chdir(work_dir)
            proc = subprocess.Popen(
                [mb_exec],
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            stdout, stderr = proc.communicate(input=mb_input_str)
            self.logger.debug("==Stdout of MrBayes==")
            self.logger.debug(stdout)
            if proc.returncode != 0:
                print("Standard error:")
                print(stderr)
                sys.exit(1)

            # find consensus tree output (*.con.tre)
            consensus_tree = None
            for file in Path.cwd().glob("*.con.tre"):
                consensus_tree = file
                try:
                    # Try to import a helper to convert Nexus trees to Newick
                    sys.path.append(str(Path(__file__).resolve().parents[0]))
                    from help_utils import parse_nexus_tree

                    out_file = Path(str(consensus_tree) + ".nwk")
                    parse_nexus_tree(consensus_tree, out_file)
                    consensus_tree = out_file
                except ImportError:
                    self.logger.warning(
                        "parse_mrb_tree module not found; keeping original .con.tre file"
                    )
                    # Optionally implement conversion here if needed
                break

            if consensus_tree:
                self.logger.info("Bayesian inference completed")
                return consensus_tree
            else:
                self.logger.error("MrBayes consensus tree output not found")
                return None

        except Exception as e:
            self.logger.error(f"Bayesian method failed: {e}")
            return None
        finally:
            os.chdir(self.output_dir.parent)

    def create_nexus_file(self, nexus_file):
        """Create a NEXUS file for MrBayes (protein data)"""
        sequences = list(SeqIO.parse(self.input_file, "fasta"))

        with open(nexus_file, "w") as f:
            f.write("#NEXUS\n\n")
            f.write("begin data;\n")
            f.write(
                f"    dimensions ntax={len(sequences)} nchar={len(sequences[0].seq)};\n"
            )
            f.write("    format datatype=PROTEIN gap=- missing=?;\n")
            f.write("    matrix\n")

            for seq in sequences:
                f.write(f"    {seq.id:<20} {seq.seq}\n")

            f.write("    ;\n")
            f.write("end;\n")

    def restore_names_in_trees(self, tree_files):
        """Restore original sequence names in tree files"""
        self.logger.info("Restoring original sequence names in trees...")
        if self.new_name2ori_name_dict is None:
            self.logger.warning(
                "No name mapping available; skipping name restoration in trees"
            )
            return tree_files
        """Use the ete4 library to read and rewrite trees with original names"""
        ret_paths: List[Path] = []
        for tree_file in tree_files:
            if tree_file is None or not tree_file.exists():
                ret_paths.append(None)
                self.logger.warning(f"Tree file not found, skipping: {tree_file}")
                continue

            # Ensure it's a Path
            tree_file = Path(tree_file)
            # Output path: original filename with .renamed appended
            path_output = tree_file.with_name(
                tree_file.stem + ".renamed" + tree_file.suffix
            )

            try:
                # Read the tree
                t = Tree(str(tree_file))

                # Replace leaf names using mapping
                for leaf in t.leaves():
                    if leaf.name in self.new_name2ori_name_dict:
                        leaf.name = self.new_name2ori_name_dict[leaf.name]
                    else:
                        self.logger.warning(
                            f"Leaf name {leaf.name} not found in mapping dictionary"
                        )

                # Write to a new file
                t.write(outfile=str(path_output))

                ret_paths.append(path_output)
                self.logger.info(f"Restored names in tree: {tree_file}")

            except Exception as e:
                self.logger.error(f"Failed to restore names in {tree_file}: {e}")
                ret_paths.append(tree_file)  # return original tree file
        return ret_paths

    def visualize_trees(self, tree_files, method_names):
        """Generate visualizations for inferred phylogenies"""
        self.logger.info("Generating tree visualizations...")

        vis_dir = self.output_dir / "visualizations"
        vis_dir.mkdir(parents=True, exist_ok=True)

        # Configure fonts (include Sino-fonts as fallback if available)
        try:
            plt.rcParams["font.sans-serif"] = ["SimHei", "DejaVu Sans"]
            plt.rcParams["axes.unicode_minus"] = False
        except:
            self.logger.error("plt.rcParams setting error.")
            pass

        fig, axes = plt.subplots(2, 2, figsize=(16, 12))
        axes = axes.flatten()

        for i, (tree_file, method) in enumerate(zip(tree_files, method_names)):
            if tree_file and tree_file.exists():
                try:
                    list_trees = Phylo.parse(tree_file, "newick")
                    trees = list(list_trees)
                    Phylo.draw(trees[0], axes=axes[i], do_show=False)
                    if method.startswith("Parsimony"):
                        title = f"{method} Method (unrooted), # of trees: {len(trees)}"
                    else:
                        title = f"{method} Method (protein)"
                    axes[i].set_title(title, fontsize=14)
                except Exception as e:
                    axes[i].text(
                        0.5,
                        0.5,
                        f"Failed to read tree\n{method}",
                        ha="center",
                        va="center",
                        transform=axes[i].transAxes,
                    )
                    self.logger.error(f"Failed to visualize {method}: {e}")
            else:
                axes[i].text(
                    0.5,
                    0.5,
                    f"Tree file not found\n{method}",
                    ha="center",
                    va="center",
                    transform=axes[i].transAxes,
                )
        plt.tight_layout()
        plt.savefig(
            vis_dir / "all_protein_trees_comparison.png", dpi=300, bbox_inches="tight"
        )
        plt.savefig(vis_dir / "all_protein_trees_comparison.pdf", bbox_inches="tight")
        plt.close()

        self.logger.info("Visualization completed")

    def generate_summary(self, tree_files, sequences):
        """Produce a summary report of analyses and outputs"""
        path_trees_summary = self.output_dir / "tree_summary"
        path_trees_summary.mkdir(parents=True, exist_ok=True)

        os.chdir(path_trees_summary)

        path_tree_info = path_trees_summary / "tree_meta_data.tsv"
        with open(path_tree_info, "w") as f:
            f.write(f"NJ_phylip\t{tree_files[0] if tree_files[0] else 'NULL'}\n")
            f.write(f"ML_iqtree\t{tree_files[1] if tree_files[1] else 'NULL'}\n")
            f.write(f"BI_mrbayes\t{tree_files[2] if tree_files[2] else 'NULL'}\n")
            f.write(f"MP_phylip\t{tree_files[3] if tree_files[3] else 'NULL'}\n")

        # call R script to calculate pairwise tree distances if available
        heatmap_paths = []
        if Path(self.cal_treedist_method_path).exists():
            r_commands = [
                ["Rscript", self.cal_treedist_method_path, str(path_tree_info)],
                [
                    "R",
                    "--slave",
                    "-f",
                    self.cal_treedist_method_path,
                    "--args",
                    str(path_tree_info),
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
                        self.logger.info("Tree distance calculation completed")
                        tree_distance_completed = True
                        break

                    print("Standard error:")
                    print(stderr)
                    self.logger.warning(
                        f"Tree distance calculation failed with command: {cmd[0]}"
                    )
                except Exception as e:
                    self.logger.warning(
                        f"Tree distance script execution failed with command {cmd[0]}: {e}"
                    )

            if not tree_distance_completed:
                self.logger.warning("Tree distance calculation failed")
            else:
                try:
                    heatmap_paths = create_tree_distance_heatmaps(
                        path_trees_summary / "tree_distance_matrix.tsv",
                        path_trees_summary / "rf_distance_matrix.tsv",
                        path_trees_summary / "tree_distance_heatmaps.png",
                        path_trees_summary / "tree_distance_heatmaps.pdf",
                    )
                    self.logger.info(
                        f"Tree distance heatmaps saved: {heatmap_paths[0]} and {heatmap_paths[1]}"
                    )
                except Exception as e:
                    self.logger.warning(
                        f"Failed to generate tree distance heatmaps: {e}"
                    )
        else:
            self.logger.warning(
                f"Tree distance R script not found: {self.cal_treedist_method_path}"
            )

        summary_file = path_trees_summary / "analysis_summary.txt"

        with open(summary_file, "w", encoding="utf-8") as f:
            f.write("=" * 60 + "\n")
            f.write("          Protein phylogenetic analysis summary\n")
            f.write("=" * 60 + "\n\n")

            f.write(f"Analysis time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"Input file: {self.input_file}\n")
            f.write(f"Number of sequences: {len(sequences)}\n")
            f.write(f"Sequence length: {len(sequences[0].seq)} amino acid residues\n\n")

            f.write("Methods and results:\n")
            f.write("-" * 40 + "\n")

            methods = [
                ("Distance method (Protein)", tree_files[0]),
                ("Maximum likelihood (Protein)", tree_files[1]),
                ("Bayesian inference (Protein)", tree_files[2]),
                ("Parsimony (Protein)", tree_files[3]),
            ]

            for method, tree_file in methods:
                status = "OK" if tree_file and tree_file.exists() else "FAILED"
                f.write(f"{method:<40} {status}\n")
                if tree_file and tree_file.exists():
                    try:
                        rel_path = tree_file.resolve().relative_to(
                            self.output_dir.resolve()
                        )
                        f.write(f"    Output file: {rel_path}\n")
                    except ValueError:
                        f.write(f"    Output file: {tree_file}\n")
                f.write("\n")

            f.write("Output directory structure:\n")
            f.write("-" * 40 + "\n")
            f.write(f"{self.output_dir}/\n")
            f.write("├── distance_method/        # distance-based results\n")
            f.write("├── parsimony_method/       # parsimony results\n")
            f.write("├── maximum_likelihood/     # maximum likelihood results\n")
            f.write("├── bayesian_method/        # Bayesian inference results\n")
            f.write("├── visualizations/         # tree visualizations\n")
            f.write("└── tree_summary/           # tree distance analyses\n\n")

            f.write("Tree summary outputs:\n")
            f.write("-" * 40 + "\n")
            f.write("- tree_meta_data.tsv\n")
            f.write("- tree_distance_matrix.tsv\n")
            f.write("- rf_distance_matrix.tsv\n")
            if heatmap_paths:
                f.write("- tree_distance_heatmaps.png\n")
                f.write("- tree_distance_heatmaps.pdf\n")
            f.write("- analysis_summary.txt\n\n")

            f.write("Notes for protein analyses:\n")
            f.write("-" * 40 + "\n")
            f.write("1. All tree files are in Newick format (.nwk)\n")
            f.write(
                "2. Distance method computes a protein distance matrix using protdist\n"
            )
            f.write("3. Parsimony uses protpars (protein parsimony)\n")
            f.write(
                "4. IQ-TREE will auto-select the best-fitting protein substitution model\n"
            )
            f.write(
                "5. MrBayes runs with a mixed amino-acid prior for model averaging\n"
            )
            f.write(
                "6. tree_summary includes both TSV distance matrices and a combined heatmap figure\n"
            )
            f.write(
                "7. Use FigTree, iTOL, or other viewers to inspect and edit trees further\n"
            )

        self.logger.info(f"Summary saved: {summary_file}")

    def reroot_tree_by_MAD(self, tree_files):
        """Reroot trees using the MAD method"""
        ret_paths: List[Path] = []

        for tree_file in tree_files:
            if tree_file is None or not tree_file.exists():
                ret_paths.append(None)
                continue

            tree_file_str = str(tree_file)
            path_output = tree_file_str + ".rooted"

            if not Path(self.mad_method_path).exists():
                self.logger.warning(f"MAD tool not found: {self.mad_method_path}")
                ret_paths.append(tree_file)  # return original tree file
                continue

            cmd = [
                self.mad_method_path,
                tree_file_str,
            ]
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
                self.logger.debug("==Stdout of MAD rerooting==")
                self.logger.debug(stdout)
                if proc.returncode != 0:
                    print("Standard error:")
                    print(stderr)
                    self.logger.error(f"MAD rerooting failed: {tree_file}")
                    ret_paths.append(tree_file)  # return original tree file
                else:
                    self.logger.info(f"Rerooting completed: {tree_file}")
                    ret_paths.append(Path(path_output))
            except Exception as e:
                self.logger.error(f"MAD rerooting exception: {e}")
                ret_paths.append(tree_file)  # return original tree file

        return ret_paths

    def ladderize_tree_by_ete4(self, tree_files, make_branch_equal=False) -> List[Path]:
        ret_paths: List[Path] = []

        for tree_file in tree_files:
            if tree_file is None or not tree_file.exists():
                ret_paths.append(tree_file)
                continue

            # Ensure it's a Path
            tree_file = Path(tree_file)
            # Output path: original filename with .ladderize appended
            path_output = tree_file.with_name(tree_file.name + ".ladderize")

            try:
                # Read the tree
                t = Tree(str(tree_file))

                if make_branch_equal:
                    for node in t.traverse():
                        node.dist = 1

                # Ladderize (default increasing order, reverse=True for decreasing)
                t.ladderize()
                # Write to new file
                t.write(outfile=str(path_output))

                ret_paths.append(path_output)
                self.logger.info(f"Ladderize completed: {tree_file}")

            except Exception as e:
                self.logger.error(f"Ladderize failed {tree_file}: {e}")
                ret_paths.append(tree_file)  # return original tree file

        return ret_paths

    def run_pipeline(self):
        """Run the full analysis pipeline"""
        self.logger.info(
            f"Starting protein phylogenetic pipeline...This is: {Path.cwd()}"
        )
        """
        Statement: Note that fasta sequence IDs are best kept at 10 characters or fewer
        so PHYLIP's strict format can recognize them correctly.
        """

        # 1. Check software dependencies
        self.check_and_install_software()

        # 2. Validate input file
        sequences = self.validate_input()

        self.name_convention()

        # 3. Format conversion
        phylip_file = self.convert_to_phylip()

        # 4. Build trees using four methods
        tree_files = [None, None, None, None]

        entry_path = Path.cwd()

        if self.runtime_settings["distance"]["enabled"]:
            os.chdir(entry_path)
            tree_files[0] = self.distance_method(phylip_file)
            self.logger.info(
                "====Distance method complete==========================================================="
            )
        else:
            self.logger.info("====Distance method skipped by runtime config====")

        if self.runtime_settings["maximum_likelihood"]["enabled"]:
            os.chdir(entry_path)
            tree_files[1] = self.maximum_likelihood_method()
            self.logger.info(
                "====Maximum likelihood method complete==========================================================="
            )
        else:
            self.logger.info("====Maximum likelihood method skipped by runtime config====")

        if self.runtime_settings["bayesian"]["enabled"]:
            os.chdir(entry_path)
            tree_files[2] = self.bayesian_method()
            self.logger.info(
                "====Bayesian method complete==========================================================="
            )
        else:
            self.logger.info("====Bayesian method skipped by runtime config====")

        # Parsimony for proteins does not have branch length information
        if self.runtime_settings["parsimony"]["enabled"]:
            os.chdir(entry_path)
            tree_files[3] = self.parsimony_method(phylip_file)
            self.logger.info(
                "====Parsimony method complete==========================================================="
            )
        else:
            self.logger.info("====Parsimony method skipped by runtime config====")

        # 5. Rerooting and ladderizing
        # Parsimony trees typically lack branch lengths and may not need rerooting/ladderizing
        os.chdir(entry_path)
        rooted_tree_files = self.reroot_tree_by_MAD(tree_files[:3])

        os.chdir(entry_path)
        rooted_ladd_tree_files = self.ladderize_tree_by_ete4(rooted_tree_files)
        rooted_ladd_tree_files.extend(
            self.ladderize_tree_by_ete4([tree_files[3]], make_branch_equal=True)
        )

        result_trees = self.restore_names_in_trees(rooted_ladd_tree_files)
        # 6. Visualization and summary
        os.chdir(entry_path)

        methods = ["Distance", "Maximum Likelihood", "Bayesian", "Parsimony"]

        self.visualize_trees(result_trees, methods)

        os.chdir(entry_path)
        self.generate_summary(result_trees, sequences)

        self.logger.info("Protein phylogenetic pipeline completed!")
        self.logger.info(f"Results saved in: {self.output_dir}")


def main():
    """Main function"""
    parser = argparse.ArgumentParser(
        description="Automated protein phylogenetic pipeline"
    )
    parser.add_argument(
        "input_file", help="Input aligned FASTA file (protein sequences)"
    )
    parser.add_argument(
        "-o",
        "--output",
        default="phylo_results_protein",
        help="Output directory (default: phylo_results_protein)",
    )
    parser.add_argument(
        "--config",
        help="Optional runtime JSON config generated by the oneBuilder GUI",
    )

    args = parser.parse_args()
    runtime_config = load_runtime_config(args.config)

    # Create and run the pipeline
    pipeline = ProteinPhylogeneticPipeline(args.input_file, args.output, runtime_config=runtime_config)
    pipeline.run_pipeline()


if __name__ == "__main__":
    # Optionally clean previous results
    # shutil.rmtree("phylo_results_protein", ignore_errors=True)
    # # Optionally clean log files
    # for file in glob.glob("*.log"):
    #     os.remove(file)

    main()
