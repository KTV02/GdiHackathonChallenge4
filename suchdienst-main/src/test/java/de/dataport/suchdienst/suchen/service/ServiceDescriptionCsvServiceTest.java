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
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.google.common.collect.Sets;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.indexer.util.ModulesStringExtractor;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    classes = {
        JacksonAutoConfiguration.class,
        ElasticPublicSearchConnector.class,
        ServiceDescriptionCsvService.class,
    }
)
@ContextConfiguration(classes = { SuchdienstProperties.class })
class ServiceDescriptionCsvServiceTest {

    private final String ars = "020000000000";

    @MockitoBean
    private ElasticPublicSearchConnector publicSearchConnector;

    @Test
    void testLbCsvExportWithEmptyTimestamp() {
        Mockito.when(
            publicSearchConnector.getMaxLastUpdate(Mockito.anyString(), Mockito.isNull())
        ).thenReturn(Optional.empty());
        ServiceDescriptionCsvService service = new ServiceDescriptionCsvService(
            publicSearchConnector
        );
        ByteArrayOutputStream stream = service.searchServiceDescriptionAsCSV(ars, null);
        String csv = stream.toString();
        assertThat(csv, is(emptyString()));
        Mockito.verify(publicSearchConnector).getMaxLastUpdate(ars, null);
        Mockito.verifyNoMoreInteractions(publicSearchConnector);
    }

    @Test
    void testLbCsvExportWithoutResult() {
        Instant timestamp = Instant.now().minus(2, ChronoUnit.HOURS);
        Mockito.when(
            publicSearchConnector.getMaxLastUpdate(Mockito.anyString(), Mockito.isNull())
        ).thenReturn(Optional.of(timestamp));
        Mockito.when(
            publicSearchConnector.searchServiceDescriptionsByArsWithSearchAfter(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.anyInt()
            )
        ).thenReturn(emptyHitsMetadata());

        ServiceDescriptionCsvService service = new ServiceDescriptionCsvService(
            publicSearchConnector
        );
        ByteArrayOutputStream stream = service.searchServiceDescriptionAsCSV(ars, null);
        String csv = stream.toString();
        assertThat(csv, is(emptyString()));

        Mockito.verify(publicSearchConnector).searchServiceDescriptionsByArsWithSearchAfter(
            Mockito.eq(ars),
            Mockito.isNull(),
            Mockito.isNull(),
            Mockito.eq(expectedIncludes()),
            Mockito.eq(List.of()),
            Mockito.eq(500)
        );
    }

    @Test
    void testLbCsvExportWithOds() {
        Instant timestamp = Instant.now().minus(2, ChronoUnit.HOURS);
        Verwaltungsleistung sd = initCsvExportLb(timestamp);
        initCsvExportOd(sd);

        ServiceDescriptionCsvService service = new ServiceDescriptionCsvService(
            publicSearchConnector
        );
        ByteArrayOutputStream stream = service.searchServiceDescriptionAsCSV(ars, null);
        String csv = stream.toString();
        validateCsvResult(sd, csv, timestamp);
    }

    @Test
    void testLbCsvExportLoadsNameModulesAndWritesUtf8() {
        Instant timestamp = Instant.now().minus(2, ChronoUnit.HOURS);
        Verwaltungsleistung sd = initCsvExportLb(timestamp);
        sd.getLeistungsbeschreibung().setDetails(
            Map.of(
                "02",
                List.of(ServiceModule.builder().text("Bezeichnung ÄÖÜ").build()),
                "03",
                List.of(ServiceModule.builder().text("Name éß").build())
            )
        );
        var service = new ServiceDescriptionCsvService(publicSearchConnector);
        var csv = service.searchServiceDescriptionAsCSV(ars, null).toString(StandardCharsets.UTF_8);
        validateCsvResult(sd, csv, timestamp);
        Mockito.verify(publicSearchConnector).searchServiceDescriptionsByArsWithSearchAfter(
            Mockito.eq(ars),
            Mockito.isNull(),
            Mockito.isNull(),
            Mockito.eq(expectedIncludes()),
            Mockito.eq(List.of()),
            Mockito.eq(500)
        );
    }

