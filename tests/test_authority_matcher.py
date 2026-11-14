"""Offline regression and contract tests; synthetic fixtures are not authority evidence."""

import contextlib
import copy
import io
import json
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path
from unittest.mock import Mock, patch

import requests

from authority_matcher import AuthorityMatcher
from authority_matcher.__main__ import main
from authority_matcher.catalog import LiveCatalog, SnapshotCatalog, export_snapshot
from authority_matcher.categories import CategoryRegistry
from authority_matcher.config import load_config
from authority_matcher.coverage import coverage_report
from authority_matcher.errors import MatcherError
from authority_matcher.resolver import ROLE_URI
from authority_matcher.text import normalize_query

ARS = "073150000000"
SID = "L100039.LB.8966636"
ORG = "L100039.OE.249983665"


def organisation(org_id=ORG, code="02", **extra):
    return {"id": org_id, "title": "Fixture organisation",
            "role": {"uri": ROLE_URI, "code": code, "name": "Fixture role"}, "criteria": [], **extra}


class FakeCatalog:
    mode = "live"

    def __init__(self, *, items=None, orgs=None, incomplete=False):
        self.items = items if items is not None else [{"id": SID, "name": "Hundehaltung anmelden"}]
        self.orgs = orgs if orgs is not None else [organisation()]
        self.incomplete = incomplete
        self.evidence = [{"url": "https://example.invalid/test-fixture", "retrieved_at": "2026-11-13T12:00:00+00:00"}]

    def search(self, query, ars):
        return copy.deepcopy(self.items), self.incomplete

    def detail(self, service_id, ars):
        return {"id": service_id, "name": "Fixture service", "leikakeys": ["99102013104000"],
                "organisations": copy.deepcopy(self.orgs)}


class SnapshotTimeTests(unittest.TestCase):
    def setUp(self):
        # The application expires extracts; tests must remain reproducible later.
        self.config = load_config()
        data = json.loads(Path(self.config["snapshot_file"]).read_text())
        self.test_now = datetime.fromisoformat(data["retrieved_at"]) + timedelta(hours=1)
        mocked = patch("authority_matcher.catalog.datetime", wraps=datetime).start()
        mocked.now.return_value = self.test_now
        self.addCleanup(patch.stopall)


