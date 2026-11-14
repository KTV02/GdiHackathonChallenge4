"""Stichwort-Rückfall für die Behördenart (mit den echten Matcher-Ausgaben aus dem Live-Lauf)."""
import importlib.util
import unittest

from behoerden_finder import themen
from behoerden_finder.matching import apply_topic_hints, interpret_match
from behoerden_finder.pipeline import Finder
from helpers import FakeGeocoder, FakeMatcher, polygon_result
from test_pipeline import Recorder, geo

HAS_RESOLVER = importlib.util.find_spec("behoerden_resolver") is not None


def raw_without_authority(*titles, status="behoerdenart_in_fim_nicht_ermittelbar"):
    return {"status": status, "behoerdenkategorie": None, "score": None, "top_kategorie": None, "alternativen": [],
            "leistungen": [{"titel": t, "leistungsschluessel": "99000000000000", "score": 0.02,
                            "behoerdenkategorien": []} for t in titles]}


def run(anliegen, *titles):
    return apply_topic_hints(interpret_match(raw_without_authority(*titles)), anliegen)


class TopicInferenceTest(unittest.TestCase):
    def test_cases_from_the_live_run_get_a_category(self):
        cases = [
            ("Finanzamt", ["Finanzamt"], "finanzamt"),
            ("Steuern", ["Sonstige Steuern"], "finanzamt"),
            ("Steuererklärung", ["Steuererklärung"], "finanzamt"),
            ("Meine Oma hat mir 10.000 € geschenkt. Wo muss ich das melden?",
             ["Erbschaft oder Schenkung; Anzeige beim Finanzamt"], "finanzamt"),
            ("Ich möchte Kindergeld beantragen", ["Kindergeld beantragen"], "familienkasse"),
            ("Ich habe meinen Job verloren und brauche Arbeitslosengeld", ["Arbeitslosengeld beantragen"],
             "arbeitsagentur"),
            ("Ich möchte BAföG für mein Studium beantragen", ["BAföG für ein Studium beantragen"], "bafoegamt"),
        ]
        for anliegen, titles, expected in cases:
            with self.subTest(anliegen):
                o = run(anliegen, *titles)
                self.assertEqual((o.category, o.status, o.source), (expected, "themenhinweis", "themenhinweis"))
                self.assertIn("nicht amtlich belegt", o.note)
                self.assertIsNone(o.score)
                self.assertEqual(o.service["titel"], titles[0])

    def test_no_keyword_means_no_guess(self):
        o = run("Ich brauche einen Termin", "Terminvereinbarung")
        self.assertIsNone(o.category)
        self.assertEqual(o.status, "behoerdenart_in_fim_nicht_ermittelbar")

    def test_conflicting_keywords_do_not_guess(self):
        self.assertIsNone(themen.infer("Steuer und Kindergeld"))

    def test_strong_keyword_beats_weaker_one(self):
        self.assertEqual(themen.infer("Kfz-Steuer zahlen").category, "zoll")        # nicht Finanzamt

    def test_umlauts_and_case_are_ignored(self):
        self.assertEqual(themen.infer("BAFÖG BEANTRAGEN").category, "bafoegamt")
        self.assertEqual(themen.infer("bafög").category, "bafoegamt")

    def test_word_start_keyword(self):
        self.assertIsNone(themen.infer("Grundsteuer"))                              # 'steuer' nur am Wortanfang
        self.assertEqual(themen.infer("Hundesteuer").category, "stadtverwaltung")   # nicht Finanzamt
        self.assertEqual(themen.infer("Steuernummer").category, "finanzamt")

    def test_table_only_uses_known_categories_and_covers_most(self):
        self.assertGreaterEqual(len(themen.TOPICS), 34)
        if HAS_RESOLVER:
            from behoerden_resolver.catalog import category
            for cid in themen.TOPICS:
                self.assertEqual(category(cid)["id"], cid)


