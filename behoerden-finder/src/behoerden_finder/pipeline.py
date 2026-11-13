"""Pipeline: Anliegen + Adresse -> zuständige Behörde.

    Anliegen  --Matcher-->  Leistung + Kategorie (eine von 36)
    Adresse   --Geocoder->  lat / lon
    Kategorie + lat/lon --Resolver (PostGIS / WFS)--> örtlich zuständige Behörde

Jeder Schritt ist austauschbar (``matcher``, ``geocoder``, ``resolve_fn``), damit dieselbe
Klasse später hinter einer Website laufen und in Tests mit Test-Doubles benutzt werden kann.
"""
from __future__ import annotations

import copy
import math
from dataclasses import dataclass

from .geocode import GeocodingError

# Statuswerte des Gesamtergebnisses
FOUND = ("zustaendige_behoerde", "mehrere_kandidaten", "naechster_standort", "ortsunabhaengig")


@dataclass
class ResolveOptions:
    backend: str = "auto"            # auto | local | wfs
    point_fallback: bool = True
    radius_km: float | None = None
    point_limit: int = 3
    dsn: str | None = None


def _default_resolve(category, lon, lat, **options):
    try:
        from behoerden_resolver.resolver import resolve
    except ImportError as exc:  # fehlende Pakete wie psycopg/shapely/pyproj sind hier ebenfalls ein ImportError
        raise RuntimeError(
            "behoerden-resolver ist in dieser Python-Umgebung nicht (vollständig) installiert: "
            f"{exc}. Installation: pip install -e '../behoerden-resolver'"
        ) from exc
    return resolve(category, lon, lat, **options)


def _category_info(value):
    try:
        from behoerden_resolver.catalog import category
    except ImportError:
        return {"id": value, "label": value}
    c = category(value)  # ValueError bei unbekannter oder mehrdeutiger Kategorie
    return {"id": c["id"], "label": c["label"]}


def _strip_polygons(zustaendigkeit: dict) -> dict:
    """Große Flächengeometrien entfernen, Standortpunkte (Koordinaten) bleiben erhalten."""
    out = copy.deepcopy(zustaendigkeit)
    for match in out.get("matches") or []:
        geom = match.get("geometry")
        if isinstance(geom, dict) and geom.get("type") not in (None, "Point"):
            match["geometry_type"] = geom["type"]
            match["geometry"] = None
    return out


def _final_status(z: dict) -> str:
    matches = z.get("matches") or []
    if matches:
        if z.get("fallback_used"):
            return "naechster_standort"
        return "zustaendige_behoerde" if len(matches) == 1 else "mehrere_kandidaten"
    if z.get("status") == "non_spatial":
        return "ortsunabhaengig"
    if z.get("status") == "context_required":
        return "kontext_erforderlich"
    return "keine_abdeckung"


