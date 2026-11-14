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

import de.dataport.suchdienst.suchen.model.out.formular.FoOutModel;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OdOutModelV4;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Version 5 des Ausgabemodells für Leistungen, eingeführt mit PV-8450.
 * Hier sind neue Felder für die Lebenslagen hinzugekommen.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(
    description = "Vom Standard abweichende Version des LB-Ausgabemodells mit neuen Feldern an den onlineServiceLinks",
    example = """
    {
      "name": "Schulform wechseln",
      "description": "Sie wünschen für Ihr Kind einen Wechsel der Schulform? Dann können Sie einen Wechsel der Schulform bis zum Ende der Klasse 8 bei der Schule beantragen.",
      "leikakeys": [
        "99088013005000"
      ],
      "sdgInformation": [
        {
          "id": "urn:xoev-de:fim:codeliste:sdginformationsbereich10501003",
          "uri": "urn:xoev-de:fim:codeliste:sdginformationsbereich",
          "version": "3",
          "code": "1050100",
          "texts": {
            "de": "Bildungswesen in einem anderen Mitgliedstaat, einschließlich der frühkindlichen Betreuung, Bildung und Erziehung, der Primar- und Sekundarschulbildung, der Hochschulbildung und der Erwachsenenbildung",
            "en_GB": "education system in another Member State, including early childhood education and care, primary and secondary education, higher education and adult learning"
          },
          "values": [
            {
              "key": "eu-code",
              "value": "E01",
              "locale": "de"
            }
          ]
        }
      ],
      "id": "L100002.LB.00c33c8f98a69ac19c9ac945f4ffde1e",
      "languageCode": "de",
      "timestamp": "2022-11-11T12:57:07.808994100Z",
      "ars": [
        "050000000000"
      ],
      "sourcePortals": [
        {
          "title": "Weiterleitungsdienst: Deep-Link zum Ursprungsportal",
          "uri": "https://verwaltungsleistung.nrw",
          "replace": false
        }
      ],
      "languageComplete": {},
      "availableLanguages": [
        "de"
      ],
      "organisationIds": [
        "L100002.OE.661b259513840cad0ad1cc0bf90b149a"
      ],
      "lastUpdate": "2022-11-11T12:57:04.026993Z",
      "externalLastUpdate": "2021-06-22T04:03:20Z",
      "addressees": [],
      "arsFederalstate": [
        "05"
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
        "Wechsel in eine andere Schulform Erlaubnis",
        "erlaubnis",
        "gymnasium",
        "weiterleitungsdienst",
        "wechsel",
        "sekundarschule",
        "wechseln",
        "Schulform wechseln",
        "DeepLink",
        "ursprungsportal",
        "hauptschule",
        "schulform",
        "sekundarstufe1",
        "förderschule",
        "realschule",
        "gesamtschule",
        "schulberatung"
      ],
      "details": [
        {
          "@type": "_module",
          "title": "Volltext",
          "text": "Das Team Verwaltung, Personal und Haushalt der Abteilung Schule (FB 45/400.010) organisiert von Seiten des Schulträgers das Einschulungsverfahren sowie das Anmeldeverfahren für die weiterführenden Schulen.   Darüber hinaus überwacht das Team die Schulpflicht bei Kindern, die nach Aachen ziehen.",
          "links": [],
          "replace": true,
          "code": "06",
          "additionalInformation": [],
          "filled": true,
          "placeholder": false,
          "xZufiCategory": "TEXT_MODUL"
        },
        {
          "@type": "_module",
          "title": "Voraussetzungen",
          "text": "<ul><li>evtl. Zeugnisse</li><li>Beschluss der Versetzungskonferenz der bisher besuchten Klasse</li><li>für den Wechsel in eine Förderschule: Nachweis sonderpädagogischer Förderbedarf</li><li>für den Wechsel ins Gymnasium ab Klasse 8 (G9), ab Klasse 7 (G8): Kenntnisse einer zweiten Fremdsprache</li></ul>",
          "links": [],
          "replace": false,
          "code": "09",
          "additionalInformation": [],
          "filled": true,
          "placeholder": false,
          "xZufiCategory": "TEXT_MODUL"
        },
        {
          "@type": "_module",
          "title": "erforderliche Unterlagen",
          "text": "<p>Informationen hierzu können Sie den Ausführungen der jeweiligen Schulform entnehmen.</p>",
          "links": [],
          "replace": false,
          "code": "08",
          "additionalInformation": [],
          "filled": true,
          "placeholder": false,
          "xZufiCategory": "TEXT_MODUL"
        },
        {
          "@type": "_moduleDetail",
          "title": "Kosten & Gebühren",
          "text": "keine",
          "links": [],
          "replace": false,
          "code": "98",
          "details": [],
          "filled": true,
          "placeholder": false,
          "xZufiCategory": "KOSTEN"
        },
        {
          "@type": "_module",
          "title": "Ansprechpunkt",
          "text": "nicht angegeben",
          "links": [],
          "replace": false,
          "code": "23",
          "additionalInformation": [],
          "filled": true,
          "placeholder": true,
          "xZufiCategory": "TEXT_MODUL"
        },
        {
          "@type": "_moduleDetail",
          "title": "Fristen",
          "text": "Sie müssen die Anträge in der Regel im Verlauf des 2. Halbjahres, spätestens 6 Wochen vor Ausgabe der Jahreszeugnisse bei der Schule stellen.",
          "links": [],
          "replace": false,
          "code": "97",
          "details": [],
          "filled": true,
          "placeholder": false,
          "xZufiCategory": "FRIST"
        },
        {
          "@type": "_module",
          "title": "weiterführende Informationen",
          "text": "http://broschüren.nrw/sekundarstufe-1",
          "links": [],
          "replace": false,
          "code": "15",
          "additionalInformation": [],
          "filled": true,
          "placeholder": false,
          "xZufiCategory": "TEXT_MODUL"
        },
        {
          "@type": "_module",
          "title": "Rechtsgrundlage(n)",
          "text": "",
          "links": [
            {
              "uri": "https://recht.nrw.de/lmi/owa/br_bes_text?anw_nr=2&gld_nr=2&ugl_nr=223&bes_id=7345&menu=0&sg=0&aufgehoben=N&keyword=Schulgesetz#det0",
              "title": "§ 13, § 46, § 50 SchulG",
              "replace": false,
              "dynamicParameters": []
            },
            {
              "uri": "https://recht.nrw.de/lmi/owa/br_text_anzeigen?v_id=10000000000000000640",
              "title": "§ 11, § 12, § 13 APO-S I",
              "replace": false,
              "dynamicParameters": []
            }
          ],
          "replace": false,
          "code": "07",
          "additionalInformation": [],
          "filled": true,
          "placeholder": false,
          "xZufiCategory": "TEXT_MODUL"
        },
        {
          "@type": "_module",
          "title": "Urheber",
          "text": "nicht angegeben",
          "links": [],
          "replace": false,
          "code": "18",
          "additionalInformation": [],
          "filled": true,
          "placeholder": true,
          "xZufiCategory": "TEXT_MODUL"
        },
        {
          "@type": "_module",
          "title": "Zuständige Stelle",
          "text": "nicht angegeben",
          "links": [],
          "replace": false,
          "code": "22",
          "additionalInformation": [],
          "filled": true,
          "placeholder": true,
          "xZufiCategory": "TEXT_MODUL"
        },
        {
          "@type": "_module",
          "title": "Leistungsbezeichnung",
          "text": "Wechsel in eine andere Schulform Erlaubnis",
          "links": [],
          "replace": false,
          "code": "02",
          "additionalInformation": [],
          "filled": true,
          "placeholder": false,
          "xZufiCategory": "TEXT_MODUL"
        },
        {
          "@type": "_module",
          "title": "Leistungsbezeichnung II (Leistungsname)",
          "text": "Schulform wechseln",
          "links": [],
          "replace": false,
          "code": "03",
          "additionalInformation": [],
          "filled": true,
          "placeholder": false,
          "xZufiCategory": "TEXT_MODUL"
        },
        {
          "@type": "_module",
          "title": "Verfahrensablauf",
          "text": "<ul><li>Die Schülerinnen und Schüler bzw. die Erziehungsberechtigten können die Anmeldung über das Internetportal Schüler Online (www.schueleranmeldung.de) abgeben.</li><li>Die jeweilige Schule entscheidet über die Anmeldung und teilt die Entscheidung den Beteiligten über das Internetportal und schriftlich mit.</li><li>Bitte beachten Sie, dass auch bei einer Online-Anmeldung ein persönlicher Termin in der Schule für die Entscheidung der Schulleiterin oder des Schulleiters über eine Aufnahme Ihres Kindes notwendig ist</li></ul>",
          "links": [],
          "replace": false,
          "code": "11",
          "additionalInformation": [],
          "filled": true,
          "placeholder": false,
          "xZufiCategory": "TEXT_MODUL"
        },
        {
          "@type": "_module",
          "title": "Teaser",
          "text": "Sie wünschen für Ihr Kind einen Wechsel der Schulform? Dann können Sie einen Wechsel der Schulform bis zum Ende der Klasse 8 bei der Schule beantragen.",
          "links": [],
          "replace": false,
          "code": "24",
          "additionalInformation": [],
          "filled": true,
          "placeholder": false,
          "xZufiCategory": "TEXT_MODUL"
        },
        {
          "@type": "_moduleDetail",
          "title": "Bearbeitungsdauer",
          "text": "Genauere Informationen zu den Anmeldeterminen und zum zeitlichen Ablauf erhalten Sie von der Kommune der gewünschten Schule oder von der jeweiligen Schule unmittelbar.",
          "links": [],
          "replace": false,
          "code": "95",
          "details": [],
          "filled": true,
          "placeholder": false,
          "xZufiCategory": "BEARBEITUNGSDAUER"
        }
      ],
      "typing": [
        {
          "code": "4",
          "listURI": "urn:de:fim:leika:typisierung",
          "listVersionID": "20201204"
        }
      ],
      "onlineServiceLinks": [
        {
          "uri": "https://serviceportal.aachen.de:443/suche/-/vr-bis-detail/dienstleistung/5740/show",
          "title": "https://serviceportal.aachen.de:443/suche/-/vr-bis-detail/dienstleistung/5740/show",
          "replace": false,
          "dynamicParameters": [],
          "id": "L100002.OD.c84aec1bd98b8773fad0d48c749dd1b3",
          "validFrom": "1970-01-01",
          "validTo": "2999-12-31",
          "validityPeriods": [
            {
              "validFrom": "1970-01-01",
              "validTo": "2999-12-31"
            }
          ],
          "executionLanguages": [],
          "executionNationalities": [],
          "levelOfConfidence": "00",
          "identificationMethods": [],
          "paymentMethods": [],
          "helpLinks": []
        }
      ],
      "personalMatters": [
        {
          "code": "1000000",
          "name": "Bürger",
          "children": [
            {
              "code": "1020000",
              "name": "Partnerschaft und Familie",
              "children": [
                {
                  "code": "1020200",
                  "name": "Kinderbetreuung",
                  "children": []
                }
              ]
            },
            {
              "code": "1030000",
              "name": "Schule, Ausbildung und Studium",
              "children": [
                {
                  "code": "1030100",
                  "name": "Schule",
                  "children": []
                }
              ]
            }
          ]
        }
      ],
      "forms": [
        {
        "id": "L100002.FO.c84aec1bd98b8773fad0d48c749dd1b3",
        "name": "Schulwechsel-Formular",
        "description": "Formular zum Beantragen des Schulwechsels",
        "mimeCode": "text/html",
        "uri": "https://beispiel.de/formulare/antrag-schulwechsel.pdf"
        }
      ],
      "suggest": [
        "Wechsel in eine andere Schulform Erlaubnis",
        "erlaubnis",
        "gymnasium",
        "weiterleitungsdienst",
        "wechsel",
        "sekundarschule",
        "wechseln",
        "Schulform wechseln",
        "sekundarstufeI",
        "ursprungsportal",
        "hauptschule",
        "Deep-Link",
        "schulform",
        "förderschule",
        "realschule",
        "gesamtschule",
        "schulberatung"
      ]
    }
    """
)
public class SdOutModelV5 extends SdOutModelBase {

    @Schema(description = "Zugehörige Onlinedienste")
    private List<OdOutModelV4> onlineServiceLinks;

    // Lebenslagen-Wald der Leistung enthält die Roots (Bürger/Unternehmen) und alle Zwischenlagen.
    @Schema(description = "Enthält Lebenslagen")
    private List<LifeSituationOutModel> personalMatters;

    @Schema(description = "Zugehörige Formulare")
    private List<FoOutModel> forms;
}
