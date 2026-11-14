AUTHORITY MATCHER: Ausgabe auf Behördenkategorie und Score reduzieren

Installation (Mac):
  cd ~/Desktop/GdiHackathonChallenge4
  unzip -o ~/Downloads/authority-matcher-compact-output-hotfix.zip
  cd behoerden-matcher

Test:
  ./.venv/bin/authority-matcher --json --no-semantic 'Ich möchte meinen Wohnsitz abmelden'

Standard-JSON:
  {
    "behoerdenkategorie": "buergeramt",
    "score": <aggregierter Ranking-Score>
  }

Ohne --json wird eine kurze Textzeile ausgegeben.
Nur fuer Diagnose: --json --details (oder --debug) zeigt die ausfuehrlichen Nachweise.

- Kein Neuimport und keine Neuinstallation erforderlich (editable Python install)
- Index und SQLite-Daten bleiben erhalten
- Der Score ist ein relativer Ranking-Wert, keine Wahrscheinlichkeit
- Bei mehrdeutigen Ergebnissen: beide Werte null
- Das eigentliche Matching und die PVOG-Abfragen bleiben unveraendert

49 lokale Tests erfolgreich.
