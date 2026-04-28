#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from __future__ import annotations

import argparse
import csv
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path


STRUCTURE_FORMAT_OUTPUT = "query,target,fident,alnlen,evalue,bits,alntmscore,qtmscore,ttmscore,lddt,prob"
SEQUENCE_FORMAT_OUTPUT = "query,target,fident,alnlen,evalue,bits,prob"
SIMILARITY_RULES = {
    "mean_qtmscore_ttmscore",
    "alntmscore",
    "prob",
    "fident",
}
ALL_OUTPUT_COLUMNS = [
    "query",
    "target",
    "pair_type",
    "backend",
    "score_type",
    "similarity",
    "distance",
    "fident",
    "alnlen",
    "evalue",
    "bits",
    "alntmscore",
    "qtmscore",
    "ttmscore",
    "lddt",
    "prob",
]


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Compute protein pairwise structure similarity with Foldseek."
    )
    parser.add_argument("--input-fasta", required=True, help="Input protein FASTA/MSA file.")
    parser.add_argument("--output-dir", required=True, help="Output directory.")
    parser.add_argument(
        "--structure-manifest",
        help="Optional TSV mapping FASTA sequence IDs to PDB/mmCIF files.",
    )
    parser.add_argument(
        "--prostt5-model",
        help="Local ProstT5 model weights path required for FASTA-only Foldseek mode. oneBuilder never downloads this automatically.",
    )
    parser.add_argument("--threads", type=int, default=0, help="Foldseek threads; 0 lets Foldseek decide.")
    parser.add_argument("--sensitivity", type=float, default=9.5, help="Foldseek sensitivity (-s).")
    parser.add_argument("--evalue", type=float, default=10.0, help="Foldseek E-value threshold (-e).")
    parser.add_argument("--max-seqs", type=int, default=1000, help="Foldseek --max-seqs value.")
    parser.add_argument("--coverage-threshold", type=float, default=0.0, help="Foldseek coverage threshold (-c).")
    parser.add_argument("--coverage-mode", type=int, default=0, help="Foldseek --cov-mode value.")
    parser.add_argument("--alignment-type", type=int, default=2, help="Foldseek --alignment-type value.")
    parser.add_argument("--tmscore-threshold", type=float, default=0.0, help="Foldseek --tmscore-threshold value.")
    parser.add_argument("--exhaustive-search", action="store_true", help="Pass --exhaustive-search 1 to Foldseek.")
    parser.add_argument("--exact-tmscore", action="store_true", help="Pass --exact-tmscore 1 to Foldseek.")
    parser.add_argument("--gpu", action="store_true", help="Pass --gpu 1 to Foldseek.")
    parser.add_argument("--verbosity", type=int, default=3, help="Foldseek verbosity (-v).")
    parser.add_argument(
        "--foldseek-extra-arg",
        action="append",
        default=[],
        help="Additional Foldseek argument token. Repeat for multiple tokens.",
    )
    parser.add_argument(
        "--similarity-rule",
        default="mean_qtmscore_ttmscore",
        help="Similarity score rule: mean_qtmscore_ttmscore, alntmscore, prob, or fident.",
    )
    parser.add_argument(
        "--missing-distance",
        default="1",
        help="Distance value used when Foldseek reports no hit for a pair. Use an empty string to keep blanks.",
    )
    parser.add_argument(
        "--foldseek",
        default=os.environ.get("FOLDSEEK_EXE", "foldseek"),
        help="Foldseek executable path or command name.",
    )
    args = parser.parse_args()

    input_fasta = Path(args.input_fasta).expanduser().resolve()
    output_dir = Path(args.output_dir).expanduser().resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    sequence_ids = parse_fasta_ids(input_fasta)
    if not sequence_ids:
        raise SystemExit(f"No FASTA records found in {input_fasta}")
    similarity_rule = normalize_similarity_rule(args.similarity_rule)
    prostt5_model_path = None
    if not args.structure_manifest:
        prostt5_model_path = resolve_required_prostt5_model(args.prostt5_model)

    foldseek_exe = resolve_foldseek(args.foldseek)
    tmp_dir = output_dir / "foldseek_tmp"
    tmp_dir.mkdir(parents=True, exist_ok=True)
    foldseek_extra_args = build_foldseek_extra_args(args)

    if args.structure_manifest:
        mode = "structure"
        normalized_manifest, id_lookup = prepare_structure_inputs(
            sequence_ids,
            Path(args.structure_manifest).expanduser().resolve(),
            output_dir / "structure_inputs",
            output_dir / "normalized_structure_manifest.tsv",
        )
        raw_output = output_dir / "foldseek_raw.tsv"
        run_foldseek_easy_search(
            foldseek_exe,
            output_dir / "structure_inputs",
            output_dir / "structure_inputs",
            raw_output,
            tmp_dir,
            STRUCTURE_FORMAT_OUTPUT,
            args.threads,
            foldseek_extra_args,
        )
        rows = normalize_foldseek_rows(
            raw_output,
            STRUCTURE_FORMAT_OUTPUT.split(","),
            id_lookup,
            "structure_vs_structure",
            similarity_rule,
        )
        run_config_extra = {"structure_manifest": str(normalized_manifest)}
    else:
        mode = "sequence_prostt5"
        raw_output = output_dir / "foldseek_raw.tsv"
        run_foldseek_sequence_search(
            foldseek_exe,
            input_fasta,
            raw_output,
            tmp_dir,
            args.threads,
            prostt5_model_path,
            foldseek_extra_args,
        )
        rows = normalize_foldseek_rows(
            raw_output,
            SEQUENCE_FORMAT_OUTPUT.split(","),
            {sequence_id: sequence_id for sequence_id in sequence_ids},
            "sequence_vs_sequence",
            similarity_rule,
        )
        run_config_extra = {"prostt5_model_path": str(prostt5_model_path)}

    write_pairwise_scores(output_dir / "pairwise_scores.tsv", rows)
    write_matrices(output_dir, sequence_ids, rows, args.missing_distance)
    write_run_config(
        output_dir / "run_config.json",
        input_fasta,
        mode,
        foldseek_exe,
        args.threads,
        similarity_rule,
        args.missing_distance,
        foldseek_extra_args,
        run_config_extra,
    )
    print(f"Protein structure similarity completed: {output_dir}")
    return 0


