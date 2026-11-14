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
package de.dataport.suchdienst.util;

import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.importer.model.GenericFormular;
import de.dataport.suchdienst.importer.model.GenericLeistung;
import de.dataport.suchdienst.importer.model.GenericLeistungSpezialisierung;
import de.dataport.suchdienst.importer.model.GenericLoescheObjekt;
import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.dataport.suchdienst.importer.model.GenericOrganisationseinheit;
import de.dataport.suchdienst.importer.model.GenericSchreibeObjekt;
import de.dataport.suchdienst.importer.model.GenericTransferOperation;
import de.dataport.suchdienst.importer.model.GenericXZuFiObjekt;
import de.dataport.suchdienst.importer.model.GenericZustaendigkeitTransferObjekt;
import de.dataport.suchdienst.importer.model.jzufi.FormularEntity;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity;
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.importer.service.ImportChecker;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Bietet Methoden zum Extrahieren von Geschäftsobjekten aus einer TransferOperation
 */
@Slf4j
public class XzufiObjectProvider {

    private final GenericTransferOperation transfer;
    private final ImportChecker importChecker;
    private final Map<Class<?>, List<GenericXZuFiObjekt<?, ?>>> writeObjectsByClass;
    private final Map<IdentificationType, List<GenericLoescheObjekt>> removeObjectsObjectsByClass;

    public XzufiObjectProvider(GenericTransferOperation obj, ImportChecker importChecker) {
        this.transfer = obj;
        this.importChecker = importChecker;
        this.writeObjectsByClass = collectWriteObjectsByClass();
        this.removeObjectsObjectsByClass = collectRemoveObjectsByClass();
    }

    public int countWriteObjects() {
        return writeObjectsByClass.values().stream().mapToInt(List::size).sum();
    }

    public int countRemoveObjects() {
        return removeObjectsObjectsByClass.values().stream().mapToInt(List::size).sum();
    }

    public Stream<GenericZustaendigkeitTransferObjekt> getRelations() {
        return writeObjectsByClass
            .getOrDefault(GenericZustaendigkeitTransferObjekt.class, Collections.emptyList())
            .stream()
            .filter(GenericZustaendigkeitTransferObjekt.class::isInstance)
            .map(GenericZustaendigkeitTransferObjekt.class::cast)
            .filter(this::checkXzufiRelation)
            .filter(this::isProcessable);
    }

    public Stream<GenericLeistung> getLbs() {
        return writeObjectsByClass
            .getOrDefault(GenericLeistung.class, Collections.emptyList())
            .stream()
            .filter(GenericLeistung.class::isInstance)
            .map(GenericLeistung.class::cast)
            .filter(this::isProcessable);
    }

    public Stream<GenericOrganisationseinheit> getOes() {
        return writeObjectsByClass
            .getOrDefault(GenericOrganisationseinheit.class, Collections.emptyList())
            .stream()
            .filter(GenericOrganisationseinheit.class::isInstance)
            .map(GenericOrganisationseinheit.class::cast)
            .filter(this::isProcessable);
    }

    public Stream<GenericOnlinedienst> getOds() {
        return writeObjectsByClass
            .getOrDefault(GenericOnlinedienst.class, Collections.emptyList())
            .stream()
            .filter(GenericOnlinedienst.class::isInstance)
            .map(GenericOnlinedienst.class::cast)
            .filter(this::isProcessable);
    }

    public Stream<GenericFormular> getFos() {
        return writeObjectsByClass
            .getOrDefault(GenericFormular.class, Collections.emptyList())
            .stream()
            .filter(GenericFormular.class::isInstance)
            .map(GenericFormular.class::cast)
            .filter(this::isProcessable);
    }

    public Stream<GenericLeistungSpezialisierung> getSps() {
        return writeObjectsByClass
            .getOrDefault(GenericLeistungSpezialisierung.class, Collections.emptyList())
            .stream()
            .filter(GenericLeistungSpezialisierung.class::isInstance)
            .map(GenericLeistungSpezialisierung.class::cast)
            .filter(this::isProcessable);
    }

    public Set<String> getDeleteIds(IdentificationType identificationType) {
        return removeObjectsObjectsByClass
            .getOrDefault(identificationType, Collections.emptyList())
            .stream()
            .filter(bo -> this.isProcessable(bo, identificationType))
            .map(GenericLoescheObjekt::getIdentifikatorString)
            .collect(Collectors.toSet());
    }

