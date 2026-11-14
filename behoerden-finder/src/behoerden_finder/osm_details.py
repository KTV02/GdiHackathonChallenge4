"""Zusatzinfos zu einer gefundenen Behörde aus OpenStreetMap (Open Data, ODbL): Anschrift, Öffnungszeiten
(``opening_hours``), Telefon, Webseite, E-Mail, Link zum OSM-Objekt. Kein Schlüssel nötig.

Abgefragt wird die Nominatim-Suche mit ``extratags`` im Umkreis der Behörde; der Treffer wird über Namensähnlichkeit
und Straße/Hausnummer gewählt. Gesendet werden nur Name/Ort der Behörde, nie Anliegen oder Adresse der Person.
"""
from __future__ import annotations

import os
import re
import threading
import time
from datetime import datetime

import requests

CONTACT = os.environ.get("GEOCODER_CONTACT", "https://github.com/KTV02/GdiHackathonChallenge4")
CACHE_SEKUNDEN = 6 * 3600
MIN_AEHNLICHKEIT = 0.5

_FOLD = str.maketrans({"ä": "a", "ö": "o", "ü": "u", "ß": "ss"})
_FUELL = set("der die das des dem den und fur in im am an bei von zu zur zum mit stadt landeshauptstadt kreisfreie "
             "gemeinde markt e v gmbh amt".split())
TAGE = ["Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"]
TAGNAMEN = ["Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"]


def _tokens(text: str) -> set[str]:
    words = re.findall(r"[a-z0-9]+", (text or "").lower().translate(_FOLD))
    return {w for w in words if w not in _FUELL and len(w) > 1}


def aehnlichkeit(behoerde: str, osm_name: str) -> float:
    a, b = _tokens(behoerde), _tokens(osm_name)
    if not a or not b:
        return 0.0
    gemeinsam = sum(1 for x in a if any(x == y or (len(x) >= 5 and (x in y or y in x)) for y in b))
    return 2 * gemeinsam / (len(a) + len(b))   # Dice-Koeffizient: Ortsnamen allein reichen nicht


# --- opening_hours (häufige Formen; Unbekanntes -> nur Rohtext) ------------------------------------------------------

def _days(spec: str) -> list[int] | None:
    out = []
    for part in spec.split(","):
        part = part.strip()
        if "-" in part:
            a, b = part.split("-", 1)
            if a not in TAGE or b not in TAGE:
                return None
            i, j = TAGE.index(a), TAGE.index(b)
            out += list(range(i, j + 1)) if i <= j else list(range(i, 7)) + list(range(0, j + 1))
        elif part in TAGE:
            out.append(TAGE.index(part))
        else:
            return None
    return out


def oeffnungszeiten(raw: str | None) -> dict | None:
    """``{"tage": [7 Zeilen], "intervalle": {tag: [(von, bis)]}}`` oder None, wenn das Format nicht verstanden wird.
    Feiertage (PH) und Schulferien (SH) werden ignoriert."""
    if not raw:
        return None
    week: dict[int, list[tuple[str, str]]] = {i: [] for i in range(7)}
    for rule in [r.strip() for r in raw.split(";") if r.strip()]:
        if rule.startswith(("PH", "SH")):
            continue
        if rule == "24/7":
            for i in range(7):
                week[i] = [("00:00", "24:00")]
            continue
        m = re.fullmatch(r"([A-Za-z,\- ]+?)\s+(off|closed|(?:\d\d:\d\d-\d\d:\d\d)(?:\s*,\s*\d\d:\d\d-\d\d:\d\d)*)", rule)
        days = _days(m.group(1).replace(" ", "")) if m else None
        if days is None:
            return None
        times = [] if m.group(2) in ("off", "closed") else [tuple(t.strip().split("-")) for t in m.group(2).split(",")]
        for d in days:
            week[d] = times   # spätere Regel überschreibt frühere (OSM-Semantik)
    def zeile(i):
        if week[i] == [("00:00", "24:00")]:
            return f"{TAGNAMEN[i]}: rund um die Uhr"
        return f"{TAGNAMEN[i]}: " + (", ".join(f"{a}–{b}" for a, b in week[i]) if week[i] else "geschlossen")
    lines = [zeile(i) for i in range(7)]
    return {"tage": lines, "intervalle": week}


def jetzt_geoeffnet(parsed: dict | None, now: datetime | None = None) -> bool | None:
    if not parsed:
        return None
    if now is None:
        try:
            from zoneinfo import ZoneInfo
            now = datetime.now(ZoneInfo("Europe/Berlin"))
        except Exception:
            now = datetime.now()
    hm = now.strftime("%H:%M")
    return any(a <= hm < b for a, b in parsed["intervalle"][now.weekday()])


# --- Abfrage über Nominatim (OSM-Suche mit Zusatzattributen) ------------------------------------------------------
# Die öffentliche Overpass-API ist oft überlastet (Zeitüberschreitungen); Nominatim liefert dieselben OSM-Attribute
# (opening_hours, phone, website ...) über ``extratags`` schnell und zuverlässig.

NOMINATIM_URL = os.environ.get("NOMINATIM_URL", "https://nominatim.openstreetmap.org/search")
FENSTER_PUNKT, FENSTER_FLAECHE = 0.01, 0.25     # halbe Kantenlänge des Suchfensters in Grad
_takt_lock = threading.Lock()
_letzte_anfrage = [0.0]