def parse_fasta_ids(input_fasta: Path) -> list[str]:
    if not input_fasta.is_file():
        raise SystemExit(f"Input FASTA does not exist: {input_fasta}")
    sequence_ids: list[str] = []
    with input_fasta.open(encoding="utf-8") as handle:
        for line in handle:
            if line.startswith(">"):
                sequence_id = line[1:].strip().split()[0]
                if sequence_id:
                    sequence_ids.append(sequence_id)
    duplicates = sorted({sequence_id for sequence_id in sequence_ids if sequence_ids.count(sequence_id) > 1})
    if duplicates:
        raise SystemExit("Duplicate FASTA IDs are not supported: " + ", ".join(duplicates))
    return sequence_ids


def resolve_foldseek(foldseek_command: str) -> str:
    candidate = Path(foldseek_command).expanduser()
    if candidate.is_file() and os.access(candidate, os.X_OK):
        return str(candidate.resolve())
    resolved = shutil.which(foldseek_command)
    if resolved:
        return resolved
    raise SystemExit(
        "Foldseek executable was not found. Install Foldseek or set FOLDSEEK_EXE."
    )


def resolve_required_prostt5_model(prostt5_model_arg: str | None) -> Path:
    if not prostt5_model_arg or not str(prostt5_model_arg).strip():
        raise SystemExit(
            "FASTA-only Foldseek mode requires --prostt5-model pointing to local ProstT5 weights. "
            "oneBuilder does not download ProstT5 automatically; download it yourself or provide --structure-manifest."
        )
    prostt5_model_path = Path(str(prostt5_model_arg).strip()).expanduser().resolve()
    if not prostt5_model_path.exists():
        raise SystemExit(f"ProstT5 model weights path does not exist: {prostt5_model_path}")
    return prostt5_model_path


def normalize_similarity_rule(value: str | None) -> str:
    rule = str(value or "mean_qtmscore_ttmscore").strip().lower()
    if not rule:
        rule = "mean_qtmscore_ttmscore"
    if rule not in SIMILARITY_RULES:
        raise ValueError(
            "Unsupported similarity rule: "
            + rule
            + ". Expected one of: "
            + ", ".join(sorted(SIMILARITY_RULES))
        )
    return rule


