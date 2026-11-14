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

import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.GenericFormular;
import de.dataport.suchdienst.importer.model.GenericLeistung;
import de.dataport.suchdienst.importer.model.GenericLeistungSpezialisierung;
import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.dataport.suchdienst.importer.model.GenericOrganisationseinheit;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.indexer.parser.x22.XZuFiFoParserService;
import de.dataport.suchdienst.indexer.parser.x22.XZuFiLbParserService;
import de.dataport.suchdienst.indexer.parser.x22.XZuFiOdParserService;
import de.dataport.suchdienst.indexer.parser.x22.XZuFiOeParserService;
import de.dataport.suchdienst.indexer.parser.x22.XZuFiSpParserService;
import de.dataport.suchdienst.util.Bundeslaender;
import de.dataport.suchdienst.util.LogUtil;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.XzufiObjectProvider;
import de.dataport.suchdienst.util.model.MdcKeys;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

/**
 * Berechnet aus einer über den {@link XzufiObjectProvider} bereitgestellten {@link de.xoev.xzufi.v2_2_0.TransferOperation040502} eine denormalisierte Menge an {@link Verwaltungsleistung} Dokumenten.
 * Das einzelne Dokument enthält zu einer Leistung und Verortung in ARS alle Details zu verknüpften
 * Organisationseinheiten, Onlinedienste und Spezialisierungen.
 * <p>
 * Die {@link de.xoev.xzufi.v2_2_0.TransferOperation040502} enthält alle Geschäftsobjekte zu einer Leistung.
 * Diese Geschäftsobjekte können in verschiedenen Orten zuständig sein (Onlinedienste, Organisationseinheiten) bzw. Anwendung finden (Spezialisierugnen).
 * Um gegenüber dem naiven Ansatz, einer Berechnung von einem Dokument je ARS, Speicherplatz zu sparen werden partitionen von ARS Mengen gebildet.
 * Ein Dokument in dieser Partition enthält eine Menge von ARS, für die alle Geschäftsobjekte verortet sind.
 * So ist sichergestellt, dass zu einer Leistungs ID und einer ARS immer genau ein Dokument gefunden wird, welches exakt
 * die Geschäftsobjekte enthält, welche mit der Leistung verortet sind.
 *
 * @see VerwaltungsleistungImportService
 */
@Service
@Slf4j
public class XzufiParserService {

    private final XZuFiOeParserService oeParserServiceX22;
    private final XZuFiOdParserService odParserServiceX22;
    private final XZuFiFoParserService foParserServiceX22;
    private final XZuFiSpParserService spParserServiceX22;
    private final XZuFiLbParserService lbParserServiceX22;
    private final de.dataport.suchdienst.indexer.parser.x23.XZuFiOeParserService oeParserServiceX23;
    private final de.dataport.suchdienst.indexer.parser.x23.XZuFiOdParserService odParserServiceX23;
    private final de.dataport.suchdienst.indexer.parser.x23.XZuFiFoParserService foParserServiceX23;
    private final de.dataport.suchdienst.indexer.parser.x23.XZuFiSpParserService spParserServiceX23;
    private final de.dataport.suchdienst.indexer.parser.x23.XZuFiLbParserService lbParserServiceX23;
    private final ArsTreeService arsService;
    private final LeikaService leikaService;

    // Dedicated executor for I/O-intensive parsing tasks
    private final Executor parsingExecutor;

