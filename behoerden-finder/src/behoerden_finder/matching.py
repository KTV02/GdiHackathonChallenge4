"""Anbindung des Behörden-Matchers: Freitext -> erkannte Leistung -> Behördenkategorie.

``AuthorityMatcher`` ruft die Funktionen aus ``authority_matcher.autorun`` in derselben
Reihenfolge auf wie die Matcher-CLI (``authority-matcher``), ohne den Matcher zu verändern.
Der Elasticsearch-Check und der Indexaufbau laufen einmal beim ersten Aufruf, nicht pro Anfrage.
"""
from __future__ import annotations

from dataclasses import dataclass, field


class MatcherUnavailable(RuntimeError):
    """behoerden-matcher ist nicht installiert oder nicht startklar."""


@dataclass
class MatchOutcome:
    status: str                     # Status des Matchers, z. B. 'vorschlag', 'mehrdeutig', 'keine_zuordnung'
    category: str | None            # Kategorie, mit der weitergearbeitet wird
    score: float | None
    service: dict | None            # erkannte Leistung: titel, leistungsschluessel, score
    candidates: list[dict] = field(default_factory=list)  # Rangliste [{'id', 'score'}], beste zuerst
    ambiguous: bool = False
    raw: dict = field(default_factory=dict)


def _service_summary(raw: dict, category: str | None) -> dict | None:
    services = raw.get("leistungen") or []
    chosen = None
    if category:
        chosen = next((s for s in services if category in (s.get("behoerdenkategorien") or [])), None)
    chosen = chosen or (services[0] if services else None)
    if not chosen:
        return None
    return {"titel": chosen.get("titel"), "leistungsschluessel": chosen.get("leistungsschluessel"),
            "score": chosen.get("score")}


def interpret_match(raw: dict, *, accept_ambiguous: bool = False) -> MatchOutcome:
    """Ergebnis von ``match_authority`` in ein einheitliches Objekt umsetzen.

    Bei ``status == 'mehrdeutig'`` liefert der Matcher bewusst keine Kategorie. Nur wenn
    ``accept_ambiguous`` gesetzt ist, wird die bestplatzierte Kategorie übernommen.
    """
    status = raw.get("status") or "unbekannt"
    category = raw.get("behoerdenkategorie")
    top = raw.get("top_kategorie")
    ambiguous = category is None and status == "mehrdeutig" and top is not None
    candidates = []
    if top is not None:
        candidates.append({"id": top, "score": raw.get("score")})
    candidates += [{"id": a.get("id"), "score": a.get("score")} for a in (raw.get("alternativen") or [])]
    if category is None and ambiguous and accept_ambiguous:
        category = top
        status = "mehrdeutig_beste_gewaehlt"
    return MatchOutcome(
        status=status,
        category=category,
        score=raw.get("score") if (category is not None or ambiguous) else None,
        service=_service_summary(raw, category if category is not None else top),
        candidates=candidates,
        ambiguous=ambiguous,
        raw=raw,
    )


class AuthorityMatcher:
    """Lazy Adapter um ``authority_matcher.autorun`` (FIM + Elasticsearch + PVOG)."""

    def __init__(self, *, semantic: bool = True, live: bool = True, reference_ars: str | None = None,
                 live_top: int = 3, start_docker: bool = True, accept_ambiguous: bool = False):
        if not 1 <= live_top <= 10:
            raise ValueError("--live-top muss zwischen 1 und 10 liegen")
        self.semantic, self.live = semantic, live
        self.reference_ars, self.live_top = reference_ars, live_top
        self.start_docker, self.accept_ambiguous = start_docker, accept_ambiguous
        self._am = None
        self._taxonomy = None
        self._regions = None

    def _ensure(self) -> None:
        if self._am is not None:
            return
        try:
            from authority_matcher import autorun as am
            from authority_matcher.catalog import AuthorityTaxonomy
        except ImportError as exc:
            raise MatcherUnavailable(
                "behoerden-matcher ist in dieser Python-Umgebung nicht installiert "
                "(pip install -e '../behoerden-matcher[elastic,semantic]')."
            ) from exc
        if not am.DB.exists():
            raise MatcherUnavailable(
                f"FIM-Datenbank des Matchers fehlt: {am.DB}. Vorhandenen Download nicht löschen; "
                "ggf. AUTHORITY_MATCHER_HOME setzen."
            )
        # Elasticsearch erreichbar machen und den Index nur bei Datenänderung neu aufbauen.
        am.get_index_docs(semantic=self.semantic, force=False, no_start_docker=not self.start_docker)
        self._taxonomy = AuthorityTaxonomy(am.DEFAULT_TAXONOMY)
        self._regions = am.reference_regions(self.reference_ars)
        self._am = am

    def match(self, anliegen: str) -> MatchOutcome:
        self._ensure()
        am = self._am
        enricher = None
        if self.live:
            enricher = am.make_enricher(am.DB, self._taxonomy, regions=self._regions,
                                        max_keys=self.live_top, trace=[])
        raw = am.match_authority(anliegen, backend="elasticsearch", es_url=am.ES_URL, es_index=am.INDEX,
                                 semantic=self.semantic, embedding_model=am.MODEL, enrich=enricher)
        return interpret_match(raw, accept_ambiguous=self.accept_ambiguous)
