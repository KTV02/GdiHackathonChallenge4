"""Measure coverage from the loaded snapshots without PostGIS and render docs/ABDECKUNG.md.

Reproduces scripts/report.py with numpy only: one GEOS-style interior point
(ST_PointOnSurface scan line) per VG25 AGS, tested against every authority
polygon (even-odd rule incl. holes); location states via VG25 Kreise.
Uses the newest complete snapshot per source, i.e. the state that `load`
puts into PostGIS. Validated against the PostGIS run of 2026-11-13: all
unchanged categories reproduce identical counts per state.

Usage: python scripts/measure_snapshots.py
"""
import gzip, hashlib, json, sys
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from coverage_render import ROOT, RES, add_mapping_coverage, render

root = ROOT; snapdir = root / 'data/snapshots'


def latest():
    sel = {}
    for m in sorted(snapdir.glob('*/manifest.json')):
        md = json.loads(m.read_text()); k = md['source']['id']
        if k not in sel or md['completed_at'] > sel[k][0]:
            sel[k] = (md['completed_at'], m.parent)
    return {k: v[1] for k, v in sel.items()}


snaps = latest()


def records(path):
    with gzip.open(path, 'rt', encoding='utf-8') as f:
        for line in f:
            yield json.loads(line)


def polys(g):
    if g['type'] == 'Polygon': return [g['coordinates']]
    if g['type'] == 'MultiPolygon': return g['coordinates']
    if g['type'] == 'GeometryCollection': return [p for x in g['geometries'] for p in polys(x)]
    return []


def interior_point(g):
    best = None; maxw = -1.0
    for poly in polys(g):
        rings = [np.asarray(r, dtype=float)[:, :2] for r in poly]
        if not rings or len(rings[0]) == 0: continue
        sh = rings[0]; lo, hi = sh[:, 1].min(), sh[:, 1].max(); c = (lo + hi) / 2
        for r in rings:
            y = r[:, 1]
            below = y[(y <= c) & (y > lo)]; above = y[(y > c) & (y < hi)]
            if below.size: lo = below.max()
            if above.size: hi = above.min()
        Y = (lo + hi) / 2
        xs = []
        for r in rings:
            p0, p1 = r[:-1], r[1:]
            y0, y1 = p0[:, 1], p1[:, 1]
            inter = ~(((y0 > Y) & (y1 > Y)) | ((y0 < Y) & (y1 < Y)))
            counted = (y0 != y1) & ~((y0 == Y) & (y1 < Y)) & ~((y1 == Y) & (y0 < Y))
            m = inter & counted
            a, b = p0[m], p1[m]
            with np.errstate(divide='ignore', invalid='ignore'):
                dx = b[:, 0] - a[:, 0]; dy = b[:, 1] - a[:, 1]
                x = np.where(dx == 0, a[:, 0], a[:, 0] + (Y - a[:, 1]) / (dy / dx))
            xs.extend(x.tolist())
        xs.sort(); w = 0.0; px = None
        for i in range(0, len(xs) - 1, 2):
            if xs[i + 1] - xs[i] > w: w = xs[i + 1] - xs[i]; px = (xs[i] + xs[i + 1]) / 2
        if px is None: w = 0.0; px = sh[0, 0]; Y = sh[0, 1]
        if w > maxw: maxw = w; best = (px, Y)
    return best


def prep(g):
    out = []
    for poly in polys(g):
        arr = [np.asarray(r, dtype=float)[:, :2] for r in poly]
        sh = arr[0]
        out.append((arr, (sh[:, 0].min(), sh[:, 0].max(), sh[:, 1].min(), sh[:, 1].max())))
    return out


