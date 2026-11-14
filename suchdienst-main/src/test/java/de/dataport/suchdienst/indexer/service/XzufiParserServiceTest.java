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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.importer.model.GenericOrganisationseinheit;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityCriterion;
import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityRole;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.parser.x22.XZuFiFoParserService;
import de.dataport.suchdienst.indexer.parser.x22.XZuFiLbParserService;
import de.dataport.suchdienst.indexer.parser.x22.XZuFiOdParserService;
import de.dataport.suchdienst.indexer.parser.x22.XZuFiOeParserService;
import de.dataport.suchdienst.indexer.parser.x22.XZuFiSpParserService;
import de.dataport.suchdienst.indexer.parser.x22.locale.LanguageAwareOeParser;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageOeCompleteCheckService;
import de.dataport.suchdienst.util.XzufiObjectProvider;
import de.xoev.xzufi.v2_2_0.CodeZustaendigkeitskriterium;
import de.xoev.xzufi.v2_2_0.CodeZustaendigkeitsrolle;
import de.xoev.xzufi.v2_2_0.Identifikator;
import de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert;
import de.xoev.xzufi.v2_2_0.ZustaendigkeitOrganisationseinheit;
import de.xoev.xzufi.v2_2_0.Zustaendigkeitskriterium;
import de.xoev.xzufi.v2_2_0.ZustaendigkeitskriteriumCodeErweiterbar;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;

@SuppressWarnings("java:S116")
class XzufiParserServiceTest {

    private final CodelistEntry CL_1 = createCodelistEntry("1");
    private final CodelistEntry CL_2 = createCodelistEntry("2");
    private final CodelistEntry CL_3 = createCodelistEntry("3");
    private final CodelistEntry CL_4 = createCodelistEntry("4");
    private final CodelistEntry CL_5 = createCodelistEntry("5");

    private final List<CodelistEntry> ALL_ENTRIES = List.of(CL_1, CL_2, CL_3, CL_4, CL_5);

    private XzufiParserService xzufiParserService;

    private int organisationseinheitVLInstanceCounter;

    @BeforeEach
    void setup() {
        organisationseinheitVLInstanceCounter = 0;

        xzufiParserService = new XzufiParserService(
            mockOeParserService(),
            Mockito.mock(XZuFiOdParserService.class),
            Mockito.mock(XZuFiFoParserService.class),
            Mockito.mock(XZuFiSpParserService.class),
            Mockito.mock(XZuFiLbParserService.class),
            Mockito.mock(de.dataport.suchdienst.indexer.parser.x23.XZuFiOeParserService.class),
            Mockito.mock(de.dataport.suchdienst.indexer.parser.x23.XZuFiOdParserService.class),
            Mockito.mock(de.dataport.suchdienst.indexer.parser.x23.XZuFiFoParserService.class),
            Mockito.mock(de.dataport.suchdienst.indexer.parser.x23.XZuFiSpParserService.class),
            Mockito.mock(de.dataport.suchdienst.indexer.parser.x23.XZuFiLbParserService.class),
            Mockito.mock(ArsTreeService.class),
            Mockito.mock(LeikaService.class),
            Mockito.mock(Executor.class)
        );
    }

