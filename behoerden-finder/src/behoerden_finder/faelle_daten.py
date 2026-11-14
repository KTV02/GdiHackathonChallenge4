"""Tabelle gängiger Behördenanliegen: Formulierung -> Behördenkategorie.

Format: eine Zeile ``kategorie:`` leitet einen Block ein; darunter stehen Formulierungen, getrennt durch ``;``
oder Zeilenumbruch. Eine Formulierung trifft, wenn alle ihre Wörter (Wortstamm, beliebige Reihenfolge) in der
Eingabe vorkommen. Ein Eintrag mit mehr Wörtern ist spezifischer und zählt mehr.

Die Zuordnung ist Fachwissen des Projektteams und **nicht amtlich belegt**; wo Zuständigkeiten je nach
Bundesland oder Gemeinde unterschiedlich sind, steht die häufigste Lösung. Bitte bei Fehlern hier korrigieren.
"""

FAELLE = """
finanzamt:
  Steuererklärung abgeben; Steuererklärung machen; Steuererklärung einreichen; Steuererklärung Frist verlängern;
  Steuererklärung nachreichen; Steuererklärung berichtigen; Steuererklärung Hilfe; Steuererklärung Rentner;
  Steuererklärung Student; Steuererklärung für Vorjahre; Einkommensteuererklärung; Einkommensteuer; Steuerbescheid;
  Steuerbescheid Einspruch; Einspruch Steuerbescheid; Steuerbescheid nicht erhalten; Steuerbescheid falsch;
  Steuernummer beantragen; Steuernummer erhalten; Steuernummer vergessen; Steuer-ID erfragen; Steueridentifikationsnummer;
  Identifikationsnummer verloren; ELSTER; Elster Zugang; Elster Registrierung; Elster Zertifikat; Elster Freischaltcode;
  Steuerklasse ändern; Steuerklassenwechsel; Steuerklasse Heirat; Lohnsteuerklasse wechseln; Faktorverfahren;
  Freibetrag eintragen; Lohnsteuerermäßigung; Kinderfreibetrag; Werbungskosten absetzen; Werbungskosten;
  Homeoffice Pauschale Steuer; Steuerrückerstattung; Steuererstattung; Steuernachzahlung; Steuern nachzahlen;
  Steuerschulden; Steuerstundung; Steuer stunden; Steuererlass; Steuer erlassen; Steuervorauszahlung;
  Vorauszahlung Steuer anpassen; Umsatzsteuer; Umsatzsteuer Voranmeldung; Umsatzsteuervoranmeldung;
  Umsatzsteuererklärung; Umsatzsteuer-Identifikationsnummer; USt-IdNr; Kleinunternehmerregelung; Kleinunternehmer;
  fragebogen steuerliche Erfassung; steuerliche Erfassung; Existenzgründung Finanzamt; Freiberufler Finanzamt;
  Selbstständigkeit Finanzamt anmelden; freiberufliche Tätigkeit anmelden; Gewinnermittlung; Einnahmenüberschussrechnung;
  Körperschaftsteuer; Kapitalertragsteuer; Abgeltungsteuer; Erbschaftsteuer; Erbschaftsteuererklärung;
  Erbe Steuer anzeigen; Erbschaft Finanzamt melden; Schenkungsteuer; Schenkung melden; Schenkung anzeigen;
  Geld geschenkt bekommen; Haus geschenkt bekommen; Grundstück geschenkt; Grunderwerbsteuer; Grunderwerbsteuer Bescheid;
  Grundsteuererklärung; Grundsteuerwert; Feststellungserklärung Grundsteuer; Grundsteuermessbescheid; Spekulationssteuer;
  Immobilie verkaufen Steuer; Vermietung Steuer; Anlage V Vermietung; Mieteinnahmen versteuern; Zinsen versteuern;
  Aktiengewinn versteuern; Krypto Steuer; Bitcoin versteuern; Gewinn versteuern; Nebeneinkünfte angeben;
  Nebeneinkünfte versteuern; Nebenjob Steuer; Kinderbetreuungskosten absetzen; Spenden absetzen; Spendenbescheinigung Steuer;
  haushaltsnahe Dienstleistungen absetzen; Handwerkerleistungen absetzen; Pendlerpauschale; Entfernungspauschale;
  doppelte Haushaltsführung; Arbeitszimmer absetzen; Fortbildungskosten absetzen; Lohnsteuerbescheinigung;
  Lohnsteuerjahresausgleich; Ansässigkeitsbescheinigung; Freistellungsbescheinigung Bauabzugsteuer;
  Unbedenklichkeitsbescheinigung Finanzamt; Bescheinigung in Steuersachen; Säumniszuschlag; Verspätungszuschlag;
  Selbstanzeige Steuer; Steuerhinterziehung Selbstanzeige; Kontopfändung Finanzamt; Vollstreckung Finanzamt;
  Einspruch Finanzamt; Finanzamt Termin; Finanzamt Öffnungszeiten; Finanzamt Adresse; Finanzamt Kontakt; Finanzamt;
  Finanzamt zuständig; Steuern; Steuer; Steuerrecht Auskunft; verbindliche Auskunft Finanzamt; Rente versteuern;
  Rentner Steuer; Verlustvortrag; Steuerbefreiung Verein; Gemeinnützigkeit beantragen; Freistellungsbescheid Verein;
  Verein Gemeinnützigkeit; Steuerpflicht; Steuerpflichtig; Finanzbehörde; Betriebsprüfung; Umsatzsteuer Sonderprüfung;
  Steuerberater Vollmacht Finanzamt; Steuerklasse nach Scheidung; Sonderausgaben; außergewöhnliche Belastungen;
  Krankheitskosten absetzen; Unterhalt absetzen; Riester Zulage Steuer; Basisrente Steuer; Altersvorsorge absetzen;
  Kirchensteuer Finanzamt; Solidaritätszuschlag; Quellensteuer; Doppelbesteuerung; Wegzugsbesteuerung;
  Auslandseinkünfte versteuern; Erbschaft versteuern; Schenkung versteuern; Freibetrag Schenkung; Freibetrag Erbschaft

stadtverwaltung:
  Hund anmelden; Hund abmelden; Hundesteuer; Hundeanmeldung; Hundemarke; Hund ummelden; Hundehaltung anzeigen;
  Hundesteuer befreien; Zweitwohnungssteuer; Zweitwohnsitz Steuer; Vergnügungsteuer; Kurtaxe; Fremdenverkehrsabgabe;
  Müllabfuhr; Mülltonne bestellen; Mülltonne tauschen; Mülltonne abbestellen; Abfalltonne; Biotonne bestellen;
  Restmülltonne; Sperrmüll anmelden; Sperrmüll abholen; Sperrmülltermin; Müllgebühren; Abfallgebühren; Abfallkalender;
  Grünschnitt abholen; Gartenabfälle entsorgen; Wertstoffhof; Sondermüll abgeben; Elektroschrott abholen;
  Kita anmelden; Kindergarten anmelden; Kinderkrippe Platz;
  Kita Gebühren; Kindertagespflege Tagesmutter; Kitaplatz beantragen; Kita Gutschein; Friedhof; Grabstelle;
  Grabstelle verlängern; Grabnutzungsrecht; Grab auflösen; Urnengrab; Bestattung Friedhof; Friedhofsgebühren;
  Grabpflege; Friedhofsverwaltung; Straßenreinigung; Gehweg räumen; Winterdienst Gehweg; Schnee räumen Pflicht;
  Straßenbeleuchtung defekt; Schlagloch melden; Straßenschaden melden; Mängelmelder; Baum umgestürzt Straße;
  Spielplatz Schaden melden; Parkgebühren; Anwohnerparken; Parkausweis Anwohner; Straßenbeitrag;
  Anliegerbeitrag; Wasseranschluss; Wasserzähler; Abwassergebühren; Kanalanschluss;
  Anschlussbeitrag Wasser; Stadtwerke Anmeldung; Grundbesitzabgaben; Grundsteuerbescheid Gemeinde; Grundsteuer zahlen;
  Gewerbesteuer Gemeinde; Hebesatz; Ratsinformation; Gemeinderat Sitzung; Bürgermeister sprechen; Bürgermeister Termin;
  Rathaus Öffnungszeiten; Rathaus; Stadtverwaltung; Gemeindeverwaltung; Verbandsgemeinde; Stadt Kontakt;
  Gemeinde Kontakt; Bürgerbegehren; Bürgerentscheid; Bürgerhaushalt;
  Ehrenamtskarte; Stadtbibliothek Ausweis; Volkshochschule Anmeldung; Schwimmbad Eintritt Ermäßigung; Familienpass;
  Sozialpass; Stadtpass; Marktstand Platz; Wochenmarkt Standplatz; Straßenfest anmelden; Sondernutzung Gehweg;
  Halteverbotszone Umzug; Halteverbot beantragen; Baustelle Straßenverkehr;
  Veranstaltung anmelden; Stadtfest; Kinderspielplatz; Obdachlosigkeit Stadt; Obdachlosenunterkunft; Notunterkunft Stadt;
  Wohnungsnotfall; Räumungsklage Stadt; Mietschulden Stadt; Verwaltungsgebühren; Gebührenbescheid Stadt; Widerspruch Stadt;
  Beschwerde Stadtverwaltung; Anregung Stadtverwaltung; Fundbüro; Fundsache abgeben; Fundsache abholen; Fahrrad gefunden;
  Geldbörse gefunden; Schlüssel verloren Fundbüro; Portemonnaie verloren Fundbüro; Fundsache Zug

ordnungsamt:
  Ruhestörung melden; Lärmbelästigung melden; Nachbar Lärm; Lärm Nachbarn; Lärmbeschwerde; Nachtruhe gestört;
  Musik zu laut; Baulärm melden; Gaststätte Lärm; Falschparker melden; falsch parken Anzeige; Auto blockiert Einfahrt;
  Parkverstoß melden; Knöllchen Einspruch; Bußgeldbescheid Einspruch; Verwarnungsgeld; Bußgeld Stadt; Ordnungswidrigkeit;
  Ordnungswidrigkeit anzeigen; Anhörungsbogen Ordnungswidrigkeit; Verkehrsverstoß Bußgeld; Parkknöllchen; Strafzettel;
  Strafzettel Einspruch; Abschleppen Auto; Auto abgeschleppt; abgeschlepptes Fahrzeug abholen; Fahrzeug abschleppen lassen;
  Schrottauto Straße melden; Fahrzeugwrack entfernen; herrenloses Fahrrad; Fahrrad herrenlos melden; Fahrradwrack;
  Sperrmüll illegal melden; wilde Müllkippe melden; Müll abgeladen; Graffiti melden; Vandalismus melden;
  Vermüllung melden; Taubenplage; Taubenfütterung; Ungeziefer Nachbar; Wespennest entfernen;
  Hornissennest; Bienen Schwarm; gefährlicher Hund melden; Hundebiss melden; Hund Leinenpflicht; Hundekot melden;
  Kampfhund Erlaubnis; Rasseliste Hund Erlaubnis; Hundehaltung Erlaubnis;
  Hundeführerschein Nachweis; Tierhaltung Nachbar; Wildtier gefunden; Fundtier abgeben; Fundtier;
  Katze gefunden; Hund gefunden; Hund entlaufen; Tier entlaufen; Notunterbringung Obdachlos; Zwangsräumung;
  Schlüsseldienst Notöffnung; Waffenbesitzkarte; Waffenschein; kleiner Waffenschein; Waffenerlaubnis; Waffe anmelden;
  Schreckschusswaffe; Waffensachkunde; Gaststättenerlaubnis; Gaststättenkonzession; Schankerlaubnis;
  Gestattung vorübergehend Gaststätte; Straßenfest Erlaubnis; Veranstaltung Genehmigung; Veranstaltung Sicherheit;
  Feuerwerk Genehmigung; Feuerwerk Erlaubnis; Silvesterfeuerwerk Verbot; Lagerfeuer Erlaubnis; Osterfeuer anmelden;
  Brauchtumsfeuer; Demonstration anmelden; Versammlung anmelden; Kundgebung anmelden; Flohmarkt anmelden;
  Trödelmarkt Genehmigung; Plakatieren Genehmigung; Werbeanlage Genehmigung; Sondernutzungserlaubnis; Straßenmusik Erlaubnis;
  Straßenverkauf Erlaubnis; Spielhalle Erlaubnis; Spielautomat Aufstellen; Sperrzeit Verkürzung;
  Sperrzeitverkürzung; Spielbank; Lotterie Genehmigung; Tombola Genehmigung; Glücksspiel Erlaubnis; Prostitution anmelden;
  Prostituiertenschutzgesetz Anmeldung; Fischereischein; Fischereiabgabe; Angelschein;
  Drohne Erlaubnis; Fundsachen Ordnungsamt; Obdachlose melden; Betrunkene Hilfe; Ordnungsamt; Ordnungsbehörde;
  Ordnungsamt melden; Ordnungsamt Kontakt; Verkehrsüberwachung; Verkehrskontrolle Stadt; Blitzer Beschwerde;
  Geschwindigkeitskontrolle Anwohner; Radarkontrolle Bußgeld; Sicherheitsleistung Ordnungsamt; Bußgeld Hundehaltung

buergeramt:
  Personalausweis beantragen; Personalausweis verlängern; Personalausweis abgelaufen; Personalausweis verloren;
  Personalausweis gestohlen; Personalausweis Termin; Personalausweis Kosten; Personalausweis Abholung; Personalausweis Verlust melden;
  Personalausweis Online-Funktion; Personalausweis PIN vergessen; Personalausweis ändern; Personalausweis Adresse ändern;
  Personalausweis Kind; Ausweis beantragen; Ausweis erneuern; Ausweis abgelaufen; Ausweis verloren; Ausweis Verlust;
  Ausweis gestohlen; Ausweis Termin; Ausweis abholen; Perso beantragen; Perso abgelaufen; Perso verloren; Reisepass beantragen;
  Reisepass verlängern; Reisepass abgelaufen; Reisepass verloren; Reisepass gestohlen; Reisepass Termin; Reisepass Kosten;
  Reisepass Express; Reisepass schnell; Reisepass Kind; Kinderreisepass beantragen; Kinderreisepass verlängern;
  Kinderreisepass; vorläufiger Reisepass; vorläufiger Personalausweis; Passfoto Anforderung; biometrisches Foto;
  Pass beantragen; Pass abgelaufen; Pass verloren; Pass abholen; Wohnsitz anmelden; Wohnsitz ummelden;
  Wohnsitz abmelden; Wohnsitz ändern; Wohnsitzbestätigung; Wohnsitzanmeldung; Wohnung anmelden; Wohnung ummelden;
  Wohnung abmelden; neue Wohnung anmelden; Umzug anmelden; Umzug melden; Umzug Meldebehörde; Umzug ummelden;
  Umzug Adresse ändern; umgezogen anmelden; ich ziehe um anmelden; Anmeldung neuer Wohnsitz; Anmeldung Wohnsitz Deutschland;
  Anmeldung Zweitwohnsitz; Zweitwohnsitz anmelden; Nebenwohnsitz anmelden; Hauptwohnsitz ändern; Hauptwohnsitz anmelden;
  Auslandsumzug abmelden; Abmeldung Ausland; ins Ausland ziehen abmelden; Abmeldung ins Ausland; Anmeldung aus dem Ausland;
  Meldebescheinigung; Meldebescheinigung beantragen; Meldebescheinigung online; Meldebescheinigung Kosten; Aufenthaltsbescheinigung;
  Meldeauskunft; einfache Melderegisterauskunft; erweiterte Melderegisterauskunft; Melderegisterauskunft; Auskunftssperre;
  Übermittlungssperre; Widerspruch Datenweitergabe Meldeamt; Meldeamt; Einwohnermeldeamt; Meldebehörde; Meldestelle;
  Bürgeramt; Bürgerbüro; Bürgerservice; Bürgerbüro Termin; Bürgeramt Termin; Bürgeramt Öffnungszeiten; Termin Bürgeramt;
  Termin Bürgerbüro; Terminvereinbarung Bürgeramt; Führungszeugnis beantragen; Führungszeugnis online; Führungszeugnis erweitert;
  erweitertes Führungszeugnis; Führungszeugnis Ehrenamt; Führungszeugnis Arbeitgeber; polizeiliches Führungszeugnis;
  Führungszeugnis Kosten; Führungszeugnis Ausland; Führungszeugnis Beantragung; Beglaubigung Kopie; Kopie beglaubigen;
  Dokumente beglaubigen; amtliche Beglaubigung; Unterschrift beglaubigen; Unterschriftsbeglaubigung; beglaubigte Kopie;
  Beglaubigung Zeugnis; Zeugnis beglaubigen; Urkunde beglaubigen; Abschrift beglaubigen; Lebensbescheinigung;
  Wahlbenachrichtigung; Wahlschein; Briefwahl beantragen;
  Briefwahl; Wahlbenachrichtigung nicht erhalten; Wählerverzeichnis; Wahlrecht Auslandsdeutsche; Wählen Ausland;
  Wahlhelfer werden; Bewohnerparken Meldebehörde; Lohnsteuerkarte; Bürgerkonto; BundID; Online-Ausweisfunktion freischalten;
  eID freischalten; AusweisApp; Ausweis PIN; Entsperrcode Ausweis; PUK Ausweis; Sperrkennwort Ausweis;
  Ausweis sperren lassen; Ausweis Sperrung; Ausweis Verlust Polizei melden; Reisedokument; Reisedokument verloren;
  Notreisepass; Ausweisdokument; Ausweispapiere; Passbehörde; Personalausweisbehörde; Ausweisbehörde; Staatsangehörigkeit nachweisen;
  Staatsangehörigkeitsausweis; Ausweisersatz; Ersatzausweis; Personalausweis Namensänderung; Ausweis Namensänderung;
  Ausweis Anschriftenänderung; Aufkleber Adresse Ausweis; Ausweis neue Adresse; Pass neue Adresse; Pass Namensänderung

standesamt:
  heiraten; Heirat; heiraten Anmeldung; Eheschließung; Eheschließung anmelden; Hochzeit anmelden; Hochzeit Standesamt;
  standesamtliche Trauung; standesamtliche Hochzeit; Trauung Termin; Trautermin; Trauung buchen; Trauzeugen;
  Trauzeuge Anforderung; Ehefähigkeitszeugnis; Ehefähigkeitszeugnis Ausland; Heirat Ausland anerkennen;
  Heirat im Ausland; Heirat Ausländer; Heirat mit Ausländer; Hochzeit Ausland anerkennen; Eheurkunde; Heiratsurkunde;
  Heiratsurkunde beantragen; Heiratsurkunde international; beglaubigte Heiratsurkunde; Eheurkunde beantragen;
  Eheurkunde verloren; Familienbuch; Auszug Familienbuch; Namensänderung Heirat; Name nach Heirat; Ehename;
  Ehenamen bestimmen; Familienname ändern; Nachname ändern; Nachname ändern Heirat; Nachname Scheidung ändern;
  Name nach Scheidung; Geburtsname annehmen; Doppelname; Namensänderung; Namensänderung Kind; Kindesname;
  Kindesname bestimmen; Vorname ändern; Vornamensänderung; Vornamen ändern; Geschlechtseintrag ändern; Geschlechtseintrag;
  Personenstand ändern; Personenstandsurkunde; Personenstandsregister; Personenstandsurkunde beantragen; Lebenspartnerschaft;
  Lebenspartnerschaft eintragen; eingetragene Lebenspartnerschaft; Lebenspartnerschaft Umwandlung Ehe; Ehe für alle;
  gleichgeschlechtliche Ehe; Geburt anmelden; Geburt anzeigen; Geburt melden; Geburtsanzeige; Geburtsurkunde;
  Geburtsurkunde beantragen; Geburtsurkunde Kind; Geburtsurkunde verloren; Geburtsurkunde Ausland; Geburtsurkunde international;
  Geburtsurkunde mehrsprachig; Geburtsurkunde Elterngeld; Geburtsurkunde Kindergeld; Geburtsurkunde Kopie;
  Geburtsurkunde beglaubigt; Geburtsurkunde nachträglich; Geburtsregister; Geburtseintrag; Geburt nachbeurkunden;
  Auslandsgeburt nachbeurkunden; im Ausland geboren; Kind im Ausland geboren; Geburt Ausland anerkennen; Kind geboren;
  Baby geboren Papiere; Baby Anmeldung; Neugeborenes anmelden; Kind Namen geben; Vaterschaftsanerkennung;
  Vaterschaft anerkennen; Vaterschaftsanerkennung Standesamt; Vaterschaftsanerkennung vor Geburt; Sorgeerklärung;
  gemeinsames Sorgerecht Standesamt; Adoption Namensänderung; Sterbefall; Sterbefall melden; Sterbefall anzeigen;
  Todesfall; Todesfall melden; Todesfall Standesamt; Sterbeurkunde; Sterbeurkunde beantragen; Sterbeurkunde Kopie;
  Sterbeurkunde Erbschein; Sterbeurkunde Bank; Sterbeurkunde Versicherung; Sterbeurkunde mehrfach; Sterbeurkunde Ausland;
  Sterbeurkunde international; Todesbescheinigung; Todesbescheinigung Standesamt; Todesanzeige Standesamt; Verstorbener melden;
  Angehöriger gestorben; Vater gestorben melden; Mutter gestorben melden; Ehepartner gestorben; Bestattung anmelden;
  Bestattungsfrist; Beisetzung Sterbeurkunde; Leichenschau Schein; Totenschein; Sterbefall Ausland melden;
  Urkunden beantragen; Urkunde aus Personenstandsregister; Urkunde Standesamt; Standesamt; Standesbeamter; Standesamt Termin;
  Standesamt Öffnungszeiten; Standesamt Kontakt; Standesamt zuständig; Standesamt Trauung; Standesamt Gebühren; Eheschließung Gebühren;
  Eheschließung Unterlagen; Heirat Unterlagen; Heirat Dokumente; Heirat Voraussetzungen; Hochzeit Dokumente; Heiraten Behörde;
  Trauung außerhalb Standesamt; Trauung im Freien; Trauung Burg; Trauung Schloss; Silberhochzeit Jubiläum Stadt; Eheversprechen
"""

