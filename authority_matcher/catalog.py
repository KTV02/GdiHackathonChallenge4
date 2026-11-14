"""Official PVOG search and bounded, explicitly dated offline extracts."""

import hashlib
import json
import os
import re
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import quote, urlencode, urlparse

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from .config import read_json
from .errors import MatcherError
from .text import plain, service_synonyms

ID = re.compile(r"[A-Za-z0-9_.-]{1,180}\Z")


def records(value, field):
    if not isinstance(value, list) or not all(isinstance(x, dict) for x in value):
        raise MatcherError("invalid_source", f"Unerwartetes Datenformat: {field}.")
    return value


def check_service(item):
    if not isinstance(item.get("id"), str) or not ID.fullmatch(item["id"]) or not isinstance(item.get("name"), str) or not item["name"].strip():
        raise MatcherError("invalid_source", "Leistungskennung oder Leistungstitel fehlt.")
    service_synonyms(item)
    return item


class LiveCatalog:
    mode = "live"

    def __init__(self, config, session=None):
        self.config = config
        self.session = session or requests.Session()
        if session is None:
            retry = Retry(total=1, backoff_factor=0.3, status_forcelist=[502, 503, 504], allowed_methods=["GET"])
            self.session.mount("https://", HTTPAdapter(max_retries=retry))
        self.evidence = []

    def _get(self, path, params):
        url = self.config["pvog_url"].rstrip("/") + path
        headers = {"User-Agent": "GDI-AuthorityCategoryMatcher/1.0", "Accept": "application/json"}
        # Credentials are read only from the environment, never saved in snapshots.
        token = os.getenv("PVOG_BEARER_TOKEN")
        if token:
            headers["Authorization"] = f"Bearer {token}"
        try:
            response = self.session.get(url, params=params, headers=headers, timeout=self.config["timeout_seconds"])
            response.raise_for_status()
            data = response.json()
        except requests.RequestException:
            raise MatcherError("pvog_unavailable", "PVOG ist nicht erreichbar oder hat die Anfrage abgewiesen.") from None
        except ValueError:
            raise MatcherError("invalid_source", "PVOG liefert kein gültiges JSON.") from None
        if not isinstance(data, dict):
            raise MatcherError("invalid_source", "PVOG liefert kein JSON-Objekt.")
        self.evidence.append({"url": url + "?" + urlencode(params), "retrieved_at": datetime.now(timezone.utc).isoformat(),
                              "sha256": hashlib.sha256(json.dumps(data, sort_keys=True, ensure_ascii=False).encode()).hexdigest()})
        return data

    def search(self, query, ars):
        items = {}
        for page in range(self.config["max_pages"]):
            data = self._get(f"/v7/servicedescriptions/{ars}", {
                "q": query, "size": self.config["page_size"], "page": page,
                "useArsHierarchy": "true", "fuzzysearch": "false"})
            listing = data.get("serviceDescriptions")
            if not isinstance(listing, dict) or type(listing.get("last")) is not bool:
                raise MatcherError("invalid_source", "PVOG-Suchantwort enthält keine gültige Seitennavigation.")
            for item in records(listing.get("content"), "serviceDescriptions.content"):
                check_service(item)
                items[item["id"]] = item
            if listing["last"]:
                return list(items.values()), False
        return list(items.values()), True

    def detail(self, service_id, ars):
        data = check_service(self._get(f"/v6/servicedescriptions/{quote(service_id, safe='')}/detail",
                                      {"ars": ars, "useArsHierarchy": "true"}))
        if data["id"] != service_id:
            raise MatcherError("invalid_source", "PVOG-Detailkennung stimmt nicht mit der gewählten Leistung überein.")
        records(data.get("organisations"), "organisations")
        return data


