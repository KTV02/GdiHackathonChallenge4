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
import de.dataport.suchdienst.error.model.ErrorModel;
import de.dataport.suchdienst.importer.model.GenericLeistung;
import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.dataport.suchdienst.jzufi.model.mapper.JzufiModelMapper;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.service.ServiceDescriptionService;
import de.dataport.suchdienst.util.XZuFiVersionHeaderUtil;
import de.xoev.xzufi.v2_2_0.Leistung;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@ApiAudienceMarker(ApiAudience.PUBLIC)
@Tag(
    name = "JZuFi",
    description = "Endpunkte zum Laden von JZuFi. JZuFi ist eine JSON-Representation des XZuFi Datenmodells."
)
public class LeistungJzufiController {

    public static final String CODE_API_404 = "api.404";
    private final ServiceDescriptionService serviceDescriptionService;
    private final MessageSource messageSource;

    @Value("${de.dataport.suchdienst.endpoint.enableDeprecated.v3-servicedescriptions-jzufi:false}")
    private boolean endpointV3ServicedescriptionsJzufiActive;

    public LeistungJzufiController(
        ServiceDescriptionService serviceDescriptionService,
        MessageSource messageSource
    ) {
        this.serviceDescriptionService = serviceDescriptionService;
        this.messageSource = messageSource;
    }

