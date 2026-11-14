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

import static net.logstash.logback.argument.StructuredArguments.keyValue;

import de.dataport.suchdienst.aop.AntivirusMethod;
import de.dataport.suchdienst.config.AuthorityRoles;
import de.dataport.suchdienst.config.Profiles;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.config.conditions.OnMasterCondition;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.importer.service.AlwaysAllowedImportChecker;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.measurement.ImportMeasurement;
import de.dataport.suchdienst.indexer.service.XZufiImportService;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.XZuFiVersionHeaderUtil;
import de.dataport.suchdienst.util.model.MdcKeys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArgument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/admin/xzufi")
@Tag(name = "ImportResource für xZufi Dateien")
@Profile(Profiles.DEVELOPMENT)
@ApiAudienceMarker(ApiAudience.INTERNAL)
@Conditional(OnMasterCondition.class)
@SecurityRequirement(name = "pvog_auth")
public class XzufiImportController {

    private final MessageSource messageSource;
    private final XZufiImportService importService;
    private final TaskScheduler scheduler;

    public XzufiImportController(
        XZufiImportService importService,
        MessageSource messageSource,
        @Qualifier("singleTaskExecutor") TaskScheduler scheduler
    ) {
        this.importService = importService;
        this.messageSource = messageSource;
        this.scheduler = scheduler;
    }

    @PostMapping
    @Operation(
        summary = "Import von xZufi Dateien aus dem angegebenen Verzeichnis",
        description = "Import von xZufi Dateien aus dem angegebenen Verzeichnis"
    )
    @PreAuthorize(AuthorityRoles.XzufiImport.FROM_DIR)
    public void importFiles(
        @Parameter(
            description = "Content-Type-Header mit Angabe der XZuFi-Version",
            required = true,
            hidden = true
        ) @RequestHeader(value = "Content-Type") MediaType contentType,
        @RequestParam(name = "path") @Parameter(
            description = "Der Pfad, in dem die xZufi Dateien liegen",
            required = true
        ) String path
    ) {
        XZuFiVersion xZuFiVersion = XZuFiVersionHeaderUtil.getXZuFiVersionFromHeader(
            contentType
        ).orElse(XZuFiVersion.XZUFI22);
        scheduler.schedule(() -> performFileImport(Paths.get(path), xZuFiVersion), Instant.now());
    }