def build_foldseek_extra_args(args) -> list[str]:
    extra_args = [
        "-s",
        format_cli_float(max(1.0, args.sensitivity)),
        "-e",
        format_cli_float(max(0.0, args.evalue)),
        "--max-seqs",
        str(max(1, args.max_seqs)),
        "-c",
        format_cli_float(clamp(args.coverage_threshold, 0.0, 1.0)),
        "--cov-mode",
        str(int(clamp(args.coverage_mode, 0, 5))),
        "--alignment-type",
        str(int(clamp(args.alignment_type, 0, 2))),
        "--tmscore-threshold",
        format_cli_float(clamp(args.tmscore_threshold, 0.0, 1.0)),
        "-v",
        str(int(clamp(args.verbosity, 0, 3))),
    ]
    if args.exhaustive_search:
        extra_args.extend(["--exhaustive-search", "1"])
    if args.exact_tmscore:
        extra_args.extend(["--exact-tmscore", "1"])
    if args.gpu:
        extra_args.extend(["--gpu", "1"])
    extra_args.extend(str(item) for item in args.foldseek_extra_arg if str(item).strip())
    return extra_args


def clamp(value, minimum, maximum):
    return max(minimum, min(maximum, value))


def format_cli_float(value: float) -> str:
    return f"{value:.8g}"


def prepare_structure_inputs(
    sequence_ids: list[str],
    manifest_path: Path,
    structure_input_dir: Path,
    normalized_manifest_path: Path,
) -> tuple[Path, dict[str, str]]:
    if not manifest_path.is_file():
        raise SystemExit(f"Protein structure TSV does not exist: {manifest_path}")

    sequence_id_set = set(sequence_ids)
    mappings: dict[str, Path] = {}
    with manifest_path.open(encoding="utf-8", newline="") as handle:
        for line_number, raw_line in enumerate(handle, start=1):
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            columns = line.split("\t")
            if len(columns) < 2:
                raise SystemExit(
                    f"Invalid TSV row {line_number}: expected sequence_id<TAB>structure_file"
                )
            sequence_id = columns[0].strip()
            structure_text = columns[1].strip()
            if not sequence_id or not structure_text:
                raise SystemExit(f"Invalid TSV row {line_number}: sequence ID and structure path are required")
            if sequence_id in mappings:
                raise SystemExit(f"Duplicate sequence ID in structure TSV: {sequence_id}")
            if sequence_id not in sequence_id_set:
                raise SystemExit(f"Structure TSV ID is not present in FASTA: {sequence_id}")
            structure_path = Path(structure_text).expanduser()
            if not structure_path.is_absolute():
                structure_path = (manifest_path.parent / structure_path).resolve()
            else:
                structure_path = structure_path.resolve()
            if not structure_path.is_file():
                raise SystemExit(f"Structure file does not exist for {sequence_id}: {structure_path}")
            if not is_supported_structure_path(structure_path):
                raise SystemExit(f"Unsupported structure file extension for {sequence_id}: {structure_path}")
            mappings[sequence_id] = structure_path

    missing_ids = [sequence_id for sequence_id in sequence_ids if sequence_id not in mappings]
    if missing_ids:
        raise SystemExit("Structure TSV is missing FASTA IDs: " + ", ".join(missing_ids))

    if structure_input_dir.exists():
        shutil.rmtree(structure_input_dir)
    structure_input_dir.mkdir(parents=True, exist_ok=True)

    id_lookup: dict[str, str] = {}
    with normalized_manifest_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(["sequence_id", "foldseek_id", "structure_file"])
        for index, sequence_id in enumerate(sequence_ids, start=1):
            structure_path = mappings[sequence_id]
            foldseek_id = f"seq{index}_{sanitize_identifier(sequence_id)}"
            target_path = structure_input_dir / f"{foldseek_id}{combined_suffix(structure_path)}"
            link_or_copy(structure_path, target_path)
            id_lookup[foldseek_id] = sequence_id
            writer.writerow([sequence_id, foldseek_id, str(structure_path)])
    return normalized_manifest_path, id_lookup


def is_supported_structure_path(path: Path) -> bool:
    name = path.name.lower()
    return name.endswith((".pdb", ".ent", ".cif", ".mmcif", ".pdb.gz", ".ent.gz", ".cif.gz", ".mmcif.gz"))


