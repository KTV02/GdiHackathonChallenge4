"""A local category vocabulary, separate from the official PVOG relation data."""

import re
from urllib.parse import urlparse

from .catalog import ID, records
from .config import read_json
from .errors import MatcherError


class CategoryRegistry:
    def __init__(self, path):
        data = read_json(path)
        if not isinstance(data, dict) or data.get("format_version") != 1:
            raise MatcherError("invalid_config", "Ungültiges Kategorieverzeichnis.")
        self.categories = records(data.get("categories"), "categories")
        self.index = {}
        seen_codes = set()
        for category in self.categories:
            code, label = category.get("code"), category.get("label")
            if not isinstance(code, str) or not re.fullmatch(r"[A-Z][A-Z0-9_]{1,79}", code) or code in seen_codes or not isinstance(label, str) or not label.strip():
                raise MatcherError("invalid_config", "Kategorien brauchen eindeutige Codes und Bezeichnungen.")
            seen_codes.add(code)
            for member in records(category.get("organisations"), "category.organisations"):
                org_id, ars, evidence = member.get("id"), member.get("ars"), member.get("evidence_url")
                parsed = urlparse(evidence) if isinstance(evidence, str) else None
                if not isinstance(org_id, str) or not ID.fullmatch(org_id) or not isinstance(ars, str) or not re.fullmatch(r"[0-9]{12}", ars) or not parsed or parsed.scheme not in {"https", "http"} or not parsed.netloc or parsed.username:
                    raise MatcherError("invalid_config", "Jede Kategorienzuordnung braucht OE-ID, Referenz-ARS und eine Beleg-URL.")
                self.index.setdefault((org_id, ars), []).append({"code": code, "label": label, "evidence_url": evidence})

    def for_organisation(self, org_id, ars):
        return self.index.get((org_id, ars), [])
