#!/bin/zsh
# Importiert den zugangsbeschränkten BZB-Dump (Staatsanwaltschaften, Hauptzollämter) in die Resolver-Datenbank
# und erzeugt das Abdeckungsdokument neu. Protokoll: behoerden-resolver/data/restricted/import.log
cd "${0:A:h}/behoerden-resolver" || exit 1
mkdir -p data/restricted
LOG=data/restricted/import.log
PY=../behoerden-finder/.venv/bin/python
{
  echo "Start: $(date)"
  if [ ! -f data/restricted/zustandigkeit.backup ]; then
    SRC=$(mdfind -name zustandigkeit.backup 2>/dev/null | grep -v '/data/restricted/' | head -1)
    [ -z "$SRC" ] && SRC=$(find ~/Downloads ~/Desktop ~/Documents -name 'zustandigkeit.backup' 2>/dev/null | head -1)
    if [ -z "$SRC" ]; then echo "FEHLER: zustandigkeit.backup nicht gefunden. Bitte nach behoerden-resolver/data/restricted/ kopieren."; exit 1; fi
    echo "Kopiere $SRC"; cp "$SRC" data/restricted/zustandigkeit.backup || exit 1
  fi
  docker compose up -d db || { echo "FEHLER: Docker Desktop läuft nicht?"; exit 1; }
  docker compose build cli || exit 1
  docker compose run --rm cli sync --source bzb_restricted_staatsanwaltschaft --source bzb_restricted_zoll --input /data/restricted/zustandigkeit.backup || exit 1
  docker compose run --rm cli resolve staatsanwaltschaft --lon 6.96 --lat 50.94 | head -c 1500; echo
  docker compose run --rm cli resolve zoll --lon 13.40 --lat 52.52 | head -c 1500; echo
  docker compose run --rm cli resolve bauamt --lon 7.4606 --lat 50.4289 | head -c 1500; echo
  $PY -m pytest -q tests 2>&1 | tail -15
  $PY scripts/report.py | tail -20 || exit 1
  $PY -c "import docx" 2>/dev/null || $PY -m pip install -q python-docx
  $PY scripts/create_document.py
  echo "Exit: $?"; echo "Ende: $(date)"
} 2>&1 | tee $LOG
echo; echo "Fertig. Dieses Fenster kann geschlossen werden."
