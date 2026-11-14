# Web-API

Implementierung: [`behoerden_finder/web/__init__.py`](../behoerden-finder/src/behoerden_finder/web/__init__.py). Standard: `http://127.0.0.1:8765`, JSON in UTF-8. Die API gehört zur lokalen Demo; sie ist kein versionierter öffentlicher Dienst.

| Methode/Pfad | Eingabe | Antwort |
| --- | --- | --- |
| `GET /` | – | Oberfläche; `/index.html`, `/app.css`, `/app.js` liefern die statischen Dateien |
| `GET /api/vorschlaege?q=…` | Photon-Suchtext ab drei Zeichen | `vorschlaege` mit `text`, `lat`, `lon`, `genauigkeit` |
| `POST /api/suche` | Anliegen, Adresse oder Koordinaten; optional feste Kategorie | Dekoriertes Finder-Ergebnis mit Geometrien |
| `POST /api/behoerde-details` | Gefundene Behörde und optional Leistung/Standort | Kontakt, Öffnungszeiten, Quellen und Online-Dienste soweit vorhanden |

## Suche

```json
{
  "anliegen": "Ich möchte meinen Hund anmelden",
  "adresse": "Rathaus Mainz",
  "geocoder": "nominatim"
}
```

`geocoder` ist `nominatim` (Standard) oder `photon`. Sind `lat` und `lon` gemeinsam angegeben, verwendet die Pipeline diese Koordinate. Eine optionale `kategorie`, etwa `finanzamt`, überspringt den Matcher; so kann die Oberfläche die Auswahl nach einem mehrdeutigen Ergebnis erneut senden. Der Server begrenzt Anliegen auf 500 und Adresse auf 200 Zeichen.

Das Ergebnis enthält `status`, `ueberschrift`, `leistung`, `kategorie`, `standort`, `zustaendigkeit`, `hinweise` und ggf. `fehler`. Resolver-Treffer enthalten Quelle, Lizenz, Eigenschaften und Geometrie. Die Web-Dekoration ergänzt je Treffer `kontakt`, ggf. `amtlich` und `zentrum`. Leistungsdetails werden anhand des LeiKa-Schlüssels aus dem lokalen JZuFi-Cache gelesen.

Ein fachlich leeres oder mehrdeutiges Ergebnis kann HTTP **200** liefern; Anwendungen müssen `status` auswerten. Eingabefehler ergeben **400**, nicht abgefangene technische Suchfehler **503**. Ein `resolver_fehler` kann bereits als fachliches Pipeline-Ergebnis zurückgegeben sein. Der vollständige Statuskatalog steht in der [Finder-README](../behoerden-finder/README.md).

## Behörden-Details

```json
{
  "name": "Finanzamt Ingolstadt",
  "adresse": "Esplanade 38, Ingolstadt",
  "lat": 48.7665,
  "lon": 11.4258,
  "punkt": true
}
```

Zusätzlich unterstützt: `standort_lat`, `standort_lon`, `leika`, `web` und `ohne_zeiten`. Die Standortkoordinate ermöglicht die Ermittlung der Gemeinde und passender PVOG-Organisationen/Online-Dienste. Sie ist von `lat/lon` der Behörde zu unterscheiden. `ohne_zeiten` unterbindet den abschließenden Webseitenversuch, nicht alle anderen Quellenabrufe.

Die Antwort kann `gefunden=false` enthalten, obwohl der Endpunkt erfolgreich lief. Einzelne Zusatzdienstausfälle sollen die Hauptsuche nicht verhindern. Quellen und Öffnungszeiten können unvollständig sein. `verfuegbar=true` allein bestätigt keinen Datentreffer.

## Transport und Vorschläge

POST-Anfragen müssen ein JSON-Objekt und eine `Content-Length` von 1 bis 8.000 Byte enthalten. Ungültiges JSON oder zu große/leere Bodies ergeben 400; unbekannte Pfade 404. Die API implementiert keine Anmeldung und keine CORS-Freigabe für beliebige fremde Frontends.

Vorschläge stammen ausschließlich von Photon. Bei weniger als drei Zeichen ist die Liste leer; Geocoder-Fehler werden ebenfalls als 200 mit leerer Liste und `fehler` zurückgegeben. Nominatim wird nicht für Autovervollständigung verwendet.
