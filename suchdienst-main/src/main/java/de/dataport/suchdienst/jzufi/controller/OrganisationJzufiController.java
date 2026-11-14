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
package de.dataport.suchdienst.jzufi.controller;

import static de.dataport.suchdienst.util.XZuFiVersionHeaderUtil.MEDIA_TYPE_JSON_22;
import static de.dataport.suchdienst.util.XZuFiVersionHeaderUtil.MEDIA_TYPE_JSON_23;

import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.error.model.ApiErrorModel;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.GenericOrganisationseinheit;
import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.dataport.suchdienst.jzufi.model.mapper.JzufiModelMapper;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.service.OrganisationUnitService;
import de.dataport.suchdienst.util.XZuFiVersionHeaderUtil;
import de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@ApiAudienceMarker(ApiAudience.PUBLIC)
@Tag(
    name = "JZuFi",
    description = "Endpunkte zum Laden von JZuFi. JZuFi ist eine JSON-Representation des XZuFi Datenmodells."
)
@Validated
public class OrganisationJzufiController {

    public static final String CODE_API_404 = "api.404";
    private final OrganisationUnitService organisationUnitService;
    private final MessageSource messageSource;

    @Value("${de.dataport.suchdienst.endpoint.enableDeprecated.v2-organisationunits-jzufi:false}")
    private boolean endpointV2OrganisationUnitsJzufiActive;

    public OrganisationJzufiController(
        OrganisationUnitService organisationUnitService,
        MessageSource messageSource
    ) {
        this.organisationUnitService = organisationUnitService;
        this.messageSource = messageSource;
    }

