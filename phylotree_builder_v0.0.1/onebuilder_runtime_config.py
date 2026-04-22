#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import copy
import json
from pathlib import Path


PROTEIN_DEFAULTS = {
    "distance": {
        "enabled": True,
        "neighbor_method": "NJ",
        "neighbor_outgroup_index": None,
        "protdist_menu_overrides": [],
        "neighbor_menu_overrides": [],
    },
    "maximum_likelihood": {
        "enabled": True,
        "bootstrap_replicates": 1000,
        "model_strategy": "MFP",
        "model_set": "WAG,LG,JTT,Dayhoff,DCMut,rtREV,cpREV,VT,Blosum62,mtMam,mtArt,HIVb,HIVw",
        "threads": None,
        "threads_max": None,
        "seed": None,
        "safe": False,
        "keep_ident": False,
        "quiet": True,
        "verbose": False,
        "redo": True,
        "memory_limit": None,
        "outgroup": None,
        "sequence_type": None,
        "alrt": None,
        "abayes": False,
        "extra_args": [],
    },
    "bayesian": {
        "enabled": True,
        "protein_model_prior": "mixed",
        "rates": "invgamma",
        "ngen": 50000,
        "samplefreq": 100,
        "printfreq": 1000,
        "diagnfreq": 5000,
        "nruns": None,
        "nchains": None,
        "temp": None,
        "stoprule": None,
        "stopval": None,
        "burnin": None,
        "burninfrac": None,
        "relburnin": None,
        "command_block": [],
    },
    "parsimony": {
        "enabled": True,
        "protpars_outgroup_index": None,
        "protpars_menu_overrides": [],
    },
}


DNA_DEFAULTS = {
    "distance": {
        "enabled": True,
        "dnadist_model": "F84",
        "dnadist_transition_transversion_ratio": 2.0,
        "dnadist_empirical_base_frequencies": True,
        "neighbor_method": "NJ",
        "neighbor_outgroup_index": None,
        "dnadist_menu_overrides": [],
        "neighbor_menu_overrides": [],
    },
    "maximum_likelihood": {
        "enabled": True,
        "bootstrap_replicates": 1000,
        "model_strategy": "MFP",
        "model_set": "",
        "threads": None,
        "threads_max": None,
        "seed": None,
        "safe": False,
        "keep_ident": False,
        "quiet": True,
        "verbose": False,
        "redo": True,
        "memory_limit": None,
        "outgroup": None,
        "sequence_type": None,
        "alrt": None,
        "abayes": False,
        "extra_args": [],
    },
    "bayesian": {
        "enabled": True,
        "rates": "invgamma",
        "ngen": 10000,
        "samplefreq": 100,
        "printfreq": 100,
        "diagnfreq": 1000,
        "nst": 6,
        "nruns": None,
        "nchains": None,
        "temp": None,
        "stoprule": None,
        "stopval": None,
        "burnin": None,
        "burninfrac": None,
        "relburnin": None,
        "command_block": [],
    },
    "parsimony": {
        "enabled": True,
        "dnapars_outgroup_index": None,
        "dnapars_transversion_parsimony": False,
        "dnapars_menu_overrides": [],
    },
}


def load_runtime_config(config_path):
    if config_path is None:
        return {}

    path = Path(config_path)
    if not path.exists():
        raise FileNotFoundError(f"Runtime config does not exist: {path}")

    content = path.read_text(encoding="utf-8").strip()
    if not content:
        return {}

    payload = json.loads(content)
    if not isinstance(payload, dict):
        raise ValueError("Runtime config must be a JSON object.")
    return payload


def protein_runtime_settings(runtime_config):
    return _merged_settings(PROTEIN_DEFAULTS, runtime_config)


def dna_runtime_settings(runtime_config):
    return _merged_settings(DNA_DEFAULTS, runtime_config)


