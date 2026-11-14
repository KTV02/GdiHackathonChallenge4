"""Leistungsbeschreibung aus jzufi_services: Abschnitte, Unterlagen, Online-Hinweis, Länderwahl."""
import json
import sqlite3
import tempfile
import unittest
from pathlib import Path

from behoerden_finder import leistung_details as ld

RAW = {
    "gueltigkeitGebietID": [{"value": "110000000000"}],
    "kennzeichenFormularPersoenlich": True,
    "modulText": [
        {"leikaTextmodul": {"code": "24"}, "inhalt": [{"value": "<p>Wenn Sie ins Ausland ziehen, müssen Sie sich abmelden.</p><p>If you move abroad, you must deregister your residence.</p>", "languageCode": "de"}]},
        {"leikaTextmodul": {"code": "11"}, "inhalt": [{"value": "<p>Sie können die Abmeldung auch online über das Serviceportal erledigen.</p>", "languageCode": "de"}]},
        {"leikaTextmodul": {"code": "14"}, "inhalt": [{"value": "<ul><li><a href=\"https://example.org/abmeldung.pdf\">Abmeldung bei der Meldebehörde</a></li></ul>", "languageCode": "de"}]},
    ],
    "modulDokument": [{"beschreibungEingehend": [{"value": "<strong>Personaldokument</strong><br>Personalausweis oder Reisepass", "languageCode": "de"}]}],
    "modulKosten": [{"beschreibung": [{"value": "gebührenfrei", "languageCode": "de"}]}],
    "modulUrsprungsportal": [{"uri": "https://service.berlin.de/dienstleistung/120335/", "titel": "Abmeldung"}],
}


class ParseTest(unittest.TestCase):
    def test_sections(self):
        d = ld.auswerten(RAW)
        self.assertEqual(d["kurztext"], ["Wenn Sie ins Ausland ziehen, müssen Sie sich abmelden."])   # Englisch entfernt
        self.assertEqual(d["unterlagen"], ["Personaldokument: Personalausweis oder Reisepass"])
        self.assertEqual(d["kosten"], ["gebührenfrei"])
        self.assertEqual(d["formulare"][0]["uri"], "https://example.org/abmeldung.pdf")
        self.assertTrue(d["online"]["moeglich"])
        self.assertEqual(d["land"], "Berlin")
        self.assertEqual(d["portal"][0]["uri"], "https://service.berlin.de/dienstleistung/120335/")

    def test_no_online_hint(self):
        raw = {**RAW, "modulText": RAW["modulText"][:1] + RAW["modulText"][2:]}
        self.assertFalse(ld.auswerten(raw)["online"]["moeglich"])

    def test_only_http_links(self):
        teile, links = ld.text([{"value": '<a href="javascript:alert(1)">x</a> <a href="https://ok.example">ok</a>'}])
        self.assertEqual(links, [{"titel": "ok", "uri": "https://ok.example"}])


class LookupTest(unittest.TestCase):
    def test_lookup_prefers_land(self):
        db = Path(tempfile.mkdtemp()) / "fim_catalog.sqlite3"
        con = sqlite3.connect(db)
        con.execute("CREATE TABLE jzufi_services (lb_id TEXT, keys_json TEXT, title TEXT, raw_json TEXT, source TEXT)")
        hh = {**RAW, "gueltigkeitGebietID": [{"value": "020000000000"}], "modulKosten": []}
        for lb, raw in (("a", RAW), ("b", hh)):
            con.execute("INSERT INTO jzufi_services VALUES (?,?,?,?,?)", (lb, '["99115001000000"]', "Abmeldung", json.dumps(raw), "x"))
        con.commit()
        con.close()
        self.assertEqual(ld.lookup("99115001000000", "Hamburg", db)["land"], "Hamburg")
        self.assertEqual(ld.lookup("99115001000000", None, db)["land"], "Berlin")     # umfangreicher
        self.assertFalse(ld.lookup("99000000000000", None, db)["gefunden"])
        self.assertFalse(ld.lookup("kaputt", None, db)["gefunden"])

    def test_land_from_address(self):
        self.assertEqual(ld.land_aus_text("Schillerstraße 13, 55116 Mainz, Rheinland-Pfalz, Deutschland"), "Rheinland-Pfalz")


if __name__ == "__main__":
    unittest.main()
