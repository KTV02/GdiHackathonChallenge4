import unittest

import requests

from behoerden_finder.geocode import GeocodingError, Geocoder, haversine_km
from helpers import FakeHttp, FakeResponse, hit


class Clock:
    def __init__(self):
        self.now, self.slept = 100.0, []

    def __call__(self):
        return self.now

    def sleep(self, seconds):
        self.slept.append(round(seconds, 3))
        self.now += seconds


def make(*responses, clock=None):
    clock = clock or Clock()
    http = FakeHttp(*responses)
    return Geocoder("https://nominatim.invalid/search", "kontakt@example.invalid", http_get=http,
                    clock=clock, sleep=clock.sleep), http, clock


class GeocoderTest(unittest.TestCase):
    def test_request_parameters_and_user_agent(self):
        g, http, _ = make(FakeResponse([hit(50.0, 8.27)]))
        result = g.locate("Musterstraße 12, Mainz")
        url, kw = http.calls[0]
        self.assertEqual(kw["params"]["countrycodes"], "de")
        self.assertEqual(kw["params"]["q"], "Musterstraße 12, Mainz")
        self.assertIn("kontakt@example.invalid", kw["headers"]["User-Agent"])
        self.assertEqual((result.best.lat, result.best.lon), (50.0, 8.27))
        self.assertEqual(result.best.precision, "hausnummer")
        self.assertFalse(result.ambiguous)

    def test_precision_levels(self):
        g, _, _ = make(FakeResponse([hit(50, 8, house=None)]))
        self.assertEqual(g.locate("Musterstraße, Mainz").best.precision, "strasse")
        g, _, _ = make(FakeResponse([hit(50, 8, house=None, road=None)]))
        self.assertEqual(g.locate("Mainz").best.precision, "ort")

    def test_rate_limit_waits_between_requests(self):
        g, _, clock = make(FakeResponse([hit(50, 8)]))
        g.search("Adresse eins")
        g.search("Adresse zwei")
        self.assertEqual(len(clock.slept), 1)
        self.assertGreaterEqual(clock.slept[0], 1.09)

    def test_cache_avoids_second_request(self):
        g, http, _ = make(FakeResponse([hit(50, 8)]))
        g.search("Musterstraße 12  Mainz")
        g.search("musterstraße 12 mainz")
        self.assertEqual(len(http.calls), 1)

    def test_no_result(self):
        g, _, _ = make(FakeResponse([]))
        with self.assertRaisesRegex(GeocodingError, "Keine Koordinate"):
            g.locate("Gibtesnicht 99")

    def test_http_errors_and_network_errors(self):
        for response, pattern in [(FakeResponse([], 429), "429"), (FakeResponse([], 500), "500"),
                                  (FakeResponse(ValueError("kaputt")), "JSON"), (FakeResponse({"a": 1}), "Antwortformat"),
                                  (requests.ConnectionError("down"), "nicht erreichbar")]:
            g, _, _ = make(response)
            with self.assertRaisesRegex(GeocodingError, pattern):
                g.search("Musterstraße 12")

    def test_failed_request_still_counts_for_rate_limit(self):
        g, _, clock = make(requests.ConnectionError("down"))
        with self.assertRaises(GeocodingError):
            g.search("Musterstraße 12")
        with self.assertRaises(GeocodingError):
            g.search("Musterstraße 13")
        self.assertEqual(len(clock.slept), 1)

    def test_input_length(self):
        g, http, _ = make(FakeResponse([hit(50, 8)]))
        for bad in ("", "  ", "ab", "x" * 201, None):
            with self.assertRaises(GeocodingError):
                g.search(bad)
        self.assertEqual(http.calls, [])

    def test_invalid_coordinates_are_dropped(self):
        g, _, _ = make(FakeResponse([{"lat": "abc", "lon": "8"}, {"lat": "95", "lon": "8"}, {"lat": "nan", "lon": "8"}]))
        with self.assertRaises(GeocodingError):
            g.locate("Musterstraße 12")

    def test_ambiguity_only_for_far_apart_hits(self):
        near = [hit(50.0, 8.27), hit(50.0001, 8.2701), hit(50.01, 8.28)]       # <2 km: Alternative, aber eindeutig
        g, _, _ = make(FakeResponse(near))
        r = g.locate("Hauptstraße 1 Mainz")
        self.assertFalse(r.ambiguous)
        self.assertEqual(len(r.alternatives), 1)          # der fast identische Treffer wird nicht als Alternative gezählt
        far = [hit(50.0, 8.27), hit(48.14, 11.58, name="Hauptstraße 1, München")]
        g, _, _ = make(FakeResponse(far))
        r = g.locate("Hauptstraße 1")
        self.assertTrue(r.ambiguous)
        self.assertEqual(r.alternatives[0].display_name, "Hauptstraße 1, München")

    def test_haversine(self):
        self.assertAlmostEqual(haversine_km(50.0, 8.0, 50.0, 8.0), 0.0)
        self.assertAlmostEqual(haversine_km(48.137, 11.575, 52.52, 13.405), 504, delta=5)  # München-Berlin


if __name__ == "__main__":
    unittest.main()
