"""Fall-Katalog: gängige Behördengänge (Freitext-Wendungen) -> Behördenart.

Der Katalog besteht aus mehreren tausend kurzen Wendungen ("Hund anmelden", "Kfz zulassen", "Scheidung einreichen"),
die jeweils einer der Behördenkategorien zugeordnet sind. Er liegt als Tabelle ``faelle`` in derselben SQLite-Datei
wie der FIM-Katalog des Matchers (``fim_catalog.sqlite3``) und wird von dort gelesen. Fehlt die Tabelle (oder ist sie
veraltet), legt ``ensure_installed`` sie aus den mitgelieferten Daten (``faelle_daten``) an. Die Spalte ``quelle``
hält fest, woher eine Zeile stammt; eigene Ergänzungen mit anderer ``quelle`` bleiben bei einer Neuinstallation erhalten.

Die Zuordnung ist Teamwissen und **nicht amtlich belegt**; die Ausgabe kennzeichnet sie entsprechend.

Zuordnung: Eingabe und Wendungen werden gefaltet (Kleinschreibung, Umlaute), in Wörter zerlegt, von Füllwörtern
befreit und leicht gestemmt (Endungen e/en/er/n/s). Eine Wendung trifft, wenn alle ihre Wörter in der Eingabe
vorkommen (auch als Wortanfang bzw. in Zusammensetzungen). Die Stärke einer Wendung ist die Zahl ihrer Wörter;
je Kategorie zählt die beste Wendung, weitere Treffer erhöhen den Wert nur leicht.
"""
from __future__ import annotations

import hashlib
import os
import re
import sqlite3
import sys
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

from . import faelle_daten

QUELLE = "fallkatalog"
TABLE = "faelle"
META = "faelle_meta"

_FOLD = str.maketrans({"ä": "a", "ö": "o", "ü": "u", "ß": "ss"})
_TOKEN = re.compile(r"[a-z0-9]+")
STOP = frozenset("""ich mein meine meinen meiner meinem meines mir mich möchte moechte mochte will wollen kann muss soll
wie was wo wer wann ein eine einen einem einer eines der die das den dem des und oder für fur von vom zu zur zum bei beim
in im an am auf aus mit nach über ueber uber um ob es ist sind hat habe haben bitte gerne nicht kein keine wir uns unser
mein dass wegen als auch noch nur ihr ihre sich""".split())

MARGIN = 1.3             # bester Wert muss den zweitbesten um diesen Faktor übertreffen
CONTAINS_MIN = 7         # Wörter ab dieser Länge dürfen auch mitten in einer Zusammensetzung stehen
PREFIX_MIN = 6           # ab dieser Länge genügt ein Wortanfang ("anmeld" -> "anmeldung")


def fold(text: str) -> str:
    return text.lower().translate(_FOLD)


def _stem(word: str) -> str:
    """Leichtes Stemming: bis zu zwei Endungen (ung/en/er/es/em/e/n/s) abschneiden, Rest mindestens 4 Zeichen."""
    for _ in range(2):
        for suffix in ("ung", "en", "er", "es", "em", "e", "n", "s"):
            if len(word) - len(suffix) >= 4 and word.endswith(suffix):
                word = word[: -len(suffix)]
                break
        else:
            break
    return word


_STOP_FOLDED = frozenset(fold(w) for w in STOP)

