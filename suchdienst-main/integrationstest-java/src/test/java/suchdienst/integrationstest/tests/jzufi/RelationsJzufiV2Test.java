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
package suchdienst.integrationstest.tests.jzufi;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zeitraum;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.RelationsConnector;
import suchdienst.integrationstest.validator.ErrorValidator;
import suchdienst.integrationstest.validator.jzufi.OpenApiSchemaValidator;
import suchdienst.integrationstest.validator.relations.RelationV3ResultListValidator;

/** Testziel: Fachliche Filter, Konvertierung, Paging und Importaenderungen von /v2/relations/jzufi-2-3. */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class RelationsJzufiV2Test {

    private static final String PATH = "/v2/relations/jzufi-2-3";

    @ParameterizedTest
    @CsvSource({ "false,false", "false,true", "true,false", "true,true" })
    void preservesMixedResponsibilities(boolean legacyImport, boolean rich, TestInfo info) {
        var creator = XzufiFileCreator.newInstance().name(JzufiV2Fixtures.name(info)).lb(1).oe(1).od(1);
        var file = rich ? creator.getMax() : creator.getMin();
        if (legacyImport) {
            ImportUtil.pushAndWait(file);
        } else {
            ImportUtil.pushAndWait23(file);
        }
        var service = file.getLeistungsbeschreibungen().getFirst();
        var ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        var response = RelationsConnector.getRelationsLbIdsArs231V2(List.of(service.getFullId()), ars);
        response
            .then()
            .statusCode(200)
            .contentType("application/json")
            .header("X-PVOG-DEPRECATED", nullValue())
            .body("page.totalElements", equalTo(2))
            .body("page.number", equalTo(0))
            .body("page.size", equalTo(10));
        var validator = new RelationV3ResultListValidator(response);
        validator.validateCount(2);
        for (int index = 0; index < 2; index++) {
            boolean online = index == 0;
            var parentId = online
                ? file.getOnlinedienste().getFirst().getId()
                : file.getOrganisationseinheiten().getFirst().getId();
            var responsibility = online
                ? file.getZustaendigkeitenOnlinedienst().getFirst()
                : file.getZustaendigkeitenOrganisationseinheit().getFirst();
            validator
                .validateParentIdValue(index, parentId)
                .validateParentIdSchemeAgencyId(index, responsibility.getSchemeAgencyId())
                .validateParentClass(index, online ? "003" : "004", online ? "Onlinedienst" : "Organisationseinheit")
                .validateIdValue(index, service.getId() + "." + parentId + "." + ars)
                .validateLeistungidValue(index, service.getId())
                .validateLeistungidSchemeAgencyId(index, service.getSchemeAgencyId())
                .validateGebietidValue(index, ars);
            response
                .then()
                .body(
                    "content[" + index + "].zustaendigkeit[0].'@type'",
                    equalTo(online ? "ZustaendigkeitOnlinedienst" : "ZustaendigkeitOrganisationseinheit")
                );
        }
        response
            .then()
            .body(
                "content[1].zustaendigkeit[0].rolle",
                hasSize(file.getZustaendigkeitenOrganisationseinheit().getFirst().getRolle().size())
            );
        OpenApiSchemaValidator.validate(response, PATH, "GET");
    }

    @Test
    void filtersByServiceAndArsAndHonorsHierarchy(TestInfo info) {
        var file = XzufiFileCreator.newInstance().name(JzufiV2Fixtures.name(info)).lb(2).oe(1).getMin();
        var state = "010000000000";
        var city = "010010000000";
        var responsibilities = file.getZustaendigkeitenOrganisationseinheit();
        responsibilities.get(0).setArss(List.of(state));
        responsibilities.get(1).setArss(List.of(city));
        ImportUtil.pushAndWait23(file);
        var ids = file
            .getLeistungsbeschreibungen()
            .stream()
            .map(service -> service.getFullId())
            .toList();
        RelationsConnector.getRelationsLbIdsArs231V2(ids, city)
            .then()
            .statusCode(200)
            .body("page.totalElements", equalTo(2));
        RelationsConnector.getRelationsLbIdsArs231V2(ids, city, false, 0, 5)
            .then()
            .statusCode(200)
            .body("page.totalElements", equalTo(1))
            .body(
                "content[0].zustaendigkeit[0].leistungID[0].value",
                equalTo(file.getLeistungsbeschreibungen().get(1).getId())
            );
        RelationsConnector.getRelationsLbIdsArs231V2(List.of(ids.getFirst()), city, false, 0, 5)
            .then()
            .statusCode(200)
            .body("content", hasSize(0))
            .body("page.totalElements", equalTo(0));
        RelationsConnector.getRelationsLbIdsArs231V2(ids, "020000000000")
            .then()
            .statusCode(200)
            .body("content", hasSize(0));
        RelationsConnector.getRelationsLbIdsArs231V2(List.of("missing." + ids.getFirst()), city)
            .then()
            .statusCode(200)
            .body("content", hasSize(0));
    }

    @Test
    void pagesAllResponsibilitiesWithoutDuplicatesOrOmissions(TestInfo info) {
        var file = XzufiFileCreator.newInstance().name(JzufiV2Fixtures.name(info)).lb(6).oe(1).od(1).getMin();
        ImportUtil.pushAndWait23(file);
        var ids = file
            .getLeistungsbeschreibungen()
            .stream()
            .map(service -> service.getFullId())
            .toList();
        var ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        var expected = new HashSet<String>();
        for (var service : file.getLeistungsbeschreibungen()) {
            expected.add(service.getId() + "." + file.getOrganisationseinheiten().getFirst().getId() + "." + ars);
            expected.add(service.getId() + "." + file.getOnlinedienste().getFirst().getId() + "." + ars);
        }
        var actual = new ArrayList<String>();
        for (int page = 0; page < 4; page++) {
            var response = RelationsConnector.getRelationsLbIdsArs231V2(ids, ars, false, page, 5);
            response
                .then()
                .statusCode(200)
                .body("page.number", equalTo(page))
                .body("page.size", equalTo(5))
                .body("page.totalElements", equalTo(12))
                .body("page.totalPages", equalTo(3))
                .body("content", hasSize(page < 2 ? 5 : page == 2 ? 2 : 0));
            actual.addAll(
                response.jsonPath().getList("content.collect { it.zustaendigkeit[0].id.value }", String.class)
            );
        }
        assertEquals(12, actual.size());
        assertEquals(expected, new HashSet<>(actual));
        var defaults = RelationsConnector.getRelationsLbIdsArs231V2(ids, ars);
        defaults.then().statusCode(200).body("page.size", equalTo(10)).body("content", hasSize(10));
    }

    @Test
    void returnsAllValidityPeriodsWithoutImplicitTodayFilter(TestInfo info) {
        var file = XzufiFileCreator.newInstance().name(JzufiV2Fixtures.name(info)).lb(1).oe(3).getMin();
        var responsibilities = file.getZustaendigkeitenOrganisationseinheit();
        for (int index = 0; index < 3; index++) {
            var start = LocalDate.now()
                .plusYears((index - 1) * 10L)
                .minusMonths(1);
            responsibilities
                .get(index)
                .setGueltigkeiten(List.of(Zeitraum.builder().beginn(start).ende(start.plusMonths(2)).build()));
        }
        ImportUtil.pushAndWait23(file);
        var response = RelationsConnector.getRelationsLbIdsArs231V2(
            List.of(file.getLeistungsbeschreibungen().getFirst().getFullId()),
            responsibilities.getFirst().getArss().getFirst()
        );
        response.then().statusCode(200).body("page.totalElements", equalTo(3)).body("content", hasSize(3));
        var expectedParents = file
            .getOrganisationseinheiten()
            .stream()
            .map(organisation -> organisation.getId())
            .toList();
        assertEquals(
            new HashSet<>(expectedParents),
            new HashSet<>(response.jsonPath().getList("content.uebergeordnetesObjektID.value", String.class))
        );
    }

    @Test
    void reflectsImportUpdatesAndDeletion(TestInfo info) {
        var file = XzufiFileCreator.newInstance().name(JzufiV2Fixtures.name(info)).lb(1).oe(1).od(1).getMin();
        ImportUtil.pushAndWait23(file);
        var service = file.getLeistungsbeschreibungen().getFirst();
        var responsibility = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        var ars = responsibility.getArss().getFirst();
        var ids = List.of(service.getFullId());
        RelationsConnector.getRelationsLbIdsArs231V2(ids, ars)
            .then()
            .statusCode(200)
            .body("page.totalElements", equalTo(2));
        responsibility.setFitConnectId("fachtest-fitconnect");
        var update = new XzufiFile();
        update.getZustaendigkeitenOrganisationseinheit().add(responsibility);
        ImportUtil.pushAndWait23(update);
        var changed = RelationsConnector.getRelationsLbIdsArs231V2(ids, ars);
        new RelationV3ResultListValidator(changed)
            .validateCount(2)
            .validateKommunikationssystemDestinationSignatureValue(1, "fachtest-fitconnect");
        responsibility.setGeloescht(true);
        var deletion = new XzufiFile();
        deletion.getZustaendigkeitenOrganisationseinheit().add(responsibility);
        ImportUtil.pushAndWait23(deletion);
        var remaining = RelationsConnector.getRelationsLbIdsArs231V2(ids, ars);
        new RelationV3ResultListValidator(remaining)
            .validateCount(1)
            .validateParentIdValue(0, file.getOnlinedienste().getFirst().getId());
        var onlineResponsibility = file.getZustaendigkeitenOnlinedienst().getFirst();
        onlineResponsibility.setGeloescht(true);
        var onlineDeletion = new XzufiFile();
        onlineDeletion.getZustaendigkeitenOnlinedienst().add(onlineResponsibility);
        ImportUtil.pushAndWait23(onlineDeletion);
        RelationsConnector.getRelationsLbIdsArs231V2(ids, ars)
            .then()
            .statusCode(200)
            .body("content", hasSize(0))
            .body("page.totalElements", equalTo(0));
    }

    @ParameterizedTest
    @CsvSource({ "1,0", "0,1", "2,0", "0,2" })
    void returnsOnlyRequestedResponsibilityKind(int organisations, int onlineServices, TestInfo info) {
        var file = XzufiFileCreator.newInstance()
            .name(JzufiV2Fixtures.name(info))
            .lb(1)
            .oe(organisations)
            .od(onlineServices)
            .getMin();
        ImportUtil.pushAndWait23(file);
        var responsibilities =
            organisations > 0 ? file.getZustaendigkeitenOrganisationseinheit() : file.getZustaendigkeitenOnlinedienst();
        var response = RelationsConnector.getRelationsLbIdsArs231V2(
            List.of(file.getLeistungsbeschreibungen().getFirst().getFullId()),
            responsibilities.getFirst().getArss().getFirst()
        );
        response
            .then()
            .statusCode(200)
            .body("content", hasSize(organisations + onlineServices))
            .body(
                "content.collect { it.zustaendigkeit[0].'@type' }",
                org.hamcrest.Matchers.everyItem(
                    equalTo(organisations > 0 ? "ZustaendigkeitOrganisationseinheit" : "ZustaendigkeitOnlinedienst")
                )
            );
        var expectedIds =
            organisations > 0
                ? file
                      .getOrganisationseinheiten()
                      .stream()
                      .map(organisation -> organisation.getId())
                      .toList()
                : file
                      .getOnlinedienste()
                      .stream()
                      .map(service -> service.getId())
                      .toList();
        assertEquals(
            new HashSet<>(expectedIds),
            new HashSet<>(response.jsonPath().getList("content.uebergeordnetesObjektID.value", String.class))
        );
        OpenApiSchemaValidator.validate(response, PATH, "GET");
    }

    @ParameterizedTest
    @MethodSource("invalidParameters")
    void rejectsInvalidParameters(List<String> ids, String ars, Integer page, Integer size) {
        new ErrorValidator(
            RelationsConnector.getRelationsLbIdsArs231V2(ids, ars, null, page, size)
        ).validateBadRequest();
    }

    private static Stream<Arguments> invalidParameters() {
        var id = List.of("missing");
        return Stream.of(
            Arguments.of(null, "01", null, null),
            Arguments.of(id, null, null, null),
            Arguments.of(List.of(), "01", null, null),
            Arguments.of(List.of(""), "01", null, null),
            Arguments.of(List.of("x".repeat(256)), "01", null, null),
            Arguments.of(
                IntStream.range(0, 51)
                    .mapToObj(index -> "missing" + index)
                    .toList(),
                "01",
                null,
                null
            ),
            Arguments.of(id, "a", null, null),
            Arguments.of(id, "0".repeat(13), null, null),
            Arguments.of(id, "01", -1, 5),
            Arguments.of(id, "01", 0, 4),
            Arguments.of(id, "01", 0, 10000)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 50 })
    void acceptsServiceCountBoundaries(int count) {
        var ids = IntStream.range(0, count)
            .mapToObj(index -> "missing-" + index)
            .toList();
        RelationsConnector.getRelationsLbIdsArs231V2(ids, "01")
            .then()
            .statusCode(200)
            .body("page.totalElements", equalTo(0));
    }

    @ParameterizedTest
    @CsvSource({ "0,5", "0,9999", "1,5" })
    void acceptsPageSizeBoundaries(int page, int size) {
        RelationsConnector.getRelationsLbIdsArs231V2(List.of("x".repeat(255)), "000000000000", false, page, size)
            .then()
            .statusCode(200)
            .body("page.number", equalTo(page))
            .body("page.size", equalTo(size))
            .body("page.totalElements", equalTo(0));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "0", "000000000000" })
    void acceptsArsWithoutInventedMinimumLength(String ars) {
        RelationsConnector.getRelationsLbIdsArs231V2(List.of("missing"), ars)
            .then()
            .statusCode(200)
            .body("page.totalElements", equalTo(0));
    }
}
