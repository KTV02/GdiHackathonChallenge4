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
package de.dataport.suchdienst.suchen.model.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import de.dataport.suchdienst.indexer.model.AccessChannel;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceBlockSpecialization;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.AbstractServiceModule;
import de.dataport.suchdienst.suchen.service.ServiceModuleSpecializationUtil;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ServiceModuleSpecializationUtilTest {

    @Test
    void mapDetailsV6ReturnsEmptyListWhenServiceLbDetailsIsNull() {
        var modules = ServiceModuleSpecializationUtil.applySpecializations(
            null,
            List.of(),
            "010020000000",
            LocalDate.now()
        );
        List<AbstractServiceModule> result = moduleWrappersToServiceBlocks(modules);
        assertThat(result, equalTo(List.of()));
    }

    @Test
    void mapDetailsV6ReturnsEmptyListWhenNoModulesAndNoSpecializations() {
        Map<
            String,
            List<de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule>
        > serviceLbDetails = new HashMap<>();
        var modules = ServiceModuleSpecializationUtil.applySpecializations(
            serviceLbDetails,
            List.of(),
            "010020000000",
            LocalDate.now()
        );
        List<AbstractServiceModule> result = moduleWrappersToServiceBlocks(modules);
        assertThat(result, equalTo(List.of()));
    }

    @Test
    void mapDetailsV6MapsModulesWithoutSpecializations() {
        Map<
            String,
            List<de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule>
        > serviceLbDetails = new HashMap<>();
        serviceLbDetails.put(
            ServiceModuleCode.TEASER.getCode(),
            List.of(ServiceModule.builder().title("Teaser").text("text").build())
        );
        var modules = ServiceModuleSpecializationUtil.applySpecializations(
            serviceLbDetails,
            List.of(),
            "010020000000",
            LocalDate.now()
        );
        List<AbstractServiceModule> result = moduleWrappersToServiceBlocks(modules);
        assertThat(result.size(), equalTo(1));
        assertThat(result.getFirst().getTitle(), equalTo("Teaser"));
    }

    @Test
    void mapDetailsV6AppliesReplacementSpecialization() {
        Map<
            String,
            List<de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule>
        > serviceLbDetails = new HashMap<>();
        serviceLbDetails.put(
            ServiceModuleCode.TEASER.getCode(),
            List.of(ServiceModule.builder().title("Original").text("text").build())
        );

        ServiceModule specializedModule = ServiceModule.builder()
            .title("Specialized")
            .text("textSpecialized")
            .build();
        ServiceBlockSpecialization specialization = new ServiceBlockSpecialization(
            true,
            specializedModule
        );
        Map<String, List<ServiceBlockSpecialization>> detailsMap = new HashMap<>();
        detailsMap.put(ServiceModuleCode.TEASER.getCode(), List.of(specialization));
        SpezialisierungVL spezialisierungVL = new SpezialisierungVL();
        spezialisierungVL.setArs(Set.of("010020000000"));
        spezialisierungVL.setDetails(detailsMap);

        var modules = ServiceModuleSpecializationUtil.applySpecializations(
            serviceLbDetails,
            List.of(spezialisierungVL),
            "010020000000",
            LocalDate.now()
        );
        List<AbstractServiceModule> result = moduleWrappersToServiceBlocks(modules);
        assertThat(result.size(), is(1));
        assertThat(result.getFirst().getTitle(), is("Specialized"));
    }

    @Test
    void mapDetailsV6AddsNonReplacementSpecialization() {
        Map<
            String,
            List<de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule>
        > serviceLbDetails = new HashMap<>();

        ServiceModule addedModule = ServiceModule.builder()
            .title("Added")
            .text("textAdded")
            .build();
        ServiceBlockSpecialization specialization = new ServiceBlockSpecialization(
            false,
            addedModule
        );
        Map<String, List<ServiceBlockSpecialization>> detailsMap = new HashMap<>();
        detailsMap.put(ServiceModuleCode.TEASER.getCode(), List.of(specialization));
        SpezialisierungVL spezialisierungVL = new SpezialisierungVL();
        spezialisierungVL.setArs(Set.of("010020000000"));
        spezialisierungVL.setDetails(detailsMap);

        var modules = ServiceModuleSpecializationUtil.applySpecializations(
            serviceLbDetails,
            List.of(spezialisierungVL),
            "010020000000",
            LocalDate.now()
        );
        List<AbstractServiceModule> result = moduleWrappersToServiceBlocks(modules);
        assertThat(result.size(), equalTo(1));
        assertThat(result.getFirst().getTitle(), equalTo("Added"));
    }

    @Test
    void mapDetailsV6AppliesMixedSpecialization() {
        Map<
            String,
            List<de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule>
        > serviceLbDetails = new HashMap<>();
        serviceLbDetails.put(
            ServiceModuleCode.TEASER.getCode(),
            List.of(ServiceModule.builder().title("Original").build())
        );

        ServiceModule specializedModule1 = ServiceModule.builder()
            .title("Specialized 1")
            .text("bla")
            .build();
        ServiceBlockSpecialization specialization1 = new ServiceBlockSpecialization(
            true,
            specializedModule1
        );
        ServiceModule specializedModule2 = ServiceModule.builder()
            .title("Specialized 2")
            .text("blub")
            .accessChannel(AccessChannel.ONLINE)
            .build();
        ServiceBlockSpecialization specialization2 = new ServiceBlockSpecialization(
            false,
            specializedModule2
        );

        Map<String, List<ServiceBlockSpecialization>> detailsMap1 = new HashMap<>();
        detailsMap1.put(
            ServiceModuleCode.TEASER.getCode(),
            List.of(specialization1, specialization2)
        );
        SpezialisierungVL spezialisierungVL1 = new SpezialisierungVL();
        spezialisierungVL1.setArs(Set.of("010020000000"));
        spezialisierungVL1.setDetails(detailsMap1);

        //

        ServiceModule specializedModule3 = ServiceModule.builder()
            .title("Specialized 3")
            .text("bla")
            .build();

        ServiceBlockSpecialization specialization3 = new ServiceBlockSpecialization(
            false,
            specializedModule3
        );

        Map<String, List<ServiceBlockSpecialization>> detailsMap2 = new HashMap<>();
        detailsMap2.put(ServiceModuleCode.TEASER.getCode(), List.of(specialization3));
        SpezialisierungVL spezialisierungVL2 = new SpezialisierungVL();
        spezialisierungVL2.setArs(Set.of("010020000001"));
        spezialisierungVL2.setDetails(detailsMap2);

        var modules1 = ServiceModuleSpecializationUtil.applySpecializations(
            serviceLbDetails,
            List.of(spezialisierungVL1, spezialisierungVL2),
            "010020000000",
            LocalDate.now()
        );
        List<AbstractServiceModule> result1 = moduleWrappersToServiceBlocks(modules1);
        assertThat(result1.size(), is(2));
        assertThat(result1.getFirst().getTitle(), is("Specialized 2"));
        assertThat(result1.getFirst().getText(), is("blablub"));
        assertThat(result1.get(1).getTitle(), is("Specialized 1"));
        assertThat(result1.get(1).getText(), is("bla"));

        var modules2 = ServiceModuleSpecializationUtil.applySpecializations(
            serviceLbDetails,
            List.of(spezialisierungVL1, spezialisierungVL2),
            "010020000001",
            LocalDate.now()
        );
        List<AbstractServiceModule> result2 = moduleWrappersToServiceBlocks(modules2);
        assertThat(result2.size(), is(2));
        assertThat(result2.getFirst().getTitle(), is("Specialized 3"));
        assertThat(result2.getFirst().getText(), is("blablubbla"));
        assertThat(result2.get(1).getTitle(), is("Specialized 3"));
        assertThat(result2.get(1).getText(), is("blabla"));
    }

    @Test
    void mapDetailsV6AppliesMultipleSpecializations() {
        Map<
            String,
            List<de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule>
        > serviceLbDetails = new HashMap<>();
        serviceLbDetails.put(
            ServiceModuleCode.TEASER.getCode(),
            List.of(ServiceModule.builder().title("Original").build())
        );

        // SpecializationVL 1
        ServiceModule specializedModule1 = ServiceModule.builder()
            .title("Specialized 1")
            .text("bla1")
            .build();
        ServiceBlockSpecialization specialization1 = new ServiceBlockSpecialization(
            true,
            specializedModule1
        );
        ServiceModule specializedModule2 = ServiceModule.builder()
            .title("Specialized 2")
            .text("blub2")
            .build();
        ServiceBlockSpecialization specialization2 = new ServiceBlockSpecialization(
            false,
            specializedModule2
        );

        Map<String, List<ServiceBlockSpecialization>> detailsMap = new HashMap<>();
        detailsMap.put(ServiceModuleCode.TEASER.getCode(), List.of(specialization1));
        detailsMap.put(ServiceModuleCode.VOLLTEXT.getCode(), List.of(specialization2));
        SpezialisierungVL spezialisierungVL1 = new SpezialisierungVL();
        spezialisierungVL1.setId("sp1");
        spezialisierungVL1.setArs(Set.of("010020000000"));
        spezialisierungVL1.setDetails(detailsMap);

        // SpecializationVL 2
        ServiceModule specializedModule3 = ServiceModule.builder()
            .title("Specialized 3")
            .text("bla3")
            .build();
        ServiceBlockSpecialization specialization3 = new ServiceBlockSpecialization(
            false,
            specializedModule3
        );
        ServiceModule specializedModule4 = ServiceModule.builder()
            .title("Specialized 4")
            .text("blub4")
            .build();
        ServiceBlockSpecialization specialization4 = new ServiceBlockSpecialization(
            true,
            specializedModule4
        );

        Map<String, List<ServiceBlockSpecialization>> detailsMap2 = new HashMap<>();

        detailsMap2.put(ServiceModuleCode.TEASER.getCode(), List.of(specialization3));
        detailsMap2.put(ServiceModuleCode.VOLLTEXT.getCode(), List.of(specialization4));
        SpezialisierungVL spezialisierungVL2 = new SpezialisierungVL();
        spezialisierungVL2.setId("sp2");
        spezialisierungVL2.setArs(Set.of("010020000010"));
        spezialisierungVL2.setDetails(detailsMap2);

        // Assertion 1
        var modules1 = ServiceModuleSpecializationUtil.applySpecializations(
            serviceLbDetails,
            List.of(spezialisierungVL1),
            "010020000000",
            LocalDate.now()
        );
        List<AbstractServiceModule> result1 = moduleWrappersToServiceBlocks(modules1);
        assertThat(result1.size(), is(2));
        assertThat(result1.getFirst().getTitle(), is("Specialized 1"));
        assertThat(result1.getFirst().getText(), is("bla1"));

        assertThat(result1.get(1).getTitle(), is("Specialized 2"));
        assertThat(result1.get(1).getText(), is("blub2"));

        // Assertion 2
        var modules2 = ServiceModuleSpecializationUtil.applySpecializations(
            serviceLbDetails,
            List.of(spezialisierungVL1, spezialisierungVL2),
            "010020000010",
            LocalDate.now()
        );
        List<AbstractServiceModule> result2 = moduleWrappersToServiceBlocks(modules2);
        assertThat(result2.size(), is(2));
        assertThat(result2.getFirst().getTitle(), is("Specialized 3"));
        assertThat(result2.getFirst().getText(), is("bla1bla3"));

        assertThat(result2.get(1).getTitle(), is("Specialized 4"));
        assertThat(result2.get(1).getText(), is("blub4"));
    }

    @Test
    void mapDetailsV6FiltersSpecializationsByValidityPeriod() {
        Map<
            String,
            List<de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule>
        > serviceLbDetails = new HashMap<>();

        ServiceModule invalidModule = ServiceModule.builder().title("Invalid").build();
        ServiceBlockSpecialization specialization = new ServiceBlockSpecialization(
            true,
            invalidModule
        );
        invalidModule
            .getValidityPeriods()
            .add(new ValidityPeriod(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5)));
        Map<String, List<ServiceBlockSpecialization>> detailsMap = new HashMap<>();
        detailsMap.put(ServiceModuleCode.TEASER.getCode(), List.of(specialization));
        SpezialisierungVL spezialisierungVL = new SpezialisierungVL();
        spezialisierungVL.setDetails(detailsMap);

        var modules = ServiceModuleSpecializationUtil.applySpecializations(
            serviceLbDetails,
            List.of(spezialisierungVL),
            "010020000000",
            LocalDate.now()
        );
        List<AbstractServiceModule> result = moduleWrappersToServiceBlocks(modules);
        assertThat(result, equalTo(List.of()));
    }

    @Test
    void mapDetailsV6ProcessesLimitedCodesOnly() {
        Map<
            String,
            List<de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule>
        > serviceLbDetails = new HashMap<>();
        serviceLbDetails.put(
            ServiceModuleCode.TEASER.getCode(),
            List.of(ServiceModule.builder().title("Teaser").text("textTeaser").build())
        );
        serviceLbDetails.put(
            ServiceModuleCode.KOSTEN.getCode(),
            List.of(ServiceModule.builder().title("Kosten").text("textKosten").build())
        );

        var modules = ServiceModuleSpecializationUtil.applySpecializations(
            serviceLbDetails,
            List.of(),
            "010020000000",
            LocalDate.now(),
            ServiceModuleCode.TEASER.getCode()
        );
        List<AbstractServiceModule> result = moduleWrappersToServiceBlocks(modules);
        assertThat(result.size(), equalTo(1));
        assertThat(result.getFirst().getTitle(), equalTo("Teaser"));
    }

    @Test
    void mapDetailsV6FilterModulesWithNoContent() {
        Map<
            String,
            List<de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule>
        > serviceLbDetails = new HashMap<>();
        serviceLbDetails.put(
            ServiceModuleCode.TEASER.getCode(),
            List.of(ServiceModule.builder().title("Teaser").build())
        );
        serviceLbDetails.put(
            ServiceModuleCode.VORAUSSETZUNGEN.getCode(),
            List.of(
                ServiceModule.builder().title("Voraussetzungen").text("voraussetzungenText").build()
            )
        );
        var modules = ServiceModuleSpecializationUtil.applySpecializations(
            serviceLbDetails,
            List.of(),
            "010020000000",
            LocalDate.now()
        );
        List<AbstractServiceModule> result = moduleWrappersToServiceBlocks(modules);
        assertThat(result.size(), equalTo(1));
        assertThat(result.getFirst().getTitle(), equalTo("Voraussetzungen"));
    }

    @Test
    void mapDetailsV6FilterModulesWithInvalidPeriod() {
        Map<
            String,
            List<de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule>
        > serviceLbDetails = new HashMap<>();

        ValidityPeriod valid = new ValidityPeriod(LocalDate.EPOCH, LocalDate.now());
        ValidityPeriod invalid = new ValidityPeriod(LocalDate.EPOCH, LocalDate.EPOCH);

        serviceLbDetails.put(
            ServiceModuleCode.TEASER.getCode(),
            List.of(
                ServiceModule.builder()
                    .title("Teaser")
                    .text("voraussetzungenText")
                    .validityPeriods(List.of(valid))
                    .build()
            )
        );
        serviceLbDetails.put(
            ServiceModuleCode.VORAUSSETZUNGEN.getCode(),
            List.of(
                ServiceModule.builder()
                    .title("Voraussetzungen")
                    .text("voraussetzungenText")
                    .validityPeriods(List.of(invalid))
                    .build()
            )
        );
        var modules = ServiceModuleSpecializationUtil.applySpecializations(
            serviceLbDetails,
            List.of(),
            "010020000000",
            LocalDate.now()
        );
        List<AbstractServiceModule> result = moduleWrappersToServiceBlocks(modules);
        assertThat(result.size(), equalTo(1));
        assertThat(result.getFirst().getTitle(), equalTo("Teaser"));
    }

    private List<AbstractServiceModule> moduleWrappersToServiceBlocks(
        Map<
            String,
            List<de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule>
        > modules
    ) {
        return modules
            .entrySet()
            .stream()
            .flatMap(entrySet ->
                entrySet
                    .getValue()
                    .stream()
                    .map(m ->
                        ServiceDescriptionBlockV6Mapper.INSTANCE.map(
                            m,
                            entrySet.getKey(),
                            ServiceModuleCode.fromCode(entrySet.getKey()).get().getCategory()
                        )
                    )
            )
            .toList();
    }
}
