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
import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityRole;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.dataport.suchdienst.suchen.model.mapper.ResponsibilityMapper;
import de.dataport.suchdienst.suchen.model.out.organisation.OeInfoOutModel;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrganisationModelConverterService {

    private final MessageSource messageSource;

    public OrganisationModelConverterService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public List<OeInfoOutModel> buildOeInfoOutModels(
        List<OrganisationseinheitVL> oes,
        String ars,
        Locale searchLanguage
    ) {
        if (oes == null || oes.isEmpty()) {
            return List.of();
        }

        // Cache ARS-Hierarchie für wiederholte Verwendung
        final Collection<String> arsHierarchy = ARS.of(ars).getArsHierarchy();

        return oes
            .stream()
            .map(oe -> buildOeInfoOutModel(oe, arsHierarchy, searchLanguage))
            .sorted((o1, o2) -> Strings.CI.compare(o1.title(), o2.title()))
            .toList();
    }

    public OeInfoOutModel buildOeInfoOutModel(
        OrganisationseinheitVL oe,
        Collection<String> arsHierarchy,
        Locale requestedLanguage
    ) {
        var responsibilityRole = getMergedResponsibilityRole(oe, arsHierarchy, requestedLanguage);
        var responsibilityCriteria = getMostConcreteMatch(oe.getCriteria(), arsHierarchy);
        var responsibilityCriterionOut = ResponsibilityMapper.INSTANCE.mapCriterions(
            responsibilityCriteria,
            requestedLanguage
        );

        String oeName;
        if (Locale.GERMAN.equals(requestedLanguage)) {
            oeName = oe.getName();
        } else {
            oeName =
                Optional.ofNullable(oe.getOtherLanguages().get(requestedLanguage))
                    .map(OrganisationseinheitContent::getName)
                    // PV-9729: Fallback auf deutschen Namen, falls requestedLanguage nicht verfügbar
                    .orElseGet(oe::getName);
        }

        return new OeInfoOutModel(
            oe.getId(),
            oeName,
            responsibilityRole,
            responsibilityCriterionOut,
            oe.isOeffentlichAnzeigen()
        );
    }

    /**
     * Optimierte Version mit gecachter ARS-Hierarchie für bessere Performance bei Batch-Verarbeitung
     */
    public de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole getMergedResponsibilityRole(
        OrganisationseinheitVL oeVl,
        Collection<String> arsHierarchy,
        Locale requestedLanguage
    ) {
        List<ResponsibilityRole> responsibilityRoles = getResponsibilityRoles(oeVl, arsHierarchy);
        List<de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole> responsibilityRolesOut =
            ResponsibilityMapper.INSTANCE.mapRoles(responsibilityRoles, requestedLanguage);
        List<de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole> injectedRoles =
            injectRoles(responsibilityRolesOut, requestedLanguage);
        // An erster Stelle findet sich entweder
        // die kombinierte Rolle 01 (Zuständige Stelle und Ansprechpunkt) oder
        // die Default-Rolle 03 (Ansprechpunkt) oder
        // die zuerst im XZuFi definierte Rolle
        return injectedRoles.getFirst();
    }

    private List<ResponsibilityRole> getResponsibilityRoles(
        OrganisationseinheitVL oeVl,
        Collection<String> arsHierarchy
    ) {
        List<ResponsibilityRole> roles;

        if (ObjectUtils.isNotEmpty(arsHierarchy)) {
            // Setze konkreteste passende Zuständigkeitsrolle
            roles = getMostConcreteMatch(oeVl.getRelations(), arsHierarchy);
        } else {
            // Beziehe Zuständigkeitsrollen aus den persistierten Relationen, nimm irgendeine:
            roles = oeVl
                .getRelations()
                .values()
                .stream()
                .filter(l -> l != null && !l.isEmpty())
                .findFirst()
                .orElse(List.of());
        }
        return roles;
    }

    private List<de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole> injectRoles(
        List<de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole> roles,
        Locale requestedLanguage
    ) {
        if (!roles.isEmpty()) {
            // Fallback für xzufi 2.3: Wenn 02 und 03 vorhanden sind, dann 01 prependen
            // Bei XZUFi 2.3 ist es nicht möglich die Rolle Zuständige-Stelle und Ansprechpunkt mit dem Code 01 zu übertragen.
            // Dafür werden zwei Rollen 02 und 03 übertragen. Um Abwärtskompatibilität zu gewährleisten, werden die Rollen 02 und 03 zu einer Rolle 01 zusammengefasst.
            Set<String> codes = roles
                .stream()
                .map(de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole::code)
                .collect(Collectors.toSet());
            if (codes.contains("02") && codes.contains("03")) {
                var role01 = createRole(
                    messageSource,
                    "org_unit_controller_responsibility_single_point",
                    "01",
                    requestedLanguage
                );
                return Stream.concat(Stream.of(role01), roles.stream()).toList();
            } else {
                // Rollen werden unverändert zurückgegeben
                return List.copyOf(roles);
            }
        } else {
            // Default-Rolle verwenden, falls keine gefunden wurde:
            var role = createRole(
                messageSource,
                "org_unit_controller_responsibility_contact_point",
                "03",
                requestedLanguage
            );
            return List.of(role);
        }
    }

    private de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole createRole(
        MessageSource messageSource,
        String messageCode,
        String code,
        Locale language
    ) {
        // Default-Rolle verwenden, falls keine gefunden wurde:
        return new de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole(
            messageSource.getMessage(messageCode, null, language),
            code,
            XZuFiUtil.DEFAULT_URI_ZUSTAENDIGKEITS_ROLLE,
            XZuFiUtil.DEFAULT_VERSION_ZUSTAENDIGKEITS_ROLLE
        );
    }

    /**
     * Optimierte Version mit gecachter ARS-Hierarchie für bessere Performance bei Batch-Verarbeitung.
     * Ermittelt die konkreteste Übereinstimmung in der ARS-Hierarchie. Dabei wird von konkretem Gemeinde-ARS bis zum
     * Landes-ARS gesucht und die konkreteste vorhandene Liste zurückgegeben.
     *
     * @param byArs Map mit ARS-Schlüsseln und zugehörigen Listen
     * @param arsHierarchy Gecachte ARS-Hierarchie zur Vermeidung wiederholter Berechnungen
     * @param <T> Der Typ der Elemente in den Listen
     * @return Die konkreteste gefundene Liste oder eine leere Liste
     */
    private <T> List<T> getMostConcreteMatch(
        Map<String, List<T>> byArs,
        Collection<String> arsHierarchy
    ) {
        if (byArs.isEmpty()) {
            return List.of();
        }
        for (var item : arsHierarchy) {
            var values = byArs.get(item);
            if (ObjectUtils.isNotEmpty(values)) {
                return values;
            }
        }
        return List.of();
    }
}