def combined_suffix(path: Path) -> str:
    name = path.name
    for suffix in (".pdb.gz", ".ent.gz", ".cif.gz", ".mmcif.gz", ".pdb", ".ent", ".cif", ".mmcif"):
        if name.lower().endswith(suffix):
            return suffix
    return path.suffix


def sanitize_identifier(value: str) -> str:
    sanitized = re.sub(r"[^A-Za-z0-9_.-]+", "_", value).strip("._-")
    return sanitized or "protein"


def link_or_copy(source: Path, target: Path) -> None:
    try:
        target.symlink_to(source)
    except OSError:
        shutil.copy2(source, target)


def run_foldseek_easy_search(
    foldseek_exe: str,
    query: Path,
    target: Path,
    output_file: Path,
    tmp_dir: Path,
    format_output: str,
    threads: int,
    extra_args: list[str] | None = None,
) -> None:
    command = [
        foldseek_exe,
        "easy-search",
        str(query),
        str(target),
        str(output_file),
        str(tmp_dir),
        "--format-output",
        format_output,
    ]
    if threads and threads > 0:
        command.extend(["--threads", str(threads)])
    if extra_args:
        command.extend(extra_args)
    run_command(command)


def run_foldseek_sequence_search(
    foldseek_exe: str,
    input_fasta: Path,
    output_file: Path,
    tmp_dir: Path,
    threads: int,
    prostt5_model_path: Path,
    extra_args: list[str] | None = None,
) -> None:
    sequence_db = output_file.parent / "sequence_db"
    result_db = output_file.parent / "sequence_search_result"
    run_command([
        foldseek_exe,
        "createdb",
        str(input_fasta),
        str(sequence_db),
        "--prostt5-model",
        str(prostt5_model_path),
    ])
    search_command = [foldseek_exe, "search", str(sequence_db), str(sequence_db), str(result_db), str(tmp_dir)]
    if threads and threads > 0:
        search_command.extend(["--threads", str(threads)])
    if extra_args:
        search_command.extend(extra_args)
    run_command(search_command)
    run_command([
        foldseek_exe,
        "convertalis",
        str(sequence_db),
        str(sequence_db),
        str(result_db),
        str(output_file),
        "--format-output",
        SEQUENCE_FORMAT_OUTPUT,
    ])


def run_command(command: list[str]) -> None:
    print("$ " + " ".join(command))
    subprocess.run(command, check=True)


def normalize_foldseek_rows(
    raw_output: Path,
    raw_columns: list[str],
    id_lookup: dict[str, str],
    pair_type: str,
    similarity_rule: str,
) -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    if not raw_output.is_file():
        raise SystemExit(f"Foldseek output was not created: {raw_output}")
    with raw_output.open(encoding="utf-8") as handle:
        for raw_line in handle:
            line = raw_line.rstrip("\n")
            if not line:
                continue
            values = line.split("\t")
            raw = {column: values[index] if index < len(values) else "" for index, column in enumerate(raw_columns)}
            query = map_foldseek_id(raw.get("query", ""), id_lookup)
            target = map_foldseek_id(raw.get("target", ""), id_lookup)
            similarity, score_type = choose_similarity(raw, pair_type, similarity_rule)
            distance = None if similarity is None else max(0.0, 1.0 - similarity)
            row = {column: "" for column in ALL_OUTPUT_COLUMNS}
            row.update(raw)
            row.update(
                {
                    "query": query,
                    "target": target,
                    "pair_type": pair_type,
                    "backend": "foldseek",
                    "score_type": score_type,
                    "similarity": format_float(similarity),
                    "distance": format_float(distance),
                }
            )
            rows.append(row)
    return rows


def map_foldseek_id(raw_id: str, id_lookup: dict[str, str]) -> str:
    if raw_id in id_lookup:
        return id_lookup[raw_id]
    for foldseek_id, sequence_id in id_lookup.items():
        if raw_id == foldseek_id or raw_id.startswith(foldseek_id + "_") or raw_id.startswith(foldseek_id + "."):
            return sequence_id
    return raw_id


