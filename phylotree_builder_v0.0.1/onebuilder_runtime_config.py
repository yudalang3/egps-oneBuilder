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
        "protpars_print_steps": True,
        "protpars_print_sequences": True,
        "protpars_menu_overrides": [],
    },
    "protein_structure": {
        "enabled": False,
        "backend": "foldseek",
        "use_structure_manifest": False,
        "structure_manifest_file": None,
        "sequence_only_mode": "prostt5",
        "similarity_rule": "mean_qtmscore_ttmscore",
        "missing_distance": "1",
        "tree_builder_method": "NJ",
        "threads": 0,
        "sensitivity": 9.5,
        "evalue": 10.0,
        "max_seqs": 1000,
        "coverage_threshold": 0.0,
        "coverage_mode": 0,
        "alignment_type": 2,
        "tmscore_threshold": 0.0,
        "exhaustive_search": False,
        "exact_tmscore": False,
        "gpu": False,
        "verbosity": 3,
        "extra_args": [],
    },
    "reroot": {
        "method": "MAD",
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
    "protein_structure": {
        "enabled": False,
        "backend": "foldseek",
        "use_structure_manifest": False,
        "structure_manifest_file": None,
        "sequence_only_mode": "prostt5",
        "similarity_rule": "mean_qtmscore_ttmscore",
        "missing_distance": "1",
        "tree_builder_method": "NJ",
        "threads": 0,
        "sensitivity": 9.5,
        "evalue": 10.0,
        "max_seqs": 1000,
        "coverage_threshold": 0.0,
        "coverage_mode": 0,
        "alignment_type": 2,
        "tmscore_threshold": 0.0,
        "exhaustive_search": False,
        "exact_tmscore": False,
        "gpu": False,
        "verbosity": 3,
        "extra_args": [],
    },
    "reroot": {
        "method": "MAD",
    },
}


PROTEIN_FIXED_MODELS = {"LG", "WAG", "JTT"}
DNA_FIXED_MODELS = {"GTR", "HKY", "JC"}


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
    _resolve_runtime_config_paths(payload, path.parent)
    return payload


def _resolve_runtime_config_paths(payload, config_dir):
    methods = payload.get("methods")
    if not isinstance(methods, dict):
        return
    protein_structure = methods.get("protein_structure")
    if not isinstance(protein_structure, dict):
        return
    manifest_file = protein_structure.get("structure_manifest_file")
    if not manifest_file:
        return
    manifest_path = Path(str(manifest_file).strip()).expanduser()
    if not manifest_path.is_absolute():
        manifest_path = (config_dir / manifest_path).resolve()
    protein_structure["structure_manifest_file"] = str(manifest_path)


def protein_runtime_settings(runtime_config):
    return _merged_settings(PROTEIN_DEFAULTS, runtime_config, "PROTEIN")


def dna_runtime_settings(runtime_config):
    return _merged_settings(DNA_DEFAULTS, runtime_config, "DNA_CDS")


def _merged_settings(defaults, runtime_config, input_type):
    merged = copy.deepcopy(defaults)
    methods = (runtime_config or {}).get("methods", {})
    reroot = (runtime_config or {}).get("reroot", {})
    if isinstance(reroot, dict):
        _merge_reroot(merged["reroot"], reroot)

    if isinstance(methods, dict):
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
            elif method_name == "protein_structure":
                _merge_protein_structure(merged[method_name], overrides, input_type)
            else:
                _merge_scalar_overrides(merged[method_name], overrides)

    _normalize_input_type_specific_settings(merged, input_type)

    return merged


def _merge_reroot(target, overrides):
    method = str(overrides.get("method") or target.get("method") or "MAD").strip()
    if method not in {"MAD", "root-at-middle-point"}:
        method = "MAD"
    target["method"] = method


def _normalize_input_type_specific_settings(merged, input_type):
    maximum_likelihood = merged.get("maximum_likelihood")
    if not isinstance(maximum_likelihood, dict):
        return

    model_strategy = _normalize_tree_build_model_strategy(
        maximum_likelihood.get("model_strategy")
    )
    maximum_likelihood["model_strategy"] = model_strategy
    raw_model_set = str(maximum_likelihood.get("model_set") or "").strip()

    if input_type == "DNA_CDS":
        if model_strategy in DNA_FIXED_MODELS:
            maximum_likelihood["model_set"] = ""
            return
        maximum_likelihood["model_set"] = _filter_model_set(raw_model_set, DNA_FIXED_MODELS)
        return

    if model_strategy in PROTEIN_FIXED_MODELS:
        maximum_likelihood["model_set"] = ""
        return
    maximum_likelihood["model_set"] = raw_model_set


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