    @Test
    void testParseGenericOrganisationseinheit() {
        List<Identifikator> gebietIDs = Stream.of("ARS_1", "ARS_2", "ARS_3")
            .map(this::createIdentifikator)
            .toList();
        List<OrganisationseinheitVL> result = mockXzufiObjectProvider(
            createOrganisationseinheitErweitert(
                createZustaendigkeitOrganisationseinheit(
                    ALL_ENTRIES.stream()
                        .map(this::createZustaendigkeitskriteriumCodeErweiterbar)
                        .map(this::createZustaendigkeitskriterium)
                        .toList(),
                    gebietIDs
                )
            ),
            createOrganisationseinheitErweitert(),
            createOrganisationseinheitErweitert(
                createZustaendigkeitOrganisationseinheit(null, null),
                createZustaendigkeitOrganisationseinheit(
                    List.of(
                        createZustaendigkeitskriterium(
                            createZustaendigkeitskriteriumCodeErweiterbar(CL_1)
                        )
                    ),
                    List.of(createIdentifikator("ARS_4"))
                )
            )
        )
            .getOes()
            .map(oe -> xzufiParserService.parseGenericOrganisationseinheit(oe, Instant.now()))
            .toList();

        assertThat(result, hasSize(3));
        Map<String, List<ResponsibilityCriterion>> criteriaByArs = result.getFirst().getCriteria();
        assertThat(criteriaByArs, Matchers.aMapWithSize(gebietIDs.size()));
        gebietIDs.forEach(gebietID -> {
            String ars = gebietID.getValue();
            assertThat(criteriaByArs.containsKey(ars), Matchers.is(true));
            List<ResponsibilityCriterion> criteria = criteriaByArs.get(ars);
            assertThat(criteria, Matchers.hasSize(ALL_ENTRIES.size()));
            for (int i = 0; i < ALL_ENTRIES.size(); i++) {
                CodelistEntry entry = ALL_ENTRIES.get(i);
                ResponsibilityCriterion criterion = criteria.get(i);
                assertThat(criterion.getName(), is(entry.getText(Locale.GERMAN)));
                assertThat(criterion.getCode(), is(entry.getCode()));
                assertThat(criterion.getUri(), is(entry.getUri()));
                assertThat(criterion.getVersion(), is(entry.getVersion()));
            }
        });
    }

    @Test
    void testParseGenericOrganisationseinheitMapsResponsibilityRoleUriAndVersion() {
        String ars = "ARS_1";
        String uri = "urn:xoev-de:fim:codeliste:test:rolle";
        String version = "20260310";

        OrganisationseinheitVL result = mockXzufiObjectProvider(
            createOrganisationseinheitErweitert(
                createZustaendigkeitOrganisationseinheit(
                    null,
                    List.of(createIdentifikator(ars)),
                    createZustaendigkeitsrolle(CL_1, uri, version)
                )
            )
        )
            .getOes()
            .map(oe -> xzufiParserService.parseGenericOrganisationseinheit(oe, Instant.now()))
            .findFirst()
            .orElseThrow();

        assertThat(result.getRelations(), aMapWithSize(1));

        ResponsibilityRole role = result.getRelations().get(ars).getFirst();
        assertThat(role.getCode(), is(CL_1.getCode()));
        assertThat(role.getUri(), is(uri));
        assertThat(role.getVersion(), is(version));
    }

    private XzufiObjectProvider mockXzufiObjectProvider(
        GenericOrganisationseinheit... organisationseinheitErweitert
    ) {
        XzufiObjectProvider result = Mockito.mock(XzufiObjectProvider.class);
        Mockito.when(result.getOes()).thenReturn(Arrays.stream(organisationseinheitErweitert));
        return result;
    }

    private GenericOrganisationseinheit createOrganisationseinheitErweitert(
        ZustaendigkeitOrganisationseinheit... zOE
    ) {
        OrganisationseinheitErweitert result = new OrganisationseinheitErweitert();
        if (zOE != null) {
            result.getZustaendigkeit().addAll(Arrays.asList(zOE));
        }
        organisationseinheitVLInstanceCounter++;
        Identifikator id = new Identifikator();
        id.setValue("ID_" + organisationseinheitVLInstanceCounter);
        id.setSchemeAgencyID("TEST");
        result.setId(id);

        return new GenericOrganisationseinheit(result);
    }

    private Zustaendigkeitskriterium createZustaendigkeitskriterium(
        ZustaendigkeitskriteriumCodeErweiterbar code
    ) {
        Zustaendigkeitskriterium result = new Zustaendigkeitskriterium();
        result.setCodeKriterium(code);
        return result;
    }

