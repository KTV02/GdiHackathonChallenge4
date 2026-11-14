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

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toSet;

import com.google.common.hash.Hashing;
import de.dataport.suchdienst.common.model.ARS;
import de.dataport.suchdienst.indexer.model.Identifiable;
import de.dataport.suchdienst.indexer.model.mapper.BusinessObjectMapper;
import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityCriterion;
import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityRole;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.indexer.parser.SearchTermsExtractor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Diese Klasse enthält den Kern-Algorithmus zum Erstellen von Verwaltungsleistungen aus einer Leistungsbeschreibung.
 *
 * <p><strong>Grundidee des Algorithmus:</strong></p>
 * <ul>
 *  <li>Ermittle für jeden ARS-Code die Menge der zugehörigen Geschäftsobjekte (Organisationseinheiten, Onlinedienste, Formulare, Spezialisierungen)</li>
 *  <li>Reduziere identische Geschäftsobjekt-Kombinationen durch Zusammenfassung der ARS-Codes</li>
 *  <li>Dadurch entstehen Kombinationen mit der maximalen Menge von ARS-Codes, bei denen jedes Geschäftsobjekt für jeden ARS-Code zuständig ist</li>
 *  <li>Generiere für jede reduzierte Kombination eine Verwaltungsleistung</li>
 * </ul>
 */
@Slf4j
public class VerwaltungsleistungenBuilder {

    private static final String BUND_ARS = "000000000000";
    private static final String BUNDESLAND_ARS_SUFFIX = "0000000000";

    private final ArsTree arsTree;
    private final String targetArsFederalState;
    private final boolean isBundImGemeinsamenVollzug;

    /**
     * ARS-Auflösungsstrategien für verschiedene Suchvarianten.
     */
    private enum ArsResolutionStrategy {
        /** Exakte ARS-Suche: ARS wird unverändert zurückgegeben */
        EXACT {
            @Override
            public Set<String> resolve(
                String ars,
                ArsTree arsTree,
                String targetState,
                boolean bundImVollzug
            ) {
                return Set.of(ars);
            }
        },

        /** Hierarchische ARS-Suche: Alle untergeordneten Gemeinden werden aufgelöst */
        HIERARCHICAL {
            @Override
            public Set<String> resolve(
                String ars,
                ArsTree arsTree,
                String targetState,
                boolean bundImVollzug
            ) {
                try {
                    if (bundImVollzug && BUND_ARS.equals(ars)) {
                        // Bundesleistungen im gemeinsamen Vollzug müssen alle Gemeinden des
                        // Zielbundeslandes bekommen
                        return arsTree.resolveToGemeinden(targetState + BUNDESLAND_ARS_SUFFIX);
                    }
                    return arsTree.resolveToGemeinden(ars);
                } catch (IllegalArgumentException e) {
                    log.warn("Fehler bei ARS-Auflösung für '{}': {}", ars, e.getMessage());
                    return Set.of(); // Leere Menge statt Exception
                }
            }
        };

        public abstract Set<String> resolve(
            String ars,
            ArsTree arsTree,
            String targetState,
            boolean bundImVollzug
        );
    }

    /**
     * Erstellt einen neuen VerwaltungsleistungenBuilder.
     *
     * @param arsTree Der ARS-Baum zur Auflösung von ARS-Hierarchien
     * @param targetArsFederalState Das Ziel-Bundesland für die ARS-Verarbeitung
     * @param isBundImGemeinsamenVollzug Flag, ob Bundesleistungen im gemeinsamen Vollzug vorliegen
     */
    public VerwaltungsleistungenBuilder(
        ArsTree arsTree,
        String targetArsFederalState,
        boolean isBundImGemeinsamenVollzug
    ) {
        this.arsTree = arsTree;
        this.targetArsFederalState = targetArsFederalState;
        this.isBundImGemeinsamenVollzug = isBundImGemeinsamenVollzug;
    }

    /**
     * Validiert einen ARS-Code auf korrekte Format und Länge.
     *
     * @param ars Der zu validierende ARS-Code
     * @return true wenn der ARS-Code gültig ist, false andernfalls
     */
    private boolean isValidArs(String ars) {
        return ars != null && ars.length() == 12 && ars.matches("\\d{12}");
    }

