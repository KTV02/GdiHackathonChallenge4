"""Integrationstest über die ganze Kette mit ECHTEN Resolver-Snapshots.

Echt:    Finder, CLI, Geocoder-Antwortparser, interpret_match, Kategorie-Katalog und Radien des Resolvers,
         Zuständigkeitsdaten (Polygone und Standortpunkte) aus behoerden-resolver/data/snapshots.
Ersetzt: Nominatim-Netzwerk (Antwort im Nominatim-Format), Elasticsearch/PVOG (Matcher-Rohantwort im Format von
         match_authority) und PostGIS (SnapshotResolver mit Punkt-in-Polygon in reinem Python).
Übersprungen, wenn die Snapshots oder der Resolver-Katalog nicht erreichbar sind.
"""
import contextlib
import importlib.util
import io
import json
import unittest

from behoerden_finder import cli
from behoerden_finder.geocode import Geocoder
from behoerden_finder.matching import interpret_match
from behoerden_finder.render import kontakt

try:
    import snapshot_resolver as sr
    READY = importlib.util.find_spec("behoerden_resolver") is not None and all(
        sr.latest_snapshot(s) for s in ("by_finanzamt", "bb_polizei", "osm_locations"))
except ImportError:
    READY = False

from helpers import FakeHttp, FakeResponse, hit

RAW_MATCH = {"status": "vorschlag", "behoerdenkategorie": "finanzamt", "score": 0.0381, "top_kategorie": "finanzamt",
             "alternativen": [{"id": "buergeramt", "score": 0.01}],
             "leistungen": [{"id": "x", "titel": "Schenkung anzeigen", "leistungsschluessel": "99012345678901", "score": 0.3,
                             "behoerdenkategorien": ["finanzamt"]}]}


class RawMatcher:
    """Liefert eine Matcher-Rohantwort, die ECHTE interpret_match-Logik wertet sie aus."""
    def __init__(self, raw=RAW_MATCH, accept_ambiguous=False):
        self.raw, self.accept_ambiguous, self.calls = raw, accept_ambiguous, []

    def match(self, text):
        self.calls.append(text)
        return interpret_match(self.raw, accept_ambiguous=self.accept_ambiguous)