# Umgangssprachliche Formen auf die Form bringen, in der die Wendungen stehen (gilt für Eingabe und Wendungen).
SYNONYME = {
    "geklaut": "gestohlen", "klauen": "gestohlen", "bestohlen": "gestohlen", "stehlen": "gestohlen", "geklaute": "gestohlen",
    "eingebrochen": "einbruch", "uberfallen": "uberfall", "uberfallt": "uberfall",
    "weggezogen": "umzug", "umgezogen": "umzug", "umziehen": "umzug", "umzieht": "umzug", "umziehe": "umzug",
    "geboren": "geburt", "verstorben": "gestorben", "eingeschult": "einschulung", "einschulen": "einschulung",
    "geschieden": "scheidung", "scheiden": "scheidung", "gekundigt": "kundigung", "heiraten": "heirat", "geheiratet": "heirat",
    "verheiratet": "heirat", "pleite": "insolvenz", "fuhrerschein": "fuhrerschein", "perso": "personalausweis",
}
# Allgemeine Wörter (Verwandtschaft, Lebenslage), die für sich allein wenig über die Behörde sagen: zählen nur halb.
GENERIC = frozenset(_stem(w) for w in ("vater mutter eltern kind sohn tochter oma opa mann frau ehemann ehefrau nachbar "
                                      "gestorben haus wohnung").split())


# Abgetrennte Verbpartikel ("melde ich meinen Hund an", "melde mein Auto ab"): Partikel + Verb ergibt zusätzlich
# das zusammengesetzte Verb (anmelden, abmelden, ummelden ...). Nur für Verben, die in VERBEN stehen.
PARTIKEL = ("an", "ab", "um", "aus", "ein", "zuruck", "nach", "vor", "auf", "zu", "weg", "mit")
VERBEN = ("meld", "beantrag", "stell", "geb", "schlag", "zieh", "nehm", "trag", "fahr", "les", "lass", "hol", "schreib",
          "wechs", "tausch", "zahl", "reich", "fuhr", "kund", "setz", "send", "leg", "hor", "bau", "fall", "fang", "bring")


def _separable(words: list[str]) -> list[str]:
    particles = [w for w in words if w in PARTIKEL]
    if not particles:
        return []
    extra = []
    for w in words:
        if w in PARTIKEL or w in _STOP_FOLDED:
            continue
        stem = _stem(w)
        if any(stem.startswith(v) for v in VERBEN):
            extra += [_stem(p + w) for p in particles]
    return extra


def tokens(text: str) -> tuple[str, ...]:
    words = _TOKEN.findall(fold(text))
    out = []
    for t in words:
        if t in _STOP_FOLDED:
            continue
        out.append(_stem(SYNONYME.get(t, t)))
    return tuple(out + _separable(words))


def parse(text: str) -> list[tuple[str, str]]:
    """DSL-Block -> [(kategorie, wendung)]. ``kategorie:`` leitet einen Abschnitt ein; Wendungen stehen durch
    ``;`` oder Zeilenumbruch getrennt."""
    out, category = [], None
    for line in text.splitlines():
        line = line.strip()
        if not line:
            continue
        if line.endswith(":") and ";" not in line:
            category = line[:-1].strip()
            continue
        if category is None:
            raise ValueError(f"Wendung ohne Kategorie: {line[:60]!r}")
        for phrase in line.split(";"):
            phrase = phrase.strip()
            if phrase:
                out.append((category, phrase))
    return out


def builtin_rows() -> list[tuple[str, str]]:
    seen, rows = set(), []
    for block in faelle_daten.FAELLE_ALLE:
        for category, phrase in parse(block):
            key = (category, phrase.casefold())
            if key not in seen:
                seen.add(key)
                rows.append((category, phrase))
    return rows


def _fingerprint(rows: list[tuple[str, str]]) -> str:
    h = hashlib.sha256()
    for category, phrase in sorted(rows):
        h.update(f"{category}\t{phrase}\n".encode())
    return h.hexdigest()[:16]


@dataclass(frozen=True)
class FallHint:
    category: str
    score: float
    phrases: tuple[str, ...]   # treffende Wendungen der Siegerkategorie, stärkste zuerst
    strong: bool               # mindestens eine Wendung aus ≥2 Wörtern oder eine mit denselben Wörtern wie die Eingabe
    scores: dict               # alle Kategoriewerte (für Diagnosen)