class MatchingTests(SnapshotTimeTests):
    def resolve(self, query="Ich möchte meinen Hund anmelden", **kwargs):
        return AuthorityMatcher(catalog=FakeCatalog(**kwargs)).match(query)

    def test_natural_language_uses_real_relation_id_and_local_category(self):
        result = self.resolve()
        self.assertEqual(result["status"], "matched")
        self.assertEqual(result["normalized_query"], "hund anmelden")
        self.assertEqual(result["category"]["code"], "KOMMUNALE_STEUERVERWALTUNG")
        self.assertFalse(result["jurisdiction_resolved"])

    def test_opposing_action_cannot_be_selected(self):
        result = self.resolve(items=[{"id": "X.1", "name": "Hundehaltung abmelden"}])
        self.assertEqual(result["status"], "no_match")
        self.assertIsNone(result["category"])

    def test_official_synonym_can_match_without_title_word_overlap(self):
        result = self.resolve("Ich möchte mein Auto anmelden", items=[{
            "id": SID, "name": "Erstzulassung eines Fahrzeugs (Neuzulassung) beantragen",
            "synonyms": ["Auto anmelden", "Fahrzeug anmelden"]}])
        self.assertEqual(result["status"], "matched")
        self.assertEqual(result["service"]["matched_field"], "synonyms")
        self.assertEqual(result["service"]["matched_text"], "Auto anmelden")
        self.assertFalse(result["service"]["title_coverage"])

    def test_synonyms_do_not_erase_opposing_actions_or_mix_separate_intents(self):
        for query, synonyms in [("Auto abmelden", ["Auto anmelden"]),
                                ("Auto reparieren", ["Auto anmelden", "Fahrrad reparieren"])]:
            with self.subTest(query=query):
                result = self.resolve(query, items=[{"id": SID, "name": "Zulassung beantragen", "synonyms": synonyms}])
                self.assertNotEqual(result["status"], "matched")

    def test_equal_official_synonyms_remain_ambiguous(self):
        items = [{"id": "X.1", "name": "Verfahren A", "synonyms": ["Auto anmelden"]},
                 {"id": "X.2", "name": "Verfahren B", "synonyms": ["Auto anmelden"]}]
        self.assertEqual(self.resolve("Auto anmelden", items=items)["status"], "ambiguous_service")

    def test_no_match_distinguishes_no_source_hits_from_local_rejection(self):
        no_source = self.resolve("Rakete anmelden", items=[])
        filtered = self.resolve("Rakete anmelden")
        self.assertEqual(no_source["status"], "no_match")
        self.assertEqual(no_source["search_summary"]["source_hits"], 0)
        self.assertEqual(filtered["status"], "no_match")
        self.assertEqual(filtered["search_summary"]["source_hits"], 1)
        self.assertEqual(filtered["search_summary"]["plausible_hits"], 0)

    def test_single_subject_requires_clarification(self):
        self.assertEqual(self.resolve("Hund")["status"], "ambiguous_service")

    def test_negation_does_not_silently_disappear(self):
        result = self.resolve("Ich möchte meinen Hund nicht anmelden")
        self.assertEqual(result["status"], "ambiguous_service")

    def test_unknown_object_not_matched_by_generic_verb(self):
        result = self.resolve("Ich möchte eine Rakete anmelden")
        self.assertNotEqual(result["status"], "matched")

    def test_distinct_compound_words_are_not_prefix_matches(self):
        for query, name in [("Auto anmelden", "Autobahn anmelden"), ("Pass beantragen", "Passwort beantragen")]:
            with self.subTest(query=query):
                result = self.resolve(query, items=[{"id": "X.1", "name": name}])
                self.assertNotEqual(result["status"], "matched")

    def test_equal_matches_and_incomplete_search_require_selection(self):
        result = self.resolve(items=[{"id": SID, "name": "Hund anmelden"}, {"id": "X.2", "name": "Hund anmelden"}])
        self.assertEqual(result["status"], "ambiguous_service")
        self.assertEqual(self.resolve(incomplete=True)["status"], "ambiguous_service")

    def test_explicit_selection_is_bounded_by_plausible_search_results(self):
        matcher = AuthorityMatcher(catalog=FakeCatalog(incomplete=True))
        self.assertEqual(matcher.match("Hund anmelden", service_id=SID)["status"], "matched")
        self.assertEqual(matcher.match("Hund anmelden", service_id="X.unknown")["status"], "invalid_service_selection")

    def test_contact_only_hidden_and_unknown_roles_never_prove_category(self):
        for orgs, expected in [([organisation(code="03")], "no_competent_role"),
                               ([organisation(isPublicDisplayAllowed=False)], "no_association"),
                               ([organisation(role={"code": "02", "uri": "wrong-code-list"})], "no_competent_role"),
                               ([], "no_association")]:
            with self.subTest(expected=expected):
                self.assertEqual(self.resolve(orgs=orgs)["status"], expected)

    def test_additional_official_criteria_are_preserved(self):
        result = self.resolve(orgs=[organisation(criteria=[{"name": "Wohnort", "code": "01", "auspraegungen": ["A"]}])])
        self.assertEqual(result["status"], "criteria_required")
        self.assertTrue(result["organisations"][0]["criteria"])

    def test_unknown_organisation_and_partial_mapping_are_not_guessed(self):
        for orgs in [[organisation("X.unknown")], [organisation(), organisation("X.unknown")]]:
            result = self.resolve(orgs=orgs)
            self.assertEqual(result["status"], "category_unmapped")
            self.assertIsNone(result["category"])

    def test_multiple_categories_are_exposed(self):
        result = self.resolve(orgs=[organisation(), organisation("L100039.OE.250472653")])
        self.assertEqual(result["status"], "ambiguous_category")
        self.assertEqual(len(result["categories"]), 2)

    def test_region_scope_is_not_inferred_nationwide(self):
        result = AuthorityMatcher(catalog=FakeCatalog()).match("Hund anmelden", ars="110000000000")
        self.assertEqual(result["status"], "category_unmapped")

    def test_invalid_inputs_and_terminal_controls(self):
        matcher = AuthorityMatcher(catalog=FakeCatalog())
        for query in [None, "", "a", "a" * 501, "ich möchte bitte", "anmelden"]:
            self.assertEqual(matcher.match(query)["status"], "invalid_query")
        self.assertEqual(matcher.match("Hund anmelden", ars="07")["status"], "invalid_ars")
        self.assertNotIn("\x1b", normalize_query("\x1b[31mHund anmelden"))


