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
package de.dataport.suchdienst.backoffice.controller;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

import de.dataport.suchdienst.aop.AntivirusMethod;
import de.dataport.suchdienst.config.Profiles;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.config.conditions.OnMasterCondition;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.measurement.ImportMeasurement;
import de.dataport.suchdienst.indexer.service.ImportSchedulingService;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Bietet Endpunkte zur Administration der Verwaltungsleistungen, die nur für dev und test verfügbar sein sollen.
 */
@Profile({ Profiles.DEVELOPMENT, Profiles.INTEGRATION_TEST })
@RestController
@RequestMapping("/admin/verwaltungsleistung")
@Tag(name = "Dev/Test Admin Endpunkte zum Steuern der Verwaltungsleistungen")
@Slf4j
@ApiAudienceMarker(ApiAudience.INTERNAL)
@Conditional(OnMasterCondition.class)
@SecurityRequirement(name = "pvog_auth")
public class AdminVerwaltungsleistungDevController {

    private static final String REGEX = "^(00|01|02|03|04|05|06|07|08|09|10|11|12|13|14|15|16)$";

    private final ImportSchedulingService importSchedulingService;
    private final TaskScheduler scheduler;
    private final MessageSource messageSource;

    public AdminVerwaltungsleistungDevController(
        ImportSchedulingService importSchedulingService,
        @Qualifier("singleTaskExecutor") TaskScheduler scheduler,
        MessageSource messageSource
    ) {
        this.importSchedulingService = importSchedulingService;
        this.scheduler = scheduler;
        this.messageSource = messageSource;
    }

