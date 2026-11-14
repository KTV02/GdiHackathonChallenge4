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

import de.dataport.suchdienst.common.model.ARS;
import de.dataport.suchdienst.indexer.model.AccessChannel;
import de.dataport.suchdienst.indexer.model.mapper.BusinessObjectMapper;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.model.mapping.XZufiCategory;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceBlockSpecialization;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDate;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleLink;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

public class ServiceModuleSpecializationUtil {

    /**
     * Hilfs-Record zur effizienten Verarbeitung von Spezialisierungen mit ihrer besten ARS.
     * Vermeidet doppelte Berechnungen in Filter- und Sortieroperationen.
     */
    private record SpecializationWithBestArs(
        SpezialisierungVL specialization,
        String normalizedBestArs
    ) {}

    /**
     * Allgemeiner Pair-Record für verschiedene Zwecke in der Klasse.
     */
    private record Pair<T, U>(T first, U second) {}

    private static final Set<String> NO_CLEARING_ALLOWED = Set.of(
        ServiceModuleCode.TEASER.getCode(),
        ServiceModuleCode.VOLLTEXT.getCode(),
        ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
        ServiceModuleCode.LEISTUNGSNAME.getCode()
    );

    public static Map<String, List<AbstractServiceModule>> applySpecializations(
        Map<String, List<AbstractServiceModule>> serviceLbDetails,
        List<SpezialisierungVL> serviceSps,
        String ars,
        LocalDate validDate,
        String... limitedCodes
    ) {
        if (serviceLbDetails == null) {
            return new HashMap<>();
        }

        var hierarchyOrderedSps = getHierarchySpecializations(serviceSps, ars);
        var groupedSps = groupSpsByCode(hierarchyOrderedSps);
        var codesToProcess =
            limitedCodes != null && limitedCodes.length > 0
                ? Stream.of(limitedCodes)
                : Stream.concat(
                      serviceLbDetails.keySet().stream(),
                      groupedSps.keySet().stream()
                  ).distinct();

        return codesToProcess
            .flatMap(code -> processCode(code, serviceLbDetails, groupedSps, validDate))
            .collect(
                Collectors.groupingBy(
                    Map.Entry::getKey,
                    Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                )
            );
    }

    private static Stream<Map.Entry<String, AbstractServiceModule>> processCode(
        String code,
        Map<String, List<AbstractServiceModule>> serviceLbDetails,
        Map<String, List<ServiceBlockSpecialization>> groupedSps,
        LocalDate validDate
    ) {
        var modulesByCode = serviceLbDetails.getOrDefault(code, List.of());
        var spsByCode = groupedSps.getOrDefault(code, List.of());
        var isTextModule = isTextModule(code);

        if (modulesByCode.isEmpty() && spsByCode.isEmpty()) {
            return Stream.empty();
        }

        List<AbstractServiceModule> specializedModules = new ArrayList<>(modulesByCode);
        for (var sp : spsByCode) {
            if (sp.serviceBlock().hasContent() || canBeCleared(code)) {
                specializedModules = applySpecialization(specializedModules, sp, isTextModule);
            }
        }

        return specializedModules
            .stream()
            .filter(AbstractServiceModule::hasContent)
            .filter(m -> isValid(m.getValidityPeriods(), validDate))
            .map(m -> Map.entry(code, m));
    }

    private static boolean canBeCleared(String moduleCode) {
        return !NO_CLEARING_ALLOWED.contains(moduleCode);
    }

    private static boolean isTextModule(String code) {
        return ServiceModuleCode.fromCode(code)
            .map(
                c ->
                    XZufiCategory.TEXT_MODUL.equals(c.getCategory()) ||
                    XZufiCategory.TEXT_MODUL_INDIVIDUELL.equals(c.getCategory())
            )
            .orElse(false);
    }

