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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.indexer.service.LeikaService;
import de.dataport.suchdienst.suchen.dynamicparams.DynamicParamsFetcherService;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelV5;
import de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlock;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans;

@SpringBootTest(
    classes = {
        ServiceDescriptionService.class,
        OrganisationModelConverterService.class,
        JacksonAutoConfiguration.class,
        ElasticPublicSearchConnector.class,
        ElasticReadConnector.class,
        VerwaltungsleistungMappingService.class,
    }
)
@MockitoBeans(
    @MockitoBean(
        types = {
            ElasticPublicSearchConnector.class,
            ElasticReadConnector.class,
            MessageSource.class,
            LeikaService.class,
            DynamicParamsFetcherService.class,
            PvogRequestContext.class,
        }
    )
)
class ExperienceOutModelTest {

    @Autowired
    private ServiceDescriptionService service;

    private final String ars = "020000000000";

    @Test
    void emptyOutModelWithPlaceholdersDETest() {
        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        Verwaltungsleistung sd = new Verwaltungsleistung();
        sd.setLeistungsbeschreibung(lb);
        sd.setId("1");
        lb.setDetails(new HashMap<>()); // empty

        var outModelDe = service.mapServiceDescriptionToOutModelBase(
            sd,
            ars,
            Locale.GERMAN,
            LocalDate.now(),
            true,
            SdOutModelV5.class
        );
        var codesDe = outModelDe.getDetails().stream().map(SeDeBlock::getCode).toList();

        assertNotNull(outModelDe);
        assertThat(codesDe, hasSize(8));
        assertThat(codesDe, contains("23", "09", "08", "98", "97", "07", "18", "22"));
        assertThat(
            outModelDe.getDetails().stream().map(SeDeBlock::getTitle).toList(),
            contains(
                "Ansprechpunkt",
                "Voraussetzungen",
                "Erforderliche Unterlagen",
                "Kosten & Gebühren",
                "Fristen",
                "Rechtsgrundlage(n)",
                "Urheber",
                "Zuständige Stelle"
            )
        );
        assertThat(
            outModelDe.getDetails().stream().map(SeDeBlock::getText).toList(),
            everyItem(Is.is("nicht angegeben"))
        );
    }

    @Test
    void emptyOutModelWithPlaceholdersENTest() {
        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        Verwaltungsleistung sd = new Verwaltungsleistung();
        sd.setLeistungsbeschreibung(lb);
        sd.setId("1");
        lb.setAvailableLanguages(Set.of(Locale.ENGLISH));
        lb.setDetails(new HashMap<>()); // empty

        var outModelEn = service.mapServiceDescriptionToOutModelBase(
            sd,
            ars,
            Locale.ENGLISH,
            LocalDate.now(),
            true,
            SdOutModelV5.class
        );
        var codesEn = outModelEn.getDetails().stream().map(SeDeBlock::getCode).toList();

        assertNotNull(outModelEn);
        assertThat(codesEn, hasSize(8));
        assertThat(codesEn, contains("23", "09", "08", "98", "97", "07", "18", "22"));
        assertThat(
            outModelEn.getDetails().stream().map(SeDeBlock::getText).toList(),
            everyItem(Is.is("not specified"))
        );
    }

    @Test
    void mappingToOutModelTest() {
        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        Verwaltungsleistung sd = new Verwaltungsleistung();
        sd.setLeistungsbeschreibung(lb);
        sd.setId("1");
        lb.setDetails(
            new HashMap<>() {
                {
                    put(
                        ServiceModuleCode.VOLLTEXT.getCode(),
                        new ArrayList<>(
                            List.of(
                                ServiceModule.builder().title("Volltext").text("Volltext").build()
                            )
                        )
                    );
                    put(
                        ServiceModuleCode.TEASER.getCode(),
                        new ArrayList<>(
                            List.of(ServiceModule.builder().title("Teaser").text("Teaser").build())
                        )
                    );
                }
            }
        );

        var outModel = service.mapServiceDescriptionToOutModelBase(
            sd,
            ars,
            Locale.GERMAN,
            LocalDate.now(),
            true,
            SdOutModelV5.class
        );
        var codes = outModel.getDetails().stream().map(SeDeBlock::getCode).toList();

        assertNotNull(outModel);
        assertThat(codes, hasSize(10));
        assertThat(codes, contains("24", "23", "06", "09", "08", "98", "97", "07", "18", "22"));
        assertThat(
            outModel.getDetails().stream().map(SeDeBlock::getTitle).toList(),
            allOf(
                hasItem("Kurzinfo"),
                hasItem("Beschreibung"),
                not(hasItem("Teaser")),
                not(hasItem("Volltext"))
            )
        );
    }
}