FAELLE_RECHT = """
auslaenderbehoerde:
  Aufenthaltserlaubnis beantragen; Aufenthaltserlaubnis verlängern; Aufenthaltserlaubnis abgelaufen; Aufenthaltserlaubnis Termin;
  Aufenthaltserlaubnis Arbeit; Aufenthaltserlaubnis Studium; Aufenthaltserlaubnis Familiennachzug; Aufenthaltserlaubnis Ehegatte;
  Aufenthaltserlaubnis Kind; Aufenthaltserlaubnis ändern; Aufenthaltserlaubnis verloren; Aufenthaltstitel; Aufenthaltstitel verlängern;
  Aufenthaltstitel beantragen; Aufenthaltstitel Termin; Aufenthaltstitel verloren; Aufenthaltstitel abgelaufen;
  Aufenthaltstitel Wohnort ändern; Aufenthaltstitel Adresse ändern; elektronischer Aufenthaltstitel; eAT; Aufenthaltskarte;
  Aufenthaltskarte EU Familienangehörige; Freizügigkeitsbescheinigung; Daueraufenthalt EU; Daueraufenthaltsrecht;
  Niederlassungserlaubnis; Niederlassungserlaubnis beantragen; unbefristete Aufenthaltserlaubnis; Blaue Karte EU;
  Blaue Karte beantragen; Fachkräfteeinwanderung; Chancenkarte; Arbeitsvisum; Arbeitserlaubnis Ausländer; Beschäftigungserlaubnis;
  Beschäftigung Ausländer Erlaubnis; Visum beantragen; Visum verlängern; Visum Deutschland; Visumantrag; Visum Familiennachzug;
  Visum Studium; Visum Arbeit; Visum Touristen verlängern; Schengen Visum verlängern; Visa Termin; Einladung Visum;
  Verpflichtungserklärung; Verpflichtungserklärung Einladung; Einladung Besuch Ausland; Besuchsvisum Einladung; Familiennachzug;
  Familiennachzug beantragen; Ehegattennachzug; Kindernachzug; Nachzug Eltern; Einbürgerung; Einbürgerung beantragen;
  Einbürgerungstest; Einbürgerung Voraussetzungen; Einbürgerung Termin; Einbürgerungsantrag; deutsche Staatsbürgerschaft beantragen;
  deutscher Pass Ausländer; Staatsangehörigkeit beantragen; Staatsangehörigkeit erwerben; doppelte Staatsbürgerschaft beantragen;
  Staatsangehörigkeit Feststellung; Staatsangehörigkeit Beibehaltungsgenehmigung; Entlassung Staatsangehörigkeit; Duldung;
  Duldung verlängern; Duldung Arbeit; Aufenthaltsgestattung; Ausbildungsduldung; Beschäftigungsduldung; Abschiebung;
  Abschiebung verhindern; Abschiebung Aufschub; Ausweisung; Ausweisung Einspruch; Härtefallkommission; Härtefallantrag;
  Bleiberecht; Bleiberecht Antrag; Aufenthaltsrecht; Aufenthaltsrecht prüfen; Aufenthalt Ausländer; Ausländerbehörde;
  Ausländeramt; Ausländerbehörde Termin; Ausländerbehörde Öffnungszeiten; Ausländerbehörde Kontakt; Ausländerbehörde zuständig;
  Ausländerbehörde Adresse; Fiktionsbescheinigung Ausländer; Passersatz; Reiseausweis für Ausländer; Reiseausweis Flüchtling;
  Reiseausweis Staatenlose; Staatenlos; Flüchtlingspass; Aufenthaltstitel Flüchtling; Aufenthalt Studium Ausländer;
  Studienvisum; Aufenthaltserlaubnis Student verlängern; Praktikum Aufenthalt; Au-pair Visum; Au-pair Aufenthaltserlaubnis;
  Sprachkurs Visum; Opferschutz Aufenthaltserlaubnis; Aufenthalt humanitär; Aufenthalt Ukraine; Ukraine Aufenthaltserlaubnis;
  Schutz Ukraine Aufenthalt; Aufenthaltserlaubnis Ukraine; Ukraine Flüchtling Aufenthaltstitel; Anerkennung Aufenthalt;
  Mitwirkungspflicht Ausländerbehörde; Wohnsitzauflage; Wohnsitzauflage aufheben; Umverteilung Flüchtling;
  Verlassen Bundesland Erlaubnis; Aufenthalt Verlängerung Termin; Termin Aufenthalt

bundesamt_migration:
  Asylantrag; Asyl beantragen; Asyl; Asylantrag stellen; Asylverfahren; Asylbewerber; Asylbescheid;
  Asyl Anhörung; Asyl Klage; Asylstatus; Flüchtlingsschutz; Flüchtlingsstatus; Flüchtlingsanerkennung; subsidiärer Schutz;
  Integrationskurs; Integrationskurs anmelden; Integrationskurs Berechtigung; Integrationskurs Kosten;
  Integrationskurs Abbruch; Integrationskurs Wiederholung; Orientierungskurs; Deutschtest Zuwanderer; Sprachkurs Migranten;
  Berufssprachkurs; Berufssprachkurs Anmeldung; DTZ Prüfung; Deutsch-Test für Zuwanderer; Migrationsberatung; BAMF;
  Bundesamt für Migration; BAMF Termin; BAMF Bescheid; BAMF Kontakt; Dublin Verfahren; Dublin Bescheid; Asylfolgeantrag;
  Asyl Widerruf; Rückkehrförderung; freiwillige Ausreise Förderung; Rückkehrberatung; Resettlement; Aufnahmeprogramm;
  Ankunftszentrum; Ankunftsnachweis; Erstaufnahme; Erstaufnahmeeinrichtung; Aufnahmeeinrichtung

amtsgericht:
  Scheidung; Scheidung einreichen; Scheidung beantragen; Scheidungsantrag; Scheidungsantrag stellen; Scheidung Anwalt;
  Scheidung Kosten; Scheidung Dauer; Scheidung Verfahren; Scheidung Trennungsjahr; Trennungsjahr; Scheidung online;
  einvernehmliche Scheidung; einvernehmliche Scheidung ohne Anwalt; Scheidung ohne Anwalt; sich scheiden lassen;
  scheiden lassen; Ehe beenden; Ehe auflösen; Ehe annullieren; Scheidungstermin; Scheidungsverfahrenskosten; Versorgungsausgleich;
  Versorgungsausgleich Scheidung; Zugewinnausgleich; Zugewinnausgleich Scheidung; Ehevertrag Scheidung; Trennung Ehe;
  Trennung Eheleute Wohnung; Getrenntleben; Getrenntleben Nachweis; Unterhalt Ehegatte; Ehegattenunterhalt; Trennungsunterhalt;
  Kindesunterhalt Klage; Unterhaltsklage; Unterhalt einklagen; Unterhaltstitel; Unterhaltstitel beantragen; Umgangsrecht;
  Umgangsrecht beantragen; Umgang Kind Gericht; Sorgerecht beantragen; Sorgerecht Gericht; Sorgerechtsentzug; alleiniges Sorgerecht;
  alleiniges Sorgerecht beantragen; gemeinsames Sorgerecht Gericht; Sorgerecht Streit; Sorgerecht Scheidung; Kindschaftssache;
  Familiengericht; Familiengericht Termin; Familiengericht Antrag; Familiensache; Gewaltschutzgesetz; Gewaltschutz Antrag;
  Kontaktverbot Gericht; Näherungsverbot; Wohnungszuweisung; Wohnungszuweisung Scheidung; Gewaltschutzanordnung;
  einstweilige Anordnung; einstweilige Verfügung; einstweilige Verfügung beantragen; Verfahrenskostenhilfe; Verfahrenskostenhilfe Scheidung;
  Prozesskostenhilfe; Prozesskostenhilfe beantragen; Beratungshilfe; Beratungshilfeschein; Beratungshilfe beantragen;
  Beratungshilfe Anwalt; Rechtsantragstelle; Rechtsantragsstelle; Rechtsantragstelle Termin; Klage einreichen; Klage erheben;
  Klage Amtsgericht; Klage Mieter; Mahnbescheid; Mahnbescheid beantragen; Mahnbescheid online; Mahnbescheid Widerspruch;
  Mahnverfahren; Mahngericht; Vollstreckungsbescheid; Vollstreckungsbescheid Widerspruch; Vollstreckungsbescheid Einspruch;
  Zwangsvollstreckung; Gerichtsvollzieher; Gerichtsvollzieher beauftragen; Gerichtsvollzieher Termin; Vermögensauskunft;
  eidesstattliche Versicherung; eidesstattliche Versicherung Schulden; Schuldnerverzeichnis; Schuldnerverzeichnis Auskunft;
  Schuldnerverzeichnis Löschung; Privatinsolvenz; Privatinsolvenz beantragen; Verbraucherinsolvenz; Insolvenzantrag;
  Insolvenzantrag stellen; Insolvenzverfahren; Insolvenzgericht; Restschuldbefreiung; Restschuldbefreiung beantragen;
  Insolvenz Firma anmelden; Insolvenz anmelden; Pfändung Konto; Kontopfändung; P-Konto Bescheinigung; Pfändungsschutzkonto;
  Pfändungsschutz Gericht; Lohnpfändung; Lohnpfändung Widerspruch; Zwangsversteigerung; Zwangsversteigerung Immobilie;
  Zwangsverwaltung; Räumungstitel; Räumungsklage Vermieter; Räumungsfrist; Räumungsschutz; Mietklage; Mietprozess;
  Nachlassgericht; Nachlassgericht Termin; Erbschein; Erbschein beantragen; Erbschein Kosten; Erbschein Bank; Erbschein online;
  Erbscheinantrag; Erbausschlagung; Erbschaft ausschlagen; Erbe ausschlagen; Erbschaft ausschlagen Frist; Ausschlagung Erbe;
  Erbausschlagung Kind; Testament hinterlegen; Testament Hinterlegung; Testament abgeben; Testament eröffnen; Testamentseröffnung;
  Testament Nachlassgericht; Testament finden; Testament Gericht; Erbvertrag hinterlegen; Erbvertrag; Testamentsregister;
  Zentrales Testamentsregister; Nachlasspfleger; Nachlasspflegschaft; Nachlassverwalter; Nachlassinsolvenz; Nachlassverzeichnis;
  Nachlassverzeichnis Gericht; Erbengemeinschaft; Miterben Streit; Erbfall Gericht; Erbfall melden Gericht; Todesfall Erbschein;
  Todesfall Nachlassgericht; Betreuung beantragen; rechtliche Betreuung; rechtliche Betreuung beantragen; Betreuungsgericht;
  Betreuer bestellen; Betreuer werden; Betreuer Antrag; Betreuungsverfahren; Betreuungsverfügung; Vorsorgevollmacht;
  Vorsorgevollmacht hinterlegen; Vorsorgevollmacht Register; Patientenverfügung hinterlegen; Vorsorgeregister; Betreuung aufheben;
  Betreuung Eltern; Betreuer wechseln; Betreuer Beschwerde; Unterbringung Betreuung; Unterbringung geschlossen;
  Zwangseinweisung Psychiatrie; Pflichtbetreuer; Vormundschaft; Vormund bestellen; Vormund werden; Pflegschaft; Ergänzungspflegschaft;
  Ergänzungspfleger; Grundbuch; Grundbuchamt; Grundbuchauszug; Grundbuchauszug beantragen; Grundbucheinsicht; Grundbuch einsehen;
  Grundbuchberichtigung; Grundbucheintragung; Eigentümerwechsel Grundbuch; Eintragung Grundbuch; Auflassungsvormerkung; Grundschuld;
  Grundschuld löschen; Grundschuld eintragen; Grundschuldlöschung; Löschungsbewilligung; Wohnungsrecht eintragen; Nießbrauch eintragen;
  Altenteil Grundbuch; Wegerecht Grundbuch; Dienstbarkeit Grundbuch; Teilungserklärung; Handelsregister; Handelsregisterauszug;
  Handelsregister Eintragung; Handelsregister Anmeldung; Handelsregister Änderung; Handelsregister einsehen; GmbH Eintragung;
  GmbH gründen Handelsregister; GmbH Löschung; Firma ins Handelsregister; Prokura eintragen; Registergericht; Vereinsregister;
  Verein eintragen; Verein gründen eintragen; e.V. gründen; Verein Eintragung; Vereinsregister Änderung; Vorstandswechsel Verein;
  Vereinsregisterauszug; Genossenschaftsregister; Partnerschaftsregister; Güterrechtsregister; Gütertrennung eintragen;
  Gütergemeinschaft; Amtsgericht; Amtsgericht Termin; Amtsgericht Öffnungszeiten; Amtsgericht Kontakt; Amtsgericht Adresse;
  Amtsgericht zuständig; Gericht zuständig; Gerichtstermin; Gerichtstermin verschieben; Ladung Amtsgericht; Zeuge Ladung;
  Zeugenladung; Zeugenentschädigung; Schöffe werden; Schöffenwahl; Schiedsmann; Schiedsstelle; Schlichtung Gericht;
  Güteverfahren; Güteverhandlung; Namensänderung Gericht; Öffentliche Beglaubigung Gericht; Eidesstattliche Versicherung abgeben;
  Kirchenaustritt; Kirchenaustritt Amtsgericht; aus der Kirche austreten; Austritt Kirche; Kirche austreten

landgericht:
  Landgericht; Landgericht Termin; Landgericht Klage; Landgericht Berufung; Berufung Landgericht; Zivilkammer; Strafkammer;
  Landgericht Anwaltszwang; Landgericht Streitwert; Landgericht Kammer; Landgericht Prozesskostenhilfe

verwaltungsgericht:
  Verwaltungsgericht; Verwaltungsgericht Klage; Klage Verwaltungsgericht; Verwaltungsgericht Eilantrag; Verwaltungsgericht Widerspruch;
  Verwaltungsklage; Anfechtungsklage; Untätigkeitsklage; Klage gegen Behörde; Klage gegen Bescheid; Klage gegen Ablehnung;
  Verwaltungsrechtsweg; Verwaltungsstreit

staatsanwaltschaft:
  Staatsanwaltschaft; Staatsanwaltschaft Anzeige; Staatsanwaltschaft Akteneinsicht; Staatsanwaltschaft Einstellung; Ermittlungsverfahren;
  Ermittlungsverfahren Ladung; Strafverfahren; Strafbefehl; Strafbefehl Einspruch; Geldstrafe; Geldstrafe zahlen; Geldstrafe Raten;
  Ratenzahlung Geldstrafe; Ersatzfreiheitsstrafe; Ersatzfreiheitsstrafe abwenden; Bewährung; Strafvollstreckung; Zeuge Staatsanwaltschaft

polizei:
  Anzeige erstatten; Anzeige erstatten Polizei; Strafanzeige; Strafanzeige stellen; Strafanzeige online; Onlinewache;
  Online Anzeige; Diebstahl; Diebstahl anzeigen; Diebstahl melden; Fahrraddiebstahl; Fahrrad gestohlen; Fahrrad Diebstahl anzeigen;
  Handy gestohlen; Handy Diebstahl melden; Handy verloren Polizei; Portemonnaie gestohlen; Geldbörse gestohlen; Tasche gestohlen;
  Auto gestohlen; Auto aufgebrochen; Autoeinbruch; Kennzeichen gestohlen; Kennzeichendiebstahl; Motorrad gestohlen; Roller gestohlen;
  Einbruch; Einbruch melden; Einbruch Wohnung; Einbruch Haus; Einbruchschutz Beratung; Einbruchschutzberatung; Polizei Beratung;
  Kriminalpolizei Beratung; Betrug; Betrug anzeigen; Betrug melden; Onlinebetrug; Internetbetrug; Internetbetrug anzeigen;
  Phishing anzeigen; Betrugsmasche; Enkeltrick; falscher Polizist; Schockanruf; Datendiebstahl; Identitätsdiebstahl; Identitätsmissbrauch;
  Cyberkriminalität; Cybercrime Anzeige; Hackerangriff Anzeige; Erpressung; Erpressung anzeigen; Bedrohung; Bedrohung anzeigen;
  Nötigung; Stalking; Stalking anzeigen; Belästigung anzeigen; Beleidigung anzeigen; Beleidigung online; Hasskommentar anzeigen;
  Hassrede melden; Volksverhetzung melden; Körperverletzung; Körperverletzung anzeigen; Schlägerei; Gewalt anzeigen; häusliche Gewalt;
  häusliche Gewalt Hilfe; häusliche Gewalt melden; Gewalt in der Ehe; Gewalt Partner; Vergewaltigung anzeigen; sexuelle Belästigung anzeigen;
  Sachbeschädigung; Sachbeschädigung anzeigen; Vandalismus; Graffiti Anzeige; Brandstiftung; Verkehrsunfall; Unfall melden Polizei;
  Verkehrsunfall Anzeige; Unfallflucht; Fahrerflucht; Fahrerflucht melden; Unfallaufnahme; Unfallbericht; Unfallbericht anfordern;
  Unfallprotokoll Polizei; Akteneinsicht Polizei; Anzeige zurückziehen; Anzeige zurücknehmen; Strafantrag; Strafantrag stellen;
  Strafantrag zurücknehmen; Vermisstenanzeige; Vermisst melden; Person vermisst; Kind vermisst; Kind vermisst Polizei;
  Waffenfund; Fundmunition; Munition gefunden; Drogen melden; Drogenhandel melden; Drogenhandel Nachbarschaft; Drogenkonsum melden;
  Notruf; Notruf 110; Polizei rufen; Polizei Notruf; Polizei Kontakt; Polizei Dienststelle; Polizeidienststelle; Polizeirevier;
  Polizeiinspektion; Polizeipräsidium; Polizeiwache; Polizeiwache Öffnungszeiten; Polizei Adresse; Polizei Telefonnummer;
  Polizei Nummer; Polizei Termin; Polizeiliche Beratung; Opferberatung; Opferhilfe; Opferschutz; Zeugenschutz; Zeuge Polizei;
  Vorladung Polizei; Vorladung als Beschuldigter; Beschuldigtenvernehmung; Anhörungsbogen Polizei;
  Anhörungsbogen Straftat; Kriminalpolizei; Kripo; Staatsschutz melden; Terror melden; Bombendrohung; Verdächtige Person melden;
  verdächtige Beobachtung; Wohnungseinbruch; Ladendiebstahl; Taschendiebstahl; Trickdiebstahl; Fahrraddiebstahl melden;
  Fahrradcodierung; Fahrrad codieren; Fahrradregistrierung; Fahrrad registrieren Polizei; Fahrrad Seriennummer Polizei;
  Wertgegenstände kennzeichnen; Verlustanzeige; Verlustmeldung; Verlust Polizei melden; Ausweis Verlust Polizei;
  Ausweis gestohlen Polizei; Führerschein verloren Polizei; Kfz Schein gestohlen; Polizeibericht

zoll:
  Zoll; Zollamt; Hauptzollamt; Zollverwaltung; Zollanmeldung; Zoll anmelden; Zollerklärung; Zollgebühren; Zollabfertigung;
  Zoll Paket; Zoll Paket abholen; Paket Zoll abholen; Zollstelle; Zoll Paket Ausland; Zollinhaltserklärung; Einfuhr; Einfuhrumsatzsteuer;
  Einfuhrzoll; Einfuhrabgaben; Einfuhr aus China; Einfuhr aus den USA; Einfuhr Großbritannien; Ausfuhr; Ausfuhrerklärung;
  Ausfuhranmeldung; Ausfuhrbescheinigung; Ausfuhrnachweis; Ausfuhrgenehmigung; EORI Nummer; EORI beantragen; Warenverkehr Zoll;
  Reisen Bargeld Zoll; Bargeld Einreise anmelden; Bargeld mitführen; Bargeld über 10000 Euro Reise; Zigaretten Einfuhr; Alkohol Einfuhr;
  Mitbringsel Zollfreigrenze; Zollfreigrenze; Reisefreimenge; Zollfreimenge; Tabaksteuer; Alkoholsteuer; Branntweinsteuer; Biersteuer;
  Kaffeesteuer; Energiesteuer; Stromsteuer; Stromsteuer Erlass; Energiesteuer Entlastung; Kraftfahrzeugsteuer; Kfz-Steuer;
  Kfz Steuer zahlen; Kfz Steuer Bescheid; Kfz Steuer Rückerstattung; Kfz Steuer Befreiung; Kfz-Steuer Schwerbehinderung;
  Kfz Steuer Elektroauto; Kfz-Steuer Lastschrift; Kfz Steuer Rückstand; Schwarzarbeit; Schwarzarbeit melden; Schwarzarbeit anzeigen;
  Mindestlohn; Mindestlohn melden; Mindestlohn Verstoß; Mindestlohn Kontrolle; Arbeitnehmerentsendung; Entsendung Meldung;
  Zoll Finanzkontrolle Schwarzarbeit; Finanzkontrolle Schwarzarbeit; Zoll Artenschutz; Artenschutz Einfuhr; Tiere Einfuhr Zoll;
  Pflanzen Einfuhr Zoll; Waffen Einfuhr Zoll; Medikamente Einfuhr Zoll; Lebensmittel Einfuhr Zoll; Zollverfahren; Zollrechtlich;
  Zoll Einspruch; Zollbescheid; Zollbescheid Einspruch; Zollschuld; Zollkontrolle; Zollfahndung; Grenzkontrolle Zoll;
  Warenbegleitschein; Zollager; Zolllager; Verbrauchsteuern; Verbrauchsteuer; Verbrauchsteuer Antrag; Biersteuer Brauerei;
  Sektsteuer; Spielkartensteuer; Luftverkehrsteuer; Versicherungsteuer; Spirituosen Brennerei Anmeldung; Brennerei anmelden;
  Zigarettensteuer; Wasserpfeifentabak Steuer; E-Zigarette Steuer; Tabaksteuerzeichen; Pkw Einfuhr Zoll; Auto Import Zoll;
  Fahrzeugeinfuhr; Fahrzeug Import Zoll; Umzugsgut Zoll; Umzug Ausland Zoll; Umzugsgut verzollen; Erbschaft Ausland Zoll
"""

