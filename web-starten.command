#!/bin/zsh
# Lokale Webapp; Einrichtung: bash scripts/setup.sh
set -euo pipefail
cd "${0:A:h}"
if [[ ! -x .venv/bin/python ]]; then
  echo 'Python-Umgebung fehlt. Zuerst: bash scripts/setup.sh' >&2
  exit 1
fi
docker compose -f behoerden-resolver/compose.yaml up -d db
exec .venv/bin/python -m behoerden_finder.web "$@"
