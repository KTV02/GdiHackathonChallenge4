import importlib.util
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from behoerden_finder.matching import AuthorityMatcher, MatcherUnavailable, interpret_match

HAS_MATCHER = importlib.util.find_spec("authority_matcher") is not None

PROPOSAL = {"status": "vorschlag", "behoerdenkategorie": "finanzamt", "score": 0.0381, "top_kategorie": "finanzamt",
            "alternativen": [{"id": "buergeramt", "score": 0.01}],
            "leistungen": [{"id": "a", "titel": "Hund anmelden", "leistungsschluessel": "99000000000001", "score": 0.4,
                            "behoerdenkategorien": ["ordnungsamt"]},
                           {"id": "b", "titel": "Schenkung anzeigen", "leistungsschluessel": "99000000000002", "score": 0.3,
                            "behoerdenkategorien": ["finanzamt"]}]}
AMBIGUOUS = {"status": "mehrdeutig", "behoerdenkategorie": None, "score": 0.05, "top_kategorie": "buergeramt",
             "alternativen": [{"id": "standesamt", "score": 0.049}],
             "leistungen": [{"id": "a", "titel": "Namensänderung", "leistungsschluessel": "99000000000003", "score": 0.4,
                             "behoerdenkategorien": ["buergeramt", "standesamt"]}]}


class InterpretTest(unittest.TestCase):
    def test_proposal_picks_service_belonging_to_category(self):
        o = interpret_match(PROPOSAL)
        self.assertEqual((o.status, o.category, o.score), ("vorschlag", "finanzamt", 0.0381))
        self.assertEqual(o.service["titel"], "Schenkung anzeigen")          # nicht die bestplatzierte Leistung (Hund), sondern die der Kategorie
        self.assertEqual([c["id"] for c in o.candidates], ["finanzamt", "buergeramt"])
        self.assertFalse(o.ambiguous)

    def test_ambiguous_stops_by_default(self):
        o = interpret_match(AMBIGUOUS)
        self.assertIsNone(o.category)
        self.assertTrue(o.ambiguous)
        self.assertEqual([c["id"] for c in o.candidates], ["buergeramt", "standesamt"])
        self.assertEqual(o.service["titel"], "Namensänderung")

    def test_ambiguous_can_take_best(self):
        o = interpret_match(AMBIGUOUS, accept_ambiguous=True)
        self.assertEqual((o.category, o.status), ("buergeramt", "mehrdeutig_beste_gewaehlt"))

    def test_no_assignment(self):
        o = interpret_match({"status": "keine_zuordnung", "behoerdenkategorie": None, "score": None,
                             "alternativen": [], "leistungen": []})
        self.assertIsNone(o.category)
        self.assertIsNone(o.service)
        self.assertIsNone(o.score)
        self.assertFalse(o.ambiguous)

    def test_service_found_but_no_authority_type(self):
        o = interpret_match({"status": "behoerdenart_in_fim_nicht_ermittelbar", "behoerdenkategorie": None,
                             "score": None, "top_kategorie": None, "alternativen": [],
                             "leistungen": [{"titel": "Erbschein", "leistungsschluessel": "99000000000004",
                                             "score": 0.2, "behoerdenkategorien": []}]})
        self.assertIsNone(o.category)
        self.assertEqual(o.service["titel"], "Erbschein")
        self.assertFalse(o.ambiguous)


@unittest.skipUnless(HAS_MATCHER, "authority_matcher nicht im Importpfad")
class AdapterWiringTest(unittest.TestCase):
    """Aufrufe werden mit autospec gegen die ECHTEN Signaturen aus authority_matcher.autorun geprüft."""

    def test_calls_match_the_real_signatures(self):
        from authority_matcher import autorun as am
        with tempfile.TemporaryDirectory() as tmp:
            db = Path(tmp) / "fim.sqlite3"
            db.write_text("")
            with mock.patch.object(am, "DB", db), \
                    mock.patch.object(am, "get_index_docs", autospec=True) as index, \
                    mock.patch.object(am, "reference_regions", autospec=True, return_value=("110000000000",)) as regions, \
                    mock.patch.object(am, "make_enricher", autospec=True, return_value="ENRICH") as enricher, \
                    mock.patch.object(am, "match_authority", autospec=True, return_value=PROPOSAL) as match:
                matcher = AuthorityMatcher(semantic=False, live=True, reference_ars="110000000000", live_top=2,
                                           start_docker=False)
                first = matcher.match("Meine Oma hat mir Geld geschenkt")
                matcher.match("Noch eine Frage")
            self.assertEqual(first.category, "finanzamt")
            index.assert_called_once_with(semantic=False, force=False, no_start_docker=True)   # Indexcheck nur einmal
            regions.assert_called_once_with("110000000000")
            self.assertEqual(enricher.call_count, 2)
            _, kwargs = enricher.call_args
            self.assertEqual((kwargs["regions"], kwargs["max_keys"]), (("110000000000",), 2))
            _, kwargs = match.call_args
            self.assertEqual(kwargs["backend"], "elasticsearch")
            self.assertEqual(kwargs["enrich"], "ENRICH")
            self.assertFalse(kwargs["semantic"])

    def test_no_live_means_no_enricher(self):
        from authority_matcher import autorun as am
        with tempfile.TemporaryDirectory() as tmp:
            db = Path(tmp) / "fim.sqlite3"
            db.write_text("")
            with mock.patch.object(am, "DB", db), \
                    mock.patch.object(am, "get_index_docs", autospec=True), \
                    mock.patch.object(am, "reference_regions", autospec=True, return_value=()), \
                    mock.patch.object(am, "make_enricher", autospec=True) as enricher, \
                    mock.patch.object(am, "match_authority", autospec=True, return_value=PROPOSAL) as match:
                AuthorityMatcher(live=False).match("Anliegen text")
            enricher.assert_not_called()
            self.assertIsNone(match.call_args.kwargs["enrich"])

    def test_missing_fim_database_is_reported(self):
        from authority_matcher import autorun as am
        with mock.patch.object(am, "DB", Path("/nonexistent/fim.sqlite3")):
            with self.assertRaisesRegex(MatcherUnavailable, "FIM-Datenbank"):
                AuthorityMatcher().match("Anliegen text")

    def test_live_top_validated(self):
        with self.assertRaises(ValueError):
            AuthorityMatcher(live_top=0)


if __name__ == "__main__":
    unittest.main()