FAELLE_SOZIALES = """
jugendamt:
  Jugendamt; Jugendamt Termin; Jugendamt Kontakt; Jugendamt Beratung; Jugendamt Hilfe; Jugendhilfe; Hilfe zur Erziehung;
  Erziehungsberatung; Familienberatung; Familienberatung Stadt; Erziehungshilfe; Familienhilfe; sozialpädagogische Familienhilfe;
  Familienhelfer; Erziehungsbeistand; Heimunterbringung Kind; Pflegefamilie; Pflegefamilie werden; Pflegekind aufnehmen;
  Pflegeeltern werden; Bereitschaftspflege; Adoption; Adoption beantragen; Adoptionsvermittlung; Stiefkindadoption;
  Kind adoptieren; Adoption Kosten; Adoptionsverfahren; Inobhutnahme; Kindeswohlgefährdung; Kindeswohlgefährdung melden;
  Kindesmisshandlung melden; Kindeswohl melden; Kinderschutz; Kinderschutz melden; Kind Gewalt Hilfe; Kind vernachlässigt melden;
  Unterhaltsvorschuss; Unterhaltsvorschuss beantragen; Unterhaltsvorschuss verlängern; Unterhaltsvorschussstelle;
  Beistandschaft; Beistandschaft Vaterschaft; Beistandschaft Unterhalt; Beistand Unterhalt; Unterhaltsberechnung Kind;
  Kindesunterhalt berechnen; Kindesunterhalt durchsetzen; Kindesunterhalt Titel; Unterhaltstitel Jugendamt; Urkunde Unterhalt Jugendamt;
  Sorgeerklärung abgeben; Sorgeerklärung Jugendamt; Sorgerechtserklärung; gemeinsame Sorge; gemeinsame Sorge unverheiratet;
  Vaterschaftsanerkennung Jugendamt; Vaterschaft Jugendamt; Vaterschaftsfeststellung; Vaterschaftstest Gericht Jugendamt;
  Umgangsberatung; Umgangsbegleitung; begleiteter Umgang; Trennungsberatung; Scheidungsberatung Kinder; Trennung Kinder Beratung;
  Beratung Trennung Familie; Mediation Familie; Kita Platz Jugendamt; Kita Platz Anspruch; Kita Anspruch Klage;
  Kindertagespflege; Tagesmutter; Tagesmutter finden; Tagesmutter Förderung; Tagespflegeperson werden; Kita Elternbeitrag;
  Kita Beitrag Erlass; Elternbeitrag erlassen; Kita Gebühren Übernahme; Kita Kosten Übernahme; Kita Beitrag Ermäßigung;
  Beitragsfreie Kita; Hort Platz; Hortplatz; Ganztagsbetreuung Anspruch; Schulkindbetreuung; Ferienbetreuung Kosten; Jugendzentrum;
  Jugendarbeit; Jugendförderung; Jugendleiter Card; Juleica beantragen; Juleica; Jugendschutz; Jugendschutz Veranstaltung;
  Jugendgerichtshilfe; Jugendgerichtshilfe Termin; Straffällige Jugendliche; Jugendamt Straftat; Jugendliche Hilfe;
  Jugendlicher Wohnung; Jugendlicher Ausbildung Hilfe; Verselbständigung Jugendliche; Hilfe junge Volljährige;
  Hilfe für junge Volljährige; Care Leaver; Vormundschaft Jugendamt; Amtsvormund; Amtspflegschaft; Amtsvormundschaft;
  Eltern Kind Beratung; Elternberatung; Elternkurs; Familienzentrum; Familienbildung; Frühe Hilfen; Familienpate;
  Willkommensbesuch Baby; Babybesuchsdienst; Babybegrüßung; Familienförderung; Erholung Familien; Familienerholung;
  Mutter Kind Kur Antrag; Eingliederungshilfe Kind; Eingliederungshilfe seelisch behinderte Kinder; Schulbegleiter Antrag;
  Schulbegleitung; Integrationshelfer Schule; Integrationshelfer beantragen; Teilhabe Kinder; Kinder mit Behinderung Hilfe;
  Frühförderung; Frühförderung Antrag; Frühförderung Kind; Betreuungsgeld; Kindergartenplatz Anspruch; Kita Notbetreuung;
  Schulwegbegleitung; Hilfeplan; Hilfeplangespräch; Jugendhilfeplanung; Kinder- und Jugendhilfe; Kinder Jugendhilfe Antrag

sozialamt:
  Sozialamt; Sozialamt Termin; Sozialamt Kontakt; Sozialhilfe; Sozialhilfe beantragen; Sozialhilfe Antrag; Sozialhilfe Höhe;
  Sozialhilfe Voraussetzungen; Hilfe zum Lebensunterhalt; Hilfe zum Lebensunterhalt beantragen; Grundsicherung im Alter;
  Grundsicherung im Alter beantragen; Grundsicherung Rentner; Grundsicherung Rente aufstocken; Rente aufstocken;
  Grundsicherung bei Erwerbsminderung; Grundsicherung Erwerbsminderung beantragen; Grundsicherung Alter Antrag;
  Altersarmut Hilfe; Rente reicht nicht; Aufstockung Rente Sozialamt; Hilfe zur Pflege; Hilfe zur Pflege beantragen;
  Pflegeheim Kosten Sozialamt; Pflegeheimkosten Übernahme; Pflegeheim Sozialhilfe; Heimkosten Übernahme; Kostenübernahme Pflegeheim;
  Elternunterhalt; Elternunterhalt Sozialamt; Elternunterhalt Rückforderung; Unterhaltsheranziehung; Angehörigenentlastungsgesetz;
  Eingliederungshilfe; Eingliederungshilfe beantragen; Eingliederungshilfe Behinderung; Eingliederungshilfe Antrag;
  Teilhabe Behinderte; Teilhabeleistungen; persönliches Budget; Persönliches Budget beantragen; Assistenzleistungen;
  Wohnheim Behinderte Kosten; Werkstatt für behinderte Menschen; Werkstatt Behinderung Aufnahme; Behindertenhilfe; Blindengeld;
  Blindenhilfe; Blindengeld beantragen; Gehörlosengeld; Landesblindengeld; Sozialhilfe Bestattung; Bestattungskosten Sozialamt;
  Bestattungskosten Übernahme; Bestattungskosten nicht bezahlen; Sozialbestattung; Beerdigung Kosten Hilfe; Beerdigung bezahlen Sozialamt;
  Asylbewerberleistungen; Asylbewerberleistungen beantragen; Leistungen Asylbewerber; Krankenhilfe Sozialamt; Krankenversicherung Sozialhilfe;
  Mehrbedarf Sozialhilfe; einmalige Beihilfe Sozialamt; Erstausstattung Wohnung Sozialamt; Erstausstattung Baby Sozialamt;
  Erstausstattung Schwangerschaft; Kleidung Hilfe Sozialamt; Mietübernahme Sozialamt; Mietschulden Übernahme; Mietschulden Sozialamt;
  Energieschulden Übernahme; Stromschulden Übernahme; Stromschulden Sozialamt; Strom abgestellt Hilfe; Wohnungslos Hilfe;
  Wohnungslosenhilfe; Obdachlos Beratung; Schuldnerberatung; Schuldnerberatung Stadt; Schuldnerberatung kostenlos; Sozialberatung;
  Sozialberatung Stadt; Sozialpass beantragen; Sozialticket; Sozialticket beantragen; Sozialticket Stadt; Tafel Berechtigung;
  Tafel Ausweis; Lebensmittel Tafel; Tafel Anmeldung; Bildung und Teilhabe; Bildungspaket; Bildungspaket beantragen;
  Bildungs- und Teilhabepaket; Schulbedarf Zuschuss; Schulbedarfspaket; Schulstarterpaket; Schulmittagessen Zuschuss;
  Mittagessen Kita Zuschuss; Lernförderung Zuschuss; Schulausflug Zuschuss; Klassenfahrt Zuschuss; Klassenfahrt Kosten Hilfe;
  Vereinsbeitrag Zuschuss; Musikschule Zuschuss; Sportverein Zuschuss Kinder; Teilhabe Kinder Verein

jobcenter:
  Jobcenter; Jobcenter Termin; Jobcenter Kontakt; Jobcenter Öffnungszeiten; Jobcenter zuständig; Jobcenter Adresse; Bürgergeld;
  Bürgergeld beantragen; Bürgergeld Antrag; Bürgergeld Höhe; Bürgergeld Voraussetzungen; Bürgergeld Weiterbewilligung;
  Bürgergeld Weiterbewilligungsantrag; Bürgergeld Bewilligung; Bürgergeld Ablehnung; Bürgergeld Widerspruch; Bürgergeld Sanktion;
  Bürgergeld Mehrbedarf; Bürgergeld Miete; Bürgergeld Umzug; Bürgergeld Umzug genehmigen; Bürgergeld Student; Bürgergeld Aufstocker;
  Bürgergeld Einkommen anrechnen; Bürgergeld Nebenjob; Bürgergeld Rückforderung; Bürgergeld Erstausstattung; Bürgergeld Kinder;
  Bürgergeld Vermögen; Bürgergeld Schonvermögen; Bürgergeld Bedarfsgemeinschaft; Bedarfsgemeinschaft; Bedarfsgemeinschaft Änderung;
  Arbeitslosengeld II; Arbeitslosengeld 2; ALG 2; ALG II; ALG II beantragen; Hartz IV; Hartz 4; Hartz IV beantragen; Hartz IV Antrag;
  Hartz IV Miete; Hartz IV Umzug; Grundsicherung für Arbeitsuchende; Grundsicherung Arbeitsuchende beantragen;
  Grundsicherung beantragen; Grundsicherung Antrag; Sozialgeld; Aufstockung Lohn; aufstockendes Bürgergeld; Mehrbedarf Alleinerziehende;
  Mehrbedarf Schwangerschaft; Mehrbedarf Warmwasser; Mehrbedarf Ernährung; Heizkosten Nachzahlung Jobcenter; Nebenkosten Nachzahlung Jobcenter;
  Mietkaution Jobcenter; Kaution Jobcenter Übernahme; Umzugskosten Jobcenter; Wohnungsbeschaffungskosten Jobcenter;
  Erstausstattung Wohnung Jobcenter; Erstausstattung Baby Jobcenter; Erstausstattung Kleidung Jobcenter; Waschmaschine Jobcenter;
  Kühlschrank defekt Jobcenter; Einmalige Leistung Jobcenter; Darlehen Jobcenter; Jobcenter Darlehen beantragen; Jobcenter Vorschuss;
  Vorschuss Bürgergeld; Jobcenter Bescheid; Jobcenter Bescheid Widerspruch; Jobcenter Einladung; Jobcenter Einladung Termin verschieben;
  Meldeaufforderung Jobcenter; Eingliederungsvereinbarung; Kooperationsplan Jobcenter; Weiterbildung Jobcenter; Bildungsgutschein Jobcenter;
  Umschulung Jobcenter; Maßnahme Jobcenter; Ein-Euro-Job; Arbeitsgelegenheit; Bewerbungskosten Jobcenter; Bewerbungskosten erstatten Jobcenter;
  Fahrtkosten Bewerbung Jobcenter; Eingliederungszuschuss Jobcenter; Eingliederungszuschuss; Einstiegsgeld; Selbstständigkeit Jobcenter;
  Gründungszuschuss Jobcenter; Jobcenter Selbstständig; Jobcenter Ausbildung; Jobcenter Studium; Jobcenter Elternzeit; Jobcenter Krankheit;
  Jobcenter Krankmeldung; Jobcenter Umzug Ausland; Jobcenter Ortsabwesenheit; Ortsabwesenheit Jobcenter; Urlaub Jobcenter; Urlaubsantrag Jobcenter;
  Kinderzuschlag Jobcenter; Wohngeld oder Bürgergeld; Jobcenter Kindergeld; Jobcenter Einkommensnachweis; Jobcenter Kontoauszüge;
  Jobcenter Unterlagen; Jobcenter Antrag online; Jobcenter online Antrag; jobcenter.digital; Jobcenter Hotline; Jobcenter Beschwerde;
  Jobcenter Mitarbeiter Beschwerde; Jobcenter Fallmanager; Jobcenter Sachbearbeiter; Jobcenter Leistungsabteilung; Teilhabechancengesetz;
  Teilhabechancen Arbeit; Eingliederung Langzeitarbeitslose; Langzeitarbeitslos; Langzeitarbeitslose Förderung; Sanktion Jobcenter

arbeitsagentur:
  Arbeitsagentur; Agentur für Arbeit; Arbeitsamt; Arbeitsagentur Termin; Arbeitsagentur Kontakt; Arbeitsagentur Öffnungszeiten;
  Arbeitsagentur zuständig; arbeitslos melden; Arbeitslosmeldung; arbeitslos werden; arbeitslos gemeldet; Arbeitslosigkeit;
  Arbeitslosigkeit melden; Arbeitslosengeld; Arbeitslosengeld beantragen; Arbeitslosengeld Antrag; Arbeitslosengeld online;
  Arbeitslosengeld I; Arbeitslosengeld 1; ALG 1; ALG I; ALG 1 beantragen; Arbeitslosengeld Höhe; Arbeitslosengeld berechnen;
  Arbeitslosengeld Dauer; Arbeitslosengeld Voraussetzungen; Arbeitslosengeld Anspruch; Arbeitslosengeld Kündigung; Arbeitslosengeld Sperrzeit;
  Sperrzeit Arbeitsagentur; Sperrzeit Eigenkündigung; Arbeitslosengeld Eigenkündigung; Arbeitslosengeld Aufhebungsvertrag;
  Arbeitslosengeld Nebenjob; Arbeitslosengeld Nebeneinkommen; Arbeitslosengeld Krankheit; Arbeitslosengeld Elternzeit;
  Arbeitslosengeld Selbstständige; Arbeitslosengeld Selbstständig Versicherung; freiwillige Arbeitslosenversicherung;
  Arbeitslosengeld Ausland; Arbeitslosengeld Auslandsaufenthalt; Arbeitslosengeld EU; Arbeitslosengeld Urlaub; Arbeitslosengeld Ortsabwesenheit;
  Arbeitslosengeld Rente; Arbeitslosengeld Student; Arbeitslosengeld nach Ausbildung; Arbeitslosengeld nach Studium; Arbeitslosengeld Ablehnung;
  Arbeitslosengeld Widerspruch; Arbeitslosengeld Rückforderung; Arbeitslosengeld Bescheid; Arbeitslosengeld Auszahlung; Arbeitslosengeld verspätet;
  Arbeitslosengeld Vorschuss; Arbeitslosengeld Bescheinigung; Arbeitsbescheinigung; Arbeitsbescheinigung Arbeitgeber; Arbeitsbescheinigung beantragen;
  Arbeitsuchend melden; arbeitssuchend melden; arbeitsuchend; Arbeitssuchendmeldung; Arbeitsuchendmeldung; Kündigung melden Arbeitsagentur;
  Kündigung bekommen melden; Arbeit verloren; Job verloren; Job gekündigt; Arbeitsplatz verloren; Arbeitsplatzverlust; Entlassung;
  Entlassung Arbeitsagentur; Insolvenz Arbeitgeber; Insolvenzgeld; Insolvenzgeld beantragen; Insolvenzgeld Antrag; Kurzarbeitergeld;
  Kurzarbeitergeld beantragen; Kurzarbeit anzeigen; Kurzarbeit Anzeige; Kurzarbeit Abrechnung; Saison-Kurzarbeitergeld; Transferkurzarbeitergeld;
  Transfergesellschaft; Berufsberatung; Berufsberatung Termin; Berufsberatung Schüler; Berufsberatung Erwachsene; Berufsorientierung;
  Berufswahl Beratung; Ausbildungsplatz Beratung; Ausbildungsplatz finden; Ausbildungsvermittlung; Ausbildungsstelle suchen; Lehrstelle suchen;
  Berufsausbildungsbeihilfe; Berufsausbildungsbeihilfe beantragen; BAB; BAB Antrag; Ausbildungsgeld; Ausbildungsgeld beantragen;
  Arbeitsvermittlung; Stellensuche; Jobsuche Arbeitsagentur; Jobbörse; Jobbörse Arbeitsagentur; Stellenangebote Arbeitsagentur;
  Vermittlungsgutschein; Aktivierungs- und Vermittlungsgutschein; Bildungsgutschein; Bildungsgutschein beantragen; Bildungsgutschein Weiterbildung;
  Weiterbildung Arbeitsagentur; Weiterbildung gefördert; Weiterbildung Förderung; Weiterbildungsförderung; Umschulung; Umschulung beantragen;
  Umschulung Arbeitsagentur; Umschulung Förderung; Qualifizierungschancengesetz; Qualifizierungsgeld; Existenzgründung Zuschuss;
  Gründungszuschuss; Gründungszuschuss beantragen; Förderung Selbstständigkeit Arbeitsagentur; Eingliederungszuschuss Arbeitgeber;
  Arbeitgeberservice; Arbeitgeber Förderung; Ausbildung Förderung Arbeitgeber; Einstellungszuschuss; Mobilitätshilfen; Mobilitätshilfe;
  Umzugskostenbeihilfe Arbeit; Trennungskostenbeihilfe; Bewerbungskostenerstattung; Bewerbungskosten Arbeitsagentur; Reisekosten Vorstellungsgespräch;
  Vorstellungsgespräch Kosten; Arbeitsagentur Gutschein; Arbeitsmarktzulassung; Arbeitserlaubnis Arbeitsagentur; Zustimmung Bundesagentur;
  Anerkennung Berufsabschluss Beratung; Rehabilitation Arbeit; berufliche Rehabilitation; Reha Beruf; Teilhabe am Arbeitsleben;
  schwerbehinderte Arbeit Förderung; Leistungen zur Teilhabe am Arbeitsleben; Berufsinformationszentrum; BiZ; BiZ Termin; Arbeitsagentur Online;
  eServices Arbeitsagentur; Arbeitsagentur Konto; Arbeitsagentur Hotline; Arbeitsagentur Beschwerde; Arbeitsagentur Online Antrag;
  Arbeitslosenversicherung; Arbeitslosenversicherung freiwillig; Versicherungspflichtverhältnis; Anwartschaftszeit; Rahmenfrist

familienkasse:
  Familienkasse; Familienkasse Termin; Familienkasse Kontakt; Familienkasse Antrag; Familienkasse Hotline; Kindergeld; Kindergeld beantragen;
  Kindergeld Antrag; Kindergeld online; Kindergeld Höhe; Kindergeld Auszahlung; Kindergeld Auszahlungstermine; Kindergeld Nachzahlung;
  Kindergeld Rückforderung; Kindergeld Bescheid; Kindergeld Widerspruch; Kindergeld Ablehnung; Kindergeld Ausbildung; Kindergeld Studium;
  Kindergeld Volljährig; Kindergeld Volljähriges Kind; Kindergeld nach 18; Kindergeld über 18; Kindergeld Ausland; Kindergeld EU;
  Kindergeld Umzug; Kindergeld Kontoänderung; Kindergeld Bankverbindung ändern; Kindergeld Name ändern; Kindergeld Pflegekind;
  Kindergeld Enkelkind; Kindergeld Alleinerziehend; Kindergeld Trennung; Kindergeld Scheidung; Kindergeld Geburt; Kindergeld Baby;
  Kindergeld Zwillinge; Kindergeld Behinderung; Kindergeld Arbeitslos Kind; Kindergeld FSJ; Kindergeld Freiwilligendienst; Kindergeld Wehrdienst;
  Kindergeld Praktikum; Kindergeld Wartezeit Studium; Kindergeld Zweitausbildung; Kindergeld Einkommen Kind; Kindergeldantrag; Kindergeldnummer;
  Kindergeld Steuer-ID; Kindergeld Abzweigung; Kindergeld Abzweigung Sozialamt; Kindergeld Auszahlung Kind; Kindergeldkasse;
  Kinderzuschlag; Kinderzuschlag beantragen; Kinderzuschlag Antrag; Kinderzuschlag Höhe; Kinderzuschlag online; Kinderzuschlag Voraussetzungen;
  Kinderzuschlag Rechner; Kinderzuschlag Bescheid; Kinderzuschlag Ablehnung; Kinderzuschlag Weiterbewilligung; Kinderzuschlag Wohngeld;
  Kindergrundsicherung; Kinderbonus; Familienleistungen; Familienleistungen Antrag; Kinder Geldleistungen; Kindergeld für Kinder

wohngeldstelle:
  Wohngeld; Wohngeld beantragen; Wohngeld Antrag; Wohngeld Antrag online; Wohngeld Antrag Formular; Wohngeld Höhe; Wohngeld berechnen;
  Wohngeldrechner; Wohngeld Anspruch; Wohngeld Voraussetzungen; Wohngeld Einkommensgrenze; Wohngeld Weiterbewilligung;
  Wohngeld Verlängerung; Wohngeld Folgeantrag; Wohngeld Erhöhung; Wohngeld Bescheid; Wohngeld Ablehnung; Wohngeld Widerspruch;
  Wohngeld Rückforderung; Wohngeld Nachzahlung; Wohngeld Auszahlung; Wohngeld Rentner; Wohngeld Student; Wohngeld Azubi; Wohngeld Auszubildende;
  Wohngeld Alleinerziehend; Wohngeld Arbeitslos; Wohngeld Umzug; Wohngeld neue Wohnung; Wohngeld Miete; Wohngeld Mietbescheinigung;
  Mietbescheinigung Wohngeld; Wohngeld Heizkosten; Heizkostenzuschuss; Heizkostenzuschuss Wohngeld; Heizkostenzuschuss beantragen;
  Wohngeld Hauseigentümer; Lastenzuschuss; Lastenzuschuss Eigentum; Lastenzuschuss beantragen; Wohngeld Eigentumswohnung; Wohngeld Eigenheim;
  Wohngeldstelle; Wohngeldstelle Termin; Wohngeldstelle Kontakt; Wohngeldbehörde; Wohngeldamt; Wohngeld Stadt; Wohngeld Landkreis;
  Wohngeld zuständig; Wohngeld Plus; Wohngeld Plus beantragen; Mietzuschuss; Mietzuschuss beantragen; Mietzuschuss Staat; Zuschuss Miete;
  Zuschuss zur Miete; Miete bezahlen Hilfe Staat; Staatliche Hilfe Miete; Miete zu hoch Hilfe
"""

FAELLE_BAUEN_VERKEHR = """
bauamt:
  Bauantrag; Bauantrag stellen; Bauantrag einreichen; Bauantrag Einfamilienhaus; Bauantrag Kosten; Bauantrag Dauer; Bauantrag online;
  Baugenehmigung; Baugenehmigung beantragen; Baugenehmigung Kosten; Baugenehmigung Dauer; Baugenehmigung Anbau; Baugenehmigung Garage;
  Baugenehmigung Carport; Baugenehmigung Gartenhaus; Baugenehmigung Terrasse; Baugenehmigung Wintergarten; Baugenehmigung Balkon;
  Baugenehmigung Dachausbau; Baugenehmigung Dachgeschoss; Baugenehmigung Gaube; Baugenehmigung Dachfenster; Baugenehmigung Solaranlage;
  Baugenehmigung Photovoltaik; Baugenehmigung Wärmepumpe; Baugenehmigung Zaun; Baugenehmigung Mauer; Baugenehmigung Pool;
  Baugenehmigung Schwimmbad; Baugenehmigung Stellplatz; Baugenehmigung Nutzungsänderung; Nutzungsänderung; Nutzungsänderung beantragen;
  Nutzungsänderung Wohnung Gewerbe; Zweckentfremdung Wohnung; Zweckentfremdung Genehmigung; Ferienwohnung Genehmigung;
  Baugenehmigung Abriss; Abrissgenehmigung; Abbruchgenehmigung; Abbruchanzeige; Abriss Haus Genehmigung; Bauvoranfrage; Bauvoranfrage stellen;
  Vorbescheid Bau; Bauvorbescheid; Bauvorbescheid Kosten; Baulast; Baulastenverzeichnis; Baulastenauskunft; Baulast eintragen;
  Baulast löschen; Bauaufsicht; Bauaufsichtsbehörde; Bauaufsichtsamt; Bauordnungsamt; Bauamt; Bauamt Termin; Bauamt Kontakt;
  Bauamt Öffnungszeiten; Bauamt zuständig; Bauamt Beratung; Bauberatung; Baubeginn anzeigen; Baubeginnanzeige; Bauanzeige; Baufertigstellung anzeigen;
  Fertigstellungsanzeige; Schlussabnahme Bau; Rohbauabnahme; Bauabnahme; Bauleiter anzeigen; Bauleiter Benennung; Bauherr; Bauherrenwechsel;
  Bauüberwachung; Baukontrolle; Baustopp; Baueinstellung; Schwarzbau; Schwarzbau melden; Schwarzbau Nachbar; Schwarzbau legalisieren;
  Nachbar baut ohne Genehmigung; Nachbar Bau Beschwerde; Nachbarschaftsbeteiligung Bau; Nachbarunterschrift Bauantrag; Abstandsflächen;
  Abstandsfläche Grenzabstand; Grenzabstand Bau; Grenzbebauung; Bebauungsplan; Bebauungsplan einsehen; Bebauungsplan Auskunft;
  Bebauung Grundstück; Bauland; Baugrundstück; Baugrundstück Auskunft; Bauplanungsrecht; Baurecht Auskunft; Baurecht Beratung;
  Baulandkataster; Bauleitplanung; Genehmigungsfreistellung; Freistellungsverfahren Bau; Bauvorlageberechtigung; Bauvorlagen; Bauunterlagen;
  Lageplan; Lageplan beantragen; Lageplan Bauantrag; amtlicher Lageplan; Lageplan Kosten; Standsicherheitsnachweis; Brandschutznachweis;
  Brandschutzkonzept; Brandschutz Bau Genehmigung; Stellplatznachweis; Stellplatzablöse; Ablösebetrag Stellplatz; Denkmalschutz;
  Denkmalschutz Genehmigung; Denkmalschutz Umbau; Denkmalschutz Fenster; Denkmalschutz Dach; Denkmalschutz Fassade; denkmalgeschütztes Haus;
  denkmalgeschützt Umbau; Denkmalbehörde; Denkmalpflege; Denkmal Steuer; Sanierung Genehmigung; Sanierung Altbau Genehmigung; Fassadensanierung Genehmigung;
  Fassade streichen Genehmigung; Werbeanlage Bau; Werbeanlage Genehmigung Bauamt; Schild anbringen Genehmigung; Mobilfunkmast Genehmigung;
  Windrad Genehmigung; Windkraftanlage Genehmigung; Biogasanlage Genehmigung; Silo Genehmigung; Stallbau Genehmigung; Scheune Umnutzung;
  Aussiedlerhof Bauen; Landwirtschaft Bauen Genehmigung; Bauen im Außenbereich; Außenbereich Bauen; Innenbereich Bauen; Baulücke;
  Baulücke schließen; Grundstück teilen; Grundstücksteilung; Grundstücksteilung Genehmigung; Teilungsgenehmigung; Grundstück bebaubar;
  Grundstück bebaubar prüfen; Erschließung Grundstück; Erschließung Straße Bauland; Hausanschluss Bau; Hausnummer beantragen; Hausnummer Neubau;
  Hausnummer vergeben; Straßenname Neubau; Gebäudeeinmessung; Gebäudeaufmaß; Wohnflächenberechnung Bauamt; Wohnraumförderung;
  Wohnraumförderung Antrag; Wohnraumförderung Bau; Wohnberechtigungsschein; Wohnberechtigungsschein beantragen; WBS; WBS beantragen;
  WBS Antrag; Sozialwohnung; Sozialwohnung beantragen; Sozialwohnung Antrag; Sozialwohnung Wartezeit; Wohnungsamt; Wohnungsamt Termin;
  Wohnungsvermittlung Stadt; öffentlich geförderte Wohnung; geförderte Wohnung; Mietpreisbindung; Belegungsrecht; Fehlbelegungsabgabe;
  Wohnungsaufsicht; Wohnungsaufsicht Mängel; Wohnungsmängel melden; Schimmel Wohnung Stadt; Schimmel Vermieter Amt; Wohnraumpflege;
  Wohnungsmängel Stadt; Unbewohnbare Wohnung; Wohnung unbewohnbar; Vermieter Verstoß melden; Mietwucher melden; Mietpreisbremse Stadt

strassenverkehrsbehoerde:
  Führerschein; Führerschein beantragen; Führerschein machen; Führerschein Antrag; Führerschein Klasse B; Führerschein Klasse A;
  Führerschein Klasse C; Führerschein Klasse CE; Führerschein Motorrad; Führerschein Moped; Führerschein AM; Führerschein Roller;
  Führerschein Anhänger; Führerschein B96; Führerschein BE; Führerschein Traktor; Führerschein Bus; Führerschein LKW; Führerschein Taxi;
  Führerschein Fahrgastbeförderung; Personenbeförderungsschein; P-Schein; P-Schein beantragen; Führerschein mit 17; begleitetes Fahren;
  begleitetes Fahren 17; Führerschein Fahranfänger; Führerschein Probezeit; Probezeit Verlängerung; Probezeit Verstoß; Aufbauseminar;
  Fahrschule Anmeldung; Fahrschule Theorie Anmeldung; Theorieprüfung Anmeldung; praktische Prüfung Anmeldung; Führerschein Sehtest;
  Sehtest Führerschein; Erste-Hilfe-Kurs Führerschein; Lichtbild Führerschein; Führerschein verloren; Führerschein Verlust; Führerschein gestohlen;
  Führerschein Ersatz; Ersatzführerschein; Führerschein Duplikat; Führerschein neu beantragen; Führerschein Namensänderung;
  Führerschein Adresse ändern; Führerschein umtauschen; Führerschein umtauschen Pflicht; Führerschein Umtausch Frist; Führerschein Pflichtumtausch;
  Führerschein EU-Kartenführerschein; Kartenführerschein; grauer Führerschein; rosa Führerschein; Papierführerschein umtauschen;
  Führerschein DDR umtauschen; Führerschein Ausland umschreiben; Führerschein Ausland anerkennen; Führerschein umschreiben;
  Führerschein umschreiben Drittstaat; Führerschein Türkei umschreiben; Führerschein Ukraine umschreiben; Führerschein Syrien umschreiben;
  Führerschein Indien umschreiben; Führerschein USA umschreiben; Führerschein Schweiz umschreiben; Führerschein Großbritannien umschreiben;
  Führerschein EU Austausch; internationaler Führerschein; internationaler Führerschein beantragen; Führerschein international;
  Führerscheinstelle; Führerscheinstelle Termin; Führerscheinstelle Kontakt; Führerscheinstelle Öffnungszeiten; Führerscheinstelle zuständig;
  Fahrerlaubnisbehörde; Fahrerlaubnis; Fahrerlaubnis beantragen; Fahrerlaubnis erweitern; Fahrerlaubnis Klasse erweitern;
  Fahrerlaubnis neu erteilen; Fahrerlaubnis Wiedererteilung; Fahrerlaubnis entzogen; Fahrerlaubnis Entzug; Führerschein entzogen;
  Führerscheinentzug; Führerschein Entzug Dauer; Führerschein Entzug Alkohol; Führerschein zurückbekommen; Führerschein wiederbekommen;
  Führerschein Wiedererteilung; Führerschein Neuerteilung; Führerschein Sperrfrist; Sperrfrist Führerschein; Führerschein Fahrverbot;
  Fahrverbot; Fahrverbot Führerschein abgeben; Fahrverbot Frist; Fahrverbot Beginn; Führerschein abgeben; Führerschein abgeben Fahrverbot;
  Führerschein Punkte; Punkte Flensburg; Punkte in Flensburg abfragen; Punkteauskunft; Punkte Flensburg abbauen; Punkte abbauen;
  Fahreignungsseminar; Fahreignungsregister; Fahreignungsregister Auskunft; Fahreignungsregister Auszug; Auskunft Fahreignungsregister;
  Kraftfahrt-Bundesamt Auskunft; MPU; MPU Anordnung; MPU Vorbereitung; MPU Alkohol; MPU Drogen; MPU Punkte; Medizinisch-Psychologische Untersuchung;
  Gutachten Fahreignung; Fahreignung Gutachten; ärztliches Gutachten Führerschein; Fahreignungsprüfung; Eignungsgutachten;
  Fahrerlaubnis Gesundheit; Führerschein Alter Prüfung; Führerschein Senioren; Führerschein Sehschwäche; Führerschein Epilepsie;
  Führerschein Diabetes; Führerschein Behinderung; Führerschein Auflagen; Führerschein Schlüsselzahl; Führerschein Schlüsselzahl 01;
  Führerschein Beschränkung; Fahrlehrer Erlaubnis; Fahrschule gründen; Fahrschule Erlaubnis; Fahrlehrerlaubnis; Fahrlehrerschein;
  Parkerleichterung; Parkerleichterung Schwerbehinderte; Parkerleichterung beantragen; Behindertenparkplatz; Behindertenparkplatz beantragen;
  Behindertenparkplatz vor Haus; Behindertenparkausweis; Behindertenparkausweis beantragen; Parkausweis Schwerbehinderte;
  Parkausweis Behinderung; blauer Parkausweis; EU Parkausweis Behinderung; Bewohnerparkausweis beantragen; Bewohnerparkausweis Kosten;
  Bewohnerparken beantragen; Bewohnerparken Ausweis; Bewohnerparken Zone; Anwohnerparkausweis; Anwohnerparkausweis beantragen;
  Handwerkerparkausweis; Handwerker Parkausweis; Parkausweis Pflegedienst; Parkausweis Arzt; Ausnahmegenehmigung Parken;
  Ausnahmegenehmigung Fahrverbot; Ausnahmegenehmigung Umweltzone; Umweltzone Ausnahme; Feinstaubplakette; Umweltplakette; Umweltplakette beantragen;
  Dieselfahrverbot Ausnahme; Ausnahmegenehmigung Straßenverkehr; Ausnahmegenehmigung Sonntagsfahrverbot; Ausnahmegenehmigung Gewicht;
  Ausnahmegenehmigung Schwertransport; Schwertransport; Schwertransport Genehmigung; Großraumtransport; Großraumtransport Genehmigung;
  Großraum- und Schwertransport; Transportgenehmigung; Dauergenehmigung Schwertransport; Erlaubnis Überbreite; Erlaubnis Überlänge;
  Verkehrsrechtliche Anordnung; Verkehrsrechtliche Anordnung beantragen; verkehrsrechtliche Erlaubnis; Straßensperrung Genehmigung;
  Straßensperrung Baustelle; Straßensperrung Umzug; Straßensperrung Fest; Straßenverkehrsamt; Straßenverkehrsbehörde; Verkehrsbehörde;
  Verkehrsschild Beschwerde; Verkehrsschild beantragen; Verkehrszeichen aufstellen; Verkehrszeichen Antrag; Tempo 30 Antrag; Tempo 30 beantragen;
  Tempolimit Antrag; Zebrastreifen beantragen; Fußgängerüberweg beantragen; Ampel beantragen; Verkehrsberuhigung beantragen;
  Verkehrsberuhigung Antrag; Spielstraße beantragen; Radweg beantragen; Fahrradstraße; Parkplatz reservieren; Parkplatz Reservierung Umzug;
  Schulwegsicherung; Schulweg Verkehr Sicherheit; Schulweg gefährlich; gefährliche Kreuzung melden; Verkehrsunfallkommission;
  Radverkehr Beschwerde; Fahrradparken beantragen; Taxikonzession; Taxi Konzession; Taxi Genehmigung; Taxischein; Taxi anmelden;
  Mietwagenkonzession; Mietwagen Genehmigung; Mietwagen Konzession; Personenbeförderung Genehmigung;
  Busverkehr Genehmigung; Linienverkehr Genehmigung; Güterverkehr Erlaubnis; Güterkraftverkehr Erlaubnis; Güterkraftverkehr Lizenz;
  Gemeinschaftslizenz; Fahrerkarte; Fahrerkarte beantragen; Fahrerqualifizierungsnachweis; Berufskraftfahrer Qualifikation;
  Berufskraftfahrerqualifikation; Schlüsselzahl 95; Code 95; Fahrerkarte Tachograph; Kontrollgerätekarte

zulassungsstelle:
  Kfz Zulassung; Kfz-Zulassung; KFZ Zulassung beantragen; Zulassung; Fahrzeugzulassung; Fahrzeugzulassung beantragen; Fahrzeug zulassen;
  Auto zulassen; Auto anmelden; Auto anmelden Termin; Auto anmelden Kosten; Auto anmelden online; Auto anmelden Neuwagen; Auto anmelden Gebrauchtwagen;
  Auto anmelden Wohnort; Auto ummelden; Auto ummelden Wohnort; Auto ummelden Umzug; Auto abmelden; Auto abmelden online; Auto abmelden Kosten;
  Auto abmelden Verkauf; Auto Abmeldung; Auto Zulassung; Auto Zulassung Kosten; Auto Zulassung online; Auto Zulassung Termin; Auto zugelassen;
  Auto zulassen Neuwagen; Auto zulassen Gebrauchtwagen; Auto zulassen Kosten; Auto zulassen Ausland; Auto zulassen Import; Auto zulassen Leasing;
  Auto zulassen eVB; Fahrzeug anmelden; Fahrzeug anmelden Kosten; Fahrzeug anmelden online; Fahrzeug anmelden Termin; Fahrzeug anmelden Ausland;
  Fahrzeug ummelden; Fahrzeug ummelden Umzug; Fahrzeug ummelden Besitzerwechsel; Fahrzeug abmelden; Fahrzeug abmelden online; Fahrzeug abmelden Verkauf;
  Fahrzeug abmelden Kosten; Fahrzeug abmelden Tod; Fahrzeug stilllegen; Fahrzeug Stilllegung; Fahrzeug außer Betrieb setzen; Außerbetriebsetzung;
  Außerbetriebsetzung Fahrzeug; Fahrzeug wiederzulassen; Wiederzulassung; Wiederzulassung Fahrzeug; Fahrzeug wieder anmelden; Fahrzeug Halterwechsel;
  Halterwechsel; Halterwechsel Fahrzeug; Halterwechsel Kosten; Halterwechsel online; Halterwechsel Erbe; Fahrzeug erben; Fahrzeug Erbe anmelden;
  Fahrzeug Verkauf abmelden; Auto verkauft abmelden; Auto verkauft; Auto geerbt; Auto geerbt anmelden; Kennzeichen; Kennzeichen beantragen;
  Kennzeichen reservieren; Kennzeichen reservieren online; Wunschkennzeichen; Wunschkennzeichen reservieren; Wunschkennzeichen Kosten;
  Wunschkennzeichen online; Kennzeichen verloren; Kennzeichen Verlust; Kennzeichen gestohlen Zulassungsstelle; Kennzeichen Ersatz;
  Kennzeichen prägen; Kennzeichen Schilder; Schilderprägung; Schilderpräger; Kennzeichen mitnehmen; Kennzeichenmitnahme; Kennzeichen Umzug;
  Kennzeichen behalten Umzug; Kennzeichen wechseln; Kennzeichen Umzug neues Kennzeichen; Saisonkennzeichen; Saisonkennzeichen beantragen;
  Saisonkennzeichen Kosten; Saisonkennzeichen ändern; Saisonkennzeichen Zeitraum; Oldtimerkennzeichen; H-Kennzeichen; H-Kennzeichen beantragen;
  H Kennzeichen Oldtimer; Oldtimer anmelden; Oldtimer zulassen; Oldtimer Zulassung; Oldtimer Gutachten; Oldtimergutachten; Rotes Kennzeichen;
  rotes Kennzeichen beantragen; 07er Kennzeichen; Kurzzeitkennzeichen; Kurzzeitkennzeichen beantragen; Kurzzeitkennzeichen Kosten;
  Kurzzeitkennzeichen Überführung; Ausfuhrkennzeichen; Ausfuhrkennzeichen beantragen; Zollkennzeichen; Zulassung Ausfuhrkennzeichen; E-Kennzeichen;
  E-Kennzeichen Elektroauto; Elektroauto zulassen; Elektroauto anmelden; Elektroauto Zulassung; Elektro Kennzeichen; Wechselkennzeichen;
  Wechselkennzeichen beantragen; Wechselkennzeichen Kosten; Wechselkennzeichen Motorrad; Motorrad anmelden; Motorrad zulassen; Motorrad ummelden;
  Motorrad abmelden; Motorrad Zulassung; Motorrad Saisonkennzeichen; Roller anmelden; Roller zulassen; Roller Versicherungskennzeichen;
  Versicherungskennzeichen; Mofa anmelden; Moped anmelden; Moped Versicherungskennzeichen; E-Scooter Zulassung; E-Scooter Versicherung;
  E-Bike zulassen; S-Pedelec anmelden; S-Pedelec Zulassung; Quad anmelden; Quad zulassen; Trike zulassen; Anhänger anmelden; Anhänger zulassen;
  Anhänger Zulassung; Anhänger abmelden; Anhänger ummelden; Anhänger Kennzeichen; Wohnwagen anmelden; Wohnwagen zulassen; Wohnwagen abmelden;
  Wohnmobil anmelden; Wohnmobil zulassen; Wohnmobil abmelden; Wohnmobil ummelden; Wohnmobil Saisonkennzeichen; Camper zulassen; Camper anmelden;
  Campingbus zulassen; Campingbus Umbau; Umbau Wohnmobil Zulassung; Traktor zulassen; Traktor anmelden; Landwirtschaftliche Fahrzeuge Zulassung;
  Lkw anmelden; Lkw zulassen; Lkw abmelden; Lkw Zulassung; Transporter anmelden; Transporter zulassen; Sprinter zulassen; Firmenwagen anmelden;
  Firmenwagen Zulassung; Firmenwagen ummelden; Leasingfahrzeug zulassen; Leasingfahrzeug anmelden; Leasing Fahrzeug Zulassung;
  Zulassungsbescheinigung; Zulassungsbescheinigung Teil 1; Zulassungsbescheinigung Teil 2; Zulassungsbescheinigung verloren;
  Zulassungsbescheinigung Verlust; Zulassungsbescheinigung gestohlen; Zulassungsbescheinigung Ersatz; Zulassungsbescheinigung Duplikat;
  Zulassungsbescheinigung ändern; Zulassungsbescheinigung Adresse ändern; Zulassungsbescheinigung Namensänderung; Fahrzeugschein;
  Fahrzeugschein verloren; Fahrzeugschein Ersatz; Fahrzeugschein ändern; Fahrzeugbrief; Fahrzeugbrief verloren; Fahrzeugbrief Ersatz;
  Fahrzeugbrief Bank; Fahrzeugbrief Finanzierung; Kfz Brief; KFZ Schein; Fahrzeugpapiere; Fahrzeugpapiere verloren; Fahrzeugpapiere ändern;
  Fahrzeugpapiere Adresse ändern; Fahrzeugpapiere Umzug; Fahrzeugpapiere Besitzerwechsel; Fahrzeugpapiere Erbe; Fahrzeugpapiere Ausland;
  Fahrzeugpapiere Neuwagen; Fahrzeugpapiere Gebrauchtwagen; eVB Nummer; eVB-Nummer; Versicherungsbestätigung Zulassung; Doppelkarte;
  Doppelkarte Versicherung; Zulassung Versicherung; Zulassung Steuer; Zulassung Lastschrift; Zulassung SEPA; Zulassung Gebühren; Zulassung Kosten;
  Zulassung Termin; Zulassung Terminvereinbarung; Zulassungsstelle; Zulassungsstelle Termin; Zulassungsstelle Kontakt; Zulassungsstelle Öffnungszeiten;
  Zulassungsstelle zuständig; Zulassungsstelle online; Zulassungsstelle Adresse; Zulassungsbehörde; Kfz-Zulassungsstelle; Kfz Zulassungsstelle;
  Kfz-Zulassungsstelle Termin; Zulassungsstelle Abmeldung; Zulassungsstelle Ummeldung; Zulassungsstelle Anmeldung; i-Kfz; iKfz; internetbasierte Zulassung;
  Online Zulassung Auto; Online Abmeldung Auto; Online Anmeldung Auto; Fahrzeug Online Zulassung; Fahrzeug Online abmelden; Fahrzeug digital anmelden;
  Sicherheitscode Zulassung; Sicherheitscode Fahrzeugschein; Sicherheitscode freirubbeln; Sicherheitscode Fahrzeugbrief; Stilllegung Kfz;
  Stilllegung Auto; Auto stilllegen; Auto Stilllegung; Auto temporär abmelden; Auto vorübergehend abmelden; Auto vorübergehend stilllegen;
  Fahrzeug vorübergehend stilllegen; Fahrzeug Stilllegung Dauer; Fahrzeug Stilllegung Versicherung; Fahrzeug stilllegen Steuer;
  Fahrzeugverwertung Abmeldung; Auto verschrotten; Auto verschrotten abmelden; Verwertungsnachweis; Verwertungsnachweis Zulassung;
  Altfahrzeug abmelden; Altauto abmelden; Autowrack abmelden; Autowrack; Schrottauto abmelden; Schrottauto; Totalschaden abmelden;
  Unfallwagen abmelden; Unfallfahrzeug abmelden; Export Auto abmelden; Auto exportieren abmelden; Auto Ausland verkaufen abmelden;
  Auto Ausland anmelden; Auto aus Ausland zulassen; Auto aus dem Ausland anmelden; Auto Import Zulassung; Auto Import Deutschland;
  Auto Import Kosten; EU-Fahrzeug anmelden; EU Fahrzeug zulassen; EU-Neuwagen zulassen; Reimport Auto; Reimport Zulassung; Reimport Fahrzeug;
  Fahrzeug Reimport; Fahrzeug Einzelabnahme; Einzelabnahme; Einzelabnahme Fahrzeug; Einzelabnahme Zulassung; Einzelzulassung; Einzelgenehmigung Fahrzeug;
  Betriebserlaubnis; Betriebserlaubnis Fahrzeug; Betriebserlaubnis erlischt; Betriebserlaubnis eintragen; Betriebserlaubnis Umbau;
  Tuning Eintragung; Tuning Eintragung Zulassung; Tuning Teile eintragen; Umbau Fahrzeug eintragen; Fahrzeugumbau Eintragung;
  Fahrzeug Typgenehmigung; Fahrzeug Datenblatt; COC Papiere; COC Papiere beantragen; Übereinstimmungsbescheinigung; Übereinstimmungsbescheinigung COC
"""

