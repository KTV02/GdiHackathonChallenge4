"""Query -> service -> official organisations -> explicitly local authority categories.

No database, GeoServer, geocoding, web framework or terminal dependency.
"""

import re

from .catalog import ID, LiveCatalog, SnapshotCatalog, records
from .categories import CategoryRegistry
from .config import load_config
from .errors import MatcherError
from .text import ACTIONS, normalize_query, plain, rank_services, related_subject_query, tokens

ROLE_URI = "urn:xoev-de:fim:codeliste:xzufi.zustaendigkeitsrolle"


class AuthorityMatcher:
    def __init__(self, config_path=None, *, config=None, catalog=None):
        self.config = config if config is not None else load_config(config_path)
        self.registry = CategoryRegistry(self.config["categories_file"])
        self.catalog = catalog

    def match(self, query, *, ars=None, service_id=None, offline=False):
        """Return a serializable result. Missing evidence is a result, never a guess.

        `ars` is a search reference region, not a geospatial jurisdiction claim.
        Callers can render ambiguous service candidates and repeat with service_id.
        """
        result = {"status": "invalid_query", "category": None, "categories": [], "organisations": [],
                  "service": None, "candidates": [], "related_candidates": [], "warnings": [], "evidence": [],
                  "category_system": "local_normalization", "jurisdiction_resolved": False}
        try:
            if not isinstance(query, str) or not 2 <= len(query.strip()) <= 500:
                raise MatcherError("invalid_query", "Bitte ein Anliegen mit 2 bis 500 Zeichen eingeben.")
            prepared = normalize_query(query)
            if not prepared or not any(t not in ACTIONS for t in tokens(prepared)):
                raise MatcherError("invalid_query", "Bitte den Gegenstand des Anliegens nennen, etwa Hund oder Reisepass.")
            ars = self.config["reference_ars"] if ars is None else ars
            if not isinstance(ars, str) or not re.fullmatch(r"[0-9]{12}", ars):
                raise MatcherError("invalid_ars", "Bitte einen zwölfstelligen Gemeinde-ARS angeben.")
            if service_id is not None and (not isinstance(service_id, str) or not ID.fullmatch(service_id)):
                raise MatcherError("invalid_service_id", "Ungültige Leistungskennung.")
            result.update(query=plain(query), normalized_query=prepared, reference_ars=ars,
                          reference_region_name=self.config.get("reference_region_name", "") if ars == self.config["reference_ars"] else "",
                          geographic_scope="reference_region_only")
            result["warnings"].append("Kategorien gelten für die belegte Referenzregion; keine bundesweite Zuständigkeitsaussage.")
            catalog = self.catalog or (SnapshotCatalog(self.config) if offline else LiveCatalog(self.config))
            try:
                items, incomplete = catalog.search(prepared, ars)
            except MatcherError as error:
                if offline or self.catalog or error.code != "pvog_unavailable" or not self.config.get("allow_snapshot_fallback", False):
                    raise
                catalog = SnapshotCatalog(self.config)
                items, incomplete = catalog.search(prepared, ars)
                result["warnings"].append("PVOG nicht erreichbar: ausdrücklich konfigurierter Offline-Auszug verwendet.")
            result["data_mode"] = catalog.mode
            result["evidence"] = list(catalog.evidence)
            if catalog.mode == "snapshot":
                result["snapshot_retrieved_at"] = catalog.retrieved_at
                result["warnings"].append("Begrenzter Offline-Auszug; keine Prüfung gegen den aktuellen PVOG-Gesamtbestand.")
            ranked = rank_services(prepared, items)
            result["candidates"] = [i for i in ranked if i["score"] >= 0.45][:10]
            result["search_summary"] = {"source_hits": len(items), "plausible_hits": sum(i["score"] >= 0.45 for i in ranked),
                                        "incomplete": incomplete}
            if not result["candidates"]:
                message = ("Der abgefragte Bestand liefert keine Leistung zu diesem Suchbegriff." if not items else
                           f"Der abgefragte Bestand liefert {len(items)} Treffer, aber keiner besteht die lokale Plausibilitätsprüfung.")
                subject = related_subject_query(prepared)
                if not items and not incomplete and subject and self.config.get("suggest_related_on_no_match", True):
                    # A broader search can suggest services, but dropping a verb
                    # must never turn another procedure into a confirmed match.
                    try:
                        related, related_incomplete = catalog.search(subject, ars)
                        result["related_query"] = subject
                        result["search_summary"].update(related_source_hits=len(related), related_incomplete=related_incomplete)
                        result["related_candidates"] = [{**item, "match_scope": "subject_only", "request_match_verified": False}
                            for item in rank_services(subject, related) if item["score"] >= 0.45][:10]
                        result["evidence"] = list(catalog.evidence)
                        if result["related_candidates"]:
                            message = "Keine Leistung für die vollständige Anfrage gefunden. Verwandte Treffer zum Sachbegriff sind Vorschläge; die gewünschte Handlung ist nicht bestätigt."
                    except MatcherError as error:
                        result["warnings"].append(f"Die zusätzliche Suche nach verwandten Leistungen war nicht verfügbar ({error.code}).")
                raise MatcherError("no_match", message)
            selected = None
            if service_id:
                selected = next((item for item in ranked if item["id"] == service_id and item["score"] >= 0.45), None)
                if selected is None:
                    raise MatcherError("invalid_service_selection", "Diese Leistung gehört nicht zu den plausiblen Treffern der Anfrage.")
                result["selection_method"] = "explicit_service_id"
            else:
                top = ranked[0]
                second = ranked[1]["score"] if len(ranked) > 1 else 0
                unique_title = top["exact_title"] and not any(item["exact_title"] for item in ranked[1:])
                unique_exact = unique_title or (top["exact_label"] and not any(item["exact_label"] for item in ranked[1:]))
                specific_request = len(tokens(prepared)) >= 2 or unique_title
                if not incomplete and specific_request and top["score"] >= 0.85 and top["label_coverage"] and (unique_exact or top["score"] - second >= 0.10):
                    selected = top
                    result["selection_method"] = "lexical_match"
            if selected is None:
                message = "Mehrere oder nur schwach passende Leistungen: bitte präzisieren oder eine Leistungs-ID auswählen."
                if incomplete:
                    message = "PVOG-Trefferliste unvollständig: Suchbegriff eingrenzen oder eine angezeigte Leistungs-ID auswählen."
                raise MatcherError("ambiguous_service", message)
            result["service"] = selected
            try:
                detail = catalog.detail(selected["id"], ars)
            except MatcherError as error:
                if catalog.mode != "live" or self.catalog or error.code != "pvog_unavailable" or not self.config.get("allow_snapshot_fallback", False):
                    raise
                catalog = SnapshotCatalog(self.config)
                detail = catalog.detail(selected["id"], ars)
                result.update(data_mode="snapshot", snapshot_retrieved_at=catalog.retrieved_at)
                result["warnings"].append("PVOG-Details nicht erreichbar: begrenzter Offline-Auszug verwendet; kein aktueller Gesamtdatenbestand.")
            keys = detail.get("leikakeys", [])
            if not isinstance(keys, list) or not all(isinstance(key, str) for key in keys):
                raise MatcherError("invalid_source", "Unerwartetes Format der LeiKa-Schlüssel.")
            result["service"]["leika_keys"] = keys
            result["evidence"] = list(catalog.evidence)
            all_orgs = records(detail.get("organisations"), "organisations")
            public = [o for o in all_orgs if o.get("isPublicDisplayAllowed") is not False]
            if not public:
                raise MatcherError("no_association", "PVOG enthält keine öffentlich verwendbare Organisation-Verknüpfung zu dieser Leistung.")
            competent = []
            for org in public:
                role = org.get("role")
                if isinstance(role, dict) and role.get("uri") == ROLE_URI and role.get("code") in {"01", "02"}:
                    competent.append(org)
            if not competent:
                raise MatcherError("no_competent_role", "Nur Ansprechpunkte oder unklare Rollen vorhanden; keine belegte zuständige Stelle.")
            categories, unmapped, conditional = {}, [], False
            for org in competent:
                org_id = org.get("id")
                if not isinstance(org_id, str) or not ID.fullmatch(org_id) or not isinstance(org.get("title"), str):
                    raise MatcherError("invalid_source", "Ungültige Organisation-Verknüpfung.")
                criteria = records(org.get("criteria"), "organisation.criteria")
                conditional |= bool(criteria)
                mapped = self.registry.for_organisation(org_id, ars)
                result["organisations"].append({"id": org_id, "name": plain(org["title"]), "role": org["role"],
                                                "criteria": criteria, "category_codes": [c["code"] for c in mapped]})
                if not mapped:
                    unmapped.append(org_id)
                for category in mapped:
                    categories[category["code"]] = category
            result["categories"] = list(categories.values())
            if conditional:
                raise MatcherError("criteria_required", "Die offiziellen Verknüpfungen haben zusätzliche Zuständigkeitskriterien; Kategorie noch nicht eindeutig bestätigt.")
            if unmapped:
                result["unmapped_organisation_ids"] = unmapped
                raise MatcherError("category_unmapped", "Offizielle Organisation gefunden, aber noch keine belegte lokale Kategorienzuordnung hinterlegt.")
            if len(categories) != 1:
                raise MatcherError("ambiguous_category", "Die Leistung ist offiziell mit mehreren Behördenkategorien verknüpft.")
            result["category"] = next(iter(categories.values()))
            result["status"] = "matched_snapshot" if catalog.mode == "snapshot" else "matched"
            result["message"] = "Behördenkategorie aus offizieller Leistungs-Organisation-Verknüpfung und lokaler Normalisierung ermittelt."
        except MatcherError as error:
            result["status"], result["message"] = error.code, error.message
        return result
