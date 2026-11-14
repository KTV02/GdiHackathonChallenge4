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

import static de.dataport.suchdienst.util.XZuFiVersionHeaderUtil.MEDIA_TYPE_XML_22;
import static de.dataport.suchdienst.util.XZuFiVersionHeaderUtil.MEDIA_TYPE_XML_23;

import de.dataport.suchdienst.config.AuthorityRoles;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.config.conditions.OnMasterCondition;
import de.dataport.suchdienst.error.ApiException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ErrorModel;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.dataport.suchdienst.importer.model.out.PushResponse;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequestStatus;
import de.dataport.suchdienst.importer.model.warteschlange.QueueableImportRequest;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.importer.service.DownloadImportRequestTokenService;
import de.dataport.suchdienst.importer.service.QueueImportService;
import de.dataport.suchdienst.util.RequestUtils;
import de.dataport.suchdienst.util.ResultPage;
import de.dataport.suchdienst.util.XZuFiVersionHeaderUtil;
import de.dataport.suchdienst.util.model.TransaktionsPosition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Conditional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Resource für die asynchrone Annahme von XZufi-Push-Nachrichten. Beinhaltet Endpunkte fuer die
 * Interaktion mit der Queue.
 */
@Slf4j
@RestController
@RequestMapping(PushImportController.BASE_PATH)
@Tag(name = "Queue-basierter, asynchroner Push-Import für xZuFi ")
@ApiAudienceMarker(ApiAudience.INTERNAL)
@Conditional(OnMasterCondition.class)
@SecurityRequirement(name = "pvog_auth")
public class PushImportController {

    public static final String BASE_PATH = "/push/xzufi-22";
    public static final String ENQUEUE_PATH = "enqueue";
    public static final String MESSAGE_ID_HEADER = "X-PVOG-NACHRICHTUUID";
    public static final String TRANSACTION_ID_HEADER = "X-PVOG-TRANSAKTIONID";

    private final MessageSource messageSource;
    private final QueueImportService queueImportService;
    private final PvogRequestContext requestContext;

    private final DownloadImportRequestTokenService downloadImportRequestTokenService;

    public PushImportController(
        QueueImportService queueImportService,
        MessageSource messageSource,
        PvogRequestContext requestContext,
        DownloadImportRequestTokenService downloadImportRequestTokenService
    ) {
        this.queueImportService = queueImportService;
        this.messageSource = messageSource;
        this.requestContext = requestContext;
        this.downloadImportRequestTokenService = downloadImportRequestTokenService;
    }