    /**
     * Löst einen ARS-Code entsprechend der gewählten Strategie auf.
     *
     * @param ars Der aufzulösende ARS-Code
     * @param exactArs true für exakte ARS-Zuordnung, false für hierarchische ARS-Auflösung
     * @return Menge aller zugehörigen ARS-Codes
     */
    private Set<String> resolveArs(String ars, boolean exactArs) {
        if (!isValidArs(ars)) {
            log.warn("Ungültiger ARS-Code ignoriert: {}", ars);
            return Set.of();
        }

        ArsResolutionStrategy strategy = exactArs
            ? ArsResolutionStrategy.EXACT
            : ArsResolutionStrategy.HIERARCHICAL;

        return strategy.resolve(ars, arsTree, targetArsFederalState, isBundImGemeinsamenVollzug);
    }

    /**
     * Erstellt Verwaltungsleistungen aus einer Leistungsbeschreibung durch Clustering der Geschäftsobjekte
     * nach gemeinsamen Zuständigkeitsbereichen. Dabei wird eine Ausmultiplikation der ARS-Codes durchgeführt,
     * sodass jede entstehende Verwaltungsleistung nur Geschäftsobjekte enthält, die alle in den gleichen
     * ARS-Bereichen zuständig sind.
     *
     * <p>Mit PV-10016 (Exakte ARS-Suche ermöglichen) werden zusätzliche Verwaltungsleistungen erstellt,
     * die bei der Suche nach exakten ARS-Codes gefunden werden können, wobei doppelte Suchtreffer vermieden werden.</p>
     *
     * @param lb Die Leistungsbeschreibung als Grundlage für die Verwaltungsleistungen
     * @param oes Die Organisationseinheiten aus der Leistungsbeschreibung
     * @param ods Die Onlinedienste aus der Leistungsbeschreibung
     * @param fos Die Formulare aus der Leistungsbeschreibung
     * @param sps Die Spezialisierungen aus der Leistungsbeschreibung
     * @return Liste der generierten Verwaltungsleistungen, gruppiert nach gemeinsamen Zuständigkeitsbereichen
     */
    public List<Verwaltungsleistung> buildVerwaltungsleistungen(
        LeistungsbeschreibungVL lb,
        List<OrganisationseinheitVL> oes,
        List<OnlinedienstVL> ods,
        List<FormularVL> fos,
        List<SpezialisierungVL> sps
    ) {
        log.debug(
            "Beginne Erstellung der Verwaltungsleistungen für LB '{}' mit {} OEs, {} ODs, {} FOs, {} SPs",
            lb.getId(),
            oes.size(),
            ods.size(),
            fos.size(),
            sps.size()
        );

        // Alle Listen sortieren, dadurch wird die Reihenfolge der Geschäftsobjekte in den entstehenden VLs deterministisch.
        // Sortierung bleibt für Nachfolgende Operationen erhalten.
        // Dies ist für die eigentliche Verarbeitung nicht zwingend nötig, allerdings ist es hilfreich beim Testen und Debuggen,
        // da die Ergebnisse zwischen verschiedenen Läufen oder Umgebungen konsistent bleiben.
        var sortedBusinessObjects = sortBusinessObjects(oes, ods, fos, sps);

        // Erstelle Business Object Holder für normale und exakte ARS-Suche
        var allBoHolders = Stream.concat(
            buildBoHoldersForType(sortedBusinessObjects, false),
            buildBoHoldersForType(sortedBusinessObjects, true)
        );

        // Reduziere und erstelle VLs
        var reducedBoHoldersByVlKey = reduceBoHolders(lb, allBoHolders);
        log.debug(
            "Reduzierung ergab {} eindeutige Geschäftsobjekt-Kombinationen für LB '{}'",
            reducedBoHoldersByVlKey.size(),
            lb.getId()
        );

        var verwaltungsleistungen = createVLsFromBusinessObjects(lb, reducedBoHoldersByVlKey);

        if (verwaltungsleistungen.isEmpty()) {
            log.warn(
                "Es wurden keine Verwaltungsleistungen zur LB '{}' berechnet, da zu dieser LB keine zuständigen Objekte vorliegen.",
                lb.getId()
            );
        } else {
            log.debug(
                "Erfolgreich {} Verwaltungsleistungen für LB '{}' erstellt",
                verwaltungsleistungen.size(),
                lb.getId()
            );
        }

        return verwaltungsleistungen;
    }