    @Test
    void testLbCsvExportUsesLastLbIdAsSearchAfterCursor() {
        Instant timestamp = Instant.now().minus(2, ChronoUnit.HOURS);
        List<Verwaltungsleistung> firstPage = IntStream.rangeClosed(1, 500)
            .mapToObj(i -> initCsvExportLb("1.LB.%03d".formatted(i)))
            .toList();
        Verwaltungsleistung secondPageEntry = initCsvExportLb("1.LB.501");

        Mockito.when(
            publicSearchConnector.getMaxLastUpdate(Mockito.anyString(), Mockito.isNull())
        ).thenReturn(Optional.of(timestamp));
        Mockito.when(
            publicSearchConnector.searchServiceDescriptionsByArsWithSearchAfter(
                Mockito.eq(ars),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.eq(500)
            )
        ).thenReturn(createMetadata(firstPage));
        Mockito.when(
            publicSearchConnector.searchServiceDescriptionsByArsWithSearchAfter(
                Mockito.eq(ars),
                Mockito.isNull(),
                Mockito.eq("1.LB.500"),
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.eq(500)
            )
        ).thenReturn(createMetadata(List.of(secondPageEntry)));

        ServiceDescriptionCsvService service = new ServiceDescriptionCsvService(
            publicSearchConnector
        );
        String csv = service.searchServiceDescriptionAsCSV(ars, null).toString();

        List<String> lines = csv.lines().toList();
        assertThat(lines.size(), equalTo(502));
        assertThat(lines.get(1).startsWith("1.LB.001|"), is(true));
        assertThat(lines.get(501).startsWith("1.LB.501|"), is(true));

        ArgumentCaptor<String> lastIdCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(
            publicSearchConnector,
            Mockito.times(2)
        ).searchServiceDescriptionsByArsWithSearchAfter(
            Mockito.eq(ars),
            Mockito.isNull(),
            lastIdCaptor.capture(),
            Mockito.eq(expectedIncludes()),
            Mockito.eq(List.of()),
            Mockito.eq(500)
        );
        assertThat(lastIdCaptor.getAllValues(), equalTo(Arrays.asList(null, "1.LB.500")));
    }

    @Test
    void testLbCsvExportStopsAfterEmptyPageFollowingFullPage() {
        Instant timestamp = Instant.now().minus(2, ChronoUnit.HOURS);
        List<Verwaltungsleistung> fullPage = IntStream.rangeClosed(1, 500)
            .mapToObj(i -> initCsvExportLb("1.LB.%03d".formatted(i)))
            .toList();

        Mockito.when(
            publicSearchConnector.getMaxLastUpdate(Mockito.anyString(), Mockito.isNull())
        ).thenReturn(Optional.of(timestamp));
        Mockito.when(
            publicSearchConnector.searchServiceDescriptionsByArsWithSearchAfter(
                Mockito.eq(ars),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.eq(500)
            )
        ).thenReturn(createMetadata(fullPage));
        Mockito.when(
            publicSearchConnector.searchServiceDescriptionsByArsWithSearchAfter(
                Mockito.eq(ars),
                Mockito.isNull(),
                Mockito.eq("1.LB.500"),
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.eq(500)
            )
        ).thenReturn(emptyHitsMetadata());

        ServiceDescriptionCsvService service = new ServiceDescriptionCsvService(
            publicSearchConnector
        );
        String csv = service.searchServiceDescriptionAsCSV(ars, null).toString();

        assertThat(csv.lines().count(), equalTo(501L));

        ArgumentCaptor<String> lastIdCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(
            publicSearchConnector,
            Mockito.times(2)
        ).searchServiceDescriptionsByArsWithSearchAfter(
            Mockito.eq(ars),
            Mockito.isNull(),
            lastIdCaptor.capture(),
            Mockito.eq(expectedIncludes()),
            Mockito.eq(List.of()),
            Mockito.eq(500)
        );
        assertThat(lastIdCaptor.getAllValues(), equalTo(Arrays.asList(null, "1.LB.500")));
    }