FAELLE_GEWERBE_SOZIALVERSICHERUNG = """
gesundheitsamt:
  Gesundheitsamt; Gesundheitsamt Termin; Gesundheitsamt Kontakt; Gesundheitszeugnis; Gesundheitszeugnis Lebensmittel; Belehrung Infektionsschutzgesetz;
  Infektionsschutzbelehrung; Belehrung Lebensmittel; Rote Karte Gastronomie; Infektionsschutz; Infektionsschutz Meldung; meldepflichtige Krankheit;
  Corona Quarantäne Gesundheitsamt; Quarantäne Anordnung; Quarantäne Bescheinigung; Isolation Anordnung; Tuberkulose Untersuchung; Röntgen Tuberkulose;
  Impfberatung Gesundheitsamt; Impfung Gesundheitsamt; Reiseimpfung Gesundheitsamt; Impfpass Gesundheitsamt; Amtsarzt; Amtsarzt Termin;
  amtsärztliches Gutachten; amtsärztliches Attest; amtsärztliche Untersuchung; Amtsarzt Beamte; Amtsarzt Dienstfähigkeit; Amtsarzt Krankschreibung;
  Schuleingangsuntersuchung; Einschulungsuntersuchung; Schulärztliche Untersuchung; Schularzt; Jugendzahnpflege; Zahnuntersuchung Kita;
  Sozialpsychiatrischer Dienst; Sozialpsychiatrischer Dienst Hilfe; psychische Krise Hilfe Stadt; Suchtberatung; Suchtberatung Stadt;
  Drogenberatung; Drogenberatung Stadt; Aids Beratung; HIV Test anonym; HIV Test Gesundheitsamt; Beratung Geschlechtskrankheiten; STI Beratung;
  Schwangerschaftskonfliktberatung; Schwangerenberatung; Schwangerenberatung Stadt; Beratungsschein Schwangerschaft; Hygiene Beschwerde;
  Hygienekontrolle; Badewasserqualität; Trinkwasseranalyse; Trinkwasserqualität Beschwerde; Legionellen Meldung; Legionellen Untersuchung;
  Schädlingsbefall Gesundheitsamt; Bettwanzen melden; Läusebefall Schule; Kopfläuse Meldung; Krätze Meldung; Gesundheitsaufsicht;
  Heilpraktiker Erlaubnis; Heilpraktikerprüfung; Heilpraktiker Zulassung; Heilpraktikererlaubnis beantragen; Tattoo Studio Genehmigung;
  Kosmetikstudio Anmeldung Gesundheitsamt; Friseur Hygiene Gesundheitsamt; Pflegeheim Aufsicht Gesundheitsamt; Heimaufsicht;
  Leichenschau; Leichenpass; Leichenüberführung Genehmigung; Bestattung Genehmigung Gesundheitsamt; Totenschein Gesundheitsamt

veterinaeramt:
  Veterinäramt; Veterinäramt Kontakt; Veterinäramt Termin; Veterinärbehörde; Amtstierarzt; Amtstierarzt Kontakt; Tierschutz; Tierschutz melden;
  Tierquälerei melden; Tierquälerei anzeigen; Tiere vernachlässigt melden; Tierhaltung melden; Tierhaltung Verstoß; Tierhaltung Beschwerde;
  Tierhaltung Genehmigung; Tierhaltung Erlaubnis; Tierheim Erlaubnis; Tierpension Erlaubnis; Hundezucht Erlaubnis; Hundezüchter Genehmigung;
  Tierhandel Erlaubnis; Tierbörse Genehmigung; Hundeschule Erlaubnis; Tierschutz Hundeschule Erlaubnis; Reitstall Genehmigung; Pferdehaltung Genehmigung;
  Pferdepension Erlaubnis; Tiertransport; Tiertransport Genehmigung; Tiertransport Zulassung; Tierseuche; Tierseuche melden; Tierseuchenkasse;
  Geflügelpest; Vogelgrippe; Vogelgrippe melden; Aufstallungspflicht; Schweinepest; Afrikanische Schweinepest; Maul- und Klauenseuche;
  Blauzungenkrankheit; Tollwut; Tollwut Biss; Tollwutimpfung Pflicht; Tierimpfung Pflicht; Haustier Einreise; Hund Einreise; Katze Einreise;
  Haustiere Einreise EU; EU Heimtierausweis; Heimtierausweis; Heimtierausweis beantragen; Gesundheitszeugnis Tier; Gesundheitszeugnis Tier Export;
  Tiergesundheitszeugnis; Exportzertifikat Tier; Exportzertifikat Lebensmittel; Veterinärzertifikat; Veterinärbescheinigung; Lebensmittelüberwachung;
  Lebensmittelüberwachung melden; Lebensmittelkontrolle; Lebensmittelkontrolle Beschwerde; Lebensmittelhygiene; Lebensmittelhygiene Beschwerde;
  Lebensmittel verdorben melden; Restaurant Hygiene melden; Gaststätte Hygiene Beschwerde; Lebensmittelbetrieb anmelden; Lebensmittelunternehmer Registrierung;
  Lebensmittelbetrieb Registrierung; Imbiss Registrierung; Foodtruck Registrierung; Marktstand Lebensmittel Registrierung; Hofladen Registrierung;
  Direktvermarktung Registrierung; Schlachtung Genehmigung; Hausschlachtung; Hausschlachtung Genehmigung; Schlachthof Zulassung; Fleischbeschau;
  Fleischuntersuchung; Trichinenuntersuchung; Trichinenschau; Wildfleisch Untersuchung; Wildbret Untersuchung; Jagdbeute Untersuchung;
  Tierarzt Notdienst Behörde; Tierkörperbeseitigung; Tierkörperbeseitigung Anmeldung; Kadaver melden; Kadaver abholen; totes Tier melden;
  Wildunfall Tier verletzt; Verletztes Wildtier melden; Wildtier Auffangstation; Fundtier Tierheim; Haltung Wildtiere Genehmigung;
  Exotische Tiere Haltung Genehmigung; Gefährliche Tiere Haltung Genehmigung; Giftschlange Haltung Genehmigung; Zirkus Tiere Genehmigung;
  Zoo Genehmigung; Tierversuch Genehmigung; Tierversuche Anzeige; Qualzucht; Qualzucht melden; Tierhaltungsverbot; Tierhalteverbot;
  Tierhalteverbot Aufhebung; Tiere beschlagnahmt; Tierwegnahme; Tiere sicherstellen; Viehverkehrsverordnung; Viehzählung; Tierbestand melden;
  Tierbestand Meldung; Tierkennzeichnung; Ohrmarken beantragen; Ohrmarken Rinder; Rinderpass; HIT Datenbank; Schweinehaltung anmelden;
  Geflügelhaltung anmelden; Geflügelhaltung Anzeige; Hühnerhaltung anzeigen; Hühner anmelden; Hühner halten Genehmigung; Bienenhaltung anzeigen;
  Bienen anmelden; Imker anmelden; Imkerei anmelden; Bienenseuche; Amerikanische Faulbrut; Bienen Sperrbezirk

umweltamt:
  Umweltamt; Umweltamt Kontakt; Umweltamt Termin; Umweltbehörde; Umweltschutz; Umweltschutz Beschwerde; Umweltverschmutzung melden;
  Umweltverstoß melden; Umweltstraftat melden; Gewässerverschmutzung melden; Gewässerverunreinigung; Gewässerschutz; Gewässer Verschmutzung Meldung;
  Fischsterben melden; Ölspur melden; Ölunfall melden; Bodenverunreinigung; Altlasten; Altlastenauskunft; Altlastenkataster; Altlastenverdacht;
  Bodenschutz; Bodenschutzbehörde; Bodenaushub Entsorgung; Bodenaushub Genehmigung; Bodenaushub Verwertung; Bauschutt Entsorgung Genehmigung;
  Abfallentsorgung gewerblich; Abfallentsorgung Genehmigung; Abfallentsorgungsanlage; Abfallanlage Genehmigung; Abfallbeauftragter; Abfallbeauftragter melden;
  Entsorgungsfachbetrieb; Gefährliche Abfälle Nachweis; Nachweisverfahren Abfall; Abfallerzeuger Nummer; Abfallerzeugernummer; Abfallerzeugernummer beantragen;
  Altöl Entsorgung; Asbest Entsorgung; Asbest Entsorgung Genehmigung; Asbest melden; Asbest Sanierung; Asbest Sanierung Anzeige; Naturschutz;
  Naturschutzbehörde; Naturschutz Genehmigung; Naturschutzgebiet; Naturschutzgebiet Betreten; Landschaftsschutzgebiet; Landschaftsschutzgebiet Genehmigung;
  Biotopschutz; Biotop; Biotop Eingriff; Eingriffsregelung; Eingriffsregelung Ausgleich; Ausgleichsmaßnahmen; Ausgleichsfläche; Ersatzgeld Natur;
  Artenschutz; Artenschutz Genehmigung; Artenschutz Fledermaus; Fledermäuse Haus; Fledermaus Quartier melden; Nistkasten Genehmigung;
  geschützte Tiere melden; geschützte Pflanzen; Baumschutz; Baumschutzsatzung; Baumfällung; Baumfällung Genehmigung; Baum fällen;
  Baum fällen Antrag; Baum fällen Genehmigung; Baum fällen Nachbar; Baum fällen Garten; Baum entfernen Genehmigung; Baumschutzsatzung Antrag;
  Baumfällantrag; Baumpflege Genehmigung; Baum zurückschneiden Genehmigung; Hecke schneiden Verbot; Heckenschnitt Verbot Brutzeit; Heckenschnitt Zeitraum;
  Gartenbau Genehmigung; Wald Genehmigung; Waldumwandlung; Rodung Genehmigung; Rodung Antrag; Rodungsgenehmigung; Aufforstung Genehmigung;
  Wasserrecht; Wasserrecht Genehmigung; wasserrechtliche Erlaubnis; Gewässerbenutzung Erlaubnis; Wasserentnahme Genehmigung; Brunnen Genehmigung;
  Brunnen bohren Genehmigung; Brunnenbohrung; Brunnen anzeigen; Grundwasser Entnahme; Grundwasserbohrung; Erdwärme Bohrung Genehmigung;
  Erdwärme Genehmigung; Geothermie Genehmigung; Erdwärmesonde Genehmigung; Wärmepumpe Erdwärme Genehmigung; Regenwasser Einleitung Genehmigung;
  Einleitung Abwasser Genehmigung; Kleinkläranlage; Kleinkläranlage Genehmigung; Kleinkläranlage Wartung; Kleinkläranlage Nachrüstung;
  Abwassergrube; Abwassergrube Entsorgung; Wasserschutzgebiet; Wasserschutzgebiet Auflagen; Wasserschutzgebiet Genehmigung; Überschwemmungsgebiet;
  Überschwemmungsgebiet Genehmigung; Hochwasserschutz; Hochwasserschutz Genehmigung; Gewässerrandstreifen; Gewässer Ausbau Genehmigung;
  Teich anlegen Genehmigung; Fischteich Genehmigung; Steg Genehmigung; Bootssteg Genehmigung; Bootshaus Genehmigung; Immissionsschutz;
  Immissionsschutz Genehmigung; Immissionsschutzgesetz; Lärmschutz; Lärmschutz Beschwerde; Gewerbelärm; Industrielärm; Fluglärm Beschwerde;
  Schienenlärm; Straßenlärm Beschwerde; Geruchsbelästigung; Geruchsbelästigung melden; Gestank melden; Staub melden; Staubbelästigung;
  Luftverschmutzung melden; Rauchbelästigung; Rauchbelästigung Kamin; Kaminofen Nachbar; Kaminofen Nachbar Rauch; Schornsteinfeger Beschwerde;
  Feuerungsanlage; Feuerungsanlage Anzeige; Holzheizung Anzeige; Pelletheizung Genehmigung; Ölheizung Tank Genehmigung; Heizöltank Genehmigung;
  Heizöltank Anzeige; Heizöltank stilllegen; Lagerung wassergefährdende Stoffe; Tankstelle Genehmigung; Chemikalien Lagerung; Gefahrstoff Lagerung Genehmigung;
  Störfallverordnung; Seveso Betrieb; Emissionserklärung; Emissionserklärung abgeben; Emissionsmessung; Emissionsmessung Anordnung; Umweltverträglichkeitsprüfung;
  UVP; UVP Vorprüfung; Umweltinformation; Umweltinformationen beantragen; Umweltinformationsgesetz Antrag; Umweltbeschwerde; Umweltberatung;
  Umweltberatung Stadt; Energieberatung Stadt; Klimaschutz Förderung; Klimaschutzmanager; Klimaanpassung; Klimaschutz Antrag; Förderung Klimaschutz Stadt;
  Solaranlage Förderung Stadt; Balkonkraftwerk Anmeldung; Photovoltaik Anmeldung; Photovoltaik Anlage anmelden; Photovoltaik Genehmigung;
  Photovoltaik Marktstammdatenregister; Marktstammdatenregister Anmeldung; Solaranlage Marktstammdatenregister; Solaranlage anmelden;
  Solaranlage Netzbetreiber; Solaranlage Meldung; Solaranlage Denkmalschutz

gewerbeamt:
  Gewerbe anmelden; Gewerbe anmelden Kosten; Gewerbe anmelden online; Gewerbe anmelden Nebenberuf; Gewerbe anmelden Hauptberuf; Gewerbe anmelden Kleinunternehmer;
  Gewerbe anmelden Termin; Gewerbe anmelden Unterlagen; Gewerbe anmelden Voraussetzungen; Gewerbe anmelden GbR; Gewerbe anmelden GmbH; Gewerbe anmelden UG;
  Gewerbe anmelden Einzelunternehmen; Gewerbe anmelden Freiberufler; Gewerbe anmelden Student; Gewerbe anmelden Rentner; Gewerbe anmelden Arbeitslos;
  Gewerbe anmelden Ausländer; Gewerbe anmelden Wohnort; Gewerbeanmeldung; Gewerbeanmeldung Formular; Gewerbeanmeldung online; Gewerbeanmeldung Kosten;
  Gewerbeanmeldung ausfüllen; Gewerbeanmeldung Stadt; Gewerbeanmeldung Nebenerwerb; Gewerbeanmeldung Handel; Gewerbeanmeldung Online-Shop;
  Online Shop Gewerbe anmelden; Onlineshop anmelden; Amazon Verkäufer Gewerbe; Ebay Verkäufer Gewerbe; Etsy Gewerbe anmelden; Dropshipping Gewerbe;
  Dropshipping Gewerbe anmelden; YouTuber Gewerbe anmelden; Influencer Gewerbe anmelden; Gewerbe ummelden; Gewerbeummeldung; Gewerbe Adresse ändern;
  Gewerbe Umzug; Gewerbe verlegen; Gewerbe Tätigkeit ändern; Gewerbe erweitern; Gewerbe Erweiterung; Gewerbe abmelden; Gewerbeabmeldung;
  Gewerbe abmelden online; Gewerbe abmelden Kosten; Gewerbe aufgeben; Gewerbe einstellen; Gewerbe beenden; Gewerbe stilllegen; Gewerbe ruhen lassen;
  Gewerbeschein; Gewerbeschein beantragen; Gewerbeschein verloren; Gewerbeschein Kopie; Gewerbeanzeige; Gewerbeanzeige Änderung; Gewerbeamt;
  Gewerbeamt Termin; Gewerbeamt Kontakt; Gewerbeamt Öffnungszeiten; Gewerbeamt zuständig; Gewerbeamt Adresse; Gewerbebehörde; Gewerbestelle;
  Gewerbemeldestelle; Gewerbeangelegenheiten; Gewerberegister; Gewerberegisterauskunft; Gewerbezentralregister; Gewerbezentralregister Auskunft;
  Auskunft aus dem Gewerbezentralregister; Gewerbezentralregister Auszug; Gewerbeuntersagung; Gewerbeuntersagung Einspruch; Gewerbeuntersagung Aufhebung;
  Gewerbeuntersagung Wiederzulassung; Zuverlässigkeitsprüfung Gewerbe; Gewerbeerlaubnis; Gewerbeerlaubnis beantragen; Reisegewerbe; Reisegewerbekarte;
  Reisegewerbekarte beantragen; Reisegewerbekarte Kosten; Reisegewerbekarte Verkauf; Reisegewerbe Anzeige; Marktgewerbe; Marktprivileg; Marktgewerbe Anmeldung;
  Straßenhandel Erlaubnis; Straßenhändler Erlaubnis; Hausierer Erlaubnis; Haustürgeschäft Erlaubnis; Schausteller; Schausteller Erlaubnis;
  Schausteller Reisegewerbe; Gaststättengewerbe anmelden; Gaststättenerlaubnis beantragen; Gaststättenkonzession beantragen; Gaststätte eröffnen;
  Gaststätte eröffnen Genehmigung; Restaurant eröffnen; Restaurant eröffnen Genehmigung; Restaurant eröffnen Anmeldung; Café eröffnen; Café eröffnen Genehmigung;
  Bar eröffnen; Bar eröffnen Genehmigung; Kneipe eröffnen; Kneipe eröffnen Genehmigung; Imbiss eröffnen; Imbiss anmelden; Imbiss Genehmigung; Foodtruck anmelden;
  Foodtruck Genehmigung; Foodtruck Gewerbe; Eisdiele eröffnen; Bäckerei eröffnen; Schankgenehmigung; Schankerlaubnis beantragen; Alkohol ausschenken Erlaubnis;
  Alkoholausschank Genehmigung; Ausschank Genehmigung; Ausschank Verein Genehmigung; Ausschank Fest Genehmigung; Gestattung Ausschank; Gestattung Gaststätte;
  Vorübergehende Gaststättenerlaubnis; Sperrzeit beantragen; Sperrzeitverkürzung beantragen; Außengastronomie Genehmigung;
  Außengastronomie Erlaubnis; Außenbestuhlung Genehmigung; Biergarten Genehmigung; Terrasse Gaststätte Genehmigung; Shisha Bar Genehmigung;
  Shisha Bar eröffnen; Spielhalle eröffnen; Spielhalle Erlaubnis beantragen; Spielhalle Genehmigung; Wettbüro Erlaubnis; Wettbüro eröffnen;
  Spielautomaten aufstellen Erlaubnis; Geldspielgerät Aufstellung; Geldspielgerät aufstellen; Lotto Annahmestelle; Pfandleihe Erlaubnis; Pfandleiher Erlaubnis;
  Pfandleihgewerbe; Makler Erlaubnis; Immobilienmakler Erlaubnis; Immobilienmakler Erlaubnis 34c; 34c; 34c beantragen; Erlaubnis 34c; Erlaubnis nach 34c;
  Bauträger Erlaubnis; Baubetreuer Erlaubnis; Darlehensvermittler Erlaubnis; Darlehensvermittler 34i; 34i; 34i beantragen; Immobiliardarlehensvermittler;
  Finanzanlagenvermittler Erlaubnis; Finanzanlagenvermittler 34f; 34f; 34f beantragen; Versicherungsvermittler Erlaubnis; Versicherungsvermittler 34d;
  34d; 34d beantragen; Versicherungsmakler Erlaubnis; Versicherungsberater Erlaubnis; Honorar-Finanzanlagenberater; Bewachungsgewerbe; Bewachungsgewerbe 34a;
  34a; 34a beantragen; 34a Erlaubnis; Sicherheitsdienst gründen; Sicherheitsdienst Erlaubnis; Sicherheitsdienst Gewerbe;
  Türsteher Erlaubnis; Türsteher Zulassung; Detektei Gewerbe; Detektei Erlaubnis; Schlüsseldienst Gewerbe; Schlüsseldienst anmelden; Auktionator Erlaubnis;
  Versteigerer Erlaubnis; Versteigerungsgewerbe; Trödelhandel anmelden; Antiquitätenhandel anmelden; Gebrauchtwarenhandel Anzeige; Altmetallhandel Anzeige;
  Altwarenhandel Anzeige; Schrotthandel anmelden; Autohandel anmelden; Autohandel Gewerbe; Gebrauchtwagenhandel anmelden; Kfz Handel Gewerbe;
  Waffenhandel Erlaubnis; Waffenhandel Gewerbe; Pyrotechnik Erlaubnis; Pyrotechnik Gewerbe;
  Feuerwerk verkaufen Erlaubnis; Feuerwerk Verkauf Genehmigung; Tabakwaren Verkauf; Tabakhandel anmelden; Kiosk eröffnen; Kiosk anmelden; Spätkauf anmelden;
  Spätkauf Gewerbe; Lebensmittelhandel anmelden; Einzelhandel anmelden; Einzelhandel Gewerbe; Großhandel anmelden; Großhandel Gewerbe; Gewerbe Nebentätigkeit;
  Gewerbe nebenbei; Nebengewerbe anmelden; Nebengewerbe Anmeldung; Nebengewerbe Steuer; Nebengewerbe Rentner; Kleingewerbe anmelden;
  Kleingewerbe Anmeldung; Kleingewerbe Kosten; Kleingewerbe Voraussetzungen; Kleingewerbe Wohnung; Gewerbe von zu Hause anmelden; Gewerbe Wohnung anmelden;
  Gewerbe Home Office anmelden; Gewerbe Privatadresse; Gewerbe Vermieter Erlaubnis; Gewerbe Mietwohnung; Firma gründen; Firma gründen Ablauf;
  Firma gründen Anmeldung; Firma gründen Kosten; Firma anmelden; Firma abmelden; Firma ummelden; Betrieb anmelden; Betrieb abmelden; Betrieb ummelden;
  Betriebsanmeldung; Betriebsabmeldung; Betriebsverlegung; Betriebsübernahme; Betriebsübernahme Gewerbe; Betriebsübernahme Gewerbeamt; Geschäftsübernahme;
  Geschäftsübernahme Gewerbe; Gewerbe übernehmen; Gewerbe übernehmen Anmeldung; Gewerbe Erbe; Gewerbe Erbe anmelden; Gewerbe Inhaberwechsel;
  Inhaberwechsel Gewerbe; Gesellschafterwechsel Gewerbe; Gesellschafterwechsel Gewerbeamt; Geschäftsführerwechsel Gewerbeamt; Rechtsformwechsel Gewerbe;
  Existenzgründung; Existenzgründung Anmeldung; Existenzgründung Beratung; Existenzgründung Gewerbeamt; Existenzgründer Anmeldung; Existenzgründer Gewerbe;
  Selbstständig machen; Selbstständig machen Anmeldung; selbstständig machen Gewerbe; Selbstständigkeit anmelden; Selbstständigkeit Gewerbe anmelden;
  Selbstständigkeit Gewerbeanmeldung; Selbstständigkeit Gewerbe; Freiberuf oder Gewerbe; Freiberufler oder Gewerbe; Gewerbe oder Freiberuf
"""

