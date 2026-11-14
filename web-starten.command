#!/bin/zsh
# Startet die Weboberfläche des Behörden-Finders auf http://127.0.0.1:8765 (Beenden: Ctrl+C).
cd "${0:A:h}"
( cd behoerden-resolver && docker compose up -d db >/dev/null 2>&1 ) || echo "Hinweis: PostGIS-Container nicht gestartet (Docker Desktop läuft?)."
( sleep 3 && open "http://127.0.0.1:8765" ) &
# Aus behoerden-finder/ starten: im Hauptordner liegt ein älteres Paket "authority_matcher", das sonst den Matcher verdeckt.
cd behoerden-finder
exec .venv/bin/python -m behoerden_finder.web "$@"
