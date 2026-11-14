"""Stichwort-Rückfall für die Behördenart, wenn die amtlichen Daten keine liefern.

Der Matcher leitet die Behördenkategorie aus den JZuFi-Daten des PVOG ab. Für viele Leistungen
(Steuern, Kindergeld, Arbeitslosengeld, BAföG, ...) steht dort keine Behördenart; der Matcher meldet dann
``behoerdenart_in_fim_nicht_ermittelbar``. Dieser Rückfall ordnet in dem Fall anhand von Stichwörtern in der
Eingabe und in den Titeln der bestplatzierten Leistungen zu. Das Ergebnis ist **nicht amtlich belegt** und wird
in der Ausgabe als aus Stichwörtern abgeleitet gekennzeichnet.

Schreibweise der Stichwörter: Teilzeichenfolge ohne Beachtung von Groß-/Kleinschreibung und Umlauten; ``^`` vorn
verlangt einen Wortanfang; ein Tupel ``(stichwort, gewicht)`` zählt mehr als ein einzelnes Stichwort.
"""
from __future__ import annotations

import re
from dataclasses import dataclass

_FOLD = str.maketrans({"ä": "a", "ö": "o", "ü": "u", "ß": "ss"})

# Gewicht eines Treffers je Quelle: eigene Worte der Person > bestplatzierte Leistung > weitere Leistungen
W_QUERY, W_TOP_TITLE, W_OTHER_TITLE = 2.0, 1.0, 0.5
MIN_SCORE = 1.0          # mindestens ein Treffer im Titel der besten Leistung oder ein halber Treffer in den eigenen Worten
MARGIN = 1.5             # bester Wert muss den zweitbesten um diesen Faktor übertreffen

TOPICS: dict[str, list] = {
    "finanzamt": ["finanzamt", "^steuer", "einkommensteuer", "umsatzsteuer", "lohnsteuer", "körperschaftsteuer",
                  "erbschaftsteuer", "schenkungsteuer", "steuererklärung", "steuernummer", "steuer-id",
                  "steuerbescheid", "elster", "elstam", "^schenkung", "^geschenkt", "finanzamt"],
    "stadtverwaltung": ["stadtverwaltung", "gemeindeverwaltung", "rathaus", "hund anmelden", "hund abmelden",
                        "hundesteuer", "hundeanmeldung", "hundemarke"],
    "kreisverwaltung": ["landratsamt", "kreisverwaltung"],
    "regierungsprs": ["regierungspräsidium", "bezirksregierung"],
    "ordnungsamt": ["ordnungsamt", "ordnungsbehörde", "lärmbelästigung", "ruhestörung"],
    "buergeramt": ["bürgeramt", "bürgerbüro", "einwohnermeldeamt", "meldebehörde", "personalausweis", "reisepass",
                   "ummelden", "wohnsitz", "meldebescheinigung", "führungszeugnis", "kinderreisepass"],
    "standesamt": ["standesamt", "heirat", "eheschließung", "geburtsurkunde", "sterbeurkunde", "heiratsurkunde",
                   "lebenspartnerschaft", "sterbefall", "ehename"],
    "auslaenderbehoerde": ["ausländerbehörde", "aufenthaltstitel", "aufenthaltserlaubnis", "niederlassungserlaubnis",
                           "blaue karte", "einbürgerung", "staatsangehörigkeit", "duldung"],
    "bundesamt_migration": ["bamf", "asylantrag", "^asyl", "integrationskurs", "flüchtling"],
    "amtsgericht": ["amtsgericht", "scheidung", "^scheiden", "nachlassgericht", "erbschein", "ausschlagung",
                    "betreuungsgericht", "rechtliche betreuung", "grundbuch", "handelsregister", "vereinsregister",
                    "mahnbescheid", "insolvenz", "zwangsversteigerung"],
    "landgericht": ["landgericht"],
    "verwaltungsgericht": ["verwaltungsgericht"],
    "staatsanwaltschaft": ["staatsanwaltschaft"],
    "polizei": ["polizei", "diebstahl", "gestohlen", "strafanzeige", "anzeige erstatten", "einbruch", "überfall"],
    "zoll": ["^zoll", "hauptzollamt", "einfuhr", "ausfuhr", "schwarzarbeit", "mindestlohn",
             ("kfz-steuer", 3), ("kraftfahrzeugsteuer", 3)],
    "jugendamt": ["jugendamt", "jugendhilfe", "unterhaltsvorschuss", "sorgerecht", "adoption", "pflegekind",
                  "vaterschaft", "beistandschaft", "kindeswohl", "hilfe zur erziehung"],
    "sozialamt": ["sozialamt", "sozialhilfe", "hilfe zum lebensunterhalt", "grundsicherung im alter",
                  "grundsicherung bei erwerbsminderung"],
    "jobcenter": ["jobcenter", "bürgergeld", ("arbeitslosengeld ii", 3), "grundsicherung für arbeitsuchende", "hartz"],
    "arbeitsagentur": ["arbeitsagentur", "arbeitsamt", "arbeitslos", "arbeitsuchend", "arbeitssuchend",
                       "kurzarbeitergeld", "insolvenzgeld", "berufsberatung", "arbeitsvermittlung"],
    "familienkasse": ["familienkasse", "kindergeld", "kinderzuschlag"],
    "wohngeldstelle": ["wohngeld", "lastenzuschuss"],
    "bauamt": ["bauamt", "bauantrag", "baugenehmigung", "bauvoranfrage", "bauaufsicht", "baulast", "bauvorhaben"],
    "strassenverkehrsbehoerde": ["führerschein", "fahrerlaubnis", "straßenverkehrsamt", "verkehrsbehörde",
                                 "fahreignung", "schwertransport", "bewohnerpark"],
    "zulassungsstelle": ["zulassungsstelle", "fahrzeugzulassung", "kfz-zulassung", "auto zulassen",
                         "fahrzeug zulassen", "fahrzeug anmelden", "auto anmelden", "fahrzeug abmelden",
                         "^kfz", "kennzeichen", "zulassungsbescheinigung", "kraftfahrzeug"],
    "gesundheitsamt": ["gesundheitsamt", "gesundheitszeugnis", "infektionsschutz", "amtsarzt", "amtsärztlich"],
    "veterinaeramt": ["veterinär", "amtstierarzt", "tierseuche", "tiertransport", "lebensmittelüberwachung",
                      "tierschutz"],
    "umweltamt": ["umweltamt", "naturschutz", "immissionsschutz", "altlast", "baumschutz", "baum fällen",
                  "wasserschutzgebiet", "gewässer", "grundwasser", "artenschutz"],
    "gewerbeamt": ["gewerbeamt", "gewerbeanmeldung", "gewerbe anmelden", "gewerbeabmeldung", "gewerbe abmelden",
                   "gewerbeummeldung", "gewerbe ummelden", "gewerbeschein", "reisegewerbe", "gaststättenerlaubnis",
                   "gewerbeuntersagung"],
    "ihk": ["^ihk", "handelskammer", "sachkundeprüfung", "ursprungszeugnis", "fachkundeprüfung"],
    "handwerkskammer": ["handwerkskammer", "handwerksrolle", "^hwk", "lehrlingsrolle", "meisterbrief"],
    "rentenversicherung": ["^rente", "altersrente", "erwerbsminderungsrente", "witwenrente", "waisenrente",
                           "rentenversicherung", "kontenklärung", "versicherungsverlauf"],
    "krankenkasse": ["krankenkasse", "krankenversicherung", "krankengeld", "gesundheitskarte", "versichertenkarte",
                     "familienversicherung", "mitgliedsbescheinigung"],
    "pflegekasse": ["pflegekasse", "pflegegrad", "pflegegeld", "pflegeversicherung", "pflegehilfsmittel"],
    "bafoegamt": ["bafög", "ausbildungsförderung"],
    "schulamt": ["schulamt", "schulaufsicht", "schulpflicht"],
    "bundesverwaltungsamt": ["bundesverwaltungsamt", "spätaussiedler", "aussiedler", "auslandsschulwesen"],
}