class SnapshotCatalog:
    mode = "snapshot"

    def __init__(self, config):
        data = read_json(config["snapshot_file"])
        try:
            if data["format_version"] != 1 or not isinstance(data["ars"], str) or not re.fullmatch(r"[0-9]{12}", data["ars"]):
                raise ValueError
            fetched = datetime.fromisoformat(data["retrieved_at"])
            if fetched.tzinfo is None:
                raise ValueError
            age = (datetime.now(timezone.utc) - fetched).total_seconds() / 86400
            if age < -1 or age > config["snapshot_max_age_days"]:
                raise MatcherError("snapshot_expired", "Der Offline-Auszug ist abgelaufen; bitte mit snapshot neu abrufen.")
            self.items = records(data["services"], "snapshot.services")
            self.entries = {}
            for entry in self.items:
                summary = check_service(entry["summary"])
                detail = check_service(entry["detail"])
                if summary["id"] != detail["id"] or plain(summary["name"]) != plain(detail["name"]) or summary["id"] in self.entries:
                    raise ValueError
                records(detail.get("organisations"), "snapshot.organisations")
                evidence = entry["evidence"]
                parsed = urlparse(evidence["url"])
                if parsed.scheme not in {"https", "http"} or not parsed.netloc or parsed.username:
                    raise ValueError
                digest = hashlib.sha256(json.dumps(detail, sort_keys=True, ensure_ascii=False).encode()).hexdigest()
                if digest != evidence["sha256"]:
                    raise MatcherError("invalid_source", "Prüfsumme des Offline-Auszuges stimmt nicht.")
                self.entries[summary["id"]] = entry
            self.ars = data["ars"]
            self.retrieved_at = fetched.isoformat()
            self.evidence = []
        except (KeyError, TypeError, ValueError, AttributeError):
            raise MatcherError("invalid_source", "Offline-Auszug ist unvollständig oder ungültig.") from None

    def _check_region(self, ars):
        if ars != self.ars:
            raise MatcherError("snapshot_region_uncovered", "Der Offline-Auszug deckt diesen Referenz-ARS nicht ab.")

    def search(self, query, ars):
        self._check_region(ars)
        # The lexical matcher searches all records in this deliberately small extract.
        return [entry["summary"] for entry in self.items], False

    def detail(self, service_id, ars):
        self._check_region(ars)
        entry = self.entries.get(service_id)
        if entry is None:
            raise MatcherError("no_match", "Leistung nicht im Offline-Auszug enthalten.")
        self.evidence.append(entry["evidence"])
        return entry["detail"]


def export_snapshot(config, query, ars, output, service_id=None):
    """Export real public service data and relation fields, without people or secrets."""
    catalog = LiveCatalog(config)
    items, truncated = catalog.search(query, ars)
    if truncated:
        raise MatcherError("incomplete_search", "Suche unvollständig; Suchbegriff eingrenzen oder max_pages erhöhen.")
    if service_id:
        items = [item for item in items if item["id"] == service_id]
    if not items:
        raise MatcherError("no_match", "Keine Leistungen zum Exportieren gefunden.")
    search_evidence = list(catalog.evidence)
    exported = []
    for item in items:
        detail = catalog.detail(item["id"], ars)
        # Save only the public relation information needed for category matching.
        detail = {k: detail[k] for k in ("id", "name", "leikakeys", "organisations", "externalLastUpdate") if k in detail}
        detail["organisations"] = [o for o in detail["organisations"] if o.get("isPublicDisplayAllowed") is not False]
        evidence = {**catalog.evidence[-1], "sha256": hashlib.sha256(json.dumps(detail, sort_keys=True, ensure_ascii=False).encode()).hexdigest()}
        exported.append({"summary": {**{k: plain(item.get(k)) for k in ("id", "name", "description", "teaser")},
                                      "synonyms": service_synonyms(item)},
                         "detail": detail, "evidence": evidence})
    data = {"format_version": 1, "ars": ars, "query": query,
            "retrieved_at": datetime.now(timezone.utc).isoformat(), "complete_catalog": False,
            "coverage_note": "Nur die Treffer dieser Abfrage; kein vollständiger PVOG-Datenbestand.",
            "search_evidence": search_evidence, "services": exported}
    try:
        # Never overwrite an existing user file.
        with Path(output).open("x", encoding="utf-8") as stream:
            json.dump(data, stream, ensure_ascii=False, indent=2)
            stream.write("\n")
    except OSError:
        raise MatcherError("output_unavailable", "Ausgabedatei existiert bereits oder ist nicht beschreibbar.") from None
    return len(exported)
