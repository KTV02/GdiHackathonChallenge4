"""Read-only access to PostgreSQL custom-format dumps (pg_dump -Fc, archive 1.14-1.16).

Pure Python (zlib only), so a dump from a newer PostgreSQL (e.g. 18) can be
imported without a matching pg_restore and without restoring it into a
database. Only uncompressed or gzip-compressed table data is supported.
"""
import struct, zlib

class Dump:
    def __init__(self, path):
        self.f = open(path, 'rb')
        f = self.f
        assert f.read(5) == b'PGDMP'
        self.vmaj, self.vmin, self.vrev = f.read(3)
        self.intsize, self.offsize, self.fmt = f.read(3)
        self.ver = (self.vmaj, self.vmin)
        self.comp = f.read(1)[0] if self.ver >= (1, 15) else self.rint()
        if self.ver < (1, 14) or self.ver > (1, 16) or self.fmt != 1:
            raise ValueError(f'Nicht unterstütztes Dumpformat {self.ver}/{self.fmt}')
        if self.comp not in (0, 1):
            raise ValueError('Nur unkomprimierte oder gzip-komprimierte Dumps werden unterstützt')
        self.time = [self.rint() for _ in range(7)]
        self.dbname, self.remote, self.dumpver = self.rstr(), self.rstr(), self.rstr()
        n = self.rint()
        self.toc = [self.rtoc() for _ in range(n)]

    def rint(self):
        sign = self.f.read(1)[0]
        v = int.from_bytes(self.f.read(self.intsize), 'little')
        return -v if sign else v

    def rstr(self):
        n = self.rint()
        if n < 0: return None
        return self.f.read(n).decode('utf-8')

    def roff(self):
        flag = self.f.read(1)[0]
        return flag, int.from_bytes(self.f.read(self.offsize), 'little')

    def rtoc(self):
        t = {}
        t['id'] = self.rint(); t['had_dumper'] = self.rint()
        t['tableoid'] = self.rstr(); t['oid'] = self.rstr()
        t['tag'] = self.rstr(); t['desc'] = self.rstr(); t['section'] = self.rint()
        t['defn'] = self.rstr(); t['drop'] = self.rstr(); t['copy'] = self.rstr()
        t['namespace'] = self.rstr(); t['tablespace'] = self.rstr(); t['tableam'] = self.rstr()
        if self.ver >= (1, 16): t['relkind'] = self.rint()
        t['owner'] = self.rstr(); self.rstr()
        deps = []
        while True:
            d = self.rstr()
            if d is None: break
            deps.append(d)
        t['deps'] = deps
        t['offflag'], t['offset'] = self.roff()
        return t

    def table_data(self, table, schema='public'):
        hits = [t for t in self.toc if t['desc'] == 'TABLE DATA' and t['tag'] == table and t['namespace'] == schema]
        if len(hits) != 1:
            raise ValueError(f'Tabelle {schema}.{table} nicht eindeutig im Dump')
        entry = hits[0]
        columns = entry['copy'].split('(', 1)[1].split(')', 1)[0]
        names = [c.strip().strip('"') for c in columns.split(',')]
        for line in self.data(entry):
            if not line or line == '\\.':
                continue
            values = [None if v == '\\N' else unescape(v) for v in line.split('\t')]
            if len(values) != len(names):
                raise ValueError(f'Spaltenzahl stimmt nicht in {table}')
            yield dict(zip(names, values))

    def close(self):
        self.f.close()

    def data(self, entry):
        """Yield decoded text of a table data block (COPY format)."""
        f = self.f
        f.seek(entry['offset'])
        btype = f.read(1)[0]
        dump_id = self.rint()
        assert btype == 1 and dump_id == entry['id'], (btype, dump_id)
        dec = zlib.decompressobj() if self.comp else None
        buf = b''
        while True:
            n = self.rint()
            if n == 0: break
            chunk = f.read(n)
            out = dec.decompress(chunk) if dec else chunk
            buf += out
            *lines, buf = buf.split(b'\n')
            for l in lines: yield l.decode('utf-8')
        if dec: buf += dec.flush()
        for l in buf.split(b'\n'):
            if l: yield l.decode('utf-8')


def unescape(value):
    """COPY text format escapes."""
    if '\\' not in value:
        return value
    out, i = [], 0
    simple = {'t': '\t', 'n': '\n', 'r': '\r', 'b': '\b', 'f': '\f', 'v': '\v', '\\': '\\'}
    while i < len(value):
        c = value[i]
        if c == '\\' and i + 1 < len(value):
            out.append(simple.get(value[i + 1], value[i + 1])); i += 2
        else:
            out.append(c); i += 1
    return ''.join(out)


def parse_ewkb(hexstr):
    """Hex (E)WKB Polygon/MultiPolygon -> (srid, GeoJSON geometry dict); Z/M are dropped."""
    b = bytes.fromhex(hexstr); pos = 0

    def geom():
        nonlocal pos
        order = '<' if b[pos] == 1 else '>'; pos += 1
        t, = struct.unpack_from(order + 'I', b, pos); pos += 4
        srid = None
        if t & 0x20000000:
            srid, = struct.unpack_from(order + 'I', b, pos); pos += 4
        dim = 2 + bool(t & 0x80000000) + bool(t & 0x40000000)
        base = t & 0x0FFFFFFF

        def ring():
            nonlocal pos
            n, = struct.unpack_from(order + 'I', b, pos); pos += 4
            vals = struct.unpack_from(order + 'd' * (n * dim), b, pos); pos += 8 * n * dim
            return [[vals[i * dim], vals[i * dim + 1]] for i in range(n)]

        def poly():
            nonlocal pos
            n, = struct.unpack_from(order + 'I', b, pos); pos += 4
            return [ring() for _ in range(n)]
        if base == 3:
            return srid, {'type': 'Polygon', 'coordinates': poly()}
        if base == 6:
            n, = struct.unpack_from(order + 'I', b, pos); pos += 4
            parts = []
            for _ in range(n):
                _, g = geom(); parts.append(g['coordinates'])
            return srid, {'type': 'MultiPolygon', 'coordinates': parts}
        raise ValueError(f'Nicht unterstützter Geometrietyp {base}')
    srid, g = geom()
    if pos != len(b):
        raise ValueError('Überzählige Bytes in der Geometrie')
    return srid, g
