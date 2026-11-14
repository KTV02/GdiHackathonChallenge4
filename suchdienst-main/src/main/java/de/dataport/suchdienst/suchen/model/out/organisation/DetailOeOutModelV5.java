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
package de.dataport.suchdienst.suchen.model.out.organisation;

import de.dataport.suchdienst.suchen.model.out.common.HyperLink;
import de.dataport.suchdienst.suchen.model.out.formular.FoOutModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Dieses OutModel unterscheidet sich von dem V4 darin, dass das Feld "forms" entfällt und die neuen geschachtelten Formulare
 * (PV-10975) im Feld "nestedForms" enthält. Weiterhin sind Internet-Links im neuen Feld "internetAddresses" enthalten.
 */
@Schema(
    description = "Organisationseinheit",
    example = """
    {
        "id": "L100042.OE.868",
        "languages": [
            "de"
        ],
        "lastUpdate": "2022-10-10T09:12:27.031512400Z",
        "languageComplete": {},
        "title": "Landgericht München I",
        "internetAddresses": [
            {
                "uri" : "https://example.com/index.html",
                "title": "Landgericht München I",
                "description" : "Website des Landgerichts München"
            }
        ],
        "nestedForms" : [
            {
                "id" : "L100002.FO.12345",
                "name" : "Einreichung Antrag",
                "description": "Einreichung eines Antrags ",
                "uri": "https://example.com/formulare/antrag.pdf"
            }
        ],
        "employees": [],
        "additionalInformation": [],
        "bankAccounts": [],
        "location": {
            "urls": [],
            "information": [],
            "addresses": [
                {
                    "code": "001",
                    "city": "München",
                    "type": "Hausanschrift",
                    "zip": "80335",
                    "street": "Prielmayerstr. 7",
                    "geo": {
                        "longitude": "11.564538874129338",
                        "latitude": "48.14029082463299",
                        "system": "EPSG:4326"
                    }
                },
                {
                    "code": "003",
                    "city": "München",
                    "type": "Postfach",
                    "zip": "80316",
                    "postbox": "1234567"
                }
            ],
            "communications": [
                {
                    "code": "02",
                    "name": "Telefon Festnetz",
                    "value": "+49 89 5597-03",
                    "additional": ""
                },
                {
                    "code": "01",
                    "name": "E-Mail",
                    "value": "Poststelle@lg-m1.bayern.de",
                    "additional": ""
                },
                {
                    "code": "04",
                    "name": "Fax",
                    "value": "+49 89 5597-2991",
                    "additional": ""
                }
            ]
        },
        "communicationSystems": [],
        "role": {
            "name": "Code 01 konnte nicht aufgelöst werden.",
            "code": "01"
        }
    }
    """
)
@Data
@EqualsAndHashCode(callSuper = true)
public class DetailOeOutModelV5 extends DetailOeOutModelBase {

    @Schema(description = "Zugehörige Formulare, xzufi:formular")
    private List<FoOutModel> nestedForms;

    @Schema(description = "Internet-Links, xzufi:internetAdresse")
    private List<HyperLink> internetAddresses;
}
