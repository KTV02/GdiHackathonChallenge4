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
package de.dataport.suchdienst.indexer.service;

import com.google.common.collect.Iterables;
import de.dataport.suchdienst.elasticsearch.api.model.ElasticBulkError;
import de.dataport.suchdienst.error.SuchdienstException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.GenericTransferOperation;
import de.dataport.suchdienst.importer.model.jzufi.FormularEntity;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity;
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.importer.service.GenericFoParser;
import de.dataport.suchdienst.importer.service.GenericLbParser;
import de.dataport.suchdienst.importer.service.GenericOdParser;
import de.dataport.suchdienst.importer.service.GenericOeParser;
import de.dataport.suchdienst.importer.service.GenericSpParser;
import de.dataport.suchdienst.importer.service.GenericZsParser;
import de.dataport.suchdienst.importer.service.ImportChecker;
import de.dataport.suchdienst.indexer.model.measurement.ImportMeasurement;
import de.dataport.suchdienst.indexer.model.measurement.MeasurementType;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.XzufiObjectProvider;
import de.dataport.suchdienst.util.model.MdcKeys;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class XZufiImportService {

    private final Executor executor;
    private final DeleteRememberService deleteRememberService;

    private final GenericZsParser relationsParserService;
    private final RelationImportService zsImportService;

    private final OeImportService oeImportService;
    private final GenericOeParser oeParserService;

    private final OdImportService odImportService;
    private final FoImportService foImportService;
    private final GenericOdParser odParserService;
    private final GenericFoParser foParserService;

    private final SpImportService spImportService;
    private final GenericSpParser spParserService;

    private final GenericLbParser lbParserService;
    private final LbImportService lbImportService;

    private final AffectedLbResolver affectedLbResolver;

    private final XmlBinder xmlBinder;

    public XZufiImportService(
        @Qualifier("ioExecutor") Executor executor,
        DeleteRememberService deleteRememberService,
        AffectedLbResolver affectedLbResolver,
        XmlBinder xmlBinder,
        GenericZsParser relationsParserService,
        RelationImportService zsImportService,
        GenericOeParser oeParserService,
        OeImportService oeImportService,
        GenericOdParser odParserService,
        OdImportService odImportService,
        GenericFoParser foParserService,
        FoImportService foImportService,
        GenericSpParser spParserService,
        SpImportService spImportService,
        GenericLbParser lbParserService,
        LbImportService lbImportService
    ) {
        this.executor = executor;
        this.deleteRememberService = deleteRememberService;
        this.affectedLbResolver = affectedLbResolver;
        this.xmlBinder = xmlBinder;
        this.relationsParserService = relationsParserService;
        this.zsImportService = zsImportService;
        this.oeParserService = oeParserService;
        this.oeImportService = oeImportService;
        this.odParserService = odParserService;
        this.odImportService = odImportService;
        this.foParserService = foParserService;
        this.foImportService = foImportService;
        this.spParserService = spParserService;
        this.spImportService = spImportService;
        this.lbParserService = lbParserService;
        this.lbImportService = lbImportService;
    }

    public ImportMeasurement importXZufiFile(
        ImportRequest importRequest,
        ImportChecker importChecker
    ) {
        Instant start = Instant.now();
        try {
            return processImport(importRequest, importChecker, start);
        } catch (Exception e) {
            return handleImportError(e, start);
        }
    }

    private ImportMeasurement processImport(
        ImportRequest importRequest,
        ImportChecker importChecker,
        Instant start
    ) {
        GenericTransferOperation transferOperation = parseTransferOperation(importRequest);
        XzufiObjectProvider operation = new XzufiObjectProvider(transferOperation, importChecker);
        int objectCount = operation.countWriteObjects() + operation.countRemoveObjects();

        affectedLbResolver.markAffectedLbIds(
            transferOperation,
            importRequest.getMetaData().schemeAgencyId(),
            importRequest.getMetaData().producer()
        );

        // Einfügen/Updaten von Geschäftsobjekten in Entity-Indizes
        List<ElasticBulkError> insertErrors = insertBOs(operation, importRequest);
        // Löschen von Geschäftsobjekten aus Entity-Indizes
        List<ElasticBulkError> removeErrors = removeBOs(operation, importRequest);
        deleteRememberService.persistDeletedObjects();

        IndexerUtil.throwIfError(log, Iterables.concat(insertErrors, removeErrors));

        return ImportMeasurement.builder()
            .type(MeasurementType.XZUFI_VERARBEITUNG_NORMAL)
            .objectCount(objectCount)
            .startTime(start)
            .endTime(Instant.now())
            .success(true)
            .build();
    }

    private ImportMeasurement handleImportError(Exception e, Instant start) {
        String errorCode =
            e instanceof SuchdienstException se
                ? String.valueOf(se.getCode().getNumber())
                : String.valueOf(ErrorCodeEnum.XZUFI_IMPORT_ERROR.getNumber());
        try (var _ = MdcTemplate.scoped(MdcKeys.ERROR_CODE, errorCode)) {
            log.error("IMPORT - FEHLER: ", e);
        }

        return ImportMeasurement.builder()
            .type(MeasurementType.XZUFI_VERARBEITUNG_NORMAL)
            .startTime(start)
            .endTime(Instant.now())
            .success(false)
            .build();
    }

    private GenericTransferOperation parseTransferOperation(ImportRequest importRequest) {
        return switch (importRequest.getXzufiVersion()) {
            case XZUFI22 -> new GenericTransferOperation(
                xmlBinder.unmarshallXzufi(importRequest.getXzufi())
            );
            case XZUFI23 -> new GenericTransferOperation(
                xmlBinder.unmarshallXzufi23(importRequest.getXzufi())
            );
        };
    }

    /**
     * Importiert Geschäftsobjekte, für die eine Schreiboperation im ImportRequest enthalten ist.
     * Parst die Geschäftsobjekte und startet dann jede Klasse asynchron einen entsprechenden Import-Service-Aufruf,
     * wartet auf alle Terminierungen und sammelt die Ergebnisse ein.
     *
     * @return Liste aller aufgetretenen ElasticBulkErrors
     */
    private List<ElasticBulkError> insertBOs(
        XzufiObjectProvider xzufi,
        ImportRequest importRequest
    ) {
        List<Supplier<List<ElasticBulkError>>> serviceCalls = createInsertServiceCalls(
            xzufi,
            importRequest
        );
        return executeAndAwaitServiceCall(serviceCalls);
    }

    private List<Supplier<List<ElasticBulkError>>> createInsertServiceCalls(
        XzufiObjectProvider xzufi,
        ImportRequest importRequest
    ) {
        // Build entities for upsert operations
        var relations = buildRelations(xzufi, importRequest);
        var ods = buildOnlineServices(xzufi, importRequest);
        var oes = buildOrganisations(xzufi, importRequest);
        var sps = buildSpecializations(xzufi, importRequest);
        var fos = buildForms(xzufi, importRequest);
        var lbs = buildServiceDescriptions(xzufi, importRequest);

        return List.of(
            () -> zsImportService.importXzufiRelations(relations),
            () -> oeImportService.importXzufiOes(oes),
            () -> odImportService.importXzufiOds(ods),
            () -> foImportService.importXzufiFos(fos),
            () -> spImportService.importXzufiSps(sps),
            () -> lbImportService.importXzufiLbs(lbs)
        );
    }

    /**
     * Löscht Geschäftsobjekte, für die eine Löschoperation im ImportRequest enthalten ist.
     * Startet für jede Klasse asynchron einen entsprechenden Service-Aufruf,
     * wartet auf alle Terminierungen und sammelt die Ergebnisse ein.
     *
     * @return Liste aller aufgetretenen ElasticBulkErrors
     */
    private List<ElasticBulkError> removeBOs(
        XzufiObjectProvider operation,
        ImportRequest importRequest
    ) {
        List<Supplier<List<ElasticBulkError>>> serviceCalls = createRemoveServiceCalls(
            operation,
            importRequest
        );
        return executeAndAwaitServiceCall(serviceCalls);
    }

    private List<Supplier<List<ElasticBulkError>>> createRemoveServiceCalls(
        XzufiObjectProvider operation,
        ImportRequest importRequest
    ) {
        return List.of(
            () -> spImportService.deleteXzufiSps(operation, importRequest),
            () -> lbImportService.deleteXzufiLbs(operation, importRequest),
            () -> odImportService.deleteXzufiOds(operation, importRequest),
            () -> foImportService.deleteXzufiFos(operation, importRequest),
            () -> oeImportService.deleteXzufiOes(operation, importRequest),
            () -> zsImportService.deleteRelations(operation, importRequest)
        );
    }

    /**
     * Führt alle serviceCalls als CompletableFutures parallel aus.
     * Wartet bis sie terminieren und gib dann das Gesamtergebnis (die BulkErrors) zurück.
     */
    private List<ElasticBulkError> executeAndAwaitServiceCall(
        List<Supplier<List<ElasticBulkError>>> serviceCalls
    ) {
        List<CompletableFuture<List<ElasticBulkError>>> futures = serviceCalls
            .stream()
            .map(call -> CompletableFuture.supplyAsync(call, executor))
            .toList();

        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (Exception e) {
            futures.forEach(future -> future.cancel(true));
            throw e;
        }

        return futures
            .stream()
            .map(CompletableFuture::join)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .toList();
    }

    private List<ZustaendigkeitEntity> buildRelations(
        XzufiObjectProvider operation,
        ImportRequest importRequest
    ) {
        return relationsParserService.parseRelations(operation.getRelations(), importRequest);
    }

    private Collection<OrganisationseinheitEntity> buildOrganisations(
        XzufiObjectProvider operation,
        ImportRequest importRequest
    ) {
        return oeParserService.parseOes(
            operation.getOes(),
            importRequest.getMetaData(),
            importRequest.getRequestAcceptedAt()
        );
    }

    private Collection<OnlinedienstEntity> buildOnlineServices(
        XzufiObjectProvider operation,
        ImportRequest importRequest
    ) {
        return odParserService.parseOnlinediensteElastic(
            operation.getOds(),
            importRequest.getMetaData(),
            importRequest.getRequestAcceptedAt()
        );
    }

    private Collection<FormularEntity> buildForms(
        XzufiObjectProvider operation,
        ImportRequest importRequest
    ) {
        return foParserService.parseFosElastic(
            operation.getFos(),
            importRequest.getMetaData(),
            importRequest.getRequestAcceptedAt()
        );
    }

    private Collection<LeistungEntity> buildServiceDescriptions(
        XzufiObjectProvider operation,
        ImportRequest importRequest
    ) {
        return lbParserService.parseLbsElastic(
            operation.getLbs(),
            importRequest.getMetaData(),
            importRequest.getRequestAcceptedAt()
        );
    }

    private List<SpezialisierungEntity> buildSpecializations(
        XzufiObjectProvider operation,
        ImportRequest importRequest
    ) {
        return spParserService.parseSpsElastic(
            operation.getSps(),
            importRequest.getMetaData(),
            importRequest.getRequestAcceptedAt()
        );
    }
}