class Finder:
    def __init__(self, *, matcher=None, geocoder=None, resolve_fn=None, options: ResolveOptions | None = None,
                 include_geometry: bool = False, include_details: bool = False):
        self.matcher = matcher
        self.geocoder = geocoder
        self.resolve_fn = resolve_fn or _default_resolve
        self.options = options or ResolveOptions()
        self.include_geometry = include_geometry
        self.include_details = include_details

    # --- Schritte -----------------------------------------------------------------
    def _categorise(self, anliegen, kategorie, result):
        if kategorie:
            info = _category_info(kategorie)
            result["kategorie"] = {**info, "quelle": "vorgegeben", "status": "vorgegeben", "score": None,
                                   "kandidaten": []}
            return info["id"]
        if self.matcher is None:
            raise ValueError("Kein Matcher konfiguriert; --kategorie angeben.")
        outcome = self.matcher.match(anliegen)
        result["leistung"] = outcome.service
        if self.include_details:
            result["matcher_details"] = outcome.raw
        label = None
        if outcome.category:
            label = _category_info(outcome.category)["label"]
        result["kategorie"] = {"id": outcome.category, "label": label, "quelle": "matcher",
                               "status": outcome.status, "score": outcome.score,
                               "kandidaten": outcome.candidates}
        if outcome.category:
            return outcome.category
        if outcome.ambiguous:
            result["status"] = "kategorie_mehrdeutig"
            result["hinweise"].append(
                "Das Anliegen passt zu mehreren Behördenarten; es wurde keine gewählt. "
                "Anliegen genauer formulieren oder mit --kategorie vorgeben (--bei-mehrdeutig beste nimmt die erste).")
        else:
            result["status"] = "keine_kategorie"
            result["hinweise"].append(
                "Für dieses Anliegen konnte keine Behördenkategorie belegt werden "
                f"(Matcher-Status: {outcome.status}).")
        return None

    def _locate(self, adresse, coords, result):
        if coords is not None:
            lat, lon = coords
            if not (math.isfinite(lat) and math.isfinite(lon) and -90 <= lat <= 90 and -180 <= lon <= 180):
                raise ValueError("Koordinaten müssen endliche WGS84-Werte sein (lat, lon).")
            result["standort"] = {"lat": lat, "lon": lon, "anzeige_name": None, "genauigkeit": None,
                                  "quelle": "vorgegeben", "mehrdeutig": False, "alternativen": []}
            return lat, lon
        if self.geocoder is None:
            raise ValueError("Kein Geocoder konfiguriert; --lat und --lon angeben.")
        try:
            geo = self.geocoder.locate(adresse)
        except GeocodingError as exc:
            result["status"] = "adresse_nicht_gefunden"
            result["fehler"] = str(exc)
            return None
        best = geo.best
        result["standort"] = {"lat": best.lat, "lon": best.lon, "anzeige_name": best.display_name,
                              "genauigkeit": best.precision, "quelle": "nominatim",
                              "mehrdeutig": geo.ambiguous,
                              "alternativen": [a.as_dict() for a in geo.alternatives]}
        if geo.ambiguous:
            result["hinweise"].append(
                "Die Adresse ist nicht eindeutig (Treffer an weit entfernten Orten); der erste Treffer wurde "
                "verwendet. Bitte mit Ort/PLZ präzisieren.")
        if best.precision != "hausnummer":
            result["hinweise"].append(
                f"Die Koordinate ist nicht hausgenau (Genauigkeit: {best.precision}). "
                "Nahe an Zuständigkeitsgrenzen kann das Ergebnis abweichen.")
        return best.lat, best.lon

    def _resolve(self, category, lat, lon, result):
        o = self.options
        try:
            z = self.resolve_fn(category, lon, lat, dsn=o.dsn, backend=o.backend,
                                point_fallback=o.point_fallback, radius_km=o.radius_km,
                                point_limit=o.point_limit)
        except Exception as exc:  # DB nicht erreichbar, WFS-Ausfall, Importfehler ...
            result["status"] = "resolver_fehler"
            result["fehler"] = f"{type(exc).__name__}: {exc}"
            name = type(exc).__name__
            if "OperationalError" in name:
                result["hinweise"].append(
                    "Datenbank nicht erreichbar? Im Ordner behoerden-resolver: 'docker compose up -d db'; "
                    "DATABASE_URL bzw. --dsn prüfen. Alternativ --backend wfs.")
            elif name in ("UndefinedTable", "InvalidSchemaName"):
                result["hinweise"].append(
                    "Die Datenbank ist noch leer. Im Ordner behoerden-resolver: "
                    "'docker compose run --rm cli load' (oder init-db + sync).")
            return
        result["status"] = _final_status(z)
        result["zustaendigkeit"] = z if self.include_geometry else _strip_polygons(z)
        if z.get("requires_case_context"):
            result["hinweise"].append(
                "Die Zuständigkeit hängt vom Einzelfall ab: " + str(z.get("required_context") or
                                                                    z.get("limitations") or "").strip())
        if z.get("warning"):
            result["hinweise"].append(str(z["warning"]))

    # --- Hauptaufruf ---------------------------------------------------------------
    def find(self, anliegen: str | None = None, adresse: str | None = None, *, kategorie: str | None = None,
             coords: tuple[float, float] | None = None) -> dict:
        anliegen = (anliegen or "").strip() or None
        adresse = (adresse or "").strip() or None
        if not kategorie and not anliegen:
            raise ValueError("Bitte ein Anliegen oder eine Kategorie angeben.")
        if coords is None and not adresse:
            raise ValueError("Bitte eine Adresse oder lat/lon angeben.")
        result = {"anliegen": anliegen, "adresse": adresse, "leistung": None, "kategorie": None,
                  "standort": None, "zustaendigkeit": None, "status": None, "hinweise": [], "fehler": None}
        category = self._categorise(anliegen, kategorie, result)
        if category is None:
            return result  # ohne Kategorie keine Adresse an den Geocoder senden
        point = self._locate(adresse, coords, result)
        if point is None:
            return result
        self._resolve(category, point[0], point[1], result)
        return result