    /**
     * Erstellt BusinessObjectHolder für die angegebene Art der ARS-Suche.
     *
     * @param sortedBusinessObjects Die bereits nach ID sortierten Geschäftsobjekte
     * @param exactArs true für exakte ARS-Suche, false für normale hierarchische ARS-Suche
     * @return Stream von BusinessObjectHoldern für die angegebene Suchvariante
     */
    private Stream<BusinessObjectHolder> buildBoHoldersForType(
        SortedBusinessObjects sortedBusinessObjects,
        boolean exactArs
    ) {
        return buildBoHolders(
            sortedBusinessObjects.oes(),
            sortedBusinessObjects.ods(),
            sortedBusinessObjects.fos(),
            sortedBusinessObjects.sps(),
            exactArs
        );
    }

    /**
     * Sortiert alle Geschäftsobjekte nach ihrer ID, um eine deterministische Verarbeitungsreihenfolge
     * sicherzustellen. Dies ist wichtig für die Reproduzierbarkeit der Ergebnisse und erleichtert
     * das Testen und Debugging.
     *
     * @param oes Die zu sortierenden Organisationseinheiten
     * @param ods Die zu sortierenden Onlinedienste
     * @param fos Die zu sortierenden Formulare
     * @param sps Die zu sortierenden Spezialisierungen
     * @return Record mit allen nach ID sortierten Geschäftsobjekten
     */
    private SortedBusinessObjects sortBusinessObjects(
        List<OrganisationseinheitVL> oes,
        List<OnlinedienstVL> ods,
        List<FormularVL> fos,
        List<SpezialisierungVL> sps
    ) {
        // Einmalige Sortierung für deterministische Reihenfolge in allen nachfolgenden Operationen
        var sortedOes = new ArrayList<>(oes);
        var sortedOds = new ArrayList<>(ods);
        var sortedFos = new ArrayList<>(fos);
        var sortedSps = new ArrayList<>(sps);

        sortedOes.sort(comparing(Identifiable::getId));
        sortedOds.sort(comparing(Identifiable::getId));
        sortedFos.sort(comparing(Identifiable::getId));
        sortedSps.sort(comparing(Identifiable::getId));

        return new SortedBusinessObjects(sortedOes, sortedOds, sortedFos, sortedSps);
    }

    /**
     * Erstellt aus gruppierten Geschäftsobjekten die finalen Verwaltungsleistungen. Jede Verwaltungsleistung
     * repräsentiert dabei einen Cluster von ARS-Codes, in denen dieselbe Kombination von Geschäftsobjekten
     * zuständig ist. Die Zuständigkeiten werden dabei auf die jeweilige ARS-Hierarchie reduziert.
     *
     * @param lb Die Leistungsbeschreibung als Grundlage für alle Verwaltungsleistungen
     * @param businessObjectsByKey Map mit eindeutigem Schlüssel und zugehörigen gruppierten Geschäftsobjekten
     * @return Liste der erstellten Verwaltungsleistungen
     */
    private List<Verwaltungsleistung> createVLsFromBusinessObjects(
        LeistungsbeschreibungVL lb,
        Map<String, BusinessObjectHolder> businessObjectsByKey
    ) {
        log.debug(
            "Erstelle {} Verwaltungsleistungen aus gruppierten Geschäftsobjekten für LB '{}'",
            businessObjectsByKey.size(),
            lb.getId()
        );

        // Anlegen von normalisierten Leistungen aus den gruppierten Geschäftsobjekten
        var result = businessObjectsByKey
            .entrySet()
            .parallelStream()
            .map(entry -> createVL(lb, entry.getKey(), entry.getValue()))
            .filter(ObjectUtils::isNotEmpty)
            .toList();

        log.debug(
            "Parallel-Verarbeitung abgeschlossen: {} gültige Verwaltungsleistungen erstellt",
            result.size()
        );
        return result;
    }