    @PostMapping(value = "/xml", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import einer XzuFi-Datei", description = "Import einer XzuFi-Datei")
    @PreAuthorize(AuthorityRoles.XzufiImport.FROM_FILE)
    public void uploadFile(
        @Parameter(
            description = "Content-Type-Header mit Angabe der XZuFi-Version",
            required = true,
            hidden = true
        ) @RequestHeader(value = "Content-Type") MediaType contentType,
        @RequestPart(name = "file") @Parameter(
            name = "XML-Datei",
            required = true
        ) MultipartFile file
    ) throws IOException {
        byte[] xzufiFile = file.getBytes();
        scheduler.schedule(() -> {
            Long start = System.currentTimeMillis();
            XZuFiVersion xZuFiVersion = XZuFiVersionHeaderUtil.getXZuFiVersionFromHeader(
                contentType
            ).orElse(XZuFiVersion.XZUFI22);
            ImportMeasurement m = performFileUploadFromByteArray(xzufiFile, xZuFiVersion);
            log.info("Import-Statistik {}", structuredArg(m));
            Long end = System.currentTimeMillis();
            log.info("Es wurden eine XZufi Datei importiert, Dauer {}ms", end - start);
        }, Instant.now());
    }

    private static StructuredArgument structuredArg(ImportMeasurement m) {
        return keyValue("statistics", m);
    }

    @PostMapping(value = "/zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Import von xZufi-Dateien aus einer ZIP-Datei (nicht verschachtelt)",
        description = "Import von xZufi-Dateien aus einer ZIP-Datei (nicht verschachtelt)"
    )
    @PreAuthorize(AuthorityRoles.XzufiImport.FROM_FILE)
    @AntivirusMethod
    public ResponseEntity<Void> uploadFileZip(
        @Parameter(
            description = "Content-Type-Header mit Angabe der XZuFi-Version",
            required = true,
            hidden = true
        ) @RequestHeader(value = "Content-Type") MediaType contentType,
        @RequestParam("file") @Parameter(
            name = "Das Archiv, das die Dateien enthält",
            required = true
        ) MultipartFile file
    ) {
        try (var input = file.getInputStream()) {
            var tempFile = Files.createTempFile("xzufi-upload", ".zip"); // NOSONAR-java:S5443 safe here
            Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            scheduler.schedule(() -> {
                try {
                    XZuFiVersion xZuFiVersion = XZuFiVersionHeaderUtil.getXZuFiVersionFromHeader(
                        contentType
                    ).orElse(XZuFiVersion.XZUFI22);
                    performZipUpload(tempFile, xZuFiVersion);
                } finally {
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (IOException _) {
                        // PV-8946 catch-log-skip
                        log.warn("Fehler beim Löschen der Datei {}", tempFile);
                    }
                }
            }, Instant.now());
        } catch (IOException e) {
            // PV-8946 catch-log-return-error
            log.warn("Fehler bei XZufi-Import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok().build();
    }

    private void performFileImport(Path path, XZuFiVersion xzufiVersion) {
        Long start = System.currentTimeMillis();

        if (!Files.isDirectory(path)) {
            throw new ImportException(
                ErrorCodeEnum.XZUFI_IMPORT_ERROR,
                messageSource.getMessage(
                    "xzufi_import_resource.exception_path_is_no_dir_error",
                    null,
                    Locale.GERMAN
                ),
                ExceptionType.TECHNISCH
            );
        }
        AtomicInteger importedFiles = new AtomicInteger();
        try (Stream<Path> files = Files.walk(path)) {
            files
                .filter(Files::isRegularFile)
                .filter(f -> f.toString().endsWith(".xml"))
                .forEach(file -> {
                    log.info("Importiere {}", file);
                    try (
                        var _ = MdcTemplate.scoped(MdcKeys.XZUFIFILE, file.getFileName().toString())
                    ) {
                        try {
                            ImportMeasurement m = performFileUploadFromByteArray(
                                Files.readAllBytes(file),
                                xzufiVersion
                            );
                            importedFiles.getAndIncrement();
                            log.info("Import-Statistik {}", structuredArg(m));
                        } catch (Exception e) {
                            // PV-8946 catch-log-skip
                            log.warn("Ignoriere Fehler beim Import von {}:", file, e);
                        }
                    }
                });
        } catch (IOException e) {
            // PV-8946 catch-log-skip
            log.warn("Fehler beim Import von {}: ", path, e);
        }

        Long end = System.currentTimeMillis();
        log.info(
            "Es wurden {} XZufi Dateien importiert, Dauer {}ms",
            importedFiles.get(),
            end - start
        );
    }

    private void performZipUpload(Path zipFile, XZuFiVersion xzufiVersion) {
        try (
            InputStream is = Files.newInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(is)
        ) {
            Long start = System.currentTimeMillis();
            ZipEntry zipEntry;
            int containedZipFiles = 0;
            int importOk = 0;

            while ((zipEntry = zis.getNextEntry()) != null) {
                //NOSONAR-java:S5042 dieser Code wird nur von Dev Endpunkten aufgerufen, daher müssen wir uns hier nicht gegen Zip-Bomben absichern
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

                importOk = performFileUploadFromZipEntry(xzufiVersion, zis, zipEntry, importOk);
            }
            Long end = System.currentTimeMillis();
            log.info(
                "Es wurden {} von {} XZufi Dateien importiert, Dauer {} ms",
                importOk,
                containedZipFiles,
                end - start
            );
        } catch (IllegalArgumentException e) {
            // PV-8946 catch-log-skip
            log.warn("Fehler bei XZufi-Import, falscher Input", e);
        } catch (IOException e) {
            // PV-8946 catch-log-skip
            log.warn("Fehler bei XZufi-Import", e);
        }
    }

    private int performFileUploadFromZipEntry(
        XZuFiVersion xzufiVersion,
        ZipInputStream zis,
        ZipEntry zipEntry,
        int importOk
    ) {
        try (var _ = MdcTemplate.scoped(MdcKeys.XZUFIFILE, zipEntry.getName())) {
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                copyZipEntryToStream(zis, output, zipEntry);

                ImportMeasurement m = performFileUploadFromByteArray(
                    output.toByteArray(),
                    xzufiVersion
                );
                log.info("Import-Statistik fuer {}: {}", zipEntry.getName(), structuredArg(m));
                ++importOk;
            } catch (Exception e) {
                // PV-8946 catch-log-skip
                log.warn("Ignoriere Fehler beim Import von {}:", zipEntry.getName(), e);
            }
        }
        return importOk;
    }

    private ImportMeasurement performFileUploadFromByteArray(
        byte[] file,
        XZuFiVersion xZuFiVersion
    ) {
        var txnMetaData = createDummyTransactionMetaData();

        ImportRequest request = ImportRequest.createOpen(txnMetaData, file, xZuFiVersion);

        return importService.importXZufiFile(request, new AlwaysAllowedImportChecker());
    }

    private TransactionMetadata createDummyTransactionMetaData() {
        return new TransactionMetadata(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        );
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