class FallKatalog:
    def __init__(self, rows: list[tuple[str, str]]):
        self.rows = list(rows)
        self._phrases: list[tuple[str, str, tuple[str, ...], float]] = []
        for category, phrase in self.rows:
            toks = tuple(dict.fromkeys(tokens(phrase)))
            if toks:
                self._phrases.append((category, phrase, toks, sum(0.5 if t in GENERIC else 1.0 for t in toks)))

    def __len__(self) -> int:
        return len(self._phrases)

    @property
    def categories(self) -> set[str]:
        return {c for c, _, _, _ in self._phrases}

    @staticmethod
    def _covers(phrase_token: str, query_tokens: tuple[str, ...]) -> set[str]:
        found = set()
        for q in query_tokens:
            if q == phrase_token:
                found.add(q)
            elif len(phrase_token) >= PREFIX_MIN and q.startswith(phrase_token):
                found.add(q)
            elif len(phrase_token) >= CONTAINS_MIN and phrase_token in q:
                found.add(q)
        return found

    def matches(self, anliegen: str) -> list[tuple[str, str, int, bool, float]]:
        """Alle treffenden Wendungen: (kategorie, wendung, stärke, exakt, gewicht) - exakt: gleiche Wörter wie die
        Eingabe; gewicht: wie stärke, aber allgemeine Wörter (GENERIC) zählen halb."""
        query = tuple(dict.fromkeys(tokens(anliegen)))
        if not query:
            return []
        hits = []
        for category, phrase, toks, weight in self._phrases:
            covered: set[str] = set()
            for t in toks:
                c = self._covers(t, query)
                if not c:
                    break
                covered |= c
            else:
                exact = set(toks) == set(query)
                hits.append((category, phrase, len(toks), exact, weight))
        return hits

    def infer(self, anliegen: str, *, restrict: set[str] | None = None) -> FallHint | None:
        per: dict[str, list[tuple[str, int, bool, float]]] = {}
        for category, phrase, strength, whole, weight in self.matches(anliegen):  # whole = exakt
            if restrict is None or category in restrict:
                per.setdefault(category, []).append((phrase, strength, whole, weight))
        if not per:
            return None
        scores = {}
        for category, items in per.items():
            ranked = sorted(items, key=lambda x: (-(x[3] + (1.0 if x[2] else 0.0)), x[0]))
            best = ranked[0][3] + (1.0 if ranked[0][2] else 0.0)
            extra = min(2.0, sum(w for _, _, _, w in ranked[1:]))
            scores[category] = (best + 0.25 * extra, ranked)
        order = sorted(scores, key=lambda c: (-scores[c][0], c))
        top = order[0]
        second = scores[order[1]][0] if len(order) > 1 else 0.0
        if second and scores[top][0] < MARGIN * second:
            return None
        ranked = scores[top][1]
        strong = any(s >= 2 or e for _, s, e, _ in ranked)
        return FallHint(top, round(scores[top][0], 3), tuple(r[0] for r in ranked[:3]), strong,
                        {c: round(v[0], 3) for c, v in scores.items()})


# --- SQLite ------------------------------------------------------------------------------------------------------

def default_db() -> Path:
    home = os.environ.get("AUTHORITY_MATCHER_HOME")
    return Path(home) / "fim_catalog.sqlite3" if home else Path.home() / ".local/share/authority-matcher/fim_catalog.sqlite3"