def _namensvarianten(name: str) -> list[str]:
    """Behördenname und kürzere Varianten ("Landeshauptstadt Wiesbaden – Kassen- und Steueramt" -> "Kassen- und Steueramt")."""
    out = [name]
    for sep in (" – ", " - ", ", ", ": "):
        if sep in name:
            out += [part.strip() for part in name.split(sep)[::-1] if len(part.strip()) > 3]
    return list(dict.fromkeys(out))[:3]


class OsmClient:
    name = "osm"
    verfuegbar = True

    def __init__(self, *, http_get=None, clock=time.monotonic, sleep=time.sleep, timeout: float = 10.0):
        self._get = http_get or requests.get
        self._clock, self._sleep, self.timeout = clock, sleep, timeout
        self._cache: dict[str, tuple[float, dict]] = {}
        self._lock = threading.Lock()

    def _suche(self, text: str, zentrum, fenster: float) -> list[dict]:
        lat, lon = zentrum
        with _takt_lock:                     # Nominatim-Richtlinie: höchstens 1 Anfrage pro Sekunde
            wait = 1.1 - (self._clock() - _letzte_anfrage[0])
            if wait > 0 and _letzte_anfrage[0]:
                self._sleep(wait)
            try:
                r = self._get(NOMINATIM_URL, timeout=self.timeout,
                              params={"q": text, "format": "jsonv2", "countrycodes": "de", "limit": 8, "extratags": 1,
                                      "addressdetails": 1, "bounded": 1,
                                      "viewbox": f"{lon - fenster:.5f},{lat + fenster:.5f},{lon + fenster:.5f},{lat - fenster:.5f}"},
                              headers={"User-Agent": f"Behoerdenfinder-Hackathon/0.1 ({CONTACT})", "Accept-Language": "de"})
            finally:
                _letzte_anfrage[0] = self._clock()
        if r.status_code != 200:
            raise RuntimeError(f"Nominatim: HTTP {r.status_code}")
        data = r.json()
        return data if isinstance(data, list) else []

    def details(self, name: str, adresse: str | None = None, zentrum: tuple[float, float] | None = None,
                punkt: bool = False) -> dict:
        name = (name or "").strip()[:200]
        if len(name) < 3 or not zentrum:
            return {"verfuegbar": True, "gefunden": False, "quelle": "osm"}
        fenster = FENSTER_PUNKT if punkt else FENSTER_FLAECHE
        key = f"{name}|{adresse}|{round(zentrum[0], 3)},{round(zentrum[1], 3)}|{fenster}"
        with self._lock:
            hit = self._cache.get(key)
            if hit and self._clock() - hit[0] < CACHE_SEKUNDEN:
                return hit[1]
        result = {"verfuegbar": True, "gefunden": False, "quelle": "osm"}
        try:
            for variante in _namensvarianten(name):
                result = self._best(self._suche(variante, zentrum, fenster), name, adresse)
                if result["gefunden"]:
                    break
        except (requests.RequestException, RuntimeError, ValueError) as exc:
            return {"verfuegbar": True, "gefunden": False, "quelle": "osm", "fehler": f"OSM-Abfrage fehlgeschlagen: {exc}"}
        with self._lock:
            if len(self._cache) >= 500:
                self._cache.pop(next(iter(self._cache)))
            self._cache[key] = (self._clock(), result)
        return result

    @staticmethod
    def _best(items: list, name: str, adresse: str | None) -> dict:
        addr = (adresse or "").lower().translate(_FOLD)
        best, best_score = None, 0.0
        for it in items:
            a = it.get("address") or {}
            score = aehnlichkeit(name, it.get("name") or "")
            road = (a.get("road") or "").lower().translate(_FOLD)
            if road and road in addr and (a.get("house_number") or "#") in addr:
                score += 0.6       # gleiche Anschrift wie in den amtlichen Daten
            if score > best_score:
                best, best_score = it, score
        if best is None or best_score < MIN_AEHNLICHKEIT:
            return {"verfuegbar": True, "gefunden": False, "quelle": "osm"}
        a, x = best.get("address") or {}, best.get("extratags") or {}
        street = " ".join(v for v in (a.get("road"), a.get("house_number")) if v)
        place = " ".join(v for v in (a.get("postcode"), a.get("city") or a.get("town") or a.get("village")) if v)
        raw = x.get("opening_hours")
        parsed = oeffnungszeiten(raw)
        return {
            "verfuegbar": True, "gefunden": True, "quelle": "osm", "name": best.get("name"),
            "adresse": ", ".join(v for v in (street, place) if v) or None,
            "lat": float(best["lat"]) if best.get("lat") else None, "lon": float(best["lon"]) if best.get("lon") else None,
            "telefon": x.get("phone") or x.get("contact:phone"),
            "web": x.get("website") or x.get("contact:website"),
            "email": x.get("email") or x.get("contact:email"),
            "oeffnungszeiten": parsed["tage"] if parsed else ([raw] if raw else []),
            "intervalle": [[list(t) for t in parsed["intervalle"][i]] for i in range(7)] if parsed else None,
            "jetzt_geoeffnet": jetzt_geoeffnet(parsed),
            "maps_url": f"https://www.openstreetmap.org/{best.get('osm_type', 'node')}/{best.get('osm_id')}",
        }