    private static List<AbstractServiceModule> applySpecialization(
        List<AbstractServiceModule> modules,
        @NonNull ServiceBlockSpecialization specialization,
        boolean isTextModule
    ) {
        AccessChannel specializationChannel = specialization.serviceBlock().getAccessChannel();
        var modulesByChannel = groupModulesByAccessChannel(modules);

        // Sonderfall: Ersatz für sowohl ONLINE- als auch OFFLINE-Module mit ONLINE_OFFLINE-Spezialisierung
        boolean replacesOnlineAndOffline =
            specialization.isReplacement() &&
            modulesByChannel.containsKey(AccessChannel.ONLINE) &&
            modulesByChannel.containsKey(AccessChannel.OFFLINE) &&
            specializationChannel == AccessChannel.ONLINE_OFFLINE;

        // Falls wir ein kombiniertes ONLINE_OFFLINE-Modul haben, aber eine Spezialisierung nur für ONLINE oder OFFLINE, das Modul splitten
        EnumMap<AccessChannel, AbstractServiceModule> modulesToSpecialize;
        if (
            modulesByChannel.containsKey(AccessChannel.ONLINE_OFFLINE) &&
            specializationChannel != AccessChannel.ONLINE_OFFLINE
        ) {
            modulesToSpecialize = splitModule(modulesByChannel.get(AccessChannel.ONLINE_OFFLINE));
        } else if (replacesOnlineAndOffline) {
            // Spezialisierung ersetzt sowohl ONLINE- als auch OFFLINE-Module
            return List.of(specialization.serviceBlock());
        } else {
            modulesToSpecialize = modulesByChannel;
        }

        // Falls wir ONLINE- oder OFFLINE-Module haben, aber eine Spezialisierung für ONLINE_OFFLINE, die Spezialisierung splitten
        List<ServiceBlockSpecialization> specializationsToApply = List.of(specialization);
        if (
            (modulesByChannel.containsKey(AccessChannel.ONLINE) ||
                modulesByChannel.containsKey(AccessChannel.OFFLINE)) &&
            specializationChannel == AccessChannel.ONLINE_OFFLINE
        ) {
            specializationsToApply = splitSpecialization(specialization);
        }

        List<AbstractServiceModule> specializedModules = new ArrayList<>();

        // Jede Spezialisierung auf das entsprechende Modul anwenden (oder neues erstellen, falls keines existiert)
        for (ServiceBlockSpecialization spToApply : specializationsToApply) {
            AccessChannel channel = spToApply.serviceBlock().getAccessChannel();
            AbstractServiceModule module = modulesToSpecialize.get(channel);

            if (module == null) {
                // Spezialisierung erzeugt ein neues Modul
                specializedModules.add(spToApply.serviceBlock());
            } else {
                // Existierendes Modul spezialisieren
                specializedModules.add(applySpecializationModule(module, spToApply, isTextModule));
            }
        }

        // Alle Module hinzufügen, die nicht spezialisiert wurden
        for (Map.Entry<
            AccessChannel,
            AbstractServiceModule
        > entry : modulesToSpecialize.entrySet()) {
            AccessChannel channel = entry.getKey();

            if (specializedModules.stream().noneMatch(m -> m.getAccessChannel() == channel)) {
                specializedModules.add(entry.getValue());
            }
        }

        return specializedModules;
    }

    private static EnumMap<AccessChannel, AbstractServiceModule> groupModulesByAccessChannel(
        List<AbstractServiceModule> modules
    ) {
        // TODO: Für den Fall, dass es mehrere Module mit dem gleichen Zugangskanal gibt, muss eine Lösung erarbeitet werden. XZuFi erlaubt eigentlich keine doppelten Zugangskanäle. Das ist aber bisher nicht technisch verhindert.
        return modules
            .stream()
            .collect(
                Collectors.toMap(
                    AbstractServiceModule::getAccessChannel,
                    m -> m,
                    (m1, _) -> m1,
                    () -> new EnumMap<>(AccessChannel.class)
                )
            );
    }

    private static List<ServiceBlockSpecialization> splitSpecialization(
        @NonNull ServiceBlockSpecialization onlineOfflineSp
    ) {
        ServiceBlockSpecialization spOnline;
        ServiceBlockSpecialization spOffline;

        spOnline = new ServiceBlockSpecialization(
            onlineOfflineSp.isReplacement(),
            cloneServiceModuleWithAccessChannel(
                onlineOfflineSp.serviceBlock(),
                AccessChannel.ONLINE
            )
        );
        spOffline = new ServiceBlockSpecialization(
            onlineOfflineSp.isReplacement(),
            cloneServiceModuleWithAccessChannel(
                onlineOfflineSp.serviceBlock(),
                AccessChannel.OFFLINE
            )
        );

        return List.of(spOnline, spOffline);
    }

    private static EnumMap<AccessChannel, AbstractServiceModule> splitModule(
        AbstractServiceModule onlineOfflineModule
    ) {
        var modules = new EnumMap<AccessChannel, AbstractServiceModule>(AccessChannel.class);
        modules.put(
            AccessChannel.ONLINE,
            cloneServiceModuleWithAccessChannel(onlineOfflineModule, AccessChannel.ONLINE)
        );
        modules.put(
            AccessChannel.OFFLINE,
            cloneServiceModuleWithAccessChannel(onlineOfflineModule, AccessChannel.OFFLINE)
        );
        return modules;
    }