def choose_similarity(raw: dict[str, str], pair_type: str, similarity_rule: str) -> tuple[float | None, str]:
    rule = normalize_similarity_rule(similarity_rule)
    if rule == "mean_qtmscore_ttmscore":
        qtmscore = parse_float(raw.get("qtmscore"))
        ttmscore = parse_float(raw.get("ttmscore"))
        if qtmscore is not None and ttmscore is not None:
            return (qtmscore + ttmscore) / 2.0, "foldseek_tmscore_mean"
        alntmscore = parse_float(raw.get("alntmscore"))
        if alntmscore is not None:
            return alntmscore, "foldseek_alntmscore"
        prob = parse_float(raw.get("prob"))
        if prob is not None:
            return prob, "foldseek_prostt5_prob" if pair_type == "sequence_vs_sequence" else "foldseek_prob"
    elif rule == "alntmscore":
        return parse_float(raw.get("alntmscore")), "foldseek_alntmscore"
    elif rule == "prob":
        return parse_float(raw.get("prob")), "foldseek_prostt5_prob" if pair_type == "sequence_vs_sequence" else "foldseek_prob"
    elif rule == "fident":
        return parse_float(raw.get("fident")), "foldseek_fident"
    return None, "unavailable"


def parse_float(value: str | None) -> float | None:
    if value is None or str(value).strip() == "":
        return None
    try:
        return float(value)
    except ValueError:
        return None


def format_float(value: float | None) -> str:
    if value is None:
        return ""
    return f"{value:.8g}"


def write_pairwise_scores(path: Path, rows: list[dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=ALL_OUTPUT_COLUMNS, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for row in rows:
            writer.writerow({column: row.get(column, "") for column in ALL_OUTPUT_COLUMNS})


def write_matrices(output_dir: Path, sequence_ids: list[str], rows: list[dict[str, str]], missing_distance: str = "1") -> None:
    similarity_by_pair = symmetric_pair_values(rows, "similarity")
    distance_by_pair = symmetric_pair_values(rows, "distance")
    missing_similarity = format_missing_similarity(missing_distance)
    write_matrix(output_dir / "similarity_matrix.tsv", sequence_ids, similarity_by_pair, "1", missing_similarity)
    write_matrix(output_dir / "distance_matrix.tsv", sequence_ids, distance_by_pair, "0", missing_distance)


def format_missing_similarity(missing_distance: str) -> str:
    if missing_distance is None or str(missing_distance) == "":
        return ""
    parsed = parse_float(str(missing_distance))
    if parsed is None:
        return ""
    return format_float(max(0.0, 1.0 - parsed))


def symmetric_pair_values(rows: list[dict[str, str]], column: str) -> dict[tuple[str, str], str]:
    raw_values: dict[tuple[str, str], float] = {}
    for row in rows:
        value = parse_float(row.get(column))
        if value is None:
            continue
        raw_values[(row["query"], row["target"])] = value

    symmetric_values: dict[tuple[str, str], str] = {}
    seen_pairs: set[tuple[str, str]] = set()
    for query, target in raw_values:
        pair_key = tuple(sorted((query, target)))
        if pair_key in seen_pairs:
            continue
        seen_pairs.add(pair_key)
        forward = raw_values.get((query, target))
        reverse = raw_values.get((target, query))
        if forward is not None and reverse is not None:
            value = (forward + reverse) / 2.0
        else:
            value = forward if forward is not None else reverse
        if value is None:
            continue
        first, second = pair_key
        formatted_value = format_float(value)
        symmetric_values[(first, second)] = formatted_value
        symmetric_values[(second, first)] = formatted_value
    return symmetric_values


def write_matrix(
    path: Path,
    sequence_ids: list[str],
    values: dict[tuple[str, str], str],
    diagonal: str,
    missing_value: str,
) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(["id", *sequence_ids])
        for query in sequence_ids:
            row = [query]
            for target in sequence_ids:
                if query == target:
                    row.append(diagonal)
                else:
                    row.append(values.get((query, target), missing_value))
            writer.writerow(row)


def write_run_config(
    path: Path,
    input_fasta: Path,
    mode: str,
    foldseek_exe: str,
    threads: int,
    similarity_rule: str,
    missing_distance: str,
    foldseek_args: list[str],
    extra: dict[str, str],
) -> None:
    payload = {
        "input_fasta": str(input_fasta),
        "mode": mode,
        "backend": "foldseek",
        "foldseek_exe": foldseek_exe,
        "threads": threads,
        "foldseek_args": foldseek_args,
        "similarity_rule": similarity_rule,
        "missing_distance": missing_distance,
        **extra,
    }
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    sys.exit(main())
