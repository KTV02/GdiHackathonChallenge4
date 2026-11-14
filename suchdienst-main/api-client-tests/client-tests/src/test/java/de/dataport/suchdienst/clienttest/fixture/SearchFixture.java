/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.clienttest.fixture;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.FoCreator;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig23;
import de.dataport.pvog.testdatengenerator.generator.XzufiGenerator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.Rolle;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zeitraum;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.Amount;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.KostenFix;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.Kostenmodul;
import de.dataport.suchdienst.client.ApiClient;
import de.dataport.suchdienst.client.api.BetaLeistungenApi;
import de.dataport.suchdienst.client.api.BetaOnlinediensteApi;
import de.dataport.suchdienst.client.api.LeistungenApi;
import de.dataport.suchdienst.clienttest.support.StrictApiClientFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/** Importiert und bereinigt einen isolierten Datensatz; lesende Klassen teilen ihn über {@link SearchFixtureExtension}. */
public final class SearchFixture implements AutoCloseable {

    public static final String ARS = "095640000000";
    public static final int COUNT = 6;
    public static final String DESCRIPTION = "Definierter Volltext für den SDK-Vertragstest.";
    public static final String TEASER = "Definierter SDK-Teaser.";
    public static final String FEE_DESCRIPTION = "SDK-Gebühr für die Bearbeitung.";
    public static final String ORGANISATION_TITLE = "SDK-Testbehörde";
    public static final String ONLINE_NAME = "SDK-Onlineantrag";
    public static final String LINK_URI = "https://example.invalid/sdk-antrag";
    public static final String UNMATCHED_LEIKA = "99988888999999";

    private final CodelistConfig23 codelists = new CodelistConfig23(CodelistConfig23.PVLAGEN, "sdk-contract-1");
    private final RestClient setupClient;
    private final ApiClient client;
    private final XzufiFile file;
    private boolean importStarted;
    private boolean relationsCalculated;
    private String producer;
    private final String query = "sdkvertrag" + UUID.randomUUID().toString().replaceAll("[^a-z]", "");

    public SearchFixture(String baseUrl) {
        client = StrictApiClientFactory.create(StrictApiClientFactory.builder().baseUrl(baseUrl), baseUrl);
        setupClient = client.getRestClient();
        file = XzufiFileCreator.newInstance().name(query).lb(COUNT).oe(1).od(1).getMin();
        var validity = List.of(
            Zeitraum.builder()
                .beginn(LocalDate.of(2000, 1, 1))
                .ende(LocalDate.of(2099, 12, 31))
                .build()
        );
        for (int i = 0; i < COUNT; i++) {
            var service = file.getLeistungsbeschreibungen().get(i);
            service.setSuchbegriffe(List.of());
            service.setTextmodule(
                List.of(
                    text("02", query + " Beschreibung " + i),
                    text("03", title(i)),
                    text("06", DESCRIPTION),
                    text("24", TEASER)
                )
            );
            service.setReferenzLeiKa(leika(i));
            service.setKlassifizierung(List.of("1180200"));
            service.setGueltigkeiten(validity);
            service.setGueltigkeitGebietIDs(List.of(ARS));
            service.setKostenmodule(
                List.of(
                    Kostenmodul.builder()
                        .positionDarstellung(13)
                        .beschreibung(FEE_DESCRIPTION)
                        .beschreibungEnglisch(FEE_DESCRIPTION)
                        .kosten(
                            List.of(
                                KostenFix.builder()
                                    .codeTyp("01")
                                    .betrag(Amount.builder().value(new BigDecimal("42.50")).currencyCode("EUR").build())
                                    .build()
                            )
                        )
                        .build()
                )
            );
        }
        var organisation = file.getOrganisationseinheiten().getFirst();
        organisation.setTitel(ORGANISATION_TITLE);
        organisation.setTitelEnglisch(ORGANISATION_TITLE);
        organisation.setBeschreibung("Definierte Beschreibung der SDK-Testbehörde.");
        organisation.setGueltigkeiten(validity);
        var form = FoCreator.getInstance().getMin(query);
        form.setLink(Link.builder().uri(LINK_URI).uriEnglisch(LINK_URI).build());
        organisation.setFormulare(List.of(form));
        var onlineService = file.getOnlinedienste().getFirst();
        onlineService.setBezeichnung(ONLINE_NAME);
        onlineService.setBezeichnungEnglisch(ONLINE_NAME);
        onlineService.setBeschreibung("Definierter SDK-Onlinedienst.");
        onlineService.setBeschreibungEnglisch("Definierter SDK-Onlinedienst.");
        onlineService.setGueltigkeiten(validity);
        onlineService.setKennzeichenOeffentlichAnzeigen(true);
        onlineService.setLinks(
            List.of(
                Link.builder()
                    .titel("SDK-Antragslink")
                    .titelEnglisch("SDK-Antragslink")
                    .uri(LINK_URI)
                    .uriEnglisch(LINK_URI)
                    .build()
            )
        );
        var serviceIds = file.getLeistungsbeschreibungen().stream().map(Leistungsbeschreibung::getId).toList();
        var responsibilities = new ArrayList<>(file.getZustaendigkeitenOrganisationseinheit());
        responsibilities.addAll(file.getZustaendigkeitenOnlinedienst());
        responsibilities.forEach(responsibility -> {
            responsibility.setArss(List.of(ARS));
            responsibility.setLeistungIDs(serviceIds);
            responsibility.setGueltigkeiten(validity);
        });
        file.getZustaendigkeitenOrganisationseinheit().forEach(responsibility ->
            responsibility.setRolle(List.of(Rolle.builder().code("02").build()))
        );
    }

