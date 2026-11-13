import json
import unittest

from behoerden_finder.geocode import GeocodingError, GeocodeResult, GeoHit
from behoerden_finder.pipeline import Finder, ResolveOptions
from helpers import FakeGeocoder, FakeMatcher, nearby_result, outcome, polygon_result


def geo(lat=50.0, lon=8.27, precision="hausnummer", ambiguous=False):
    hit = GeoHit(lat=lat, lon=lon, display_name="Musterstraße 12, Mainz", precision=precision)
    alt = (GeoHit(48.1, 11.6, "Musterstraße 12, München", "hausnummer"),) if ambiguous else ()
    return GeocodeResult("Musterstraße 12", hit, alt, ambiguous)


class Recorder:
    def __init__(self, result=None, error=None):
        self.result, self.error, self.calls = result, error, []

    def __call__(self, category, lon, lat, **options):
        self.calls.append((category, lon, lat, options))
        if self.error:
            raise self.error
        return self.result


class FinderTest(unittest.TestCase):
    def finder(self, matcher=None, geocoder=None, resolver=None, **kw):
        return Finder(matcher=matcher or FakeMatcher(outcome()), geocoder=geocoder or FakeGeocoder(geo()),
                      resolve_fn=resolver or Recorder(polygon_result()), **kw)

    def test_happy_path_passes_lon_and_lat_in_the_right_order(self):
        resolver = Recorder(polygon_result())
        f = self.finder(resolver=resolver)
        result = f.find("Meine Oma hat mir Geld geschenkt", "Musterstraße 12, Mainz")
        category, lon, lat, options = resolver.calls[0]
        self.assertEqual((category, lon, lat), ("finanzamt", 8.27, 50.0))
        self.assertEqual(options["backend"], "auto")
        self.assertEqual(result["status"], "zustaendige_behoerde")
        self.assertEqual(result["leistung"]["titel"], "Schenkung anzeigen")
        self.assertEqual(result["kategorie"]["id"], "finanzamt")
        self.assertEqual(result["standort"]["quelle"], "nominatim")
        self.assertEqual(result["zustaendigkeit"]["matches"][0]["authority_name"], "Finanzamt Mainz-Mitte")
        self.assertEqual(result["hinweise"], [])
        json.dumps(result)  # serialisierbar

    def test_polygons_are_stripped_unless_requested(self):
        r = self.finder().find("Anliegen", "Adresse 1")
        m = r["zustaendigkeit"]["matches"][0]
        self.assertIsNone(m["geometry"])
        self.assertEqual(m["geometry_type"], "MultiPolygon")
        r = self.finder(include_geometry=True).find("Anliegen", "Adresse 1")
        self.assertEqual(r["zustaendigkeit"]["matches"][0]["geometry"]["type"], "MultiPolygon")

    def test_points_are_kept(self):
        r = self.finder(matcher=FakeMatcher(outcome("polizei")), resolver=Recorder(nearby_result())).find("Anliegen", "Adresse 1")
        self.assertEqual(r["status"], "naechster_standort")
        self.assertEqual(r["zustaendigkeit"]["matches"][0]["geometry"]["coordinates"], [8.27, 50.0])
        self.assertTrue(any("Nahegelegener Standort" in h for h in r["hinweise"]))

    def test_resolver_options_are_forwarded(self):
        resolver = Recorder(polygon_result())
        opts = ResolveOptions(backend="local", point_fallback=False, radius_km=12.5, point_limit=1, dsn="postgresql://x")
        self.finder(resolver=resolver, options=opts).find("Anliegen", "Adresse 1")
        self.assertEqual(resolver.calls[0][3], {"dsn": "postgresql://x", "backend": "local", "point_fallback": False,
                                                "radius_km": 12.5, "point_limit": 1})

    def test_ambiguous_category_stops_before_geocoding(self):
        geocoder, resolver = FakeGeocoder(geo()), Recorder(polygon_result())
        amb = outcome(None, "mehrdeutig", ambiguous=True, candidates=[{"id": "buergeramt", "score": .05}, {"id": "standesamt", "score": .049}])
        r = self.finder(matcher=FakeMatcher(amb), geocoder=geocoder, resolver=resolver).find("Namensänderung", "Adresse 1")
        self.assertEqual(r["status"], "kategorie_mehrdeutig")
        self.assertEqual([c["id"] for c in r["kategorie"]["kandidaten"]], ["buergeramt", "standesamt"])
        self.assertEqual((geocoder.calls, resolver.calls), ([], []))       # keine Adresse an Nominatim, kein Resolver

    def test_no_category_stops(self):
        r = self.finder(matcher=FakeMatcher(outcome(None, "keine_zuordnung")), geocoder=FakeGeocoder(geo())).find("Irgendwas", "Adresse 1")
        self.assertEqual(r["status"], "keine_kategorie")
        self.assertIn("keine_zuordnung", r["hinweise"][0])

    def test_geocoding_failure_keeps_recognised_category(self):
        resolver = Recorder(polygon_result())
        r = self.finder(geocoder=FakeGeocoder(error=GeocodingError("Keine Koordinate gefunden")), resolver=resolver).find("Anliegen", "Nirgendwo 99")
        self.assertEqual(r["status"], "adresse_nicht_gefunden")
        self.assertEqual(r["kategorie"]["id"], "finanzamt")
        self.assertIn("Keine Koordinate", r["fehler"])
        self.assertEqual(resolver.calls, [])

    def test_manual_category_skips_matcher(self):
        matcher = FakeMatcher(outcome())
        r = self.finder(matcher=matcher).find(None, "Adresse 1", kategorie="finanzamt")
        self.assertEqual(matcher.calls, [])
        self.assertEqual(r["kategorie"]["quelle"], "vorgegeben")
        self.assertEqual(r["status"], "zustaendige_behoerde")

    def test_coordinates_skip_geocoder(self):
        geocoder, resolver = FakeGeocoder(geo()), Recorder(polygon_result())
        r = self.finder(geocoder=geocoder, resolver=resolver).find("Anliegen", None, coords=(49.488, 8.466))
        self.assertEqual(geocoder.calls, [])
        self.assertEqual(resolver.calls[0][1:3], (8.466, 49.488))     # (lon, lat)
        self.assertEqual(r["standort"]["quelle"], "vorgegeben")

    def test_imprecise_and_ambiguous_address_hints(self):
        r = self.finder(geocoder=FakeGeocoder(geo(precision="ort", ambiguous=True))).find("Anliegen", "Hauptstraße 1")
        text = " ".join(r["hinweise"])
        self.assertIn("nicht eindeutig", text)
        self.assertIn("nicht hausgenau", text)
        self.assertEqual(len(r["standort"]["alternativen"]), 1)

    def test_resolver_errors_become_a_status(self):
        class OperationalError(Exception):
            pass
        r = self.finder(resolver=Recorder(error=OperationalError("connection refused"))).find("Anliegen", "Adresse 1")
        self.assertEqual(r["status"], "resolver_fehler")
        self.assertIn("OperationalError", r["fehler"])
        self.assertTrue(any("docker compose up -d db" in h for h in r["hinweise"]))

    def test_non_spatial_and_no_coverage_and_context(self):
        r = self.finder(resolver=Recorder({"status": "non_spatial", "matches": [], "requires_case_context": True,
                                           "required_context": "Mitgliedschaft maßgeblich"})).find("Anliegen", "Adresse 1")
        self.assertEqual(r["status"], "ortsunabhaengig")
        self.assertTrue(any("Mitgliedschaft" in h for h in r["hinweise"]))
        r = self.finder(resolver=Recorder({"status": "no_coverage_at_point", "matches": []})).find("Anliegen", "Adresse 1")
        self.assertEqual(r["status"], "keine_abdeckung")
        r = self.finder(resolver=Recorder({"status": "context_required", "matches": []})).find("Anliegen", "Adresse 1")
        self.assertEqual(r["status"], "kontext_erforderlich")

    def test_multiple_polygon_matches(self):
        two = polygon_result()
        two["status"] = "ambiguous"
        two["matches"].append({**two["matches"][0], "authority_name": "Finanzamt Außenstelle"})
        self.assertEqual(self.finder(resolver=Recorder(two)).find("Anliegen", "Adresse 1")["status"], "mehrere_kandidaten")

    def test_input_validation(self):
        f = self.finder()
        with self.assertRaises(ValueError):
            f.find(None, "Adresse 1")                      # weder Anliegen noch Kategorie
        with self.assertRaises(ValueError):
            f.find("Anliegen", None)                       # weder Adresse noch Koordinaten
        with self.assertRaises(ValueError):
            f.find("Anliegen", None, coords=(95.0, 8.0))   # ungültige Breite
        with self.assertRaises(ValueError):
            f.find(None, "Adresse 1", kategorie="gibtsnicht")


if __name__ == "__main__":
    unittest.main()