    @PostMapping(
        path = ENQUEUE_PATH,
        consumes = { MediaType.APPLICATION_XML_VALUE, MEDIA_TYPE_XML_22, MEDIA_TYPE_XML_23 },
        produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    @Operation(
        summary = "Reiht die uebergebenen xZuFi Rohdaten in die Import-Queue ein.",
        description = "Reiht die uebergebenen xZuFi Rohdaten in die Import-Queue ein."
    )
    @PreAuthorize(AuthorityRoles.XzufiImport.FROM_PUSH)
    public ResponseEntity<PushResponse> pushXzufiToQueue(
        @Parameter(
            description = "Content-Type-Header mit Angabe der XZuFi-Version",
            required = true,
            hidden = true
        ) @RequestHeader(value = "Content-Type") MediaType contentType,
        @Parameter(
            description = "ID zur Nachverfolgbarkeit einer Nachricht über mehrere Systeme",
            required = true
        ) @RequestHeader(value = MESSAGE_ID_HEADER) String id,
        @Parameter(
            description = "ID der Fachtransaktion des SaD, beschreibt einen Importvorgang eines Redaktionssystems",
            required = true
        ) @RequestHeader(value = TRANSACTION_ID_HEADER) String transactionId,
        @Parameter(
            description = "Beschreibt den Start, den Fortschritt und das Ende einer Folge von Nachrichten, die zu einer transaktionID gehören. Der Wert muss zwischen 0 und 3 liegen.",
            required = true
        ) @RequestHeader(value = "X-PVOG-TRANSAKTIONPOSITION") String transactionPos,
        @Parameter(description = "Enthält die SchemeAgencyId des Imports") @RequestHeader(
            value = "X-PVOG-AGENCYID"
        ) String schemeAgencyId,
        @Parameter(description = "Enthält den Namen des Redaktionssystems") @RequestHeader(
            value = "X-PVOG-PRODUCER"
        ) String producer,
        @Parameter(
            description = "Die zu importierenden xZuFi Rohdaten",
            required = true
        ) @RequestBody byte[] xml
    ) {
        log.info("Transfernachricht mit der ID {} empfangen.", id);

        if (!isTransactionPositionValid(transactionPos)) {
            throw new ApiException(
                ErrorCodeEnum.BAD_REQUEST,
                messageSource.getMessage(
                    "push_import_resource.wrong_transaction_position",
                    null,
                    requestContext.getLocale()
                ),
                ExceptionType.TECHNISCH
            );
        }

        TransactionMetadata meta = new TransactionMetadata(
            id,
            transactionId,
            transactionPos,
            schemeAgencyId,
            producer
        );

        XZuFiVersion xZuFiVersion = XZuFiVersionHeaderUtil.getXZuFiVersionFromHeader(
            contentType
        ).orElse(XZuFiVersion.XZUFI22);
        String messageId = queueImportService.enqueueImportRequest(meta, xml, xZuFiVersion);
        log.info(
            messageSource.getMessage(
                "push_import_resource.acknowledge_push",
                new String[] { meta.toString() },
                Locale.GERMAN
            )
        );
        return ResponseEntity.ok().body(new PushResponse(messageId));
    }

    private boolean isTransactionPositionValid(String transactionPos) {
        for (TransaktionsPosition pos : TransaktionsPosition.values()) {
            if (transactionPos.equals(pos.getPosition())) {
                return true;
            }
        }
        return false;
    }

    @GetMapping(value = "queue", produces = { MediaType.APPLICATION_JSON_VALUE })
    @Operation(
        summary = "Gibt eine Sicht auf die aktuelle Queue zurueck.",
        description = "Gibt eine Sicht auf die aktuelle Queue zurueck."
    )
    @PreAuthorize(AuthorityRoles.XzufiImport.READ_IMPORT_QUEUE)
    public ResponseEntity<ResultPage<QueueableImportRequest>> queueStatus(
        @Parameter(
            description = "Definiert, welche Seite (Subset des Gesamtergebnisses) abgefragt werden soll. Beginnt bei 0."
        ) @RequestParam(name = "page", required = false, defaultValue = "0") int page,
        @Parameter(
            description = "Definiert, wie groß eine Seite sein soll. Muss größer gleich 1 sein."
        ) @RequestParam(name = "pageSize", required = false, defaultValue = "50") int pageSize,
        @Parameter(
            description = "Filtert in der Queue nach einer Id. Hiermit ist die Id des ImportRequest selbst gemeint und ist nicht zu verwechseln mit der NachrichtUUID oder der Transaktions-Id"
        ) @RequestParam(name = "id", required = false) String id,
        @Parameter(
            description = "Filtert in der Queue nach einer bestimmten NachrichtUUID"
        ) @RequestParam(name = "messageId", required = false) String messageId,
        @Parameter(
            description = "Filtert in der Queue nach einer bestimmten Transaktions-Id"
        ) @RequestParam(name = "transactionId", required = false) String transactionId,
        @Parameter(
            description = "Filtert nach Einträgen, die NACH dem angegeben Datum in die Queue aufgenommen wurden."
        ) @RequestParam(name = "acceptedAfter", required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        ) LocalDateTime acceptedAfter,
        @Parameter(
            description = "Filtert nach Einträgen, die VOR dem angegeben Datum in die Queue aufgenommen wurden."
        ) @RequestParam(name = "acceptedBefore", required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        ) LocalDateTime acceptedBefore,
        @Parameter(
            description = "Filtert in der Queue nach einer bestimmten Status. Akzeptierte Werte sind: OPEN, ERROR und DONE"
        ) @RequestParam(name = "status", required = false) String status
    ) {
        ImportRequestStatus complexStatus;

        if (status == null) {
            complexStatus = null;
        } else {
            complexStatus = EnumUtils.getEnum(ImportRequestStatus.class, status);
            if (complexStatus == null) {
                throwMalformedStatusException();
            }
        }

        return ResponseEntity.ok().body(
            queueImportService.getQueueStatus(
                page,
                pageSize,
                id,
                messageId,
                transactionId,
                acceptedAfter,
                acceptedBefore,
                complexStatus
            )
        );
    }

    @DeleteMapping(value = "queue")
    @Operation(
        summary = "Entfernt ImportRequest mit der übergebenen ID aus der Queue.",
        description = "Entfernt ImportRequest mit der übergebenen ID aus der Queue."
    )
    @PreAuthorize(AuthorityRoles.XzufiImport.DELETE_IMPORT_REQUEST)
    public ResponseEntity<Void> deleteFromQueue(
        @Parameter(
            description = "NachrichtUUID des zu löschenden ImportRequest",
            required = true
        ) @RequestParam(name = "id") String id
    ) {
        if (queueImportService.deleteFromQueue(id)) {
            log.info(
                messageSource.getMessage(
                    "push_import_resource.delete_from_queue",
                    new String[] { RequestUtils.sanitizeUserInput(id) },
                    Locale.GERMAN
                )
            );
            return ResponseEntity.accepted().build();
        }

        throw new ApiException(
            ErrorCodeEnum.IMPORT_QUEUE,
            messageSource.getMessage(
                "push_import_resource.exception_delete_from_queue_error",
                new String[] { RequestUtils.sanitizeUserInput(id) },
                Locale.GERMAN
            ),
            ExceptionType.TECHNISCH
        );
    }

    @PostMapping(value = "queue/updateStatus")
    @Operation(
        summary = "Ändert den Status eines ImportRequest, nur für Testzwecke bestimmt.",
        description = "Ändert den Status eines ImportRequest, nur für Testzwecke bestimmt."
    )
    @PreAuthorize(AuthorityRoles.XzufiImport.UPDATE_IMPORT_REQUEST_STATUS)
    public ResponseEntity<Void> updateStatus(
        @Parameter(
            description = "Id des zu ändernden ImportRequest",
            required = true
        ) @RequestParam(name = "id") String id,
        @Parameter(
            description = "Neuer Status des zu ändernden ImportRequest. Akzeptierte Werte sind: OPEN, ERROR und DONE",
            required = true
        ) @RequestParam(name = "status") String status
    ) {
        ImportRequestStatus complexStatus = EnumUtils.getEnum(ImportRequestStatus.class, status);

        if (complexStatus == null) {
            throwMalformedStatusException();
        }

        queueImportService.changeStatus(id, complexStatus);
        return ResponseEntity.accepted().build();
    }

    @PostMapping(value = "queue/reopen")
    @Operation(
        summary = "Veranlasst die Wiederholung der ImportRequests",
        description = "Veranlasst die Wiederholung der ImportRequests, die fehlerhaft abgearbeitet wurden. Wird mindestens ein Element nicht gefunden oder hat nicht den Status Error wird keins aus der Liste auf Reopen gestellt."
    )
    @PreAuthorize(AuthorityRoles.XzufiImport.UPDATE_IMPORT_REQUEST_STATUS)
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "202",
                description = "Die Warteschlangenelemente wurden alle auf reopen gestellt."
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Es wurde mindestens ein Element der Warteschlange nicht gefunden",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
            @ApiResponse(
                responseCode = "409",
                description = "Mindestens ein Element hatte nicht den Status Error",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
        }
    )
    public ResponseEntity<Void> reopenImportRequest(
        @Parameter(
            description = "Ids der zu ändernden ImportRequests",
            required = true
        ) @RequestParam(name = "ids") @Size(min = 1, max = 50) Set<String> ids
    ) {
        queueImportService.reopenImportRequests(ids);
        return ResponseEntity.accepted().build();
    }