class ApplyHintsTest(unittest.TestCase):
    def test_official_proposal_is_never_overridden(self):
        raw = {"status": "vorschlag", "behoerdenkategorie": "ordnungsamt", "score": 0.04, "top_kategorie": "ordnungsamt",
               "alternativen": [], "leistungen": [{"titel": "Hund anmelden", "behoerdenkategorien": ["ordnungsamt"]}]}
        o = apply_topic_hints(interpret_match(raw), "Finanzamt Steuer Hund")
        self.assertEqual((o.category, o.status, o.source), ("ordnungsamt", "vorschlag", "matcher"))

    def test_ambiguity_is_resolved_only_among_the_candidates(self):
        raw = {"status": "mehrdeutig", "behoerdenkategorie": None, "score": 0.03, "top_kategorie": "buergeramt",
               "alternativen": [{"id": "standesamt", "score": 0.029}],
               "leistungen": [{"titel": "Namensänderung", "behoerdenkategorien": ["buergeramt", "standesamt"]}]}
        o = apply_topic_hints(interpret_match(raw), "Wir wollen heiraten und Geburtsurkunde")
        self.assertEqual((o.category, o.status), ("standesamt", "themenhinweis"))
        # Stichwort einer Behörde, die nicht unter den Kandidaten ist: keine Wahl
        o = apply_topic_hints(interpret_match(raw), "Kindergeld")
        self.assertIsNone(o.category)
        self.assertTrue(o.ambiguous)

    def test_weak_single_candidate_is_replaced_by_clear_wording(self):
        raw = {"status": "mehrdeutig", "behoerdenkategorie": None, "score": 0.02, "top_kategorie": "buergeramt",
               "alternativen": [], "leistungen": [{"titel": "Fahrrad herrenlos melden", "behoerdenkategorien": []}]}
        o = apply_topic_hints(interpret_match(raw), "Mir wurde mein Fahrrad gestohlen")
        self.assertEqual((o.category, o.status), ("polizei", "themenhinweis"))
        self.assertIn("gestohlen", o.note)

    def test_dog_registration_is_not_a_finanzamt_matter(self):
        # Im Live-Lauf kam 'finanzamt' aus einer regionalen Variante der Leistung (Referenzgebiete Berlin/Hamburg/Mainz).
        raw = {"status": "mehrdeutig", "behoerdenkategorie": None, "score": 0.03, "top_kategorie": "finanzamt",
               "alternativen": [], "leistungen": [{"titel": "Einen Hund anmelden", "behoerdenkategorien": []},
                                                  {"titel": "Hund anmelden", "behoerdenkategorien": ["finanzamt"]}]}
        o = apply_topic_hints(interpret_match(raw), "Ich möchte meinen Hund anmelden")
        self.assertEqual((o.category, o.status), ("stadtverwaltung", "themenhinweis"))

    def test_weak_single_candidate_stays_if_wording_agrees_or_is_silent(self):
        raw = {"status": "mehrdeutig", "behoerdenkategorie": None, "score": 0.02, "top_kategorie": "finanzamt",
               "alternativen": [], "leistungen": [{"titel": "Einen Hund anmelden", "behoerdenkategorien": []}]}
        for text in ("Ich habe eine Frage", "Finanzamt Hund"):
            o = apply_topic_hints(interpret_match(raw), text)
            self.assertEqual((o.category, o.status, o.source), ("finanzamt", "einziger_kandidat", "matcher"))


class PipelineTest(unittest.TestCase):
    def test_hint_is_reported_in_result(self):
        resolver = Recorder(polygon_result())
        outcome = run("Steuererklärung", "Steuererklärung")
        f = Finder(matcher=FakeMatcher(outcome), geocoder=FakeGeocoder(geo()), resolve_fn=resolver)
        r = f.find("Steuererklärung", "Musterstraße 12, Mainz")
        self.assertEqual(resolver.calls[0][0], "finanzamt")
        self.assertEqual(r["kategorie"]["quelle"], "themenhinweis")
        self.assertTrue(any("nicht amtlich belegt" in h for h in r["hinweise"]))

    def test_render_marks_derived_category(self):
        from behoerden_finder.render import render_text
        resolver = Recorder(polygon_result())
        f = Finder(matcher=FakeMatcher(run("Steuern", "Sonstige Steuern")), geocoder=FakeGeocoder(geo()),
                   resolve_fn=resolver)
        text = render_text(f.find("Steuern", "Musterstraße 12, Mainz"))
        self.assertIn("(aus Stichwörtern abgeleitet)", text)


if __name__ == "__main__":
    unittest.main()
