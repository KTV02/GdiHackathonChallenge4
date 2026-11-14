"""Weitere Quellen für Öffnungszeiten: Freitext-Parser, amtliche Resolver-Daten, Portalverbund, Webseite, Kette."""
import unittest

from behoerden_finder import oeffnungszeiten as oz
from behoerden_finder.web import App
from behoerden_finder.zusatz_quellen import PvogClient, WebseitenClient, aus_properties
from helpers import FakeResponse

GEMFA = {"source_record_xml": """<ns0:Finanzamt xmlns:ns0="http://www.itzbund.de/GEMFA/Export/01" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" Name="Speyer-Germersheim">
 <ns0:AdresseListe><ns0:Adresse xsi:type="HausanschriftType" Strasse="Johannesstr." HausNr="9" HausNrZusatz="-12" PLZ="67346" Ort="Speyer" /></ns0:AdresseListe>
 <ns0:KontaktListe><ns0:Kontakt Bezeichnung="Tel" Inhalt="06232/60170" /><ns0:Kontakt Bezeichnung="Mail" Inhalt="Poststelle@fa-sp.fin-rlp.de" />
 <ns0:Kontakt Bezeichnung="URL" Inhalt="https://www.fa-speyer-germersheim.rlp.de" /></ns0:KontaktListe>
 <ns0:OeffnungszeitListe><ns0:Oeffnungszeit>Service-Center: Mo: 08:00-16:00 Do: 08:00-18:00</ns0:Oeffnungszeit></ns0:OeffnungszeitListe>
</ns0:Finanzamt>"""}


class ParserTest(unittest.TestCase):
    def test_variants(self):
        self.assertEqual(oz.parse("Mo-Do 09:00-12:00,14:00-15:30; Fr 09:00-12:00")[1], [["09:00", "12:00"], ["14:00", "15:30"]])
        w = oz.parse("Montag bis Freitag von 8 bis 12 Uhr, Donnerstag zusätzlich 14 - 18 Uhr")
        self.assertEqual(w[3], [["08:00", "12:00"], ["14:00", "18:00"]])
        self.assertEqual(w[5], [])
        self.assertEqual(oz.parse("Montag - Donnerstag: 9.00 - 12.00 Uhr\\n Freitag: 9.00 - 12.00 Uhr")[4], [["09:00", "12:00"]])

    def test_no_false_positives(self):
        self.assertIsNone(oz.parse("Tel. 0261 - 20179279"))
        self.assertIsNone(oz.parse("so erreichen Sie uns 8 - 12"))           # 'so' ist hier kein Sonntag
        self.assertIsNone(oz.parse(""))


class AmtlichTest(unittest.TestCase):
    def test_gemfa(self):
        a = aus_properties(GEMFA)
        self.assertEqual(a["adresse"], "Johannesstr. 9-12, 67346 Speyer")
        self.assertEqual(a["telefon"], "06232/60170")
        self.assertEqual(a["web"], "https://www.fa-speyer-germersheim.rlp.de")
        self.assertEqual(a["intervalle"][3], [["08:00", "18:00"]])
        self.assertIn("GemFA", a["zeiten_quelle"])

    def test_plain_key(self):
        a = aus_properties({"sprechzeiten": "Mo-Do 09:00-16:00; Fr: 09:00-14:00"})
        self.assertEqual(a["intervalle"][4], [["09:00", "14:00"]])
        self.assertEqual(aus_properties({}), {})


class PvogTest(unittest.TestCase):
    def test_flow(self):
        calls = []

        def get(url, params, timeout, headers):
            calls.append(url)
            if "reverse" in url:
                return FakeResponse({"extratags": {"de:regionalschluessel": "073180000000"}})
            if "leikaid" in url:
                return FakeResponse({"content": [{"id": "L1.LB.1", "organisationIds": ["L1.OE.1"],
                                                  "onlineServiceLinks": [{"title": "ELSTER", "uri": "https://www.elster.de"}]}]})
            return FakeResponse({"title": "Finanzamt Speyer-Germersheim", "role": {"name": "Zuständige Stelle"},
                                 "internetAddresses": [{"uri": "https://fa.example"}],
                                 "additionalInformation": [{"type": "INFO_TIMES", "text": "<p>Mo-Fr 08:00-12:00</p>"}],
                                 "location": {"addresses": [{"street": "Johannesstraße", "houseNumber": "9", "zip": "67346",
                                                             "city": "Speyer", "geo": {"latitude": "49.3", "longitude": "8.4"}}],
                                              "communications": [{"name": "Telefon Festnetz", "value": "+49 6232 6017-0"}]}})
        c = PvogClient(http_get=get)
        r = c.zustaendig("99102046259000", 49.31, 8.43, "Finanzamt Speyer-Germersheim")
        self.assertEqual(r["online_dienste"][0]["titel"], "ELSTER")
        self.assertEqual(r["stelle"]["adresse"], "Johannesstraße 9, 67346 Speyer")
        self.assertEqual(r["stelle"]["intervalle"][0], [["08:00", "12:00"]])
        n = len(calls)
        c.zustaendig("99102046259000", 49.31, 8.43, "Finanzamt Speyer-Germersheim")
        self.assertEqual(len(calls), n)                                       # zwischengespeichert