class CatalogTests(SnapshotTimeTests):
    def test_related_search_never_confirms_a_different_procedure(self):
        empty, related = Mock(), Mock()
        empty.json.return_value = {"serviceDescriptions": {"last": True, "content": []}}
        related.json.return_value = {"serviceDescriptions": {"last": True, "content": [{
            "id": "L100039.LB.229942405", "name": "Schenkungsteuerbescheid erhalten", "synonyms": ["Schenkung"]}]}}
        with patch("authority_matcher.catalog.requests.Session.get", side_effect=[empty, related]) as get:
            result = AuthorityMatcher().match("Schenkung anmelden")
        self.assertEqual(result["status"], "no_match")
        self.assertIsNone(result["category"])
        self.assertIsNone(result["service"])
        self.assertEqual(result["related_query"], "schenkung")
        self.assertEqual(result["search_summary"]["source_hits"], 0)
        self.assertEqual(result["search_summary"]["related_source_hits"], 1)
        self.assertFalse(result["related_candidates"][0]["request_match_verified"])
        self.assertEqual(get.call_count, 2)
        self.assertEqual(get.call_args_list[1].kwargs["params"]["q"], "schenkung")

    def test_related_search_can_be_disabled_and_its_outage_does_not_mask_original_result(self):
        empty = Mock()
        empty.json.return_value = {"serviceDescriptions": {"last": True, "content": []}}
        with patch("authority_matcher.catalog.requests.Session.get", return_value=empty) as get:
            result = AuthorityMatcher(config={**self.config, "suggest_related_on_no_match": False}).match("Schenkung anmelden")
        self.assertEqual(result["status"], "no_match")
        self.assertFalse(result["related_candidates"])
        self.assertEqual(get.call_count, 1)
        with patch("authority_matcher.catalog.requests.Session.get", side_effect=[empty, requests.ConnectionError("secret")]):
            result = AuthorityMatcher().match("Schenkung anmelden")
        self.assertEqual(result["status"], "no_match")
        self.assertNotIn("secret", json.dumps(result))
        self.assertTrue(any("zusätzliche Suche" in warning for warning in result["warnings"]))

    def test_related_suggestions_do_not_emit_a_keyword(self):
        empty, related = Mock(), Mock()
        empty.json.return_value = {"serviceDescriptions": {"last": True, "content": []}}
        related.json.return_value = {"serviceDescriptions": {"last": True, "content": [{
            "id": "L100039.LB.229942405", "name": "Schenkungsteuerbescheid erhalten", "synonyms": ["Schenkung"]}]}}
        output, diagnostics = io.StringIO(), io.StringIO()
        with patch("authority_matcher.catalog.requests.Session.get", side_effect=[empty, related]), contextlib.redirect_stdout(output), contextlib.redirect_stderr(diagnostics):
            self.assertEqual(main(["Schenkung anmelden", "--keyword-only"]), 2)
        self.assertEqual(output.getvalue(), "")
        self.assertIn("Anliegen nicht bestätigt", diagnostics.getvalue())

    def test_live_request_contract_and_pagination(self):
        session = Mock()
        first, second = Mock(), Mock()
        first.json.return_value = {"serviceDescriptions": {"last": False, "content": [{"id": SID, "name": "Hund anmelden"}]}}
        second.json.return_value = {"serviceDescriptions": {"last": True, "content": [{"id": "X.2", "name": "Hund abmelden"}]}}
        session.get.side_effect = [first, second]
        catalog = LiveCatalog(self.config, session=session)
        items, incomplete = catalog.search("hund anmelden", ARS)
        self.assertFalse(incomplete)
        self.assertEqual(len(items), 2)
        params = session.get.call_args_list[0].kwargs["params"]
        self.assertGreaterEqual(params["size"], 5)
        self.assertEqual(params["q"], "hund anmelden")
        self.assertEqual(params["useArsHierarchy"], "true")
        self.assertEqual(session.get.call_args_list[1].kwargs["params"]["page"], 1)

    def test_invalid_responses_and_wrong_detail_identity(self):
        for data in [[], {}, {"serviceDescriptions": {"last": "false", "content": []}},
                     {"serviceDescriptions": {"last": True, "content": [{"id": "../bad", "name": "bad"}]}},
                     {"serviceDescriptions": {"last": True, "content": [{"id": SID, "name": "bad", "synonyms": "not-a-list"}]}}]:
            session = Mock(); session.get.return_value.json.return_value = data
            with self.assertRaises(MatcherError):
                LiveCatalog(self.config, session=session).search("hund anmelden", ARS)
        session = Mock(); session.get.return_value.json.return_value = {"id": "X.wrong", "name": "wrong", "organisations": []}
        with self.assertRaises(MatcherError):
            LiveCatalog(self.config, session=session).detail(SID, ARS)

    def test_outage_is_safe_and_fallback_is_explicit(self):
        with patch("authority_matcher.catalog.requests.Session.get", side_effect=requests.ConnectionError("secret-password")):
            result = AuthorityMatcher().match("Hund anmelden")
            self.assertEqual(result["status"], "pvog_unavailable")
            self.assertNotIn("secret-password", json.dumps(result))
            config = {**self.config, "allow_snapshot_fallback": True}
            result = AuthorityMatcher(config=config).match("Hund anmelden")
            self.assertEqual(result["status"], "matched_snapshot")

    def test_detail_outage_can_use_same_id_from_explicit_fallback(self):
        listing = Mock()
        listing.json.return_value = {"serviceDescriptions": {"last": True, "content": [{"id": SID, "name": "Hundehaltung anmelden"}]}}
        with patch("authority_matcher.catalog.requests.Session.get", side_effect=[listing, requests.ConnectionError()]):
            result = AuthorityMatcher(config={**self.config, "allow_snapshot_fallback": True}).match("Hund anmelden")
        self.assertEqual(result["status"], "matched_snapshot")

    def test_snapshot_integrity_expiry_and_region(self):
        source = json.loads(Path(self.config["snapshot_file"]).read_text())
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "snapshot.json"
            config = {**self.config, "snapshot_file": str(path)}
            corrupted = copy.deepcopy(source)
            corrupted["services"][0]["detail"]["name"] = "Modified"
            path.write_text(json.dumps(corrupted))
            with self.assertRaises(MatcherError) as caught:
                SnapshotCatalog(config)
            self.assertEqual(caught.exception.code, "invalid_source")
            old = copy.deepcopy(source)
            old["retrieved_at"] = (self.test_now - timedelta(days=40)).isoformat()
            path.write_text(json.dumps(old))
            with self.assertRaises(MatcherError) as caught:
                SnapshotCatalog(config)
            self.assertEqual(caught.exception.code, "snapshot_expired")
        result = AuthorityMatcher().match("Hund anmelden", ars="110000000000", offline=True)
        self.assertEqual(result["status"], "snapshot_region_uncovered")

    def test_snapshot_export_never_overwrites_and_excludes_hidden_organisations(self):
        detail = {"id": SID, "name": "Hund anmelden", "leikakeys": [], "organisations": [organisation(), organisation("X.hidden", isPublicDisplayAllowed=False)]}
        fake = Mock()
        fake.search.return_value = ([{"id": SID, "name": "Hund anmelden", "synonyms": ["Hund registrieren"]}], False)
        fake.detail.return_value = detail
        fake.evidence = [{"url": "https://example.invalid/official-fixture", "retrieved_at": datetime.now(timezone.utc).isoformat()}]
        with tempfile.TemporaryDirectory() as directory, patch("authority_matcher.catalog.LiveCatalog", return_value=fake):
            path = Path(directory) / "export.json"
            self.assertEqual(export_snapshot(self.config, "hund anmelden", ARS, path), 1)
            saved = json.loads(path.read_text())
            self.assertEqual(len(saved["services"][0]["detail"]["organisations"]), 1)
            self.assertEqual(saved["services"][0]["summary"]["synonyms"], ["Hund registrieren"])
            self.assertTrue(saved["search_evidence"])
            with self.assertRaises(MatcherError):
                export_snapshot(self.config, "hund anmelden", ARS, path)