    private List<OnlinedienstEntity> initCsvExportOd(Verwaltungsleistung sd) {
        List<OnlinedienstEntity> ods = new ArrayList<>();
        OnlinedienstVL od1 = new OnlinedienstVL();
        od1.setId("1.OD.1");
        od1.setName("Onlinedienst1");
        LinkDynamic link1 = LinkDynamic.of("www.link1od1", "", "");
        LinkDynamic link2 = LinkDynamic.of("www.link2od1", "", "");
        od1.getLinks().add(link1);
        od1.getLinks().add(link2);
        sd.getOnlinedienste().add(od1);

        OnlinedienstVL od2 = new OnlinedienstVL();
        od2.setId("2.OD.2");
        od2.setName("Onlinedienst2");
        LinkDynamic link21 = LinkDynamic.of("www.link1od2", "", "");
        LinkDynamic link22 = LinkDynamic.of("www.link2od2", "", "");
        od2.getLinks().add(link21);
        od2.getLinks().add(link22);
        sd.getOnlinedienste().add(od2);

        return ods;
    }

    private void validateCsvResult(Verwaltungsleistung sd, String csv, Instant timestamp) {
        if (ObjectUtils.isNotEmpty(sd.getOnlinedienste())) {
            StringBuilder resultCsvBuilder = new StringBuilder();
            sd.getOnlinedienste().forEach(od ->
                resultCsvBuilder.append(
                    createCsvStringFromLbAndOd(sd.getLeistungsbeschreibung(), od, timestamp)
                )
            );
            String resultCsv = addHeadersToCsv(resultCsvBuilder.toString());
            assertThat(csv, equalTo(resultCsv));
        } else {
            String resultCsv = createCsvStringFromLbAndOd(
                sd.getLeistungsbeschreibung(),
                null,
                timestamp
            );
            resultCsv = addHeadersToCsv(resultCsv);
            assertThat(csv, equalTo(resultCsv));
        }
    }

    private Verwaltungsleistung initCsvExportLb(Instant timestamp) {
        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        Verwaltungsleistung sd = new Verwaltungsleistung();
        sd.setLeistungsbeschreibung(lb);
        sd.setId("1.LB.1");
        sd.getLeistungsbeschreibung().setId("1.LB.1");

        Map<String, List<AbstractServiceModule>> details = new HashMap<>();
        details.put(
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
            List.of(ServiceModule.builder().text("Leistungsname").build())
        );
        details.put(
            ServiceModuleCode.LEISTUNGSNAME.getCode(),
            List.of(ServiceModule.builder().text("Leistungsname2").build())
        );
        sd.getLeistungsbeschreibung().setDetails(details);

        sd.getLeistungsbeschreibung().setLeikakeys(Sets.newHashSet("leika1", "leika2"));
        HitsMetadata<Verwaltungsleistung> hits = createMetadata(sd);

        Mockito.when(
            publicSearchConnector.getMaxLastUpdate(Mockito.anyString(), Mockito.isNull())
        ).thenReturn(Optional.of(timestamp));
        Mockito.when(
            publicSearchConnector.searchServiceDescriptionsByArsWithSearchAfter(
                Mockito.eq(ars),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.anyInt()
            )
        ).thenReturn(hits);
        Mockito.when(
            publicSearchConnector.searchServiceDescriptionsByArsWithSearchAfter(
                Mockito.eq(ars),
                Mockito.isNull(),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.anyInt()
            )
        ).thenReturn(emptyHitsMetadata());
        return sd;
    }

