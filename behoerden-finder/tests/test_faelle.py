"""Fall-Katalog: Daten, Zuordnung, SQLite-Ablage und Vorrang vor dem Matcher."""
import importlib.util
import sqlite3
import tempfile
import unittest
from pathlib import Path

from behoerden_finder import faelle, themen
from behoerden_finder.matching import apply_topic_hints, interpret_match

HAS_RESOLVER = importlib.util.find_spec("behoerden_resolver") is not None


def raw_proposal(category, title="Leistung"):
    return {"status": "vorschlag", "behoerdenkategorie": category, "score": 0.05, "top_kategorie": category,
            "alternativen": [], "leistungen": [{"titel": title, "behoerdenkategorien": [category]}]}


def raw_nothing():
    return {"status": "behoerdenart_in_fim_nicht_ermittelbar", "behoerdenkategorie": None, "score": None,
            "top_kategorie": None, "alternativen": [], "leistungen": [{"titel": "Etwas", "behoerdenkategorien": []}]}


class DataTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.rows = faelle.builtin_rows()
        cls.cat = faelle.builtin()

    def test_enough_cases_for_all_categories(self):
        self.assertGreaterEqual(len(self.rows), 1000)
        self.assertEqual({c for c, _ in self.rows}, set(themen.TOPICS) | {"stadtverwaltung", "kreisverwaltung"})

    def test_categories_exist_in_resolver(self):
        if not HAS_RESOLVER:
            self.skipTest("behoerden_resolver nicht installiert")
        from behoerden_resolver.catalog import category
        for cid in {c for c, _ in self.rows}:
            self.assertEqual(category(cid)["id"], cid)

    def test_no_phrase_belongs_to_two_categories(self):
        seen = {}
        for c, p in self.rows:
            key = frozenset(faelle.tokens(p))
            self.assertTrue(key, p)
            self.assertNotIn(key, {k for k, v in seen.items() if v != c}, f"{p!r} steht in mehreren Kategorien")
            seen.setdefault(key, c)

    def test_every_phrase_finds_its_own_category(self):
        wrong = []
        for c, p in self.rows:
            hint = self.cat.infer(p)
            if hint is None or hint.category != c:
                wrong.append((c, p, hint and hint.category))
        self.assertEqual(wrong, [])

    def test_examples_from_the_brief(self):
        for text, cat in [("Hund anmelden", "stadtverwaltung"), ("Ich möchte meinen Hund anmelden", "stadtverwaltung"),
                          ("Scheidung einreichen", "amtsgericht"), ("Kfz Zulassung", "zulassungsstelle"),
                          ("Auto zulassen", "zulassungsstelle"), ("Kindergeld beantragen", "familienkasse"),
                          ("Schenkung melden", "finanzamt"), ("Ich möchte mich scheiden lassen", "amtsgericht"),
                          ("Personalausweis verlängern", "buergeramt"), ("Gewerbe anmelden", "gewerbeamt"),
                          ("Wohngeld beantragen", "wohngeldstelle"), ("Pflegegrad beantragen", "pflegekasse")]:
            hint = self.cat.infer(text)
            self.assertIsNotNone(hint, text)
            self.assertEqual(hint.category, cat, text)

    def test_unrelated_text_gives_no_guess(self):
        self.assertIsNone(self.cat.infer("Was ist das Wetter morgen"))
        self.assertIsNone(self.cat.infer(""))

    def test_inflection_and_compounds(self):
        self.assertEqual(self.cat.infer("Hunde anmelden").category, "stadtverwaltung")
        self.assertEqual(self.cat.infer("Ehescheidung").category, "amtsgericht")

    def test_colloquial_forms_are_normalised(self):
        for text, cat in [("Mein Handy wurde im Zug geklaut", "polizei"), ("Bei mir wurde eingebrochen", "polizei"),
                          ("Meine Tochter wurde letzte Woche geboren", "standesamt")]:
            hint = self.cat.infer(text)
            self.assertIsNotNone(hint, text)
            self.assertEqual(hint.category, cat, text)

    def test_generic_words_alone_do_not_outweigh_a_specific_word(self):
        # 'Vater gestorben' (allgemein) darf 'Erbschein' (spezifisch) nicht überstimmen
        hint = self.cat.infer("Mein Vater ist gestorben und ich brauche einen Erbschein")
        self.assertTrue(hint is None or hint.category == "amtsgericht")

    def test_separable_verbs(self):
        self.assertEqual(self.cat.infer("Wo melde ich meinen Hund an").category, "stadtverwaltung")
        self.assertEqual(self.cat.infer("Wie melde ich mein Auto ab").category, "zulassungsstelle")
        self.assertIn("anmeld", faelle.tokens("melde den Hund an"))
        self.assertNotIn("anhund", faelle.tokens("melde den Hund an"))

    def test_parse_errors(self):
        with self.assertRaises(ValueError):
            faelle.parse("ohne Kategorie; zweite")