    /**
     * Erstellt eine einzelne Verwaltungsleistung aus der Leistungsbeschreibung und den gruppierten
     * Geschäftsobjekten. Dabei werden alle Geschäftsobjekte auf die relevanten ARS-Bereiche reduziert
     * und mit den entsprechenden Suchbegriffen verknüpft.
     *
     * @param lb Die zugrunde liegende Leistungsbeschreibung
     * @param vlId Die eindeutige ID für die zu erstellende Verwaltungsleistung
     * @param holder Der Holder mit allen relevanten Geschäftsobjekten und deren ARS-Zuordnungen
     * @return Die vollständig konfigurierte Verwaltungsleistung
     */
    private Verwaltungsleistung createVL(
        LeistungsbeschreibungVL lb,
        String vlId,
        BusinessObjectHolder holder
    ) {
        var ars = new TreeSet<>(holder.ars());
        var exactArs = new TreeSet<>(holder.exactArs());

        // Die OE/OD/FO Objekte halten hier noch alle in sich originalen Zuständigkeiten aus dem XZuFi.
        // hierarchyArs werden weiter unten benötigt, um in den OE/OD/FO Objekten nur die Zuständigkeiten stehenzulassen,
        // die in den Partitions-Gebieten vorkommen.
        var hierarchyArs = buildAllHierarchyArs(holder.ars());
        hierarchyArs.addAll(exactArs);

        var vl = new Verwaltungsleistung();
        vl.setId(vlId);
        vl.setLeistungsbeschreibung(lb);
        vl.setArs(ars);
        vl.setExactArs(exactArs);
        vl.setOrganisationseinheiten(createOesInHierarchyArs(hierarchyArs, holder.oes()));
        vl.setOnlinedienste(createOdsInHierarchyArs(hierarchyArs, holder.ods()));
        vl.setFormulare(createFosInHierarchyArs(hierarchyArs, holder.fos()));
        vl.setSpezialisierungen(createSpsInHierarchyArs(hierarchyArs, holder.sps()));
        vl.setLastUpdate(lb.getLastUpdate());
        vl.setSearchTerms(
            SearchTermsExtractor.mergeSearchTermsLocalized(lb, holder.sps(), holder.ods())
        );
        return vl;
    }

    /**
     * Fasst BusinessObjectHolder mit identischen Geschäftsobjekt-Kombinationen zusammen.
     * Für jede einzigartige Kombination von Geschäftsobjekten werden die zugehörigen ARS-Codes
     * gesammelt und zu einem finalen BusinessObjectHolder zusammengefasst.
     *
     * @param lb Die Leistungsbeschreibung zur Generierung eindeutiger Schlüssel
     * @param boHolders Stream der zu gruppierenden BusinessObjectHolder
     * @return Map mit eindeutigem Kombinationsschlüssel und dem zugehörigen zusammengefassten BusinessObjectHolder
     */
    private Map<String, BusinessObjectHolder> reduceBoHolders(
        LeistungsbeschreibungVL lb,
        Stream<BusinessObjectHolder> boHolders
    ) {
        return boHolders.collect(
            // Führt die Holder zusammen
            groupingBy(
                // Gruppierung nach einem eindeutigen Merkmal der Mengen an zuständigen
                // Geschäftsobjekten
                k -> generateKey(lb, k.oes(), k.ods(), k.fos(), k.sps()),
                // Alle Holder in einer Gruppe enthalten die gleichen Geschäftsobjekte
                // Es werden nur noch die ARS zusammengefasst
                reducing(
                    new BusinessObjectHolder(
                        Set.of(),
                        Set.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                    ),
                    (partialHolder, currentHolder) -> {
                        currentHolder.ars().addAll(partialHolder.ars());
                        currentHolder.exactArs().addAll(partialHolder.exactArs());
                        return currentHolder;
                    }
                )
            )
        );
    }

    /**
     * Analysiert alle Geschäftsobjekte und erstellt für jeden ARS-Code einen BusinessObjectHolder
     * mit allen in diesem Gebiet zuständigen Geschäftsobjekten. Diese Holder werden später nach
     * identischen Geschäftsobjekt-Kombinationen gruppiert.
     *
     * @param sortedOes Sortierte Liste der Organisationseinheiten
     * @param sortedOds Sortierte Liste der Onlinedienste
     * @param sortedFos Sortierte Liste der Formulare
     * @param sortedSps Sortierte Liste der Spezialisierungen
     * @param exactArs true für exakte ARS-Zuordnung, false für hierarchische ARS-Auflösung
     * @return Stream von BusinessObjectHoldern mit den jeweiligen ARS-spezifischen Geschäftsobjekt-Kombinationen
     */
    private Stream<BusinessObjectHolder> buildBoHolders(
        List<OrganisationseinheitVL> sortedOes,
        List<OnlinedienstVL> sortedOds,
        List<FormularVL> sortedFos,
        List<SpezialisierungVL> sortedSps,
        boolean exactArs
    ) {
        var oesByArs = mapBusinessObjectsByArs(
            sortedOes,
            oe -> oe.getRelations().keySet(),
            exactArs
        );
        var odsByArs = mapBusinessObjectsByArs(sortedOds, OnlinedienstVL::getArs, exactArs);
        var fosByArs = mapBusinessObjectsByArs(sortedFos, FormularVL::getArs, exactArs);
        var spsByArs = mapBusinessObjectsByArs(sortedSps, SpezialisierungVL::getArs, exactArs);

        // Keine Verwaltungsleistungen ohne Organisationseinheiten oder Onlinedienste erzeugen.
        // Nur Leistungen, die auch in Anspruch genommen werden können, sollen ausgegeben werden.
        // Ein Formular oder eine Spezialisierung sichert dies nicht.
        Predicate<BusinessObjectHolder> hasProvider = holder ->
            ObjectUtils.isNotEmpty(holder.oes()) || ObjectUtils.isNotEmpty(holder.ods());
        return Stream.of(oesByArs, odsByArs, fosByArs, spsByArs)
            .map(Map::keySet)
            .flatMap(Collection::stream)
            .distinct()
            .map(ars ->
                createBusinessObjectHolder(ars, exactArs, oesByArs, odsByArs, fosByArs, spsByArs)
            )
            .filter(hasProvider);
    }

