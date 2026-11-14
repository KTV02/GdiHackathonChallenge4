#!/bin/zsh
# Matcher-Parameter auswerten; Ergebnisse bleiben lokale Laufartefakte.
set -euo pipefail
cd "${0:A:h}/.."
ROOT="$PWD"
mkdir -p logs/tuning
cd behoerden-matcher
"$ROOT/.venv/bin/python" -m authority_matcher.tune --finder \
  --ausgabe "$ROOT/logs/tuning/ergebnis.json" "$@" 2>&1 | tee "$ROOT/logs/tuning/tune.log"