    public XzufiParserService(
        @Qualifier("xZuFiOeParserServiceX22") XZuFiOeParserService oeParserServiceX22,
        @Qualifier("xZuFiOdParserServiceX22") XZuFiOdParserService odParserServiceX22,
        @Qualifier("xZuFiFoParserServiceX22") XZuFiFoParserService foParserServiceX22,
        @Qualifier("xZuFiSpParserServiceX22") XZuFiSpParserService spParserServiceX22,
        @Qualifier("xZuFiLbParserServiceX22") XZuFiLbParserService lbParserServiceX22,
        @Qualifier(
            "xZuFiOeParserServiceX23"
        ) de.dataport.suchdienst.indexer.parser.x23.XZuFiOeParserService oeParserServiceX23,
        @Qualifier(
            "xZuFiOdParserServiceX23"
        ) de.dataport.suchdienst.indexer.parser.x23.XZuFiOdParserService odParserServiceX23,
        @Qualifier(
            "xZuFiFoParserServiceX23"
        ) de.dataport.suchdienst.indexer.parser.x23.XZuFiFoParserService foParserServiceX23,
        @Qualifier(
            "xZuFiSpParserServiceX23"
        ) de.dataport.suchdienst.indexer.parser.x23.XZuFiSpParserService spParserServiceX23,
        @Qualifier(
            "xZuFiLbParserServiceX23"
        ) de.dataport.suchdienst.indexer.parser.x23.XZuFiLbParserService lbParserServiceX23,
        ArsTreeService arsService,
        LeikaService leikaService,
        @Qualifier("xzufiParserExecutor") Executor parsingExecutor
    ) {
        this.oeParserServiceX22 = oeParserServiceX22;
        this.odParserServiceX22 = odParserServiceX22;
        this.foParserServiceX22 = foParserServiceX22;
        this.spParserServiceX22 = spParserServiceX22;
        this.lbParserServiceX22 = lbParserServiceX22;
        this.oeParserServiceX23 = oeParserServiceX23;
        this.odParserServiceX23 = odParserServiceX23;
        this.foParserServiceX23 = foParserServiceX23;
        this.spParserServiceX23 = spParserServiceX23;
        this.lbParserServiceX23 = lbParserServiceX23;
        this.arsService = arsService;
        this.leikaService = leikaService;
        this.parsingExecutor = parsingExecutor;
    }

    /**
     * Liefert eine Map von arsFederalState auf Menge der generierten Verwaltungsleistungen in diesem arsFederalState.
     * Die Map hat nur bei Leistungen im gemeinsamen Vollzug mehrere Einträge.
     */
    public List<Verwaltungsleistung> parseXzufi(
        XzufiObjectProvider xzufi,
        String targetArsFederalState
    ) {
        validateSingleLeistung(xzufi);

        Instant lastUpdate = Instant.now();
        StopWatch sw = new StopWatch();

        sw.start();
        ParsedResults results = parseAllBusinessObjectsInParallel(xzufi, lastUpdate);
        sw.stop();
        log.debug(
            "{}, XZuFi-Datei in DTOs eingelesen",
            LogUtil.millisToSeconds(sw.lastTaskInfo().getTimeMillis())
        );

        sw.start();
        List<Verwaltungsleistung> vls = buildVerwaltungsleistungen(
            results.lb(),
            results.oes(),
            results.ods(),
            results.fos(),
            results.sps(),
            targetArsFederalState
        );
        sw.stop();
        log.debug(
            "{}, Denormalisierung berechnet",
            LogUtil.millisToSeconds(sw.lastTaskInfo().getTimeMillis())
        );

        return vls;
    }

    private void validateSingleLeistung(XzufiObjectProvider xzufi) {
        long lbCount = xzufi.getLbs().count();
        if (lbCount != 1) {
            throw new IllegalStateException(
                "Erwarte genau eine Leistung, aber erhalten: " + lbCount
            );
        }
    }

    /**
     * Container für die Ergebnisse der parallelen Verarbeitung aller Geschäftsobjekte.
     */
    private record ParsedResults(
        LeistungsbeschreibungVL lb,
        List<OrganisationseinheitVL> oes,
        List<OnlinedienstVL> ods,
        List<FormularVL> fos,
        List<SpezialisierungVL> sps
    ) {}

    /**
     * Verarbeitet alle Geschäftsobjekte parallel in einer einheitlichen Pipeline.
     * Dies ermöglicht eine bessere Auslastung, da die Verteilung der Objekttypen nicht einheitlich ist.
     */
    private ParsedResults parseAllBusinessObjectsInParallel(
        XzufiObjectProvider xzufi,
        Instant lastUpdate
    ) {
        // Erstelle alle parsing Tasks parallel
        var leistungFuture = createSingleLeistungParsingTask(xzufi);
        var organisationseinheitFutures = createOrganisationseinheitParsingTasks(xzufi, lastUpdate);
        var onlinedienstFutures = createOnlinedienstParsingTasks(xzufi);
        var formularFutures = createFormularParsingTasks(xzufi);
        var spezialisierungFutures = createSpezialisierungParsingTasks(xzufi);

        return executeParallelTasksAndCollectResults(
            leistungFuture,
            organisationseinheitFutures,
            onlinedienstFutures,
            formularFutures,
            spezialisierungFutures
        );
    }

