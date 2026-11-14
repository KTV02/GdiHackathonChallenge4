"""OpenStreetMap-Zusatzinfos: opening_hours, Namensähnlichkeit, Trefferauswahl."""
import unittest
from datetime import datetime

from behoerden_finder.osm_details import OsmClient, aehnlichkeit, jetzt_geoeffnet, oeffnungszeiten
from helpers import FakeResponse

ITEMS = [
    {"name": "Finanzamt Mainz", "lat": "50.0", "lon": "8.27", "osm_type": "way", "osm_id": 1,
     "address": {"road": "Schillerstraße", "house_number": "13", "postcode": "55116", "city": "Mainz"},
     "extratags": {"opening_hours": "Mo-Fr 08:00-12:00; Th 13:30-18:00", "phone": "+49 6131 2510",
                   "website": "https://finanzamt.example"}},
    {"name": "Rathaus Mainz", "lat": "50.01", "lon": "8.28", "osm_type": "node", "osm_id": 2, "address": {}, "extratags": {}},
]


class OpeningHoursTest(unittest.TestCase):
    def test_parse(self):
        p = oeffnungszeiten("Mo-Fr 08:00-12:00,13:00-15:00; We 08:00-12:00; Sa,Su off; PH off")
        self.assertEqual(p["tage"][0], "Montag: 08:00–12:00, 13:00–15:00")
        self.assertEqual(p["tage"][2], "Mittwoch: 08:00–12:00")       # spätere Regel gilt
        self.assertEqual(p["tage"][6], "Sonntag: geschlossen")

    def test_round_the_clock(self):
        self.assertEqual(oeffnungszeiten("24/7")["tage"][0], "Montag: rund um die Uhr")

    def test_unknown_format(self):
        self.assertIsNone(oeffnungszeiten("Mo-Fr 08:00-12:00 \"nach Vereinbarung\""))
        self.assertIsNone(oeffnungszeiten(None))

    def test_open_now(self):
        p = oeffnungszeiten("Mo-Fr 08:00-12:00")
        self.assertTrue(jetzt_geoeffnet(p, datetime(2026, 11, 16, 9, 30)))    # Montag
        self.assertFalse(jetzt_geoeffnet(p, datetime(2026, 11, 16, 12, 0)))
        self.assertFalse(jetzt_geoeffnet(p, datetime(2026, 11, 15, 9, 0)))    # Sonntag
        self.assertIsNone(jetzt_geoeffnet(None))


class MatchTest(unittest.TestCase):
    def test_similarity(self):
        self.assertGreaterEqual(aehnlichkeit("Finanzamt Mainz-Mitte", "Finanzamt Mainz"), 0.75)
        self.assertLess(aehnlichkeit("Finanzamt Mainz-Mitte", "Rathaus Mainz"), 0.5)

    def client(self, payload=ITEMS):
        self.calls = []

        def get(url, params, timeout, headers):
            self.calls.append(params)
            return FakeResponse(payload if "Jobcenter" not in params["q"] else [])
        return OsmClient(http_get=get, sleep=lambda s: None)

    def test_best_match(self):
        r = self.client().details("Finanzamt Mainz-Mitte", "Schillerstraße 13, 55116 Mainz", (50.0, 8.27))
        self.assertTrue(r["gefunden"])
        self.assertEqual(r["adresse"], "Schillerstraße 13, 55116 Mainz")
        self.assertEqual(r["oeffnungszeiten"][3], "Donnerstag: 13:30–18:00")
        self.assertEqual(r["maps_url"], "https://www.openstreetmap.org/way/1")
        self.assertEqual(r["intervalle"][3], [["08:00", "12:00"], ["13:30", "18:00"]] if False else [["13:30", "18:00"]])
        self.assertEqual(r["intervalle"][5], [])
        self.assertEqual(self.calls[0]["extratags"], 1)
        self.assertEqual(self.calls[0]["viewbox"], "8.02000,50.25000,8.52000,49.75000")

    def test_point_uses_small_window_and_cache(self):
        c = self.client()
        c.details("Finanzamt Mainz", None, (50.0, 8.27), punkt=True)
        c.details("Finanzamt Mainz", None, (50.0, 8.27), punkt=True)
        self.assertEqual(len(self.calls), 1)
        self.assertEqual(self.calls[0]["viewbox"], "8.26000,50.01000,8.28000,49.99000")

    def test_name_variants(self):
        c = self.client()
        r = c.details("Landeshauptstadt Mainz – Finanzamt Mainz", None, (50.0, 8.27))
        self.assertTrue(r["gefunden"])

    def test_no_similar_name(self):
        self.assertFalse(self.client().details("Jobcenter Wiesbaden", None, (50.0, 8.27))["gefunden"])

    def test_without_center_nothing_is_sent(self):
        self.assertFalse(self.client().details("Finanzamt Mainz")["gefunden"])
        self.assertEqual(self.calls, [])


if __name__ == "__main__":
    unittest.main()
