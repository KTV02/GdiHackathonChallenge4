"""Zusatzinformationen zu einer gefundenen Behörde über die Google Places API (New): genaue Anschrift,
Öffnungszeiten, Telefon, Webseite, Google-Maps-Link.

Es wird nur der **Name und Ort der Behörde** an Google geschickt, nie das Anliegen und nie die Adresse der Person.
Schlüssel: Umgebungsvariable ``GOOGLE_MAPS_API_KEY`` oder Datei ``~/.config/behoerdenfinder/google_maps_api_key``.
Ohne Schlüssel bleibt die Funktion aus (die Navigation per Google-Maps-Link funktioniert trotzdem).

Ergebnisse werden höchstens ``CACHE_SEKUNDEN`` im Speicher gehalten (die Places-Richtlinien erlauben kein
dauerhaftes Speichern) und in der Oberfläche mit „Daten: Google“ gekennzeichnet.
"""
from __future__ import annotations

import math
import os
import threading
import time
from pathlib import Path

import requests

URL = "https://places.googleapis.com/v1/places:searchText"
FIELDS = ",".join(f"places.{f}" for f in (
    "id", "displayName", "formattedAddress", "location", "googleMapsUri", "nationalPhoneNumber", "websiteUri",
    "regularOpeningHours.weekdayDescriptions", "regularOpeningHours.periods", "currentOpeningHours.openNow"))
KEY_FILE = Path.home() / ".config" / "behoerdenfinder" / "google_maps_api_key"
CACHE_SEKUNDEN = 3600
MAX_ABSTAND_KM = 25.0     # weiter vom erwarteten Ort entfernte Treffer gelten als Fehltreffer


def api_key() -> str | None:
    key = os.environ.get("GOOGLE_MAPS_API_KEY", "").strip()
    if not key and KEY_FILE.is_file():
        key = KEY_FILE.read_text(encoding="utf-8").strip()
    return key or None


def _km(a, b) -> float:
    p1, p2 = math.radians(a[0]), math.radians(b[0])
    dl = math.radians(b[1] - a[1])
    h = math.sin((p2 - p1) / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * 6371.0 * math.asin(math.sqrt(h))


class PlacesClient:
    name = "google"

    def __init__(self, key: str | None = None, *, http_post=None, clock=time.monotonic, timeout: float = 8.0):
        self.key = key if key is not None else api_key()
        self._post = http_post or requests.post
        self._clock, self.timeout = clock, timeout
        self._cache: dict[str, tuple[float, dict]] = {}
        self._lock = threading.Lock()

    @property
    def verfuegbar(self) -> bool:
        return bool(self.key)

    def details(self, name: str, adresse: str | None = None, zentrum: tuple[float, float] | None = None,
                punkt: bool = False) -> dict:
        """Bester Google-Treffer zu Behördenname (+ Anschrift) nahe ``zentrum`` (lat, lon) oder ``{"gefunden": False}``."""
        name = (name or "").strip()[:200]
        if not self.key:
            return {"verfuegbar": False}
        if len(name) < 3:
            return {"verfuegbar": True, "gefunden": False}
        query = ", ".join(x for x in (name, (adresse or "").strip()[:200]) if x)
        cache_key = f"{query}|{zentrum and (round(zentrum[0], 3), round(zentrum[1], 3))}"
        with self._lock:
            hit = self._cache.get(cache_key)
            if hit and self._clock() - hit[0] < CACHE_SEKUNDEN:
                return hit[1]
        body = {"textQuery": query, "languageCode": "de", "regionCode": "DE", "pageSize": 1}
        if zentrum:
            body["locationBias"] = {"circle": {"center": {"latitude": zentrum[0], "longitude": zentrum[1]},
                                               "radius": 20000.0}}
        try:
            r = self._post(URL, json=body, timeout=self.timeout,
                           headers={"X-Goog-Api-Key": self.key, "X-Goog-FieldMask": FIELDS})
        except requests.RequestException as exc:
            return {"verfuegbar": True, "gefunden": False, "fehler": f"Google nicht erreichbar: {exc}"}
        if r.status_code != 200:
            return {"verfuegbar": True, "gefunden": False, "fehler": f"Google antwortete mit HTTP {r.status_code}"}
        try:
            places = (r.json() or {}).get("places") or []
        except ValueError:
            places = []
        result = {"verfuegbar": True, "gefunden": False}
        if places:
            p = places[0]
            loc = p.get("location") or {}
            lat, lon = loc.get("latitude"), loc.get("longitude")
            weit = (zentrum and isinstance(lat, (int, float)) and isinstance(lon, (int, float))
                    and _km(zentrum, (lat, lon)) > MAX_ABSTAND_KM)
            if not weit:
                result = {
                    "verfuegbar": True, "gefunden": True, "quelle": "google", "place_id": p.get("id"),
                    "name": (p.get("displayName") or {}).get("text"), "adresse": p.get("formattedAddress"),
                    "lat": lat, "lon": lon, "maps_url": p.get("googleMapsUri"),
                    "telefon": p.get("nationalPhoneNumber"), "web": p.get("websiteUri"),
                    "oeffnungszeiten": (p.get("regularOpeningHours") or {}).get("weekdayDescriptions") or [],
                    "intervalle": intervalle((p.get("regularOpeningHours") or {}).get("periods")),
                    "jetzt_geoeffnet": (p.get("currentOpeningHours") or {}).get("openNow"),
                }
        with self._lock:
            if len(self._cache) >= 500:
                self._cache.pop(next(iter(self._cache)))
            self._cache[cache_key] = (self._clock(), result)
        return result


def intervalle(periods) -> list | None:
    """Google ``periods`` (Tag 0 = Sonntag) -> 7 Listen [von, bis] ab Montag; über Mitternacht wird am Tag gekappt."""
    if not isinstance(periods, list) or not periods:
        return None
    week = [[] for _ in range(7)]
    for per in periods:
        o, c = per.get("open") or {}, per.get("close")
        if "day" not in o:
            continue
        tag = (int(o["day"]) + 6) % 7
        von = f"{int(o.get('hour', 0)):02d}:{int(o.get('minute', 0)):02d}"
        if c is None:                       # durchgehend geöffnet
            return [[["00:00", "24:00"]] for _ in range(7)]
        bis = f"{int(c.get('hour', 0)):02d}:{int(c.get('minute', 0)):02d}"
        if int(c.get("day", o["day"])) != int(o["day"]):
            bis = "24:00"
        week[tag].append([von, bis])
    return week


def zentrum_von(geometry: dict | None) -> tuple[float, float] | None:
    """(lat, lon) eines Punkts bzw. Mittelpunkt des Umrechtecks einer Fläche (GeoJSON)."""
    if not isinstance(geometry, dict):
        return None
    coords = []

    def walk(c):
        if isinstance(c, (list, tuple)) and len(c) >= 2 and all(isinstance(v, (int, float)) for v in c[:2]):
            coords.append(c)
        elif isinstance(c, (list, tuple)):
            for x in c:
                walk(x)
    walk(geometry.get("coordinates"))
    if not coords:
        return None
    lons, lats = [c[0] for c in coords], [c[1] for c in coords]
    return ((min(lats) + max(lats)) / 2, (min(lons) + max(lons)) / 2)