    public static SearchFixture forTestServer() {
        return new SearchFixture(System.getProperty("suchdienst.base-url", "http://localhost:8080"));
    }

    public ApiClient client() {
        return client;
    }

    public String query() {
        return query;
    }

    public Set<String> expectedIds() {
        return IntStream.range(0, COUNT).mapToObj(this::id).collect(Collectors.toSet());
    }

    public int indexOf(String id) {
        return IntStream.range(0, COUNT)
            .filter(index -> id(index).equals(id))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Unbekannter Treffer: " + id));
    }

    public String title(int index) {
        return query + " Leistung " + index;
    }

    public String id(int index) {
        return file.getLeistungsbeschreibungen().get(index).getFullId();
    }

    public String organisationId() {
        return file.getOrganisationseinheiten().getFirst().getFullId();
    }

    public String onlineServiceId() {
        return file.getOnlinedienste().getFirst().getFullId();
    }

    public String leika(int index) {
        // Eine Elternleistung und fünf Detailleistungen für die Hierarchie-Kaskade.
        return index == 0 ? "99099999223000" : "9909999922300" + (index + 1);
    }

    public void importData() throws IOException {
        var body = new LinkedMultiValueMap<String, Object>();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        body.add("file", new HttpEntity<>(new ClassPathResource("fixtures/pvlagen.xml"), headers));
        JsonNode result = setupClient
            .post()
            .uri("/admin/codelist/upload")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(JsonNode.class);
        assertNotNull(result);
        assertTrue(result.path("success").asBoolean(), result.toString());
        // Vor dem Push setzen, damit close() auch nach einem nur teilweise erfolgreichen Import bereinigt.
        importStarted = true;
        pushAndWait("import");
    }

