"""Statutory category mappings onto already imported authority polygons.

Example: in Rhineland-Palatinate the town administration of a große
kreisangehörige Stadt is the lower building authority. If no own
``bauamt`` polygon covers the point, the explicit ``stadtverwaltung``
polygon of that town is returned, clearly marked as a statutory mapping.
"""
import json
from functools import lru_cache
from .catalog import resource


@lru_cache(maxsize=1)
def rules():
    return json.loads(resource('category_mappings.json').read_text(encoding='utf-8'))['rules']


def rules_for(category_id):
    return [r for r in rules() if category_id in r['targets']]


def rule_applies(rule, category_id, municipality):
    """municipality: VG25 Gemeinde properties at the point (ags, lkz, bem) or None."""
    if rule['scope'] != 'all':
        if not municipality or municipality.get('lkz') not in rule['scope']:
            return False
    flt = rule.get('municipality_filter') or {}
    if not flt:
        return True
    if not municipality:
        return False
    if 'ags' in flt and municipality.get('ags') not in flt['ags']:
        return False
    if 'vg25_bem' in flt and municipality.get('bem') not in flt['vg25_bem']:
        return False
    excluded = (flt.get('exclude_ags_for') or {}).get(category_id, [])
    if municipality.get('ags') in excluded:
        return False
    return True


def annotate(match, rule, category_id):
    return {**match, 'category': category_id, 'mapped_from_category': match['category'],
            'match_type': 'statutory_category_mapping', 'territorial_match_verified': True,
            'case_competence_verified': False,
            'mapping': {'rule_id': rule['id'], 'base_category': rule['base_category'],
                        'evidence_level': rule['evidence_level'], 'legal_basis': rule['legal_basis'],
                        'legal_reference': rule['legal_reference'], 'short_reference': rule['short_reference'], 'note': rule['note']}}


def resolve_mapped(category_id, lon, lat, dsn=None, db=None):
    """Return (rule, matches) of the first applicable rule with matches, else (None, [])."""
    candidates = rules_for(category_id)
    if not candidates:
        return None, []
    if db is None:
        from . import db
    municipality = db.municipality_at(lon, lat, dsn)
    base_cache = {}
    for rule in candidates:
        if not rule_applies(rule, category_id, municipality):
            continue
        base = rule['base_category']
        if base not in base_cache:
            base_cache[base] = db.resolve(base, lon, lat, dsn).get('matches', [])
        rows = [m for m in base_cache[base] if m['source_id'] in rule['base_sources']]
        if not rows:
            continue
        # Prefer the first listed source so one authority is not returned twice.
        best = min(rule['base_sources'].index(m['source_id']) for m in rows)
        rows = [annotate(m, rule, category_id) for m in rows
                if rule['base_sources'].index(m['source_id']) == best]
        return rule, rows
    return None, []


def mapping_warning(rule, category_label, base_label):
    return (f"Keine eigene Fläche für {category_label} integriert. Gesetzliche Zuordnung "
            f"({rule['short_reference']}): Diese Aufgabe nimmt hier die {base_label} wahr; angezeigt werden "
            f"deren Fläche und Anschrift. {rule['note']} Fachbereich und Einzelfallzuständigkeit bitte bei der "
            f"Behörde prüfen.")