    private ZustaendigkeitOrganisationseinheit createZustaendigkeitOrganisationseinheit(
        List<Zustaendigkeitskriterium> zustaendigkeitskriterien,
        List<Identifikator> gebietIDs
    ) {
        return createZustaendigkeitOrganisationseinheit(zustaendigkeitskriterien, gebietIDs, null);
    }

    private ZustaendigkeitOrganisationseinheit createZustaendigkeitOrganisationseinheit(
        List<Zustaendigkeitskriterium> zustaendigkeitskriterien,
        List<Identifikator> gebietIDs,
        CodeZustaendigkeitsrolle rolle
    ) {
        ZustaendigkeitOrganisationseinheit result = new ZustaendigkeitOrganisationseinheit();
        List<Zustaendigkeitskriterium> kriterium = result.getKriterium();
        if (zustaendigkeitskriterien != null) {
            kriterium.addAll(zustaendigkeitskriterien);
        }
        List<Identifikator> ids = result.getGebietID();
        if (gebietIDs != null) {
            ids.addAll(gebietIDs);
        }
        result.setRolle(rolle);
        return result;
    }

    private Identifikator createIdentifikator(String value) {
        Identifikator identifikator = new Identifikator();
        identifikator.setValue(value);
        return identifikator;
    }

    private ZustaendigkeitskriteriumCodeErweiterbar createZustaendigkeitskriteriumCodeErweiterbar(
        CodelistEntry entry
    ) {
        CodeZustaendigkeitskriterium code = new CodeZustaendigkeitskriterium();
        code.setCode(entry.getCode());
        code.setListURI(entry.getUri());
        code.setListVersionID(entry.getVersion());
        ZustaendigkeitskriteriumCodeErweiterbar result =
            new ZustaendigkeitskriteriumCodeErweiterbar();
        result.setCode(code);
        return result;
    }

    private CodeZustaendigkeitsrolle createZustaendigkeitsrolle(
        CodelistEntry entry,
        String uri,
        String version
    ) {
        CodeZustaendigkeitsrolle result = new CodeZustaendigkeitsrolle();
        result.setCode(entry.getCode());
        result.setListURI(uri);
        result.setListVersionID(version);
        return result;
    }

    private XZuFiOeParserService mockOeParserService() {
        return new XZuFiOeParserService(
            Mockito.mock(LanguageAwareOeParser.class),
            Mockito.mock(LanguageOeCompleteCheckService.class),
            Mockito.mock(XZuFiFoParserService.class),
            mockCodelistService(),
            mockMessageSource()
        );
    }

    private CodelistService mockCodelistService() {
        CodelistService codelistService = Mockito.mock(CodelistService.class);
        when(codelistService.getCodelistEntries(anyString(), anyString())).thenReturn(ALL_ENTRIES);
        ALL_ENTRIES.forEach(codelistEntry ->
            when(
                codelistService.getCodelistEntry(
                    anyString(),
                    anyString(),
                    eq(codelistEntry.getCode())
                )
            ).thenReturn(codelistEntry)
        );
        return codelistService;
    }

    private MessageSource mockMessageSource() {
        MessageSource messageSource = Mockito.mock(MessageSource.class);
        Mockito.when(messageSource.getMessage(anyString(), any(), eq(Locale.GERMAN))).thenReturn(
            "deutsch"
        );
        Mockito.when(messageSource.getMessage(anyString(), any(), eq(Locale.ENGLISH))).thenReturn(
            "english"
        );
        return messageSource;
    }

    private CodelistEntry createCodelistEntry(String code) {
        var entry = new CodelistEntry();
        entry.setCode(code);
        entry.setUri("urn:xoev-de:fim:codeliste:0815");
        entry.setVersion("20211001");
        entry.setText(Locale.GERMAN, "DE_" + code);
        entry.setText(Locale.ENGLISH, "EN_" + code);
        return entry;
    }
}
