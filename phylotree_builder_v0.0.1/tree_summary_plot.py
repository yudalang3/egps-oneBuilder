#!/usr/bin/env python3
"""Render combined heatmaps for tree distance summaries."""

from __future__ import annotations

import csv
import math
from pathlib import Path
from typing import List, Sequence, Tuple

import matplotlib as mpl

mpl.set_loglevel("warning")
mpl.use("Agg", force=True)

import matplotlib.pyplot as plt
from matplotlib.colors import LinearSegmentedColormap, Normalize
from matplotlib.patches import Rectangle


def _read_distance_matrix(matrix_path: Path) -> Tuple[List[str], List[List[float]]]:
    with open(matrix_path, "r", encoding="utf-8", newline="") as handle:
        rows = [row for row in csv.reader(handle, delimiter="\t") if row]

    if len(rows) < 2:
        raise ValueError(f"Matrix file is empty or incomplete: {matrix_path}")

    header = rows[0]
    labels = header[1:] if header and header[0].strip() == "" else header
    matrix: List[List[float]] = []
    row_labels: List[str] = []

    for row in rows[1:]:
        if len(row) == len(labels) + 1:
            row_labels.append(row[0])
            values = row[1:]
        elif len(row) == len(labels):
            values = row
        else:
            raise ValueError(f"Unexpected row width in matrix file: {matrix_path}")

        matrix.append([float(value) for value in values])

    if row_labels and row_labels != labels:
        raise ValueError(f"Row and column labels do not match in {matrix_path}")

    if any(len(row) != len(labels) for row in matrix):
        raise ValueError(f"Matrix shape is inconsistent in {matrix_path}")

    return labels, matrix


def _format_value(value: float) -> str:
    if abs(value - round(value)) < 1e-9:
        return str(int(round(value)))
    return f"{value:.3f}".rstrip("0").rstrip(".")


def _build_cmap(hex_colors: Sequence[str]) -> LinearSegmentedColormap:
    return LinearSegmentedColormap.from_list("tree_summary", list(hex_colors))


def _draw_single_heatmap(
    ax, labels: Sequence[str], matrix: Sequence[Sequence[float]], title: str, cmap
):
    size = len(labels)
    display_matrix: List[List[float]] = []
    off_diag_values: List[float] = []

    for row_index, row in enumerate(matrix):
        display_row: List[float] = []
        for col_index, value in enumerate(row):
            if row_index == col_index:
                display_row.append(math.nan)
            else:
                display_row.append(value)
                off_diag_values.append(value)
        display_matrix.append(display_row)

    if off_diag_values:
        vmin = min(off_diag_values)
        vmax = max(off_diag_values)
        if abs(vmax - vmin) < 1e-12:
            vmax = vmin + 1.0
    else:
        vmin, vmax = 0.0, 1.0

    cmap = cmap.copy()
    cmap.set_bad("#f3f4f6")

    image = ax.imshow(
        display_matrix,
        cmap=cmap,
        norm=Normalize(vmin=vmin, vmax=vmax),
        interpolation="nearest",
        aspect="equal",
    )

    ax.set_title(title, fontsize=15, fontweight="semibold", pad=12)
    ax.set_xticks(
        range(size), labels=labels, rotation=35, ha="right", rotation_mode="anchor"
    )
    ax.set_yticks(range(size), labels=labels)
    ax.tick_params(axis="both", which="major", labelsize=10)

    ax.set_xticks([index - 0.5 for index in range(1, size)], minor=True)
    ax.set_yticks([index - 0.5 for index in range(1, size)], minor=True)
    ax.grid(which="minor", color="#ffffff", linestyle="-", linewidth=1.4)
    ax.tick_params(which="minor", bottom=False, left=False)

    for spine in ax.spines.values():
        spine.set_visible(False)

    for row_index, row in enumerate(matrix):
        for col_index, value in enumerate(row):
            if row_index == col_index:
                ax.add_patch(
                    Rectangle(
                        (col_index - 0.5, row_index - 0.5),
                        1,
                        1,
                        facecolor="#f3f4f6",
                        edgecolor="#ffffff",
                        linewidth=1.4,
                    )
                )
                text_color = "#6b7280"
            else:
                normalized = (value - vmin) / (vmax - vmin) if vmax > vmin else 0.0
                text_color = "white" if normalized > 0.62 else "#111827"

            ax.text(
                col_index,
                row_index,
                _format_value(value),
                ha="center",
                va="center",
                fontsize=10,
                color=text_color,
                fontweight="semibold" if row_index != col_index else "normal",
            )

    return image


