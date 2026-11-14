#!/bin/zsh
# Optionaler Import eines bereits berechtigt bezogenen BZB-Dumps.
# Aufruf: zsh scripts/import-bzb.command /absoluter/pfad/zustandigkeit.backup
set -euo pipefail
cd "${0:A:h}/../behoerden-resolver"
if [[ $# -ne 1 || ! -f "$1" ]]; then
  echo 'Aufruf: zsh scripts/import-bzb.command /absoluter/pfad/zustandigkeit.backup' >&2
  exit 1
fi
mkdir -p data/restricted
TARGET="$PWD/data/restricted/zustandigkeit.backup"
if [[ "${1:A}" != "$TARGET" ]]; then
  cp "$1" "$TARGET"
fi
{
  docker compose up -d db
  docker compose build cli
  docker compose run --rm cli sync --source bzb_restricted_staatsanwaltschaft \
    --source bzb_restricted_zoll --input /data/restricted/zustandigkeit.backup
} 2>&1 | tee data/restricted/import.log