FAELLE_SONSTIGES = """
ihk:
  IHK; IHK Termin; IHK Kontakt; IHK Beitrag; IHK Mitgliedschaft; IHK Beitrag befreien; IHK Beitrag Kleingewerbe; Industrie- und Handelskammer;
  Handelskammer; IHK Ausbildung; Ausbildungsvertrag eintragen IHK; Ausbildungsverhältnis eintragen; Ausbilder Eignung IHK; Ausbildereignungsprüfung;
  AEVO Prüfung; Ausbilderschein; IHK Prüfung; IHK Abschlussprüfung; IHK Zwischenprüfung; IHK Zeugnis; IHK Zeugnis Duplikat; IHK Prüfung Anmeldung;
  IHK Fortbildung; IHK Weiterbildung; Fachwirt Prüfung; Meisterprüfung Industrie; Industriemeister Prüfung; Betriebswirt IHK; Sachkundeprüfung;
  Sachkundeprüfung 34a; Sachkundeprüfung 34c; Sachkundeprüfung 34d; Sachkundeprüfung 34f; Sachkundeprüfung Bewachung; Sachkundeprüfung Gaststätte;
  Unterrichtungsverfahren Gaststätte; Unterrichtung Gaststättengewerbe; Gaststättenunterrichtung; Ursprungszeugnis; Ursprungszeugnis beantragen;
  Ursprungszeugnis online; Carnet ATA; Carnet ATA beantragen; Exportdokumente; Exportdokumente beglaubigen; Handelsrechnung beglaubigen; Außenwirtschaft Beratung;
  Existenzgründungsberatung IHK; Gründungsberatung IHK; Unternehmensnachfolge IHK; Nachfolgebörse; Firmenbewertung IHK; Sachverständiger öffentlich bestellt;
  Sachverständiger bestellen lassen; Sachverständigenbestellung; Handelsregister IHK Stellungnahme; Firmierung IHK Stellungnahme; Wettbewerbsrecht IHK;
  IHK Schlichtung; Einigungsstelle Wettbewerb; Ausbildungsberater IHK; Prüfungsausschuss IHK; ehrenamtlicher Prüfer IHK; Fachkräftesicherung IHK;
  Berufsanerkennung IHK FOSA; Anerkennung ausländischer Berufsabschluss IHK; Anerkennung Berufsabschluss Ausland; Anerkennungsverfahren Berufsabschluss

handwerkskammer:
  Handwerkskammer; HWK; Handwerkskammer Termin; Handwerkskammer Kontakt; Handwerkskammer Beitrag; Handwerksrolle; Handwerksrolle Eintragung;
  Handwerksrolle eintragen; Handwerksrolle Löschung; Eintrag Handwerksrolle; Handwerk anmelden; Handwerksbetrieb anmelden; Handwerksbetrieb gründen;
  Handwerksbetrieb Anmeldung; Handwerk selbstständig; Meisterpflicht; Meisterpflicht Handwerk; Meisterbrief; Meisterbrief Duplikat; Meisterprüfung;
  Meisterprüfung Anmeldung; Meisterprüfung Handwerk; Meisterschule Handwerk; Meister Förderung; Meister-BAföG; Aufstiegs-BAföG Handwerk; Meistergründungsprämie;
  Gesellenprüfung; Gesellenprüfung Anmeldung; Gesellenbrief; Gesellenbrief Duplikat; Gesellenbrief verloren; Gesellenzeugnis; Lehrling anmelden;
  Lehrlingsrolle; Lehrlingsrolle Eintragung; Ausbildungsvertrag Handwerk; Ausbildungsvertrag Handwerkskammer; Ausbildungsverhältnis Handwerk eintragen;
  Lehrvertrag eintragen; Lehrstelle Handwerk; Lehrstellenbörse Handwerk; Ausbildungsberatung Handwerk; Betriebsberatung Handwerk; Betriebsnachfolge Handwerk;
  Betriebsübergabe Handwerk; Betriebsübergabe Handwerkskammer; Ausnahmebewilligung Handwerk; Ausnahmebewilligung Handwerksrolle; Altgesellenregelung;
  Altgesellenregelung Handwerk; Handwerkskarte; Handwerkskarte beantragen; Handwerkskarte verloren; Handwerkskammer Zeugnis Ausland; Anerkennung Handwerk Ausland;
  Berufsanerkennung Handwerk; Anerkennung Gesellenbrief Ausland; Sachverständiger Handwerk; Schlichtung Handwerk; Handwerkskammer Schlichtung; Innungsmitgliedschaft;
  Innung beitreten; Kreishandwerkerschaft; Schornsteinfeger Bestellung

rentenversicherung:
  Rente; Rente beantragen; Rentenantrag; Rentenantrag stellen; Rentenantrag online; Rentenantrag Termin; Rente Antrag Frist; Rente Beginn; Rente Höhe;
  Rente berechnen; Rente Auskunft; Rentenauskunft; Rentenauskunft beantragen; Rentenauskunft online; Renteninformation; Renteninformation Jahresende;
  Rentenbescheid; Rentenbescheid Widerspruch; Rentenbescheid prüfen; Rentenbescheid verloren; Rentenbescheid Duplikat; Rentenversicherung; Rentenversicherung Termin;
  Rentenversicherung Kontakt; Rentenversicherung Beratung; Rentenversicherung Beratungsstelle; Rentenversicherung Adresse; Deutsche Rentenversicherung;
  DRV; DRV Bund; Rentenversicherungsnummer; Rentenversicherungsnummer verloren; Rentenversicherungsnummer beantragen; Sozialversicherungsausweis;
  Sozialversicherungsausweis verloren; Sozialversicherungsausweis beantragen; Sozialversicherungsnummer; Sozialversicherungsnummer herausfinden;
  Altersrente; Altersrente beantragen; Altersrente Voraussetzungen; Altersrente für langjährig Versicherte; Altersrente für besonders langjährig Versicherte;
  Altersrente für schwerbehinderte Menschen; Altersrente für Frauen; Rente mit 63; Rente mit 67; Regelaltersrente; Frührente; vorzeitige Rente;
  Rente vorzeitig; Rente früher; Rente Abschläge; Rente Zuschläge; Teilrente; Teilrente beantragen; Flexirente; Hinzuverdienst Rente; Hinzuverdienstgrenze;
  Rente Nebenjob; Rente weiterarbeiten; Rentner Minijob; Erwerbsminderungsrente; Erwerbsminderungsrente beantragen; Erwerbsminderungsrente Antrag;
  Erwerbsminderungsrente Voraussetzungen; Erwerbsminderungsrente Höhe; Erwerbsminderung; Erwerbsminderung Rente; Berufsunfähigkeit Rente; Rente Krankheit;
  Rente Schwerbehinderung; Rente wegen Erwerbsminderung; teilweise Erwerbsminderung; volle Erwerbsminderung; Witwenrente; Witwenrente beantragen;
  Witwerrente; Witwenrente Antrag; Witwenrente Höhe; Witwenrente Wiederheirat; Witwenrente Einkommen; Hinterbliebenenrente; Hinterbliebenenrente beantragen;
  Waisenrente; Waisenrente beantragen; Waisenrente Antrag; Waisenrente Ausbildung; Sterbevierteljahr; Rentenanspruch Ehepartner gestorben;
  Rente Todesfall; Rente nach Tod; Rente Tod Ehepartner; Rente Erbe; Rentenüberzahlung; Rentenüberzahlung Todesfall; Rente Auszahlung Todesfall;
  Rentenauszahlung; Rentenauszahlung Termin; Rentenauszahlung Konto; Rentenauszahlung Konto ändern; Rente Kontoänderung; Rente Adresse ändern;
  Rente Namensänderung; Rente Umzug; Rente Ausland; Rente ins Ausland; Rente Auslandsumzug; Rente Auslandsaufenthalt; Rente Ausland Lebensbescheinigung;
  Rentenzahlung Ausland; Rentner Ausland; Rente EU; Rente Türkei; Rente Polen; Rente Spanien; Rente Italien; Rente Griechenland; Rente USA; Rentenanspruch Ausland;
  Rentenanspruch prüfen; Rentenanspruch Ausland prüfen; Beitragszeiten; Beitragszeiten prüfen; Beitragszeiten Lücke; Beitragslücke schließen; Versicherungsverlauf;
  Versicherungsverlauf anfordern; Versicherungsverlauf prüfen; Kontenklärung; Kontenklärung beantragen; Kontenklärung Antrag; Kontenklärung Rente; Rentenkonto;
  Rentenkonto klären; Rentenkonto prüfen; Anrechnungszeiten; Kindererziehungszeiten; Kindererziehungszeiten anrechnen; Mütterrente; Mütterrente beantragen;
  Kinderberücksichtigungszeiten; Pflegezeiten Rente; Pflege Rente Beitrag; Ausbildungszeiten Rente;
  Studienzeiten Rente; Schulzeiten Rente; Zeiten Arbeitslosigkeit Rente; Krankheitszeiten Rente; Wehrdienst Rente; Zivildienst Rente; Beitragserstattung;
  Beitragserstattung Rentenversicherung; Rentenbeiträge zurück; Rentenbeiträge Ausland erstatten; Rentenbeiträge nachzahlen; Rentenbeiträge freiwillig;
  freiwillige Beiträge Rentenversicherung; freiwillige Rentenversicherung; freiwillig versichern Rente; Nachzahlung Rentenbeiträge; Nachzahlung Rentenbeiträge Studium;
  Nachzahlung Ausbildungszeiten; Beiträge Selbstständige Rentenversicherung; Selbstständige Rentenversicherungspflicht; Rentenversicherungspflicht Selbstständige;
  Rentenversicherungspflicht; Rentenversicherungspflicht Befreiung; Befreiung Rentenversicherung; Befreiung Rentenversicherung Antrag; Befreiung Rentenversicherung Student;
  Befreiung Rentenversicherung Minijob; Minijob Rentenversicherung; Minijob Rentenversicherungspflicht; Minijob Befreiung; Minijob Befreiung Rentenversicherung;
  Statusfeststellung; Statusfeststellung Selbstständige; Statusfeststellungsverfahren; Clearingstelle Rentenversicherung; Scheinselbstständigkeit;
  Scheinselbstständigkeit prüfen; Versorgungsausgleich Rente; Versorgungsausgleich Auskunft; Rentensplitting; Rentensplitting Ehegatten; Rentensplitting beantragen;
  Rehabilitation; Reha; Reha beantragen; Reha Antrag; Reha Rentenversicherung; Reha Rentenversicherung Antrag; Reha Kur Antrag; Kur beantragen; Kur Antrag Rente;
  Kur Rentenversicherung; Medizinische Reha; Medizinische Rehabilitation; medizinische Rehabilitation beantragen; Anschlussheilbehandlung; Anschlussheilbehandlung Antrag;
  Anschlussrehabilitation; Nachsorge Reha; Rehabilitation Eltern Kind; Mutter-Kind-Kur; Vater-Kind-Kur; Kinderreha; Kinderrehabilitation; Reha Kinder Antrag;
  Reha Sucht; Suchtreha beantragen; Entwöhnungsbehandlung; Entwöhnung Antrag; Berufliche Reha; Teilhabe Arbeitsleben Rentenversicherung; Umschulung Rentenversicherung;
  Umschulung Rente; Übergangsgeld; Übergangsgeld beantragen; Übergangsgeld Reha; Übergangsgeld Höhe; Reisekosten Reha; Zuzahlung Reha; Zuzahlung Reha Befreiung;
  Riester Rente; Riester Rente Zulage; Riester Zulage beantragen; Riester Zulage Antrag; Riester kündigen; Riester Rente kündigen; Riester Rente Auszahlung; Riester Wohnriester;
  Rürup Rente; Rürup beantragen; Basisrente; Betriebsrente; Betriebsrente Auszahlung; Betriebsrente Krankenkasse; Zusatzrente; Zusatzversorgung; Zusatzversorgung öffentlicher Dienst;
  VBL Rente; Versorgungsanstalt Bund Länder; Knappschaft Rente; Knappschaft Bahn See; Rente Bergbau; Rente Seeleute; Landwirtschaftliche Alterskasse; Alterskasse Landwirte;
  Rente Landwirt; Künstlersozialkasse; Künstlersozialkasse Anmeldung; KSK Anmeldung; Künstlersozialversicherung; Versorgungswerk; Versorgungswerk Befreiung;
  Rentenversicherung Ärzte Befreiung; Anwaltsversorgung; Rente Beamte; Pension Beamte; Versorgungsbezüge; Versorgungsbezüge Beamte; Pension Antrag; Ruhegehalt

krankenkasse:
  Krankenkasse; Krankenkasse wechseln; Krankenkassenwechsel; Krankenkasse Wechsel Kündigung; Krankenkasse kündigen; Krankenkasse Kündigung Frist; Krankenkasse Sonderkündigungsrecht;
  Krankenkasse Beitrag; Krankenkasse Beitrag Erhöhung; Zusatzbeitrag; Zusatzbeitrag Krankenkasse; Zusatzbeitrag Vergleich; Krankenkassen Vergleich; Krankenkasse Vergleich;
  gesetzliche Krankenversicherung; gesetzliche Krankenkasse; GKV; GKV Beitrag; GKV Anmeldung; Krankenversicherung; Krankenversicherung anmelden; Krankenversicherung Pflicht;
  Krankenversicherungspflicht; Krankenversicherung Nachweis; Krankenversicherung Nachweis Arbeitgeber; Krankenversicherung Bescheinigung; Mitgliedsbescheinigung Krankenkasse;
  Mitgliedsbescheinigung beantragen; Versicherungsnachweis Krankenkasse; Versicherungsnachweis; Versicherungsbescheinigung; Versicherungsbescheinigung Krankenkasse;
  Familienversicherung; Familienversicherung anmelden; Familienversicherung Kind; Familienversicherung Ehepartner; Familienversicherung Student; Familienversicherung Einkommen;
  Familienversicherung Altersgrenze; Familienversicherung Ende; Familienversicherung Antrag; Kind Krankenversicherung anmelden; Baby Krankenversicherung anmelden;
  Neugeborenes Krankenversicherung; Neugeborenes versichern; Kind versichern; Kind mitversichern; Ehepartner mitversichern; Student Krankenversicherung;
  Studenten Krankenversicherung; Studentische Krankenversicherung; Student Krankenversicherung Beitrag; Student Krankenversicherung Befreiung; Student Krankenversicherung Familienversicherung;
  Krankenversicherung Praktikum; Krankenversicherung Werkstudent; Krankenversicherung Minijob; Krankenversicherung Selbstständige; Krankenversicherung Selbstständige Beitrag;
  Krankenversicherung Arbeitslosigkeit; Krankenversicherung Rentner; Krankenversicherung der Rentner; KVdR; KVdR Antrag; KVdR Zugang; Krankenversicherung Rente;
  freiwillige Krankenversicherung; freiwillig versichert Krankenkasse; Krankenversicherung freiwillig Beitrag; Beitragsbemessung freiwillig; Beitragseinstufung;
  Beitragseinstufung Krankenkasse; Einkommensnachweis Krankenkasse; Einkommensteuerbescheid Krankenkasse; Krankenkasse Rückerstattung; Krankenkasse Beitragsrückstand;
  Beitragsschulden Krankenkasse; Beitragsschulden Krankenversicherung; Krankenkasse Schulden; Krankenkasse Ratenzahlung; Krankenkasse Mahnung; Krankenkasse Stundung;
  Krankenversicherung nicht bezahlt; Krankenversicherung ruhend; ruhende Leistungen Krankenkasse; Notfallbehandlung ruhend; Basistarif; Basistarif Hilfe; Basistarif Krankenversicherung;
  private Krankenversicherung; PKV; PKV Wechsel; PKV Rückkehr GKV; PKV Beitragserhöhung; PKV zu GKV; PKV Beihilfe; Beihilfe; Beihilfe Antrag; Beihilfe Beamte;
  Gesundheitskarte; Gesundheitskarte beantragen; Gesundheitskarte verloren; Gesundheitskarte Ersatz; Gesundheitskarte neu; Gesundheitskarte Foto; Gesundheitskarte defekt;
  Gesundheitskarte abgelaufen; Gesundheitskarte Namensänderung; Gesundheitskarte Adresse ändern; Gesundheitskarte PIN; Gesundheitskarte NFC; elektronische Gesundheitskarte;
  Versichertenkarte; Versichertenkarte verloren; Krankenversichertenkarte; Krankenversichertenkarte verloren; Krankenversichertenkarte beantragen; Krankenversichertenkarte Ersatz;
  Krankenversichertennummer; Krankenversichertennummer herausfinden; Versichertennummer; Versichertennummer Krankenkasse; elektronische Patientenakte; ePA; ePA Widerspruch;
  ePA aktivieren; ePA Zugang; E-Rezept; E-Rezept App; E-Rezept einlösen; E-Rezept Gesundheitskarte; Krankengeld; Krankengeld beantragen; Krankengeld Antrag; Krankengeld Höhe;
  Krankengeld Dauer; Krankengeld Auszahlung; Krankengeld Aussteuerung; Krankengeld Selbstständige; Krankengeld Arbeitslos; Krankengeld Ende; Krankengeld Rente; Krankengeld Nachzahlung;
  Krankengeld Kind; Kinderkrankengeld; Kinderkrankengeld beantragen; Kinderkrankengeld Antrag; Kinderkrankentage; Kinderkrankentage Anspruch; Kind krank Arbeit; Kind krank Krankengeld;
  Krankschreibung; Krankschreibung Krankenkasse; Krankschreibung Arbeitgeber; Krankschreibung Arbeitsunfähigkeit; Arbeitsunfähigkeitsbescheinigung; Arbeitsunfähigkeitsbescheinigung digital;
  eAU; eAU Abruf; AU Bescheinigung; AU Bescheinigung Krankenkasse; AU Bescheinigung verloren; AU Bescheinigung nicht erhalten; Mutterschaftsgeld; Mutterschaftsgeld beantragen;
  Mutterschaftsgeld Antrag; Mutterschaftsgeld Höhe; Mutterschaftsgeld Arbeitgeber; Mutterschaftsgeld Bundesamt für Soziale Sicherung; Mutterschaftsgeld Student; Entbindungsgeld;
  Entbindungsgeld beantragen; Hebamme Kosten Krankenkasse; Hebamme Krankenkasse; Geburtsvorbereitung Krankenkasse; Schwangerschaft Krankenkasse; Schwangerschaft Leistungen Krankenkasse;
  Zahnersatz Zuschuss; Zahnersatz Festzuschuss; Zahnersatz Antrag; Zahnersatz Bonusheft; Bonusheft; Bonusheft Zahnarzt; Bonusheft verloren; Heil- und Kostenplan; Heil- und Kostenplan Zahnarzt;
  Heil- und Kostenplan Krankenkasse; Kostenübernahme Krankenkasse; Kostenübernahme beantragen Krankenkasse; Kostenübernahme Behandlung; Kostenübernahme Hilfsmittel; Kostenübernahme Brille;
  Kostenübernahme Hörgerät; Kostenübernahme Rollstuhl; Kostenübernahme Krankentransport; Kostenübernahme Kur; Kostenübernahme Reha; Kostenübernahme Psychotherapie;
  Kostenübernahme Ausland; Kostenübernahme Zahnarzt; Kostenübernahme Medikament; Kostenübernahme Operation; Kostenübernahme künstliche Befruchtung; Kinderwunsch Krankenkasse;
  Kinderwunschbehandlung Zuschuss; künstliche Befruchtung Zuschuss; Hilfsmittel; Hilfsmittel Antrag; Hilfsmittel Krankenkasse; Hilfsmittel Rezept; Hilfsmittel Zuzahlung; Hilfsmittel Verordnung;
  Rollstuhl beantragen; Rollator beantragen; Hörgerät beantragen; Hörgerät Zuschuss; Hörgerät Krankenkasse; Brille Zuschuss Krankenkasse; Brille Krankenkasse; Kontaktlinsen Krankenkasse;
  Sehhilfe Krankenkasse; Orthopädische Schuhe Krankenkasse; Einlagen Krankenkasse; Kompressionsstrümpfe Krankenkasse; Prothese Krankenkasse; Pflegebett Krankenkasse; Inkontinenz Krankenkasse;
  Zuzahlung; Zuzahlung Befreiung; Zuzahlungsbefreiung; Zuzahlungsbefreiung beantragen; Belastungsgrenze; Belastungsgrenze Krankenkasse; Befreiungsausweis; Befreiungsausweis Zuzahlung;
  Zuzahlung Medikamente; Zuzahlung Krankenhaus; Zuzahlung Reha Krankenkasse; Zuzahlung Hilfsmittel Krankenkasse; Zuzahlung Fahrtkosten; Krankenfahrt; Krankenfahrt beantragen;
  Krankenfahrt Genehmigung; Krankenfahrt Taxi; Krankenfahrt Erstattung; Krankentransport; Krankentransport Verordnung; Krankentransport Genehmigung; Fahrtkosten Krankenkasse;
  Fahrtkostenerstattung Krankenkasse; Fahrtkosten Dialyse; Fahrtkosten Chemotherapie; Fahrtkosten Bestrahlung; Krankenhausaufenthalt Krankenkasse; Krankenhaus Zuzahlung;
  Krankenhaus Kostenübernahme; Krankenhauseinweisung; Reha Krankenkasse; Kur Krankenkasse; Kur Krankenkasse Antrag; Vorsorgekur; Vorsorgekur beantragen; ambulante Vorsorgeleistung;
  Gesundheitskurs Krankenkasse; Präventionskurs Krankenkasse; Präventionskurs Zuschuss; Fitnessstudio Zuschuss Krankenkasse; Bonusprogramm Krankenkasse; Bonusprogramm; Bonusprogramm Antrag;
  Vorsorgeuntersuchung Krankenkasse; Vorsorgeuntersuchung Erwachsene; Vorsorgeuntersuchung Krebs; Krebsvorsorge; Krebsvorsorge Krankenkasse; Zahnvorsorge Krankenkasse;
  Impfung Krankenkasse; Reiseimpfung Krankenkasse; Reiseimpfung Kostenübernahme; Auslandskrankenversicherung Krankenkasse; Europäische Krankenversicherungskarte; EHIC;
  EHIC beantragen; EHIC verloren; EHIC Ersatz; EHIC Ersatzbescheinigung; Krankenversicherung Ausland; Krankenversicherung Ausland Urlaub; Krankenversicherung Auslandsaufenthalt;
  Krankenversicherung Auswanderung; Krankenversicherung Zuzug; Krankenversicherung Ausländer; Krankenversicherung Asylbewerber; Krankenversicherung Flüchtling; Krankenversicherung Ukraine;
  Krankenversicherung Einwanderer; Krankenversicherung Au-pair; Krankenversicherung Au Pair; Krankenversicherung Doktorand; Krankenversicherung Promotion; Krankenversicherung Referendar;
  Krankenversicherung Beamtenanwärter; Krankenversicherung Elternzeit; Krankenversicherung Mutterschutz; Krankenversicherung Sabbatical; Krankenversicherung unbezahlter Urlaub;
  Krankenversicherung Rente Ausland; Krankenversicherung Scheidung; Krankenversicherung Trennung; Krankenversicherung Tod Ehepartner; Krankenversicherung Witwe;
  Krankenversicherung Waise; Krankenversicherung Studium Ende; Krankenversicherung nach Studium; Krankenversicherung nach Ausbildung; Krankenversicherung Berufseinstieg

pflegekasse:
  Pflegekasse; Pflegekasse Kontakt; Pflegekasse Termin; Pflegekasse Beratung; Pflegeberatung; Pflegeberatung Anspruch; Pflegeberatung Termin; Pflegeberatung Hausbesuch;
  Pflegeberatungsbesuch; Beratungsbesuch Pflege; Beratungseinsatz Pflege; Pflegestützpunkt; Pflegestützpunkt Beratung; Pflegestützpunkt Termin; soziale Pflegeversicherung;
  Pflegeversicherung; Pflegeversicherung Beitrag; Pflegeversicherung Antrag; Pflegeversicherung Leistungen; Pflegeversicherung Kinderlose; Pflegeversicherung Zuschlag;
  Pflegeversicherung Student; Pflegeversicherung privat; Pflegegrad; Pflegegrad beantragen; Pflegegrad Antrag; Pflegegrad Antrag stellen; Pflegegrad Antrag online;
  Pflegegrad Antrag Formular; Pflegegrad erhöhen; Pflegegrad Höherstufung; Pflegegrad Höherstufung Antrag; Pflegegrad Widerspruch; Pflegegrad Ablehnung; Pflegegrad Bescheid;
  Pflegegrad Begutachtung; Pflegegrad Begutachtung Termin; Pflegegrad Begutachtung Vorbereitung; Pflegegrad Gutachten; Pflegegrad MD; MD Begutachtung; Medizinischer Dienst Pflege;
  Medizinischer Dienst Begutachtung; Medizinischer Dienst Gutachten; Pflegegrad 1; Pflegegrad 2; Pflegegrad 3; Pflegegrad 4; Pflegegrad 5; Pflegegrad Demenz; Pflegegrad Kinder;
  Pflegegrad Kind beantragen; Pflegegrad Behinderung; Pflegegrad Rückwirkend; Pflegegrad Antrag rückwirkend; Pflegegrad Wiederholungsbegutachtung; Pflegestufe; Pflegestufe beantragen;
  Pflegestufe Antrag; Pflegegeld; Pflegegeld beantragen; Pflegegeld Antrag; Pflegegeld Höhe; Pflegegeld Auszahlung; Pflegegeld Rente; Pflegegeld Steuer; Pflegegeld Angehörige;
  Pflegegeld Verhinderungspflege; Pflegesachleistung; Pflegesachleistungen; Pflegesachleistung Antrag; ambulanter Pflegedienst; ambulanter Pflegedienst Kosten; Pflegedienst Kostenübernahme;
  Pflegedienst Krankenkasse; Pflegedienst finden; Pflegedienst Vertrag; Kombinationsleistung; Kombinationsleistung Pflege; Verhinderungspflege; Verhinderungspflege Antrag;
  Verhinderungspflege beantragen; Verhinderungspflege Höhe; Verhinderungspflege Urlaub; Verhinderungspflege Angehörige; Kurzzeitpflege; Kurzzeitpflege Antrag; Kurzzeitpflege beantragen;
  Kurzzeitpflege Kosten; Kurzzeitpflege Platz; Kurzzeitpflege Krankenhaus; Kurzzeitpflege Urlaub; Tagespflege; Tagespflege Antrag; Tagespflege Kosten; Tagespflege Pflegegrad;
  Tagespflege Platz; Tagespflege Zuschuss; Nachtpflege; Nachtpflege Antrag; Entlastungsbetrag; Entlastungsbetrag Pflege; Entlastungsbetrag beantragen; Entlastungsbetrag 125 Euro;
  Entlastungsbetrag 131 Euro; Entlastungsbetrag Antrag; Entlastungsbetrag Haushaltshilfe; Entlastungsbetrag Alltagshilfe; Entlastungsbetrag verfallen; Entlastungsleistungen;
  Entlastungsleistungen Pflege; Pflegehilfsmittel; Pflegehilfsmittel Antrag; Pflegehilfsmittel beantragen; Pflegehilfsmittel Erstattung; Pflegehilfsmittel Box; Pflegehilfsmittel zum Verbrauch;
  Pflegehilfsmittel 40 Euro; Pflegehilfsmittel 42 Euro; Pflegehilfsmittel Handschuhe; Pflegehilfsmittel Desinfektion; Pflegebett beantragen; Pflegebett Antrag; Pflegebett Kosten;
  Pflegebett Leihe; Hausnotruf; Hausnotruf Kostenübernahme; Hausnotruf Pflegekasse; Hausnotruf Antrag; Hausnotruf beantragen; Treppenlift; Treppenlift Zuschuss; Treppenlift Pflegekasse;
  Wohnumfeldverbessernde Maßnahmen; Wohnumfeldverbesserung; Wohnumfeldverbesserung Antrag; Wohnraumanpassung; Wohnraumanpassung Zuschuss; Wohnraumanpassung Antrag; Wohnraumanpassung Pflegekasse;
  Badumbau Zuschuss; Badumbau Pflegekasse; Badumbau Pflegegrad; Barrierefreier Umbau Zuschuss; Barrierefreier Umbau Pflegekasse; barrierefrei umbauen Zuschuss; Umbau Pflegebedürftige;
  Pflegewohngemeinschaft; Pflegewohngemeinschaft Zuschuss; Wohngruppenzuschlag; Wohngruppenzuschlag beantragen; Anschubfinanzierung Wohngruppe; Pflegeheim; Pflegeheim Antrag;
  Pflegeheim Aufnahme; Pflegeheim Platz; Pflegeheim Kosten; Pflegeheim Zuschuss; Pflegeheim Pflegekasse; Pflegeheim Leistungszuschlag; Leistungszuschlag Pflegeheim; Leistungszuschlag stationär;
  Eigenanteil Pflegeheim; Eigenanteil Pflege; vollstationäre Pflege; vollstationäre Pflege Antrag; stationäre Pflege; stationäre Pflege Kosten; stationäre Pflege Zuschuss; Heimplatz;
  Heimplatz suchen; Heimplatz Kosten; Pflegeplatz; Pflegeplatz suchen; Pflegeplatz Kosten; Pflegezeit; Pflegezeit Arbeitgeber; Pflegezeit beantragen; Pflegezeit Antrag; Pflegezeit Angehörige;
  Familienpflegezeit; Familienpflegezeit Antrag; Familienpflegezeit Darlehen; Pflegeunterstützungsgeld; Pflegeunterstützungsgeld beantragen; Pflegeunterstützungsgeld Antrag;
  Pflegeunterstützungsgeld Höhe; kurzzeitige Arbeitsverhinderung Pflege; akute Pflege Arbeit; Pflege Angehörige Arbeit; Pflege Angehörige Rente; Rentenversicherung Pflegepersonen;
  Pflegeperson Rentenversicherung; Pflegeperson Beitrag; Pflegeperson Unfallversicherung; Pflegeperson Arbeitslosenversicherung; Pflegende Angehörige; Pflegende Angehörige Hilfe;
  Pflegende Angehörige Unterstützung; Pflegekurs; Pflegekurs Angehörige; Pflegekurs kostenlos; Pflegekurs Anmeldung; Pflegekurs Pflegekasse; Pflegeschulung; Pflegeschulung zu Hause;
  häusliche Pflege; häusliche Pflege Hilfe; häusliche Pflege Antrag; häusliche Pflege Kosten; häusliche Pflege Zuschuss; häusliche Krankenpflege; häusliche Krankenpflege Antrag;
  häusliche Krankenpflege Verordnung; häusliche Krankenpflege Krankenkasse; 24 Stunden Pflege; 24 Stunden Pflege Kosten; 24 Stunden Pflege Zuschuss; Pflegekraft Ausland; Pflegekraft Osteuropa;
  Pflege zu Hause; Pflege Eltern; Pflege Eltern Hilfe; Pflege Eltern Kosten; Pflege Eltern Antrag; Pflege Demenz; Pflege Demenz Hilfe; Pflege Demenz Antrag; Pflege Demenz Kosten;
  Demenz Betreuung; Demenz Betreuung Antrag; Demenz Betreuungsgruppe; Betreuungsgruppe; Betreuungsleistungen Pflege; Betreuungsdienst; Betreuungsdienst Pflege; Alltagsbegleiter;
  Alltagsbegleiter Pflege; Alltagsbegleiter Entlastungsbetrag; Nachbarschaftshilfe Pflege; Nachbarschaftshilfe Entlastungsbetrag; Nachbarschaftshilfe anerkennen

bafoegamt:
  BAföG; BAföG beantragen; BAföG Antrag; BAföG Antrag online; BAföG Antrag Formular; BAföG Antrag Frist; BAföG Antrag Student; BAföG Antrag Schüler; BAföG Antrag Unterlagen;
  BAföG Antrag ausfüllen; BAföG Antrag Hilfe; BAföG Antrag Eltern; BAföG Antrag Elternunabhängig; elternunabhängiges BAföG; elternunabhängig BAföG; BAföG Höhe; BAföG Höchstsatz;
  BAföG Satz; BAföG Rechner; BAföG berechnen; BAföG Voraussetzungen; BAföG Anspruch; BAföG Altersgrenze; BAföG Fachrichtungswechsel; BAföG Studiengangwechsel; BAföG Studienwechsel;
  BAföG Regelstudienzeit; BAföG Förderungshöchstdauer; BAföG Verlängerung; BAföG Weiterbewilligung; BAföG Weiterbewilligungsantrag; BAföG Folgeantrag; BAföG Bewilligungszeitraum;
  BAföG Bescheid; BAföG Bescheid prüfen; BAföG Bescheid Widerspruch; BAföG Widerspruch; BAföG Ablehnung; BAföG Rückzahlung; BAföG Darlehen; BAföG Darlehen Rückzahlung;
  BAföG Darlehen Erlass; BAföG Teilerlass; BAföG Schulden; BAföG Rückforderung; BAföG Überzahlung; BAföG Einkommen; BAföG Einkommen Eltern; BAföG Freibetrag; BAföG Nebenjob;
  BAföG Minijob; BAföG Vermögen; BAföG Vermögensfreibetrag; BAföG Auslandsstudium; BAföG Auslandssemester; BAföG Ausland; BAföG Auslands-BAföG; Auslands-BAföG; Auslands-BAföG beantragen;
  Auslandsamt BAföG; BAföG Amt für Ausbildungsförderung Ausland; BAföG Praktikum; BAföG Praktikumssemester; BAföG Masterstudium; BAföG Master; BAföG Bachelor; BAföG Promotion;
  BAföG Zweitstudium; BAföG Zweitausbildung; BAföG Abbruch; BAföG Studienabbruch; BAföG Urlaubssemester; BAföG Beurlaubung; BAföG Krankheit; BAföG Schwangerschaft; BAföG Kind;
  BAföG Kinderbetreuungszuschlag; BAföG Wohnzuschlag; BAföG Wohnkosten; BAföG Krankenversicherung Zuschlag; BAföG Pflegeversicherung Zuschlag; BAföG Studienstarthilfe; Studienstarthilfe;
  Studienstarthilfe beantragen; Studienstarthilfe 1000 Euro; Schüler-BAföG; Schüler BAföG; Schüler BAföG Antrag; Schüler BAföG Voraussetzungen; Schüler BAföG Berufsfachschule;
  Schüler BAföG Fachoberschule; Schüler BAföG Abendschule; Schüler BAföG Auswärts; BAföG Berufsschule; BAföG Fachschule; BAföG Fachoberschule; BAföG Fachhochschule; BAföG Universität;
  BAföG Hochschule; BAföG Duales Studium; BAföG Fernstudium; BAföG Teilzeitstudium; BAföG Online-Studium; Aufstiegs-BAföG; Aufstiegs-BAföG beantragen; Aufstiegs-BAföG Antrag;
  Aufstiegs-BAföG Meister; Aufstiegs-BAföG Fachwirt; Aufstiegs-BAföG Techniker; Aufstiegs-BAföG Erzieher; Aufstiegsfortbildungsförderungsgesetz; Meister-BAföG beantragen;
  Meister BAföG Antrag; Meister BAföG Höhe; Meister BAföG Zuschuss; Meister BAföG Darlehen; Meister BAföG Erlass; Meister BAföG Rückzahlung; Fortbildung BAföG; Aufstiegsfortbildung Förderung;
  Amt für Ausbildungsförderung; Amt für Ausbildungsförderung Termin; Amt für Ausbildungsförderung Kontakt; Amt für Ausbildungsförderung zuständig; BAföG Amt; BAföG Amt Termin;
  BAföG Amt Kontakt; BAföG Amt zuständig; BAföG Amt Studierendenwerk; Studierendenwerk BAföG; Studentenwerk BAföG; BAföG Beratung; BAföG Beratung Studierendenwerk;
  BAföG digital; BAföG Online; BAföG Online Antrag; bafög-digital; BAföG Nachweis; BAföG Leistungsnachweis; BAföG Leistungsnachweis Formblatt 5; Formblatt 5 BAföG; Formblatt 1 BAföG;
  Formblatt 3 BAföG; BAföG Bescheinigung; BAföG Bescheinigung Studium; BAföG Immatrikulationsbescheinigung; BAföG Mietvertrag; BAföG Kontoauszüge; BAföG Einkommensnachweis;
  BAföG Steuerbescheid Eltern; BAföG Eltern Steuerbescheid; BAföG Eltern Einkommen aktuell; BAföG Aktualisierungsantrag; BAföG Aktualisierung; BAföG Aktualisierungsantrag Eltern;
  BAföG Vorausleistung; BAföG Vorausleistung Antrag; BAföG Vorausleistung Eltern zahlen nicht; BAföG Eltern zahlen nicht; BAföG Eltern Unterhalt; BAföG Eltern Auskunft; BAföG Eltern verweigern;
  Ausbildungsförderung; Ausbildungsförderung Antrag; Ausbildungsförderung beantragen; Ausbildungsförderung Schüler; Ausbildungsförderung Student; Ausbildungsförderung Höhe;
  Ausbildungsförderung Ausland; Ausbildungsförderung Amt; Förderung Studium Staat; Studienfinanzierung Staat; Studium finanzieren Staat; Studium Förderung; Studienbeihilfe

schulamt:
  Schulamt; Schulamt Kontakt; Schulamt Termin; staatliches Schulamt; Schulaufsicht; Schulaufsicht Beschwerde; Schulbehörde; Schulpflicht; Schulpflicht befreien; Schulpflicht Befreiung;
  Schulpflicht Ausland; Schulpflicht Umzug; Schulpflicht Verletzung; Schulpflicht Bußgeld; Schulpflicht Verstoß; Schulpflichtverletzung; Schulverweigerung; Schulverweigerung Hilfe;
  Schulabsentismus; Schulwechsel; Schulwechsel beantragen; Schulwechsel Umzug; Schulwechsel Antrag; Schulwechsel Bundesland; Schulwechsel Genehmigung; Schulbezirk; Schulbezirk Befreiung;
  Schulbezirk Wechsel; Schulsprengel; Schulsprengel Wechsel; Schulplatz; Schulplatz Antrag; Schulplatz zuweisen; Schulplatz Zuweisung; Schulplatz Einspruch; Schulplatzvergabe; Schulanmeldung;
  Schulanmeldung Termin; Schulanmeldung Grundschule; Schulanmeldung Gymnasium; Schulanmeldung weiterführende Schule; Schulanmeldung Zuzug; Einschulung; Einschulung Antrag; Einschulung früher;
  vorzeitige Einschulung; Einschulung zurückstellen; Zurückstellung Schulpflicht; Zurückstellung Einschulung; Rückstellung Schule; Schulreife; Schulreifetest; Schulfähigkeit; Schuleingangsuntersuchung Schulamt;
  Sonderpädagogischer Förderbedarf; sonderpädagogischer Förderbedarf Antrag; Förderschule; Förderschule Antrag; Förderschule Aufnahme; Förderschule Inklusion; Inklusion Schule; Inklusion Schule Antrag;
  Nachteilsausgleich Schule; Nachteilsausgleich Schule Antrag; Nachteilsausgleich Prüfung; Legasthenie Schule; Legasthenie Nachteilsausgleich; Dyskalkulie Schule; Dyskalkulie Nachteilsausgleich;
  Hochbegabung Schule; Hochbegabung Förderung Schule; Klasse überspringen; Klasse überspringen Antrag; Klasse wiederholen; Klasse wiederholen freiwillig; Schulbefreiung; Schulbefreiung Antrag;
  Schulbefreiung Urlaub; Schulbefreiung Beurlaubung; Schulbefreiung Religion; Schulbefreiung Sport; Schulbefreiung Schwimmen; Beurlaubung Schule; Beurlaubung Schule Urlaub; Beurlaubung Kind Schule;
  Hausunterricht; Hausunterricht Antrag; Hausunterricht Krankheit; Hausunterricht Genehmigung; Homeschooling; Homeschooling Deutschland; Homeschooling Genehmigung; Homeschooling Antrag;
  Fernunterricht Schule; Fernschule; Fernschule Genehmigung; Privatschule; Privatschule Genehmigung; Privatschule gründen; Privatschule Anerkennung; Ersatzschule; Ersatzschule Genehmigung;
  Ersatzschule Anerkennung; Ergänzungsschule; Ergänzungsschule Anzeige; Schule gründen; Schule gründen Genehmigung; Schule eröffnen; Schule eröffnen Genehmigung; Freie Schule; Freie Schule Genehmigung;
  Waldorfschule Genehmigung; Montessori Schule Genehmigung; Schulgeld; Schulgeld Erlass; Schulgeld Zuschuss; Schulgeld Privatschule Steuer; Schülerbeförderung; Schülerbeförderung Antrag;
  Schülerbeförderung Kosten; Schülerbeförderung Erstattung; Schülerbeförderung Anspruch; Schülerbeförderung Schulbus; Schulbus; Schulbus Antrag; Schulbus Anspruch; Schulbus Fahrkarte; Schülerfahrkarte;
  Schülerfahrkarte Antrag; Schülerticket; Schülerticket beantragen; Schülerticket Zuschuss; Schülerticket Kosten; Schülerticket Erstattung; Fahrkostenerstattung Schule; Fahrkosten Schule;
  Fahrkosten Schule Erstattung; Schulwegkosten; Schulwegkosten Erstattung; Schulweg; Schulweg Beförderung; Schulweg zu weit; Schulweg unzumutbar; Schulbuch; Schulbuchausleihe; Schulbuchausleihe Antrag;
  Schulbuchausleihe Befreiung; Lernmittelfreiheit; Lernmittelfreiheit Antrag; Lernmittel Zuschuss; Lernmittel Befreiung; Schulbedarf; Schulbedarf Antrag;
  Zeugnis Schule; Zeugnis Duplikat Schule; Zeugnis verloren Schule; Zeugnis nachträglich Schule; Abschlusszeugnis Duplikat; Abschlusszeugnis verloren; Abiturzeugnis Duplikat; Abiturzeugnis verloren;
  Abitur Zeugnis Kopie; Abitur Zeugnis beglaubigen; Realschulabschluss nachholen; Hauptschulabschluss nachholen; Schulabschluss nachholen; Abitur nachholen; Abendschule; Abendgymnasium;
  Abendrealschule; Volkshochschule Schulabschluss; Externenprüfung; Externenprüfung Abitur; Externenprüfung Realschule; Externenprüfung Hauptschule; Nichtschülerprüfung; Schulabschluss Ausland anerkennen;
  Zeugnis Ausland anerkennen; Zeugnisanerkennung; Zeugnisanerkennung Schule; Zeugnisanerkennung Abitur; Zeugnis Anerkennung Ausland; Anerkennung Abitur Ausland; Anerkennung Schulabschluss Ausland;
  Anerkennung Schulzeugnis Ausland; Zeugnisbewertung Ausland; Zeugnisbewertung; Schulabschluss Bewertung Ausland; Lehrer Anerkennung Ausland; Lehramt Anerkennung Ausland; Lehrer werden; Lehrer werden Quereinstieg;
  Quereinstieg Lehrer; Lehramt Bewerbung; Lehramt Referendariat Bewerbung; Referendariat Bewerbung; Referendariat Antrag; Vorbereitungsdienst Lehramt; Lehrerstelle Bewerbung; Lehrerstelle;
  Vertretungslehrer; Vertretungslehrer werden; Vertretungslehrkraft; Seiteneinsteiger Lehrer; Schulassistenz; Schulsozialarbeit; Schulpsychologe; Schulpsychologische Beratung; Schulpsychologische Beratungsstelle;
  Schulberatung; Schulberatung Termin; Schulberatungsstelle; Schulentwicklung; Schulaufsicht Beschwerde Lehrer; Dienstaufsichtsbeschwerde Lehrer; Dienstaufsichtsbeschwerde Schule; Beschwerde Schule;
  Beschwerde Lehrer; Beschwerde Schulleiter; Beschwerde Schulleitung; Mobbing Schule; Mobbing Schule melden; Mobbing Schule Hilfe; Gewalt Schule melden; Gewalt Schule Hilfe; Schulausschluss; Schulausschluss Widerspruch;
  Schulverweis; Schulverweis Widerspruch; Ordnungsmaßnahme Schule; Ordnungsmaßnahme Schule Widerspruch; Widerspruch Schule; Widerspruch Zeugnis; Widerspruch Versetzung; Widerspruch Notenbescheid;
  Widerspruch Prüfungsergebnis Schule; Widerspruch Schulbescheid

kreisverwaltung:
  Landratsamt; Landratsamt Kontakt; Landratsamt Termin; Landratsamt Öffnungszeiten; Landratsamt zuständig; Landratsamt Adresse; Kreisverwaltung; Kreisverwaltung Kontakt; Kreisverwaltung Termin;
  Kreisverwaltung Öffnungszeiten; Kreisverwaltung zuständig; Kreisverwaltung Adresse; Kreisverwaltungsbehörde; Landkreis; Landkreis Kontakt; Landkreis zuständig; Kreis Verwaltung; Kreisamt;
  Kreishaus; Kreisausschuss; Landrat; Landrat sprechen; Landrat Termin; Jagdschein; Jagdschein beantragen; Jagdschein verlängern; Jagdschein verloren; Jagdschein Antrag; Jagdschein Prüfung;
  Jagdschein Prüfung Anmeldung; Jagdschein Gesundheit; Jagdschein Zuverlässigkeit; Jagdschein Haftpflicht; Jahresjagdschein; Jahresjagdschein beantragen; Tagesjagdschein; Jugendjagdschein; Jagdbehörde;
  Untere Jagdbehörde; Jagdpacht; Jagdpacht Anzeige; Jagdgenossenschaft; Jagdgenossenschaft Mitglied; Wildschaden; Wildschaden melden; Wildschaden Antrag; Wildschaden Ersatz; Wildschaden Landwirt;
  Wildschaden Garten; Wildschaden Wald; Abschussplan; Abschussplan Wild; Jagdwaffe Besitzkarte; Jagdwaffe erwerben; Waffenbesitzkarte Jäger; Waffenbehörde; Waffenbehörde Termin; Waffenbehörde Kontakt;
  Waffenbehörde zuständig; Waffenbehörde Anmeldung; Waffenbehörde Erbe; Waffe geerbt; Waffe geerbt anmelden; Waffe Erbe; Waffe vererben; Waffenerbschaft; Waffe abgeben; Waffe abgeben Behörde;
  Waffe finden; Waffe gefunden; Waffe verloren; Waffen Aufbewahrung; Waffenschrank; Waffenschrank Kontrolle; Waffenschrank Nachweis; Waffenkontrolle; Waffenkontrolle Hausbesuch; Waffenbesitzkarte beantragen;
  Waffenbesitzkarte Sportschütze; Waffenbesitzkarte Antrag; Waffenbesitzkarte Kosten; Waffenbesitzkarte Bedürfnis; Waffenbesitzkarte Sachkunde; Waffenbesitzkarte Zuverlässigkeit; Waffenbesitzkarte Gutachten;
  Waffenbesitzkarte Verlängerung; Waffenbesitzkarte Adresse ändern; Waffenbesitzkarte verloren; Waffenbesitzkarte Ersatz; Waffenbesitzkarte Eintrag; Waffenbesitzkarte Eintragung Waffe;
  Waffenbesitzkarte Widerruf; Waffenschein beantragen; Waffenschein Antrag; Waffenschein Kosten; Waffenschein Voraussetzungen; Waffenschein Bedürfnis; Waffenschein Sicherheitsgewerbe; kleiner Waffenschein beantragen;
  kleiner Waffenschein Kosten; kleiner Waffenschein Antrag; kleiner Waffenschein Reizgas; kleiner Waffenschein Pfefferspray; Pfefferspray Erlaubnis; Pfefferspray Tierabwehr; Schreckschusswaffe führen;
  Schreckschusswaffe Erlaubnis; Schreckschusswaffe Waffenschein; Schreckschusswaffe kaufen; Waffenhandel Erlaubnis Kreis; Waffensachkundeprüfung; Waffensachkundeprüfung Anmeldung; Sachkundeprüfung Waffen;
  Sprengstofferlaubnis; Sprengstofferlaubnis beantragen; Sprengstofferlaubnis Pyrotechnik; Sprengstofferlaubnis Kosten; Sprengstoffschein; Befähigungsschein; Befähigungsschein Pyrotechnik;
  Großfeuerwerk; Großfeuerwerk Genehmigung; Feuerwerk Anzeige; Feuerwerk Anzeige Kreis; Fischereierlaubnis; Fischereierlaubnis Kreis; Fischereischein beantragen;
  Fischereischein Prüfung; Fischereischein Jugend; Fischereischein verlängern; Fischereischein Ausländer; Fischerprüfung; Fischerprüfung Anmeldung; Angelschein beantragen; Angelschein Prüfung;
  Angelschein Kosten; Angelschein Jugendliche; Angelschein Urlaub; Angelschein Verlängerung; Angelerlaubnis; Angelerlaubnis Gewässer; Angelkarte; Angelkarte kaufen; Pachtgewässer Angeln; Kreisstraße;
  Kreisstraße Schaden; Kreisstraße Sperrung; Kreisstraße Baustelle; Kreisstraße Lärm; Kreisstraße Tempo; Kreisstraße Radweg; Kreisstraße Winterdienst; Kreisstraße Mängel; Kreisstraße Unfall;
  Kreisabfallwirtschaft; Abfallwirtschaft Kreis; Abfallwirtschaft Landkreis; Müllentsorgung Landkreis; Müllgebühren Landkreis; Müllabfuhr Landkreis; Mülldeponie Landkreis; Deponie; Deponie Anlieferung;
  Deponie Bauschutt; Deponie Preise; Deponie Öffnungszeiten; Wertstoffhof Landkreis; Kompostplatz; Kompostplatz Landkreis; Grüngutannahme; Grüngutannahme Landkreis; Sperrmüllabfuhr Landkreis;
  Schadstoffmobil; Schadstoffmobil Termin; Schadstoffsammlung; Schadstoffsammlung Termin; Rettungsdienst; Rettungsdienst Rechnung; Rettungsdienst Gebühren; Rettungsdienst Beschwerde; Rettungsdienst Kreis;
  Katastrophenschutz; Katastrophenschutz Landkreis; Katastrophenschutz Anmeldung; Katastrophenschutz Ehrenamt; Kreisbrandmeister; Kreisbrandinspektor; Kreisbrandinspektion; Feuerwehr Kreis;
  Kreisjugendamt; Kreisjugendamt Termin; Kreissozialamt; Kreisgesundheitsamt; Kreisveterinäramt; Kreisbauamt; Kreisbauamt Termin; Kreisbauamt Bauantrag; Kreisbauamt Genehmigung; Kreisplanungsamt

regierungsprs:
  Regierungspräsidium; Regierungspräsidium Kontakt; Regierungspräsidium Termin; Regierungspräsidium zuständig; Regierungspräsidium Beschwerde; Regierungspräsidium Widerspruch; Regierungspräsidium Genehmigung;
  Regierungspräsidium Antrag; Regierungspräsidium Anerkennung; Regierungspräsidium Förderung; Regierungspräsidium Aufsicht; Bezirksregierung; Bezirksregierung Kontakt; Bezirksregierung Termin;
  Bezirksregierung zuständig; Bezirksregierung Beschwerde; Bezirksregierung Widerspruch; Bezirksregierung Genehmigung; Bezirksregierung Antrag; Bezirksregierung Anerkennung; Bezirksregierung Förderung;
  Bezirksregierung Aufsicht; Landesverwaltungsamt; Landesverwaltungsamt Kontakt; Landesverwaltungsamt Termin; Landesverwaltungsamt zuständig; Landesverwaltungsamt Beschwerde; Landesverwaltungsamt Genehmigung;
  Landesdirektion; Landesdirektion Sachsen; Landesdirektion Kontakt; Landesdirektion zuständig; Landesdirektion Genehmigung; Aufsichts- und Dienstleistungsdirektion; ADD Trier; ADD Rheinland-Pfalz;
  Kommunalaufsicht; Kommunalaufsicht Beschwerde; Kommunalaufsicht Gemeinde; Rechtsaufsicht Gemeinde; Fachaufsicht; Fachaufsichtsbeschwerde; Dienstaufsichtsbeschwerde; Dienstaufsichtsbeschwerde Behörde;
  Dienstaufsichtsbeschwerde Beamter; Dienstaufsichtsbeschwerde Bürgermeister; Widerspruchsbehörde; Widerspruchsbehörde zuständig; Widerspruchsstelle; Planfeststellung; Planfeststellungsverfahren;
  Planfeststellungsbeschluss; Planfeststellung Einwendung; Planfeststellung Einwendung einreichen; Planfeststellung Auslegung; Raumordnungsverfahren; Raumordnung; Raumordnung Beteiligung; Genehmigungsbehörde Anlage;
  Anlagengenehmigung; Anlagengenehmigung Immissionsschutz; Anlagengenehmigung Windrad; Anlagengenehmigung Biogas; Anlagengenehmigung Tierhaltung; Anlagengenehmigung Industrie; Bergamt; Bergbehörde;
  Bergrecht; Bergbau Genehmigung; Bergschaden; Bergschaden melden; Bergschaden Antrag; Bergschaden Ersatz; Flurbereinigung; Flurbereinigung Antrag; Flurbereinigung Beteiligung; Förderung Landwirtschaft Regierungspräsidium;
  Agrarförderung; Agrarförderung Antrag; Agrarförderung Sammelantrag; Sammelantrag Landwirtschaft; Direktzahlungen Landwirtschaft; Landwirtschaft Förderung Antrag; Landwirtschaft Förderung Land;
  Tierärztekammer Aufsicht; Apothekenaufsicht; Apothekenaufsicht Genehmigung; Apothekenaufsicht Betriebserlaubnis; Apotheke eröffnen Genehmigung; Apotheke eröffnen Erlaubnis; Betriebserlaubnis Apotheke;
  Approbation; Approbation Arzt; Approbation Arzt beantragen; Approbation Arzt Ausland; Approbation Arzt Anerkennung; Approbation Zahnarzt; Approbation Apotheker; Approbation Psychotherapeut;
  Approbation Tierarzt; Berufserlaubnis Arzt; Berufserlaubnis Arzt Ausland; Berufserlaubnis Pflegekraft; Berufserlaubnis Pflege Anerkennung; Anerkennung Pflegekraft Ausland; Anerkennung Arzt Ausland;
  Anerkennung Ärzte Ausland; Anerkennung Apotheker Ausland; Anerkennung Zahnarzt Ausland; Anerkennung Lehrer Ausland Regierungspräsidium; Anerkennung Ingenieur Ausland; Anerkennung Architekt Ausland;
  Anerkennung Berufsabschluss reglementierte Berufe; reglementierter Beruf Anerkennung; Berufsanerkennung reglementierte Berufe; Heilberufe Anerkennung; Heilberufe Erlaubnis; Pflegeberufe Anerkennung;
  Altenpfleger Anerkennung; Krankenpfleger Anerkennung Ausland; Gesundheitsberufe Anerkennung; Gesundheitsfachberufe Erlaubnis; Heilpraktiker Überprüfung; Heilpraktikerüberprüfung; Heilpraktiker Erlaubnis Regierungspräsidium

bundesverwaltungsamt:
  Bundesverwaltungsamt; BVA; BVA Kontakt; BVA Köln; Bundesverwaltungsamt Köln; Bundesverwaltungsamt Kontakt; Bundesverwaltungsamt Antrag; Bundesverwaltungsamt Bescheid; Bundesverwaltungsamt Widerspruch;
  Bundesverwaltungsamt Beihilfe; Bundesverwaltungsamt Bundesbeamte; Bundesverwaltungsamt Auslandsschulwesen; Auslandsschulwesen; Auslandsschule; Auslandsschule Lehrer; Auslandsschule Bewerbung; Auslandsdienstlehrkraft;
  Auslandsdienstlehrkraft Bewerbung; ZfA; Zentralstelle für das Auslandsschulwesen; Auslandsschule Förderung; Spätaussiedler; Spätaussiedler Aufnahme; Spätaussiedler Antrag; Spätaussiedler Aufnahmebescheid;
  Spätaussiedler Aufnahmeverfahren; Aufnahmebescheid Spätaussiedler; Aufnahmebescheid Aussiedler; Aussiedler Antrag; Aussiedler Bescheinigung; Aussiedler Bescheinigung nach 15 BVFG; Bescheinigung nach § 15 BVFG;
  Spätaussiedlerbescheinigung; Spätaussiedlerbescheinigung beantragen; Spätaussiedler Staatsangehörigkeit; Spätaussiedler Statusfeststellung; Spätaussiedler Deutschtest; Spätaussiedler Sprachtest;
  Spätaussiedler Einreise; Spätaussiedler Familienangehörige; Spätaussiedler Härtefall; Bundesausgleichsamt; Lastenausgleich; Lastenausgleich Antrag; Vertriebene Ausweis; Vertriebenenausweis;
  Vertriebenenausweis beantragen; Auslandsdeutsche Antrag; Auslandsdeutsche Beratung; Auslandsdeutsche Hilfe; Deutsche im Ausland Hilfe; Deutsche im Ausland Notlage; Rückkehrer Ausland Hilfe;
  Rückführung Deutsche Ausland; Vereinsrecht Bund; Vereinsverbot; Bundeszentralamt für Steuern; Bundesamt für Justiz; Bundesamt für Justiz Führungszeugnis; Bundeszentralregister; Bundeszentralregister Auskunft;
  Bundeszentralregister Antrag; Bundeszentralregister Auszug; Bundeszentralregister Tilgung; Strafregister; Strafregisterauszug; Strafregisterauszug Ausland; Auszug Strafregister; Auszug Zentralregister;
  Europäische Dienstleistungsrichtlinie; Einheitlicher Ansprechpartner; Einheitlicher Ansprechpartner Gründung; Einheitlicher Ansprechpartner Dienstleister; Behördenwegweiser; Behördennummer 115; 115 Behördennummer;
  Bürgertelefon; Bürgertelefon Behörden; Bürgertelefon Auskunft; Bürgertelefon Bund; Bürgerservice Bund; Bürgerservice Bundesverwaltungsamt; Service-Center Bundesverwaltungsamt; Auslandsvertretung; Auslandsvertretung Hilfe
"""