    private static AbstractServiceModule cloneServiceModuleWithAccessChannel(
        @NonNull AbstractServiceModule module,
        @NonNull AccessChannel accessChannel
    ) {
        return switch (module) {
            case ServiceModule m -> {
                ServiceModule copy = BusinessObjectMapper.INSTANCE.shallowCopy(m);
                copy.setAccessChannel(accessChannel);
                yield copy;
            }
            case ServiceModuleDetail m -> {
                ServiceModuleDetail copy = BusinessObjectMapper.INSTANCE.shallowCopy(m);
                copy.setAccessChannel(accessChannel);
                yield copy;
            }
            case ServiceModuleDate m -> {
                ServiceModuleDate copy = BusinessObjectMapper.INSTANCE.shallowCopy(m);
                copy.setAccessChannel(accessChannel);
                yield copy;
            }
            case ServiceModuleLink m -> {
                ServiceModuleLink copy = BusinessObjectMapper.INSTANCE.shallowCopy(m);
                copy.setAccessChannel(accessChannel);
                yield copy;
            }
        };
    }

    private static Map<String, List<ServiceBlockSpecialization>> groupSpsByCode(
        List<SpezialisierungVL> sps
    ) {
        return sps
            .stream()
            .map(SpezialisierungContent::getDetails)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(
                Collectors.groupingBy(
                    Map.Entry::getKey,
                    Collectors.flatMapping(entry -> entry.getValue().stream(), Collectors.toList())
                )
            );
    }

    private static AbstractServiceModule applySpecializationModule(
        @NonNull AbstractServiceModule serviceModule,
        @NonNull ServiceBlockSpecialization serviceBlockSpecialization,
        boolean isTextModule
    ) {
        var isReplacement = serviceBlockSpecialization.isReplacement();
        var spModule = serviceBlockSpecialization.serviceBlock();

        if (serviceModule.getAccessChannel() != spModule.getAccessChannel()) {
            throw new IllegalStateException(
                "Zugangskanäle von Spezialisierung und zu spezialisierendem Modul stimmen nicht überein"
            );
        }

        if (isReplacement) {
            return spModule;
        }

        addValuesToModule(isTextModule, serviceModule, spModule);

        return serviceModule;
    }

    private static void addValuesToModule(
        boolean isTextModule,
        @NonNull AbstractServiceModule module,
        @NonNull AbstractServiceModule spModule
    ) {
        // Collections zusammenführen
        switch (new Pair<>(module, spModule)) {
            case Pair(ServiceModule sm, ServiceModule spSm) -> {
                sm.getAdditionalInformation().addAll(spSm.getAdditionalInformation());
                sm.getLinks().addAll(spSm.getLinks());
            }
            case Pair(ServiceModuleDetail smd, ServiceModuleDetail spSmd) -> {
                smd.getDetails().addAll(spSmd.getDetails());
                smd.getLinks().addAll(spSmd.getLinks());
            }
            case Pair(ServiceModuleDate smd, ServiceModuleDate spSmd) ->
                // Ergänzung von Datum ist nicht möglich, daher wird mit SP-Datum überschrieben
                smd.setDate(spSmd.getDate());
            case Pair(ServiceModuleLink smd, ServiceModuleLink spSmd) ->
                // Ergänzung von Link ist nicht möglich, daher wird mit SP-Link überschrieben
                smd.setLink(spSmd.getLink());
            default -> throw new IllegalStateException("Unerwarteter Wert oder Typ-Kombination");
        }
        module.getValidityPeriods().addAll(spModule.getValidityPeriods());
        module.setText(StringUtils.join(module.getText(), spModule.getText()));

        if (spModule.getTitle() != null && isTextModule) {
            module.setTitle(spModule.getTitle());
        }
    }

    private static boolean isValid(List<ValidityPeriod> validityPeriods, LocalDate validDate) {
        return (
            !CollectionUtils.isNotEmpty(validityPeriods) ||
            validityPeriods.stream().anyMatch(period -> period.isValidDate(validDate))
        );
    }

