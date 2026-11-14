Authority Matcher v3: PVOG lbId / blank OE ID correction (2026-11-13)

Problem confirmed from the user's real PVOG response:
  HTTP 400: Der Parameter lbId vom Typ String fehlt ...
The previous client sent 'lbids' to GET /v2/organisationunits/titles.
At the same time, transfer parsing persisted links with empty org_id.

Fix:
  * Use 'lbId' (singular) for /v2/organisationunits/titles.
  * Ignore relation objects that contain no explicit organisation ID.
  * Delete ONLY broken jzufi_links rows with empty org_id/lb_id at runtime startup.
  * Ignore corrupt blank IDs in follow-up organisation fetches.
  * Use titles fallback if the fetched JZuFi organisation has no proven
    responsible role; don't consider another region's mapping a success.
  * 40 local tests passed (simulated API responses).

Install on Mac:
  cd ~/Desktop/GdiHackathonChallenge4
  unzip -o ~/Downloads/authority-matcher-pvog-lbId-fix.zip
  cd behoerden-matcher
  ./.venv/bin/authority-matcher --json --no-semantic \
    "Ich möchte meinen Wohnsitz abmelden"

The project was installed editable, so no pip reinstall is necessary.
This ZIP changes only two Python modules and regression tests.
It does NOT overwrite FIM SQLite, Elasticsearch, Docker, PostGIS or credentials.
No live PVOG API verification was possible from the build environment.