    private ParsedResults executeParallelTasksAndCollectResults(
        CompletableFuture<LeistungsbeschreibungVL> leistungFuture,
        List<CompletableFuture<OrganisationseinheitVL>> organisationseinheitFutures,
        List<CompletableFuture<OnlinedienstVL>> onlinedienstFutures,
        List<CompletableFuture<FormularVL>> formularFutures,
        List<CompletableFuture<SpezialisierungVL>> spezialisierungFutures
    ) {
        // Sammle alle Futures für koordinierte Ausführung
        List<CompletableFuture<?>> allParsingTasks = Stream.of(
            Stream.of(leistungFuture),
            organisationseinheitFutures.stream(),
            onlinedienstFutures.stream(),
            formularFutures.stream(),
            spezialisierungFutures.stream()
        )
            // Hier kompiliert Function::identity nicht, daher Lambda
            .flatMap(s -> s)
            .collect(Collectors.toList());

        try {
            // Warte auf Abschluss aller parsing Tasks
            CompletableFuture.allOf(allParsingTasks.toArray(CompletableFuture[]::new)).join();

            // Sammle Ergebnisse (join() sollte hier nicht mehr fehlschlagen, da alle Tasks bereits abgeschlossen sind)
            return new ParsedResults(
                leistungFuture.join(),
                collectCompletedResults(organisationseinheitFutures),
                collectCompletedResults(onlinedienstFutures),
                collectCompletedResults(formularFutures),
                collectCompletedResults(spezialisierungFutures)
            );
        } catch (CompletionException e) {
            Throwable rootCause = e.getCause() != null ? e.getCause() : e;
            String errorMsg = "Fehler beim Parsen der XZuFi-Geschäftsobjekte";
            log.error(errorMsg, rootCause);
            throw new ImportException(ErrorCodeEnum.XZUFI_IMPORT_ERROR, errorMsg, rootCause);
        }
    }

    private <T> List<T> collectCompletedResults(List<CompletableFuture<T>> futures) {
        return futures.stream().map(CompletableFuture::join).toList();
    }

    /**
     * Bildet Verwaltungsleistungen zu einer LB, führt dabei eine Ausmultiplikation der ARS durch, sodass jede entstehende VL
     * OEs/ODs/SPs enthält, die alle in den ARS der VL zuständig sein (Clusterung).
     * Mit PV-10016 (Exakte ARS-Suche ermöglichen) entstehen zusätzliche VLs, die bei der Suche nach exakten ARS gefunden werden sollen,
     * hierbei werden duplikate Suchtreffer vermieden.
     *
     * @param lb  Die LB, aus der die VL generiert werden sollen.
     * @param oes Die OEs aus der LB
     * @param ods Die ODs aus der LB
     * @param fos Die FOs aus der LB
     * @param sps Die SPs aus der LB
     * @return Die generierten Verwaltungsleistungen.
     */
    private List<Verwaltungsleistung> buildVerwaltungsleistungen(
        LeistungsbeschreibungVL lb,
        List<OrganisationseinheitVL> oes,
        List<OnlinedienstVL> ods,
        List<FormularVL> fos,
        List<SpezialisierungVL> sps,
        String targetArsFederalState
    ) {
        ArsTree arsTree = arsService.getArsTree();
        boolean isBundImGemeinsamenVollzug = isBundInGemeinsamerVollzug(lb);
        return new VerwaltungsleistungenBuilder(
            arsTree,
            targetArsFederalState,
            isBundImGemeinsamenVollzug
        ).buildVerwaltungsleistungen(lb, oes, ods, fos, sps);
    }

    private boolean isBundInGemeinsamerVollzug(LeistungsbeschreibungVL lb) {
        boolean isGemeinsamerVollzug = lb.getArsFederalstate().size() > 1;
        boolean isBund = lb.getArsFederalstate().contains(Bundeslaender.BUND.getArsFederalstate());
        return isBund && isGemeinsamerVollzug;
    }

    private LeistungsbeschreibungVL parseGenericLeistung(GenericLeistung genericLeistung) {
        LeistungsbeschreibungVL leistungVL = switch (genericLeistung.getVersion()) {
            case XZUFI22 -> lbParserServiceX22.parseLeistungen(genericLeistung.getXZuFi22());
            case XZUFI23 -> lbParserServiceX23.parseLeistungen(genericLeistung.getXZuFi23());
        };

        leikaService.computeLifeSituations(leistungVL, genericLeistung);

        return leistungVL;
    }