    /**
     * Filtert und sortiert Spezialisierungen basierend auf einer ARS-Hierarchie.
     *
     * <p>Diese Methode führt folgende Schritte aus:</p>
     * <ol>
     *   <li><strong>Filtern:</strong> Behält nur Spezialisierungen, die mindestens eine ARS haben,
     *       welche in der gegebenen ARS-Hierarchie enthalten ist</li>
     *   <li><strong>Beste ARS ermitteln:</strong> Für Spezialisierungen mit mehreren ARS wird die
     *       spezifischste (längste nach Entfernen der Endnullen) verwendet</li>
     *   <li><strong>Sortieren:</strong> Sortiert nach Spezifität der ARS, vom allgemeinsten zum
     *       spezifischsten</li>
     * </ol>
     *
     * <p><strong>ARS-Spezifität:</strong> Eine ARS ist spezifischer, wenn sie nach dem Entfernen
     * der Endnullen länger ist. Beispiel: "091234567890" ist spezifischer als "091234567000".</p>
     *
     * @param unsortedSp Liste der ungefilterten und unsortierten Spezialisierungen
     * @param ars Die Basis-ARS für die Hierarchiebestimmung (12 Zeichen lang)
     * @return Gefilterte und sortierte Liste von Spezialisierungen, wobei die ursprünglichen
     *         Objekte unverändert bleiben. Spezialisierungen ohne passende ARS werden ausgeschlossen.
     *
     * @throws NumberFormatException wenn eine normalisierte ARS nicht als Long geparst werden kann
     *
     * @see ARS#getArsHierarchy() für die Hierarchiebestimmung
     * @see #findBestMatchingArs(SpezialisierungVL, Collection) für die ARS-Auswahl
     * @see #normalizeArs(String) für die ARS-Normalisierung
     */
    private static List<SpezialisierungVL> getHierarchySpecializations(
        List<SpezialisierungVL> unsortedSp,
        String ars
    ) {
        Collection<String> arsHierarchy = new ARS(ars).getArsHierarchy();

        return unsortedSp
            .stream()
            // Nur Spezialisierungen mit ARS behalten
            .filter(sp -> ObjectUtils.isNotEmpty(sp.getArs()))
            // Beste ARS für jede Spezialisierung ermitteln
            .map(sp -> mapToSpecializationWithBestArs(sp, arsHierarchy))
            // Nur Spezialisierungen mit gültiger ARS in der Hierarchie behalten
            .filter(Objects::nonNull)
            // Nach ARS-Spezifität sortieren (allgemeinste zuerst)
            .sorted(createArsSpecificityComparator())
            // Ursprüngliche Spezialisierungen extrahieren
            .map(SpecializationWithBestArs::specialization)
            .toList();
    }

    /**
     * Mappt eine Spezialisierung zu einem SpecializationWithBestArs-Objekt,
     * falls eine passende ARS in der Hierarchie gefunden wird.
     */
    private static SpecializationWithBestArs mapToSpecializationWithBestArs(
        SpezialisierungVL specialization,
        Collection<String> arsHierarchy
    ) {
        return findBestMatchingArs(specialization, arsHierarchy)
            .map(normalizedBestArs ->
                new SpecializationWithBestArs(specialization, normalizedBestArs)
            )
            .orElse(null);
    }

    /**
     * Findet die spezifischste ARS einer Spezialisierung, die in der ARS-Hierarchie enthalten ist.
     * Gibt die bereits normalisierte ARS zurück (ohne trailing Nullen).
     *
     * @param specialization Die zu prüfende Spezialisierung
     * @param arsHierarchy Die verfügbare ARS-Hierarchie
     * @return Die spezifischste passende ARS (normalisiert) oder empty falls keine gefunden
     */
    private static Optional<String> findBestMatchingArs(
        SpezialisierungVL specialization,
        Collection<String> arsHierarchy
    ) {
        return specialization
            .getArs()
            .stream()
            .filter(arsHierarchy::contains)
            .map(ServiceModuleSpecializationUtil::normalizeArs)
            .max(Comparator.comparingInt(String::length));
    }

    /**
     * Erstellt einen Comparator für die Sortierung nach ARS-Spezifität.
     * Sortiert vom allgemeinsten zum spezifischsten (aufsteigend).
     */
    private static Comparator<SpecializationWithBestArs> createArsSpecificityComparator() {
        return Comparator.comparingInt(entry -> entry.normalizedBestArs().length());
    }

    /**
     * Entfernt trailing Nullen von einer ARS für Spezifitätsvergleiche.
     *
     * @param ars Die ARS (z.B. "091234567000")
     * @return ARS ohne trailing Nullen (z.B. "091234567")
     */
    private static String normalizeArs(String ars) {
        if (ars == null) {
            throw new NullPointerException("Ars was null");
        }
        return StringUtils.stripEnd(ars, "0");
    }
}
