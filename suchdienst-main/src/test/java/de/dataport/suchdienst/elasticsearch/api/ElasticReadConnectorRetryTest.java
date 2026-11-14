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
package de.dataport.suchdienst.elasticsearch.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.util.ObjectBuilder;
import de.dataport.pvog.pvexternaldownloader.service.PvExternalDownloaderCodelistService;
import de.dataport.suchdienst.SuchdienstInitializer;
import de.dataport.suchdienst.config.DevSecurityConfig;
import de.dataport.suchdienst.config.OpenApiConfig;
import de.dataport.suchdienst.config.SwaggerCoreConfiguration;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity22;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans;

/**
 * Testet die zwei Retry-Ebenen des ElasticReadConnector:
 * den domänenspezifischen Retry auf verbleibende MGET-Failures und den äußeren Aspect-Retry bei
 * technischen Verbindungsfehlern.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "de.dataport.suchdienst.elastic.retry.initialInterval=PT0.1S",
        "de.dataport.suchdienst.elastic.retry.maxInterval=PT0.5S",
        "spring.profiles.active=default,test,openapi_show_internal,security_off",
    }
)
@TestPropertySource("classpath:application-test.yaml")
@Slf4j
@MockitoBeans(
    value = {
        @MockitoBean(types = SuchdienstInitializer.class),
        @MockitoBean(types = SwaggerCoreConfiguration.class),
        @MockitoBean(types = OpenApiConfig.class),
        @MockitoBean(types = PvExternalDownloaderCodelistService.class),
        @MockitoBean(types = DevSecurityConfig.class),
        @MockitoBean(types = InMemoryUserDetailsManager.class),
    }
)
class ElasticReadConnectorRetryTest {

    private static final String INITIAL_REQUEST_ID = "L1";

    @MockitoBean
    private ElasticsearchClient clientMock;

    @Autowired
    private ElasticReadConnector readConnector;

    OnlinedienstEntity odEntity1 = createOnlinedienstEntity("test001");
    OnlinedienstEntity odEntity2 = createOnlinedienstEntity("test002");
    OnlinedienstEntity odEntity3 = createOnlinedienstEntity("test003");
    OnlinedienstEntity odEntity4 = createOnlinedienstEntity("test004");

    MultiGetResponseItem<OnlinedienstEntity> item1OK = createMultigetResponseItem(odEntity1, false);
    MultiGetResponseItem<OnlinedienstEntity> item2OK = createMultigetResponseItem(odEntity2, false);
    MultiGetResponseItem<OnlinedienstEntity> item3OK = createMultigetResponseItem(odEntity3, false);
    MultiGetResponseItem<OnlinedienstEntity> item4OK = createMultigetResponseItem(odEntity4, false);

    MultiGetResponseItem<OnlinedienstEntity> item2False = createMultigetResponseItem(
        odEntity2,
        true
    );
    MultiGetResponseItem<OnlinedienstEntity> item3False = createMultigetResponseItem(
        odEntity3,
        true
    );
    MultiGetResponseItem<OnlinedienstEntity> item4False = createMultigetResponseItem(
        odEntity4,
        true
    );

    @Test
    void doWithRetryFinallyOk() throws IOException {
        List<MgetRequest> capturedRequests = new ArrayList<>();
        stubMget(
            capturedRequests,
            List.of(
                mgetResponse(item1OK, item2False, item3False, item4False),
                mgetResponse(item2OK, item3False, item4False),
                mgetResponse(item3OK, item4False),
                mgetResponse(item4OK)
            )
        );

        List<OnlinedienstEntity> onlinedienstEntities = readConnector.getByIdsWithRetry(
            IndexNames.ONLINEDIENSTE,
            Set.of(INITIAL_REQUEST_ID),
            Collections.emptyList(),
            Collections.emptyList(),
            OnlinedienstEntity.class
        );

        assertReturnedIds(onlinedienstEntities);
        assertThat(
            requestedIdsPerAttempt(capturedRequests),
            contains(
                List.of(INITIAL_REQUEST_ID),
                List.of(odEntity2.getId(), odEntity3.getId(), odEntity4.getId()),
                List.of(odEntity3.getId(), odEntity4.getId()),
                List.of(odEntity4.getId())
            )
        );
    }

    @Test
    // readConnector.getByIdsWithRetry ist die einzige
    // Methode die innerhalb asertThrows eineRuntimeException werfen kann
    @SuppressWarnings("java:S5778")
    void doWithRetryFinallyFailure() throws IOException {
        List<MgetRequest> capturedRequests = new ArrayList<>();
        stubMget(
            capturedRequests,
            List.of(
                mgetResponse(item1OK, item2False, item3False, item4False),
                mgetResponse(item2OK, item3False, item4False),
                mgetResponse(item3OK, item4False),
                mgetResponse(item4False),
                mgetResponse(item4False)
            )
        );

        var exception = assertThrows(ElasticSearchException.class, () ->
            readConnector.getByIdsWithRetry(
                IndexNames.ONLINEDIENSTE,
                Set.of(INITIAL_REQUEST_ID),
                Collections.emptyList(),
                Collections.emptyList(),
                OnlinedienstEntity.class
            )
        );
        assertThat(exception.getCode(), is(ErrorCodeEnum.FAILURE_RESPONSE));

        assertThat(
            requestedIdsPerAttempt(capturedRequests),
            contains(
                List.of(INITIAL_REQUEST_ID),
                List.of(odEntity2.getId(), odEntity3.getId(), odEntity4.getId()),
                List.of(odEntity3.getId(), odEntity4.getId()),
                List.of(odEntity4.getId()),
                List.of(odEntity4.getId())
            )
        );
    }

    @Test
    void doWithRetryThrowIO() throws IOException {
        List<MgetRequest> capturedRequests = new ArrayList<>();
        stubMget(
            capturedRequests,
            List.of(
                new IOException("test io exception handling"),
                mgetResponse(item1OK, item2OK, item3OK, item4OK)
            )
        );

        List<OnlinedienstEntity> onlinedienstEntities = readConnector.getByIdsWithRetry(
            IndexNames.ONLINEDIENSTE,
            Set.of(INITIAL_REQUEST_ID),
            Collections.emptyList(),
            Collections.emptyList(),
            OnlinedienstEntity.class
        );

        assertReturnedIds(onlinedienstEntities);
        assertThat(
            requestedIdsPerAttempt(capturedRequests),
            contains(List.of(INITIAL_REQUEST_ID), List.of(INITIAL_REQUEST_ID))
        );
    }

    @Test
    void ignoresNotFoundResultsWithoutRetryingThem() throws IOException {
        List<MgetRequest> capturedRequests = new ArrayList<>();
        stubMget(
            capturedRequests,
            List.of(
                mgetResponse(
                    item1OK,
                    item2OK,
                    createMissingMultigetResponseItem(odEntity3),
                    item4OK
                )
            )
        );

        List<OnlinedienstEntity> onlinedienstEntities = readConnector.getByIdsWithRetry(
            IndexNames.ONLINEDIENSTE,
            Set.of(INITIAL_REQUEST_ID),
            Collections.emptyList(),
            Collections.emptyList(),
            OnlinedienstEntity.class
        );

        assertThat(
            onlinedienstEntities.stream().map(OnlinedienstEntity::getId).toList(),
            containsInAnyOrder(odEntity1.getId(), odEntity2.getId(), odEntity4.getId())
        );
        assertThat(requestedIdsPerAttempt(capturedRequests), contains(List.of(INITIAL_REQUEST_ID)));
    }

    private void assertReturnedIds(List<OnlinedienstEntity> onlinedienstEntities) {
        assertThat(onlinedienstEntities.size(), is(4));
        List<String> returnedIds = onlinedienstEntities
            .stream()
            .map(OnlinedienstEntity::getId)
            .toList();
        String[] expected = new String[] {
            odEntity1.getId(),
            odEntity2.getId(),
            odEntity3.getId(),
            odEntity4.getId(),
        };
        assertThat(returnedIds, containsInAnyOrder(expected));
    }

    /**
     * Baut aus den im Test übergebenen Outcomes eine vollständige MGET-Sequenz auf und hält fest,
     * welche IDs der Connector pro Versuch tatsächlich an Elasticsearch senden würde.
     */
    private void stubMget(List<MgetRequest> capturedRequests, List<Object> outcomes)
        throws IOException {
        Queue<Object> queuedOutcomes = new ArrayDeque<>(outcomes);
        @SuppressWarnings("unchecked")
        var _ = Mockito.when(
            clientMock.mget(
                (Function<MgetRequest.Builder, ObjectBuilder<MgetRequest>>) Mockito.any(),
                Mockito.eq(OnlinedienstEntity.class)
            )
        ).thenAnswer(invocation -> {
            Function<MgetRequest.Builder, ObjectBuilder<MgetRequest>> requestFactory =
                invocation.getArgument(0);
            capturedRequests.add(requestFactory.apply(new MgetRequest.Builder()).build());

            Object outcome = queuedOutcomes.remove();
            if (outcome instanceof IOException ioException) {
                throw ioException;
            }
            return outcome;
        });
    }

    private List<List<String>> requestedIdsPerAttempt(List<MgetRequest> capturedRequests) {
        return capturedRequests
            .stream()
            .map(request -> request.ids().stream().sorted().toList())
            .toList();
    }

    @SafeVarargs
    private MgetResponse<OnlinedienstEntity> mgetResponse(
        MultiGetResponseItem<OnlinedienstEntity>... items
    ) {
        return MgetResponse.of(b -> b.docs(List.of(items)));
    }

    private MultiGetResponseItem<OnlinedienstEntity> createMissingMultigetResponseItem(
        OnlinedienstEntity odEntity
    ) {
        return MultiGetResponseItem.of(b ->
            b.result(res ->
                res.index("suchdienst_onlinedienste1").found(false).id(odEntity.getId()).version(3L)
            )
        );
    }

    private OnlinedienstEntity createOnlinedienstEntity(String name) {
        OnlinedienstEntity onlinedienstEntity = new OnlinedienstEntity22(Instant.now());
        onlinedienstEntity.setId("L100042.OD.OD_" + name);
        onlinedienstEntity.setArsFederalState(Collections.emptySet());
        onlinedienstEntity.setMetaData(null);
        return onlinedienstEntity;
    }

    private MultiGetResponseItem<OnlinedienstEntity> createMultigetResponseItem(
        OnlinedienstEntity odEntity,
        boolean isFailure
    ) {
        MultiGetResponseItem<OnlinedienstEntity> multiGetResponseItem;
        if (isFailure) {
            multiGetResponseItem = MultiGetResponseItem.of(b ->
                b.failure(res ->
                    res
                        .index("suchdienst_onlinedienste1")
                        .id(odEntity.getId())
                        .error(er ->
                            er.causedBy(cb ->
                                cb.reason(
                                    odEntity.getId() + " konnte nicht korrekt verarbeitet werden"
                                )
                            )
                        )
                )
            );
        } else {
            multiGetResponseItem = MultiGetResponseItem.of(b ->
                b.result(res ->
                    res
                        .index("suchdienst_onlinedienste1")
                        .found(true)
                        .id(odEntity.getId())
                        .source(odEntity)
                        .version(3L)
                )
            );
        }
        return multiGetResponseItem;
    }
}
