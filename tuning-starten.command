#!/bin/zsh
# Misst und stellt den Behörden-Matcher ein (Elasticsearch + Auswertung). Läuft nur in diesem Projektordner.
# Ergebnis: tuning-ergebnis/tune.log und tuning-ergebnis/tune_ergebnis.json. Dauer: je nach PVOG-Abruf 30-90 Minuten.
cd "${0:A:h}"
OUT=tuning-ergebnis
mkdir -p $OUT
PY=behoerden-finder/.venv/bin/python
{
  echo "Start: $(date)"
  $PY --version
  cd behoerden-matcher
  ../$PY -m authority_matcher.tune --finder --ausgabe ../$OUT/tune_ergebnis.json
  echo "Exit: $?"
  echo "Ende: $(date)"
} 2>&1 | tee $OUT/tune.log
echo; echo "Fertig. Dieses Fenster kann geschlossen werden."