    private CompletableFuture<LeistungsbeschreibungVL> createSingleLeistungParsingTask(
        XzufiObjectProvider xzufi
    ) {
        GenericLeistung leistung = xzufi.getLbs().findFirst().orElseThrow();
        return CompletableFuture.supplyAsync(() -> parseGenericLeistung(leistung), parsingExecutor);
    }

    private List<CompletableFuture<OrganisationseinheitVL>> createOrganisationseinheitParsingTasks(
        XzufiObjectProvider xzufi,
        Instant lastUpdate
    ) {
        return createParsingTasks(xzufi.getOes(), oe ->
            parseGenericOrganisationseinheit(oe, lastUpdate)
        );
    }

    private List<CompletableFuture<OnlinedienstVL>> createOnlinedienstParsingTasks(
        XzufiObjectProvider xzufi
    ) {
        return createParsingTasks(xzufi.getOds(), this::parseGenericOnlinedienst);
    }

    private List<CompletableFuture<FormularVL>> createFormularParsingTasks(
        XzufiObjectProvider xzufi
    ) {
        return createParsingTasks(xzufi.getFos(), this::parseGenericFormular);
    }

    private List<CompletableFuture<SpezialisierungVL>> createSpezialisierungParsingTasks(
        XzufiObjectProvider xzufi
    ) {
        return createParsingTasks(xzufi.getSps(), this::parseGenericSpezialisierung);
    }

    private <T, R> List<CompletableFuture<R>> createParsingTasks(
        Stream<T> items,
        Function<T, R> parser
    ) {
        return items
            .map(item -> CompletableFuture.supplyAsync(() -> parser.apply(item), parsingExecutor))
            .toList();
    }

    OrganisationseinheitVL parseGenericOrganisationseinheit(
        GenericOrganisationseinheit genericOrganisationseinheit,
        Instant lastUpdate
    ) {
        try (var _ = MdcTemplate.scoped(MdcKeys.OEID, genericOrganisationseinheit.getId())) {
            return switch (genericOrganisationseinheit.getVersion()) {
                case XZUFI22 -> oeParserServiceX22.parseOrganisationseinheit(
                    genericOrganisationseinheit.getXZuFi22(),
                    lastUpdate
                );
                case XZUFI23 -> oeParserServiceX23.parseOrganisationseinheit(
                    genericOrganisationseinheit.getXZuFi23(),
                    lastUpdate
                );
            };
        }
    }

    private OnlinedienstVL parseGenericOnlinedienst(GenericOnlinedienst genericOnlinedienst) {
        try (var _ = MdcTemplate.scoped(MdcKeys.ODID, genericOnlinedienst.getId())) {
            return switch (genericOnlinedienst.getVersion()) {
                case XZUFI22 -> odParserServiceX22.parseOnlinedienst(
                    genericOnlinedienst.getXZuFi22()
                );
                case XZUFI23 -> odParserServiceX23.parseOnlinedienst(
                    genericOnlinedienst.getXZuFi23()
                );
            };
        }
    }

    private FormularVL parseGenericFormular(GenericFormular genericFormular) {
        try (var _ = MdcTemplate.scoped(MdcKeys.FOID, genericFormular.getId())) {
            return switch (genericFormular.getVersion()) {
                case XZUFI22 -> foParserServiceX22.parseFormular(genericFormular.getXZuFi22());
                case XZUFI23 -> foParserServiceX23.parseFormular(genericFormular.getXZuFi23());
            };
        }
    }

    private SpezialisierungVL parseGenericSpezialisierung(
        GenericLeistungSpezialisierung genericSpezialisierung
    ) {
        String spId = genericSpezialisierung.getId();
        String lbId = genericSpezialisierung.getLbId();
        try (var _ = MdcTemplate.scoped(MdcKeys.SPID, spId)) {
            return switch (genericSpezialisierung.getVersion()) {
                case XZUFI22 -> spParserServiceX22.parseSpezialisierung(
                    genericSpezialisierung.getXZuFi22(),
                    lbId,
                    spId
                );
                case XZUFI23 -> spParserServiceX23.parseSpezialisierung(
                    genericSpezialisierung.getXZuFi23(),
                    lbId,
                    spId
                );
            };
        }
    }
}
