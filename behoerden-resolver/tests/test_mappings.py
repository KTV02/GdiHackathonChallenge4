import json
import pytest
from behoerden_resolver import mappings
from behoerden_resolver.catalog import categories, resource, source


class StubDB:
    def __init__(self, municipality, matches):
        self.municipality = municipality
        self.matches = matches
        self.calls = []

    def municipality_at(self, lon, lat, dsn=None):
        return self.municipality

    def resolve(self, category, lon, lat, dsn=None):
        self.calls.append(category)
        return {'matches': [m for m in self.matches if m['category'] == category]}


def area(name, category, source_id):
    return {'authority_name': name, 'category': category, 'source_id': source_id, 'source_key': name,
            'properties': {}, 'geometry': {'type': 'MultiPolygon', 'coordinates': []}}


def test_rules_reference_existing_categories_and_sources():
    ids = {c['id'] for c in categories()}
    raw = json.loads(resource('category_mappings.json').read_text(encoding='utf-8'))
    assert raw['rules']
    for r in mappings.rules():
        assert set(r['targets']) <= ids and r['base_category'] in ids
        assert r['base_category'] not in r['targets']
        for sid in r['base_sources']:
            assert any(route['category'] == r['base_category'] for route in source(sid)['routes'])
        assert r['legal_basis'].startswith('https://') and r['legal_reference'] and r['short_reference']
        assert r['evidence_level'] in ('norm_verified', 'statutory_general_rule')


def test_rp_large_town_is_building_authority():
    stub = StubDB({'ags': '07138045', 'lkz': 'RP', 'bem': '--'},
                  [area('Stadtverwaltung Neuwied, Stadt', 'stadtverwaltung', 'gemfa_vg25')])
    rule, rows = mappings.resolve_mapped('bauamt', 7.46, 50.43, db=stub)
    assert rule['id'] == 'rp_grosse_kreisangehoerige_staedte_bauaufsicht'
    assert rows[0]['category'] == 'bauamt' and rows[0]['mapped_from_category'] == 'stadtverwaltung'
    assert rows[0]['match_type'] == 'statutory_category_mapping' and rows[0]['case_competence_verified'] is False


def test_ordinary_rp_municipality_gets_no_building_authority():
    stub = StubDB({'ags': '07331003', 'lkz': 'RP', 'bem': '--'},
                  [area('Stadtverwaltung Alzey, Stadt', 'stadtverwaltung', 'gemfa_vg25')])
    assert mappings.resolve_mapped('bauamt', 8.11, 49.75, db=stub) == (None, [])


def test_kreisfreie_stadt_and_source_preference():
    stub = StubDB({'ags': '09161000', 'lkz': 'BY', 'bem': 'kreisfrei'},
                  [area('Stadt Ingolstadt', 'stadtverwaltung', 'by_kreis'),
                   area('Stadtverwaltung Ingolstadt', 'stadtverwaltung', 'gemfa_vg25')])
    rule, rows = mappings.resolve_mapped('zulassungsstelle', 11.42, 48.76, db=stub)
    assert rule['id'] == 'kreisfreie_stadt' and [r['source_id'] for r in rows] == ['by_kreis']


def test_health_office_of_kreisfreie_stadt_is_not_guessed():
    stub = StubDB({'ags': '09562000', 'lkz': 'BY', 'bem': 'kreisfrei'},
                  [area('Stadt Erlangen', 'stadtverwaltung', 'by_kreis')])
    assert mappings.resolve_mapped('gesundheitsamt', 11.0, 49.6, db=stub) == (None, [])


def test_landkreis_rule_and_schwedt_exception():
    lra = area('Landkreis Uckermark', 'kreisverwaltung', 'bb_kommunal_gebiete')
    town = area('Stadt Schwedt/Oder', 'stadtverwaltung', 'bb_kommunal_gebiete')
    stub = StubDB({'ags': '12073532', 'lkz': 'BB', 'bem': 'gemeinschaftsangehörig'}, [lra, town])
    rule, rows = mappings.resolve_mapped('auslaenderbehoerde', 14.28, 53.06, db=stub)
    assert rule['id'] == 'bb_schwedt_auslaenderbehoerde' and rows[0]['authority_name'] == 'Stadt Schwedt/Oder'
    rule, rows = mappings.resolve_mapped('jugendamt', 14.28, 53.06, db=stub)
    assert rule['id'] == 'bb_landkreis' and rows[0]['authority_name'] == 'Landkreis Uckermark'


def test_no_municipality_means_no_scoped_mapping():
    stub = StubDB(None, [area('Landratsamt X', 'kreisverwaltung', 'by_kreis')])
    assert mappings.resolve_mapped('jugendamt', 11, 48, db=stub) == (None, [])


def test_category_without_rules_does_not_touch_database():
    stub = StubDB(None, [])
    stub.municipality_at = lambda *a, **k: pytest.fail('no lookup without rules')
    assert mappings.resolve_mapped('krankenkasse', 8, 50, db=stub) == (None, [])


def test_resolver_prefers_statutory_mapping_over_point_fallback(monkeypatch):
    from behoerden_resolver import db, resolver
    monkeypatch.setattr(resolver, '_resolve_areas', lambda *a, **k: {'category': 'bauamt', 'status': 'context_required', 'matches': []})
    monkeypatch.setattr(db, 'municipality_at', lambda *a, **k: {'ags': '07138045', 'lkz': 'RP', 'bem': '--'})
    monkeypatch.setattr(db, 'resolve', lambda c, *a, **k: {'matches': [area('Stadtverwaltung Neuwied, Stadt', 'stadtverwaltung', 'gemfa_vg25')] if c == 'stadtverwaltung' else []})
    monkeypatch.setattr(db, 'nearby', lambda *a, **k: pytest.fail('statutory mapping has priority over nearby points'))
    r = resolver.resolve('bauamt', 7.46, 50.43)
    assert r['data_status'] == 'statutory_mapping_match' and r['fallback_used'] is False
    assert r['status'] == 'context_required' and r['mapping']['id'].startswith('rp_') and r['warning']
    assert r['matches'][0]['authority_name'] == 'Stadtverwaltung Neuwied, Stadt'
    monkeypatch.setattr(db, 'nearby', lambda *a, **k: [])
    assert resolver.resolve('bauamt', 7.46, 50.43, mapping_enabled=False)['matches'] == []