    public void recalculateOnlineServiceRelations() {
        // Kein /calculate/all: ausschließlich das Redaktionssystem dieses Laufs bearbeiten.
        setupClient
            .post()
            .uri("/admin/onlineservices/relations/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("schemeAgencyId", file.getSchemeAgencyId(), "producer", producer))
            .retrieve()
            .toBodilessEntity();
        relationsCalculated = true;
    }

    /**
     * Prüft die tatsächliche Lesbarkeit in beiden Suchversionen und optional im Relationsindex.
     * Ein abgeschlossenes Queue-Element allein belegt noch keine Suchsichtbarkeit.
     */
    public void awaitVisibility(boolean includeRelations) {
        var publicApi = new LeistungenApi(client);
        var betaApi = new BetaLeistungenApi(client);
        var onlineApi = new BetaOnlinediensteApi(client);
        await("Such-APIs sehen die Fixture" + (includeRelations ? " samt Onlinedienstrelationen" : ""))
            .atMost(Duration.ofSeconds(60))
            .pollInterval(Duration.ofMillis(250))
            .untilAsserted(() -> {
                assertEquals(
                    COUNT,
                    publicApi
                        .getSearchOutModelV7(ARS, null, false, List.of(query), 0, 5, false, null, null, false, false)
                        .getServiceDescriptions()
                        .getTotalElements()
                );
                assertEquals(
                    COUNT,
                    betaApi
                        .getServiceDescriptions(ARS, List.of(query), null, false, false, 0, 5)
                        .getServiceDescriptions()
                        .getPage()
                        .getTotalElements()
                );
                if (includeRelations) {
                    var relations = onlineApi.getOnlineServiceWithRelations(ARS, List.of(leika(0)), null);
                    assertEquals(1L, relations.getTotalHits());
                    assertEquals(onlineServiceId(), relations.getOnlineservices().getFirst().getId());
                    assertEquals(
                        1,
                        onlineApi.getOrganisationsByOnlineService(onlineServiceId()).getOrganisations().size()
                    );
                }
            });
    }

    private void pushAndWait(String phase) throws IOException {
        var messages = XzufiGenerator.generateSaDMessages23(file, codelists);
        assertFalse(messages.isEmpty());
        producer = messages.getFirst().getProducer();
        Path artifacts = Path.of("target", "fixture-artifacts", query, phase);
        Files.createDirectories(artifacts);
        for (int i = 0; i < messages.size(); i++) {
            var message = messages.get(i);
            String xml = JzufiPolymorphismFixture.enrich(message.getXzufi());
            Files.writeString(artifacts.resolve("message-" + i + ".xml"), xml);
            // Historischer Routenname: Die tatsächliche XZuFi-Version steht im Content-Type-Parameter.
            JsonNode accepted = setupClient
                .post()
                .uri("/push/xzufi-22/enqueue")
                .header("X-PVOG-NACHRICHTUUID", message.getNachrichtenId())
                .header("X-PVOG-TRANSAKTIONID", message.getTransaktionsId())
                .header("X-PVOG-TRANSAKTIONPOSITION", message.getTransaktionsPosition())
                .header("X-PVOG-PRODUCER", message.getProducer())
                .header("X-PVOG-AGENCYID", message.getSchemeAgencyId())
                .header("Content-Type", "application/xml;charset=UTF-8;xzufi-version=" + message.getXzufiVersion())
                .body(xml)
                .retrieve()
                .body(JsonNode.class);
            assertNotNull(accepted);
            String importId = accepted.path("id").asString();
            assertFalse(importId.isBlank(), accepted.toString());
            await("XZuFi-Import " + importId)
                .atMost(Duration.ofSeconds(180))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    JsonNode queue = setupClient
                        .get()
                        .uri(uri -> uri.path("/push/xzufi-22/queue").queryParam("id", importId).build())
                        .retrieve()
                        .body(JsonNode.class);
                    assertNotNull(queue);
                    assertEquals("DONE", queue.path("content").path(0).path("status").asString(), queue.toString());
                });
        }
        // Ein kurzer uptodate-Zwischenstand reicht nicht; die abgeleiteten Daten müssen stabil fertig sein.
        await("Stabile Neuberechnung der Fixture")
            .atMost(Duration.ofSeconds(180))
            .during(Duration.ofSeconds(1))
            .pollInterval(Duration.ofMillis(250))
            .until(() ->
                Boolean.TRUE.equals(
                    setupClient.get().uri("/admin/verwaltungsleistung/uptodate").retrieve().body(Boolean.class)
                )
            );
    }

    @Override
    public void close() throws IOException {
        if (importStarted) {
            file.getLeistungsbeschreibungen().forEach(service -> service.setGeloescht(true));
            file.getOrganisationseinheiten().forEach(organisation -> organisation.setGeloescht(true));
            file.getOnlinedienste().forEach(onlineService -> onlineService.setGeloescht(true));
            pushAndWait("delete");
            if (relationsCalculated) {
                recalculateOnlineServiceRelations();
                await("Onlinedienstrelationen der Fixture sind bereinigt")
                    .atMost(Duration.ofSeconds(180))
                    .pollInterval(Duration.ofMillis(250))
                    .until(() ->
                        Boolean.TRUE.equals(
                            setupClient
                                .get()
                                .uri("/v1beta2/onlineservices/{odId}/organisations", onlineServiceId())
                                .exchange((request, response) -> response.getStatusCode().value() == 404)
                        )
                    );
            }
        }
    }

    private static Textmodul text(String code, String content) {
        return Textmodul.builder()
            .leikaCode(code)
            .positionDarstellung(Integer.valueOf(code))
            .inhalt(content)
            .inhaltEnglisch(content)
            .build();
    }
}
