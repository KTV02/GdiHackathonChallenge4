"""Gemeinsame Test-Doubles."""
from behoerden_finder.matching import MatchOutcome


class FakeResponse:
    def __init__(self, payload, status_code=200):
        self._payload, self.status_code = payload, status_code

    def json(self):
        if isinstance(self._payload, Exception):
            raise self._payload
        return self._payload


class FakeHttp:
    def __init__(self, *responses):
        self.responses, self.calls = list(responses), []

    def __call__(self, url, **kwargs):
        self.calls.append((url, kwargs))
        item = self.responses.pop(0) if len(self.responses) > 1 else self.responses[0]
        if isinstance(item, Exception):
            raise item
        return item


def hit(lat, lon, name="Musterstraße 12, Mainz", house="12", road="Musterstraße"):
    address = {k: v for k, v in {"house_number": house, "road": road, "city": "Mainz"}.items() if v}
    return {"lat": str(lat), "lon": str(lon), "display_name": name, "address": address,
            "osm_type": "node", "osm_id": 42, "importance": 0.4}


class FakeMatcher:
    def __init__(self, outcome):
        self.outcome, self.calls = outcome, []

    def match(self, text):
        self.calls.append(text)
        return self.outcome


class FakeGeocoder:
    def __init__(self, result=None, error=None):
        self.result, self.error, self.calls = result, error, []

    def locate(self, query):
        self.calls.append(query)
        if self.error:
            raise self.error
        return self.result


def outcome(category="finanzamt", status="vorschlag", ambiguous=False, candidates=None):
    return MatchOutcome(status=status, category=category, score=0.0381 if category or ambiguous else None,
                        service={"titel": "Schenkung anzeigen", "leistungsschluessel": "99012345678901",
                                 "score": 0.5} if status != "keine_zuordnung" else None,
                        candidates=candidates or ([{"id": category, "score": 0.0381}] if category else []),
                        ambiguous=ambiguous, raw={"status": status})


def polygon_result(category="finanzamt"):
    return {"category": category, "point": {"lon": 8.27, "lat": 50.0}, "backend": "local",
            "status": "candidate", "fallback_used": False, "requires_case_context": False,
            "matches": [{"authority_name": "Finanzamt Mainz-Mitte", "source_id": "gemfa_vg25",
                         "evidence_url": "https://example.invalid/beleg",
                         "properties": {"anschriftStrasse": "Schillerstraße", "hausnummer": "13",
                                        "anschriftPlz": "55116", "anschriftOrt": "Mainz",
                                        "telefon": "06131 000", "email": "poststelle@fa.example",
                                        "homepage": "https://fa.example"},
                         "geometry": {"type": "MultiPolygon", "coordinates": [[[[0, 0], [1, 0], [1, 1], [0, 0]]]]}}]}


def nearby_result():
    return {"category": "polizei", "status": "nearby_candidates", "fallback_used": True,
            "requires_case_context": False, "warning": "Nahegelegener Standort. Zuständigkeit nicht belegt.",
            "matches": [{"authority_name": "Polizeirevier A", "source_id": "osm_locations", "properties": {},
                         "geometry": {"type": "Point", "coordinates": [8.27, 50.0]}, "distance_m": 408.4}]}