def install(db_path: str | Path, rows: list[tuple[str, str]] | None = None) -> int:
    """Tabelle ``faelle`` anlegen bzw. die Zeilen mit ``quelle='fallkatalog'`` ersetzen. Andere Tabellen und
    Zeilen anderer Herkunft bleiben unberührt. Gibt die Zahl der eingetragenen Wendungen zurück."""
    rows = builtin_rows() if rows is None else rows
    con = sqlite3.connect(str(db_path), timeout=30)
    try:
        with con:
            con.execute(f"CREATE TABLE IF NOT EXISTS {TABLE} (kategorie TEXT NOT NULL, phrase TEXT NOT NULL, "
                        f"quelle TEXT NOT NULL DEFAULT '{QUELLE}', PRIMARY KEY (kategorie, phrase))")
            con.execute(f"CREATE TABLE IF NOT EXISTS {META} (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
            con.execute(f"DELETE FROM {TABLE} WHERE quelle = ?", (QUELLE,))
            con.executemany(f"INSERT OR IGNORE INTO {TABLE} (kategorie, phrase, quelle) VALUES (?, ?, ?)",
                            [(c, p, QUELLE) for c, p in rows])
            con.execute(f"INSERT OR REPLACE INTO {META} (key, value) VALUES ('fingerprint', ?)", (_fingerprint(rows),))
        return len(rows)
    finally:
        con.close()


def _read_rows(con: sqlite3.Connection) -> list[tuple[str, str]] | None:
    try:
        return [(c, p) for c, p in con.execute(f"SELECT kategorie, phrase FROM {TABLE} ORDER BY kategorie, phrase")]
    except sqlite3.Error:
        return None


def load(db_path: str | Path | None = None) -> FallKatalog:
    """Katalog aus der Datenbank lesen; ohne Datenbank oder Tabelle aus den mitgelieferten Daten."""
    path = Path(db_path) if db_path else default_db()
    if path.exists():
        try:
            con = sqlite3.connect(f"file:{path}?mode=ro", uri=True, timeout=30)
        except sqlite3.Error:
            con = None
        if con is not None:
            try:
                rows = _read_rows(con)
            finally:
                con.close()
            if rows:
                return FallKatalog(rows)
    return builtin()


@lru_cache(maxsize=1)
def builtin() -> FallKatalog:
    return FallKatalog(builtin_rows())


def ensure_installed(db_path: str | Path | None = None) -> FallKatalog:
    """Tabelle bei Bedarf anlegen oder aktualisieren (ändert nur die Tabellen ``faelle``/``faelle_meta``) und den
    Katalog laden. Ist die Datenbank nicht beschreibbar, wird aus den mitgelieferten Daten gearbeitet."""
    path = Path(db_path) if db_path else default_db()
    if not path.exists():
        return builtin()
    rows = builtin_rows()
    try:
        con = sqlite3.connect(f"file:{path}?mode=ro", uri=True, timeout=30)
        try:
            try:
                stored = con.execute(f"SELECT value FROM {META} WHERE key = 'fingerprint'").fetchone()
            except sqlite3.Error:
                stored = None
        finally:
            con.close()
        if not stored or stored[0] != _fingerprint(rows):
            install(path, rows)
    except sqlite3.Error:
        return builtin()
    return load(path)


def main(argv: list[str] | None = None) -> int:
    import argparse
    p = argparse.ArgumentParser(prog="python -m behoerden_finder.faelle",
                                description="Fall-Katalog in die FIM-Datenbank eintragen bzw. prüfen")
    p.add_argument("aktion", choices=["installieren", "status", "pruefen"])
    p.add_argument("text", nargs="*", help="bei 'pruefen': Anliegen")
    p.add_argument("--db", help="Pfad der SQLite-Datei (Standard: FIM-Datenbank des Matchers)")
    args = p.parse_args(argv)
    db = Path(args.db) if args.db else default_db()
    if args.aktion == "installieren":
        if not db.exists():
            print(f"Datenbank nicht gefunden: {db}", file=sys.stderr)
            return 2
        n = install(db)
        print(f"{n} Wendungen in Tabelle '{TABLE}' von {db} eingetragen.")
        return 0
    cat = load(db)
    if args.aktion == "status":
        print(f"{len(cat)} Wendungen, {len(cat.categories)} Behördenarten (Quelle: {db if db.exists() else 'mitgeliefert'})")
        return 0
    hint = cat.infer(" ".join(args.text))
    print(hint if hint else "keine eindeutige Zuordnung")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