class WebseiteTest(unittest.TestCase):
    def client(self, pages):
        def get(url, timeout, headers, stream, allow_redirects):
            r = FakeResponse(None, 200 if url in pages else 404)
            r.headers, r.encoding = {"Content-Type": "text/html"}, "utf-8"
            r.iter_content = lambda n: [pages.get(url, "").encode()]
            return r
        return WebseitenClient(http_get=get, pruefe_host=lambda u: True)

    def test_subpage(self):
        c = self.client({"https://amt.example": '<a href="/service/oeffnungszeiten">Öffnungszeiten</a>',
                         "https://amt.example/service/oeffnungszeiten": "<h2>Öffnungszeiten</h2><p>Mo - Fr 8.00 - 12.30 Uhr</p>"})
        r = c.oeffnungszeiten("https://amt.example")
        self.assertEqual(r["intervalle"][2], [["08:00", "12:30"]])
        self.assertEqual(r["zeiten_url"], "https://amt.example/service/oeffnungszeiten")

    def test_private_hosts_blocked(self):
        from behoerden_finder.zusatz_quellen import _oeffentlich
        self.assertFalse(_oeffentlich("http://127.0.0.1:8765/"))
        self.assertFalse(_oeffentlich("file:///etc/passwd"))


class ChainTest(unittest.TestCase):
    def test_osm_without_hours_then_website(self):
        class Osm:
            def details(self, *a, **k):
                return {"verfuegbar": True, "gefunden": True, "quelle": "osm", "adresse": "X 1", "web": "https://amt.example"}

        class Web:
            def oeffnungszeiten(self, url):
                return {"oeffnungszeiten": ["Montag: 08:00–12:00"], "intervalle": [[["08:00", "12:00"]]] + [[]] * 6,
                        "zeiten_quelle": "Webseite der Behörde (automatisch ausgelesen)"}
        app = App(matcher=None, geocoders={"nominatim": None}, places=Osm(), pvog=False, webseiten=Web())
        status, d = app.details({"name": "Amt", "lat": 50, "lon": 8})
        self.assertEqual(d["oeffnungszeiten"], ["Montag: 08:00–12:00"])
        self.assertEqual(d["quellen"], ["osm", "webseite"])

    def test_pvog_hours_win_and_foreign_office_ignored(self):
        class Osm:
            def details(self, *a, **k):
                return {"verfuegbar": True, "gefunden": True, "quelle": "osm", "oeffnungszeiten": ["Montag: 01:00–02:00"]}

        class Pv:
            def __init__(self, name):
                self.name = name

            def zustaendig(self, *a):
                return {"online_dienste": [{"titel": "ELSTER", "uri": "https://www.elster.de"}],
                        "stelle": {"name": self.name, "oeffnungszeiten": ["Montag: 08:00–12:00"],
                                   "intervalle": [[["08:00", "12:00"]]] + [[]] * 6, "zeiten_quelle": "Portalverbund"}}
        payload = {"name": "Finanzamt Speyer-Germersheim", "lat": 49.3, "lon": 8.4, "leika": "99102046259000",
                   "standort_lat": 49.31, "standort_lon": 8.43}
        d = App(matcher=None, geocoders={"x": None}, places=Osm(), pvog=Pv("Finanzamt Speyer-Germersheim"), webseiten=False).details(payload)[1]
        self.assertEqual(d["zeiten_quelle"], "Portalverbund")
        self.assertEqual(d["online_dienste"][0]["titel"], "ELSTER")
        d = App(matcher=None, geocoders={"x": None}, places=Osm(), pvog=Pv("Stadtverwaltung Speyer"), webseiten=False).details(payload)[1]
        self.assertEqual(d["oeffnungszeiten"], ["Montag: 01:00–02:00"])          # fremde Stelle nicht übernommen


if __name__ == "__main__":
    unittest.main()