    @GetMapping(
        value = "queue/{id}/request-download-import-request-token",
        produces = { MediaType.TEXT_PLAIN_VALUE }
    )
    @Operation(
        summary = "Gibt das Downloadtoken für einen XZufi ImportRequest zurück.",
        description = "Gibt das Downloadtoken für einen XZufi ImportRequest zurück."
    )
    @PreAuthorize(AuthorityRoles.XzufiImport.READ_IMPORT_REQUEST_DETAILS)
    // exists.get() ist aufgrund des Optionals niemals null,
    // daher kann !exists.get() keine NullPointerException werfen,
    // da exists.isEmpty() ein early return verursacht
    @SuppressWarnings("java:S5411")
    public ResponseEntity<String> requestDownloadXZuFiImportRequest(
        @PathVariable @Parameter(description = "Die Id des ImportRequests.") String id
    ) {
        Optional<Boolean> exists = queueImportService.exists(id);

        if (exists.isEmpty()) {
            log.warn("Fehler bei Exists-Abfrage für Import Request '{}'!", id);
            return ResponseEntity.internalServerError().build();
        }
        if (!exists.get()) {
            log.warn("Der Import Request '{}' wurde wurde nicht gefunden.", id);
            return ResponseEntity.notFound().build();
        }

        log.info("Ein Downloadtoken für Import Request '{}' wird erstellt.", id);

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(downloadImportRequestTokenService.addAndGetToken(id));
    }

    private void throwMalformedStatusException() {
        throw new ApiException(
            ErrorCodeEnum.BAD_REQUEST,
            messageSource.getMessage("api.400_bad_request_params", null, Locale.GERMAN),
            ExceptionType.TECHNISCH
        );
    }
}
