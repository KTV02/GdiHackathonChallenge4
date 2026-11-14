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

import de.dataport.suchdienst.aop.AntivirusMethod;
import de.dataport.suchdienst.config.AuthorityRoles;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.config.conditions.OnMasterCondition;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexConfiguration;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ErrorModel;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.RegionalKeyKind;
import de.dataport.suchdienst.indexer.model.ZipcodeCsvDefinition;
import de.dataport.suchdienst.indexer.service.ArsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequestMapping("/admin/ars")
@Tag(name = "Upload von ARS Informationen")
@ApiAudienceMarker(ApiAudience.INTERNAL)
@Conditional(OnMasterCondition.class)
@SecurityRequirement(name = "pvog_auth")
public class ArsController {

    private final MessageSource messageSource;
    private final ArsService arsService;
    private final ElasticWriteConnector writeConnector;
    private final IndexConfiguration config;

    public ArsController(
        ArsService arsService,
        ElasticWriteConnector writeConnector,
        IndexConfiguration config,
        MessageSource messageSource
    ) {
        this.arsService = arsService;
        this.writeConnector = writeConnector;
        this.config = config;
        this.messageSource = messageSource;
    }

    @PostMapping
    @PreAuthorize(AuthorityRoles.Ars.UPLOAD)
    @Operation(
        summary = "Upload von ARS Dateien von Destatis. ",
        description = "Der Ars-Index wird vor dem Import geleert. Für die Postleitzahlen müssen im Anschluss die Daten aus dem Datensatz von Hauskoordinaten Deutschland importiert werden."
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "204",
                description = "Upload der ARS-Datei wird verarbeitet",
                content = @Content
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Fehler beim ARS-Import",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Es wurde eine falsche Anfrage gestellt",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
        }
    )
    @AntivirusMethod
    public ResponseEntity<Void> uploadArsFile(
        @Parameter(description = "Datei von Destatis", required = true) @RequestParam(
            name = "file"
        ) MultipartFile file
    ) {
        try (InputStream stream = file.getInputStream()) {
            writeConnector.clearIndex(config.getCurrentIndexName(IndexNames.ARS));
            arsService.importArsList(stream, StandardCharsets.UTF_8);
            log.info("ARS-Datei erfolgreich importiert.");
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            // PV-8946 catch-log-return-error
            log.warn("Fehler beim ARS-Import.", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Upload von CSV-Dateien, die irgendwo AGS oder ARS mit PLZ enthalten. Schoener
     * waere es, als
     * Parameter direkt {@link ZipcodeCsvDefinition} zu verwenden, aber das macht
     * Probleme mit Swagger
     * (vgl. z.B. https://github.com/springfox/springfox-demos/issues/40)
     * curl -X POST -F file=@plz-nach-hk-de-2026.csv.gz
     * http://localhost:8080/master/admin/ars/zip?zipColumn=1\&rsColumn=0
     */
    @PostMapping("zip")
    @PreAuthorize(AuthorityRoles.Ars.UPLOAD_ZIP)
    @Operation(
        summary = "Upload von Postleitzahlen als CSV",
        description = "Upload von Postleitzahlen als CSV, das Format kann angegeben werden. Es müssen mindestens ARS oder AGS und Postleitzahlen enthalten sein. Die Datei muss Gzip-komprimiert sein."
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "204",
                description = "Upload der CSV-Datei wird verarbeitet",
                content = @Content
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Fehler beim Import der Postleitzahlen",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Es wurde eine ungültige CSV-Datei hochgeladen",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
        }
    )
    @AntivirusMethod
    public ResponseEntity<Void> uploadPostcodesFile(
        @Parameter(description = "Csv-Datei", required = true) @RequestParam(
            name = "file"
        ) MultipartFile file,
        @Parameter(description = "Trennzeichen in der Csv-Datei", example = ";") @RequestParam(
            defaultValue = ";"
        ) String delimiter,
        @Parameter(
            description = "Spalte in der sich die Postleitzahl befindet, angefangen bei 0",
            required = true,
            example = "0"
        ) @RequestParam int rsColumn,
        @RequestParam int zipColumn,
        @Parameter(
            description = "Legt fest ob die Postleitzahlen basierend auf dem ARS oder dem AGS mit dem Datenbestand aus destatis zusammengeführt werden sollen",
            examples = { @ExampleObject("ARS"), @ExampleObject("AGS") }
        ) @RequestParam(defaultValue = "AGS") RegionalKeyKind keyKind,
        @Parameter(
            description = "Ob in der Csv-Datei eine Kopfzeile enthalten ist",
            example = "false"
        ) @RequestParam(defaultValue = "true") boolean headerLine
    ) {
        var csvDef = ZipcodeCsvDefinition.builder()
            .delimiter(delimiter)
            .headerLine(headerLine)
            .rsColumn(rsColumn)
            .zipColumn(zipColumn)
            .rsKind(keyKind)
            .build();
        return uploadPostcodesFile(file, csvDef, true);
    }

    private ResponseEntity<Void> uploadPostcodesFile(
        MultipartFile file,
        ZipcodeCsvDefinition def,
        boolean isCompressed
    ) {
        try (
            var is = isCompressed
                ? new GZIPInputStream(file.getInputStream())
                : file.getInputStream();
            var isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            var br = new BufferedReader(isr)
        ) {
            Stream<String> lines = br.lines();
            arsService.importPostcodes(lines, def);
        } catch (IOException e) {
            // PV-8946 catch-log-rethrow
            log.warn("Fehler beim PLZ-Import", e);
            throw new ImportException(
                ErrorCodeEnum.BAD_REQUEST,
                messageSource.getMessage("exception_plz_import_error", null, Locale.GERMAN),
                e
            );
        }
        return ResponseEntity.noContent().build();
    }
}