def create_tree_distance_heatmaps(
    tree_distance_matrix: Path | str,
    rf_distance_matrix: Path | str,
    output_png: Path | str,
    output_pdf: Path | str,
    figure_title: str = "Tree Distance Comparison Across Inference Methods",
    tree_title: str = "TreeDist Matrix",
    rf_title: str = "Robinson-Foulds Matrix",
    tree_colorbar_label: str = "TreeDist",
    rf_colorbar_label: str = "RF Distance",
) -> Tuple[Path, Path]:
    tree_distance_matrix = Path(tree_distance_matrix)
    rf_distance_matrix = Path(rf_distance_matrix)
    output_png = Path(output_png)
    output_pdf = Path(output_pdf)

    labels, tree_distance = _read_distance_matrix(tree_distance_matrix)
    rf_labels, rf_distance = _read_distance_matrix(rf_distance_matrix)

    if labels != rf_labels:
        raise ValueError("TreeDist and RF matrices must use the same method order")

    plt.rcParams["font.sans-serif"] = ["DejaVu Sans", "Arial"]
    plt.rcParams["axes.unicode_minus"] = False

    fig, axes = plt.subplots(1, 2, figsize=(15, 7.8), constrained_layout=True)
    fig.patch.set_facecolor("#fcfcfd")
    fig.suptitle(
        figure_title,
        fontsize=18,
        fontweight="bold",
        y=1.02,
    )

    tree_cmap = _build_cmap(["#e0f2fe", "#38bdf8", "#0f766e"])
    rf_cmap = _build_cmap(["#ede9fe", "#8b5cf6", "#4c1d95"])

    tree_image = _draw_single_heatmap(
        axes[0], labels, tree_distance, tree_title, tree_cmap
    )
    rf_image = _draw_single_heatmap(
        axes[1], labels, rf_distance, rf_title, rf_cmap
    )

    tree_colorbar = fig.colorbar(tree_image, ax=axes[0], shrink=0.82, pad=0.02)
    tree_colorbar.set_label(tree_colorbar_label, fontsize=10)
    rf_colorbar = fig.colorbar(rf_image, ax=axes[1], shrink=0.82, pad=0.02)
    rf_colorbar.set_label(rf_colorbar_label, fontsize=10)

    output_png.parent.mkdir(parents=True, exist_ok=True)
    output_pdf.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(output_png, dpi=300, bbox_inches="tight", facecolor=fig.get_facecolor())
    fig.savefig(output_pdf, bbox_inches="tight", facecolor=fig.get_facecolor())
    plt.close(fig)

    return output_png, output_pdf


def main(summary_dir: Path | str) -> Tuple[Path, Path]:
    summary_dir = Path(summary_dir)
    return create_tree_distance_heatmaps(
        summary_dir / "tree_distance_matrix.tsv",
        summary_dir / "rf_distance_matrix.tsv",
        summary_dir / "tree_distance_heatmaps.png",
        summary_dir / "tree_distance_heatmaps.pdf",
    )


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(
        description="Create combined heatmaps for tree distance summary matrices."
    )
    parser.add_argument(
        "summary_dir",
        nargs="?",
        default=".",
        help="Directory containing tree_distance_matrix.tsv and rf_distance_matrix.tsv",
    )
    arguments = parser.parse_args()
    png_path, pdf_path = main(arguments.summary_dir)
    print(f"Saved heatmaps: {png_path} and {pdf_path}")
