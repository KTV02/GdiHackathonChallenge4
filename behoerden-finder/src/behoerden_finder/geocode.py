"""Adresse -> WGS84-Koordinate über Nominatim (OpenStreetMap).

Der öffentliche Nominatim-Dienst verlangt einen eigenen User-Agent mit Kontakt
und höchstens eine Anfrage pro Sekunde. Beides ist hier eingebaut. Für den
späteren Webbetrieb wird ``NOMINATIM_URL`` auf eine eigene Instanz gesetzt.
"""
from __future__ import annotations

import math
import os
import re
import threading
import time
from dataclasses import asdict, dataclass

import requests

DEFAULT_URL = "https://nominatim.openstreetmap.org/search"
DEFAULT_CONTACT = "https://github.com/KTV02/GdiHackathonChallenge4"
# Weitere Treffer weiter als das vom besten Treffer entfernt gelten als echte Alternative bzw. Mehrdeutigkeit.
ALTERNATIVE_MIN_KM = 0.3
AMBIGUOUS_MIN_KM = 2.0


class GeocodingError(RuntimeError):
    """Die Adresse konnte nicht in eine Koordinate umgewandelt werden."""


@dataclass(frozen=True)
class GeoHit:
    lat: float
    lon: float
    display_name: str
    precision: str  # 'hausnummer' | 'strasse' | 'ort'
    importance: float | None = None
    osm_type: str | None = None
    osm_id: int | None = None

    def as_dict(self) -> dict:
        return asdict(self)


@dataclass(frozen=True)
class GeocodeResult:
    query: str
    best: GeoHit
    alternatives: tuple[GeoHit, ...] = ()
    ambiguous: bool = False


def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * 6371.0088 * math.asin(math.sqrt(a))


def _parse_hit(item: dict) -> GeoHit | None:
    try:
        lat, lon = float(item["lat"]), float(item["lon"])
    except (KeyError, TypeError, ValueError):
        return None
    if not (math.isfinite(lat) and math.isfinite(lon) and -90 <= lat <= 90 and -180 <= lon <= 180):
        return None
    address = item.get("address") or {}
    if address.get("house_number"):
        precision = "hausnummer"
    elif address.get("road") or item.get("category") == "highway":
        precision = "strasse"
    else:
        precision = "ort"
    osm_id = item.get("osm_id")
    return GeoHit(
        lat=lat,
        lon=lon,
        display_name=str(item.get("display_name") or ""),
        precision=precision,
        importance=float(item["importance"]) if isinstance(item.get("importance"), (int, float)) else None,
        osm_type=item.get("osm_type"),
        osm_id=int(osm_id) if isinstance(osm_id, (int, str)) and str(osm_id).isdigit() else None,
    )


class Geocoder:
    def __init__(self, url: str | None = None, contact: str | None = None, *, http_get=None,
                 min_interval: float = 1.1, timeout: float = 12.0, clock=time.monotonic, sleep=time.sleep):
        self.url = url or os.environ.get("NOMINATIM_URL", DEFAULT_URL)
        self.contact = contact or os.environ.get("GEOCODER_CONTACT", DEFAULT_CONTACT)
        self._get = http_get or requests.get
        self.min_interval = min_interval
        self.timeout = timeout
        self._clock, self._sleep = clock, sleep
        self._lock = threading.Lock()
        self._last_request: float | None = None
        self._cache: dict[str, list[GeoHit]] = {}

    @staticmethod
    def _normalise(query: str) -> str:
        return re.sub(r"\s+", " ", query).strip()

    def _throttle(self) -> None:
        if self._last_request is not None:
            wait = self.min_interval - (self._clock() - self._last_request)
            if wait > 0:
                self._sleep(wait)

    def search(self, query: str, limit: int = 5) -> list[GeoHit]:
        query = self._normalise(query or "")
        if not 3 <= len(query) <= 200:
            raise GeocodingError("Die Adresse muss zwischen 3 und 200 Zeichen lang sein.")
        key = f"{query.casefold()}|{limit}"
        with self._lock:
            if key in self._cache:
                return list(self._cache[key])
            self._throttle()
            try:
                response = self._get(
                    self.url,
                    params={"q": query, "format": "jsonv2", "countrycodes": "de", "limit": limit,
                            "addressdetails": 1},
                    headers={"User-Agent": f"Behoerdenfinder-Hackathon/0.1 ({self.contact})",
                             "Accept-Language": "de"},
                    timeout=self.timeout,
                )
            except requests.RequestException as exc:
                raise GeocodingError(f"Geocoder nicht erreichbar: {exc}") from exc
            finally:
                self._last_request = self._clock()
            if response.status_code == 429:
                raise GeocodingError("Geocoder meldet zu viele Anfragen (HTTP 429). Bitte kurz warten.")
            if response.status_code != 200:
                raise GeocodingError(f"Geocoder antwortete mit HTTP {response.status_code}.")
            try:
                items = response.json()
            except ValueError as exc:
                raise GeocodingError("Geocoder lieferte keine gültige JSON-Antwort.") from exc
            if not isinstance(items, list):
                raise GeocodingError("Geocoder lieferte ein unerwartetes Antwortformat.")
            hits = [h for h in (_parse_hit(i) for i in items if isinstance(i, dict)) if h is not None]
            if len(self._cache) >= 100:
                self._cache.pop(next(iter(self._cache)))
            self._cache[key] = hits
            return list(hits)

    def locate(self, query: str) -> GeocodeResult:
        """Bester Treffer; weit entfernte weitere Treffer werden als Mehrdeutigkeit markiert."""
        hits = self.search(query)
        if not hits:
            raise GeocodingError(f"Keine Koordinate für die Adresse gefunden: {self._normalise(query)}")
        best = hits[0]
        distances = [(h, haversine_km(best.lat, best.lon, h.lat, h.lon)) for h in hits[1:]]
        alternatives = tuple(h for h, km in distances if km >= ALTERNATIVE_MIN_KM)
        ambiguous = any(km >= AMBIGUOUS_MIN_KM for _, km in distances)
        return GeocodeResult(query=self._normalise(query), best=best, alternatives=alternatives, ambiguous=ambiguous)