    /**
     * @deprecated Stattdessen /v2/servicedescriptions/{id}/jzufi-2-3 verwenden.
     */
    @Deprecated(since = "2026-10", forRemoval = true)
    @GetMapping(
        value = "/v1/servicedescriptions/{id}/jzufi-2-3",
        produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Gibt das ursprüngliche XZuFi als JSON (JZuFi) der Leistung in der XZuFi-Version 2.3.1 zurück.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = de.xoev.xzufi.v2_3_1.Leistung.class),
                    examples = {
                        @ExampleObject(
                            name = "Leistung im JZuFi 2.3.1 Format",
                            description = "in diesem Beispiel: Ingenieur / Ingenieurin: Erlaubnis zum Führen der Berufsbezeichnung",
                            value = Example.RESPONSE_EXAMPLE_JZUFI_LEISTUNG_23
                        ),
                    }
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Es wurde kein Ergebnis gefunden.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorModel.class)
                )
            ),
        }
    )
    @Operation(
        summary = "JZuFi der Leistung in der XZuFi-Version 2.3.1",
        description = "Gibt das ursprüngliche XZuFi als JSON (JZuFi) der Leistungsbeschreibung in der XZuFi-Version 2.3.1 zurück. Wenn die Auslieferungsversion der Daten von der Version abweicht, in der die Daten an das PVOG geliefert wurden, werden die ausgelieferten Daten entsprechend konvertiert."
    )
    @ApiResponse(
        responseCode = "400",
        description = "Ungültige ID oder Datumsangabe.",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class)
        )
    )
    public de.xoev.xzufi.v2_3_1.Leistung getJzufiLb231V1(
        @PathVariable @Parameter(
            description = "Die ID der Leistungsbeschreibung, deren Details geladen werden sollen",
            example = "L100012.LB.9413106",
            required = true
        ) @Size(min = 1, max = 255) String id,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validdate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate
    ) {
        return serviceDescriptionService
            .fetchJzufi(id, validDate)
            .map(GenericLeistung::getOrConvertXZuFi23)
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage(CODE_API_404, null, Locale.GERMAN)
                )
            );
    }

    @GetMapping(
        value = "/v1/servicedescriptions/{id}/jzufi-2-2",
        produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Gibt das ursprüngliche XZuFi als JSON (JZuFi) der Leistung in der XZuFi-Version 2.2.0 zurück.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Leistung.class),
                    examples = {
                        @ExampleObject(
                            name = "Leistung im JZuFi 2.2.0 Format",
                            description = "in diesem Beispiel: Ingenieur / Ingenieurin: Erlaubnis zum Führen der Berufsbezeichnung",
                            value = Example.RESPONSE_EXAMPLE_JZUFI_LEISTUNG_22
                        ),
                    }
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Es wurde kein Ergebnis gefunden.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorModel.class)
                )
            ),
        }
    )
    @Operation(
        summary = "JZuFi der Leistung in der XZuFi-Version 2.2.0",
        description = "Gibt das ursprüngliche XZuFi als JSON (JZuFi) der Leistungsbeschreibung in der XZuFi-Version 2.2.0 zurück. Wenn die Auslieferungsversion der Daten von der Version abweicht, in der die Daten an das PVOG geliefert wurden, werden die ausgelieferten Daten entsprechend konvertiert."
    )
    @ApiResponse(
        responseCode = "400",
        description = "Ungültige ID oder Datumsangabe.",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class)
        )
    )
    public Leistung getJzufiLb220V1(
        @PathVariable @Parameter(
            description = "Die ID der Leistungsbeschreibung, deren Details geladen werden sollen",
            example = "L100012.LB.9413106",
            required = true
        ) @Size(min = 1, max = 255) String id,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validdate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate
    ) {
        return serviceDescriptionService
            .fetchJzufi(id, validDate)
            .map(GenericLeistung::getOrConvertXZuFi22)
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage(CODE_API_404, null, Locale.GERMAN)
                )
            );
    }

    /**
     * @deprecated Stattdessen /v2/servicedescriptions/{id}/jzufi-2-3 verwenden.
     */
    @Deprecated(since = "11.12.0", forRemoval = true)
    @GetMapping(
        value = "/v3/servicedescriptions/jzufi",
        produces = { MEDIA_TYPE_JSON_22, MEDIA_TYPE_JSON_23, MediaType.APPLICATION_JSON_VALUE }
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Gibt das ursprüngliche XZuFi als JSON (JZuFi) der Leistung in angefragter Version zurück. Es werden Accept-Headers application/json; xzufi-version=2.2.0 oder application/json; xzufi-version=2.3.1 unterstützt.",
                content = {
                    @Content(
                        mediaType = MEDIA_TYPE_JSON_22,
                        schema = @Schema(implementation = Leistung.class),
                        examples = {
                            @ExampleObject(
                                name = "Leistung im JZuFi 2.2.0 Format",
                                description = "in diesem Beispiel: Ingenieur / Ingenieurin: Erlaubnis zum Führen der Berufsbezeichnung",
                                value = Example.RESPONSE_EXAMPLE_JZUFI_LEISTUNG_22
                            ),
                        }
                    ),
                    @Content(
                        mediaType = MEDIA_TYPE_JSON_23,
                        schema = @Schema(implementation = de.xoev.xzufi.v2_3_1.Leistung.class),
                        examples = {
                            @ExampleObject(
                                name = "Leistung im JZuFi 2.3.1 Format",
                                description = "in diesem Beispiel: Ingenieur / Ingenieurin: Erlaubnis zum Führen der Berufsbezeichnung",
                                value = Example.RESPONSE_EXAMPLE_JZUFI_LEISTUNG_23
                            ),
                        }
                    ),
                }
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Es wurde kein Ergebnis gefunden.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
        }
    )
    @Operation(
        summary = "JZuFi der Leistung in angefragter Version",
        description = "Gibt das ursprüngliche XZuFi als JSON (JZuFi) der Leistungsbeschreibung in angefragter Version zurück. Es werden Accept-Headers application/json; xzufi-version=2.2.0 oder application/json; xzufi-version=2.3.1 unterstützt. Wenn die Auslieferungsversion der Daten von der Version abweicht, in der die Daten an das PVOG geliefert wurden, werden die ausgelieferten Daten entsprechend konvertiert."
    )
    public ResponseEntity<Serializable> getJzufiLbV3(
        @Parameter(hidden = true) @RequestHeader(value = "Accept") MediaType accept,
        @Parameter(
            description = "Die ID der Leistungsbeschreibung, deren Details geladen werden sollen",
            example = "L100012.LB.9413106",
            required = true
        ) @RequestParam(name = "id") @Size(min = 1, max = 255) String id,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validdate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate
    ) {
        if (!endpointV3ServicedescriptionsJzufiActive) {
            throw new SuchenException(
                ErrorCodeEnum.NOT_FOUND,
                "Der Endpunkt '/v3/servicedescriptions/jzufi' ist nicht länger aktiv."
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
        return serviceDescriptionService
            .fetchJzufi(id, validDate)
            .map(genericLeistung ->
                ResponseEntity.ofNullable(
                    version == XZuFiVersion.XZUFI22
                        ? genericLeistung.getOrConvertXZuFi22()
                        : genericLeistung.getOrConvertXZuFi23()
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
        value = "/v2/servicedescriptions/{id}/jzufi-2-3",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "JZuFi der Leistung in der XZuFi-Version 2.3.1",
        description = "Liefert das Suchdienst-eigene JZuFi-Ausgabemodell. Zuständigkeiten verwenden den Discriminator @type. Daten anderer XZuFi-Versionen werden nach 2.3.1 konvertiert."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Leistung für ID gefunden",
        content = @Content(
            schema = @Schema(implementation = de.dataport.suchdienst.jzufi.model.v23.Leistung.class)
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Leistung nicht gefunden",
        content = @Content(schema = @Schema(implementation = ApiErrorModel.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Ungültige ID.",
        content = @Content(schema = @Schema(implementation = ApiErrorModel.class))
    )
    public de.dataport.suchdienst.jzufi.model.v23.Leistung getJzufiLb231V2(
        @PathVariable @Parameter(
            description = "Die ID der Leistungsbeschreibung",
            required = true
        ) @Size(min = 1, max = 255) String id
    ) {
        return serviceDescriptionService
            .fetchJzufi(id, null)
            .map(GenericLeistung::getOrConvertXZuFi23)
            .map(JzufiModelMapper.INSTANCE::map23)
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage(CODE_API_404, null, Locale.GERMAN)
                )
            );
    }

    private static class Example {

        public static final String RESPONSE_EXAMPLE_JZUFI_LEISTUNG_22 = """
        {
            "id": {
                "value": "9413106",
                "schemeAgencyID": "L100012",
                "schemeID": "OID"
            },
            "idSekundaer": [
                {
                    "value": "99147006067000",
                    "schemeAgencyID": "GK_LEIKA",
                    "schemeAgencyName": "GK LeiKa",
                    "schemeID": "LEIKA_LEISTUNG_SCHLUESSEL",
                    "schemeName": "LeiKa Leistungsschlüssel",
                    "schemeVersionID": "2"
                }
            ],
            "struktur": {
                "leistungsobjektID": {
                    "value": "01147006000000",
                    "schemeAgencyID": "GK_LEIKA",
                    "schemeAgencyName": "GK LeiKa",
                    "schemeID": "LEIKA_LEISTUNG_SCHLUESSEL",
                    "schemeName": "LeiKa Leistungsschlüssel",
                    "schemeVersionID": "2"
                }
            },
            "referenzLeiKa": [
                {
                    "code": "99147006067000",
                    "listURI": "urn:de:fim:leika:leistung",
                    "listVersionID": "20180705"
                }
            ],
            "modulText": [
                {
                    "inhalt": [
                        {
                            "value": "Engineer: Permission to take the professional title",
                            "languageCode": "en"
                        },
                        {
                            "value": "Ingenieur / Ingenieurin: Erlaubnis zum Führen der Berufsbezeichnung",
                            "languageCode": "de"
                        }
                    ],
                    "leikaTextmodul": {
                        "code": "02",
                        "listURI": "urn:de:xzufi:codeliste:leistungstextmodulleika",
                        "listVersionID": "20190605"
                    },
                    "leikaTextModulAbweichendeBezeichnung": [],
                    "weiterfuehrenderLink": [],
                    "idSekundaer": [],
                    "gueltigkeit": []
                },
                {
                    "inhalt": [
                        {
                            "value": "<p>Die Berufsbezeichnung &#34;Ingenieurin&#34; oder &#34;Ingenieur&#34; ist nach dem Ingenieurgesetz Schleswig-Holstein (IngG) gesetzlich geschützt. Diesen Titel - allein oder in einer Wortverbindung - darf führen,</p> <ol><li>wer ein Studium einer technischen oder technisch-naturwissenschaftlichen Fachrichtung mit einer Regelstudienzeit von mindestens sechs Semestern in Vollzeit oder während einer entsprechenden Gesamtdauer in Teilzeit an einer deutschen, staatlichen oder staatlich anerkannten Universität, Fachhochschule oder Berufsakademie mit Erfolg abgeschlossen hat, wobei dieser Studiengang überwiegend von den Bereichen Mathematik, Informatik, Naturwissenschaften und Technik geprägt sein muss; für die Bezeichnung &#34;Wirtschaftsingenieurin&#34; oder &#34;Wirtschaftsingenieur&#34; muss der Studiengang von diesen Fächern zumindest geprägt sein,</li><li>wer von der zuständigen Stelle die Genehmigung hierzu erhalten hat,</li><li>wer nach dem Recht eines anderen Landes der Bundesrepublik Deutschland zur Führung dieser Berufsbezeichnung berechtigt ist oder</li><li>wer bis zum Inkrafttreten dieses Gesetzes bereits berechtigt war, die Berufsbezeichnung zu führen.</li>/ol> <p>Bezeichnungen, die auf wirtschaftlich tätige Zusammenschlüsse (&#34;Ingenieurbüro&#34; o.ä.) hinweisen, dürfen in Verbindung mit der Berufsbezeichnung nach Absatz 1 oder ähnlichen Bezeichnungen nur geführt werden, wenn mindestens die Hälfte der Mitglieder des Vorstands, der Geschäftsführung oder der Personen, die mindestens über die Hälfte der Stimmrechte verfügen, zur Führung der Berufsbezeichnung berechtigt sind.</p> <p>Eine Genehmigung können Personen aus einem Mitgliedstaat der Europäischen Union, einem Vertragsstaat des Abkommens über den Europäischen Wirtschaftsraum oder einem sonstigen durch Abkommen gleichgestellten Staat (Mitglieds- oder Vertragsstaat) beantragen, wenn sie in Schleswig-Holstein ihre Hauptwohnung, ihre Hauptniederlassung oder ihre überwiegende berufliche Beschäftigung haben.</p> <p>Die für eine Genehmigung erforderlichen Befähigungs- oder Ausbildungsnachweise und/oder Berufserfahrungen hängen davon ab, ob der Beruf im Herkunftsland reglementiert ist oder nicht.</p> <p>Die zuständige Stelle (s.u.) kann bei wesentlichen Qualifikationsunterschieden Ausgleichsmaßnahmen in Form von Anpassungslehrgängen oder Eignungsprüfungen verlangen.</p> <p>Rechtfertigen Tatsachen die Annahme, dass die erforderliche fachliche Kenntnis fehlt oder Leben und Gesundheit von Menschen erheblich gefährdet sind, dann kann das Führen der Berufsbezeichnung durch die zuständige Behörde untersagt werden.</p> <p>Eine Genehmigung können Personen aus einem Mitgliedstaat der Europäischen Union, einem Vertragsstaat des Abkommens über den Europäischen Wirtschaftsraum oder einem sonstigen Abkommen gleichgestellten</p> <p>Rechtfertigen Tatsachen die Annahme, dass die erforderliche fachliche Kenntnis fehlt oder Leben und Gesundheit von Menschen erheblich gefährdet sind, dann kann das Führen der Berufsbezeichnung durch die zuständige Behörde untersagt werden.</p>",
                            "languageCode": "de"
                        },
                        {
                            "value": "<p>The professional title &#34;engineer&#34; or &#34;engineer&#34; is protected by law under the Schleswig-Holstein Engineering Act (IngG). This title may lead to this title, alone or in a word connection,</p> <ol><li>who has successfully completed a degree in a technical or technical-natural science discipline with a standard period of study of at least six semesters full-time or for a corresponding total period of part-time at a German, state or state-recognised university, university of applied sciences or vocational academy, the main degree of which must be in the fields of mathematics, computer science, natural sciences and technology; for the term &#34;industrial engineer&#34; or &#34;industrial engineer&#34; the course of study must at least be shaped by these subjects,</li><li>who has received authorisation from the competent body to do so,</li><li>who is entitled under the law of another country of the Federal Republic of Germany to take that professional title, or</li><li>who was already entitled to hold the professional title until this Act came into force.</li></ol> <p>Designations indicating economically active mergers (&#34;engineering office&#34; or similar) may be used in conjunction with the professional title referred to in paragraph 1 or similar designations only if at least half of the members of the management board, management or persons holding at least half of the voting rights are entitled to take the professional title.</p> <p>Persons from a Member State of the European Union, a Contracting State to the Agreement on the European Economic Area or any other State (Member State or Contracting State) assised by agreements may apply for authorisation if they have their principal residence, principal place of business or predominant professional employment in Schleswig-Holstein.</p> <p>The qualifications or qualifications and/or professional experience required for authorisation depend on whether the profession is regulated in the country of origin or not.</p> <p>The competent body (see, among others) may require compensatory measures in the form of adaptation courses or aptitude tests in the event of significant differences in qualifications.</p> <p>If facts justify the assumption that the necessary technical knowledge is lacking or that people&#39;s lives and health are seriously endangered, then the competent authority may prohibit the taking of the professional title.</p> <p>Authorisation may be granted to persons from a Member State of the European Union, a State Party to the Agreement on the European Economic Area or any other agreement</p> <p>If facts justify the assumption that the necessary technical knowledge is lacking or that people&#39;s lives and health are seriously endangered, then the competent authority may prohibit the taking of the professional title.</p>",
                            "languageCode": "en"
                        }
                    ],
                    "leikaTextmodul": {
                        "code": "06",
                        "listURI": "urn:de:xzufi:codeliste:leistungstextmodulleika",
                        "listVersionID": "20190605"
                    },
                    "leikaTextModulAbweichendeBezeichnung": [],
                    "weiterfuehrenderLink": [],
                    "positionDarstellung": 1,
                    "idSekundaer": [],
                    "gueltigkeit": []
                },
                {
                    "inhalt": [
                        {
                            "value": "<p>An die Architekten- und Ingenieurkammer Schleswig-Holstein (AIK).</p>",
                            "languageCode": "de"
                        },
                        {
                            "value": "<p>To the Chamber of Architects and Engineers Schleswig-Holstein (AIK).</p>",
                            "languageCode": "en"
                        }
                    ],
                    "leikaTextmodul": {
                        "code": "23",
                        "listURI": "urn:de:xzufi:codeliste:leistungstextmodulleika",
                        "listVersionID": "20190605"
                    },
                    "leikaTextModulAbweichendeBezeichnung": [],
                    "weiterfuehrenderLink": [],
                    "positionDarstellung": 4,
                    "idSekundaer": [],
                    "gueltigkeit": []
                },
                {
                    "inhalt": [
                        {
                            "value": "<p>Wer die Berufsbezeichnung &#34;Ingenieurin / Ingenieur&#34; führen möchte, benötigt eine Erlaubnis.</p>",
                            "languageCode": "de"
                        },
                        {
                            "value": "<p>If you want to use the professional title &#34;Engineer&#34;/ an engineer, you need a permit.</p>",
                            "languageCode": "en"
                        }
                    ],
                    "leikaTextmodul": {
                        "code": "05",
                        "listURI": "urn:de:xzufi:codeliste:leistungstextmodulleika",
                        "listVersionID": "20190605"
                    },
                    "leikaTextModulAbweichendeBezeichnung": [],
                    "weiterfuehrenderLink": [],
                    "positionDarstellung": 17,
                    "idSekundaer": [],
                    "gueltigkeit": []
                },
                {
                    "inhalt": [
                        {
                            "value": "<p>Wer die Berufsbezeichnung &#34;Ingenieur/Ingenieurin&#34; führt, ohne dazu berechtigt zu sein, handelt ordnungswidrig. Die Ordnungswidrigkeit kann mit einer Geldbuße geahndet werden.</p> <p>Das Recht zum Führen akademischer Grade bleibt unberührt.</p>",
                            "languageCode": "de"
                        },
                        {
                            "value": "<p>Anyone who holds the professional title &#34;engineer&#34; without being entitled to do so is acting in an irregular manner. The offence may be punished by a fine.</p> <p>The right to conduct academic degrees remains unaffected.</p>",
                            "languageCode": "en"
                        }
                    ],
                    "leikaTextmodul": {
                        "code": "16",
                        "listURI": "urn:de:xzufi:codeliste:leistungstextmodulleika",
                        "listVersionID": "20190605"
                    },
                    "leikaTextModulAbweichendeBezeichnung": [],
                    "weiterfuehrenderLink": [],
                    "positionDarstellung": 14,
                    "idSekundaer": [],
                    "gueltigkeit": []
                },
                {
                    "inhalt": [
                        {
                            "value": "<ul><li>EG-Richtlinie 2005/36 über die Anerkennung von Berufsqualifikationen,</li><li>Gesetz zum Schutze der Berufsbezeichnung &#34;Ingenieur&#34; (Ingenieurgesetz - IngG).</li></ul>",
                            "languageCode": "de"
                        },
                        {
                            "value": "<ul><li>EC Directive 2005/36 on the recognition of professional qualifications,</li><li>Law on the Protection of the Professional Title &#34;Engineer&#34; (Engineering Law - IngG).</li></ul>",
                            "languageCode": "en"
                        }
                    ],
                    "leikaTextmodul": {
                        "code": "07",
                        "listURI": "urn:de:xzufi:codeliste:leistungstextmodulleika",
                        "listVersionID": "20190605"
                    },
                    "leikaTextModulAbweichendeBezeichnung": [],
                    "weiterfuehrenderLink": [
                        {
                            "uri": "http://www.gesetze-rechtsprechung.sh.juris.de/jportal/?quelle=jlink&query=IngG+SH&psml=bsshoprod.psml&max=true&aiz=true",
                            "titel": "IngG",
                            "beschreibung": "Keine weiteren Hinweise vorhanden",
                            "positionDarstellung": 0,
                            "languageCode": "de"
                        },
                        {
                            "uri": "http://eur-lex.europa.eu/LexUriServ/LexUriServ.do?uri=OJ:L:2005:255:0022:0142:de:PDF",
                            "titel": "EC Directive 2005/36",
                            "beschreibung": "Keine weiteren Hinweise vorhanden",
                            "positionDarstellung": 1,
                            "languageCode": "en"
                        },
                        {
                            "uri": "http://www.gesetze-rechtsprechung.sh.juris.de/jportal/?quelle=jlink&query=IngG+SH&psml=bsshoprod.psml&max=true&aiz=true",
                            "titel": "IngG",
                            "beschreibung": "Keine weiteren Hinweise vorhanden",
                            "positionDarstellung": 2,
                            "languageCode": "en"
                        },
                        {
                            "uri": "http://eur-lex.europa.eu/LexUriServ/LexUriServ.do?uri=OJ:L:2005:255:0022:0142:de:PDF",
                            "titel": "EG-Richtlinie 2005/36",
                            "beschreibung": "Keine weiteren Hinweise vorhanden",
                            "positionDarstellung": 3,
                            "languageCode": "de"
                        }
                    ],
                    "positionDarstellung": 11,
                    "idSekundaer": [],
                    "gueltigkeit": []
                }
            ],
            "modulTextIndividuell": [],
            "modulBegriffImKontext": [],
            "modulUrsprungsportal": [
                {
                    "uri": "https://zufish.schleswig-holstein.de/portaldeeplink?tsa_leistung_id=9413106&tsa_sprache=en_EN",
                    "titel": "Engineer: Permission to take the professional title",
                    "positionDarstellung": 1,
                    "idSekundaer": [],
                    "gueltigkeit": []
                },
                {
                    "uri": "https://zufish.schleswig-holstein.de/portaldeeplink?tsa_leistung_id=9413106&tsa_sprache=de_DE",
                    "titel": "Ingenieur / Ingenieurin: Erlaubnis zum Führen der Berufsbezeichnung",
                    "positionDarstellung": 2,
                    "idSekundaer": [],
                    "gueltigkeit": []
                }
            ],
            "typisierung": [
                {
                    "code": "4",
                    "name": "Regelungskompetenz auf Landesebene",
                    "listURI": "urn:de:fim:leika:typisierung",
                    "listVersionID": "20201204"
                }
            ],
            "leistungsadressat": [],
            "kennzeichenSchriftformerfordernis": false,
            "kategorie": [
                {
                    "klasse": {
                        "id": {
                            "value": "Lebenslage",
                            "schemeAgencyID": "L100012",
                            "schemeID": "TELEPORT_LKK"
                        },
                        "idSekundaer": [],
                        "bezeichnung": [
                            {
                                "value": "Lebens- und Unternehmenslage",
                                "languageCode": "de"
                            }
                        ],
                        "beschreibung": []
                    },
                    "id": {
                        "value": "8935715",
                        "schemeAgencyID": "L100012",
                        "schemeID": "OID"
                    },
                    "idSekundaer": [],
                    "uebergeordneteKategorieID": {
                        "value": "8935703",
                        "schemeAgencyID": "L100012",
                        "schemeID": "OID"
                    },
                    "untergeordneteKategorieID": [],
                    "bezeichnung": [
                        {
                            "value": "Genehmigungen/ Bescheinigungen",
                            "languageCode": "de"
                        }
                    ],
                    "beschreibung": [
                        {
                            "value": "<p>Genehmigungen/ Bescheinigungen</p>",
                            "languageCode": "de"
                        }
                    ]
                }
            ],
            "klassifizierung": [],
            "informationsbereichSDG": [],
            "idLeistungImKontext": [],
            "idProzess": [],
            "gueltigkeitGebietID": [
                {
                    "value": "010000000000",
                    "schemeID": "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:bundesland",
                    "schemeVersionID": "2018-07-31"
                }
            ],
            "kennzeichenEA": false,
            "relevantFuerWirtschaftszweig": [],
            "relevantFuerRechtsform": [],
            "relevantFuerStaatsangehoerigkeit": [],
            "versionsinformation": {
                "geaendertDatumZeit": "2021-03-11T12:47:51+01:00"
            },
            "sprachversion": [
                {
                    "languageCode": "de",
                    "sprachbezeichnungDeutsch": "Deutsch",
                    "sprachbezeichnungNativ": "Deutsch"
                }
            ],
            "gueltigkeit": []
        }""";

        public static final String RESPONSE_EXAMPLE_JZUFI_LEISTUNG_23 = """
                        {
                            "id": {
                                "value": "9413106",
                                "schemeAgencyID": "L100012",
                                "schemeID": "OID"
                            },
                            "idSekundaer": [],
                            "struktur": {
                                "leistungsobjektID": {
                                    "value": "99150085000000",
                                    "schemeAgencyID": "GK_LEIKA",
                                    "schemeAgencyName": "GK LeiKa",
                                    "schemeID": "LEIKA_LEISTUNG_SCHLUESSEL",
                                    "schemeName": "LeiKa Leistungsschlüssel",
                                    "schemeVersionID": "2"
                                }
                            },
                            "referenzLeistungsschluessel": [{
                                    "value": "99150085001000",
                                    "schemeURI": "urn:de:fim:leika:leistung",
                                    "schemeVersionID": "20220301"
                                }
                            ],
                            "leistungsadressat": [],
                            "modulText": [{
                                    "zugangskanal": {
                                        "code": "000",
                                        "listURI": "urn:xoev-de:fim:codeliste:xzufi.zugangskanal",
                                        "listVersionID": "1"
                                    },
                                    "idSekundaer": [],
                                    "gueltigkeit": [],
                                    "inhalt": [{
                                            "value": "Ingenieurin und Ingenieur mit ausländischer Berufsqualifikation, Erlaubnis zum Führen der Berufsbezeichnung \\"Ingenieurin\\" oder \\"Ingenieur\\" beantragen",
                                            "html": true,
                                            "languageCode": "de"
                                        }, {
                                            "value": "Engineer with foreign professional qualification, apply for permission to use the professional title \\"engineer",
                                            "html": true,
                                            "languageCode": "en"
                                        }
                                    ],
                                    "leikaTextmodul": {
                                        "code": "02",
                                        "listURI": "urn:xoev-de:fim:codeliste:xzufi.leistungstextmodul",
                                        "listVersionID": "20240101"
                                    },
                                    "leikaTextModulAbweichendeBezeichnung": [],
                                    "weiterfuehrenderLink": []
                                }, {
                                    "zugangskanal": {
                                        "code": "000",
                                        "listURI": "urn:xoev-de:fim:codeliste:xzufi.zugangskanal",
                                        "listVersionID": "1"
                                    },
                                    "idSekundaer": [],
                                    "gueltigkeit": [],
                                    "inhalt": [{
                                            "value": "Ingenieurin und Ingenieur mit ausländischer Berufsqualifikation, Erlaubnis zum Führen der Berufsbezeichnung \\"Ingenieurin\\" oder \\"Ingenieur\\" beantragen",
                                            "html": true,
                                            "languageCode": "de"
                                        }, {
                                            "value": "Engineer with foreign professional qualification, apply for permission to use the professional title \\"engineer",
                                            "html": true,
                                            "languageCode": "en"
                                        }
                                    ],
                                    "leikaTextmodul": {
                                        "code": "03",
                                        "listURI": "urn:xoev-de:fim:codeliste:xzufi.leistungstextmodul",
                                        "listVersionID": "20240101"
                                    },
                                    "leikaTextModulAbweichendeBezeichnung": [],
                                    "weiterfuehrenderLink": []
                                }, {
                                    "zugangskanal": {
                                        "code": "000",
                                        "listURI": "urn:xoev-de:fim:codeliste:xzufi.zugangskanal",
                                        "listVersionID": "1"
                                    },
                                    "positionDarstellung": 1,
                                    "idSekundaer": [],
                                    "gueltigkeit": [],
                                    "inhalt": [{
                                            "value": "<p>The profession of engineer is regulated in Germany. The job title is particularly protected. This means: You must prove a specific qualification in order to be allowed to use the professional title \\"Ingenieurin\\" or \\"Ingenieur\\". To prove a foreign qualification, you can apply for recognition of your professional qualification. You can also submit the application for the recognition procedure from abroad.</p> <p>In the recognition procedure, the competent body compares your professional qualification from abroad with the professional qualification in the chosen federal state. The procedure is called: equivalence determination. You will receive a decision on the result of the equivalence assessment. The notice will state your professional qualifications. If you lack professional qualifications for recognition, the notice will also state the main differences.</p> <p>In order to use the professional title \\"engineer\\", you must fulfill other requirements in addition to the equivalence of the professional qualifications. You may have to prove these requirements at a later date:</p> <ul> <li>Main residence, establishment or predominant work in the chosen federal state. Or: You want to live or work in the chosen federal state soon.</li> <li>Personal suitability</li> </ul> <p>If your qualifications are equivalent and you meet all other requirements, you may use the professional title \\"engineer\\".</p>",
                                            "html": true,
                                            "languageCode": "en"
                                        }, {
                                            "value": "<p>Der Beruf Ingenieurin und Ingenieur ist in Deutschland reglementiert. Die Berufsbezeichnung ist besonders geschützt. Das bedeutet: Sie müssen eine spezifische Qualifikation nachweisen, um die Berufsbezeichnung \\"Ingenieurin\\" oder \\"Ingenieur\\" führen zur dürfen. Für den Nachweis einer ausländischen Qualifikation können Sie die Anerkennung Ihrer Berufsqualifikation beantragen. Sie können den Antrag für das Anerkennungsverfahren auch aus dem Ausland stellen.</p> <p>Im Anerkennungsverfahren vergleicht die zuständige Stelle Ihre Berufsqualifikation aus dem Ausland mit der Berufsqualifikation in dem gewählten Bundesland. Das Verfahren heißt: Gleichwertigkeitsfeststellung. Über das Ergebnis der Gleichwertigkeitsfeststellung erhalten Sie einen Bescheid. Der Bescheid nennt Ihre beruflichen Qualifikationen. Wenn Ihnen für eine Anerkennung berufliche Qualifikationen fehlen, nennt der Bescheid auch die wesentlichen Unterschiede.</p> <p>Für das Führen der Berufsbezeichnung \\"Ingenieurin\\" oder \\"Ingenieur\\" müssen Sie neben der Gleichwertigkeit der Berufsqualifikation noch weitere Voraussetzungen erfüllen. Diese Voraussetzungen müssen Sie vielleicht erst zu einem späteren Zeitpunkt nachweisen:</p> <ul> <li>Hauptwohnsitz, Niederlassung oder überwiegende Arbeit in dem gewählten Bundesland. Oder: Sie wollen bald in dem gewählten Bundesland wohnen oder arbeiten.</li> <li>Persönliche Eignung</li> </ul> <p>Ist Ihre Qualifikation gleichwertig und Sie erfüllen alle weiteren Voraussetzungen, dürfen Sie die Berufsbezeichnung \\"Ingenieurin\\" oder \\"Ingenieur\\" führen.</p>",
                                            "html": true,
                                            "languageCode": "de"
                                        }
                                    ],
                                    "leikaTextmodul": {
                                        "code": "06",
                                        "listURI": "urn:xoev-de:fim:codeliste:xzufi.leistungstextmodul",
                                        "listVersionID": "20240101"
                                    },
                                    "leikaTextModulAbweichendeBezeichnung": [],
                                    "weiterfuehrenderLink": []
                                }
                            ],
                            "modulTextIndividuell": [],
                            "modulFrist": [],
                            "modulBearbeitungsdauer": [],
                            "modulKosten": [{
                                    "zugangskanal": {
                                        "code": "000",
                                        "listURI": "urn:xoev-de:fim:codeliste:xzufi.zugangskanal",
                                        "listVersionID": "1"
                                    },
                                    "idSekundaer": [],
                                    "gueltigkeit": [],
                                    "kosten": [],
                                    "beschreibung": [{
                                            "value": "<p>Zusätzlich können weitere Kosten entstehen (z. B. für Übersetzungen oder Beglaubigungen Ihrer Dokumente oder Ausgleichsmaßnahmen). Diese Kosten sind individuell unterschiedlich.</p>",
                                            "html": true,
                                            "languageCode": "de"
                                        }, {
                                            "value": "<p>In addition, further costs may arise (e.g. for translations or certifications of your documents or compensation measures). These costs vary individually.</p>",
                                            "html": true,
                                            "languageCode": "en"
                                        }
                                    ],
                                    "weiterfuehrenderLink": []
                                }
                            ],
                            "modulDokument": [],
                            "modulBegriffImKontext": [{
                                    "zugangskanal": {
                                        "code": "000",
                                        "listURI": "urn:xoev-de:fim:codeliste:xzufi.zugangskanal",
                                        "listVersionID": "1"
                                    },
                                    "idSekundaer": [],
                                    "gueltigkeit": [],
                                    "begriffImKontext": [{
                                            "begriff": {
                                                "value": "Foreign qualification",
                                                "languageCode": "de"
                                            },
                                            "typ": {
                                                "code": "001",
                                                "listURI": "urn:xoev-de:fim:codeliste:xzufi.begriffimkontexttyp",
                                                "listVersionID": "3"
                                            }
                                        }
                                    ]
                                }
                            ],
                            "modulFachlicheFreigabe": [{
                                    "zugangskanal": {
                                        "code": "000",
                                        "listURI": "urn:xoev-de:fim:codeliste:xzufi.zugangskanal",
                                        "listVersionID": "1"
                                    },
                                    "idSekundaer": [],
                                    "gueltigkeit": [],
                                    "fachlichFreigegebenAm": "2021-09-30",
                                    "fachlichFreigegebenDurch": [{
                                            "value": "Federal Institute for Vocational Education and Training Saxony-Anhalt Chamber of Engineers",
                                            "languageCode": "en"
                                        }, {
                                            "value": "Bundesinstitut für Berufsbildung Ingenieurkammer Sachsen-Anhalt",
                                            "languageCode": "de"
                                        }
                                    ]
                                }
                            ],
                            "modulAuskunftshinweis": [],
                            "modulUrsprungsportal": [{
                                    "zugangskanal": {
                                        "code": "000",
                                        "listURI": "urn:xoev-de:fim:codeliste:xzufi.zugangskanal",
                                        "listVersionID": "1"
                                    },
                                    "positionDarstellung": 1,
                                    "idSekundaer": [],
                                    "gueltigkeit": [],
                                    "uri": "https://zufish.schleswig-holstein.de/portaldeeplink?tsa_leistung_id=9413106&tsa_sprache=de_DE",
                                    "titel": "Ingenieurin und Ingenieur mit ausländischer Berufsqualifikation, Erlaubnis zum Führen der Berufsbezeichnung „Ingenieurin“ oder „Ingenieur“ beantragen",
                                    "dynamischeParameter": [],
                                    "languageCode": "de"
                                }
                            ],
                            "typisierung": [{
                                    "code": "4",
                                    "name": "Regelungskompetenz auf Landesebene",
                                    "listURI": "urn:xoev-de:fim:codeliste:xzufi.typisierung",
                                    "listVersionID": "20240101"
                                }
                            ],
                            "kennzeichenFormularSchriftformerfordernis": false,
                            "kategorie": [],
                            "klassifizierung": [{
                                    "code": "1040400",
                                    "listURI": "urn:xoev-de:fim:codeliste:pvlagen",
                                    "listVersionID": "20211001"
                                }
                            ],
                            "informationsbereicheSDG": {
                                "informationsbereichSDG1": [{
                                        "code": "1020300",
                                        "listURI": "urn:xoev-de:fim:codeliste:xzufi.sdginformationsbereich1",
                                        "listVersionID": "1"
                                    }
                                ],
                                "informationsbereichSDG2": []
                            },
                            "leistungImKontextID": [],
                            "prozessID": [],
                            "gueltigkeitGebietID": [{
                                    "value": "010000000000",
                                    "schemeID": "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:bundesland",
                                    "schemeVersionID": "2010-04-01"
                                }
                            ],
                            "durchfuehrbarEinheitlicheStelle": [{
                                    "code": "11",
                                    "listURI": "urn:xoev-de:fim:codeliste:xzufi.einheitlichestelletyp",
                                    "listVersionID": "1"
                                }
                            ],
                            "relevantFuerWirtschaftszweig": [],
                            "relevantFuerRechtsform": [],
                            "relevantFuerStaatsangehoerigkeit": {
                                "staatsangehoerigkeit": [],
                                "staatengruppe": []
                            },
                            "versionsinformation": {
                                "geaendertDatumZeit": "2024-05-31"
                            },
                            "sprachversion": [{
                                    "languageCode": "de",
                                    "sprachbezeichnungDeutsch": "Deutsch",
                                    "sprachbezeichnungNativ": "Deutsch",
                                    "kennzeichenMaschinelleUebersetzung": false
                                }, {
                                    "languageCode": "en",
                                    "sprachbezeichnungDeutsch": "Englisch",
                                    "sprachbezeichnungNativ": "English",
                                    "kennzeichenMaschinelleUebersetzung": false
                                }
                            ],
                            "gueltigkeit": []
                        }

        """;
    }
}
