#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import copy
import json
from pathlib import Path


PROTEIN_DEFAULTS = {
    "distance": {"enabled": True},
    "maximum_likelihood": {
        "enabled": True,
        "bootstrap_replicates": 1000,
        "model_strategy": "MFP",
        "model_set": "WAG,LG,JTT,Dayhoff,DCMut,rtREV,cpREV,VT,Blosum62,mtMam,mtArt,HIVb,HIVw",
    },
    "bayesian": {
        "enabled": True,
        "protein_model_prior": "mixed",
        "rates": "invgamma",
        "ngen": 50000,
        "samplefreq": 100,
        "printfreq": 1000,
        "diagnfreq": 5000,
    },
    "parsimony": {"enabled": True},
}


DNA_DEFAULTS = {
    "distance": {"enabled": True},
    "maximum_likelihood": {
        "enabled": True,
        "bootstrap_replicates": 1000,
        "model_strategy": "MFP",
        "model_set": "",
    },
    "bayesian": {
        "enabled": True,
        "rates": "invgamma",
        "ngen": 10000,
        "samplefreq": 100,
        "printfreq": 100,
        "diagnfreq": 1000,
        "nst": 6,
    },
    "parsimony": {"enabled": True},
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
        for key, value in overrides.items():
            if value is not None:
                merged[method_name][key] = value

    return merged
