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
package de.dataport.suchdienst.suchen.service;

import co.elastic.clients.util.TriFunction;
import de.dataport.suchdienst.common.model.ARS;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticQueryConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.GenericModelMapper;
import de.dataport.suchdienst.importer.model.GenericOrganisationseinheit;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.indexer.model.mapping.Communication;
import de.dataport.suchdienst.indexer.model.mapping.Contact;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrganisationUnitService {

    private final ElasticPublicSearchConnector publicSearchConnector;
    private final ElasticReadConnector readConnector;
    private final ElasticQueryConnector elasticQueryConnector;
    private final OrganisationModelConverterService modelConverterService;
    private final MessageSource messageSource;
    private final PvogRequestContext context;

    public OrganisationUnitService(
        ElasticPublicSearchConnector publicSearchConnector,
        ElasticReadConnector readConnector,
        ElasticQueryConnector elasticQueryConnector,
        OrganisationModelConverterService modelConverterService,
        MessageSource messageSource,
        PvogRequestContext context
    ) {
        this.publicSearchConnector = publicSearchConnector;
        this.readConnector = readConnector;
        this.elasticQueryConnector = elasticQueryConnector;
        this.modelConverterService = modelConverterService;
        this.messageSource = messageSource;
        this.context = context;
    }

    /**
     * Bereitet die Organisationseinheit versionsunabhängig auf. Das Ausgabeformat wird durch den
     * übergebenen Mapper bestimmt.
     */
    public <T> T getOrganisationUnitDetail(
        @NonNull String oeId,
        @Nullable String ars,
        @Nullable String lbId,
        @Nullable LocalDate validDate,
        TriFunction<
            OrganisationseinheitVL,
            ResponsibilityRole,
            List<Communication>,
            T
        > outModelProducer
    ) {
        List<String> includes = List.of("id", "organisationseinheiten", "leistungsbeschreibung.id");

        Optional<Verwaltungsleistung> verwaltungsleistungOptional =
            publicSearchConnector.searchVerwaltungsleistungByLbOe(
                includes,
                null,
                validDate,
                lbId,
                oeId
            );

        Verwaltungsleistung vl = verwaltungsleistungOptional.orElseThrow(() ->
            // Wir konnten keine Verwaltungsleistung finden, die zu den Eingabeparametern passt
            new SuchenException(
                ErrorCodeEnum.NOT_FOUND,
                messageSource.getMessage("api.404", null, Locale.GERMAN)
            )
        );

        // Wählt die korrekte OE aus (OE-Id-Match, muss öffentlich sein) und passt die Daten innerhalb der OE so an, dass sie zu dem Anfrage-ARS passen:
        OrganisationseinheitVL oeVl = vl
            .getOrganisationseinheiten()
            .stream()
            .filter(oe -> oe.getId().equals(oeId))
            .map(oe -> filterNestedObjectsAndApplyLanguage(oe, lbId, ars))
            .findFirst()
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage("api.404", null, Locale.GERMAN)
                )
            );

        ResponsibilityRole role = modelConverterService.getMergedResponsibilityRole(
            oeVl,
            ARS.of(ars).getArsHierarchy(),
            context.getLocale()
        );
        List<Communication> communications = getCommunications(ars, oeVl);

        return outModelProducer.apply(oeVl, role, communications);
    }

    public Optional<GenericOrganisationseinheit> getJzufi(String oeId, LocalDate validDate) {
        return readConnector
            .searchById(
                oeId,
                List.of(),
                List.of(),
                IndexNames.ORGANISATIONSEINHEITEN,
                validDate,
                OrganisationseinheitEntity.class
            )
            .map(GenericModelMapper::mapOrganisationseinheitEntity);
    }

    /**
     * Die Kommunikationssysteme aus den Relations ermitteln. Dabei in der ARS-Hierarchie von konkretem Gemeinde-ARS bis zum
     * Landes-Ars suchen und die konkreteste vorhandene Liste von Kommunikationssystemen liefern, die mindestens einen öffentlichen Eintrag enthält.
     */
    public List<Communication> getMostConcreteMatchingCommunicationsInHierarchy(
        OrganisationseinheitVL oe,
        String ars
    ) {
        Map<String, List<Communication>> communications = oe.getCommunicationSystems();
        Collection<String> arsHierarchy = new ARS(ars).getArsHierarchy();
        return arsHierarchy
            .stream()
            .filter(communications::containsKey)
            .map(communications::get)
            .findFirst()
            .orElse(Collections.emptyList());
    }

    /**
     * Diese Methode präpariert den Inhalt einer OE so, dass sie über eine API ausgeliefert werden kann.
     * Die OEs innerhalb von Verwaltungsleistungen sind ja "redundant", d.h. alle Vorkommen derselben OE sind über alle VLs identisch.
     * Über die API sollen aber nur diejenigen
     * Bestandteile ausgeliefert werden, die zu der ARS (Gemeinde- sowohl als höhere Level) passen.
     * Konkret wird hier folgendes gemacht:
     * <ul>
     *     <li> Die Liste der Kontaktpersonen so zuschneiden, dass nur diejenigen übrig bleiben, die zum ARS passen. </li>
     *     <li> Die Liste der geschachtelten Formulare so zuschneiden, dass nur diejenigen übrigbleiben, die zum angefragten ARS passen.</li>
     *     <li> Die Daten der angefragten Sprache zur Anwendung bringen, so dass die mehrsprachigen Felder korrekt ausgeliefert werden. </li>
     * </ul>
     *
     * @param oe   Die zu reduzierende OE
     * @param lbId Die angefragte LB-Id, kann fehlen - in diesem Fall werden weder Kontaktpersonen noch Formulare geliefert, da diese immer
     *             per Zuständigkeit auch einen LB-Bezug haben.
     * @param ars  Der angefragte ARS, kann sowohl Gemeinde- als auch HigherLevel ARS sein
     * @return Die präparierte OE
     */
    private OrganisationseinheitVL filterNestedObjectsAndApplyLanguage(
        OrganisationseinheitVL oe,
        @Nullable String lbId,
        @Nullable String ars
    ) {
        if (StringUtils.isNotBlank(lbId)) {
            filterNestedContacts(oe, ars);
            filterNestedForms(oe, ars);
        } else {
            oe.setEmployees(List.of());
            oe.setNestedForms(List.of());
        }
        oe.applyLanguageOverride(context.getLocale());
        return oe;
    }

    /**
     * Reduziert die geschachtelten Formulare der OE auf diejenigen, die für das Gebiet zuständig sind.
     * Die so modifizierte OE wird später zur Generierung des Out-Models verwendet.
     * Auf die lbId muss hier im nicht geachtet werden, denn schon beim Fetchen der
     * OEs für das Bilden der Verwaltungsleistungen wurden die unpassenden geschachtelten Formulare entsprechend aussortiert,
     * siehe {@link de.dataport.suchdienst.indexer.service.XzufiFetchService}
     */
    private void filterNestedForms(OrganisationseinheitVL oe, String ars) {
        Predicate<FormularVL> formulareFilter = fo -> {
            if (fo.getArs() == null || fo.getArs().isEmpty()) {
                return false;
            }
            if (ars != null) {
                Collection<String> givenArsHierarchy = new ARS(ars).getArsHierarchy();
                return !CollectionUtils.intersection(givenArsHierarchy, fo.getArs()).isEmpty();
            }

            return false;
        };
        oe.setNestedForms(oe.getNestedForms().stream().filter(formulareFilter).toList());
    }

    /**
     * Reduziert die Kontaktpersonen der OE auf diejenigen, die für die Lb und das Gebiet zuständig sind.
     * Die so modifizierte OE wird später zur Generierung des Out-Models verwendet.
     * Auf die lbId muss hier im nicht geachtet werden, denn schon beim Fetchen der
     * OEs für das Bilden der Verwaltungsleistungen wurden die unpassenden geschachtelten Formulare entsprechend aussortiert,
     */
    private void filterNestedContacts(OrganisationseinheitVL oe, String ars) {
        Predicate<Contact> kontaktPersonenFilter = contact -> {
            if (contact.getRelations() == null || contact.getRelations().isEmpty()) {
                return false;
            }

            if (ars != null) {
                List<String> allContactArs = contact
                    .getRelations()
                    .stream()
                    .map(ZustaendigkeitEntity::getArs)
                    .toList();

                Collection<String> givenArsHierarchy = new ARS(ars).getArsHierarchy();
                return !CollectionUtils.intersection(givenArsHierarchy, allContactArs).isEmpty();
            }

            return false;
        };

        oe.setEmployees(oe.getEmployees().stream().filter(kontaktPersonenFilter).toList());
    }

    private List<Communication> getCommunications(
        @Nullable String ars,
        OrganisationseinheitVL oeVl
    ) {
        List<Communication> communications;
        if (StringUtils.isNotBlank(ars)) {
            // Setze konkreteste passende Liste von Kommunikationssystemen
            communications = getMostConcreteMatchingCommunicationsInHierarchy(oeVl, ars);
        } else {
            // Beziehe Kommunikationssysteme aus den persistierten Relationen,
            // nimm irgendeine Liste, die öffentliche Einträge enthält:
            communications = oeVl
                .getCommunicationSystems()
                .values()
                .stream()
                .findFirst()
                .orElse(Collections.emptyList());
        }
        return communications;
    }

    public List<String> findArsByOeIdAndLeikaId(String leikaId, String oeId) {
        return elasticQueryConnector.findArsByOeIdAndLeikaId(leikaId, oeId);
    }
}