class SqliteTest(unittest.TestCase):
    def make_db(self):
        d = tempfile.mkdtemp()
        path = Path(d) / "fim_catalog.sqlite3"
        con = sqlite3.connect(path)
        con.execute("CREATE TABLE records (id INTEGER, imported_at TEXT)")
        con.execute("INSERT INTO records VALUES (1, '2026-01-01')")
        con.commit()
        con.close()
        return path

    def test_install_load_roundtrip_keeps_other_tables(self):
        db = self.make_db()
        n = faelle.install(db)
        self.assertGreaterEqual(n, 1000)
        con = sqlite3.connect(db)
        self.assertEqual(con.execute("SELECT COUNT(*) FROM records").fetchone()[0], 1)
        self.assertEqual(con.execute("SELECT DISTINCT quelle FROM faelle").fetchall(), [("fallkatalog",)])
        con.close()
        self.assertEqual(len(faelle.load(db)), n)
        self.assertEqual(faelle.load(db).infer("Hund anmelden").category, "stadtverwaltung")

    def test_reinstall_keeps_rows_of_other_origin(self):
        db = self.make_db()
        faelle.install(db)
        con = sqlite3.connect(db)
        con.execute("INSERT INTO faelle VALUES ('stadtverwaltung', 'Bienenvolk anmelden', 'team')")
        con.commit()
        con.close()
        faelle.install(db)
        self.assertEqual(faelle.load(db).infer("Bienenvolk anmelden").category, "stadtverwaltung")

    def test_ensure_installed_creates_table_once(self):
        db = self.make_db()
        cat = faelle.ensure_installed(db)
        self.assertGreaterEqual(len(cat), 1000)
        mtime = db.stat().st_mtime_ns
        faelle.ensure_installed(db)
        self.assertEqual(db.stat().st_mtime_ns, mtime)   # unverändert: kein erneutes Schreiben

    def test_missing_database_falls_back_to_builtin(self):
        self.assertGreaterEqual(len(faelle.ensure_installed(Path(tempfile.mkdtemp()) / "fehlt.sqlite3")), 1000)
        self.assertGreaterEqual(len(faelle.load(Path(tempfile.mkdtemp()) / "fehlt.sqlite3")), 1000)


class PriorityTest(unittest.TestCase):
    cat = faelle.builtin()

    def test_catalog_overrides_wrong_official_proposal_on_strong_hit(self):
        o = apply_topic_hints(interpret_match(raw_proposal("finanzamt", "Hund anmelden")), "Ich möchte meinen Hund anmelden", self.cat)
        self.assertEqual((o.category, o.status, o.source), ("stadtverwaltung", "fallkatalog", "fallkatalog"))
        self.assertIn("finanzamt", o.note)
        self.assertIn("nicht amtlich belegt", o.note)

    def test_matcher_priority_flag_keeps_official_proposal(self):
        o = apply_topic_hints(interpret_match(raw_proposal("finanzamt")), "Hund anmelden", self.cat, faelle_vorrang=False)
        self.assertEqual((o.category, o.source), ("finanzamt", "matcher"))

    def test_confirmation_leaves_matcher_result_untouched(self):
        o = apply_topic_hints(interpret_match(raw_proposal("stadtverwaltung")), "Hund anmelden", self.cat)
        self.assertEqual((o.status, o.source), ("vorschlag", "matcher"))

    def test_weak_single_word_does_not_override_official_proposal(self):
        o = apply_topic_hints(interpret_match(raw_proposal("finanzamt")), "Frage zum Kindergeld sonstiges", self.cat)
        self.assertEqual((o.category, o.source), ("finanzamt", "matcher"))

    def test_fills_gap_when_official_data_has_no_category(self):
        o = apply_topic_hints(interpret_match(raw_nothing()), "Kfz zulassen", self.cat)
        self.assertEqual((o.category, o.source), ("zulassungsstelle", "fallkatalog"))
        self.assertIn("Fall-Katalog", o.note)

    def test_ambiguous_candidates_are_decided_by_catalog(self):
        raw = {"status": "mehrdeutig", "behoerdenkategorie": None, "score": 0.03, "top_kategorie": "buergeramt",
               "alternativen": [{"id": "standesamt", "score": 0.029}],
               "leistungen": [{"titel": "Namensänderung", "behoerdenkategorien": ["buergeramt", "standesamt"]}]}
        o = apply_topic_hints(interpret_match(raw), "Geburtsurkunde beantragen", self.cat)
        self.assertEqual((o.category, o.source), ("standesamt", "fallkatalog"))

    def test_without_catalog_behaviour_is_unchanged(self):
        o = apply_topic_hints(interpret_match(raw_proposal("finanzamt")), "Hund anmelden")
        self.assertEqual((o.category, o.source), ("finanzamt", "matcher"))

    def test_unknown_text_falls_back_to_keyword_table(self):
        small = faelle.FallKatalog([("buergeramt", "Pass verlängern")])
        o = apply_topic_hints(interpret_match(raw_nothing()), "Frage ans Finanzamt", small)
        self.assertEqual((o.category, o.source), ("finanzamt", "themenhinweis"))


if __name__ == "__main__":
    unittest.main()