    /**
     * Erstellt einen BusinessObjectHolder für einen spezifischen ARS-Code mit allen
     * in diesem Gebiet zuständigen Geschäftsobjekten.
     *
     * @param ars Der ARS-Code des Zuständigkeitsgebiets
     * @param exactArs true für exakte ARS-Zuordnung, false für hierarchische ARS-Auflösung
     * @param oesByArs Map der Organisationseinheiten gruppiert nach ARS-Code
     * @param odsByArs Map der Onlinedienste gruppiert nach ARS-Code
     * @param fosByArs Map der Formulare gruppiert nach ARS-Code
     * @param spsByArs Map der Spezialisierungen gruppiert nach ARS-Code
     * @return BusinessObjectHolder mit allen für den ARS-Code relevanten Geschäftsobjekten
     */
    private BusinessObjectHolder createBusinessObjectHolder(
        String ars,
        boolean exactArs,
        Map<String, List<OrganisationseinheitVL>> oesByArs,
        Map<String, List<OnlinedienstVL>> odsByArs,
        Map<String, List<FormularVL>> fosByArs,
        Map<String, List<SpezialisierungVL>> spsByArs
    ) {
        Set<String> arsSet = exactArs ? Set.of() : Set.of(ars);
        Set<String> exactArsSet = exactArs ? Set.of(ars) : Set.of();
        return new BusinessObjectHolder(
            new HashSet<>(arsSet),
            new HashSet<>(exactArsSet),
            oesByArs.getOrDefault(ars, new ArrayList<>()),
            odsByArs.getOrDefault(ars, new ArrayList<>()),
            fosByArs.getOrDefault(ars, new ArrayList<>()),
            spsByArs.getOrDefault(ars, new ArrayList<>())
        );
    }

    /**
     * Generische Methode zur Zuordnung von Geschäftsobjekten zu ihren ARS-Codes.
     * Extrahiert aus jedem Geschäftsobjekt die zugehörigen ARS-Codes und erstellt eine
     * umgekehrte Zuordnung von ARS-Code zu allen zugehörigen Geschäftsobjekten.
     * Da die Input-Listen bereits sortiert sind, bleiben die Ergebnis-Listen automatisch sortiert.
     *
     * @param <T> Der Typ der zu verarbeitenden Geschäftsobjekte
     * @param businessObjects Die bereits nach ID sortierte Liste der zu verarbeitenden Geschäftsobjekte
     * @param arsExtractor Funktion zur Extraktion der ARS-Codes aus einem Geschäftsobjekt
     * @param exactArs true für exakte ARS-Zuordnung, false für hierarchische ARS-Auflösung
     * @return Map mit ARS-Code als Schlüssel und automatisch sortierter Liste aller zugehörigen Geschäftsobjekte als Wert
     */
    private <T extends Identifiable> Map<String, List<T>> mapBusinessObjectsByArs(
        List<T> businessObjects,
        Function<T, ? extends Collection<String>> arsExtractor,
        boolean exactArs
    ) {
        if (businessObjects.isEmpty()) {
            return Map.of();
        }

        return businessObjects
            .stream()
            .filter(bo -> ObjectUtils.isNotEmpty(arsExtractor.apply(bo)))
            .flatMap(bo -> {
                Collection<String> objArs = arsExtractor.apply(bo);
                if (objArs == null || objArs.isEmpty()) {
                    return Stream.empty();
                }

                return objArs
                    .stream()
                    .filter(Objects::nonNull)
                    .flatMap(ars -> resolveArs(ars, exactArs).stream())
                    // Werden mehrere ARS auf verschiedenen Hierarchie-Ebenen geliefert, welche keine Disjunkten Gemeinde-Mengen ergeben,
                    // so können hier ARS mehrfach auftreten und müssen herausgefiltert werden. (PV-10157)
                    .distinct()
                    .map(resolvedArs -> Map.entry(resolvedArs, bo));
            })
            .collect(
                groupingBy(
                    Map.Entry::getKey,
                    Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                )
            );
    }

