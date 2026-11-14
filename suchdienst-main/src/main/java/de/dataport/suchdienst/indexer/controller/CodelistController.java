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
package de.dataport.suchdienst.indexer.controller;

import static de.dataport.suchdienst.config.CacheConfiguration.CACHE_NAME_CODELIST_DOWNLOAD;
import static de.dataport.suchdienst.config.CacheConfiguration.CACHE_NAME_CODELIST_ENTRIES;

import de.dataport.suchdienst.aop.AntivirusMethod;
import de.dataport.suchdienst.config.AuthorityRoles;
import de.dataport.suchdienst.config.CacheConfiguration;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.config.conditions.OnMasterCondition;
import de.dataport.suchdienst.error.SuchdienstException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ErrorModel;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@Slf4j
@RestController
@RequestMapping("admin/codelist")
@Tag(name = "Codelist")
@ApiAudienceMarker(ApiAudience.INTERNAL)
@Conditional(OnMasterCondition.class)
@SecurityRequirement(name = "pvog_auth")
public class CodelistController {

    private final MessageSource messageSource;
    private final CodelistService codelistService;

    public CodelistController(CodelistService codelistService, MessageSource messageSource) {
        this.codelistService = codelistService;
        this.messageSource = messageSource;
    }

    @GetMapping("entries")
    @Operation(
        summary = "Ruft die zu einer Codeliste gehörenden Einträge ab",
        description = "Ruft die zu einer Codeliste gehörenden Einträge ab"
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine Liste mit Codelisteneinträgen. Die Liste kann auch leer sein.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = CodelistEntry.class)),
                    examples = {
                        @ExampleObject(
                            name = "Codelisteneinträge",
                            value = """
                            [
                              {
                                "id": "urn:de:xzufi:codeliste:leistungstextmodulleika_20201201_02",
                                "uri": "urn:de:xzufi:codeliste:leistungstextmodulleika",
                                "version": "20201201",
                                "code": "02",
                                "texts": {
                                  "de": "Leistungsbezeichnung",
                                  "en": "Service description"
                                },
                                "comments": {},
                                "values": [],
                                "key": {
                                  "uri": "urn:de:xzufi:codeliste:leistungstextmodulleika",
                                  "version": "20201201",
                                  "code": "02"
                                }
                              },
                              {
                                "id": "urn:de:xzufi:codeliste:leistungstextmodulleika_20201201_03",
                                "uri": "urn:de:xzufi:codeliste:leistungstextmodulleika",
                                "version": "20201201",
                                "code": "03",
                                "texts": {
                                  "de": "Leistungsbezeichnung 2",
                                  "en": "Service description 2"
                                },
                                "comments": {},
                                "values": [],
                                "key": {
                                  "uri": "urn:de:xzufi:codeliste:leistungstextmodulleika",
                                  "version": "20201201",
                                  "code": "02"
                                }
                              }
                            ]
                            """
                        ),
                    }
                )
            ),
        }
    )
    @PreAuthorize(AuthorityRoles.Codelist.READ_ENTRIES)
    public ResponseEntity<List<CodelistEntry>> getCodelistEntries(
        @Parameter(
            name = "uri",
            description = "URI der Codeliste, die heruntergeladen werden soll. Bspw. urn:de:xzufi:codeliste:leistungstextmodulleika",
            required = true
        ) @RequestParam(name = "uri") URI uri,
        @Parameter(
            description = "Die Version der Codeliste, die heruntergeladen werden soll.",
            name = "version",
            required = true
        ) @RequestParam(name = "version") String version
    ) {
        return ResponseEntity.ok().body(
            codelistService.getCodelistEntries(uri.toString(), version)
        );
    }

    @GetMapping("{id}")
    @Operation(
        summary = "Ruft einen Codelisten-Eintrag anhand der übergebenen ID ab",
        description = "Ruft einen Codelisten-Eintrag anhand der übergebenen ID ab"
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Der gesuchte Codelisteneintrag",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CodelistEntry.class),
                    examples = {
                        @ExampleObject(
                            name = "Codelisteintrag",
                            value = """
                            {
                              "id": "urn:de:xzufi:codeliste:leistungstextmodulleika_20201201_02",
                              "uri": "urn:de:xzufi:codeliste:leistungstextmodulleika",
                              "version": "20201201",
                              "code": "02",
                              "texts": {
                                "de": "Leistungsbezeichnung",
                                "en": "Service description"
                              },
                              "comments": {},
                              "values": [],
                              "key": {
                                "uri": "urn:de:xzufi:codeliste:leistungstextmodulleika",
                                "version": "20201201",
                                "code": "02"
                              }
                            }
                            """
                        ),
                    }
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Es wurde zu der ID kein Codelisteneintrag gefunden",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
        }
    )
    @PreAuthorize(AuthorityRoles.Codelist.READ_ID)
    public ResponseEntity<CodelistEntry> getCodelistEntryById(
        @PathVariable @Parameter(
            description = "Die ID der Codeliste, die abgerufen werden soll.",
            name = "id",
            required = true
        ) String id
    ) {
        CodelistEntry entry = codelistService
            .getCodelistById(id)
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage("api.404", new String[] { id }, Locale.GERMAN)
                )
            );
        return ResponseEntity.ok().body(entry);
    }

    @GetMapping("download")
    @Operation(
        summary = "Ergänzt die Suchdienst Datenbank um eine Codeliste aus dem xRepository anhand der URI und der Version. ",
        description = "Ergänzt die Suchdienst Datenbank um eine Codeliste aus dem xRepository anhand der URI und der Version. " +
            "Wenn keine Version angegeben wird, dann wird die aktuelle Version ermittelt. " +
            "Bereits vorhandene Einträge werden überschrieben."
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Der Download war erfolgreich",
                content = @Content
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Fehler beim Download oder Speichern der Codeliste",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
        }
    )
    @PreAuthorize(AuthorityRoles.Codelist.READ_URI)
    @Caching(
        // Manuelle administration der Codelisten müssen den Cache zurücksetzen, da davon ausgegangen werden muss,
        // dass falsche Daten ins System gebracht wurden. Ggf. könnten diese Daten dann sowohl bei uns, als auch
        // aufseiten des XRepositorys behoben werden. In diesem Falle wollen wir keine alten, falschen Daten im Cache behalten.
        evict = {
            @CacheEvict(
                cacheNames = CACHE_NAME_CODELIST_ENTRIES,
                cacheManager = CacheConfiguration.IN_MEM_MGR,
                allEntries = true
            ),
            @CacheEvict(
                cacheNames = CACHE_NAME_CODELIST_DOWNLOAD,
                cacheManager = CacheConfiguration.IN_MEM_MGR,
                allEntries = true
            ),
        }
    )
    public ResponseEntity<Object> downloadCodelist(
        @Parameter(
            description = "URI der Codeliste, die heruntergeladen werden soll. Bspw. urn:de:xzufi:codeliste:leistungstextmodulleika",
            required = true
        ) @RequestParam(name = "uri") URI uri,
        @Parameter(
            description = "Die Version der Codeliste, die heruntergeladen werden soll."
        ) @RequestParam(name = "version", required = false) String version
    ) {
        if (codelistService.downloadCodelist(uri.toString(), version) == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                messageSource.getMessage(
                    "codelist_resource.no_codelist_imported_error",
                    null,
                    Locale.GERMAN
                )
            );
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Importiert eine Codeliste im XML-Format von xRepository in ElasticSearch, sodass sie zum Indizieren zur Verfügung steht.",
        description = "Importiert eine Codeliste im XML-Format von xRepository in ElasticSearch, sodass sie zum Indizieren zur Verfügung steht."
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Der Upload war erfolgreich",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {
                        @ExampleObject(
                            name = "Upload erfolgreich",
                            value = """
                            {
                                "success": true,
                                "msg": "14 Codelisteinträge importiert."
                            }"""
                        ),
                    }
                )
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Fehler beim Upload oder Speichern der Codeliste",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {
                        @ExampleObject(
                            name = "Fehler beim Parsen der Codeliste",
                            value = "{\"success\":\"false\"," +
                                "\"msg\":\"Fehler beim Parsen der Codeliste\"}"
                        ),
                    }
                )
            ),
        }
    )
    @PreAuthorize(AuthorityRoles.Codelist.UPLOAD)
    @AntivirusMethod
    @Caching(
        // Manuelle administration der Codelisten müssen den Cache zurücksetzen, da davon ausgegangen werden muss,
        // dass falsche Daten ins System gebracht wurden. Ggf. könnten diese Daten dann sowohl bei uns, als auch
        // aufseiten des XRepositorys behoben werden. In diesem Falle wollen wir keine alten, falschen Daten im Cache behalten.
        evict = {
            @CacheEvict(
                cacheNames = CACHE_NAME_CODELIST_ENTRIES,
                cacheManager = CacheConfiguration.IN_MEM_MGR,
                allEntries = true
            ),
            @CacheEvict(
                cacheNames = CACHE_NAME_CODELIST_DOWNLOAD,
                cacheManager = CacheConfiguration.IN_MEM_MGR,
                allEntries = true
            ),
        }
    )
    public ResponseEntity<ObjectNode> uploadCodelist(
        @RequestParam(name = "file") @Parameter(
            name = "Die XML-Datei die hochgeladen werden soll",
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_XML_VALUE,
                contentSchema = @Schema(type = "string")
            )
        ) MultipartFile file
    ) {
        try (InputStream codelist = file.getInputStream()) {
            List<CodelistEntry> entries = codelistService.saveCodelistInElasticSearch(codelist);
            if (!entries.isEmpty()) {
                String result = messageSource.getMessage(
                    "codelist_resource.codelist_upload_import_success",
                    new String[] { Integer.toString(entries.size()) },
                    Locale.GERMAN
                );
                CodelistEntry next = entries.getFirst();
                log.info(
                    "Codeliste erfolgreich hochgeladen. URI: {}, Version: {}",
                    next.getUri(),
                    next.getVersion()
                );
                return ResponseEntity.ok().body(createResponse(true, result));
            } else {
                return ResponseEntity.ok().body(createResponse(true, "Leere Codeliste importiert"));
            }
        } catch (RuntimeException | IOException e) {
            // PV-8946 catch-log-return-error
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            if (e instanceof SuchdienstException suEx) {
                status = ErrorCodeEnum.mapToHttpStatus(suEx.getCode());
            }
            log.warn("Fehler beim Parsen der Codeliste", e);
            return ResponseEntity.status(status).body(
                createResponse(
                    false,
                    messageSource.getMessage(
                        "codelist_upload_import_error",
                        new String[] { e.getMessage() },
                        Locale.GERMAN
                    )
                )
            );
        }
    }

    private ObjectNode createResponse(boolean success, String message) {
        return JsonNodeFactory.instance.objectNode().put("success", success).put("msg", message);
    }
}