FAELLE_EINZELWOERTER = """
finanzamt:
  Lohnsteuer; Steuererklärung; Steuernummer; ELStAM; Schenkung; geschenkt; Steuerklasse; Steuerbescheid; Steuer-ID; Elster
ordnungsamt:
  Lärmbelästigung; Ruhestörung; Lärm Nachbarn
buergeramt:
  Personalausweis; Reisepass; ummelden; Ummeldung; Wohnsitz; Führungszeugnis; Meldebescheinigung
auslaenderbehoerde:
  Aufenthaltserlaubnis; Blaue Karte; Blaue Karte EU; Staatsangehörigkeit; Einbürgerung; Niederlassungserlaubnis
bundesamt_migration:
  Flüchtling; Asylantrag; Integrationskurs
amtsgericht:
  Ehescheidung; Ehescheidung einreichen; Scheidungsverfahren; Ausschlagung; Erbausschlagung; Insolvenz; Privatinsolvenz; Erbschein; Nachlassgericht; Grundbuch; Mahnbescheid
polizei:
  Polizei; gestohlen; Überfall; Diebstahl; Einbruch; Anzeige erstatten; Strafanzeige
jugendamt:
  Sorgerecht; Pflegekind; Vaterschaft; Kindeswohl; Kindeswohlgefährdung; Unterhaltsvorschuss; Adoption
jobcenter:
  Hartz IV; Hartz; Bürgergeld; Arbeitslosengeld II
arbeitsagentur:
  arbeitslos; arbeitssuchend; arbeitsuchend; Arbeitslosengeld; Kurzarbeitergeld; Insolvenzgeld
bauamt:
  Bauvorhaben; Bauantrag; Baugenehmigung; Bauvoranfrage
strassenverkehrsbehoerde:
  Fahreignung; Bewohnerparken; Bewohnerparkausweis; Führerschein; Fahrerlaubnis
zulassungsstelle:
  Kfz; Kraftfahrzeug; Kennzeichen; Zulassungsbescheinigung
veterinaeramt:
  Veterinär; Veterinäramt; Tierseuche; Tierschutz
umweltamt:
  Gewässer; Grundwasser; Naturschutz; Artenschutz; Baumschutz
ihk:
  Fachkundeprüfung; Sachkundeprüfung; Ursprungszeugnis
krankenkasse:
  Mitgliedsbescheinigung; Krankengeld; Gesundheitskarte; Krankenversicherung
bundesverwaltungsamt:
  Aussiedler; Spätaussiedler; Auslandsschulwesen
"""


