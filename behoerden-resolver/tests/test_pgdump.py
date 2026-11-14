import struct
import zlib
import pytest
from behoerden_resolver.pgdump import Dump, parse_ewkb, unescape


def wint(v):
    return bytes([1 if v < 0 else 0]) + abs(v).to_bytes(4, 'little')


def wstr(v):
    if v is None:
        return wint(-1)
    b = v.encode()
    return wint(len(b)) + b


def ewkb_square(x0, y0, srid=25832):
    ring = [(x0, y0), (x0 + 1, y0), (x0 + 1, y0 + 1), (x0, y0 + 1), (x0, y0)]
    poly = struct.pack('<BII', 1, 3, 1) + struct.pack('<I', len(ring)) + b''.join(struct.pack('<dd', *p) for p in ring)
    return (struct.pack('<BII', 1, 6 | 0x20000000, srid) + struct.pack('<I', 1) + poly).hex().upper()


def write_dump(path, rows, table='Zoll_HauptZA'):
    copy = f'COPY public."{table}" (id, geom, "Name", "Category") FROM stdin;\n'
    text = ''.join('\t'.join(r) + '\n' for r in rows) + '\\.\n\n'
    data = zlib.compress(text.encode())
    def toc(offset):
        e = wint(7) + wint(1) + wstr('0') + wstr('0') + wstr(table) + wstr('TABLE DATA') + wint(3)
        e += wstr('') + wstr('') + wstr(copy) + wstr('public') + wstr('') + wstr('') + wint(114)
        e += wstr('postgres') + wstr('false') + wstr(None) + bytes([2]) + offset.to_bytes(8, 'little')
        return e
    head = b'PGDMP' + bytes([1, 16, 0, 4, 8, 1, 1]) + b''.join(wint(0) for _ in range(7))
    head += wstr('postgres') + wstr('18.7') + wstr('18.7') + wint(1)
    offset = len(head) + len(toc(0))
    body = bytes([1]) + wint(7) + wint(len(data[:10])) + data[:10] + wint(len(data[10:])) + data[10:] + wint(0)
    path.write_bytes(head + toc(offset) + body)


def test_reads_table_rows_and_geometry_from_custom_dump(tmp_path):
    f = tmp_path / 'test.backup'
    write_dump(f, [['1', ewkb_square(500000, 5500000), 'Hauptzollamt TEST', 'Zoll_HauptZA'],
                   ['2', ewkb_square(600000, 5500000), 'Hauptzollamt TEST\\tZWEI', 'Zoll_HauptZA']])
    d = Dump(f)
    assert d.ver == (1, 16) and d.dumpver == '18.7'
    rows = list(d.table_data('Zoll_HauptZA'))
    assert [r['id'] for r in rows] == ['1', '2'] and rows[1]['Name'] == 'Hauptzollamt TEST\tZWEI'
    srid, g = parse_ewkb(rows[0]['geom'])
    assert srid == 25832 and g['type'] == 'MultiPolygon' and g['coordinates'][0][0][2] == [500001.0, 5500001.0]
    with pytest.raises(ValueError):
        list(d.table_data('fehlt'))


def test_copy_unescape():
    assert unescape('a\\\\b\\nc') == 'a\\b\nc'
