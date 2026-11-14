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
package de.dataport.suchdienst.suchen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity23;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstParameter;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OdRelationOutModel;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OdRelations;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceDetail;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceOutModelV2;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class OnlineDienstServiceTest {

    @Mock
    private ElasticPublicSearchConnector elasticPublicSearchConnector;

    @Mock
    private ElasticReadConnector elasticReadConnector;

    @Mock
    private MessageSource messageSource;

    @Mock
    private PvogRequestContext pvogRequestContext;

    @InjectMocks
    private OnlineDienstService onlineDienstService;

    private Set<OdRelationOutModel> expectedRelations;
    private Set<OdRelations> expectedRelationsV3;
    private List<ZustaendigkeitEntity> zustaendigkeitEntities;

    @BeforeEach
    void setUp() {
        OdRelationOutModel expectedRelation = new OdRelationOutModel();
        expectedRelation.setId("1");
        OdRelationOutModel expectedRelation2 = new OdRelationOutModel();
        expectedRelation2.setId("2");
        expectedRelations = Set.of(expectedRelation, expectedRelation2);

        OdRelations expectedRelationV3 = new OdRelations();
        expectedRelationV3.setId("1");
        OdRelations expectedRelationV3_2 = new OdRelations();
        expectedRelationV3_2.setId("2");
        expectedRelationsV3 = Set.of(expectedRelationV3, expectedRelationV3_2);

        ZustaendigkeitEntity23 zustaendigkeitEntity = new ZustaendigkeitEntity23(Instant.MIN);
        zustaendigkeitEntity.setId("1");
        ZustaendigkeitEntity23 zustaendigkeitEntity2 = new ZustaendigkeitEntity23(Instant.MAX);
        zustaendigkeitEntity2.setId("2");
        zustaendigkeitEntities = List.of(zustaendigkeitEntity, zustaendigkeitEntity2);
    }

    @Test
    @DisplayName(
        "Sollte SuchenException mit NOT_FOUND Fehlercode werfen, wenn Onlinedienst nicht gefunden wird"
    )
    void shouldThrowExceptionWhenNotFound() {
        // Setup
        String odId = "L000000.OD.000000000";
        String expectedApiMessage = "Die angegebene Resource konnte nicht gefunden werden.";

        // Mock
        when(elasticPublicSearchConnector.searchOnlinedienst(odId, null)).thenReturn(
            Optional.empty()
        );
        when(messageSource.getMessage("api.404", null, Locale.GERMAN)).thenReturn(
            expectedApiMessage
        );

        // Act & Assert
        SuchenException exception = assertThrows(SuchenException.class, () ->
            onlineDienstService.getOnlineServiceV2ById(odId, null)
        );
        assertEquals(expectedApiMessage, exception.getMessage());
        assertEquals(ErrorCodeEnum.NOT_FOUND, exception.getCode());
    }

    @Test
    @DisplayName("Sollte ein OnlineServiceOutModelV2 zurückgeben, wenn odId gefunden wird")
    void shouldReturnOnlineServiceV2() {
        // Setup
        String odId = "L0";
        OnlinedienstVL odVL = new OnlinedienstVL();
        odVL.setId(odId);

        // Mock
        when(elasticPublicSearchConnector.searchOnlinedienst(odId, null)).thenReturn(
            Optional.of(odVL)
        );
        when(
            elasticReadConnector.searchRelations(
                null,
                List.of(odVL.getId()),
                null,
                List.of("id", "lbId", "ars", "parentId")
            )
        ).thenReturn(zustaendigkeitEntities);

        when(pvogRequestContext.getLocale()).thenReturn(Locale.GERMAN);

        // Act & Assert
        OnlineServiceOutModelV2 testResult = onlineDienstService.getOnlineServiceV2ById(odId, null);
        assertEquals(odId, testResult.getId());
        assertEquals(expectedRelations, testResult.getRelations());
        verify(elasticPublicSearchConnector).searchOnlinedienst(odId, null);
        verify(elasticReadConnector).searchRelations(
            null,
            List.of(odId),
            null,
            List.of("id", "lbId", "ars", "parentId")
        );
        verifyNoMoreInteractions(elasticPublicSearchConnector);
    }

    @Test
    void shouldReturnOnlineServiceV3() {
        String odId = "L0";
        OnlinedienstVL odVL = new OnlinedienstVL();
        odVL.setId(odId);

        when(elasticPublicSearchConnector.searchOnlinedienst(odId, LocalDate.now())).thenReturn(
            Optional.of(odVL)
        );
        when(
            elasticReadConnector.searchRelations(
                null,
                List.of(odId),
                null,
                List.of("id", "lbId", "ars", "parentId")
            )
        ).thenReturn(zustaendigkeitEntities);
        when(pvogRequestContext.getLocale()).thenReturn(Locale.GERMAN);

        OnlineServiceDetail result = onlineDienstService.getOnlineServiceV3Beta1ById(odId);

        assertEquals(odId, result.getId());
        assertEquals(expectedRelationsV3, result.getRelations());
    }

    @Test
    void shouldReturnParametersForRequestedLanguageInV3() {
        String odId = "L0";
        OnlinedienstVL odVL = new OnlinedienstVL();
        odVL.setId(odId);
        OnlinedienstParameter germanParameter = new OnlinedienstParameter();
        germanParameter.setValue("Deutsch");
        odVL.setParametersEfa(List.of(germanParameter));
        OnlinedienstParameter germanCustomParameter = new OnlinedienstParameter();
        germanCustomParameter.setValue("Deutsch individuell");
        odVL.setParametersCustom(List.of(germanCustomParameter));
        OnlinedienstContent englishContent = new OnlinedienstContent();
        OnlinedienstParameter englishParameter = new OnlinedienstParameter();
        englishParameter.setValue("English");
        englishContent.setParametersEfa(List.of(englishParameter));
        OnlinedienstParameter englishCustomParameter = new OnlinedienstParameter();
        englishCustomParameter.setValue("English custom");
        englishContent.setParametersCustom(List.of(englishCustomParameter));
        odVL.setOtherLanguages(Map.of(Locale.ENGLISH, englishContent));

        when(elasticPublicSearchConnector.searchOnlinedienst(odId, LocalDate.now())).thenReturn(
            Optional.of(odVL)
        );
        when(
            elasticReadConnector.searchRelations(
                null,
                List.of(odId),
                null,
                List.of("id", "lbId", "ars", "parentId")
            )
        ).thenReturn(zustaendigkeitEntities);
        when(pvogRequestContext.getLocale()).thenReturn(Locale.ENGLISH);

        OnlineServiceDetail result = onlineDienstService.getOnlineServiceV3Beta1ById(odId);

        assertEquals("English", result.getParametersEfa().getFirst().value());
        assertEquals("English custom", result.getParametersCustom().getFirst().value());
    }

    @Test
    @DisplayName("Sollte NullpointerException werfen, wenn odId null")
    void shouldThrowNullpointerExceptionWhenOdIdNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () ->
            onlineDienstService.getOnlineServiceV2ById(null, null)
        );
    }

    @Test
    @DisplayName("Sollte IllegalArgumentExcpetion werfen, wenn odId zu kurz ist (< 2 Zeichen)")
    void shouldThrowExceptionWhenOdIdNullOrEmpty() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            onlineDienstService.getOnlineServiceV2ById("", null)
        );
        assertThrows(IllegalArgumentException.class, () ->
            onlineDienstService.getOnlineServiceV2ById("A", null)
        );
    }

    @Test
    @DisplayName(
        "Sollte ein OnlineServiceOutModelV2 zurückgeben, wenn odId gefunden wird und validDate korrekt ist"
    )
    void shouldReturnOnlineServiceV2WithValidDate() {
        // Setup
        String odId = "L000000.OD.000000000";
        LocalDate validDate = LocalDate.of(2024, 1, 1);
        OnlinedienstVL odVL = new OnlinedienstVL();
        odVL.setId(odId);

        // Mock
        when(elasticPublicSearchConnector.searchOnlinedienst(odId, validDate)).thenReturn(
            Optional.of(odVL)
        );
        when(
            elasticReadConnector.searchRelations(
                null,
                List.of(odVL.getId()),
                null,
                List.of("id", "lbId", "ars", "parentId")
            )
        ).thenReturn(zustaendigkeitEntities);

        when(pvogRequestContext.getLocale()).thenReturn(Locale.GERMAN);

        // Act & Assert
        OnlineServiceOutModelV2 testResult = onlineDienstService.getOnlineServiceV2ById(
            odId,
            validDate
        );
        assertEquals(odId, testResult.getId());
        assertEquals(expectedRelations, testResult.getRelations());
        verify(elasticPublicSearchConnector).searchOnlinedienst(odId, validDate);
        verify(elasticReadConnector).searchRelations(
            null,
            List.of(odId),
            null,
            List.of("id", "lbId", "ars", "parentId")
        );
        verifyNoMoreInteractions(elasticPublicSearchConnector);
    }

    @Test
    @DisplayName(
        "Sollte eine SuchenExcpetion werfen, wenn odId gefunden wird aber kein Ergebnis zum validDate existiert"
    )
    void shouldReturnOnlineServiceV2OnlyWithValidDate() {
        // Setup
        String odId = "L000000.OD.000000000";
        LocalDate wrongDate = LocalDate.of(2025, 1, 1);
        OnlinedienstVL odVL = new OnlinedienstVL();
        odVL.setId(odId);

        // Mock
        when(elasticPublicSearchConnector.searchOnlinedienst(odId, wrongDate)).thenReturn(
            Optional.empty()
        );

        when(messageSource.getMessage("api.404", null, Locale.GERMAN)).thenReturn("");

        // Act & Assert
        assertThrows(SuchenException.class, () ->
            onlineDienstService.getOnlineServiceV2ById(odId, wrongDate)
        );
    }
}
