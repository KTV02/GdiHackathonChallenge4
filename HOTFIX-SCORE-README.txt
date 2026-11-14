Authority Matcher – Top-Level Ranking-Score (Hotfix)

Neu: matcher.py liefert bei vorschlag/mehrdeutig im JSON einen Top-Level-Schluessel
  "score": <float>
mit demselben aggregierten Kategorie-Ranking wie bei alternativen[].score.
Ohne ermittelte Kategorie lautet der Wert null.
Der Score ist keine kalibrierte Konfidenzwahrscheinlichkeit.

cli.py gibt den Top-Kategorie-Score auch im normalen Terminalmodus aus.
Keine Aenderung an FIM, PVOG, Cache, Index, SQLite oder Abhaengigkeiten.

Installieren:
  cd ~/Desktop/GdiHackathonChallenge4
  unzip -o ~/Downloads/authority-matcher-score-hotfix.zip
  cd behoerden-matcher
  ./.venv/bin/authority-matcher --json --no-semantic "Ich moechte meinen Wohnsitz abmelden"

Die bestehende editierbare Installation (.venv) bleibt nutzbar: kein pip install notwendig.

45 lokale Tests bestanden (darunter 5 neue Score-Tests).