    private String createCsvStringFromLbAndOd(
        LeistungsbeschreibungVL sd,
        OnlinedienstVL od,
        Instant timestamp
    ) {
        return (
            sd.getId() +
            "|" +
            String.join(";", sd.getLeikakeys()) +
            "|" +
            ModulesStringExtractor.findName(sd.getDetails()).get() +
            "|" +
            ModulesStringExtractor.findNameOtherDescription(sd.getDetails()).get() +
            "|" +
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.of(ZoneOffset.UTC.toString()))
                .format(timestamp) +
            "|" +
            (od == null ? "" : od.getId()) +
            "|" +
            (od == null ? "" : od.getName()) +
            "|" +
            (od == null
                ? ""
                : od.getLinks().stream().map(LinkDynamic::uri).collect(Collectors.joining(";"))) +
            "\n"
        );
    }

    private String addHeadersToCsv(String csv) {
        return (
            "ID-LB" +
            "|" +
            "Leikaschlüssel" +
            "|" +
            "Name" +
            "|" +
            "Name2" +
            "|" +
            "Updatedatum" +
            "|" +
            "ID-OD" +
            "|" +
            "Bezeichnung OD" +
            "|" +
            "Links" +
            "\n" +
            csv
        );
    }

    @Test
    void testLbCsvExportWithoutOds() {
        Instant timestamp = Instant.now().minus(2, ChronoUnit.HOURS);
        Verwaltungsleistung sd = initCsvExportLb(timestamp);

        ServiceDescriptionCsvService service = new ServiceDescriptionCsvService(
            publicSearchConnector
        );
        ByteArrayOutputStream stream = service.searchServiceDescriptionAsCSV(ars, null);
        String csv = stream.toString();
        validateCsvResult(sd, csv, timestamp);
    }

    private <T> HitsMetadata<T> emptyHitsMetadata() {
        return HitsMetadata.of(b ->
            b
                .hits(Collections.emptyList())
                .total(v -> v.value(0L).relation(TotalHitsRelation.Eq))
                .maxScore(0.0)
        );
    }

    private HitsMetadata<Verwaltungsleistung> createMetadata(Verwaltungsleistung obj) {
        return createMetadata(List.of(obj));
    }

    private HitsMetadata<Verwaltungsleistung> createMetadata(List<Verwaltungsleistung> objects) {
        List<Hit<Verwaltungsleistung>> hits = objects
            .stream()
            .map(obj ->
                Hit.<Verwaltungsleistung>of(h -> h.index("index").id(obj.getId()).source(obj))
            )
            .toList();
        return HitsMetadata.of(m ->
            m
                .hits(hits)
                .total(
                    co.elastic.clients.elasticsearch.core.search.TotalHits.of(t ->
                        t.value(objects.size()).relation(TotalHitsRelation.Eq)
                    )
                )
        );
    }

    private Verwaltungsleistung initCsvExportLb(String id) {
        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        Verwaltungsleistung sd = new Verwaltungsleistung();
        sd.setLeistungsbeschreibung(lb);
        sd.setId(id);
        sd.getLeistungsbeschreibung().setId(id);

        Map<String, List<AbstractServiceModule>> details = new HashMap<>();
        details.put(
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
            List.of(ServiceModule.builder().text("Leistungsname").build())
        );
        details.put(
            ServiceModuleCode.LEISTUNGSNAME.getCode(),
            List.of(ServiceModule.builder().text("Leistungsname2").build())
        );
        sd.getLeistungsbeschreibung().setDetails(details);
        sd.getLeistungsbeschreibung().setLeikakeys(
            new LinkedHashSet<>(List.of("leika1", "leika2"))
        );
        return sd;
    }

    private List<String> expectedIncludes() {
        return List.of(
            "id",
            "leistungsbeschreibung.id",
            "leistungsbeschreibung.details.02",
            "leistungsbeschreibung.details.03",
            "onlinedienste",
            "leistungsbeschreibung.leikakeys"
        );
    }
}