FAELLE_NACHSCHAERFUNG = """
finanzamt:
  Fragebogen steuerliche Erfassung; steuerliche Anmeldung Gewerbe; steuerliche Anmeldung nach Gewerbeanmeldung; Photovoltaik Finanzamt anmelden; Photovoltaikanlage Finanzamt melden;
  Solaranlage Finanzamt anmelden
stadtverwaltung:
  Hund umgezogen; Hund ummelden; Hund Wohnort ändern; Hund Wohnort ändern Steuer; Hundehalter Adressänderung
ordnungsamt:
  Feuerwerk anmelden; Großfeuerwerk anmelden; Schrottauto entfernen; Schrottauto Grundstück; Schrottauto Grundstück entfernen lassen; Schrottfahrzeug entfernen; Hundeführerschein
buergeramt:
  Online-Ausweisfunktion freischalten; Online Ausweis Funktion; Online-Ausweis Funktion freischalten; eID freischalten; Ausweis Kinder; Ausweis für Kinder unter 16; Personalausweis Kind
auslaenderbehoerde:
  Beschäftigungserlaubnis; Beschäftigungserlaubnis Asylbewerber; Fiktionsbescheinigung; Wohnsitzauflage; Wohnsitzauflage ändern; Wohnsitzauflage aufheben
amtsgericht:
  Räumungsklage; Räumungsklage Mieter; Todeserklärung; Todeserklärung vermisst; Todeserklärung Verschollene
landgericht:
  Berufung gegen Urteil Amtsgericht; Berufung gegen Urteil Amtsgericht einlegen; Beschwerde gegen Beschluss Amtsgericht; Berufung Zivilurteil
verwaltungsgericht:
  Klage gegen Baugenehmigung; Klage gegen Führerscheinentzug; Klage gegen Gewerbeuntersagung; Klage gegen Lärmschutz; Klage gegen Schulverweis; Eilantrag Abschiebung;
  Eilantrag gegen Abschiebung; Rechtsmittel abgelehnte Aufenthaltserlaubnis; Normenkontrolle Bebauungsplan; Klage gegen Bescheid; Klage gegen Behörde; Klage gegen Genehmigung;
  Klage gegen Ablehnung; Klage gegen Entzug; Klage gegen Untersagung; Klage gegen Widerspruchsbescheid; Klage gegen Stadt; Klage gegen Gemeinde; Klage gegen Landratsamt
polizei:
  Autoaufbruch; Kfz Aufbruch; Kfz Aufbruch Zeugen; Autoaufbruch anzeigen; Fahrzeugaufbruch
zoll:
  Zollstrafverfahren; Zollfahndung
sozialamt:
  Pflegewohngeld; Barbetrag Pflegeheim; Taschengeld Pflegeheim; Barbetrag Pflegeheim Taschengeld
jobcenter:
  Weiterbildung Bürgergeld; Weiterbildung Bürgergeld Förderung; Bildung und Teilhabe Jobcenter; Leistungen Bildung Teilhabe Jobcenter
zulassungsstelle:
  Gebrauchtwagen ummelden; Auto ummelden; Fahrzeug ummelden; Kfz ummelden; Motorrad ummelden
bauamt:
  Flächennutzungsplan; Flächennutzungsplan Auskunft; Erschließungsbeitrag; Erschließungsbeitrag Bescheid
veterinaeramt:
  Tiergesundheitszeugnis; Tiergesundheitszeugnis Reise mit Hund; Wesenstest Hund
ihk:
  Abschlusszeugnis IHK; IHK Abschlusszeugnis; IHK Abschlusszeugnis verloren; Abschlusszeugnis IHK verloren
handwerkskammer:
  Kfz-Werkstatt eröffnen; Werkstatt eröffnen Meister; Kfz-Werkstatt eröffnen Meister; Friseursalon eröffnen Meister
bundesverwaltungsamt:
  Beihilfe Bundesbeamte; Bundesbeamte Beihilfe Antrag; Beihilfe Bundesbeamte Antrag
"""


