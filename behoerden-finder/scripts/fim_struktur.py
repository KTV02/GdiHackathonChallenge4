"""Zeigt den Aufbau der FIM-Datensätze (welche Felder es gibt), um Leistungsbeschreibung, Unterlagen und
Online-Dienst auslesen zu können. Schreibt nach ../tuning-ergebnis/fim_struktur.txt.
    behoerden-finder/.venv/bin/python behoerden-finder/scripts/fim_struktur.py
"""
import collections, json, os, sqlite3, sys
from pathlib import Path

db = Path(os.environ.get("AUTHORITY_MATCHER_HOME", Path.home() / ".local/share/authority-matcher")) / "fim_catalog.sqlite3"
out = Path(__file__).resolve().parents[2] / "tuning-ergebnis" / "fim_struktur.txt"
con = sqlite3.connect(f"file:{db}?mode=ro", uri=True)
lines = [f"DB: {db}"]
lines += [f"Tabelle {n}: {con.execute(f'SELECT COUNT(*) FROM {n}').fetchone()[0]} Zeilen"
          for (n,) in con.execute("SELECT name FROM sqlite_master WHERE type='table'")]
paths = collections.Counter()
sources = collections.Counter()


def walk(o, p=""):
    if isinstance(o, dict):
        for k, v in o.items():
            walk(v, f"{p}.{k}" if p else k)
    elif isinstance(o, list):
        for v in o[:5]:
            walk(v, p + "[]")
    else:
        paths[p] += 1


samples = {}
for source, doc in con.execute("SELECT source, document FROM records ORDER BY RANDOM() LIMIT 3000"):
    sources[source] += 1
    try:
        d = json.loads(doc)
    except ValueError:
        continue
    walk(d, source)
    if source not in samples or len(doc) > len(samples[source]):
        samples[source] = doc
lines.append(f"Quellen in der Stichprobe: {dict(sources)}")
lines.append("\nFeldpfade (Häufigkeit in 3000 Datensätzen):")
lines += [f"{n:6}  {p}" for p, n in sorted(paths.items(), key=lambda x: x[0])]
for s, doc in samples.items():
    lines.append(f"\n===== Beispiel {s} (gekürzt) =====")
    lines.append(json.dumps(json.loads(doc), ensure_ascii=False, indent=1)[:12000])
key = sys.argv[1] if len(sys.argv) > 1 else "99012002000000"
lines.append(f"\n===== Datensätze zum Leistungsschlüssel {key} =====")
for source, doc in con.execute("SELECT source, document FROM records WHERE document LIKE ? LIMIT 3", (f"%{key}%",)):
    lines.append(f"--- {source}\n" + json.dumps(json.loads(doc), ensure_ascii=False, indent=1)[:15000])
out.parent.mkdir(exist_ok=True)
out.write_text("\n".join(lines), encoding="utf-8")
print(f"geschrieben: {out}")