def covers(prepared, pts):
    res = np.zeros(len(pts), bool)
    for arr, (x0b, x1b, y0b, y1b) in prepared:
        m = (pts[:, 0] >= x0b) & (pts[:, 0] <= x1b) & (pts[:, 1] >= y0b) & (pts[:, 1] <= y1b) & ~res
        idx = np.nonzero(m)[0]
        if not idx.size: continue
        inside = np.zeros(idx.size, bool); P = pts[idx]
        px = P[:, 0:1]; py = P[:, 1:2]
        for r in arr:
            x0, y0, x1, y1 = r[:-1, 0], r[:-1, 1], r[1:, 0], r[1:, 1]
            for s in range(0, len(x0), 4000):
                X0, Y0, X1, Y1 = x0[s:s+4000], y0[s:s+4000], x1[s:s+4000], y1[s:s+4000]
                cond = (Y0 > py) != (Y1 > py)
                with np.errstate(divide='ignore', invalid='ignore'):
                    xi = X0 + (py - Y0) * (X1 - X0) / (Y1 - Y0)
                inside ^= (np.count_nonzero(cond & (px < xi), axis=1) % 2).astype(bool)
        res[idx[inside]] = True
    return res



def measure():
    ref = {}; st = {}; ref_bem = {}
    for r in records(snaps['vg25_gem'] / 'records.jsonl.gz'):
        a = r['properties']['ags']
        ref.setdefault(a, []).append(r['geometry']); st[a] = max(st.get(a, ''), r['properties']['lkz']); ref_bem[a] = r['properties'].get('bem')
    ags = sorted(ref)
    pts = np.array([interior_point(ref[a][0] if len(ref[a]) == 1 else {'type': 'MultiPolygon', 'coordinates': [p for x in ref[a] for p in polys(x)]}) for a in ags])
    states = np.array([st[a] for a in ags])
    print('ref', len(ags), file=sys.stderr, flush=True)

    krs = [(r['properties']['lkz'], prep(r['geometry'])) for r in records(snaps['vg25_krs'] / 'records.jsonl.gz')]

    auth = Counter(); loc = Counter(); bnd = Counter(); src_counts = Counter()
    geom = {'authorities': [0, 0], 'boundaries': [0, 0], 'locations': [0, 0]}
    hit = defaultdict(lambda: np.zeros(len(ags), bool)); locstates = defaultdict(set)
    src_hit = defaultdict(lambda: np.zeros(len(ags), bool))
    inputs = [(s, snaps[s] / 'records.jsonl.gz') for s in sorted(snaps)]
    for name, path in inputs:
        for r in records(path):
            k, c, src = r['kind'], r['category'], r['source_id']
            if k == 'authority':
                auth[c] += 1; src_counts[(c, src, 'polygon')] += 1
                geom['authorities'][0] += 1; geom['authorities'][1] += bool(r['geometry_repaired'])
                h = covers(prep(r['geometry']), pts); hit[c] |= h
                if c in ('stadtverwaltung', 'kreisverwaltung'): src_hit[c + '|' + src] |= h
            elif k == 'authority_point':
                loc[c] += 1; src_counts[(c, src, 'point')] += 1; geom['locations'][0] += 1
                p = np.array([r['geometry']['coordinates'][:2]], float)
                for lkz, pr in krs:
                    if covers(pr, p)[0]: locstates[c].add(lkz); break
            else:
                bnd[c] += 1; geom['boundaries'][0] += 1; geom['boundaries'][1] += bool(r['geometry_repaired'])
        print('done', name, file=sys.stderr, flush=True)

    S = sorted(set(states))
    out = {'auth': auth, 'loc': loc, 'bnd': bnd,
           'src_counts': [{'category': c, 'source_id': s, 'records': n, 'geometry_kind': k} for (c, s, k), n in sorted(src_counts.items())],
           'geom': geom, 'ref_total': len(ags),
           'ref_by_state': [{'state': s, 'municipalities': int((states == s).sum())} for s in S],
           'matches': {c: [{'state': s, 'matched': int((h & (states == s)).sum())} for s in S if (h & (states == s)).any()] for c, h in hit.items()},
           'matched_ags': {c: [ags[i] for i in np.nonzero(h)[0]] for c, h in hit.items()},
           'locstates': {c: sorted(v) for c, v in locstates.items()},
           'src_matched_ags': {k: [ags[i] for i in np.nonzero(h)[0]] for k, h in src_hit.items()},
           'ref_attrs': {a: {'lkz': st[a], 'bem': ref_bem[a]} for a in ags}}
    return out


