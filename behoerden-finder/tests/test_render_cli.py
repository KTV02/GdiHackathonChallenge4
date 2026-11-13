import contextlib
import io
import json
import unittest
from unittest import mock

from behoerden_finder import cli
from behoerden_finder.geocode import GeocodeResult, GeoHit
from behoerden_finder.pipeline import Finder
from behoerden_finder.render import kontakt, render_text
from helpers import FakeGeocoder, FakeMatcher, nearby_result, outcome, polygon_result


def finder(resolve_result, matcher=None):
    geo = GeocodeResult("Musterstraße 12", GeoHit(50.0, 8.27, "Musterstraße 12, Mainz", "hausnummer"))
    return Finder(matcher=matcher or FakeMatcher(outcome()), geocoder=FakeGeocoder(geo),
                  resolve_fn=lambda *a, **k: resolve_result)


def run(argv, factory):
    out, err = io.StringIO(), io.StringIO()
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err), mock.patch("sys.stdin", io.StringIO("")):
        code = cli.main(argv, finder_factory=factory)
    return code, out.getvalue(), err.getvalue()


class RenderTest(unittest.TestCase):
    def test_kontakt_aliases(self):
        self.assertEqual(kontakt({"anschriftStrasse": "Schillerstraße", "hausnummer": "13", "anschriftPlz": "55116",
                                  "anschriftOrt": "Mainz", "telefon": " 06131 ", "homepage": "https://x.invalid"}),
                         {"adresse": "Schillerstraße 13, 55116 Mainz", "telefon": "06131", "web": "https://x.invalid"})
        self.assertEqual(kontakt({"strasse": "Hauptstr. 1", "hausnummer": "1", "plz": "10115", "ort": "Berlin", "link": "u"})["adresse"],
                         "Hauptstr. 1, 10115 Berlin")      # Hausnummer nicht doppelt
        self.assertEqual(kontakt(None), {})
        self.assertEqual(kontakt({"telefon": "", "email": None}), {})

    def test_text_for_found_authority(self):
        result = finder(polygon_result()).find("Meine Oma hat mir Geld geschenkt", "Musterstraße 12, Mainz")
        text = render_text(result)
        for part in ("Schenkung anzeigen", "Finanzamt [finanzamt]", "Score 0.0381", "lat 50.00000, lon 8.27000",
                     "Zuständige Behörde:", "Finanzamt Mainz-Mitte", "Schillerstraße 13, 55116 Mainz",
                     "06131 000", "poststelle@fa.example", "gemfa_vg25"):
            self.assertIn(part, text)

    def test_text_for_nearby_and_failure(self):
        text = render_text(finder(nearby_result(), FakeMatcher(outcome("polizei"))).find("Anliegen", "Adresse 1"))
        self.assertIn("nur Anlaufstellen", text)
        self.assertIn("ca. 408 m entfernt", text)
        self.assertIn("Hinweis: Nahegelegener Standort", text)
        failing = Finder(matcher=FakeMatcher(outcome()), geocoder=FakeGeocoder(None, error=__import__("behoerden_finder.geocode", fromlist=["x"]).GeocodingError("Keine Koordinate")),
                         resolve_fn=lambda *a, **k: {}).find("Anliegen", "Nirgendwo")
        text = render_text(failing)
        self.assertIn("Adresse nicht gefunden", text)
        self.assertIn("Keine Koordinate", text)


class CliTest(unittest.TestCase):
    def test_success_text_exit_0(self):
        code, out, _ = run(["--anliegen", "Geschenk", "--adresse", "Musterstraße 12"], lambda a: finder(polygon_result()))
        self.assertEqual(code, 0)
        self.assertIn("Finanzamt Mainz-Mitte", out)

    def test_json_output_has_no_polygons(self):
        code, out, _ = run(["--anliegen", "Geschenk", "--adresse", "Musterstraße 12", "--json"], lambda a: finder(polygon_result()))
        data = json.loads(out)
        self.assertEqual(code, 0)
        self.assertEqual(data["status"], "zustaendige_behoerde")
        self.assertIsNone(data["zustaendigkeit"]["matches"][0]["geometry"])

    def test_no_result_exit_2(self):
        code, _, _ = run(["--anliegen", "x", "--adresse", "y"], lambda a: finder({"status": "no_coverage_at_point", "matches": []}))
        self.assertEqual(code, 2)

    def test_resolver_error_exit_1(self):
        def boom(*a, **k):
            raise RuntimeError("WFS down")
        f = Finder(matcher=FakeMatcher(outcome()), geocoder=FakeGeocoder(GeocodeResult("a", GeoHit(50, 8, "x", "ort"))), resolve_fn=boom)
        self.assertEqual(run(["--anliegen", "x", "--adresse", "y"], lambda a: f)[0], 1)

    def test_missing_address_without_tty_is_an_error(self):
        code, _, err = run(["--anliegen", "Geschenk"], lambda a: finder(polygon_result()))
        self.assertEqual(code, 1)
        self.assertIn("Adresse", err)

    def test_lat_without_lon_is_rejected(self):
        with self.assertRaises(SystemExit), contextlib.redirect_stderr(io.StringIO()):
            cli.main(["--kategorie", "finanzamt", "--lat", "50.0"], finder_factory=lambda a: None)

    def test_category_and_coordinates_need_no_matcher_or_geocoder(self):
        args = cli.build_parser().parse_args(["--kategorie", "jobcenter", "--lat", "49.488", "--lon", "8.466"])
        f = cli._make_finder(args)
        self.assertIsNone(f.matcher)
        self.assertIsNone(f.geocoder)

    def test_interactive_loop_reuses_address(self):
        seen = []
        f = finder(polygon_result())
        original = f.find
        f.find = lambda *a, **k: (seen.append(a), original(*a, **k))[1]
        answers = iter(["Geschenk", "Musterstraße 12", "Hund anmelden", "", "exit"])
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err), mock.patch("builtins.input", lambda p="": next(answers)):
            code = cli.main([], finder_factory=lambda a: f)
        self.assertEqual(code, 0)
        self.assertEqual(seen, [("Geschenk", "Musterstraße 12"), ("Hund anmelden", "Musterstraße 12")])


if __name__ == "__main__":
    unittest.main()