FAELLE_LUECKEN = """
finanzamt:
  Gewerbesteuer Bescheid; Gewerbesteuer Einspruch; Grundsteuer Wert Erklärung; Grundsteuerwert; Freistellungsbescheinigung; Freistellungsbescheinigung 48b; Anlage N; Anlage V; Anlage KAP;
  Wohnung vermietet versteuern; Mieteinnahmen versteuern; Mieteinnahmen Steuer; Einkünfte aus Vermietung; Steuer auf Mieteinnahmen; Steuerpflicht; Steuererklärung Pflicht; Steuererklärung Frist;
  Steuererklärung Rentner; Steuererklärung Student; Steuern nachzahlen; Einspruch Steuer; Steuerbescheid Einspruch
stadtverwaltung:
  Welpe registrieren; Welpe anmelden; Hund registrieren; Müllsäcke; Müllsack; Gelbe Säcke; Gelber Sack; Sperrmüll Termin; Sperrmüllabholung; Mülltonne reklamieren; Mülltonne nicht geleert;
  Abfuhr verpasst; Müll nicht abgeholt; Marktstand; Marktstand beantragen; Wochenmarkt Standplatz; Weihnachtsmarkt Standplatz; Weihnachtsmarkt bewerben; Veranstaltung öffentliche Fläche;
  Veranstaltung öffentlicher Raum; Stadtbibliothek Ausweis; Bücherei Ausweis; Straßenlaterne defekt; Straßenlaterne kaputt; Laterne defekt melden; Baum umgestürzt Gehweg; Baum auf der Straße;
  Beschwerde an die Gemeinde; Beschwerde Stadt; Anregung Beschwerde Gemeinde; Kanalgebühren; Kanalgebühren Bescheid; Abwassergebühren; Abwassergebühren Widerspruch; Straßenreinigung Gebühren;
  Mängelmelder; Mängel melden Stadt; Schlagloch melden; Wasserzähler ablesen; Wassergebühren; Gemeindesteuer
kreisverwaltung:
  Wildunfall Bescheinigung; Wildunfall melden; Jägerprüfung; Jägerprüfung anmelden; Jäger werden; Jagdprüfung; Jagdprüfung anmelden; Kreistag; Kreistag Sitzung; Kreistag Beschluss; Sportschütze Waffe;
  Sportschützen Bedürfnis; Bedürfnisprüfung Waffe; Schießsport Waffenbesitzkarte
regierungsprs:
  Einwendung Autobahn; Einwendung Straßenbau; Einwendung Bahnstrecke; Einwendung Planfeststellung; Berufserlaubnis Ärztin; Berufserlaubnis Arzt; Anerkennung Medizinstudium; Anerkennung Medizinstudium Ausland;
  Anerkennung Pflegefachkraft; Anerkennung Pflegefachkraft Ausland; Anerkennung Krankenschwester Ausland; Anerkennung Ärzte; Direktzahlungen; Direktzahlungen Landwirt; Landwirt Antrag Förderung; Betriebsprämie
ordnungsamt:
  Falschparker; Auto Einfahrt zugeparkt; Einfahrt zugeparkt; Auto blockiert Einfahrt; Knöllchen; Knöllchen Widerspruch; Bußgeldbescheid; Bußgeldbescheid Einspruch; Verwarnung Parken; Parkverstoß;
  Bellender Hund; Hund bellt Nachbar; Nachbarhund bellt; Hund gebissen; Hundebiss; Hundebiss melden; Wilde Müllablagerung; Müllkippe Wald; illegale Müllkippe; Müll illegal entsorgt;
  Gestattung; Gestattung Fest; Gestattung Vereinsfest; Schankgestattung; Verkehrsschild beschädigt; Verkehrsschild umgefahren; Halteverbotszone; Umzug Halteverbot;
  Betrunkene Gewahrsam; Baum droht umzustürzen; Gefahr durch Baum; Unterbringung Obdachlose
buergeramt:
  Beglaubigung Vollmacht; Vollmacht beglaubigen; Unterschriftsbeglaubigung; Beglaubigung Urkunde; Kopie beglaubigen
standesamt:
  Aufgebot; Aufgebot bestellen; Namen des Kindes; Name des Kindes ändern; Vorname Kind ändern; Vater gestorben; Mutter gestorben; Angehöriger gestorben; Todesfall Behörde; Stammbuch;
  Familienstammbuch; Stammbuch Neuausstellung; Nachbeurkundung; Nachbeurkundung Geburt Ausland; Urkunde Ausland nachbeurkunden; Auslandsgeburt beurkunden; Hochzeit im Garten; Trauung im Freien;
  Trauung außerhalb Standesamt
auslaenderbehoerde:
  Ehefrau nachholen; Frau soll nachkommen; Ehemann nachkommen; Familie nachholen; Kinder nachholen; Deutscher werden; Deutscher Pass; Deutschen Pass beantragen; Visum Besuch; Besuchsvisum;
  Visum Mutter Besuch; Visum Eltern Besuch; Aufenthaltsstatus; Aufenthaltsstatus ändern; Aufenthaltsgenehmigung Ehegatte; Aufenthaltsgenehmigung Ehegatten; Aufenthaltserlaubnis Ehegatte
bundesamt_migration:
  Berufsbezogene Deutschförderung; Deutschförderung; Sprachkurs Migration; Sprachkurs Zuschuss; Schutzstatus; Schutzstatus verlängern; Familienasyl; Rückkehrberatung; freiwillige Rückkehr;
  Integrationskurs Befreiung; Integrationskurs Härtefall; Integrationskurs Kosten; Aufnahme Ortskräfte; Ortskräfte Afghanistan; Aufnahmeprogramm
amtsgericht:
  Gewaltschutz; Gewaltschutzantrag; Kontaktverbot; Kontaktverbot Ex; Annäherungsverbot; Näherungsverbot; Wohnungseigentum Klage; WEG Klage; Mietrechtsstreit; Mietklage; Nachbarschaftsstreit Klage;
  Vermisste Person Todeserklärung; Verschollenheit; Kleine Klage Amtsgericht; Zivilklage; Klage bis 5000 Euro
landgericht:
  Klage hoher Streitwert; Klage über 10000 Euro; Klage über 5000 Euro; Streitwert über 5000 Euro; Schadensersatzklage; Schadensersatzklage hoher Streitwert; Revision Strafsache; Revision Strafverfahren;
  Schwurgericht; Schwurgericht Prozess; Prozess Zuschauer; Handelssache Klage; Kammer für Handelssachen
verwaltungsgericht:
  Widerspruchsbescheid angefochten; Widerspruchsbescheid Klage; Klage Rundfunkbeitrag; Klage gegen Rundfunkbeitrag; Rundfunkbeitrag Klage; Beamtenrecht Klage; Klage Versetzung; Beamte Klage Dienstherr;
  Anfechtungsklage; Verpflichtungsklage; Untätigkeitsklage; einstweiliger Rechtsschutz Behörde
staatsanwaltschaft:
  Strafanzeige Staatsanwaltschaft; Strafanzeige bei der Staatsanwaltschaft; Anzeige Staatsanwaltschaft; Einstellung des Verfahrens; Einstellung Verfahren Beschwerde; Einstellung Ermittlungsverfahren;
  Wirtschaftsstrafsachen; Wirtschaftsstrafsachen Anzeige; Verfahrenseinstellung; Verfahrenseinstellung Auflage; Korruption; Korruption melden; Hinweis Korruption; Akteneinsicht Geschädigter;
  Akteneinsicht Opfer Strafverfahren
polizei:
  bestohlen; Ich wurde bestohlen; Kind verschwunden; Kind vermisst; Kind wird vermisst; Cybercrime; Cyberkriminalität; Hackerangriff Anzeige; Konto gehackt Anzeige; Hacker Konto melden
zoll:
  Einfuhrumsatzsteuer Paket; Freigrenze Urlaub; Mitbringsel Urlaub; Mitbringsel Freigrenze; Reisefreimenge; Bargeld Ausreise; Bargeld Ausreise anmelden; Bargeld mitnehmen; Bargeld Grenze;
  Bargeld über 10000 Euro; Barmittel Anmeldung
jugendamt:
  Ex zahlt keinen Unterhalt; Unterhalt Kind nicht gezahlt; Kindesunterhalt; Kindesunterhalt Beistand; Kindesmisshandlung; Verdacht Kindesmisshandlung; Kind misshandelt; Kind vernachlässigt Nachbar;
  Kinderbetreuung; Kinderbetreuung Platz; Betreuungsplatz Kind; Krippenplatz; Krippenplatz Antrag; Kindertagespflege Erlaubnis; Tagesmutter Erlaubnis; Eingliederungshilfe Kind; Seelische Behinderung Kind Hilfe;
  Ferienbetreuung; Ferienbetreuung Zuschuss; Umgangsregelung; Umgangsregelung Beratung; Elterngeld; Elterngeld Antrag; Elterngeld Plus; Elterngeld Höhe; Elternzeit; Elternzeit Antrag; Elterngeldstelle
sozialamt:
  Schulbedarfspaket; Schulbedarfspaket beantragen; Behindertenausweis; Behindertenausweis Antrag; Schwerbehindertenausweis; Schwerbehindertenausweis beantragen; Schwerbehinderung; Schwerbehinderung Antrag;
  Grad der Behinderung; Grad der Behinderung Antrag; GdB Antrag; Merkzeichen; Merkzeichen aG; Merkzeichen beantragen; Wohnungslos; Wohnungslosigkeit; Notunterkunft; Wohnungslos Notunterkunft;
  Hilfe in besonderen Lebenslagen; Fahrdienst Behinderte; Fahrdienst für Behinderte; Behindertenfahrdienst
arbeitsagentur:
  Existenzgründungszuschuss; Gründungszuschuss; Mobilitätshilfe; Bewerbungskosten; Bewerbungskosten Erstattung; Qualifizierung Beschäftigte; Qualifizierungschancengesetz; Weiterbildung Förderung Arbeitnehmer
strassenverkehrsbehoerde:
  Straßensperrung; Straßensperrung beantragen; Straßensperrung Baustelle; Straßenabsperrung; Verkehrsunterricht; Verkehrsunterricht Anmeldung; Fahrtenbuch; Fahrtenbuch Anordnung; Fahrtenbuchauflage
zulassungsstelle:
  Fahrzeug umschreiben; Fahrzeug umschreiben nach Kauf; Fahrzeug nach Kauf umschreiben; Auto umschreiben; HU Plakette; HU Plakette Termin; Plakette Kennzeichen
bauamt:
  Haus bauen; Hausbau; Haus bauen Genehmigung; Anbau; Anbau Genehmigung; Anbau Haus; Wintergarten; Wintergarten Genehmigung; Carport; Carport Genehmigung; Carport bauen; Zaun Genehmigung; Zaun Höhe;
  Dachausbau; Dachausbau Genehmigung; Nachbar baut illegal; Nachbar baut ohne Genehmigung; illegaler Bau; Balkonkraftwerk Genehmigung; Solaranlage Genehmigung; Solaranlage Balkon; Wärmepumpe Genehmigung;
  Wärmepumpe Aufstellung; Bauschein; Bauschein Kopie; Bauakte; Bauakte Einsicht; Hausnummer Änderung; Straßenname Änderung; Wohnungsbauförderung; Wohnbauförderung; Aufzug einbauen; Aufzug Genehmigung
gesundheitsamt:
  Impfausweis; Impfberatung; Reiseimpfung Beratung; Legionellen; Legionellen Prüfung; Trinkwasser Prüfung; Meldepflicht; Meldepflicht Infektionskrankheit; Infektionskrankheit melden;
  Zahngesundheit; Zahngesundheit Untersuchung; Zahnärztlicher Dienst; Rattenbefall; Ratten melden; Schädlinge melden; Leichenschein; Leichenschau
veterinaeramt:
  Lebensmittelbetrieb Registrierung; Lebensmittelunternehmer Registrierung; Nachbar vernachlässigt Hund; Hund vernachlässigt; Tier vernachlässigt; Tierhaltungsverbot; Tierhaltung verbieten; Pferd registrieren;
  Pferd anmelden; Pferdepass; Pferdepass Beantragung; Bienenvolk; Bienen anmelden; Imker anmelden; Tierpension; Tierpension Genehmigung; Hundepension Genehmigung
umweltamt:
  Öl im Bach; Öl im Gewässer; Gewässerverschmutzung; Gartenbrunnen; Brunnen anmelden; Kaminofen; Kaminofen Anzeige; Kaminofen Messung; Feinstaub Kaminofen; Luftreinhalteplan; Umweltzone;
  Umweltzone Plakette; illegale Lagerung Abfall; Abfall Lagerung melden; Fällgenehmigung; Fällgenehmigung Hecke; Hecke schneiden Vogelschutz; Lichtverschmutzung; Beleuchtung Beschwerde
gewerbeamt:
  Etsy Gewerbe; Etsy verkaufen; eBay Gewerbe; Ebay Gewerbe anmelden; Online verkaufen Gewerbe; Straßenhandel; Straßenhandel Genehmigung; Bewachungsgewerbe; Bewachungsgewerbe Erlaubnis; 34a Erlaubnis;
  Handwerk Gewerbe eintragen; Betriebsübergabe Gewerbe; Gewerbe übergeben; Taxigewerbe; Taxigewerbe Anmeldung; Taxi Gewerbe anmelden
ihk:
  Exportdokumente beglaubigen lassen; Exportpapiere beglaubigen; Handelsrechnung Beglaubigung
handwerkskammer:
  Selbstständig als Friseur; Friseur selbstständig Meister; Friseursalon eröffnen; Selbstständig Handwerk Meister
krankenkasse:
  Auslandskrankenschein; Krankenschein Ausland; Krankenversicherung Urlaub Ausland
pflegekasse:
  Pflegebett Kostenübernahme; Pflegebett Zuschuss
bafoegamt:
  Studium finanzieren Staat; Studium Förderung staatlich; Studiengebühren Förderung
"""


FAELLE_WORTSCHATZ = """
finanzamt:
  Steuer; Steuern; Steuererklärung machen; Einkommensteuer; Umsatzsteuer; Erbschaftsteuer; Schenkungsteuer; Grundsteuer Befreiung; Nebeneinkünfte; Nebenverdienst angeben; Einkünfte angeben; Einkommen versteuern;
  Steuererlass; Steuerliche Identifikationsnummer; Steuer Freiberufler; Freiberufliche Tätigkeit anmelden; Kryptowährung Steuer; Bitcoin Steuer; Steuerrückerstattung; Werbungskosten; Sonderausgaben;
  außergewöhnliche Belastungen; Handwerkerleistungen absetzen; haushaltsnahe Dienstleistungen Steuer; Steuerabzug; Steuerzahler; Finanzbeamter
stadtverwaltung:
  Biotonne; Restmülltonne; Papiertonne; Mülltonne; Sperrmüll; Müllabfuhr; Müllgebühren; Abfallgebühren; Straßenbeleuchtung; Straßenlaterne; Grabplatz; Grabstätte; Grabnutzung; Friedhof; Stadtrat; Gemeinderat;
  Stadtratssitzung; Bürgerbegehren; Bürgerentscheid; Einwohnerantrag; Flohmarkt; Flohmarkt Stand; Trödelmarkt; Jahrmarkt; Kirmes; Volksfest; Spielplatz; Stadtbücherei; Stadtarchiv; Stadtwerke; Bürgermeister;
  Oberbürgermeister; Hundesteuer; Zweitwohnungsteuer; Vergnügungsteuer; Gemeindesatzung; Stadtreinigung
kreisverwaltung:
  Pistole geerbt; Waffe geerbt; Schusswaffe geerbt; Schießstand; Schießstand Genehmigung; Schießanlage; Schützenverein Waffe; Waffenbesitzkarte ändern; Jagd; Jäger; Jagdwaffe; Fischerei; Angeln Erlaubnis;
  Landkreis Beschwerde; Kreisentwicklung
ordnungsamt:
  Parken Gehweg; Gehweg zugeparkt; auf dem Gehweg geparkt; Parkticket; Parkticket Einspruch; Strafzettel; Strafzettel Einspruch; Strafzettel Widerspruch; Ordnungswidrigkeit; Ordnungswidrigkeitenverfahren;
  Anhörungsbogen; Lärm nachts; nächtliche Ruhestörung; laute Musik Nachbar; Party Nachbarn laut; Feiern Lärm; Sperrstunde; Sperrzeit Gaststätte; Rauchmelder; Rauchmelderpflicht; Taubenplage; Tauben füttern;
  Schädlinge Stadt; Nachbarschaftsstreit Ordnungsamt; Hecke Nachbar Überwuchs; Grundstück verwahrlost; Kehrwoche; Räumpflicht; Streupflicht; Winterdienst Pflicht; Schneeräumen Beschwerde; Fahrrad herrenlos; Fahrradwrack
buergeramt:
  Perso; Perso ablaufen; Ausweis ablaufen; Ausweis abgelaufen; Reisepass abgelaufen; Personalausweis läuft ab; Umzug melden; Umzug anmelden; Umzug Meldung; Umzug Behörde; Neuer Wohnort anmelden;
  weggezogen abmelden; Abmeldung; abmelden Wohnsitz; Meldebescheinigung Finanzamt; Meldebescheinigung beantragen online; Anmeldebestätigung; Wohnungsgeberbestätigung; Ausweisdokument; Ausweisdokumente; Pass; Reisedokument;
  Vollmacht beglaubigen lassen; Kopien beglaubigen; amtliche Beglaubigung; Beglaubigung Zeugnis Uni
standesamt:
  Geburtenregister; Geburtenregister Abschrift; Abschrift Geburtenregister; beglaubigte Abschrift Geburtsregister; Heiratsregister; Heiratsregister Auszug; Sterberegister; Sterberegister Auszug;
  Personenstandsregister; Personenstandsregister Auszug; Geburt melden; Tochter geboren; Sohn geboren; Kind geboren melden; Kind angemeldet Geburt; Neugeborenes anmelden; Baby anmelden; Partnerschaft Ehe umwandeln;
  Lebenspartnerschaft umwandeln; Geburtsname Kind; Geburtsname ändern; Familienname Kind; Familienname ändern; Namensänderung Kind; Ehe schließen; Ehe eingehen; Hochzeit; Hochzeit anmelden; heiraten Juni;
  standesamtlich heiraten; Verlobung; Trauung; Sterbefall; Sterbefall melden; verstorben; Sterbeurkunde Oma; Oma gestorben; Opa gestorben
auslaenderbehoerde:
  Visum abgelaufen; Visum Ablauf; Aufenthaltstitel läuft ab; Aufenthaltserlaubnis läuft ab; Aufenthaltserlaubnis abgelaufen; Ehefrau nach Deutschland holen; Ehemann nach Deutschland holen; Ehegatte nachholen;
  Familie nach Deutschland holen; Kinder nach Deutschland holen; Eltern nach Deutschland holen; Arbeitserlaubnis; Arbeitserlaubnis Student; Arbeitsgenehmigung Ausländer; Studentenvisum; Studienvisum; Visum Studium;
  Schengenvisum; Touristenvisum; Arbeitsvisum; Arbeitsvisum Deutschland; Einreise Deutschland Visum; Reiseausweis Ausländer; Flüchtlingspass; Passersatz
bundesamt_migration:
  Deutschkurs Zuwanderer; Deutschkurs Migranten; Deutschkurs Ausländer; Sprachkurs Zuwanderer; Sprachkurs Ausländer; Sprachkurs Flüchtlinge; Asyl; Asylverfahren; Asylverfahren Ablauf; Asylantrag abgelehnt;
  Asylantrag Ablehnung; Asylbewerber; Asylsuchende; Schutzsuchende; Geflüchtete; Geflüchtete Hilfe; Integration Kurs
amtsgericht:
  Erbschaft ausschlagen; Erbschaft Ausschlagung; Erbe ausschlagen; Erbschaft; Erbe; Erben; Testament; Nachlass; Pflichtteil; Verein gründen; Vereinsgründung; Verein eintragen; Vereinsregister Anmeldung;
  Miete nicht gezahlt Klage; Mietschulden Klage; Mietrückstand Klage; Mieter zahlt nicht; Zwangsvollstreckung; Gerichtsvollzieher; Eidesstattliche Versicherung; Vermögensauskunft; Beratungshilfe; Beratungsschein;
  Scheidung Haus; Zugewinnausgleich; Trennung Unterhalt Klage
landgericht:
  Streitwert über 5000 Euro Klage; Streitwert 20000 Euro; Klage 20000 Euro; Klage über 20000 Euro; Klage 50000 Euro; Berufungsgericht; Berufungsverfahren Zivil
verwaltungsgericht:
  Klage gegen Ordnungsverfügung; Ordnungsverfügung Klage; Klage gegen Studienplatz Ablehnung; Klage gegen Ablehnung Studienplatz; Klage gegen Bescheid der Gemeinde; Klage gegen Stadt Bescheid; Verwaltungsrechtsstreit; Verwaltungsstreitverfahren
staatsanwaltschaft:
  Verleumdung anzeigen; Nötigung anzeigen; Sexualdelikt Anzeige; Vorladung Zeuge; Vorladung Staatsanwaltschaft; Zeugenvorladung;
  Verfahren eingestellt; Verfahren eingestellt Beschwerde; Einstellungsbescheid; Einstellungsbescheid Beschwerde; Strafbefehl; Strafbefehl Einspruch; Strafverfahren; Ermittlungsverfahren eingeleitet
polizei:
  Auto zerkratzt; Auto beschädigt Anzeige; Auto zerkratzt Anzeige; Lackschaden Vandalismus; Vandalismus; Handy geklaut; Handy geklaut Zug; Fahrrad geklaut; Portemonnaie gestohlen; Taschendiebstahl; Taschendiebstahl Anzeige;
  eingebrochen; Einbruch Wohnung; Einbruch Haus; Einbruch Keller; Einbruch Auto; überfallen; Überfall melden; Raub; Raub Anzeige; Körperverletzung Polizei; Bedrohung Polizei; Nachbar bedroht; Verkehrsunfall Fahrerflucht;
  Unfallflucht; Fahrerflucht
zoll:
  Verzollung; Verzollung Paket; Verzollung Paket Amerika; Zollgebühren; Zollgebühren Paket; Einfuhrzoll; Einfuhrzoll Paket; Zoll Paket USA; Zoll Paket Großbritannien; Zoll Zigaretten Polen; Zigaretten mitgebracht;
  Zigaretten Zoll; Alkohol Zoll; Alkohol mitgebracht; Waren ausführen Großbritannien; Export Großbritannien Zoll; Nachbar arbeitet schwarz; schwarz arbeiten melden; Schwarzarbeiter melden; Kfz-Steuer Lastschrift
jugendamt:
  Pflegemutter; Pflegemutter werden; Pflegevater; Pflegefamilie; Pflegefamilie werden; Pflegeeltern; Kind adoptieren; Adoptivkind; Wohl eines Kindes; Kindeswohl Sorge; Sorge um ein Kind; Kind Nachbarschaft gefährdet;
  Kind schlägt Eltern Nachbarn; Kind allein zu Hause Nachbar; Unterhalt Kind Ex; Unterhaltszahlung Kind; Elterngeld nach der Geburt; Elterngeld Geburt; Betreuungsplatz; Kita; Kita Platz; Kindergartenplatz;
  Kindergartenplatz Antrag; Kinderkrippe
sozialamt:
  kleine Rente Unterstützung; Rente zu niedrig; Rente zu gering; Altersarmut; Altersarmut Hilfe; Pflegeheimkosten nicht zahlen; Pflegeheimkosten; Pflegeheimkosten Hilfe; Heimkosten Hilfe; Hilfe ohne Wohnung;
  Menschen ohne Wohnung; obdachlos; Obdachlosigkeit Hilfe; Schulsachen Zuschuss; Zuschuss Schulsachen; Schulsachen Amt; Schulmaterial Zuschuss; Sozialleistungen; Sozialleistungen Antrag; Leistungen Sozialamt
jobcenter:
  langzeitarbeitslos; Langzeitarbeitslosigkeit; Bürgergeld gekürzt; Bürgergeld Kürzung; Bürgergeld Antrag verlängern; Bürgergeldantrag; Bürgergeldantrag verlängern; Jobcenter Einladung; Jobcenter Termin Einladung;
  Jobcenter Brief; Jobcenter Mitwirkung; Leistungsbezieher Bürgergeld
arbeitsagentur:
  Job finden Agentur; Stellensuche Agentur; Stellenangebote Agentur; Jobsuche Agentur; Arbeit suchen; Arbeit finden; Stelle suchen; gekündigt; Kündigung erhalten Arbeitslosengeld; arbeitslos melden;
  Kurzarbeit; Kurzarbeit beantragen; Kurzarbeit Betrieb; Kurzarbeit Arbeitgeber; Kurzarbeitergeld Betrieb; Berufsberatung Schüler
familienkasse:
  Kindergeld Baby; Kindergeld Geburt beantragen; Kindergeld nicht überwiesen; Kindergeld nicht gezahlt; Kindergeld Ausbildung weiter; Kindergeld Sohn Ausbildung; Kindergeld Tochter Studium
wohngeldstelle:
  Wohngeld Rentner; Wohngeld läuft aus; Wohngeld Mietwohnung; Wohngeld Mieter; Wohngeld verlängern
strassenverkehrsbehoerde:
  Führerschein weg; Führerschein verloren Ersatz beantragen; Alkohol Führerschein; Alkohol Führerschein zurück; Führerschein zurück; Führerschein zurückbekommen; Entzug Führerschein; Nach Entzug wieder fahren;
  Führerschein im Ausland gemacht; Ausländischer Führerschein; Motorradführerschein machen; Führerschein Motorrad; Führerschein Klasse A; Fahrschule
zulassungsstelle:
  Auto abmelden; Auto verkauft abmelden; Auto verkauft; Auto gekauft anmelden; Auto gekauft; Fahrzeug gekauft; Fahrzeug verkauft; Kennzeichen weg; Kennzeichen neu; Kennzeichen wechseln; Kennzeichen Umzug; Umzug Kennzeichen;
  neue Stadt Kennzeichen; Auto stilllegen; Fahrzeug abmelden
bauamt:
  Dachfenster; Dachfenster einbauen; Dachfenster Genehmigung; genehmigungspflichtig; genehmigungsfrei; Baugenehmigung nötig; Genehmigung nötig Bau; Bau ohne Genehmigung; Bebauungsplan schauen; Bebauungsplan Einsicht;
  Umbau; Umbau Haus; Umbau Genehmigung; Anbau Haus; Neubau; Neubau Genehmigung; Garage bauen; Garage Genehmigung; Terrassenüberdachung; Terrassenüberdachung Genehmigung; Pool bauen; Pool Genehmigung; Gartenhaus bauen
gesundheitsamt:
  Belehrung; Belehrung Küche; Belehrung Lebensmittel; Belehrung Gastronomie Job; Hygienebelehrung; Hygienebelehrung Küche; Schuluntersuchung; Schuluntersuchung Kind; Schularzt; Schulärztliche Untersuchung;
  Tuberkulose Test; Tuberkulose Untersuchung; Reisemedizin; Impfung Reise Beratung; Seuchen; Gesundheitsamt Kind Untersuchung
veterinaeramt:
  Katzen quälen; Nachbar quält Katzen; Nachbar quält Tiere; Tiere quälen; Tierquälerei Nachbar; Hühner halten; Hühner halten melden; Hühner anmelden; Geflügel anmelden; Geflügelhaltung; Geflügelpest Verdacht;
  Tierhaltung melden; Tierhaltung anzeigen; Tierbestand melden; Schafe Ziegen anmelden; Kaninchen Hühner Tierseuchenkasse
umweltamt:
  Öl in den Fluss; Öl Fluss; Öl See; Gewässer Verschmutzung; Fluss verschmutzt; Bach verschmutzt; Teich verschmutzt; alten Baum fällen; Baum fällen Grundstück; Baum fällen erlaubt; Bäume fällen; Hecke fällen;
  Brunnen anmelden Wasser; Brunnen bohren; Brunnen Genehmigung; Brunnen Wasserrecht; Wasserrecht; Wasserrechtliche Erlaubnis; Wasserentnahme
gewerbeamt:
  Gewerbe anmelden wie; Gewerbe anmelden Café; Café eröffnen; Café eröffnen Genehmigung; Restaurant eröffnen; Restaurant eröffnen Genehmigung; Laden eröffnen; Geschäft eröffnen; Geschäft anmelden; Firma gründen;
  Firma anmelden; Firma gründen Anmeldung; Amazon verkaufen; Amazon FBA Gewerbe; Nebenbei verkaufen; Nebenbei selbstständig; nebenberuflich selbstständig; Gewerbe aufhören; Gewerbe beenden; Gewerbe aufgeben;
  Gewerbe abmelden aufhören
ihk:
  Ausbildereignungsprüfung machen; Ausbildereignung; Ursprungszeugnis Export; Export Ursprungszeugnis; IHK Mitglied; IHK Mitglied werden; Kammer Beitrag Unternehmer
handwerkskammer:
  Meister machen; Meister werden; Meisterschule; Meisterkurs; Lehrling melden; Lehrling eintragen; Azubi Handwerk melden; Azubi eintragen Handwerk; Handwerksbetrieb eintragen; Handwerk Betrieb eintragen
rentenversicherung:
  Rente Zeitpunkt; Rente wann; Rente gehen; in Rente gehen; Rentenalter; Rentenbeginn; Rentenhöhe; Rente Höhe berechnen; Wie hoch wird meine Rente; Rente Mann gestorben; Mann gestorben Rente; Frau gestorben Rente;
  Witwenrente Mann gestorben; Reha beantragen wo; Reha Antrag stellen; Rehabilitation Antrag; Erwerbsminderung beantragen
krankenkasse:
  Krankenkasse wechseln wie; Gesundheitskarte kaputt; Gesundheitskarte defekt; Gesundheitskarte neu bestellen; Kind mitversichern; Kind mitversichert; Ehepartner mitversichert; krank kein Gehalt; Krankengeld nach sechs Wochen;
  Zahnkrone Zuschuss; Zahnkrone Kosten; Zahnbehandlung Zuschuss; Zahnarzt Zuschuss; Zahnersatz Kosten; Brücke Zahn Zuschuss; Implantat Zuschuss
pflegekasse:
  Mutter Pflegegrad; Vater Pflegegrad; Pflegegrad Mutter beantragen; Pflegegrad Vater beantragen; Pflegegeld Vater; Pflegegeld Mutter; Pflege zu Hause Entlastung; Ehefrau pflegen; Ehemann pflegen; Angehörige pflegen;
  Angehörige pflegen Entlastung; Pflegende Entlastung
bafoegamt:
  BAföG Satz; BAföG Satz Höhe; BAföG zurückzahlen; BAföG Rückzahlung Darlehen; BAföG Studium beantragen; BAföG Student Antrag
schulamt:
  Schule wechseln; Schulwechsel Sohn; Schulwechsel Tochter; Schulwechsel Kind; eingeschult; Kind einschulen; Kind einschulen anmelden; Einschulung anmelden; Schulanmeldung Kind; Kind vom Unterricht befreien;
  Unterricht befreien; Unterrichtsbefreiung; Abiturzeugnis Duplikat beantragen; Zeugnis Duplikat; Zeugnis nachträglich ausstellen
bundesverwaltungsamt:
  Spätaussiedler Status; Spätaussiedlerstatus; Status Spätaussiedler feststellen; Lehrer Auslandsschule; Lehrer Deutsche Schule Ausland; Deutsche Schule Ausland Lehrer; Auslandsschuldienst; Auslandsschuldienst Bewerbung
"""

FAELLE_ALLE = (FAELLE, FAELLE_RECHT, FAELLE_SOZIALES, FAELLE_BAUEN_VERKEHR, FAELLE_GEWERBE_SOZIALVERSICHERUNG, FAELLE_SONSTIGES, FAELLE_EINZELWOERTER, FAELLE_NACHSCHAERFUNG, FAELLE_LUECKEN, FAELLE_WORTSCHATZ)