def build_summary(m):
    cats = json.loads((RES / 'categories.json').read_text(encoding='utf-8'))
    total = m['ref_total']
    summary = {'generated_at': datetime.now(timezone.utc).isoformat(),
               'measured_from': 'gemessen aus den neuesten vollständigen Snapshots je Quelle ohne PostGIS (scripts/measure_snapshots.py)',
               'authority_records': sum(m['auth'].values()), 'location_records': sum(m['loc'].values()),
               'boundary_records': sum(m['bnd'].values()),
               'categories_with_authorities': sum(1 for c in cats if m['auth'].get(c['id'])),
               'categories_with_locations': sum(1 for c in cats if m['loc'].get(c['id'])),
               'categories_total': len(cats), 'national_coverage_guaranteed': False, 'categories': [],
               'geometry_checks': {k: {'records': v[0], 'repaired': v[1], 'invalid': None} for k, v in m['geom'].items()},
               'source_counts': m['src_counts'], 'reference_municipalities': total,
               'reference_municipalities_by_state': m['ref_by_state'],
               'boundary_sources': ['vg25_gem', 'vg25_krs', 'vg25_rbz'],
               'measurement_note': 'Hinweis zur Messung: Diese Fassung wurde aus den geladenen Snapshots ohne laufende Datenbank berechnet. ST_IsValid wurde dabei nicht erneut ausgeführt; alle Geometrien wurden beim Abruf mit make_valid geprüft. `python scripts/report.py` misst denselben Bestand direkt in PostGIS.'}
    for c in sorted(cats, key=lambda c: c['id']):
        cid = c['id']
        summary['categories'].append({**c, 'authority_count': m['auth'].get(cid, 0), 'boundary_count': m['bnd'].get(cid, 0),
            'location_count': m['loc'].get(cid, 0), 'empty_local_table': not m['auth'].get(cid, 0),
            'municipal_reference_matches': sum(x['matched'] for x in m['matches'].get(cid, [])),
            'municipal_reference_by_state': m['matches'].get(cid, []), 'matched_ags': m['matched_ags'].get(cid, []),
            'location_states': [{'state': s} for s in m['locstates'].get(cid, [])]})
    return add_mapping_coverage(summary, m['src_matched_ags'], m['ref_attrs'])


def delivery_manifest(snaps):
    inventory = []
    for sid in sorted(snaps):
        path = snaps[sid]; mf = json.loads((path / 'manifest.json').read_text(encoding='utf-8'))
        data = (path / 'records.jsonl.gz').read_bytes()
        if hashlib.sha256(data).hexdigest() != mf['records_sha256'] or mf.get('layer_fetch_complete') is not True:
            raise ValueError(f'Snapshot {path} unvollständig oder Prüfsumme falsch')
        inventory.append({'directory': str(path.relative_to(ROOT)), 'source_id': sid, 'records_per_category': mf['records_per_category'],
                          'records_sha256': mf['records_sha256'], 'records_file_bytes': len(data), 'completed_at': mf['completed_at']})
    return {'generated_at': datetime.now(timezone.utc).isoformat(), 'snapshots': inventory}


if __name__ == '__main__':
    sources = json.loads((RES / 'sources.json').read_text(encoding='utf-8'))
    radii = json.loads((RES / 'fallback.json').read_text(encoding='utf-8'))['category_radius_km']
    if '--render-only' in sys.argv:
        print(render(json.loads((ROOT / 'data/coverage-summary.json').read_text(encoding='utf-8')), sources, radii))
        raise SystemExit
    m = measure()
    summary = build_summary(m)
    sources = json.loads((RES / 'sources.json').read_text(encoding='utf-8'))
    radii = json.loads((RES / 'fallback.json').read_text(encoding='utf-8'))['category_radius_km']
    slim = {**summary, 'categories': [{k: v for k, v in c.items() if k != 'matched_ags'} for c in summary['categories']]}
    (ROOT / 'data/coverage-summary.json').write_text(json.dumps(slim, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    (ROOT / 'data/delivery-manifest.json').write_text(json.dumps(delivery_manifest(snaps), ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(render(summary, sources, radii))
