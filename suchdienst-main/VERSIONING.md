# 📌 Versionierung

## SemVer

Das Projekt verwendet [SemVer](http://semver.org/) für die Versionierung. Eine Übersicht über die Release-Versionen finden Sie in den [Releases Notes](https://produktportal.pvog.fitko.de/release-notes/) des Produktportals und im [Changelog](CHANGELOG.md).

In der Regel spiegeln sich Breaking Changes der REST-APIs direkt in der Major-Version des Dienstes wider, z.B. wenn Schnittstellen entfernt wurden.

---

## Schnittstellen

Die API-Endpunkte werden versioniert, um Abwärtskompatibilität sicherzustellen. Die Versionsnummer wird als Pfad-Präfix angegeben (z.B. `/v1/`) und erhöht sich bei (breaking) Änderungen der Schnittstelle. Die bisherige Version bleibt weiterhin verfügbar, um bestehenden Integrationen Zeit zur Anpassung zu geben, und wird als `deprecated` markiert. Die Schnittstellen, die als `deprecated` markiert sind, werden nach einer Übergangszeit von min. drei Monaten entweder zum 01.02., 01.05., 01.08. oder 01.11. dauerhaft deaktiviert und sind somit nicht mehr nutzbar.

Fehlerbehebungen, die lediglich die dokumentierte Funktionalität wiederherstellen, erfordern keine Erhöhung der Versionsnummer.

Weitere Informationen zur API-Versionierung finden Sie im [Produktportal](https://produktportal.pvog.fitko.de/docs/suchdienst/doku/#versionierung-der-schnittstelle).

### Beispiele

- Änderungen an der öffentlichen API (inkl. `/admin`)
  - Abwärtskompatible Änderungen der API: **`MINOR` inkrementieren**
  - Deprecation von bestehenden APIs: **`MINOR` inkrementieren**
  - Entfernen von bestehenden Endpunkten, Attributen oder Hinzufügen von neuen Pflicht-Request-Parametern: **`MAJOR` inkrementieren.**
