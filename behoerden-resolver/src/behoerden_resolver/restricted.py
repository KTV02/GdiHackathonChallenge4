"""Import of an access-restricted PostGIS dump (pg_dump -Fc) with jurisdiction areas.

The dump is read directly (see pgdump.py); no restore into another database
is needed. Each configured source names one dump table and one category.
The raw dump is NOT copied into the snapshot (restricted data, 100+ MB);
the manifest records its SHA-256, the PostgreSQL version and the table.
"""
import hashlib
from pathlib import Path
from pyproj import Transformer
from shapely.geometry import mapping, shape
from shapely.ops import transform
from .pgdump import Dump, parse_ewkb
from .supplements import base_record, write_snapshot


def file_sha256(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for block in iter(lambda: f.read(1 << 20), b''):
            h.update(block)
    return h.hexdigest()


def import_pgdump_table(s, directory, input_path=None):
    path = Path(input_path or s.get('default_input', ''))
    if not path.is_file():
        raise ValueError(f"Dump nicht gefunden: {path}. Mit --input <datei.backup> angeben.")
    dump = Dump(path)
    transformers = {}

    def to_wgs84(srid, geometry):
        if srid in (None, 4326):
            if srid is None:
                raise ValueError('Geometrie ohne SRID; Koordinatensystem unbekannt')
            return geometry
        if srid not in transformers:
            transformers[srid] = Transformer.from_crs(f'EPSG:{srid}', 'EPSG:4326', always_xy=True)
        return mapping(transform(transformers[srid].transform, shape(geometry)))

    rows = list(dump.table_data(s['dump_table'], s.get('dump_schema', 'public')))
    expected = s.get('dump_category_value')
    keys = set()

    def records():
        for row in rows:
            if expected and row.get('Category') != expected:
                raise ValueError(f"Unerwartete Kategorie {row.get('Category')!r} in {s['dump_table']}")
            name = (row.get('Name') or '').strip()
            if not name:
                raise ValueError(f"Name fehlt in {s['dump_table']} id={row.get('id')}")
            if row['id'] in keys:
                raise ValueError(f"Doppelte id {row['id']} in {s['dump_table']}")
            keys.add(row['id'])
            srid, geometry = parse_ewkb(row['geom'])
            props = {'dump_table': s['dump_table'], 'dump_id': row['id'], 'dump_category': row.get('Category'),
                     'source_srid': srid, 'access': 'restricted',
                     'geometry_completeness': 'official_source_polygon'}
            yield base_record(s, f"{s['dump_table']}.{row['id']}", name, to_wgs84(srid, geometry), props)

    metadata = {'observed_features': len(rows), 'access': 'restricted',
                'dump': {'file_name': path.name, 'sha256': file_sha256(path), 'bytes': path.stat().st_size,
                         'archive_version': '.'.join(map(str, dump.ver)), 'pg_dump_version': dump.dumpver,
                         'server_version': dump.remote, 'table': f"{s.get('dump_schema', 'public')}.{s['dump_table']}"}}
    try:
        return write_snapshot(s, directory, records(), metadata)
    finally:
        dump.close()
