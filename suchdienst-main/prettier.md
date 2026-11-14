# Code-Formatierung mit Prettier

Um eine einheitliche Code-Formatierung im Projekt sicherzustellen, wird Prettier verwendet.
Prettier ist ein weit verbreitetes Tool zur automatischen Formatierung von Quellcode.
Es unterstützt viele Programmiersprachen und lässt sich in die meisten gängigen Code-Editoren integrieren.

## Links

- [Prettier](https://prettier.io)
- [Prettier Plugins](https://prettier.io/docs/en/plugins.html#official-plugins)

## Konfiguration

Beispiel für eine einfache `.prettierrc.yml`. Weitere Möglichkeiten sind [hier](https://prettier.io/docs/en/configuration.html) dokumentiert.
Die Datei kann auch im JSON-Format geschrieben werden.

```yml
printWidth: 100
tabWidth: 4
useTabs: false
```

## Java-Setup

Für die Formatierung der Java-Projekte mit Prettier wird das `spotless-maven-plugin` von DiffPlug ([Link](https://github.com/diffplug/spotless/tree/main/plugin-maven)) verwendet.
Prettier und Prettier-Java ([Link](https://github.com/jhipster/prettier-java)) werden durch die Konfiguration in der `pom.xml` automatisch heruntergeladen und installiert.

Anschließend können folgende `mvn` Goals ausgeführt werden:

- Code-Formatierung nach aktueller Konfiguration prüfen: `mvn spotless:check`
- Code-Formatierung vornehmen: `mvn spotless:apply`

---

**Wichtig!** Damit die Formatierung einheitlich bleibt, sollten Codeformat-Einstellungen für Prettier nicht über das Maven-Plugin in der `pom.xml` vorgenommen werden, sondern in der `.prettierrc.yml` Konfigurationsdatei.

---

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>x.x.x</version>
    <configuration>
        <formats>
            <format>
                <includes>
                    <include>src/*/java/**/*.java</include>
                </includes>
                <prettier>
                    <devDependencies>
                        <prettier>x.x.x</prettier>
                        <prettier-plugin-java>x.x.x</prettier-plugin-java>
                    </devDependencies>
                    <configFile>.prettierrc.yml</configFile>
                    <config>
                        <plugins>prettier-plugin-java</plugins>
                    </config>
                </prettier>
            </format>
        </formats>
    </configuration>
</plugin>
```
