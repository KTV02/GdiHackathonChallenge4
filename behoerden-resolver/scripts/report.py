"""Regenerate measured coverage, all 36 categories, and delivery inventory (live PostGIS)."""
import json
import sys
from pathlib import Path
from behoerden_resolver.catalog import sources,resource
from behoerden_resolver.db import connect, coverage
from behoerden_resolver.etl import latest_snapshots, now, read_manifest

sys.path.insert(0,str(Path(__file__).resolve().parent))
from coverage_render import add_mapping_coverage, render

root=Path(__file__).resolve().parents[1]
report=coverage()
(root/'data/coverage-full.json').write_text(json.dumps(report,ensure_ascii=False,indent=2,default=str)+'\n')
summary={'generated_at':now(),'authority_records':sum(c['authority_count'] for c in report['categories']),
         'location_records':sum(c['location_count'] for c in report['categories']),
         'boundary_records':sum(c['boundary_count'] for c in report['categories']),
         'categories_with_authorities':sum(c['authority_count']>0 for c in report['categories']),
         'categories_with_locations':sum(c['location_count']>0 for c in report['categories']),
         'categories_total':len(report['categories']),'national_coverage_guaranteed':False,'categories':report['categories'],
         'boundary_sources':['vg25_gem','vg25_krs','vg25_rbz']}
with connect() as conn:
 summary['geometry_checks']={}
 for table in ['authorities','boundaries','locations']:
  fixed='count(*) FILTER(WHERE geometry_repaired)' if table!='locations' else '0'
  summary['geometry_checks'][table]=conn.execute(f'SELECT count(*) AS records, {fixed} AS repaired, count(*) FILTER(WHERE NOT ST_IsValid(geom)) AS invalid FROM behoerden.{table}').fetchone()
 source_counts=conn.execute("""SELECT category,source_id,count(*) AS records,'polygon' AS geometry_kind FROM behoerden.authorities GROUP BY category,source_id
   UNION ALL SELECT category,source_id,count(*),'point' FROM behoerden.locations GROUP BY category,source_id ORDER BY category,source_id""").fetchall()
 summary['source_counts']=source_counts
 # One representative point per actual AGS, never a percentage of people or addresses.
 conn.execute("""CREATE TEMP TABLE municipal_reference AS SELECT properties->>'ags' AS ags,
   max(properties->>'lkz') AS state,max(properties->>'bem') AS bem,ST_PointOnSurface(ST_UnaryUnion(ST_Collect(geom))) AS geom
   FROM behoerden.boundaries WHERE source_id='vg25_gem' GROUP BY properties->>'ags'""")
 conn.execute('CREATE INDEX ON municipal_reference USING gist(geom)')
 conn.execute('ANALYZE municipal_reference')
 totals=conn.execute('SELECT state,count(*) AS municipalities FROM municipal_reference GROUP BY state ORDER BY state').fetchall()
 summary['reference_municipalities']=sum(r['municipalities'] for r in totals)
 summary['reference_municipalities_by_state']=totals
 ref_attrs={r['ags']:{'lkz':r['state'],'bem':r['bem']} for r in conn.execute('SELECT ags,state,bem FROM municipal_reference').fetchall()}
 for c in summary['categories']:
  if not c['authority_count']:c['municipal_reference_matches']=0;c['municipal_reference_by_state']=[];c['matched_ags']=[];continue
  print('Measure',c['id'],flush=True)
  rows=conn.execute("""SELECT r.ags,r.state FROM municipal_reference r
    WHERE EXISTS(SELECT 1 FROM behoerden.authorities a WHERE a.category=%s AND a.geom && r.geom AND ST_Covers(a.geom,r.geom))""",(c['id'],)).fetchall()
  c['matched_ags']=sorted(r['ags'] for r in rows)
  states=sorted({r['state'] for r in rows})
  c['municipal_reference_matches']=len(rows)
  c['municipal_reference_by_state']=[{'state':s,'matched':sum(1 for r in rows if r['state']==s)} for s in states]
 # Explicit base polygons per source, for the statutory category mappings.
 src_matched={}
 for base in ('stadtverwaltung','kreisverwaltung'):
  for r in conn.execute("""SELECT a.source_id,r.ags FROM municipal_reference r JOIN behoerden.authorities a
      ON a.category=%s AND a.geom && r.geom AND ST_Covers(a.geom,r.geom) GROUP BY a.source_id,r.ags""",(base,)).fetchall():
   src_matched.setdefault(base+'|'+r['source_id'],[]).append(r['ags'])
 for c in summary['categories']:
  c['location_states']=conn.execute("""SELECT DISTINCT b.properties->>'lkz' AS state FROM behoerden.locations l JOIN behoerden.boundaries b
   ON b.source_id='vg25_krs' AND b.geom && l.geom AND ST_Covers(b.geom,l.geom) WHERE l.category=%s ORDER BY state""",(c['id'],)).fetchall() if c['location_count'] else []
add_mapping_coverage(summary,src_matched,ref_attrs)
slim={**summary,'categories':[{k:v for k,v in c.items() if k!='matched_ags'} for c in summary['categories']]}
(root/'data/coverage-summary.json').write_text(json.dumps(slim,ensure_ascii=False,indent=2)+'\n')
radii=json.loads(resource('fallback.json').read_text())['category_radius_km']
render(summary,sources(),radii,root/'docs/ABDECKUNG.md')
inventory=[]
for path in latest_snapshots(root/'data/snapshots'):
 m=read_manifest(path)
 inventory.append({'directory':str(path.relative_to(root)),'source_id':m['source']['id'],'records_per_category':m['records_per_category'],'records_sha256':m['records_sha256'],'records_file_bytes':(path/'records.jsonl.gz').stat().st_size,'completed_at':m['completed_at']})
(root/'data/delivery-manifest.json').write_text(json.dumps({'generated_at':now(),'snapshots':inventory},ensure_ascii=False,indent=2)+'\n')
print(json.dumps({k:v for k,v in summary.items() if k not in ('categories','source_counts','mapping_rules')},ensure_ascii=False,indent=2,default=str))
