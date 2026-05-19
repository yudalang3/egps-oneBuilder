#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR/phylotree_builder_v0.0.1"

javac -cp "lib/*:java_tanglegram" -d java_tanglegram java_tanglegram/onebuilder/*.java java_tanglegram/tanglegram/*.java java_tanglegram/tree/alignment/indices/*.java java_tanglegram/tests/OneBuilderStandaloneTest.java java_tanglegram/tests/TanglegramStandaloneTest.java

echo "Java compilation completed."
