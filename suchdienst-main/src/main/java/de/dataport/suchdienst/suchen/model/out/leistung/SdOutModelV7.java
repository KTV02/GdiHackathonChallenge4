/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2025 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.suchen.model.out.leistung;

import de.dataport.suchdienst.suchen.model.out.onlinedienst.OdOutModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(
    description = "Dienstbeschreibung-Ausgabe",
    example = """
    {
        "name": "Ingenieur / Ingenieurin: Erlaubnis zum Führen der Berufsbezeichnung",
        "description": "<p>Die Berufsbezeichnung &#34;Ingenieurin&#34; oder &#34;Ingenieur&#34; ist nach dem Ingenieurgesetz Schleswig-Holstein (IngG) gesetzlich geschützt. Diesen Titel - allein oder in einer Wortverbindung - darf führen,</p> <ol><li>wer ein Studium einer technischen oder technisch-naturwissenschaftlichen Fachrichtung mit einer Regelstudienzeit von mindestens sechs Semestern in Vollzeit oder während einer entsprechenden Gesamtdauer in Teilzeit an einer deutschen, staatlichen oder staatlich anerkannten Universität, Fachhochschule oder Berufsakademie mit Erfolg abgeschlossen hat, wobei dieser Studiengang überwiegend von den Bereichen Mathematik, Informatik, Naturwissenschaften und Technik geprägt sein muss; für die Bezeichnung &#34;Wirtschaftsingenieurin&#34; oder &#34;Wirtschaftsingenieur&#34; muss der Studiengang von diesen Fächern zumindest geprägt sein,</li><li>wer von der zuständigen Stelle die Genehmigung hierzu erhalten hat,</li><li>wer nach dem Recht eines anderen Landes der Bundesrepublik Deutschland zur Führung dieser Berufsbezeichnung berechtigt ist oder</li><li>wer bis zum Inkrafttreten dieses Gesetzes bereits berechtigt war, die Berufsbezeichnung zu führen.</li></ol> <p>Bezeichnungen, die auf wirtschaftlich tätige Zusammenschlüsse (&#34;Ingenieurbüro&#34; o.ä.) hinweisen, dürfen in Verbindung mit der Berufsbezeichnung nach Absatz 1 oder ähnlichen Bezeichnungen nur geführt werden, wenn mindestens die Hälfte der Mitglieder des Vorstands, der Geschäftsführung oder der Personen, die mindestens über die Hälfte der Stimmrechte verfügen, zur Führung der Berufsbezeichnung berechtigt sind.</p> <p>Eine Genehmigung können Personen aus einem Mitgliedstaat der Europäischen Union, einem Vertragsstaat des Abkommens über den Europäischen Wirtschaftsraum oder einem sonstigen durch Abkommen gleichgestellten Staat (Mitglieds- oder Vertragsstaat) beantragen, wenn sie in Schleswig-Holstein ihre Hauptwohnung, ihre Hauptniederlassung oder ihre überwiegende berufliche Beschäftigung haben.</p> <p>Die für eine Genehmigung erforderlichen Befähigungs- oder Ausbildungsnachweise und/oder Berufserfahrungen hängen davon ab, ob der Beruf im Herkunftsland reglementiert ist oder nicht.</p> <p>Die zuständige Stelle (s.u.) kann bei wesentlichen Qualifikationsunterschieden Ausgleichsmaßnahmen in Form von Anpassungslehrgängen oder Eignungsprüfungen verlangen.</p> <p>Rechtfertigen Tatsachen die Annahme, dass die erforderliche fachliche Kenntnis fehlt oder Leben und Gesundheit von Menschen erheblich gefährdet sind, dann kann das Führen der Berufsbezeichnung durch die zuständige Behörde untersagt werden.</p> <p>Eine Genehmigung können Personen aus einem Mitgliedstaat der Europäischen Union, einem Vertragsstaat des Abkommens über den Europäischen Wirtschaftsraum oder einem sonstigen Abkommen gleichgestellten</p> <p>Rechtfertigen Tatsachen die Annahme, dass die erforderliche fachliche Kenntnis fehlt oder Leben und Gesundheit von Menschen erheblich gefährdet sind, dann kann das Führen der Berufsbezeichnung durch die zuständige Behörde untersagt werden.</p>",
        "leikakeys": [
            "99147006067000"
        ],
        "sdgInformation": [],
        "id": "L100012.LB.9413106",
        "languageCode": "de",
        "timestamp": "2022-07-14T15:13:24.045997200Z",
        "ars": [
            "010000000000"
        ],
        "sourcePortals": [],
        "languageComplete": {},
        "availableLanguages": [
            "de"
        ],
        "organisationIds": [],
        "lastUpdate": "2022-07-14T15:13:24.045997200Z",
        "externalLastUpdate": "2021-03-11T11:47:51Z",
        "addressees": [],
        "arsFederalstate": [
            "01"
        ],
        "validFrom": "1970-01-01",
        "validTo": "2999-12-31",
        "validityPeriods": [
            {
                "validFrom": "1970-01-01",
                "validTo": "2999-12-31"
            }
        ],
        "synonyms": [
            "the",
            "take",
            "berufsbezeichnung",
            "erlaubnis",
            "Ingenieur  Ingenieurin Erlaubnis zum Führen der Berufsbezeichnung",
            "führen",
            "ingenieur",
            "permission",
            "title",
            "engineer",
            "professional"
        ],
        "typing": [
            {
                "code": "4",
                "name": "Regelungskompetenz auf Landesebene",
                "listURI": "urn:de:fim:leika:typisierung",
                "listVersionID": "20201204"
            }
        ],
        "onlineServiceLinks": [],
        "teaser": "Teaser",
        "suggest": [
            "the",
            "take",
            "berufsbezeichnung",
            "erlaubnis",
            "Ingenieur / Ingenieurin: Erlaubnis zum Führen der Berufsbezeichnung",
            "führen",
            "ingenieur",
            "permission",
            "title",
            "engineer",
            "professional"
        ]
    }"""
)
public class SdOutModelV7 extends SdOutModelBase {

    @Schema(description = "Zugehörige Onlinedienste")
    private List<OdOutModel> onlineServiceLinks;

    @Schema(description = "Inhalt aus modulText mit Code 24")
    private String teaser;
}
