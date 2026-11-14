"""Google-Places-Zusatzinfos: Anfrageaufbau, Auswertung, Fehltreffer, ohne Schlüssel."""
import unittest

from behoerden_finder.google_places import PlacesClient, intervalle, zentrum_von
from helpers import FakeResponse

PLACE = {"places": [{"id": "ChIJabc", "displayName": {"text": "Finanzamt Mainz"},
                     "formattedAddress": "Schillerstraße 13, 55116 Mainz, Deutschland",
                     "location": {"latitude": 49.998, "longitude": 8.271}, "googleMapsUri": "https://maps.google.com/?cid=1",
                     "nationalPhoneNumber": "06131 2510", "websiteUri": "https://finanzamt.example",
                     "regularOpeningHours": {"weekdayDescriptions": ["Montag: 08:00–12:00", "Dienstag: Geschlossen"]},
                     "currentOpeningHours": {"openNow": False}}]}


class PlacesTest(unittest.TestCase):
    def client(self, payload=PLACE, status=200):
        self.calls = []

        def post(url, json, timeout, headers):
            self.calls.append((json, headers))
            return FakeResponse(payload, status)
        return PlacesClient("KEY", http_post=post)

    def test_without_key_nothing_is_sent(self):
        c = PlacesClient("", http_post=lambda *a, **k: self.fail("darf nicht senden"))
        self.assertEqual(c.details("Finanzamt Mainz"), {"verfuegbar": False})

    def test_request_and_parsing(self):
        c = self.client()
        r = c.details("Finanzamt Mainz-Mitte", "Schillerstraße 13, 55116 Mainz", (50.0, 8.27))
        body, headers = self.calls[0]
        self.assertEqual(body["textQuery"], "Finanzamt Mainz-Mitte, Schillerstraße 13, 55116 Mainz")
        self.assertEqual(body["locationBias"]["circle"]["center"], {"latitude": 50.0, "longitude": 8.27})
        self.assertIn("places.regularOpeningHours.weekdayDescriptions", headers["X-Goog-FieldMask"])
        self.assertTrue(r["gefunden"])
        self.assertEqual(r["oeffnungszeiten"][1], "Dienstag: Geschlossen")
        self.assertIs(r["jetzt_geoeffnet"], False)
        self.assertEqual(r["place_id"], "ChIJabc")

    def test_cached(self):
        c = self.client()
        c.details("Finanzamt Mainz", None, (50.0, 8.27))
        c.details("Finanzamt Mainz", None, (50.0, 8.27))
        self.assertEqual(len(self.calls), 1)

    def test_far_away_hit_is_rejected(self):
        r = self.client().details("Finanzamt Mainz", None, (53.55, 10.0))     # Hamburg
        self.assertFalse(r["gefunden"])

    def test_http_error(self):
        r = self.client({}, 403).details("Finanzamt Mainz")
        self.assertFalse(r["gefunden"])
        self.assertIn("403", r["fehler"])

    def test_periods(self):
        w = intervalle([{"open": {"day": 1, "hour": 8, "minute": 0}, "close": {"day": 1, "hour": 12, "minute": 30}},
                        {"open": {"day": 0, "hour": 10, "minute": 0}, "close": {"day": 0, "hour": 14, "minute": 0}}])
        self.assertEqual(w[0], [["08:00", "12:30"]])     # Montag
        self.assertEqual(w[6], [["10:00", "14:00"]])     # Sonntag
        self.assertEqual(intervalle([{"open": {"day": 0, "hour": 0, "minute": 0}}])[3], [["00:00", "24:00"]])
        self.assertIsNone(intervalle(None))

    def test_center(self):
        self.assertEqual(zentrum_von({"type": "Point", "coordinates": [8.27, 50.0]}), (50.0, 8.27))
        self.assertEqual(zentrum_von({"type": "MultiPolygon", "coordinates": [[[[0, 0], [2, 0], [2, 4], [0, 0]]]]}), (2.0, 1.0))
        self.assertIsNone(zentrum_von(None))


if __name__ == "__main__":
    unittest.main()
