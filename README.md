# Authority finder for Germany

A prototype for Challenge 4 at hack4GDI_DE. Enter a task such as `Auto anmelden`, choose a location and select the matching service. The result lists the associated authorities, with addresses, contacts, opening hours and online services where available.

The interface supports German and English. Most source records are German, so German search terms usually work better.

## Match an authority category in the terminal

The separate CLI turns a German administrative request into a **category keyword**, using actual PVOG service-to-organisation associations and an explicit local category vocabulary. It does not query PostGIS or GeoServer. The existing website is unchanged.

```bash
source .venv/bin/activate
python -m pip install -r requirements-cli.txt
python -m authority_matcher "Ich möchte meinen Hund anmelden"
python -m authority_matcher "Ich möchte mein Auto anmelden"
python -m authority_matcher "Ich möchte einen Personalausweis beantragen" --json
```

The first example returns `KOMMUNALE_STEUERVERWALTUNG`. The configured search reference is **Mainz**, since PVOG requires a regional key; this is not a nationwide authority classification. Ten local categories, including `FINANZAMT`, are configured. Unknown organisations and ambiguous requests remain unresolved.

The car example returns `KFZ_ZULASSUNGSBEHOERDE` using PVOG's own `Auto anmelden` synonym. Official service synonyms are checked alongside titles and retained in offline exports. `python -m authority_matcher coverage` lists known organisations that still need a category assignment.

For a bounded offline demo, append `--offline`. The official extract contains 90 service records fetched on 13 November 2026, expires after 30 days by default, and is clearly marked as cached data. See the [CLI guide](docs/authority-category-cli.md) for examples, configuration, source evidence, limitations and reuse from a future website or MCP server.

If the complete query has no source hits, the CLI can show related services found by the subject alone. For example, `Schenkung anmelden` produces suggestions, while the actual catalogue title `Schenkungsteuerbescheid erhalten` resolves to `FINANZAMT`. The suggestions do not confirm the original request or equate a gift notification with receiving a tax assessment.

## Run locally

Python 3.10 or newer, internet access and a modern browser are required.

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
python app/server.py
```

On Windows, create the environment with `py -m venv .venv` and activate it with `.venv\Scripts\Activate.ps1` in PowerShell. The remaining commands are the same.

Open <http://127.0.0.1:5000>. Enter a task and an address, check the location candidates, then choose a service. A point on the map can be used instead of an address.

The server is intended for a small local demo. Public Nominatim allows at most one request per second for the whole application and prohibits autocomplete. The app enforces this within one process; separate instances do not share the limit. Photon also requires moderate use. Read the [provider rules](docs/data-sources.md#address-search) before running a shared instance.

## What the results mean

BKG boundaries identify the municipality; the Portalverbund search service supplies the services and associated authorities. Results help with orientation, but do not settle every case. District responsibilities, special rules and data gaps can affect the answer. An authority's general address may differ from the counter handling a particular service. Confirm the location and opening hours in the linked official portal before a visit.

## Development

```bash
python -m unittest discover -s tests -v
node --test tests/frontend.test.cjs
```

The frontend checks require Node.js 20 or newer. See [testing and demo](docs/testing-and-demo.md) for manual checks and the last validation results.

Optional configuration: `NOMINATIM_URL`, `PHOTON_URL` and `GEOCODER_CONTACT`. Use compatible endpoints and set a monitored project contact before a wider demo. Data transfers and source conditions are described in [data sources](docs/data-sources.md).

- [Architecture](docs/architecture.md)
- [Team workflow and open tasks](CONTRIBUTING.md)
- [Validation record](docs/validation-2026-11-10.md)
- [BKG exploration notebook](notebooks/explore_bzb.ipynb)
- [Original requirements notes, in German](docs/original-requirements-de.md)
