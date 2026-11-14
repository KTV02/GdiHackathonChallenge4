"""Koordinatenabstieg und Bewertung von authority_matcher.tune mit einer Attrappe statt Elasticsearch."""
import unittest

from authority_matcher import index as idx
from authority_matcher import tune


class JudgeTest(unittest.TestCase):
    def test_judge(self):
        self.assertEqual(tune.judge(["a"], {"behoerdenkategorie": "a"}), "richtig")
        self.assertEqual(tune.judge(["a"], {"behoerdenkategorie": "b"}), "falsch")
        self.assertEqual(tune.judge(["a"], {"behoerdenkategorie": None, "top_kategorie": "a"}), "vorn")
        self.assertEqual(tune.judge(["a"], {"behoerdenkategorie": None, "top_kategorie": "b"}), "keine")
        self.assertEqual(tune.judge(["a"], {"status": "keine_zuordnung"}), "keine")

    def test_wrong_answer_costs_more_than_no_answer(self):
        self.assertGreater(tune.score({"richtig": 0, "vorn": 0, "falsch": 0, "keine": 2}),
                           tune.score({"richtig": 1, "vorn": 0, "falsch": 1, "keine": 0}))


class DescentTest(unittest.TestCase):
    def test_picks_best_value_and_restores_module_state(self):
        before = idx.FUZZY_BOOST
        cases = [{"anliegen": f"q{i}", "erwartet": ["a"]} for i in range(10)]

        def match(text):                     # Attrappe: richtig nur, wenn FUZZY_BOOST == 0.15
            ok = idx.FUZZY_BOOST == 0.15
            return {"behoerdenkategorie": "a" if ok else "b"}
        grid = {"FUZZY_BOOST": tune.GRID["FUZZY_BOOST"]}
        out = tune.coordinate_descent(cases, cases, match, rounds=2, grid=grid, log=lambda *_: None)
        self.assertEqual(out["parameter"]["FUZZY_BOOST"], 0.15)
        self.assertEqual(out["training"]["nachher"]["richtig"], 10)
        self.assertEqual(idx.FUZZY_BOOST, before)       # Modulzustand wiederhergestellt

    def test_keeps_value_when_nothing_improves(self):
        cases = [{"anliegen": "q", "erwartet": ["a"]}]
        out = tune.coordinate_descent(cases, [], lambda t: {"behoerdenkategorie": "a"}, grid={"RRF_K": tune.GRID["RRF_K"]},
                                      log=lambda *_: None)
        self.assertEqual(out["parameter"]["RRF_K"], idx.RRF_K)

    def test_lexical_body_without_fuzzy(self):
        with tune.applied({"FUZZY_BOOST": 0.0}):
            body = idx.lexical_body("test", 5)
        self.assertEqual(len(body["query"]["bool"]["should"]), 1)
        self.assertEqual(len(idx.lexical_body("test", 5)["query"]["bool"]["should"]), 2)


if __name__ == "__main__":
    unittest.main()