def _merge_protein_structure(target, overrides, input_type):
    _merge_scalar_overrides(target, overrides, skip_keys={"foldseek"})
    foldseek = overrides.get("foldseek")
    if isinstance(foldseek, dict):
        _merge_section_into_target(target, foldseek.get("common"))
        _merge_section_into_target(target, foldseek.get("advanced"))
        if "extra_args" in foldseek:
            target["extra_args"] = _normalize_string_list(foldseek.get("extra_args"))
    target["backend"] = "foldseek"
    if input_type != "PROTEIN":
        target["enabled"] = False
    target["use_structure_manifest"] = bool(target.get("use_structure_manifest", False))
    manifest_file = target.get("structure_manifest_file")
    target["structure_manifest_file"] = str(manifest_file).strip() if manifest_file else None
    sequence_only_mode = str(target.get("sequence_only_mode") or "prostt5").strip().lower()
    target["sequence_only_mode"] = sequence_only_mode or "prostt5"
    similarity_rule = str(target.get("similarity_rule") or "mean_qtmscore_ttmscore").strip()
    target["similarity_rule"] = similarity_rule or "mean_qtmscore_ttmscore"
    missing_distance = target.get("missing_distance")
    target["missing_distance"] = str(missing_distance).strip() if missing_distance is not None else "1"
    target["tree_builder_method"] = _normalize_protein_structure_tree_builder_method(
        target.get("tree_builder_method")
    )
    target["threads"] = _normalize_int(target.get("threads"), 0, 0, 512)
    target["sensitivity"] = _normalize_float(target.get("sensitivity"), 9.5, 1.0, 20.0)
    target["evalue"] = _normalize_float(target.get("evalue"), 10.0, 0.0, None)
    target["max_seqs"] = _normalize_int(target.get("max_seqs"), 1000, 1, None)
    target["coverage_threshold"] = _normalize_float(target.get("coverage_threshold"), 0.0, 0.0, 1.0)
    target["coverage_mode"] = _normalize_int(target.get("coverage_mode"), 0, 0, 5)
    target["alignment_type"] = _normalize_int(target.get("alignment_type"), 2, 0, 2)
    target["tmscore_threshold"] = _normalize_float(target.get("tmscore_threshold"), 0.0, 0.0, 1.0)
    target["exhaustive_search"] = _normalize_bool(target.get("exhaustive_search"), False)
    target["exact_tmscore"] = _normalize_bool(target.get("exact_tmscore"), False)
    target["gpu"] = _normalize_bool(target.get("gpu"), False)
    target["verbosity"] = _normalize_int(target.get("verbosity"), 3, 0, 3)
    target["extra_args"] = _normalize_string_list(target.get("extra_args"))


def _normalize_protein_structure_tree_builder_method(value):
    method = str(value or "NJ").strip()
    if method.lower() in {"swiftnj", "swift nj"}:
        return "SwiftNJ"
    return "NJ"


def _normalize_int(value, default, minimum=None, maximum=None):
    try:
        normalized = int(value)
    except (TypeError, ValueError):
        normalized = default
    if minimum is not None:
        normalized = max(minimum, normalized)
    if maximum is not None:
        normalized = min(maximum, normalized)
    return normalized


def _normalize_float(value, default, minimum=None, maximum=None):
    try:
        normalized = float(value)
    except (TypeError, ValueError):
        normalized = default
    if minimum is not None:
        normalized = max(minimum, normalized)
    if maximum is not None:
        normalized = min(maximum, normalized)
    return normalized


def _normalize_bool(value, default=False):
    if isinstance(value, bool):
        return value
    if value is None:
        return default
    text = str(value).strip().lower()
    if text in {"1", "true", "yes", "y", "on"}:
        return True
    if text in {"0", "false", "no", "n", "off"}:
        return False
    return default


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


def _filter_model_set(raw_model_set, allowed_models):
    if not raw_model_set:
        return ""
    tokens = []
    seen = set()
    for item in raw_model_set.split(","):
        normalized = str(item).strip().upper()
        if not normalized or normalized not in allowed_models or normalized in seen:
            continue
        seen.add(normalized)
        tokens.append(normalized)
    return ",".join(tokens)