def _merged_settings(defaults, runtime_config):
    merged = copy.deepcopy(defaults)
    methods = (runtime_config or {}).get("methods", {})
    if not isinstance(methods, dict):
        return merged

    for method_name, overrides in methods.items():
        if method_name not in merged or not isinstance(overrides, dict):
            continue
        if method_name == "maximum_likelihood":
            _merge_maximum_likelihood(merged[method_name], overrides)
        elif method_name == "bayesian":
            _merge_bayesian(merged[method_name], overrides)
        elif method_name == "distance":
            _merge_distance(merged[method_name], overrides)
        elif method_name == "parsimony":
            _merge_parsimony(merged[method_name], overrides)
        else:
            _merge_scalar_overrides(merged[method_name], overrides)

    return merged


def _merge_maximum_likelihood(target, overrides):
    _merge_scalar_overrides(target, overrides, skip_keys={"iqtree"})
    iqtree = overrides.get("iqtree")
    if not isinstance(iqtree, dict):
        target["model_strategy"] = _normalize_tree_build_model_strategy(
            target.get("model_strategy")
        )
        return

    _merge_scalar_overrides(target, iqtree, skip_keys={"common", "advanced", "extra_args"})
    _merge_section_into_target(target, iqtree.get("common"))
    _merge_section_into_target(target, iqtree.get("advanced"))
    if "extra_args" in iqtree:
        target["extra_args"] = _normalize_string_list(iqtree.get("extra_args"))
    target["model_strategy"] = _normalize_tree_build_model_strategy(
        target.get("model_strategy")
    )


def _merge_bayesian(target, overrides):
    _merge_scalar_overrides(target, overrides, skip_keys={"mrbayes"})
    mrbayes = overrides.get("mrbayes")
    if not isinstance(mrbayes, dict):
        return

    _merge_scalar_overrides(target, mrbayes, skip_keys={"common", "advanced", "command_block"})
    _merge_section_into_target(target, mrbayes.get("common"))
    _merge_section_into_target(target, mrbayes.get("advanced"))
    if "command_block" in mrbayes:
        target["command_block"] = _normalize_string_list(mrbayes.get("command_block"))


def _merge_distance(target, overrides):
    _merge_scalar_overrides(target, overrides, skip_keys={"protdist", "dnadist", "neighbor"})
    _merge_phylip_program(target, overrides, "protdist", "protdist_menu_overrides")
    _merge_phylip_program(target, overrides, "dnadist", "dnadist_menu_overrides")
    _merge_phylip_program(target, overrides, "neighbor", "neighbor_menu_overrides")


def _merge_parsimony(target, overrides):
    _merge_scalar_overrides(target, overrides, skip_keys={"protpars", "dnapars"})
    _merge_phylip_program(target, overrides, "protpars", "protpars_menu_overrides")
    _merge_phylip_program(target, overrides, "dnapars", "dnapars_menu_overrides")


def _merge_phylip_program(target, overrides, program_key, target_key):
    program = overrides.get(program_key)
    if not isinstance(program, dict):
        return

    _merge_section_into_target(target, program.get("common"))
    _merge_section_into_target(target, program.get("advanced"))
    if "menu_overrides" in program:
        target[target_key] = _normalize_string_list(program.get("menu_overrides"))


def _merge_section_into_target(target, section):
    if not isinstance(section, dict):
        return
    for key, value in section.items():
        if value is not None:
            target[key] = value


def _merge_scalar_overrides(target, overrides, skip_keys=None):
    skipped = set(skip_keys or ())
    for key, value in overrides.items():
        if key in skipped or isinstance(value, dict):
            continue
        if key.endswith("_args") or key.endswith("_block") or key.endswith("_overrides"):
            target[key] = _normalize_string_list(value)
        elif value is not None:
            target[key] = value


def _normalize_string_list(value):
    if value is None:
        return []
    if isinstance(value, str):
        return [value]
    if isinstance(value, (list, tuple)):
        return [str(item) for item in value if item is not None]
    return [str(value)]


def _normalize_tree_build_model_strategy(value):
    strategy = str(value).strip() if value is not None else ""
    if not strategy:
        return "MFP"
    if strategy in {"MF", "TESTNEWONLY", "TESTNEW"}:
        return "MFP"
    if strategy == "TESTONLY":
        return "TEST"
    return strategy