    private Map<Class<?>, List<GenericXZuFiObjekt<?, ?>>> collectWriteObjectsByClass() {
        return transfer
            .getTransferOperation()
            .stream()
            .filter(GenericSchreibeObjekt.class::isInstance)
            .map(GenericSchreibeObjekt.class::cast)
            .map(GenericSchreibeObjekt::data)
            .collect(Collectors.groupingBy(GenericXZuFiObjekt::getClass, Collectors.toList()));
    }

    private Map<IdentificationType, List<GenericLoescheObjekt>> collectRemoveObjectsByClass() {
        return transfer
            .getTransferOperation()
            .stream()
            .filter(GenericLoescheObjekt.class::isInstance)
            .map(GenericLoescheObjekt.class::cast)
            .collect(Collectors.groupingBy(GenericLoescheObjekt::getKlasse, Collectors.toList()));
    }

    private boolean isProcessable(GenericLoescheObjekt bo, IdentificationType identificationType) {
        return switch (identificationType) {
            case OE -> importChecker.importAllowed(
                bo.getIdentifikatorString(),
                IndexNames.ORGANISATIONSEINHEITEN,
                OrganisationseinheitEntity.class
            );
            case OD -> importChecker.importAllowed(
                bo.getIdentifikatorString(),
                IndexNames.ONLINEDIENSTE,
                OnlinedienstEntity.class
            );
            case LB -> importChecker.importAllowed(
                bo.getIdentifikatorString(),
                IndexNames.LEISTUNGSBESCHREIBUNG,
                LeistungEntity.class
            );
            case SP -> importChecker.importAllowed(
                bo.getIdentifikatorString(),
                IndexNames.SPEZIALISIERUNGEN,
                SpezialisierungEntity.class
            );
            case FO -> importChecker.importAllowed(
                bo.getIdentifikatorString(),
                IndexNames.FORMULARE,
                FormularEntity.class
            );
            case ZSOD, ZSOE, ZSFO -> importChecker.importAllowed(
                bo.getIdentifikatorString(),
                IndexNames.ZUSTAENDIGKEITEN,
                ZustaendigkeitEntity.class
            );
        };
    }

    private boolean isProcessable(GenericLeistung bo) {
        String id = bo.getId();
        return importChecker.importAllowed(
            id,
            IndexNames.LEISTUNGSBESCHREIBUNG,
            LeistungEntity.class
        );
    }

    private boolean isProcessable(GenericOrganisationseinheit bo) {
        String id = bo.getId();
        return importChecker.importAllowed(
            id,
            IndexNames.ORGANISATIONSEINHEITEN,
            OrganisationseinheitEntity.class
        );
    }

    private boolean isProcessable(GenericOnlinedienst bo) {
        String id = bo.getId();
        return importChecker.importAllowed(id, IndexNames.ONLINEDIENSTE, OnlinedienstEntity.class);
    }

    private boolean isProcessable(GenericFormular bo) {
        String id = bo.getId();
        return importChecker.importAllowed(id, IndexNames.FORMULARE, FormularEntity.class);
    }

    private boolean isProcessable(GenericLeistungSpezialisierung bo) {
        String id = bo.getId();
        return importChecker.importAllowed(
            id,
            IndexNames.SPEZIALISIERUNGEN,
            SpezialisierungEntity.class
        );
    }

    private boolean isProcessable(GenericZustaendigkeitTransferObjekt bo) {
        String id = bo.getId();
        return importChecker.importAllowed(
            id,
            IndexNames.ZUSTAENDIGKEITEN,
            ZustaendigkeitEntity.class
        );
    }

    /**
     * Der Sammlerdienst definiert, dass eine ZS nur 1 LB-ID, 1 Ars und 1 OE oder OD haben darf
     */
    private boolean checkXzufiRelation(GenericZustaendigkeitTransferObjekt zs) {
        if (zs.getLbIds().size() > 1) {
            log.error("Eine Zuständigkeit darf nur eine Leistungs-Id enthalten.");
            return false;
        }
        if (zs.getGebietIds().size() > 1) {
            log.error("Eine Zuständigkeit darf nur einen Ars enthalten.");
            return false;
        }

        return true;
    }
}
