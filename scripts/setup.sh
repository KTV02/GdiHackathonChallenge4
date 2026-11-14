#!/usr/bin/env bash
# Installiert alle drei lokalen Pakete in dieselbe virtuelle Umgebung.
set -euo pipefail
cd "$(dirname "$0")/.."
semantic=true
if [[ "${1:-}" == --no-semantic && $# == 1 ]]; then
  semantic=false
elif [[ $# != 0 ]]; then
  echo 'Aufruf: bash scripts/setup.sh [--no-semantic]' >&2
  exit 1
fi
PYTHON_BIN="${PYTHON_BIN:-python3.12}"
command -v "$PYTHON_BIN" >/dev/null
command -v npm >/dev/null
node -e 'const [major, minor] = process.versions.node.split(".").map(Number); if (major < 22 || (major === 22 && minor < 12)) { console.error("Node.js >= 22.12 erforderlich"); process.exit(1); }'
"$PYTHON_BIN" -c 'import sys; assert sys.version_info >= (3, 12), "Python >= 3.12 erforderlich"'
"$PYTHON_BIN" -m venv .venv
.venv/bin/python -m pip install --upgrade pip
matcher='./behoerden-matcher[elastic,test]'
if $semantic; then matcher='./behoerden-matcher[elastic,semantic,test]'; fi
.venv/bin/python -m pip install -e './behoerden-resolver[test,reports]' -e "$matcher" -e './behoerden-finder[test]'
npm ci --prefix tools/fim-cli --no-audit --no-fund
echo 'Installiert. Weiter mit README.md: Resolver laden und FIM-Katalog vorbereiten.'
