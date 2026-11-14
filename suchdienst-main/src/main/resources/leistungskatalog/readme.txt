Um den Leistungskatalog zu reduzieren (=> leistungskatalog_reduziert.xml) sind folgende RegEx zu nutzen und die gefundenen Zeilen zu entfernen:
Es müssen entsprechende Flags oder Optionen gesetzt werden, dass die Regex auch Zeilenumbrüche matcht. (In Notepad++ z.B. ".findet \r und \n" aktivieren)

-> löscht alle Kindelemente der Leistungen außer, IDSekundaer, ID und Klassifizierung.

<xzufi:modulText>(?:.)*?</xzufi:modulText>|<xzufi:kennzeichenEA>(?:.)*?</xzufi:kennzeichenEA>|<xzufi:leistungsadressat(?:.)*?</xzufi:leistungsadressat>|<xzufi:modulFrist>(?:.)*?</xzufi:modulFrist>|<xzufi:modulFachlicheFreigabe(?:.)*?</xzufi:modulFachlicheFreigabe>|<xzufi:kennzeichenSchriftformerfordernis>(?:.)*?</xzufi:kennzeichenSchriftformerfordernis>|<xzufi:informationsbereichSDG(?:.)*?</xzufi:informationsbereichSDG>|<xzufi:versionsinformation>(?:.)*?</xzufi:versionsinformation>|<xzufi:sprachversion>(?:.)*?</xzufi:sprachversion>|<xzufi:modulTextIndividuell>(?:.)*?</xzufi:modulTextIndividuell>|<xzufi:modulKosten>(?:.)*?</xzufi:modulKosten>|<xzufi:modulBegriffImKontext>(?:.)*?</xzufi:modulBegriffImKontext>|<xzufi:modulBearbeitungsdauer>(?:.)*?</xzufi:modulBearbeitungsdauer>|<xzufi:idLeistungImKontext>(?:.)*?</xzufi:idLeistungImKontext>|<xzufi:struktur(?:.)*?</xzufi:struktur>


-> Falls die Regex nicht alles entfernt, kann folgende probiert werden:

<xzufi:modulText>[\s\S]*?</xzufi:modulText>
<xzufi:modulTextIndividuell>[\s\S]*?</xzufi:modulTextIndividuell>
<xzufi:versionsinformation>[\s\S]*?</xzufi:versionsinformation>
<xzufi:modulFrist>[\s\S]*?</xzufi:modulFrist>
<xzufi:modulFachlicheFreigabe>[\s\S]*?</xzufi:modulFachlicheFreigabe>
<xzufi:modulBegriffImKontext>[\s\S]*?</xzufi:modulBegriffImKontext>
<xzufi:modulBearbeitungsdauer>[\s\S]*?</xzufi:modulBearbeitungsdauer>
<xzufi:modulKosten>[\s\S]*?</xzufi:modulKosten>
<xzufi:informationsbereichSDG .*?>.*?</xzufi:informationsbereichSDG>
<xzufi:struktur .*?>.*?</xzufi:struktur>


-> Lösche leere Zeilen:

^[ \t]*$\r?\n
Oder z.B. mit Notepad++ "Bearbeiten -> Zeilenoperationen -> Leerzeilen (auch mit Leerzeichen) löschen"