    /**
     * Reduziert die Zuständigkeiten der Organisationseinheiten auf die für die Verwaltungsleistung
     * relevanten ARS-Bereiche. Da die ursprünglichen Organisationseinheiten alle Zuständigkeiten
     * der gesamten Leistungsbeschreibung enthalten können, werden hier neue Organisationseinheiten
     * mit nur den für diese spezifische Verwaltungsleistung relevanten Zuständigkeiten erstellt.
     *
     * @param hierarchyArs Alle ARS-Codes inklusive der oberen Hierarchieebenen der zu bildenden Verwaltungsleistung
     * @param oes Die ursprünglichen Organisationseinheiten mit allen Zuständigkeiten
     * @return Neue Organisationseinheiten mit auf die Verwaltungsleistung reduzierten Zuständigkeiten
     */
    private List<OrganisationseinheitVL> createOesInHierarchyArs(
        Set<String> hierarchyArs,
        List<OrganisationseinheitVL> oes
    ) {
        // Die übergebene Organisationseinheiten können alle Zuständigkeiten für die gesamte Leistung enthalten.
        // Die Methode reduziert die Zuständigkeiten auf diejenigen, welche für die Verwaltungsleistung relevant sind.
        return oes
            .stream()
            .map(oe -> {
                Optional<Map<String, List<ResponsibilityRole>>> filteredResponsibilityRole =
                    hierarchyFilterResponsibilityRole(hierarchyArs, oe);
                Optional<
                    Map<String, List<ResponsibilityCriterion>>
                > filteredResponsibilityCriterion = filteredResponsibilityCriterion(
                    hierarchyArs,
                    oe
                );
                if (
                    filteredResponsibilityRole.isPresent() ||
                    filteredResponsibilityCriterion.isPresent()
                ) {
                    // Nur kopieren, wenn sich Relations ändern
                    OrganisationseinheitVL newOe = BusinessObjectMapper.INSTANCE.shallowCopy(oe);
                    filteredResponsibilityRole.ifPresent(newOe::setRelations);
                    filteredResponsibilityCriterion.ifPresent(newOe::setCriteria);
                    return newOe;
                }
                return oe;
            })
            .toList();
    }