@unittest.skipUnless(READY, "Resolver-Snapshots oder behoerden_resolver nicht erreichbar")
class RealDataChainTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.resolver = sr.SnapshotResolver(
            area_sources=["by_finanzamt", "bb_polizei"],
            point_sources=["by_finanzamt_locations", "bb_polizei_locations", "osm_locations"])

    def run_cli(self, argv, nominatim=None, matcher=None):
        self.resolver.calls.clear()
        http = FakeHttp(FakeResponse(nominatim if nominatim is not None else []))

        def factory(args):
            finder = cli._make_finder(args)               # echte Zuordnung CLI-Argumente -> Optionen
            finder.resolve_fn = self.resolver
            finder.matcher = None if args.kategorie else (matcher or RawMatcher())
            finder.geocoder = None if args.lat is not None else Geocoder("https://nominatim.invalid", "test", http_get=http)
            return finder
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = cli.main(argv, finder_factory=factory)
        return code, out.getvalue(), err.getvalue(), http

    # --- Polygon-Treffer mit echten Kontaktdaten ----------------------------------------------
    def test_finanzamt_ingolstadt_text_and_json(self):
        nominatim = [hit(48.7665, 11.4258, "Esplanade 38, Ingolstadt, Bayern, 85049, Deutschland")]
        base = ["--anliegen", "Meine Oma hat mir 10.000 € geschenkt. Wo muss ich das melden?",
                "--adresse", "Esplanade 38, Ingolstadt"]
        code, out, _, http = self.run_cli(base + ["--json"], nominatim)
        data = json.loads(out)
        self.assertEqual(code, 0, out)
        self.assertEqual(data["status"], "zustaendige_behoerde")
        self.assertEqual((data["kategorie"]["id"], data["kategorie"]["label"], data["kategorie"]["quelle"]),
                         ("finanzamt", "Finanzamt", "matcher"))
        self.assertEqual(data["leistung"]["titel"], "Schenkung anzeigen")
        self.assertEqual(data["standort"]["quelle"], "nominatim")
        match = data["zustaendigkeit"]["matches"][0]
        self.assertEqual(match["authority_name"], "Finanzamt Ingolstadt")
        self.assertIsNone(match["geometry"])                     # Polygon wird nicht ausgegeben
        self.assertEqual(match["geometry_type"], "MultiPolygon")
        self.assertEqual(self.resolver.calls[0][0], "finanzamt")
        self.assertAlmostEqual(self.resolver.calls[0][1], 11.4258)  # lon zuerst ...
        self.assertAlmostEqual(self.resolver.calls[0][2], 48.7665)  # ... dann lat
        info = kontakt(match["properties"])                      # echte BY-Eigenschaften
        self.assertEqual(info["adresse"], "Esplanade 38, 85049 Ingolstadt")
        self.assertEqual(info["telefon"], "+49 (0)841 311-0")
        self.assertEqual(http.calls[0][1]["params"]["countrycodes"], "de")
        code, text, _, _ = self.run_cli(base, nominatim)
        for part in ("Finanzamt [finanzamt]", "Schenkung anzeigen", "Zuständige Behörde:", "Finanzamt Ingolstadt",
                     "Adresse: Esplanade 38, 85049 Ingolstadt", "Telefon: +49 (0)841 311-0", "Quelle: by_finanzamt"):
            self.assertIn(part, text)
        self.assertEqual(code, 0)

    def test_polizei_brandenburg_polygon_without_contact_data(self):
        code, out, _, _ = self.run_cli(["--kategorie", "polizei", "--lat", "52.9267", "--lon", "12.8019", "--json"])
        data = json.loads(out)
        self.assertEqual(code, 0)
        self.assertEqual(data["kategorie"]["quelle"], "vorgegeben")
        self.assertEqual(data["standort"]["quelle"], "vorgegeben")
        self.assertEqual(data["zustaendigkeit"]["matches"][0]["authority_name"], "Polizeiinspektion Ostprignitz-Ruppin")
        self.assertEqual(kontakt(data["zustaendigkeit"]["matches"][0]["properties"]), {})   # Quelle hat keine Anschrift
        self.assertNotIn("Adresse:", self.run_cli(["--kategorie", "polizei", "--lat", "52.9267", "--lon", "12.8019"])[1])

    # --- Standortersatz --------------------------------------------------------------------------
    def test_nearby_location_fallback_near_mainz(self):
        code, out, _, _ = self.run_cli(["--kategorie", "polizei", "--lat", "50.0", "--lon", "8.27", "--json"])
        data = json.loads(out)
        z = data["zustaendigkeit"]
        self.assertEqual((code, data["status"]), (0, "naechster_standort"))
        self.assertTrue(z["fallback_used"])
        self.assertEqual(z["matches"][0]["authority_name"], "Bundeskriminalamt II")
        self.assertAlmostEqual(z["matches"][0]["distance_m"], 6075, delta=60)
        self.assertEqual(z["matches"][0]["geometry"]["type"], "Point")          # Standortpunkt bleibt erhalten
        self.assertTrue(any("Nahegelegener Standort" in h for h in data["hinweise"]))
        text = self.run_cli(["--kategorie", "polizei", "--lat", "50.0", "--lon", "8.27"])[1]
        self.assertIn("nur Anlaufstellen", text)
        self.assertIn("ca. 6,1 km entfernt", text)

    def test_options_reach_the_resolver_logic(self):
        argv = ["--kategorie", "polizei", "--lat", "50.0", "--lon", "8.27", "--json"]
        code, out, _, _ = self.run_cli(argv + ["--no-point-fallback"])
        self.assertEqual((code, json.loads(out)["status"]), (2, "keine_abdeckung"))
        data = json.loads(self.run_cli(argv + ["--radius-km", "1"])[1])           # BKA liegt 6 km entfernt
        self.assertEqual(data["status"], "keine_abdeckung")
        data = json.loads(self.run_cli(["--kategorie", "finanzamt", "--lat", "50.0", "--lon", "8.27", "--json",
                                        "--point-limit", "1"])[1])
        self.assertEqual(len(data["zustaendigkeit"]["matches"]), 1)

    # --- Sonderfälle ----------------------------------------------------------------------------------
    def test_non_spatial_category(self):
        code, out, _, _ = self.run_cli(["--kategorie", "krankenkasse", "--lat", "50.0", "--lon", "8.27", "--json"])
        data = json.loads(out)
        self.assertEqual((code, data["status"]), (0, "ortsunabhaengig"))
        self.assertTrue(any("Einzelfall" in h for h in data["hinweise"]))

    def test_spatial_category_without_imported_data(self):
        # Im Stub sind nur by_finanzamt/bb_polizei als Flächen geladen: arbeitsagentur hat hier weder Fläche noch Punkt.
        code, out, _, _ = self.run_cli(["--kategorie", "arbeitsagentur", "--lat", "50.0", "--lon", "8.27", "--json"])
        data = json.loads(out)
        self.assertEqual((code, data["status"]), (2, "keine_abdeckung"))
        self.assertTrue(any("keine Daten in der lokalen Datenbank" in h for h in data["hinweise"]))
        self.assertIn("Standort:   lat 50.00000, lon 8.27000", self.run_cli(
            ["--kategorie", "arbeitsagentur", "--lat", "50.0", "--lon", "8.27"])[1])      # Koordinaten ohne Adresse in einer Zeile

    def test_context_category_without_areas_and_unknown_category(self):
        code, out, _, _ = self.run_cli(["--kategorie", "bauamt", "--lat", "50.0", "--lon", "8.27", "--json"])
        data = json.loads(out)
        self.assertEqual((code, data["status"]), (2, "kontext_erforderlich"))     # bauamt: Zuständigkeit je Einzelfall
        code, _, err, _ = self.run_cli(["--kategorie", "gibtsnicht", "--lat", "50.0", "--lon", "8.27"])
        self.assertEqual(code, 1)
        self.assertIn("Unbekannte oder mehrdeutige Kategorie", err)

    def test_matcher_ambiguity_stops_before_any_lookup(self):
        raw = {"status": "mehrdeutig", "behoerdenkategorie": None, "score": 0.05, "top_kategorie": "buergeramt",
               "alternativen": [{"id": "standesamt", "score": 0.049}],
               "leistungen": [{"titel": "Namensänderung", "leistungsschluessel": "99000000000003", "score": 0.4,
                               "behoerdenkategorien": ["buergeramt", "standesamt"]}]}
        code, out, _, http = self.run_cli(["--anliegen", "Namensänderung", "--adresse", "Esplanade 38, Ingolstadt", "--json"],
                                          matcher=RawMatcher(raw))
        data = json.loads(out)
        self.assertEqual((code, data["status"]), (2, "kategorie_mehrdeutig"))
        self.assertEqual([c["id"] for c in data["kategorie"]["kandidaten"]], ["buergeramt", "standesamt"])
        self.assertEqual((http.calls, self.resolver.calls), ([], []))

    def test_address_not_found_and_ambiguous_address(self):
        code, out, _, _ = self.run_cli(["--anliegen", "Geschenk", "--adresse", "Nirgendwostraße 99"], nominatim=[])
        self.assertEqual(code, 2)
        self.assertIn("Adresse nicht gefunden", out)
        self.assertIn("Finanzamt [finanzamt]", out)                       # Kategorie bleibt sichtbar
        far = [hit(48.7665, 11.4258, "Hauptstraße 1, Ingolstadt"), hit(50.0, 8.27, "Hauptstraße 1, Mainz")]
        code, out, _, _ = self.run_cli(["--anliegen", "Geschenk", "--adresse", "Hauptstraße 1", "--json"], nominatim=far)
        data = json.loads(out)
        self.assertEqual((code, data["standort"]["mehrdeutig"]), (0, True))
        self.assertTrue(any("nicht eindeutig" in h for h in data["hinweise"]))


if __name__ == "__main__":
    unittest.main()
