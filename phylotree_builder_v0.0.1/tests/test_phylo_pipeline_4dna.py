import sys
import tempfile
import types
import unittest
from pathlib import Path
from unittest.mock import MagicMock

bio_module = types.ModuleType("Bio")
bio_module.SeqIO = types.ModuleType("Bio.SeqIO")
bio_module.Phylo = types.ModuleType("Bio.Phylo")
sys.modules.setdefault("Bio", bio_module)
sys.modules.setdefault("Bio.SeqIO", bio_module.SeqIO)
sys.modules.setdefault("Bio.Phylo", bio_module.Phylo)

matplotlib_module = types.ModuleType("matplotlib")
matplotlib_module.set_loglevel = lambda level: None
matplotlib_module.use = lambda backend, force=False: None
pyplot_module = types.ModuleType("matplotlib.pyplot")
matplotlib_module.pyplot = pyplot_module
sys.modules.setdefault("matplotlib", matplotlib_module)
sys.modules.setdefault("matplotlib.pyplot", pyplot_module)

tree_summary_plot_module = types.ModuleType("tree_summary_plot")
tree_summary_plot_module.create_tree_distance_heatmaps = lambda *args, **kwargs: []
sys.modules.setdefault("tree_summary_plot", tree_summary_plot_module)

from phylo_pipeline_4dna import PhylogeneticPipeline


class DnaPipelineFailureStatusTest(unittest.TestCase):
    def test_reports_failure_when_an_enabled_method_has_no_final_tree(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_root = Path(temp_dir)
            successful_tree = temp_root / "successful_tree.nwk"
            successful_tree.write_text("(seqA,seqB,seqC);\n", encoding="utf-8")

            pipeline = object.__new__(PhylogeneticPipeline)
            pipeline.logger = MagicMock()
            pipeline.translator = MagicMock()
            pipeline.translator.text.side_effect = lambda english, chinese: english
            pipeline.output_dir = temp_root / "output"
            pipeline.runtime_settings = {
                method_name: {"enabled": True}
                for method_name in ["distance", "maximum_likelihood", "bayesian", "parsimony"]
            }
            pipeline.check_and_install_software = MagicMock()
            pipeline.validate_input = MagicMock(return_value=[])
            pipeline.name_convention = MagicMock()
            pipeline.convert_to_phylip = MagicMock(return_value=temp_root / "alignment.phy")
            pipeline.distance_method = MagicMock(return_value=successful_tree)
            pipeline.maximum_likelihood_method = MagicMock(return_value=successful_tree)
            pipeline.bayesian_method = MagicMock(return_value=successful_tree)
            pipeline.parsimony_method = MagicMock(return_value=successful_tree)
            pipeline.reroot_trees = MagicMock(return_value=[successful_tree] * 4)
            pipeline.ladderize_tree_with_egps = MagicMock(return_value=[successful_tree] * 4)
            pipeline.restore_names_in_trees = MagicMock(return_value=[successful_tree] * 4)
            pipeline.publish_final_newick_exports = MagicMock(
                return_value=[successful_tree, None, successful_tree, successful_tree]
            )
            pipeline.visualize_trees = MagicMock()
            pipeline.generate_summary = MagicMock()

            completed = pipeline.run_pipeline()

        self.assertFalse(completed)
        pipeline.logger.error.assert_called_with(
            "Phylogenetic pipeline finished with failed methods: maximum_likelihood"
        )


if __name__ == "__main__":
    unittest.main()
