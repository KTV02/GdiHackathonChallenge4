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

import com.google.common.collect.Sets;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceBlockSpecialization;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.indexer.util.ModulesStringExtractor;
import de.dataport.suchdienst.suchen.service.ServiceModuleSpecializationUtil;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ServiceDescriptionBuilderTest {

    private final String ars = "123456789123";

    @Test
    void testLeistung() {
        Verwaltungsleistung bgl = createBuergerLeistung();
        var sd = ServiceModuleSpecializationUtil.applySpecializations(
            bgl.getLeistungsbeschreibung().getDetails(),
            bgl.getSpezialisierungen(),
            ars,
            null
        );

        Assertions.assertEquals(
            "Deutsche Beschreibung",
            ModulesStringExtractor.findFirstTextByModuleAndChannelPriority(
                sd,
                ServiceModuleCode.VOLLTEXT
            ).orElse(null)
        );
        Assertions.assertEquals("Deutscher Name", ModulesStringExtractor.findName(sd).orElse(null));

        Assertions.assertEquals(3, sd.size());
    }

    @Test
    void testSpezialisierteLeistung() {
        Verwaltungsleistung bgl = createBuergerLeistung();
        bgl.setSpezialisierungen(createSpecializations());
        var sd = ServiceModuleSpecializationUtil.applySpecializations(
            bgl.getLeistungsbeschreibung().getDetails(),
            bgl.getSpezialisierungen(),
            ars,
            null
        );

        Assertions.assertEquals(
            "sp beschreibung deutsch",
            ModulesStringExtractor.findFirstTextByModuleAndChannelPriority(
                sd,
                ServiceModuleCode.VOLLTEXT
            ).orElse(null)
        );
        Assertions.assertEquals(
            "sp name deutsch",
            ModulesStringExtractor.findName(sd).orElse(null)
        );
    }

    private List<SpezialisierungVL> createSpecializations() {
        List<SpezialisierungVL> sps = new ArrayList<>();

        SpezialisierungVL spDeutsch = new SpezialisierungVL();
        spDeutsch.setArs(Sets.newHashSet(ars));
        spDeutsch.setDetails(createDetailsSpecialization());
        sps.add(spDeutsch);

        SpezialisierungContent spEnglish = new SpezialisierungContent();
        spEnglish.setDetails(createEnglishDetailsSpecialization());

        spDeutsch.getOtherLanguages().put(Locale.ENGLISH, spEnglish);

        return sps;
    }

    private Verwaltungsleistung createBuergerLeistung() {
        Verwaltungsleistung bgl = new Verwaltungsleistung();

        LeistungsbeschreibungVL sd = new LeistungsbeschreibungVL();
        sd.setArs(Sets.newHashSet(ars));
        sd.setAvailableLanguages(Sets.newHashSet(Locale.GERMAN));

        sd.getOtherLanguages().put(Locale.ENGLISH, createEnglishContent());

        sd.setDetails(createDetails());

        bgl.setLeistungsbeschreibung(sd);
        bgl.setArs(Sets.newHashSet(ars));

        return bgl;
    }

    private Map<String, List<AbstractServiceModule>> createDetails() {
        return new HashMap<>() {
            {
                put(
                    ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
                    new ArrayList<>(
                        List.of(
                            ServiceModule.builder().title("block1").text("Deutscher Name").build()
                        )
                    )
                );
                put(
                    ServiceModuleCode.VOLLTEXT.getCode(),
                    new ArrayList<>(
                        List.of(
                            ServiceModule.builder()
                                .title("block2")
                                .text("Deutsche Beschreibung")
                                .build()
                        )
                    )
                );
                put(
                    ServiceModuleCode.RECHTSGRUNDLAGEN.getCode(),
                    new ArrayList<>(
                        List.of(
                            ServiceModule.builder()
                                .title("block7")
                                .text("Block mit Code 07")
                                .build()
                        )
                    )
                );
            }
        };
    }

    private Map<String, List<ServiceBlockSpecialization>> createDetailsSpecialization() {
        return new HashMap<>() {
            {
                put(
                    ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
                    new ArrayList<>(
                        List.of(
                            new ServiceBlockSpecialization(
                                true,
                                ServiceModule.builder()
                                    .title("block1")
                                    .text("sp name deutsch")
                                    .build()
                            )
                        )
                    )
                );
                put(
                    ServiceModuleCode.VOLLTEXT.getCode(),
                    new ArrayList<>(
                        List.of(
                            new ServiceBlockSpecialization(
                                true,
                                ServiceModule.builder()
                                    .title("block2")
                                    .text("sp beschreibung deutsch")
                                    .build()
                            )
                        )
                    )
                );
            }
        };
    }

    private LeistungsbeschreibungContent createEnglishContent() {
        LeistungsbeschreibungContent content = new LeistungsbeschreibungContent();
        content.setDetails(createEnglishDetails());
        return content;
    }

    private Map<String, List<AbstractServiceModule>> createEnglishDetails() {
        return new HashMap<>() {
            {
                put(
                    ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
                    new ArrayList<>(
                        List.of(
                            ServiceModule.builder()
                                .title("english block1")
                                .text("english name")
                                .build()
                        )
                    )
                );
                put(
                    ServiceModuleCode.VOLLTEXT.getCode(),
                    new ArrayList<>(
                        List.of(
                            ServiceModule.builder()
                                .title("english block2")
                                .text("english description")
                                .build()
                        )
                    )
                );
                put(
                    ServiceModuleCode.RECHTSGRUNDLAGEN.getCode(),
                    new ArrayList<>(
                        List.of(
                            ServiceModule.builder()
                                .title("english block7")
                                .text("block with code 07")
                                .build()
                        )
                    )
                );
            }
        };
    }

    private Map<String, List<ServiceBlockSpecialization>> createEnglishDetailsSpecialization() {
        return new HashMap<>() {
            {
                put(
                    ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
                    new ArrayList<>(
                        List.of(
                            new ServiceBlockSpecialization(
                                true,
                                ServiceModule.builder()
                                    .title("english block1")
                                    .text("sp english name")
                                    .build()
                            )
                        )
                    )
                );
                put(
                    ServiceModuleCode.VOLLTEXT.getCode(),
                    new ArrayList<>(
                        List.of(
                            new ServiceBlockSpecialization(
                                true,
                                ServiceModule.builder()
                                    .title("english block2")
                                    .text("sp english description")
                                    .build()
                            )
                        )
                    )
                );
            }
        };
    }
}