def fold(text: str) -> str:
    return text.lower().translate(_FOLD)


def _compile(entry) -> tuple[re.Pattern, float, str]:
    keyword, weight = entry if isinstance(entry, tuple) else (entry, 1.0)
    shown = keyword.lstrip("^")
    body = re.escape(fold(shown))
    pattern = re.compile((r"(?<![a-z0-9])" if keyword.startswith("^") else "") + body)
    return pattern, float(weight), shown


_COMPILED: dict[str, list[tuple[re.Pattern, float, str]]] = {}
for _cat, _entries in TOPICS.items():
    seen: set[str] = set()
    _COMPILED[_cat] = [c for c in map(_compile, _entries) if not (c[2] in seen or seen.add(c[2]))]


@dataclass(frozen=True)
class TopicHint:
    category: str
    score: float
    keywords: tuple[str, ...]
    from_query: bool          # mindestens ein Stichwort steht in den eigenen Worten der Person


def _score(category: str, sources: list[tuple[str, float]]) -> tuple[float, list[str], bool]:
    total, hits, from_query = 0.0, [], False
    for pattern, weight, shown in _COMPILED[category]:
        best = 0.0
        for text, source_weight in sources:
            if pattern.search(text):
                best = max(best, weight * source_weight)
                from_query = from_query or source_weight == W_QUERY
        if best:
            total += best
            hits.append(shown)
    return total, hits, from_query


def infer(anliegen: str, titles: list[str] | tuple[str, ...] = (), *, restrict: set[str] | None = None) -> TopicHint | None:
    """Beste Behördenart aus Stichwörtern oder ``None``, wenn nichts eindeutig vorn liegt."""
    sources = [(fold(anliegen), W_QUERY)]
    for i, title in enumerate(titles[:3]):
        sources.append((fold(title), W_TOP_TITLE if i == 0 else W_OTHER_TITLE))
    scored = []
    for category in _COMPILED:
        if restrict is not None and category not in restrict:
            continue
        score, hits, from_query = _score(category, sources)
        if score:
            scored.append((score, category, hits, from_query))
    if not scored:
        return None
    scored.sort(key=lambda x: (-x[0], x[1]))
    best = scored[0]
    second = scored[1][0] if len(scored) > 1 else 0.0
    if best[0] < MIN_SCORE or (second and best[0] < MARGIN * second):
        return None
    return TopicHint(best[1], best[0], tuple(best[2]), best[3])
