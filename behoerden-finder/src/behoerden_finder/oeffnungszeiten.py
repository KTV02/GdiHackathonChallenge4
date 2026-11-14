"""Öffnungszeiten aus freiem Text lesen ("Mo-Do 08:00-12:00 Uhr", "Montag bis Freitag 8.00 - 12.00 Uhr und
14 - 16 Uhr", "Service-Center: Mo: 08:00-16:00 Do: 08:00-18:00", OSM-Schreibweise "Mo-Fr 08:00-12:00; Sa off").

Ergebnis: 7 Listen (Montag zuerst) mit [von, bis] im Format HH:MM, oder None, wenn nichts Verlässliches erkannt wird.
"""
from __future__ import annotations

import re

TAGNAMEN = ["Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"]
_TAG = {"mo": 0, "mon": 0, "montag": 0, "montags": 0,
        "di": 1, "tu": 1, "dienstag": 1, "dienstags": 1,
        "mi": 2, "we": 2, "mittwoch": 2, "mittwochs": 2,
        "do": 3, "th": 3, "donnerstag": 3, "donnerstags": 3,
        "fr": 4, "freitag": 4, "freitags": 4,
        "sa": 5, "samstag": 5, "samstags": 5, "sonnabend": 5,
        "so": 6, "su": 6, "sonntag": 6, "sonntags": 6}
_TAGWORT = "|".join(sorted(_TAG, key=len, reverse=True))
_ZEIT = r"(\d{1,2})(?:[:.](\d{2}))?"
TOKEN = re.compile(
    rf"(?P<tag>\b(?:{_TAGWORT})\b\.?)"
    rf"|(?P<bis>\s*(?:-|–|bis)\s*)"
    rf"|(?P<zeit>\b{_ZEIT}\s*(?:uhr\s*)?(?:-|–|bis)\s*{_ZEIT}\s*(?:uhr)?)"
    rf"|(?P<ganz>\b(?:geschlossen|closed|off)\b)",
    re.I)


def _hm(h, m) -> str | None:
    h, m = int(h), int(m or 0)
    if not (0 <= h <= 24 and 0 <= m < 60) or (h == 24 and m):
        return None
    return f"{h:02d}:{m:02d}"


def parse(text: str | None) -> list[list[list[str]]] | None:
    if not text:
        return None
    week: list[list[list[str]]] = [[] for _ in range(7)]
    tage: list[int] = []
    letzter_tag: int | None = None
    bereich = False
    nach_zeit = False
    treffer = 0
    for m in TOKEN.finditer(text.replace("\\n", "\n")):
        if m.group("tag"):
            wort = m.group("tag").rstrip(".")
            if len(wort) <= 3 and not wort[0].isupper():      # "so", "do" im Fließtext sind keine Wochentage
                continue
            tag = _TAG[wort.lower()]
            if nach_zeit:                       # neue Tagesangabe nach Zeiten: neue Regel
                tage, nach_zeit = [], False
            if bereich and letzter_tag is not None:
                i = letzter_tag
                while i != tag:
                    i = (i + 1) % 7
                    if i not in tage:
                        tage.append(i)
            elif tag not in tage:
                tage.append(tag)
            letzter_tag, bereich = tag, False
        elif m.group("bis"):
            bereich = letzter_tag is not None and not nach_zeit
        elif m.group("zeit"):
            g = m.groups()
            von, bis = _hm(g[3], g[4]), _hm(g[5], g[6])
            if von and bis and von < bis and tage:
                for t in tage:
                    if [von, bis] not in week[t]:
                        week[t].append([von, bis])
                treffer += 1
            nach_zeit, bereich = True, False
        elif m.group("ganz"):
            for t in tage:
                week[t] = []
            nach_zeit, bereich = True, False
    if not treffer:
        return None
    for t in range(7):
        week[t].sort()
    return week


def zeilen(week) -> list[str]:
    def z(i):
        if week[i] == [["00:00", "24:00"]]:
            return f"{TAGNAMEN[i]}: rund um die Uhr"
        return f"{TAGNAMEN[i]}: " + (", ".join(f"{a}–{b}" for a, b in week[i]) if week[i] else "geschlossen")
    return [z(i) for i in range(7)]