class RealExtractAndCLITests(SnapshotTimeTests):
    def test_coverage_report_is_bounded_traceable_and_never_guesses_categories(self):
        with patch("requests.Session.get", side_effect=AssertionError("Coverage must not use network")):
            report = coverage_report()
        self.assertEqual(report["scope"], "bounded_snapshot_only")
        self.assertGreater(report["unmapped_organisation_count"], 0)
        self.assertLess(report["services_with_all_organisations_categorised"], report["service_count"])
        for org in report["unmapped_organisations"]:
            self.assertTrue(org["service_ids"])
            self.assertTrue(org["evidence_url"].startswith("https://pvog.fitko.net/"))
            self.assertNotIn("category", org)
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            self.assertEqual(main(["coverage", "--json"]), 0)
        self.assertEqual(json.loads(output.getvalue())["unmapped_organisation_count"], report["unmapped_organisation_count"])

    def test_official_extract_examples_without_any_network(self):
        with patch("requests.Session.get", side_effect=AssertionError("Offline must not use network")):
            matcher = AuthorityMatcher()
            for query, code in [("Ich möchte meinen Hund anmelden", "KOMMUNALE_STEUERVERWALTUNG"),
                                ("Ich möchte mein Auto anmelden", "KFZ_ZULASSUNGSBEHOERDE"),
                                ("Schenkungsteuerbescheid erhalten", "FINANZAMT"),
                                ("Erbschaftsteuerpflichtigen Erwerb anzeigen", "FINANZAMT"),
                                ("Ich möchte einen Personalausweis beantragen", "BUERGERSERVICE"),
                                ("Ich möchte einen Reisepass beantragen", "BUERGERSERVICE"),
                                ("Ich möchte mich arbeitslos melden", "BUNDESAGENTUR_FUER_ARBEIT")]:
                with self.subTest(query=query):
                    result = matcher.match(query, offline=True)
                    self.assertEqual(result["status"], "matched_snapshot")
                    self.assertEqual(result["category"]["code"], code)
                    self.assertTrue(result["evidence"])
            self.assertEqual(matcher.match("Gewerbe anmelden", offline=True)["status"], "ambiguous_category")
            self.assertEqual(matcher.match("Hund", offline=True)["status"], "ambiguous_service")

    def test_cli_json_and_keyword_output(self):
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            exit_code = main(["Ich möchte meinen Hund anmelden", "--offline", "--json"])
        self.assertEqual(exit_code, 2)
        self.assertEqual(json.loads(output.getvalue())["category"]["code"], "KOMMUNALE_STEUERVERWALTUNG")
        output, diagnostics = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(diagnostics):
            self.assertEqual(main(["Hund anmelden", "--offline", "--keyword-only"]), 2)
        self.assertEqual(output.getvalue(), "KOMMUNALE_STEUERVERWALTUNG\n")
        self.assertIn("Offline", diagnostics.getvalue())

    def test_configuration_requires_evidence_and_does_not_print_secrets(self):
        config = load_config()
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "config.json"
            path.write_text(json.dumps({**config, "pvog_url": "https://user:secret@example.invalid"}))
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                self.assertEqual(main(["Hund anmelden", "--config", str(path), "--json"]), 1)
            self.assertNotIn("secret", output.getvalue())
            self.assertEqual(json.loads(output.getvalue())["status"], "invalid_config")
            path.write_text(json.dumps({"format_version": 1, "categories": [{"code": "TEST", "label": "Test", "organisations": [{"id": "X.1", "ars": ARS}]}]}))
            with self.assertRaises(MatcherError):
                CategoryRegistry(path)


if __name__ == "__main__":
    unittest.main()
