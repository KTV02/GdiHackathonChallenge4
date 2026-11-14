#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$PWD"
PYTHON_BIN="${PYTHON_BIN:-$ROOT/.venv/bin/python}"
test_workspace="$(mktemp -d)"
trap 'rm -rf "$test_workspace"' EXIT
export AUTHORITY_MATCHER_HOME="$test_workspace/matcher"
export PYTHONDONTWRITEBYTECODE=1
for component in behoerden-matcher behoerden-resolver behoerden-finder; do
  echo "Tests: $component"
  (cd "$component" && "$PYTHON_BIN" -m pytest -q -p no:cacheprovider tests)
done