    private static Optional<
        Map<String, List<ResponsibilityRole>>
    > hierarchyFilterResponsibilityRole(Set<String> hierarchyArs, OrganisationseinheitVL oe) {
        Map<String, List<ResponsibilityRole>> original = oe.getRelations();
        Map<String, List<ResponsibilityRole>> filtered = hierarchyArs
            .stream()
            // Eine Relation, welche als Gemeinde direkt, oder in einer höheren Hierarchy vorkommt,
            // soll übernommen werden. Dadurch verlieren wir keine Informationen über Zuständigkeits-Hierarchien.
            // Alle anderen Relationen interessieren hier nicht und können eingespart werden
            .filter(original::containsKey)
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    original::get,
                    (first, second) -> first,
                    TreeMap::new
                )
            );
        if (filtered.equals(original)) {
            // Keine Änderung erkannt
            return Optional.empty();
        }
        return Optional.of(filtered);
    }

    private static Optional<
        Map<String, List<ResponsibilityCriterion>>
    > filteredResponsibilityCriterion(Set<String> hierarchyArs, OrganisationseinheitVL oe) {
        Map<String, List<ResponsibilityCriterion>> original = oe.getCriteria();
        Map<String, List<ResponsibilityCriterion>> filtered = hierarchyArs
            .stream()
            // Eine Relation, welche als Gemeinde direkt, oder in einer höheren Hierarchy vorkommt,
            // soll übernommen werden. Dadurch verlieren wir keine Informationen über Zuständigkeits-Hierarchien.
            // Alle anderen Relationen interessieren hier nicht und können eingespart werden
            .filter(original::containsKey)
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    original::get,
                    (first, second) -> first,
                    TreeMap::new
                )
            );
        if (filtered.equals(original)) {
            // Keine Änderung erkannt
            return Optional.empty();
        }
        return Optional.of(filtered);
    }

    /**
     * Reduziert die Zuständigkeiten der Onlinedienste auf die für die Verwaltungsleistung
     * relevanten ARS-Bereiche.
     * Siehe {@link #createObjectsInHierarchyArs(Set, List, Function, Function, BiConsumer)}
     */
    private List<OnlinedienstVL> createOdsInHierarchyArs(
        Set<String> hierarchyArs,
        List<OnlinedienstVL> ods
    ) {
        return createObjectsInHierarchyArs(
            hierarchyArs,
            ods,
            BusinessObjectMapper.INSTANCE::shallowCopy,
            OnlinedienstVL::getArs,
            OnlinedienstVL::setArs
        );
    }

    /**
     * Reduziert die Zuständigkeiten der Formulare auf die für die Verwaltungsleistung
     * relevanten ARS-Bereiche.
     * Siehe {@link #createObjectsInHierarchyArs(Set, List, Function, Function, BiConsumer)}
     */
    private List<FormularVL> createFosInHierarchyArs(
        Set<String> hierarchyArs,
        List<FormularVL> fos
    ) {
        return createObjectsInHierarchyArs(
            hierarchyArs,
            fos,
            BusinessObjectMapper.INSTANCE::shallowCopy,
            FormularVL::getArs,
            FormularVL::setArs
        );
    }

    /**
     * Reduziert die Zuständigkeiten der Spezialisierungen auf die für die Verwaltungsleistung
     * relevanten ARS-Bereiche.
     * Siehe {@link #createObjectsInHierarchyArs(Set, List, Function, Function, BiConsumer)}
     */
    private List<SpezialisierungVL> createSpsInHierarchyArs(
        Set<String> hierarchyArs,
        List<SpezialisierungVL> sps
    ) {
        return createObjectsInHierarchyArs(
            hierarchyArs,
            sps,
            BusinessObjectMapper.INSTANCE::shallowCopy,
            SpezialisierungVL::getArs,
            SpezialisierungVL::setArs
        );
    }

    /**
     * Reduziert die Zuständigkeiten der Geschäftsobjekte auf die für die Verwaltungsleistung
     * relevanten ARS-Bereiche. Da die ursprünglichen Geschäftsobjekte alle Zuständigkeiten
     * der gesamten Leistungsbeschreibung enthalten können, werden hier neue Geschäftsobjekte
     * mit nur den für diese spezifische Verwaltungsleistung relevanten Zuständigkeiten erstellt.
     *
     * @param hierarchyArs Alle ARS-Codes inklusive der oberen Hierarchieebenen der zu bildenden Verwaltungsleistung
     * @param objects Die ursprünglichen Geschäftsobjekte mit allen Zuständigkeiten
     * @param mapperFunction MapStruct Mapper-Funktion für effizientes Kopieren
     * @param arsGetter Funktion zum Extrahieren der ARS-Codes
     * @param arsSetter BiConsumer zum Setzen der gefilterten ARS-Codes
     * @return Neue Geschäftsobjekte mit auf die Verwaltungsleistung reduzierten Zuständigkeiten
     */
    private <T> List<T> createObjectsInHierarchyArs(
        Set<String> hierarchyArs,
        List<T> objects,
        UnaryOperator<T> mapperFunction,
        Function<T, Set<String>> arsGetter,
        BiConsumer<T, Set<String>> arsSetter
    ) {
        return objects
            .stream()
            .map(obj -> {
                Set<String> originalArs = arsGetter.apply(obj);
                Set<String> filteredArs = filterArsForHierarchy(hierarchyArs, originalArs);

                // Nur kopieren, wenn sich ARS-Codes ändern
                if (filteredArs.equals(originalArs)) {
                    return obj; // Keine Kopie notwendig
                }

                T newObj = mapperFunction.apply(obj);
                arsSetter.accept(newObj, filteredArs);
                return newObj;
            })
            .toList();
    }

    /**
     * Filtert ARS-Codes eines Geschäftsobjekts basierend auf der ARS-Hierarchie der Verwaltungsleistung.
     * Behält nur die ARS-Codes bei, die sowohl im Geschäftsobjekt als auch in der Verwaltungsleistungs-Hierarchie
     * vorkommen (Gemeinde direkt oder übergeordneten Ebene), um Zuständigkeitsinformationen der korrekten Hierarchieebenen zu erhalten.
     *
     * @param hierarchyArs Alle ARS-Codes der Verwaltungsleistungs-Hierarchie
     * @param objectArs Die ursprünglichen ARS-Codes des Geschäftsobjekts
     * @return Gefilterte und sortierte Menge der relevanten ARS-Codes
     */
    private Set<String> filterArsForHierarchy(Set<String> hierarchyArs, Set<String> objectArs) {
        return hierarchyArs
            .stream()
            // Eine Relation, welche als Gemeinde direkt, oder in einer höheren Hierarchy vorkommt,
            // soll übernommen werden. Dadurch verlieren wir keine Informationen über Zuständigkeits-Hierarchien.
            // Alle anderen Relationen interessieren hier nicht und können eingespart werden.
            .filter(objectArs::contains)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Ermittelt für eine gegebene Menge von ARS-Codes alle übergeordneten Hierarchieebenen.
     * Dies ist erforderlich, um beim Filtern der Zuständigkeiten keine ARS-Codes höherer
     * Hierarchieebenen zu verlieren und vollständige Zuständigkeitsinformationen zu bewahren.
     *
     * @param arss Die Basis-ARS-Codes (typischerweise Gemeinde-Ebene)
     * @return Unveränderliche Menge aller ARS-Codes inklusive aller übergeordneten Hierarchieebenen
     */
    private Set<String> buildAllHierarchyArs(Set<String> arss) {
        return arss
            .stream()
            .flatMap(ars -> new ARS(ars).getArsHierarchy().stream())
            .collect(toSet());
    }

    /**
     * Generiert einen eindeutigen SHA-256-basierten Schlüssel für eine spezifische Kombination
     * von Geschäftsobjekten und Leistungsbeschreibung. Der Schlüssel wird zur Gruppierung
     * identischer Geschäftsobjekt-Kombinationen verwendet.
     *
     * @param lb Die zugrunde liegende Leistungsbeschreibung
     * @param oes Die Organisationseinheiten der Kombination
     * @param ods Die Onlinedienste der Kombination
     * @param fos Die Formulare der Kombination
     * @param sps Die Spezialisierungen der Kombination
     * @return SHA-256-Hash als eindeutiger Identifikator für die Geschäftsobjekt-Kombination
     */
    private String generateKey(
        LeistungsbeschreibungVL lb,
        List<OrganisationseinheitVL> oes,
        List<OnlinedienstVL> ods,
        List<FormularVL> fos,
        List<SpezialisierungVL> sps
    ) {
        var concatenatedIds = Stream.of(oes, ods, fos, sps)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .map(Identifiable::getId)
            // Sortierung ist eigentlich nicht nötig, da die Input-Listen bereits sortiert sind.
            // Aber zur Sicherheit und für den Fall, dass sich das Input-Verhalten ändert,
            // wird hier nochmal sortiert, um konsistente Ergebnisse zu gewährleisten.
            .sorted()
            .collect(joining());

        return Hashing.sha256()
            .hashUnencodedChars(lb.getId() + concatenatedIds)
            .toString();
    }

    /**
     * Datencontainer für Geschäftsobjekte mit ihren zugehörigen ARS-Zuordnungen.
     *
     * @param ars Die ARS-Codes für hierarchische Suche (werden auf Gemeinde-Ebene aufgelöst)
     * @param exactArs Die ARS-Codes für exakte Suche (werden unverändert verwendet)
     * @param oes Die zugehörigen Organisationseinheiten
     * @param ods Die zugehörigen Onlinedienste
     * @param fos Die zugehörigen Formulare
     * @param sps Die zugehörigen Spezialisierungen
     */
    private record BusinessObjectHolder(
        Set<String> ars,
        Set<String> exactArs,
        List<OrganisationseinheitVL> oes,
        List<OnlinedienstVL> ods,
        List<FormularVL> fos,
        List<SpezialisierungVL> sps
    ) {}

    /**
     * Datencontainer für nach ID sortierte Geschäftsobjekte zur Sicherstellung
     * deterministischer Verarbeitungsreihenfolgen und reproduzierbarer Ergebnisse.
     *
     * @param oes Die nach ID sortierten Organisationseinheiten
     * @param ods Die nach ID sortierten Onlinedienste
     * @param fos Die nach ID sortierten Formulare
     * @param sps Die nach ID sortierten Spezialisierungen
     */
    private record SortedBusinessObjects(
        List<OrganisationseinheitVL> oes,
        List<OnlinedienstVL> ods,
        List<FormularVL> fos,
        List<SpezialisierungVL> sps
    ) {}
}
