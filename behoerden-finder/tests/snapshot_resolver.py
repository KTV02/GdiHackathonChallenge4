"""PostGIS-Ersatz für Integrationstests: fragt die ECHTEN Snapshots aus behoerden-resolver/data/snapshots ab.

Die Antwort ist wie in behoerden_resolver.db.resolve / .nearby / resolver.resolve aufgebaut
(Schlüssel authority_name, properties, geometry, distance_m, status, fallback_used ...).
Punkt-in-Polygon läuft in reinem Python (Ray Casting inkl. Löcher), Entfernungen per Haversine.
Kategorien, Auflösungsmodi und Radien kommen aus dem ECHTEN Resolver-Katalog bzw. fallback.json.
"""
from __future__ import annotations

import glob
import gzip
import json
import math
import os
from pathlib import Path

from behoerden_resolver.catalog import category, resource

HERE = Path(__file__).resolve().parent
SNAPSHOTS = Path(os.environ.get("RESOLVER_DATA_DIR", HERE.parent.parent / "behoerden-resolver" / "data" / "snapshots"))


def latest_snapshot(source_id):
    found = sorted(glob.glob(str(SNAPSHOTS / f"{source_id}--*")))
    return Path(found[-1]) if found else None


def load_records(source_id):
    snap = latest_snapshot(source_id)
    if snap is None:
        raise FileNotFoundError(source_id)
    with gzip.open(snap / "records.jsonl.gz", "rt", encoding="utf-8") as f:
        return [json.loads(line) for line in f]


def _in_ring(x, y, ring):
    inside = False
    j = len(ring) - 1
    for i in range(len(ring)):
        xi, yi, xj, yj = ring[i][0], ring[i][1], ring[j][0], ring[j][1]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            inside = not inside
        j = i
    return inside


def covers(geometry, lon, lat):
    polygons = [geometry["coordinates"]] if geometry["type"] == "Polygon" else geometry["coordinates"]
    for rings in polygons:
        if _in_ring(lon, lat, rings[0]) and not any(_in_ring(lon, lat, hole) for hole in rings[1:]):
            return True
    return False


def _bbox(geometry):
    xs, ys = [], []

    def walk(c):
        if isinstance(c[0], (int, float)):
            xs.append(c[0])
            ys.append(c[1])
        else:
            for item in c:
                walk(item)
    walk(geometry["coordinates"])
    return min(xs), min(ys), max(xs), max(ys)


def distance_m(lat1, lon1, lat2, lon2):
    p1, p2 = math.radians(lat1), math.radians(lat2)
    h = math.sin((p2 - p1) / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(math.radians(lon2 - lon1) / 2) ** 2
    return 2 * 6371008.8 * math.asin(math.sqrt(h))


class SnapshotResolver:
    """Aufrufbar wie behoerden_resolver.resolver.resolve(value, lon, lat, dsn=, backend=, ...)."""

    def __init__(self, area_sources, point_sources):
        self.areas = [(r, _bbox(r["geometry"])) for s in area_sources for r in load_records(s)]
        self.points = [r for s in point_sources for r in load_records(s)]
        self.calls = []

    def __call__(self, value, lon, lat, dsn=None, backend="auto", point_fallback=True, radius_km=None, point_limit=3, **_):
        self.calls.append((value, lon, lat))
        c = category(value)  # echter Katalog; unbekannte Kategorie -> ValueError wie im echten Resolver
        lon, lat = float(lon), float(lat)
        result = {"category": c["id"], "point": {"lon": lon, "lat": lat}, "limitations": c["limitations"], "matches": [],
                  "national_coverage_guaranteed": False}
        needs_context = c["resolution_mode"] in ("non_spatial", "context_required")
        if needs_context:
            result["required_context"] = c["limitations"]
        if c["resolution_mode"] == "non_spatial":
            return {**result, "status": "non_spatial", "backend": "local", "requested_backend": backend,
                    "requires_case_context": True, "data_status": "no_geometry_match", "fallback_used": False}
        pool = [(r, b) for r, b in self.areas if r["category"] == c["id"]]
        matches = [r for r, (x0, y0, x1, y1) in pool if x0 <= lon <= x1 and y0 <= lat <= y1 and covers(r["geometry"], lon, lat)]
        status = "candidate" if len(matches) == 1 else "ambiguous" if matches else "no_coverage_at_point" if pool else "no_local_data"
        result.update(status="context_required" if needs_context else status, area_search_status=status,
                      backend="local", requested_backend=backend, requires_case_context=needs_context,
                      data_status="no_geometry_match")
        if matches:
            rows = [{k: v for k, v in r.items() if k in ("authority_name", "category", "source_id", "source_key", "scope",
                                                         "source_date", "evidence_url", "geometry_repaired", "properties",
                                                         "name_rule", "geometry")} | {"kind": "authority", "match_type": "polygon_cover",
                                                                                   "territorial_match_verified": True,
                                                                                   "case_competence_verified": False}
                    for r in sorted(matches, key=lambda r: (r["authority_name"], r["source_id"], r["source_key"]))]
            return {**result, "matches": rows, "fallback_used": False, "data_status": "polygon_match"}
        radii = json.loads(resource("fallback.json").read_text(encoding="utf-8"))
        radius = float(radius_km) if radius_km is not None else radii["category_radius_km"].get(c["id"], radii["default_radius_km"])
        near = []
        if point_fallback:
            for r in self.points:
                if r["category"] != c["id"]:
                    continue
                px, py = r["geometry"]["coordinates"][:2]
                d = distance_m(lat, lon, py, px)
                if d <= radius * 1000:
                    near.append((d, r))
        near.sort(key=lambda t: (t[0], t[1]["authority_name"], t[1]["source_id"], t[1]["source_key"]))
        if not near:
            return {**result, "fallback_used": False}
        rows = [{k: v for k, v in r.items() if k in ("authority_name", "category", "source_id", "source_key", "scope",
                                                     "source_date", "evidence_url", "properties", "name_rule", "geometry")}
                | {"distance_m": d, "kind": "authority_point", "match_type": "nearest_point_fallback",
                   "geometry_completeness": "location_only_no_jurisdiction_polygon", "jurisdiction_verified": False}
                for d, r in near[:point_limit]]
        return {**result, "status": "nearby_candidate" if len(rows) == 1 else "nearby_candidates", "matches": rows,
                "backend": "local_point_fallback", "fallback_used": True, "data_status": "nearby_location_match",
                "area_search_status": status, "point_search": {"radius_km": radius, "count": len(rows), "limit": point_limit},
                "warning": "Nahegelegener Standort. Zuständigkeit nicht durch eine Fläche belegt; Nähe begründet keine fachliche oder persönliche Zuständigkeit."}
