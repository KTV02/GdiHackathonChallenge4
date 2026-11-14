"""Shared rendering of docs/ABDECKUNG.md from a measured coverage summary.

Used by scripts/report.py (live PostGIS) and scripts/measure_snapshots.py
(loaded snapshots, no database). Statutory category mappings from
resources/category_mappings.json are evaluated on the same AGS reference points.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/behoerden_resolver/resources'
STATES = 'Länderkürzel: BW Baden-Württemberg, BY Bayern, BE Berlin, BB Brandenburg, HB Bremen, HH Hamburg, HE Hessen, MV Mecklenburg-Vorpommern, NI Niedersachsen, NW Nordrhein-Westfalen, RP Rheinland-Pfalz, SL Saarland, SN Sachsen, ST Sachsen-Anhalt, SH Schleswig-Holstein, TH Thüringen.'
EVIDENCE = {'norm_verified': 'Norm geprüft', 'statutory_general_rule': 'allgemeine gesetzliche Regel'}


def mapping_rules():
    return json.loads((RES / 'category_mappings.json').read_text(encoding='utf-8'))['rules']


def rule_matches_municipality(rule, category_id, ags, attrs):
    if rule['scope'] != 'all' and attrs['lkz'] not in rule['scope']:
        return False
    flt = rule.get('municipality_filter') or {}
    if 'ags' in flt and ags not in flt['ags']:
        return False
    if 'vg25_bem' in flt and attrs.get('bem') not in flt['vg25_bem']:
        return False
    if ags in (flt.get('exclude_ags_for') or {}).get(category_id, []):
        return False
    return True


def add_mapping_coverage(summary, src_matched_ags, ref_attrs):
    """src_matched_ags: {'<base category>|<source id>': [AGS, ...]} of explicit base polygons."""
    rules = mapping_rules()
    sets = {k: set(v) for k, v in src_matched_ags.items()}
    states = sorted({a['lkz'] for a in ref_attrs.values()})
    for c in summary['categories']:
        direct = set(c.get('matched_ags', []))
        mapped = set()
        used = []
        for rule in rules:
            if c['id'] not in rule['targets']:
                continue
            covered = set().union(*(sets.get(rule['base_category'] + '|' + s, set()) for s in rule['base_sources']))
            hits = {a for a in covered if rule_matches_municipality(rule, c['id'], a, ref_attrs[a])}
            if hits:
                used.append({'rule_id': rule['id'], 'municipalities': len(hits), 'additional': len(hits - direct)})
            mapped |= hits
        total = direct | mapped
        c['mapping_rules'] = used
        c['mapped_additional_matches'] = len(total) - len(direct)
        c['combined_reference_matches'] = len(total)
        c['combined_reference_by_state'] = [{'state': s, 'matched': sum(1 for a in total if ref_attrs[a]['lkz'] == s)}
                                            for s in states if any(ref_attrs[a]['lkz'] == s for a in total)]
    summary['mapping_rules'] = rules
    return summary


def de(n):
    return format(n, ',').replace(',', '.')


def pct(n, total):
    return f"{n}/{total} ({100 * n / total:.1f} %)"


def render(summary, sources, radii, out=ROOT / 'docs/ABDECKUNG.md'):
    total = summary['reference_municipalities']
    source_counts = summary['source_counts']
    loaded = {r['source_id'] for r in source_counts} | set(summary.get('boundary_sources', []))
    mapped_cats = [c for c in summary['categories'] if c.get('mapped_additional_matches')]
    head = ['# Deutschlandweite Abdeckung und Quellen je Behördenkategorie', '',
            f"Stand der lokalen Messung: {summary['generated_at']}{'; ' + summary['measured_from'] if summary.get('measured_from') else ''}. "
            f"**{de(summary['authority_records'])} Flächeneinträge in {summary['categories_with_authorities']} Kategorien; {de(summary['location_records'])} Standorte in {summary['categories_with_locations']} Kategorien; {de(summary['boundary_records'])} reine Verwaltungsgebietsreferenzen.** "
            f"Zusätzlich übernehmen **{len(mapped_cats)} Kategorien** per gesetzlicher Zuordnung bereits importierte Stadt-, Gemeinde- oder Kreisverwaltungsflächen. Alle 36 Kategorien werden unterstützt, aber nicht alle besitzen bundesweit nachgewiesene Zuständigkeitsflächen.", '',
            'Die Mengen zählen Quellfeatures und Dienststellen einschließlich mehrfacher räumlicher Teile. Mehrere Quellen können dieselbe Institution enthalten. Ein Standort ist kein Nachweis für deren örtliche oder persönliche Zuständigkeit.', '',
            '## Wie die Abdeckung gemessen wird', '',
            f"Der Flächenindikator zählt die Gemeindeschlüssel, deren VG25-Repräsentativpunkt von mindestens einem importierten Fachpolygon erfasst wird. Basis: **{total} unterschiedliche AGS** aus dem tatsächlich geladenen VG25-Gemeindelayer. Pro AGS werden vorhandene Teile vereinigt und `ST_PointOnSurface` gebildet; geprüft wird `ST_Covers`. Diese reproduzierbare Stichprobe misst weder Flächenanteil noch Bevölkerung, Straßen oder sämtliche Adressen einer Gemeinde. Ein Treffer kann eine nur teilweise abgedeckte Gemeinde betreffen. Selbst ein Wert von 100 % beweist keine lückenlose Fachzuständigkeit und keine Aktualität aller Gebiete. Dienstgrenzen haben unterschiedliche Generalisierung und Quelldatenstände.", '',
            'Die Spalte „inkl. gesetzlicher Zuordnung“ zählt zusätzlich die Referenzpunkte, für die eine Regel aus `category_mappings.json` greift: Der Punkt liegt in einer explizit importierten Stadt-, Gemeinde- oder Kreisverwaltungsfläche, und das Landesrecht weist der Kategorie genau diese Verwaltung zu (Abschnitt „Gesetzliche Zuordnung“).', '',
            'Die Länderliste bei Flächen nennt Länder mit mindestens einem Referenzpunkttreffer (direkt oder per Zuordnung); bei Standorten nennt sie Länder, in denen die importierten Punkte tatsächlich liegen. Keine dieser Listen behauptet eine vollständige Landesabdeckung. Bundesweit angebotene BZB-Open-Datensätze gibt es für Amtsgericht, Landgericht, Jobcenter und Arbeitsagentur.', '',
            '## Alle 36 Kategorien', '',
            '| Kategorie | Flächeneinträge | Gemeindeindikator direkt | inkl. gesetzlicher Zuordnung | Länder mit Flächentreffern | Standorte und Länder | Quellen |',
            '| --- | ---: | ---: | ---: | --- | --- | --- |']
    for c in summary['categories']:
        states = ', '.join(v['state'] for v in c.get('combined_reference_by_state') or c['municipal_reference_by_state']) or '–'
        pointstates = ', '.join(v['state'] for v in c['location_states']) or '–'
        if c['resolution_mode'] == 'spatial':
            direct = pct(c['municipal_reference_matches'], total)
        else:
            direct = 'Nicht allein räumlich bestimmbar' + (f"; {pct(c['municipal_reference_matches'], total)}" if c['municipal_reference_matches'] else '')
        combined = pct(c['combined_reference_matches'], total) if c.get('mapped_additional_matches') else '–'
        ids = [r['source_id'] + (' [Punkt]' if r['geometry_kind'] == 'point' else '') for r in source_counts if r['category'] == c['id']]
        ids += [f"Zuordnung `{u['rule_id']}`" for u in c.get('mapping_rules', [])]
        head.append(f"| `{c['id']}` | {c['authority_count']} | {direct} | {combined} | {states} | {c['location_count']}; {pointstates} | {'; '.join(ids) or 'Keine importierte Quelle'} |")
    head += ['', STATES, '',
             '## Gesetzliche Zuordnung auf bestehende Behördenflächen', '',
             'Viele Aufgaben werden nicht von einem eigenen Fachamt mit eigener Gebietsgrenze erledigt, sondern per Landesrecht von der Stadt-, Gemeinde- oder Kreisverwaltung. Beispiel Rheinland-Pfalz: Untere Bauaufsichtsbehörde ist die Kreisverwaltung, in kreisfreien und großen kreisangehörigen Städten die Stadtverwaltung (§ 58 LBauO). Für solche Fälle gibt der Resolver die bereits importierte Fläche und Anschrift dieser Verwaltung zurück, wenn keine eigene Fläche der angefragten Kategorie den Punkt erfasst.', '',
             'Die Antwort kennzeichnet das ausdrücklich: `match_type=statutory_category_mapping`, `mapped_from_category`, `data_status=statutory_mapping_match`, ein `mapping`-Objekt mit Regel, Rechtsgrundlage und Belegstufe sowie ein Warntext (`case_competence_verified=false`). Eine eigene Fachfläche hat immer Vorrang; Standortpunkte kommen erst danach. `--no-category-mapping` schaltet die Zuordnung ab. Reine VG25-Grenzen werden nie zu Behörden; zugeordnet wird nur auf explizit benannte Verwaltungen.', '',
             '| Regel | Kategorien | Länder | Basis | Rechtsgrundlage | Belegstufe |', '| --- | --- | --- | --- | --- | --- |']
    for r in summary['mapping_rules']:
        scope = 'alle mit Basisdaten' if r['scope'] == 'all' else ', '.join(r['scope'])
        flt = r.get('municipality_filter') or {}
        basis = f"`{r['base_category']}` aus {', '.join(r['base_sources'])}" + (' (nur kreisfreie Städte)' if 'vg25_bem' in flt else f" (nur AGS {', '.join(flt['ags'])})" if 'ags' in flt else '')
        head.append(f"| `{r['id']}` | {', '.join(r['targets'])} | {scope} | {basis} | {r['legal_reference']} ([Beleg]({r['legal_basis']})) | {EVIDENCE[r['evidence_level']]} |")
    head += ['', '„Norm geprüft“ heißt: Der Normtext wurde für diese Regel eingesehen. „Allgemeine gesetzliche Regel“ heißt: Die Zuordnung folgt der bundesweit üblichen Aufgabenverteilung (Bundesrecht mit Landesausführung); einzelne Landesausnahmen sind möglich und in der Antwort als Hinweis vermerkt.', '',
             'Zusätzlich erfasste Gemeinde-Referenzpunkte je Kategorie durch die Zuordnung:', '']
    for c in summary['categories']:
        if c.get('mapping_rules'):
            parts = '; '.join(f"`{u['rule_id']}` {u['municipalities']} AGS" for u in c['mapping_rules'])
            head.append(f"- **{c['label']} (`{c['id']}`)**: +{c['mapped_additional_matches']} gegenüber direkt ({parts}).")
    head += ['', 'Bewusst **nicht** zugeordnet: Gesundheits- und Veterinäramt in kreisfreien Städten (teils vom Landratsamt mitversorgt, z. B. Erlangen, Schwabach), Bauaufsicht und Straßenverkehrsbehörde auf Landkreisebene (Große Kreisstädte, übertragene Gemeinden), Standesamt (Standesamtsbezirke und Ereignisort), Umweltämter (aufgabenabhängig) sowie alle Mitgliedsgemeinden von Ämtern, Verbands- und Verwaltungsgemeinschaften.', '',
             '## Fachliche Grenzen je Kategorie', '']
    for c in summary['categories']:
        head.append(f"- **{c['label']} (`{c['id']}`)**: {c['limitations']} Standort-Radius als Anwendungsvorgabe: {radii[c['id']]} km.")
    head += ['', '## Flächen haben Vorrang vor Standorten', '',
             'Die automatische Abfrage prüft relevante live erreichbare Fach-WFS und, wenn dort keine Fläche passt, lokale fachlich belegte Ergänzungen. Danach greift die gesetzliche Zuordnung auf bestehende Verwaltungsflächen. Erst wenn auch diese keinen Treffer liefert, wird der lokale Standortindex mit geodätischer Entfernung abgefragt. Ein WFS-Fehler gilt nicht als leere Flächenabdeckung: Die Abfrage meldet den Fehler und gibt keinen verdeckten Standortersatz aus. Lokaler Betrieb ist ausdrücklich mit `--backend local` wählbar.', '',
             'Standorte bleiben echte Punkte. Es werden keine Zuständigkeitskreise um Büros erzeugt. Antworten enthalten `fallback_used=true`, `match_type=nearest_point_fallback`, `distance_m`, den verwendeten Radius, `jurisdiction_verified=false` und einen Warntext. Standardmäßig werden höchstens drei mögliche Anlaufstellen zurückgegeben. `--no-point-fallback` schaltet dies ab; `--radius-km` passt den Radius zwischen größer 0 und höchstens 300 km an. Die Kategorienradien sind nachvollziehbare Anwendungsvorgaben, keine amtlichen Entfernungsregeln.', '',
             'Bei Krankenkasse, Pflegekasse, Rentenversicherung und Bundesverwaltungsamt bleibt der Status `non_spatial`; bei weiteren aufgabenabhängigen Kategorien `context_required`, auch wenn eine Fläche oder Zuordnung gefunden wird. Ein gegebenenfalls ausgegebener Standort ist nur eine Kontaktmöglichkeit. Er ersetzt weder Mitgliedschaft noch Leistung, Verfahren, Schulart oder sonstigen fehlenden Kontext.', '',
             '## GEMFA und kommunale Zuordnungen', '',
             'Der offizielle BZSt-GEMFA-Export ist vollständig eingelesen und mit den VG25-Gemeinden verknüpft. Finanzamtsflächen entstehen nur für eindeutig zugeordnete ganze Gemeinden ohne widersprechende Adressausnahme. Geteilte Städte und nicht auflösbare Schlüssel werden ausgelassen und im Snapshot auditiert. Die vereinigten Flächen sind **belegte Teilmengen eines Finanzamtsbezirks**, nicht zwangsläufig der vollständige Bezirk (`geometry_completeness=verified_municipality_subset_of_jurisdiction`). Für die räumliche Anfrage bleibt dort eine Abdeckungslücke. Bayern und Hamburg liefern zusätzlich echte Fachpolygone. Steuerart und Sonderzuständigkeit werden nicht allein aus dem Ort entschieden.', '',
             'Für Stadtverwaltungen werden ausschließlich explizite GEMFA-Felder mit Anrede Stadtverwaltung oder Gemeindeverwaltung genutzt. Verbandsgemeinde-, Amts- oder Verwaltungsgemeinschaftsnamen werden nicht aus der einzelnen Mitgliedsgemeinde erfunden. Das Brandenburger Kommunalverzeichnis ergänzt amtsfreie Gemeinden, kreisfreie Städte und Landkreise über identische VG25-ARS; 323 Ämter und gemeinsame Verwaltungen werden nicht geraten. Weitere Ergänzungen stammen aus ausdrücklich belegten Zuordnungen: Wohngeldbehörden in Bayern (§ 3 ZustV), untere Bauaufsichten in Nordrhein-Westfalen (amtliches Verzeichnis, Stand 2024, 211 Zeilen statt angekündigter 212), Landratsämter in Sachsen, Regierungspräsidien bzw. Bezirksregierungen in Baden-Württemberg, Hessen und Nordrhein-Westfalen sowie Thüringer und hessische Schulamtsflächen.', '',
             '## Quellen und Lizenzen', '',
             'Jede nachfolgende Zeile nennt die konfigurierte Quelle. Endpunkt, Layer, Filter, Namensregel, Aktualitätshinweise und Lizenz stehen zusätzlich im maschinenlesbaren [Quellenmanifest](../src/behoerden_resolver/resources/sources.json). Snapshots enthalten ihre historische Quellkonfiguration und Rohantwortprüfsummen; historische Rohantworten sind nicht vollständig im Git-Checkout enthalten. [delivery-manifest.json](../data/delivery-manifest.json) listet die tatsächlich ausgelieferten vollständigen Snapshots. Quellen mit dem Zusatz „noch nicht geladen“ sind konfiguriert, aber nicht Teil des gemessenen Bestands.', '']
    for s in sources:
        if s.get('active') is False:
            continue
        mark = '' if s['id'] in loaded else ' **(noch nicht geladen)**'
        head.append(f"- **`{s['id']}`**{mark} — {s['scope']}; {s['kind']}; {s['license']}. [Quelle]({s['documentation']})" + (f"; WFS `{s['endpoint']}`, Layer `{s['layer']}`." if s.get('layer') else f"; Datenzugang `{s.get('download_url') or s.get('endpoint') or s['documentation']}`."))
    head += ['', '**OSM-Abdeckung:** `osm_locations` enthält nur einen vollständig geladenen Suchbereich von 10 km um 50.0 N, 8.27 E im Raum Mainz/Wiesbaden. `osm_exact_offices` ergänzt bundesweit nur OSM-Objekte mit exakten Namen (Bundesverwaltungsamt, BAMF, Gewerbeamt, Familienkasse) und ist kein vollständiges Register. Die Abfrage je Bundesland (`osm_states_locations`) ist vorbereitet, aber noch nicht geladen. Die Punktabdeckung darf daher nicht als bundesweit vollständig bezeichnet werden. OSM-Quellen werden nur ausdrücklich aktualisiert, nicht im Standard-Refresh und nicht bei jeder Standortsuche.', '',
             'Die Datenlizenzen gelten unabhängig von der MIT-Lizenz des Programmcodes. OpenStreetMap-Daten sind gemeinschaftlich gepflegt und unter ODbL 1.0 lizenziert; Namensnennung und die einschlägigen Weitergabepflichten bleiben bei Nutzung der Standortdaten bestehen. Amtliche Punkte und OSM-Punkte werden mit ihrer jeweiligen Quelle und Qualität ausgewiesen.', '',
             '## Aktualität und offene Quellen', '',
             'Abrufdatum und fachlicher Datenstand sind verschieden. Der GEMFA-Export stammt vom 2. November 2026. Die Thüringer Gesundheits-, Veterinär- und Jugendamtsstandorte tragen im Atom-Eintrag den Stand 16. September 2022, die Schulamtsdatei den Stand 17. Juni 2024, das NRW-Bauaufsichtsverzeichnis den Stand Juli 2024. Diese älteren Daten werden als solche mitgeführt. Gesetzliche und redaktionelle Zuordnungen sind am 13. November 2026 geprüft, die Kategorie-Zuordnungen in `category_mappings.json` am 14. November 2026; ein Geometrieabruf erneuert diese Prüfung nicht.', '',
             'POI-Bund wird wegen Zugangsbeschränkungen nicht importiert. BZB ohne Open kann optional als berechtigt bereitgestellter PostGIS-Dump importiert werden (`format=pgdump_table`, `access=restricted`); daraus werden nur Staatsanwaltschaften (bundesweit, in Bayern nachrangig zur bayerischen Fachquelle) und die allgemeinen Bezirke der Hauptzollämter übernommen. Diese Daten dürfen nicht weitergegeben werden; Snapshots und Dump sind von Git ausgeschlossen. Bundespolizei, THW, Oberlandesgerichte, BA-Regionaldirektionen und -Geschäftsstellen sowie Zoll-Spezialbezirke aus demselben Dump passen auf keine der 36 Kategorien bzw. würden Doppeltreffer erzeugen und bleiben außen vor. Der direkte bayerische Verwaltungsgerichtsflächenlayer enthält andere Institutionen und bleibt ausgeschlossen. Die Polizeiflächen für Berlin und Baden-Württemberg sowie die Berliner Bezirke (Opt-in mit Rechtsprüfung) sind konfiguriert, aber noch nicht abgerufen; siehe [Erweiterung der Quellen](ERWEITERUNG_QUELLEN.md). Eine nicht integrierte Quelle wird nicht als nachgewiesene Abdeckung gezählt. [Recherche](RECHERCHE.md) und [Validierung](VALIDIERUNG.md) erläutern diese Entscheidungen.', '',
             '## Geometriekontrolle', '']
    for table, values in summary['geometry_checks'].items():
        invalid = values['invalid'] if values['invalid'] is not None else 'nicht neu geprüft'
        head.append(f"- `{table}`: {values['records']} Datensätze; ungültige Geometrien: {invalid}; {values['repaired']} als repariert markiert.")
    head += ['', 'Polygonreparaturen nutzen GEOS make_valid, behalten alle Flächenteile und können kollabierte Reste ohne Fläche entfernen. Reparaturkennzeichen bleiben erhalten; lokale Rohantworten können zusätzlich zur Kontrolle aufbewahrt werden. Die vollständigen Quellengeometrien werden bei Abfragen zurückgegeben; der Gemeindeindikator verändert keine Daten.']
    if summary.get('measurement_note'):
        head += ['', summary['measurement_note']]
    out.write_text('\n'.join(head) + '\n', encoding='utf-8')
    return out