    /**
     * @deprecated Stattdessen /v2/organisationunits/{id}/jzufi-2-3 verwenden.
     */
    @Deprecated(since = "2026-10", forRemoval = true)
    @GetMapping(
        value = "/v1/organisationunits/{id}/jzufi-2-3",
        produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    @Operation(
        summary = "JZuFi der Organisationseinheit in der XZuFi-Version 2.3.1",
        description = "Gibt das ursprüngliche XZuFi als JSON (JZuFi) der Organisationseinheit in der XZuFi-Version 2.3.1 zurück. Wenn die Auslieferungsversion der Daten von der Version abweicht, in der die Daten an das PVOG geliefert wurden, werden die ausgelieferten Daten entsprechend konvertiert."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Organisationseinheit für ID gefunden",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(
                implementation = de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert.class
            ),
            examples = { @ExampleObject(Example.RESPONSE_EXAMPLE_JZUFI_2_3_1_ORGANISATIONSEINHEIT) }
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Organisationseinheit nicht gefunden",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class),
            examples = {
                @ExampleObject(
                    """
                        {
                            "timestamp": "2022-10-10T12:38:03.887762500Z",
                            "code": "NOT_FOUND",
                            "message": "Die angegebene Resource konnte nicht gefunden werden.",
                            "codeNumber": 1906
                        }
                    """
                ),
            }
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Ungültige ID oder Datumsangabe.",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class)
        )
    )
    public de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert getJzufiOe231V1(
        @PathVariable @Parameter(
            description = "Die ID der Organisationseinheit",
            examples = { @ExampleObject("L100042.OE.868") },
            required = true
        ) @Size(min = 1, max = 255) String id,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validdate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate
    ) {
        return organisationUnitService
            .getJzufi(id, validDate)
            .map(GenericOrganisationseinheit::getOrConvertXZuFi23)
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage(CODE_API_404, null, Locale.GERMAN)
                )
            );
    }

    @GetMapping(
        value = "/v1/organisationunits/{id}/jzufi-2-2",
        produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    @Operation(
        summary = "JZuFi der Organisationseinheit in der XZuFi-Version 2.2.0",
        description = "Gibt das ursprüngliche XZuFi als JSON (JZuFi) der Organisationseinheit in der XZuFi-Version 2.2.0 zurück. Wenn die Auslieferungsversion der Daten von der Version abweicht, in der die Daten an das PVOG geliefert wurden, werden die ausgelieferten Daten entsprechend konvertiert."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Organisationseinheit für ID gefunden",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = OrganisationseinheitErweitert.class),
            examples = { @ExampleObject(Example.RESPONSE_EXAMPLE_JZUFI_2_2_0_ORGANISATIONSEINHEIT) }
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Organisationseinheit nicht gefunden",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class),
            examples = {
                @ExampleObject(
                    """
                        {
                            "timestamp": "2022-10-10T12:38:03.887762500Z",
                            "code": "NOT_FOUND",
                            "message": "Die angegebene Resource konnte nicht gefunden werden.",
                            "codeNumber": 1906
                        }
                    """
                ),
            }
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Ungültige ID oder Datumsangabe.",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class)
        )
    )
    public OrganisationseinheitErweitert getJzufiOe220V1(
        @PathVariable @Parameter(
            description = "Die ID der Organisationseinheit",
            examples = { @ExampleObject("L100042.OE.868") },
            required = true
        ) @Size(min = 1, max = 255) String id,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validdate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate
    ) {
        return organisationUnitService
            .getJzufi(id, validDate)
            .map(GenericOrganisationseinheit::getOrConvertXZuFi22)
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage(CODE_API_404, null, Locale.GERMAN)
                )
            );
    }

    /**
     * @deprecated Stattdessen /v2/organisationunits/{id}/jzufi-2-3 verwenden.
     */
    @Deprecated(since = "11.12.0", forRemoval = true)
    @GetMapping(
        value = "/v2/organisationunits/jzufi",
        produces = { MEDIA_TYPE_JSON_22, MEDIA_TYPE_JSON_23, MediaType.APPLICATION_JSON_VALUE }
    )
    @Operation(
        summary = "JZuFi der Organisationseinheit in angefragter Version",
        description = "Gibt das ursprüngliche XZuFi als JSON (JZuFi) der Organisationseinheit in angefragter Version zurück. Es werden Accept-Headers application/json; xzufi-version=2.2.0 oder application/json; xzufi-version=2.3.1 unterstützt. Wenn die Auslieferungsversion der Daten von der Version abweicht, in der die Daten an das PVOG geliefert wurden, werden die ausgelieferten Daten entsprechend konvertiert."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Organisationseinheit für ID gefunden",
        content = {
            @Content(
                mediaType = MEDIA_TYPE_JSON_22,
                schema = @Schema(implementation = OrganisationseinheitErweitert.class),
                examples = {
                    @ExampleObject(Example.RESPONSE_EXAMPLE_JZUFI_2_2_0_ORGANISATIONSEINHEIT),
                }
            ),
            @Content(
                mediaType = MEDIA_TYPE_JSON_23,
                schema = @Schema(
                    implementation = de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert.class
                ),
                examples = {
                    @ExampleObject(Example.RESPONSE_EXAMPLE_JZUFI_2_3_1_ORGANISATIONSEINHEIT),
                }
            ),
        }
    )
    @ApiResponse(
        responseCode = "404",
        description = "Organisationseinheit nicht gefunden",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            examples = {
                @ExampleObject(
                    """
                        {
                            "timestamp": "2022-10-10T12:38:03.887762500Z",
                            "code": "NOT_FOUND",
                            "message": "Die angegebene Resource konnte nicht gefunden werden.",
                            "codeNumber": 1906
                        }
                    """
                ),
            }
        )
    )
    public ResponseEntity<Serializable> getJzufiOeV2(
        @Parameter(hidden = true) @RequestHeader(value = "Accept") MediaType accept,
        @Parameter(
            description = "Die ID der Organisationseinheit",
            examples = { @ExampleObject("L100042.OE.868") },
            required = true
        ) @RequestParam(name = "id") @Size(min = 1, max = 255) String id,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validdate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate
    ) {
        if (!endpointV2OrganisationUnitsJzufiActive) {
            throw new SuchenException(
                ErrorCodeEnum.NOT_FOUND,
                "Der Endpunkt '/v2/organisationunits/jzufi' ist nicht länger aktiv."
            );
        }
        XZuFiVersion version = XZuFiVersionHeaderUtil.getXZuFiVersionFromHeader(accept).orElseThrow(
            () ->
                new SuchenException(
                    ErrorCodeEnum.BAD_REQUEST,
                    messageSource.getMessage(
                        "api.400_bad_request_headers",
                        new String[] { XZuFiVersion.class.getSimpleName() },
                        Locale.GERMAN
                    )
                )
        );

        return organisationUnitService
            .getJzufi(id, validDate)
            .map(genericOrganisation ->
                ResponseEntity.ofNullable(
                    version == XZuFiVersion.XZUFI22
                        ? genericOrganisation.getOrConvertXZuFi22()
                        : genericOrganisation.getOrConvertXZuFi23()
                )
            )
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage(CODE_API_404, null, Locale.GERMAN)
                )
            );
    }

    @GetMapping(
        value = "/v2/organisationunits/{id}/jzufi-2-3",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "JZuFi der Organisationseinheit in der XZuFi-Version 2.3.1",
        description = "Liefert das Suchdienst-eigene JZuFi-Ausgabemodell mit @type für verschachtelte Zuständigkeiten. Daten anderer XZuFi-Versionen werden nach 2.3.1 konvertiert."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Organisationseinheit für ID gefunden",
        content = @Content(
            schema = @Schema(
                implementation = de.dataport.suchdienst.jzufi.model.v23.OrganisationseinheitErweitert.class
            )
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Organisationseinheit nicht gefunden",
        content = @Content(schema = @Schema(implementation = ApiErrorModel.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Ungültige ID.",
        content = @Content(schema = @Schema(implementation = ApiErrorModel.class))
    )
    public de.dataport.suchdienst.jzufi.model.v23.OrganisationseinheitErweitert getJzufiOe231V2(
        @PathVariable @Parameter(
            description = "Die ID der Organisationseinheit",
            required = true
        ) @Size(min = 1, max = 255) String id
    ) {
        return organisationUnitService
            .getJzufi(id, null)
            .map(GenericOrganisationseinheit::getOrConvertXZuFi23)
            .map(JzufiModelMapper.INSTANCE::map23)
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage(CODE_API_404, null, Locale.GERMAN)
                )
            );
    }

    private static class Example {

        public static final String RESPONSE_EXAMPLE_JZUFI_2_2_0_ORGANISATIONSEINHEIT = """
            {
                "zustaendigkeit": [],
                "formular": [],
                "kontaktperson": [],
                "id": {
                    "value": "868",
                    "schemeAgencyID": "L100042"
                },
                "idSekundaer": [],
                "externeOrganisationseinheitsermittlung": false,
                "untergeordneteOrganisationseinheitID": [],
                "alternativeHierarchie": [],
                "kategorie": [],
                "name": [
                    {
                        "name": [
                            {
                                "value": "Landgericht München I",
                                "languageCode": "de"
                            }
                        ],
                        "kurzbezeichnung": [
                            {
                                "value": "LG M1",
                                "languageCode": "de"
                            }
                        ]
                    }
                ],
                "beschreibung": [],
                "kurzbeschreibung": [],
                "infoOeffnungszeitenText": [],
                "infoOeffnungszeitenStrukturiert": [],
                "infoInternServicecenter": [],
                "infoSonstige": [],
                "anschrift": [
                    {
                        "anfahrturl": [
                            {
                                "uri": "http://geoportal.bayern.de/bayernatlas?&X=5333676.26&Y=4467699.5&zoom=14&lang=de&topic=ba&bgLayer=atkis&crosshair=marker",
                                "titel": "Standort im BayernAtlas anzeigen",
                                "languageCode": "de"
                            }
                        ],
                        "infoParkplatz": [],
                        "infoOEPNV": [],
                        "infoBarrierefreiheit": [],
                        "typ": {
                            "code": "001",
                            "listURI": "urn:xoev-de:fim:codeliste:xzufi.anschrifttyp",
                            "listVersionID": "2.0"
                        },
                        "strasse": "Prielmayerstr. 7",
                        "postleitzahl": "80335",
                        "ort": "München",
                        "ortID": [],
                        "verwaltungspolitischeKodierung": {
                            "kreis": {
                                "code": "09162",
                                "name": "München",
                                "listURI": "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:kreis",
                                "listVersionID": "unbestimmt"
                            },
                            "bezirk": {
                                "code": "091",
                                "name": "Oberbayern",
                                "listURI": "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:bezirk",
                                "listVersionID": "unbestimmt"
                            },
                            "bundesland": {
                                "code": "09",
                                "name": "Bayern",
                                "listURI": "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:bundesland",
                                "listVersionID": "unbestimmt"
                            },
                            "gemeindeschluessel": {
                                "code": "09162000",
                                "name": "München, Landeshauptstadt",
                                "listURI": "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:ags",
                                "listVersionID": "unbestimmt"
                            },
                            "staat": {
                                "code": "000",
                                "name": "Deutschland",
                                "listURI": "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:staat",
                                "listVersionID": "unbestimmt"
                            }
                        },
                        "geokodierung": [
                            {
                                "point": {
                                    "id": "ID_POINT_GEB_868_868",
                                    "pos": {
                                        "value": [
                                            4467699.5,
                                            5333676.26
                                        ],
                                        "axisLabels": [],
                                        "uomLabels": []
                                    },
                                    "metaDataProperty": [],
                                    "name": [],
                                    "srsName": "EPSG:31468",
                                    "axisLabels": [],
                                    "uomLabels": []
                                }
                            }
                        ],
                        "gueltigkeit": []
                    },
                    {
                        "anfahrturl": [],
                        "infoParkplatz": [],
                        "infoOEPNV": [],
                        "infoBarrierefreiheit": [],
                        "typ": {
                            "code": "006",
                            "listURI": "urn:xoev-de:fim:codeliste:xzufi.anschrifttyp",
                            "listVersionID": "2.0"
                        },
                        "strasse": "",
                        "postleitzahl": "80316",
                        "ort": "München",
                        "ortID": [],
                        "geokodierung": [],
                        "gueltigkeit": []
                    }
                ],
                "erreichbarkeit": [
                    {
                        "kanal": {
                            "code": "02",
                            "name": "Telefon Festnetz",
                            "listURI": "urn:de:xoev:codeliste:erreichbarkeit",
                            "listVersionID": "unbestimmt"
                        },
                        "kennung": "+49 89 5597-03",
                        "gueltigkeit": []
                    },
                    {
                        "kanal": {
                            "code": "04",
                            "name": "Fax",
                            "listURI": "urn:de:xoev:codeliste:erreichbarkeit",
                            "listVersionID": "unbestimmt"
                        },
                        "kennung": "+49 89 5597-2991",
                        "gueltigkeit": []
                    },
                    {
                        "kanal": {
                            "code": "01",
                            "name": "E-Mail",
                            "listURI": "urn:de:xoev:codeliste:erreichbarkeit",
                            "listVersionID": "unbestimmt"
                        },
                        "kennung": "Poststelle@lg-m1.bayern.de",
                        "gueltigkeit": []
                    }
                ],
                "kommunikationssystem": [],
                "internetadresse": [
                    {
                        "uri": "http://www.justiz.bayern.de/gericht/lg/m1/"
                    }
                ],
                "bankverbindung": [],
                "zahlungsweisen": [],
                "zahlungsweisenText": [],
                "synonym": [],
                "sprachversion": [
                    {
                        "languageCode": "de",
                        "sprachbezeichnungDeutsch": "Deutsch",
                        "sprachbezeichnungNativ": "Deutsch"
                    }
                ],
                "gueltigkeit": [
                    {
                        "beginn": "1699-12-31+01:00",
                        "ende": "2050-02-01+01:00",
                        "zusatz": []
                    }
                ]
            }
        """;

        public static final String RESPONSE_EXAMPLE_JZUFI_2_3_1_ORGANISATIONSEINHEIT = """
         {
                     "id": {
                         "value": "9137467",
                         "schemeAgencyID": "L100012",
                         "schemeID": "OID"
                     },
                     "idSekundaer": [],
                     "externeOrganisationseinheitsermittlung": false,
                     "untergeordneteOrganisationseinheitID": [],
                     "alternativeHierarchie": [],
                     "kategorie": [{
                             "id": {
                                 "value": "9081960",
                                 "schemeAgencyID": "L100012",
                                 "schemeID": "OID"
                             },
                             "idSekundaer": [],
                             "untergeordneteKategorieID": [],
                             "klasse": {
                                 "id": {
                                     "value": "ORGANISATIONSEINHEITSKATEGORIE"
                                 },
                                 "idSekundaer": [],
                                 "typ": {
                                     "code": "002",
                                     "listURI": "urn:xoev-de:fim:codeliste:xzufi.kategorieklassetyp",
                                     "listVersionID": "1"
                                 },
                                 "bezeichnung": [{
                                         "value": "Organisationseinheitkategorie",
                                         "languageCode": "de"
                                     }
                                 ],
                                 "beschreibung": []
                             },
                             "bezeichnung": [{
                                     "value": "Kammer",
                                     "languageCode": "de"
                                 }
                             ],
                             "beschreibung": []
                         }
                     ],
                     "name": [{
                             "name": [{
                                     "value": "Architekten- und Ingenieurkammer Schleswig-Holstein",
                                     "languageCode": "de"
                                 }
                             ],
                             "kurzbezeichnung": []
                         }
                     ],
                     "beschreibung": [],
                     "kurzbeschreibung": [],
                     "infoOeffnungszeitenText": [{
                             "value": "Montag - Donnerstag: 9.00 - 12.00 Uhr und 14.00 - 16.00 Uhr\\\\n Freitag: 9.00 - 12.00 Uhr\\\\n\\\\n",
                             "html": true,
                             "languageCode": "de"
                         }
                     ],
                     "infoOeffnungszeitenStrukturiert": [],
                     "terminvereinbarungsdienst": [],
                     "infoInternServicecenter": [],
                     "infoSonstige": [],
                     "anschrift": [{
                             "typ": {
                                 "code": "001",
                                 "listURI": "urn:xoev-de:fim:codeliste:xzufi.anschrifttyp",
                                 "listVersionID": "4"
                             },
                             "strasse": "Düsternbrooker Weg",
                             "hausnummer": "71",
                             "postleitzahl": "24105",
                             "ort": "Kiel",
                             "ortID": [{
                                     "value": "010020000000",
                                     "schemeID": "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:rs",
                                     "schemeVersionID": "2022-09-30"
                                 }
                             ],
                             "verwaltungspolitischeKodierung": {
                                 "staat": {
                                     "code": "000",
                                     "name": "Deutschland",
                                     "listURI": "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:staat",
                                     "listVersionID": "2022-02-18"
                                 },
                                 "bundesland": {
                                     "code": "01",
                                     "name": "Schleswig-Holstein",
                                     "listURI": "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:bundesland",
                                     "listVersionID": "2010-04-01"
                                 },
                                 "gemeindeschluessel": {
                                     "code": "01002000",
                                     "name": "Kiel",
                                     "listURI": "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:ags",
                                     "listVersionID": "2018-07-31"
                                 },
                                 "regionalschluessel": {
                                     "code": "010020000000",
                                     "name": "Kiel",
                                     "listURI": "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:rs",
                                     "listVersionID": "2022-09-30"
                                 }
                             },
                             "geokodierung": [{
                                     "point": {
                                         "metaDataProperty": [],
                                         "name": [],
                                         "srsName": "EPSG:25832",
                                         "axisLabels": ["longitude", "latitude"],
                                         "uomLabels": [],
                                         "pos": {
                                             "value": [574786.184, 6021298.671],
                                             "axisLabels": [],
                                             "uomLabels": []
                                         }
                                     }
                                 }
                             ],
                             "bild": [],
                             "gueltigkeit": [],
                             "anfahrturl": [],
                             "infoParkplatz": [],
                             "infoOEPNV": [],
                             "infoBarrierefreiheit": [],
                             "kennzeichenAufzug": false
                         }
                     ],
                     "erreichbarkeit": [{
                             "kanal": {
                                 "code": "02",
                                 "listURI": "urn:de:xoev:codeliste:erreichbarkeit",
                                 "listVersionID": "3"
                             },
                             "kennung": "+49 431 57065-0",
                             "gueltigkeit": []
                         }, {
                             "kanal": {
                                 "code": "04",
                                 "listURI": "urn:de:xoev:codeliste:erreichbarkeit",
                                 "listVersionID": "3"
                             },
                             "kennung": "+49 431 57065-25",
                             "gueltigkeit": []
                         }, {
                             "kanal": {
                                 "code": "01",
                                 "listURI": "urn:de:xoev:codeliste:erreichbarkeit",
                                 "listVersionID": "3"
                             },
                             "kennung": "info@aik-sh.de",
                             "gueltigkeit": []
                         }
                     ],
                     "internetadresse": [{
                             "uri": "http://www.aik-sh.de",
                             "titel": "www.aik-sh.de",
                             "languageCode": "de"
                         }
                     ],
                     "bankverbindung": [],
                     "zahlungsweisen": [],
                     "zahlungsweisenText": [],
                     "synonym": [],
                     "kennzeichenOeffentlichAnzeigen": true,
                     "versionsinformation": {
                         "geaendertDatumZeit": "2023-10-12"
                     },
                     "sprachversion": [{
                             "languageCode": "de",
                             "sprachbezeichnungDeutsch": "Deutsch",
                             "sprachbezeichnungNativ": "Deutsch",
                             "kennzeichenMaschinelleUebersetzung": false
                         }
                     ],
                     "gueltigkeit": [],
                     "zustaendigkeit": [],
                     "formular": [],
                     "kontaktperson": []
                 }
        """;
    }
}