    @DeleteMapping(value = "/delete")
    @Operation(
        summary = "Löscht einen Verwaltungsleistungsindex",
        description = "Löscht einen Verwaltungsleistungsindex"
    )
    public ResponseEntity<Void> delete(
        @RequestParam(name = "arsFederalState") @Size(min = 2, max = 2) @Pattern(
            regexp = REGEX
        ) String arsFederalState
    ) {
        importSchedulingService.deleteFederalState(arsFederalState);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @DeleteMapping(value = "/delete/all")
    @Operation(
        summary = "Löscht alle Verwaltungsleistungsindizes",
        description = "Löscht alle Verwaltungsleistungsindizes"
    )
    public ResponseEntity<Void> deleteAll() {
        importSchedulingService.deleteAll();
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping(value = "import/path")
    @Operation(
        summary = "Import von xZufi Dateien aus dem angegebenen Verzeichnis",
        description = "Import von xZufi Dateien aus dem angegebenen Verzeichnis"
    )
    public void importFiles(
        @Parameter(
            description = "Der Pfad in dem die xZufi Dateien liegen",
            required = true
        ) String path,
        // TODO: Wegen LiGv ist es hier nicht sinnvoll, auf arsFederalState einzuschränken.
        // der Import speichert jetzt im ligv Fall in mehrere Indizes.
        @RequestParam(name = "arsFederalstate") @Size(min = 2, max = 2) @Pattern(
            regexp = REGEX
        ) String arsFederalstate
    ) {
        importSchedulingService.importFiles(path, arsFederalstate);
    }

    @PostMapping(value = "import/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import einer XzuFi-Datei", description = "Import einer XzuFi-Datei")
    public void uploadFile(
        @RequestPart(name = "file") @Parameter(
            name = "XML-Datei",
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_XML_VALUE,
                schema = @Schema(type = "string")
            )
        ) MultipartFile file,
        // TODO: Wegen LiGv ist es hier nicht sinnvoll, auf arsFederalState einzuschränken.
        // der Import speichert jetzt im ligv Fall in mehrere Indizes.
        @RequestParam(name = "arsFederalstate") @Size(min = 2, max = 2) @Pattern(
            regexp = REGEX
        ) String arsFederalstate
    ) throws IOException {
        importSchedulingService.uploadFile(file, arsFederalstate);
    }

    @PostMapping(value = "import/zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Import von xZufi-Dateien aus einer ZIP-Datei (nicht verschachtelt)",
        description = "Import von xZufi-Dateien aus einer ZIP-Datei (nicht verschachtelt)"
    )
    @AntivirusMethod
    public ResponseEntity<Void> importZipFile(
        @RequestPart(name = "file") @Parameter(
            name = "Das Archiv, das die Dateien enthält",
            required = true
        ) MultipartFile file,
        // TODO: Wegen LiGv ist es hier nicht sinnvoll, auf arsFederalState einzuschränken.
        @RequestParam(name = "arsFederalstate") @Size(min = 2, max = 2) @Pattern(
            regexp = REGEX
        ) String arsFederalstate
    ) {
        try {
            importZip(file, arsFederalstate);
        } catch (ImportException e) {
            log.warn("Fehler bei XZufi-Import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    public void importZip(MultipartFile file, String arsFederalstate) {
        try (var input = file.getInputStream()) {
            var tempFile = Files.createTempFile("xzufi-upload", ".zip"); // NOSONAR-java:S5443 - safe here
            Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            scheduler.schedule(() -> {
                try {
                    performZipUpload(tempFile, arsFederalstate);
                    Files.delete(tempFile);
                } catch (IOException e) {
                    throw new ImportException(
                        ErrorCodeEnum.XZUFI_IMPORT_ERROR,
                        "Fehler beim Aufräumen des Temp Verzeichnisses",
                        e
                    );
                }
            }, Instant.now());
        } catch (IOException e) {
            throw new ImportException(
                ErrorCodeEnum.XZUFI_IMPORT_ERROR,
                "Fehler bei Xzufi-Import",
                e
            );
        }
    }

    @SuppressWarnings("java:S5042") // Dieser Code wird nur von Dev Endpunkten aufgerufen, daher müssen wir uns hier nicht gegen Zip-Bomben absichern
    private void performZipUpload(Path zipFile, String arsFederalState) {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry zipEntry;
            int containedZipFiles = 0;
            int importOk = 0;

            StopWatch sw = new StopWatch();
            sw.start();
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    throw new IllegalArgumentException(
                        messageSource.getMessage(
                            "xzufi_import_resource.exception_path_is_dir_error",
                            null,
                            Locale.GERMAN
                        )
                    );
                }
                if (!zipEntry.getName().endsWith(".xml")) {
                    continue;
                }

                containedZipFiles++;

                log.info("Importiere #{} XZufi Dateien", containedZipFiles);

                importOk = importVerwaltungsleistungenFromZipEntry(
                    arsFederalState,
                    zipEntry,
                    zis,
                    importOk
                );
            }
            sw.stop();
            log.info(
                "Es wurden {} von {} XZufi Dateien importiert, {}ms",
                importOk,
                containedZipFiles,
                sw.getTotalTimeMillis()
            );
        } catch (IllegalArgumentException e) {
            log.error("Fehler bei XZufi-Import, falscher Input", e);
        } catch (IOException e) {
            log.error("Fehler bei XZufi-Import", e);
        }
    }

    private int importVerwaltungsleistungenFromZipEntry(
        String arsFederalState,
        ZipEntry zipEntry,
        ZipInputStream zis,
        int importOk
    ) {
        try (var _ = MdcTemplate.scoped(MdcKeys.XZUFIFILE, zipEntry.getName())) {
            try (var baos = new ByteArrayOutputStream()) {
                copyZipEntryToStream(zis, baos, zipEntry);

                ImportMeasurement m =
                    importSchedulingService.importVerwaltungsleistungenFromByteArray(
                        baos.toByteArray(),
                        arsFederalState
                    );
                log.debug(
                    "Import-Statistik fuer {} {}",
                    zipEntry.getName(),
                    keyValue("statistics", m)
                );

                ++importOk;
            } catch (Exception e) {
                log.warn("Ignoriere Fehler beim Import von {}:", zipEntry, e);
            }
        }
        return importOk;
    }

    private void copyZipEntryToStream(InputStream in, OutputStream out, ZipEntry entry)
        throws IOException {
        byte[] buffer = new byte[1024 * 4];
        long count = 0;
        int n;
        long size = entry.getSize();

        while (-1 != (n = in.read(buffer)) && count < size) {
            out.write(buffer, 0, n);
            count += n;
        }
    }
}
