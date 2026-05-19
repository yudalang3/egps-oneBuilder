# Tree Alignment Indices CLI

This directory keeps small command-line examples for the 3D tree alignment quantitative indices:

- TDI: Topology Difference Index
- BDI: Branch-Length Difference Index

Run from `phylotree_builder_v0.0.1/`:

```bash
java -cp "java_tanglegram:lib/*" tree.alignment.indices.CLI ../dev_test/treeAlignmentIndices/inputFile.txt
```

Input format:

- One Newick tree per non-empty line.
- Lines starting with `#` are ignored.
- The first tree is used as the reference tree, matching the 3D tree alignment view.

The expected output for `inputFile.txt` is stored in `expected_output.tsv`.
