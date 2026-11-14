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

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;

import de.dataport.suchdienst.common.model.ARS;
import de.dataport.suchdienst.indexer.model.mapping.LifeSituation;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleLink;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.indexer.parser.ParserSelector;
import de.dataport.suchdienst.indexer.service.LeikaService;
import de.dataport.suchdienst.indexer.util.ModulesStringExtractor;
import de.dataport.suchdienst.suchen.dynamicparams.DynamicParamsFetcherService;
import de.dataport.suchdienst.suchen.model.mapper.CommonMapper;
import de.dataport.suchdienst.suchen.model.mapper.FormularMapper;
import de.dataport.suchdienst.suchen.model.mapper.OnlinedienstMapper;
import de.dataport.suchdienst.suchen.model.mapper.ServiceDescriptionBlockMapper;
import de.dataport.suchdienst.suchen.model.mapper.ServiceDescriptionBlockV6Mapper;
import de.dataport.suchdienst.suchen.model.mapper.ServiceDescriptionMapper;
import de.dataport.suchdienst.suchen.model.out.leistung.LifeSituationOutModel;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelBase;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelV5;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelV7;
import de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlock;
import de.dataport.suchdienst.suchen.model.out.leistung.SourcePortal;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceDescriptionDetail;
import de.dataport.suchdienst.suchen.model.out.leistung.resultlist.ServiceDescriptionSearchHit;
import de.dataport.suchdienst.suchen.service.legacy.AccessChannelModuleAggregator;
import de.dataport.suchdienst.suchen.service.legacy.DetailModuleTitleRenamer;
import de.dataport.suchdienst.suchen.service.legacy.DetailSorting;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class VerwaltungsleistungMappingService {

    private static final String NO_NAME_AVAILABLE = "Kein Name verfügbar";

    private static final ServiceModuleCode[] NAME_PRIORITY = {
        ServiceModuleCode.LEISTUNGSNAME,
        ServiceModuleCode.LEISTUNGSBEZEICHNUNG,
    };

    private static final ServiceModuleCode[] DESCRIPTION_PRIORITY = {
        ServiceModuleCode.TEASER,
        ServiceModuleCode.VOLLTEXT,
    };

    private final LeikaService leikaService;
    private final OrganisationModelConverterService oeModelConverterService;
    private final DynamicParamsFetcherService dynamicParamsFetcherService;

    public VerwaltungsleistungMappingService(
        LeikaService leikaService,
        OrganisationModelConverterService oeModelConverterService,
        DynamicParamsFetcherService dynamicParamsFetcherService
    ) {
        this.leikaService = leikaService;
        this.oeModelConverterService = oeModelConverterService;
        this.dynamicParamsFetcherService = dynamicParamsFetcherService;
    }

    public ServiceDescriptionDetail mapToDetailV6(
        Verwaltungsleistung vl,
        String ars,
        Locale language,
        LocalDate validDate
    ) {
        var outServiceDescription = new ServiceDescriptionDetail();

        // Sprache > Code > ZK
        // Sprach-sensitiv: Details, Name, Description, Ursprungsportale
        var vlDetailsByLang = getVLDetails(vl, language);
        var vlSpsByLang = getVLSps(vl, language);
        var vlOdsByLang = getVLOds(vl, language)
            .stream()
            .filter(od -> hasValidPeriods(od.getValidityPeriods(), validDate))
            .toList();

        // Wende dynamische Parameter auf die Onlinedienste an.
        // Es werden keine Links in otherLanguages verarbeitet, deshalb muss diese Funktion nach `applyLanguageToOnlinedienste` ausgeführt werden
        dynamicParamsFetcherService.applyDynamicParamsToOnlinedienste(vl, vlOdsByLang, ars);

        var details = ServiceModuleSpecializationUtil.applySpecializations(
            vlDetailsByLang,
            vlSpsByLang,
            ars,
            validDate
        );

        outServiceDescription.setName(
            getTextModuleContentWithFallback(
                details,
                vl,
                ars,
                validDate,
                language,
                NAME_PRIORITY
            ).orElse(NO_NAME_AVAILABLE)
        );

        outServiceDescription.setLeikakeys(vl.getLeistungsbeschreibung().getLeikakeys());
        outServiceDescription.setId(vl.getLeistungsbeschreibung().getId());
        outServiceDescription.setLanguageCode(language.toLanguageTag());
        outServiceDescription.setArs(vl.getLeistungsbeschreibung().getArs());
        outServiceDescription.setLanguageComplete(computeLanguageComplete(vl, ars));
        outServiceDescription.setAvailableLanguages(mapAvailableLanguages(vl));
        outServiceDescription.setOrganisations(
            oeModelConverterService.buildOeInfoOutModels(
                vl
                    .getOrganisationseinheiten()
                    .stream()
                    .filter(oe -> hasValidPeriods(oe.getValidityPeriods(), validDate))
                    .toList(),
                ars,
                language
            )
        );
        outServiceDescription.setLastUpdate(vl.getLastUpdate());
        outServiceDescription.setExternalLastUpdate(
            vl.getLeistungsbeschreibung().getExternalLastUpdate()
        );
        outServiceDescription.setValidityPeriods(
            CommonMapper.INSTANCE.map(vl.getLeistungsbeschreibung().getValidityPeriods())
        );
        var mapToLang = outServiceDescription
            .getAvailableLanguages()
            .contains(language.toLanguageTag())
            ? language
            : Locale.GERMAN;

        outServiceDescription.setDetails(detailsToServiceBlock(details, mapToLang));
        outServiceDescription.setOnlineServiceLinks(
            OnlinedienstMapper.INSTANCE.getOnlineServiceLinksV6(vlOdsByLang)
        );
        outServiceDescription.setPersonalMatters(buildLifeSituationOutModel(language, vl));
        outServiceDescription.setForms(FormularMapper.INSTANCE.map(getVLForms(vl, language)));

        return outServiceDescription;
    }

    /**
     * Mapping für die Beta-Such-API (/v8beta1/servicedescriptions). Erzeugt eine reduzierte Sicht
     * auf eine Leistung als Teilmenge von {@link ServiceDescriptionDetail}. Es werden keine Detail-
     * Module als Map ausgegeben; stattdessen wird ein Teaser als Vorschautext geliefert.
     * <p>
     * Name und Teaser werden mit Zugangskanal-Priorisierung (ONLINE {@literal >} ONLINE/OFFLINE
     * {@literal >} OFFLINE) und unter Berücksichtigung von Spezialisierungen ermittelt. Es wird die
     * bestehende Logik aus {@link #mapToDetailV6} wiederverwendet.
     */
    public ServiceDescriptionSearchHit mapToV8Beta1(
        Verwaltungsleistung vl,
        String ars,
        Locale language,
        LocalDate validDate
    ) {
        // Sprache > Code > ZK
        // Sprach-sensitiv: Details, Name, Teaser
        var vlDetailsByLang = getVLDetails(vl, language);
        var vlSpsByLang = getVLSps(vl, language);
        var vlOdsByLang = getVLOds(vl, language)
            .stream()
            .filter(od -> hasValidPeriods(od.getValidityPeriods(), validDate))
            .toList();

        // Onlinedienste sollen in der Beta-API immer dynamische Parameter anwenden.
        // Es werden keine Links in otherLanguages verarbeitet, deshalb muss diese Funktion nach `applyLanguageToOnlinedienste` ausgeführt werden
        dynamicParamsFetcherService.applyDynamicParamsToOnlinedienste(vl, vlOdsByLang, ars);

        var details = ServiceModuleSpecializationUtil.applySpecializations(
            vlDetailsByLang,
            vlSpsByLang,
            ars,
            validDate
        );

        return ServiceDescriptionSearchHit.builder()
            .name(
                getTextModuleContentWithFallback(
                    details,
                    vl,
                    ars,
                    validDate,
                    language,
                    NAME_PRIORITY
                ).orElse(NO_NAME_AVAILABLE)
            )
            .teaser(
                getTextModuleContentWithFallback(
                    details,
                    vl,
                    ars,
                    validDate,
                    language,
                    ServiceModuleCode.TEASER
                ).orElse("")
            )
            .id(vl.getLeistungsbeschreibung().getId())
            .arsFederalstate(vl.getLeistungsbeschreibung().getArsFederalstate())
            .languageComplete(computeLanguageComplete(vl, ars))
            .onlineServiceLinks(OnlinedienstMapper.INSTANCE.getOnlineServiceLinksV6(vlOdsByLang))
            .availableLanguages(mapAvailableLanguages(vl))
            .build();
    }

    public SdOutModelV5 mapToV5(
        Verwaltungsleistung vl,
        String ars,
        Locale language,
        LocalDate validDate,
        boolean applyDynamicParams
    ) {
        SdOutModelV5 outServiceDescription = mapVlToInstanceOf(
            vl,
            ars,
            language,
            validDate,
            new SdOutModelV5()
        );

        var vlOdsByLang = getVLOds(vl, language)
            .stream()
            .filter(od -> hasValidPeriods(od.getValidityPeriods(), validDate))
            .toList();

        if (applyDynamicParams) {
            // Wende dynamische Parameter auf die Onlinedienste an.
            // Es werden keine Links in otherLanguages verarbeitet, deshalb muss diese Funktion nach `applyLanguageToOnlinedienste` ausgeführt werden
            dynamicParamsFetcherService.applyDynamicParamsToOnlinedienste(vl, vlOdsByLang, ars);
        }

        outServiceDescription.setOnlineServiceLinks(
            OnlinedienstMapper.INSTANCE.getOnlineServiceLinksV4(vlOdsByLang)
        );
        outServiceDescription.setPersonalMatters(buildLifeSituationOutModel(language, vl));
        outServiceDescription.setForms(FormularMapper.INSTANCE.map(getVLForms(vl, language)));

        return outServiceDescription;
    }

    public SdOutModelV7 mapToV7(
        Verwaltungsleistung vl,
        String ars,
        Locale language,
        LocalDate validDate,
        boolean applyDynamicParams
    ) {
        SdOutModelV7 outServiceDescription = mapVlToInstanceOf(
            vl,
            ars,
            language,
            validDate,
            new SdOutModelV7()
        );

        var vlOdsByLang = getVLOds(vl, language)
            .stream()
            .filter(od -> hasValidPeriods(od.getValidityPeriods(), validDate))
            .toList();

        if (applyDynamicParams) {
            // Wende dynamische Parameter auf die Onlinedienste an.
            // Es werden keine Links in otherLanguages verarbeitet, deshalb muss diese Funktion nach `applyLanguageToOnlinedienste` ausgeführt werden
            dynamicParamsFetcherService.applyDynamicParamsToOnlinedienste(vl, vlOdsByLang, ars);
        }

        outServiceDescription.setOnlineServiceLinks(
            OnlinedienstMapper.INSTANCE.getOnlineServiceLinks(vlOdsByLang)
        );

        return outServiceDescription;
    }

    private <T extends SdOutModelBase> T mapVlToInstanceOf(
        Verwaltungsleistung vl,
        String ars,
        Locale language,
        LocalDate validDate,
        T outModel
    ) {
        Objects.requireNonNull(outModel, "outServiceDescription must not be null");

        // Prio: Sprache > Code
        // Sprach-sensitiv: Details, Name, Description, Ursprungsportale, (Synonyme, Suggestions)
        var vlDetailsByLang = getVLDetails(vl, language);
        var vlSpsByLang = getVLSps(vl, language);
        var vlSuggestionsByLang = getVLSuggestions(vl, language);
        var vlSynonymsByLang = getVLSynonyms(vl, language);

        var details = ServiceModuleSpecializationUtil.applySpecializations(
            vlDetailsByLang,
            vlSpsByLang,
            ars,
            validDate
        );

        details = AccessChannelModuleAggregator.mergeBlocks(details);
        var vlSourcePortals = details.getOrDefault(
            ServiceModuleCode.URSPRUNGSPORTAL.getCode(),
            List.of()
        );

        outModel.setName(
            getTextModuleContentWithFallback(
                details,
                vl,
                ars,
                validDate,
                language,
                NAME_PRIORITY
            ).orElse(NO_NAME_AVAILABLE)
        );

        if (outModel instanceof SdOutModelV5 outModelV5) {
            // Da V5 kein Teaser-Feld hat, wird der Teaser - falls vorhanden - in "description" geschrieben
            outModelV5.setDescription(
                getTextModuleContentWithFallback(
                    details,
                    vl,
                    ars,
                    validDate,
                    language,
                    DESCRIPTION_PRIORITY
                ).orElse("")
            );
        } else if (outModel instanceof SdOutModelV7 outModelV7) {
            // Bei V7 sind description und teaser getrennt
            outModelV7.setDescription(
                getTextModuleContentWithFallback(
                    details,
                    vl,
                    ars,
                    validDate,
                    language,
                    ServiceModuleCode.VOLLTEXT
                ).orElse("")
            );
            outModelV7.setTeaser(
                getTextModuleContentWithFallback(
                    details,
                    vl,
                    ars,
                    validDate,
                    language,
                    ServiceModuleCode.TEASER
                ).orElse("")
            );
        }

        outModel.setTimestamp(vl.getLeistungsbeschreibung().getLastUpdate());
        outModel.setValidFrom(
            CommonMapper.INSTANCE.mapValidFrom(vl.getLeistungsbeschreibung().getValidityPeriods())
        );
        outModel.setValidTo(
            CommonMapper.INSTANCE.mapValidTo(vl.getLeistungsbeschreibung().getValidityPeriods())
        );

        outModel.setSdgInformation(
            ServiceDescriptionMapper.INSTANCE.mapCodelistEntries(
                vl.getLeistungsbeschreibung().getSdgInformation()
            )
        );
        outModel.setAddressees(vl.getLeistungsbeschreibung().getAddressees());
        outModel.setArsFederalstate(vl.getLeistungsbeschreibung().getArsFederalstate());
        outModel.setSuggestions(vlSuggestionsByLang);
        outModel.setSynonyms(vlSynonymsByLang);
        outModel.setTyping(
            ServiceDescriptionMapper.INSTANCE.map(vl.getLeistungsbeschreibung().getTyping())
        );
        List<SourcePortal> sourcePortals = vlSourcePortals
            .stream()
            .filter(ServiceModuleLink.class::isInstance)
            .map(ServiceModuleLink.class::cast)
            .filter(m -> Objects.nonNull(m.getLink()))
            // XZuFi Ursprungsportal erlaubt nur eine URI, deshalb hier getFirst()
            .map(m -> new SourcePortal(m.getLink().title(), m.getLink().uri(), false))
            .toList();
        outModel.setSourcePortals(sourcePortals);

        outModel.setLeikakeys(vl.getLeistungsbeschreibung().getLeikakeys());
        outModel.setId(vl.getLeistungsbeschreibung().getId());
        outModel.setLanguageCode(language.toLanguageTag());

        outModel.setArs(vl.getLeistungsbeschreibung().getArs());
        outModel.setLanguageComplete(computeLanguageComplete(vl, ars));
        outModel.setAvailableLanguages(mapAvailableLanguages(vl));
        outModel.setOrganisationIds(
            vl
                .getOrganisationseinheiten()
                .stream()
                .filter(oe -> hasValidPeriods(oe.getValidityPeriods(), validDate))
                .map(OrganisationseinheitVL::getId)
                .collect(toSet())
        );
        outModel.setLastUpdate(vl.getLastUpdate());
        outModel.setExternalLastUpdate(vl.getLeistungsbeschreibung().getExternalLastUpdate());
        outModel.setValidityPeriods(
            CommonMapper.INSTANCE.map(vl.getLeistungsbeschreibung().getValidityPeriods())
        );
        outModel.setDetails(detailsToSeDeBlock(details));
        outModel.setOnlineServiceIds(mapOnlineServiceIds(vl, validDate));
        return outModel;
    }

    private Optional<String> getTextModuleContentWithFallback(
        Map<String, List<AbstractServiceModule>> details,
        Verwaltungsleistung vl,
        String ars,
        LocalDate validDate,
        Locale language,
        ServiceModuleCode... codes
    ) {
        Optional<String> content = ModulesStringExtractor.findFirstTextByModuleAndChannelPriority(
            details,
            codes
        );

        if (!languageIsGermanOrNotAvailable(vl, language) && content.isEmpty()) {
            var germanDetailsSpecialized = ServiceModuleSpecializationUtil.applySpecializations(
                getVLDetails(vl, Locale.GERMAN),
                getVLSps(vl, Locale.GERMAN),
                ars,
                validDate,
                Arrays.stream(codes)
                    .map(ServiceModuleCode::getCode)
                    .toArray(String[]::new)
            );
            content = ModulesStringExtractor.findFirstTextByModuleAndChannelPriority(
                germanDetailsSpecialized,
                codes
            );
        }

        return content;
    }

    private List<de.dataport.suchdienst.suchen.model.out.leistung.detail.AbstractServiceModule> detailsToServiceBlock(
        Map<String, List<AbstractServiceModule>> details,
        Locale language
    ) {
        return details
            .entrySet()
            .stream()
            .flatMap(entrySet ->
                entrySet
                    .getValue()
                    .stream()
                    .map(m ->
                        ServiceModuleCode.fromCode(entrySet.getKey())
                            .map(ServiceModuleCode::getCategory)
                            .map(category ->
                                ServiceDescriptionBlockV6Mapper.INSTANCE.map(
                                    m,
                                    entrySet
                                        .getKey()
                                        .replaceFirst(
                                            "^" + ParserSelector.INDIVIDUAL_MODULE_PREFIX,
                                            ""
                                        ),
                                    category
                                )
                            )
                            .orElse(null)
                    )
            )
            .filter(Objects::nonNull)
            .map(m -> DetailModuleTitleRenamer.renameModuleTitle(m, language))
            .sorted(DetailSorting.getModuleComparatorV6())
            .toList();
    }

    private List<SeDeBlock> detailsToSeDeBlock(Map<String, List<AbstractServiceModule>> details) {
        return details
            .entrySet()
            .stream()
            // Ursprungsportale werden in V5/V7 separat gemappt
            .filter(entry -> !ServiceModuleCode.URSPRUNGSPORTAL.getCode().equals(entry.getKey()))
            .flatMap(entrySet ->
                ServiceModuleCode.fromCode(entrySet.getKey())
                    .map(ServiceModuleCode::getCategory)
                    .map(category ->
                        entrySet
                            .getValue()
                            .stream()
                            .map(m ->
                                ServiceDescriptionBlockMapper.INSTANCE.map(
                                    m,
                                    entrySet
                                        .getKey()
                                        .replaceFirst(
                                            "^" + ParserSelector.INDIVIDUAL_MODULE_PREFIX,
                                            ""
                                        ),
                                    category
                                )
                            )
                    )
                    .orElse(null)
            )
            .filter(Objects::nonNull)
            .toList();
    }

    private Map<Locale, Boolean> computeLanguageComplete(Verwaltungsleistung vl, String ars) {
        Map<Locale, Boolean> languageCompleteLb = vl
            .getLeistungsbeschreibung()
            .getLanguageComplete();
        Map<Locale, Boolean> languageComplete = new HashMap<>(languageCompleteLb);

        languageCompleteLb.forEach((locale, complete) -> {
            if (
                vl
                    .getOnlinedienste()
                    .stream()
                    .anyMatch(od -> !od.getLanguageComplete().getOrDefault(locale, false)) ||
                vl
                    .getFormulare()
                    .stream()
                    .anyMatch(fl -> !fl.getLanguageComplete().getOrDefault(locale, false)) ||
                getSpecializationsByHierarchy(vl.getSpezialisierungen(), ars)
                    .stream()
                    .anyMatch(sp -> !sp.getLanguageComplete().getOrDefault(locale, false))
            ) {
                languageComplete.put(locale, false);
            }
        });

        return languageComplete;
    }

    private Map<String, List<AbstractServiceModule>> getVLDetails(
        Verwaltungsleistung vl,
        Locale language
    ) {
        return vl
            .getLeistungsbeschreibung()
            .copyByLanguage(language)
            .orElse(vl.getLeistungsbeschreibung().getDetails());
    }

    private Set<String> getVLSynonyms(Verwaltungsleistung vl, Locale language) {
        return languageIsGermanOrNotAvailable(vl, language)
            ? vl.getLeistungsbeschreibung().getSynonyms()
            : Optional.ofNullable(vl.getLeistungsbeschreibung().getOtherLanguages().get(language))
                  .map(LeistungsbeschreibungContent::getSynonyms)
                  .orElse(Set.of());
    }

    private Set<String> getVLSuggestions(Verwaltungsleistung vl, Locale language) {
        return languageIsGermanOrNotAvailable(vl, language)
            ? vl.getLeistungsbeschreibung().getSuggestions()
            : Optional.ofNullable(vl.getLeistungsbeschreibung().getOtherLanguages().get(language))
                  .map(LeistungsbeschreibungContent::getSuggestions)
                  .orElse(Set.of());
    }

    private List<SpezialisierungVL> getVLSps(Verwaltungsleistung vl, Locale language) {
        return vl
            .getSpezialisierungen()
            .stream()
            .map(sp -> sp.copyByLanguage(language))
            .flatMap(Optional::stream)
            .toList();
    }

    private List<FormularVL> getVLForms(Verwaltungsleistung vl, Locale language) {
        // Liefert Formulare für die angefragte Sprache
        // Strikter Modus: Nur Formulare mit Inhalten in der angefragten Sprache werden zurückgegeben, es werden keine Fallbacks angewendet.
        return vl
            .getFormulare()
            .stream()
            .map(form -> form.copyByLanguage(language))
            .flatMap(Optional::stream)
            .toList();
    }

    private List<OnlinedienstVL> getVLOds(Verwaltungsleistung vl, Locale language) {
        return vl
            .getOnlinedienste()
            .stream()
            .map(od ->
                od
                    .copyByLanguage(
                        vl.getLeistungsbeschreibung().getAvailableLanguages().contains(language)
                            ? language
                            : Locale.GERMAN
                    )
                    .orElse(od)
            )
            .toList();
    }

    private Set<String> mapAvailableLanguages(Verwaltungsleistung vl) {
        return vl
            .getLeistungsbeschreibung()
            .getAvailableLanguages()
            .stream()
            .map(Locale::toLanguageTag)
            .collect(toSet());
    }

    private Set<String> mapOnlineServiceIds(Verwaltungsleistung vl, LocalDate validDate) {
        return vl
            .getOnlinedienste()
            .stream()
            .filter(od -> hasValidPeriods(od.getValidityPeriods(), validDate))
            .map(OnlinedienstVL::getId)
            .collect(toSet());
    }

    private boolean hasValidPeriods(List<ValidityPeriod> validityPeriods, LocalDate validDate) {
        return (
            CollectionUtils.isEmpty(validityPeriods) ||
            validityPeriods.stream().anyMatch(period -> period.isValidDate(validDate))
        );
    }

    private List<LifeSituationOutModel> buildLifeSituationOutModel(
        Locale searchLanguage,
        Verwaltungsleistung vl
    ) {
        SortedSet<String> sortedVersions = new TreeSet<>(Collections.reverseOrder());
        sortedVersions.addAll(vl.getLeistungsbeschreibung().getLifeSituationVersions());
        return vl
            .getLeistungsbeschreibung()
            .getLifeSituations()
            .stream()
            .map(ls -> recursiveCreateLifeSituationOutModel(ls, sortedVersions, searchLanguage))
            .sorted(comparing(LifeSituationOutModel::getCode))
            .toList();
    }

    private LifeSituationOutModel recursiveCreateLifeSituationOutModel(
        LifeSituation lifeSituation,
        SortedSet<String> versions,
        Locale locale
    ) {
        var code = lifeSituation.getCode();
        var name = leikaService.getLatestText(versions, code, locale);
        var children = lifeSituation.getChildren();
        if (children.isEmpty()) {
            return new LifeSituationOutModel(code, name, List.of());
        } else {
            var childOutModels = children
                .stream()
                .map(lifeSit -> recursiveCreateLifeSituationOutModel(lifeSit, versions, locale))
                .sorted(comparing(LifeSituationOutModel::getCode))
                .toList();
            return new LifeSituationOutModel(code, name, childOutModels);
        }
    }

    private static boolean languageIsGermanOrNotAvailable(
        Verwaltungsleistung verwaltungsleistung,
        Locale language
    ) {
        return (
            language.equals(Locale.GERMAN) ||
            !verwaltungsleistung
                .getLeistungsbeschreibung()
                .getAvailableLanguages()
                .contains(language)
        );
    }

    private static List<SpezialisierungVL> getSpecializationsByHierarchy(
        List<SpezialisierungVL> vlSpecializations,
        String ars
    ) {
        Collection<String> arsHierarchy = new ARS(ars).getArsHierarchy();
        // Alle Spezialisierungen, die in die ARS-Hierarchie passen und jeweils nur die speziellste ARS behalten
        return vlSpecializations
            .stream()
            .filter(sp -> sp.getArs().stream().anyMatch(arsHierarchy::contains))
            .toList();
    }
}
