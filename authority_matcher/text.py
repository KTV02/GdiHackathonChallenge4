"""Small, inspectable German query preparation; no service/authority rules here."""

import re
from difflib import SequenceMatcher
from html.parser import HTMLParser

from .errors import MatcherError

STOPWORDS = set("""
ich mich mir mein meine meinen meinem meiner meines wir uns unser unsere
du dich dein deine sie sich ihr ihre ihren man ein eine einen einem einer
eines der die das den dem des und oder aber auch bitte gerne gern möchte
moechte möchten will wollen würde wuerde kann können koennen muss müssen
brauche brauche benötige benoetige suche wissen wo wie was welche welcher
welches ist sind bin habe haben es zu zum zur für fuer von in im am an auf
bei mit nach um als dass lassen jetzt dann dafür dafuer zuständig zustaendig
""".split())

# These normalize words, never map a service to an authority.
ACTION_FORMS = {
    "anmelden": "anmelden", "anmeldung": "anmelden", "anzumelden": "anmelden",
    "abmelden": "abmelden", "abmeldung": "abmelden", "abzumelden": "abmelden",
    "ummelden": "ummelden", "ummeldung": "ummelden", "umzumelden": "ummelden",
    "beantragen": "beantragen", "beantragung": "beantragen", "beantragt": "beantragen",
    "erneuern": "erneuern", "erneuerung": "erneuern",
    "verlängern": "verlaengern", "verlängerung": "verlaengern",
    "verlaengern": "verlaengern", "verlaengerung": "verlaengern",
    "melden": "melden", "meldung": "melden", "ändern": "aendern",
    "aendern": "aendern", "änderung": "aendern", "aenderung": "aendern",
}
ACTIONS = set(ACTION_FORMS.values())


class _PlainText(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.parts = []
        self.skipped = 0

    def handle_starttag(self, tag, attrs):
        if tag in {"script", "style"}:
            self.skipped += 1
        self.parts.append(" ")

    def handle_endtag(self, tag):
        if tag in {"script", "style"} and self.skipped:
            self.skipped -= 1
        self.parts.append(" ")

    def handle_data(self, data):
        if not self.skipped:
            self.parts.append(data)


def plain(value):
    parser = _PlainText()
    parser.feed(str(value or ""))
    # Strip terminal control characters from source text as well as user text.
    return " ".join(re.sub(r"[\x00-\x1f\x7f-\x9f]", " ", "".join(parser.parts)).split())


def tokens(value):
    words = re.findall(r"[a-zäöüß0-9]+", plain(value).casefold())
    return list(dict.fromkeys(ACTION_FORMS.get(w, w) for w in words if w not in STOPWORDS))


def normalize_query(value):
    return " ".join(tokens(value))


def related_subject_query(value):
    """A broader query for suggestions only; never confirms the original action."""
    search_actions = ACTIONS | {"anzeigen", "anzeige", "anzuzeigen", "abgeben", "abgabe", "erhalten"}
    words = tokens(value)
    subject = [w for w in words if w not in search_actions]
    if subject and len(subject) < len(words):
        return " ".join(subject)
    return None


def _fold(word):
    word = word.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
    return {"hundehaltung": "hund"}.get(word, word)


def _similarity(word, other):
    word, other = _fold(word), _fold(other)
    if word == other:
        return 1.0
    # Never fuzzy-match opposing administrative actions (an-/ab-/ummelden).
    if word in ACTIONS or other in ACTIONS:
        return 0.0
    # Inflections only: arbitrary prefix matching would confuse Auto/Autobahn
    # or Pass/Passwort. Compound equivalents must be explicit lexical aliases.
    short, long = sorted((word, other), key=len)
    if len(short) >= 4 and long.startswith(short) and long[len(short):] in {"e", "en", "er", "es", "s", "n"}:
        return 0.94
    if min(len(word), len(other)) >= 6:
        ratio = SequenceMatcher(None, word, other).ratio()
        if ratio >= 0.88:
            return ratio * 0.9
    return 0.0


def service_synonyms(item):
    synonyms = item.get("synonyms")
    if synonyms is None:
        return []
    if not isinstance(synonyms, list) or not all(isinstance(s, str) for s in synonyms):
        raise MatcherError("invalid_source", "Unerwartetes Format der offiziellen Leistungssynonyme.")
    return [plain(s) for s in synonyms if plain(s)]


def _label_score(terms, label, body_coverage):
    words = tokens(label)
    label_actions = set(words) & ACTIONS
    requested_actions = set(terms) & ACTIONS
    conflict = bool(requested_actions and label_actions and not requested_actions & label_actions)
    coverage = [max((_similarity(t, w) for w in words), default=0) for t in terms]
    score = sum(max(a, b * 0.55) for a, b in zip(coverage, body_coverage)) / max(1, len(terms))
    extras = sum(not any(_similarity(t, w) for t in terms) for w in words)
    score = max(0, score - min(0.35, extras * 0.07))
    return {"score": round(0 if conflict else score, 4),
            "label_coverage": all(c >= 0.8 for c in coverage),
            "exact_label": terms == words, "action_conflict": conflict}


def rank_services(query, records):
    """Check each official title/synonym separately, without invented service aliases.

    Never pool words from unrelated aliases to manufacture a complete intent match.
    Scores remain lexical heuristics, not probabilities or PVOG relevance scores.
    """
    terms = tokens(query)
    ranked = []
    for item in records:
        name = plain(item.get("name"))
        body = tokens(item.get("description") or item.get("teaser"))
        body_coverage = [max((_similarity(t, w) for w in body), default=0) for t in terms]
        title_score = _label_score(terms, name, body_coverage)
        labels = [("name", name), *(("synonyms", s) for s in service_synonyms(item))]
        choices = [{**_label_score(terms, label, body_coverage), "matched_field": field, "matched_text": label}
                   for field, label in labels]
        best = max(choices, key=lambda c: (c["score"], c["exact_label"], c["label_coverage"]))
        ranked.append({"id": item["id"], "name": name, **best,
                       "title_coverage": title_score["label_coverage"], "exact_title": terms == tokens(name)})
    return sorted(ranked, key=lambda item: (-item["score"], not item["exact_title"], not item["exact_label"], item["name"], item["id"]))
