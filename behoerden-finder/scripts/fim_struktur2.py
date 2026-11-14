"""Zeigt den Aufbau der beim PVOG-Abruf gespeicherten Leistungsbeschreibungen (jzufi_services.raw_json):
Texte, Unterlagen, Online-Dienste. Schreibt nach ../tuning-ergebnis/fim_struktur2.txt. Nur lesend.
    behoerden-finder/.venv/bin/python behoerden-finder/scripts/fim_struktur2.py
"""
import collections, json, os, sqlite3
from pathlib import Path

db = Path(os.environ.get("AUTHORITY_MATCHER_HOME", Path.home() / ".local/share/authority-matcher")) / "fim_catalog.sqlite3"
out = Path(__file__).resolve().parents[2] / "tuning-ergebnis" / "fim_struktur2.txt"
con = sqlite3.connect(f"file:{db}?mode=ro", uri=True)
paths = collections.Counter()


def walk(o, p=""):
    if isinstance(o, dict):
        for k, v in o.items():
            walk(v, f"{p}.{k}" if p else k)
    elif isinstance(o, list):
        for v in o[:8]:
            walk(v, p + "[]")
    else:
        paths[p] += 1


rows = con.execute("SELECT lb_id, keys_json, title, raw_json, source FROM jzufi_services").fetchall()
for r in rows:
    try:
        walk(json.loads(r[3]))
    except ValueError:
        pass
lines = [f"{len(rows)} jzufi_services", "Feldpfade:"] + [f"{n:6}  {p}" for p, n in sorted(paths.items())]
for want in ("Personalausweis", "Hund", "Kindergeld"):
    for r in rows:
        if want.lower() in r[2].lower():
            lines.append(f"\n===== {r[2]} ({r[0]}, {r[4]}, Schlüssel {r[1]}) =====")
            lines.append(json.dumps(json.loads(r[3]), ensure_ascii=False, indent=1)[:20000])
            break
out.write_text("\n".join(lines), encoding="utf-8")
print(f"geschrieben: {out}")
