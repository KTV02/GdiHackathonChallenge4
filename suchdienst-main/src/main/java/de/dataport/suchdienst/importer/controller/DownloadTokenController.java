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
package de.dataport.suchdienst.importer.controller;

import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.config.conditions.OnMasterCondition;
import de.dataport.suchdienst.importer.service.DownloadImportRequestTokenService;
import de.dataport.suchdienst.importer.service.QueueImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/download")
@Tag(name = "Download von Import Requests mit Hilfe eines Download Token.")
@ApiAudienceMarker(ApiAudience.INTERNAL)
@Conditional(OnMasterCondition.class)
@SecurityRequirement(name = "pvog_auth")
public class DownloadTokenController {

    private final QueueImportService queueImportService;

    private final DownloadImportRequestTokenService downloadImportRequestTokenService;

    public DownloadTokenController(
        QueueImportService queueImportService,
        DownloadImportRequestTokenService downloadImportRequestTokenService
    ) {
        this.queueImportService = queueImportService;
        this.downloadImportRequestTokenService = downloadImportRequestTokenService;
    }

    @GetMapping(value = "/xzufi", produces = { MediaType.APPLICATION_OCTET_STREAM_VALUE })
    @Operation(
        summary = "Liefert das XZuFi aus einem Eintrag in der Import-Queue über ein Download Token.",
        description = "Liefert das XZuFi aus einem Eintrag in der Import-Queue über ein Download Token."
    )
    public ResponseEntity<ByteArrayResource> downloadImportRequestByToken(
        @Parameter(
            description = "Das Token zu download des Import Requests.",
            required = true
        ) @RequestParam String downloadToken
    ) {
        if (!downloadImportRequestTokenService.isAToken(downloadToken)) {
            log.warn("Ungültiges download Token '{}'.", downloadToken);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        var id = downloadImportRequestTokenService.getAndRemoveToken(downloadToken);
        if (id == null) {
            log.warn("Das download Token '{}' existiert nicht.", downloadToken);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        log.info("Der Download für Token '{}' wurde abgerufen.", downloadToken);

        return queueImportService
            .getDetails(id)
            .map(importRequest -> {
                String nachrichtenUUID = importRequest.getMetaData().id();
                ByteArrayResource resource = new ByteArrayResource(importRequest.getXzufi());
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(resource.contentLength())
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                            .filename(String.format("xzufi_%s.xml", nachrichtenUUID))
                            .build()
                            .toString()
                    )
                    .header(
                        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        HttpHeaders.CONTENT_DISPOSITION
                    )
                    .body(resource);
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
