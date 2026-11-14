"""Welche Textmodule (Codes) stehen in den PVOG-Leistungsbeschreibungen, mit Textanfang je Code; dazu
Dokumente/Unterlagen, Ursprungsportal und ein vollständiges Beispiel. Nur lesend.
    behoerden-finder/.venv/bin/python behoerden-finder/scripts/fim_struktur3.py
"""
import collections, json, os, re, sqlite3
from pathlib import Path

db = Path(os.environ.get("AUTHORITY_MATCHER_HOME", Path.home() / ".local/share/authority-matcher")) / "fim_catalog.sqlite3"
out = Path(__file__).resolve().parents[2] / "tuning-ergebnis" / "fim_struktur3.txt"
con = sqlite3.connect(f"file:{db}?mode=ro", uri=True)
rows = con.execute("SELECT lb_id, keys_json, title, raw_json FROM jzufi_services").fetchall()
codes, samples = collections.Counter(), {}
kanal = collections.Counter()
for lb, keys, title, raw in rows:
    d = json.loads(raw)
    for m in d.get("modulText") or []:
        c = (m.get("leikaTextmodul") or {}).get("code")
        codes[c] += 1
        kanal[str((m.get("zugangskanal") or {}).get("name"))] += 1
        txt = " ".join(i.get("value") or "" for i in m.get("inhalt") or [])
        if c not in samples and len(txt) > 40:
            samples[c] = f"[{title}] " + re.sub(r"\s+", " ", txt)[:300]
lines = [f"{len(rows)} Leistungen", "Textmodul-Codes:"] + [f"  {c}: {n}x  | {samples.get(c, '')}" for c, n in sorted(codes.items(), key=str)]
lines.append(f"zugangskanal.name: {dict(kanal.most_common(10))}")
best = max(rows, key=lambda r: len(r[3]))
for lb, keys, title, raw in rows:
    if "personalausweis" in title.lower():
        best = (lb, keys, title, raw); break
d = json.loads(best[3])
lines.append(f"\n===== Beispiel: {best[2]} {best[1]} =====")
for k in ("modulDokument", "modulUrsprungsportal", "modulKosten", "modulBearbeitungsdauer", "typisierung",
          "kennzeichenFormularPersoenlich", "kennzeichenFormularSchriftformerfordernis", "vertrauensniveau", "gueltigkeitGebietID"):
    lines.append(f"--- {k}: " + json.dumps(d.get(k), ensure_ascii=False)[:2500])
for m in d.get("modulText") or []:
    lines.append(f"--- modulText {json.dumps(m.get('leikaTextmodul'))} zugangskanal={json.dumps(m.get('zugangskanal'), ensure_ascii=False)}: "
                 + json.dumps(m.get("inhalt"), ensure_ascii=False)[:1500]
                 + " links=" + json.dumps(m.get("weiterfuehrenderLink"), ensure_ascii=False)[:600])
out.write_text("\n".join(lines), encoding="utf-8")
print(f"geschrieben: {out}")
