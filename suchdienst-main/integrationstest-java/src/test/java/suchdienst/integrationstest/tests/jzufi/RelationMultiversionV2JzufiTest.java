/*-
 * ---license-start
 * Suchdienst Integrationstests
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package suchdienst.integrationstest.tests.jzufi;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.pvog.testdatengenerator.creator.CreatorConstants;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.LbCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.OeCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOrganisationseinheitCreator;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig22;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig23;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22ZustaendigkeitsGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi23.Xzufi23ZustaendigkeitsGenerator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zeitraum;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.RelationsConnector;
import suchdienst.integrationstest.validator.jzufi.OpenApiSchemaValidator;
import suchdienst.integrationstest.validator.relations.RelationV3ResultListValidator;

/**
 * Mit den Tests soll die Funktionalität der Endpunkte /v1/relations/jzufi-2-2 und /v1/relations/jzufi-2-3 sichergestellt werden.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class RelationMultiversionV2JzufiTest {

    CodelistConfig22 codelistConfig22 = new CodelistConfig22();
    CodelistConfig23 codelistConfig23 = new CodelistConfig23();

    @Test
    void compareZustaendigkeitAndZustaendigkeitTransferObjekt(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(file);

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Zustaendigkeit zs = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        String ars = zs.getArss().getFirst();

        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();

        var expected23 = new Xzufi23ZustaendigkeitsGenerator(codelistConfig23)
            .generateZustaendigkeitOETranferObjekt(file.getZustaendigkeitenOrganisationseinheit().getFirst())
            .getFirst()
            .getZustaendigkeitTransferObjekt()
            .getZustaendigkeit()
            .getFirst();

        var expected22 = new Xzufi22ZustaendigkeitsGenerator(codelistConfig22)
            .generateZustaendigkeitOETranferObjekt(file.getZustaendigkeitenOrganisationseinheit().getFirst())
            .getFirst()
            .getZustaendigkeitTransferObjekt()
            .getZustaendigkeit()
            .getFirst();

        // Daten Einspielen
        ImportUtil.pushAndWait23(file);

        Response response23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);
        Response response22 = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);

        // Validierung
        // Mappen und Vergleichen der Zustaendigkeiten und ZustaendigkeitTransferObjekt-Attribute
        JsonPath response23JsonPath = response23
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json")
            .extract()
            .jsonPath();
        de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit actual23 = response23JsonPath.getObject(
            "content[0].zustaendigkeit[0]",
            de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit.class
        );
        assertEquals(expected23, actual23);

        de.xoev.xzufi.v2_3_1.Identifikator actual23UebergeordnetesObjektID = response23JsonPath.getObject(
            "content[0].uebergeordnetesObjektID",
            de.xoev.xzufi.v2_3_1.Identifikator.class
        );
        assertEquals(oe.getId(), actual23UebergeordnetesObjektID.getValue());
        assertEquals(oe.getSchemeAgencyId(), actual23UebergeordnetesObjektID.getSchemeAgencyID());

        de.xoev.xzufi.v2_3_1.CodeTransferZustaendigkeitTyp actual23UebergeordnetesObjektKlasse =
            response23JsonPath.getObject(
                "content[0].uebergeordnetesObjektKlasse",
                de.xoev.xzufi.v2_3_1.CodeTransferZustaendigkeitTyp.class
            );
        assertEquals("Organisationseinheit", actual23UebergeordnetesObjektKlasse.getName());
        assertEquals("004", actual23UebergeordnetesObjektKlasse.getCode());
        assertEquals(
            "urn:xoev-de:fim:codeliste:xzufi.transferzustaendigkeittyp",
            actual23UebergeordnetesObjektKlasse.getListURI()
        );
        assertEquals("1", actual23UebergeordnetesObjektKlasse.getListVersionID());

        // Vorsicht hier soll nicht der Konverter geprüft werden, für den Vergleich wird sich auf Id beschränkt.
        JsonPath response22JsonPath = response22
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json")
            .extract()
            .jsonPath();
        de.xoev.xzufi.v2_2_0.ZustaendigkeitOrganisationseinheit actual22 = response22JsonPath.getObject(
            "content[0].zustaendigkeit[0]",
            de.xoev.xzufi.v2_2_0.ZustaendigkeitOrganisationseinheit.class
        );
        assertEquals(expected22.getId(), actual22.getId());

        de.xoev.xzufi.v2_2_0.Identifikator actual22UebergeordnetesObjektID = response22JsonPath.getObject(
            "content[0].uebergeordnetesObjektID",
            de.xoev.xzufi.v2_2_0.Identifikator.class
        );
        assertEquals(oe.getId(), actual22UebergeordnetesObjektID.getValue());
        assertEquals(oe.getSchemeAgencyId(), actual22UebergeordnetesObjektID.getSchemeAgencyID());
    }

    @Test
    void checkJsonSchemaTest(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMax();
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();

        // Daten Einspielen
        ImportUtil.pushAndWait23(file);
        Response response22 = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);
        Response response23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);

        // Validierung
        String endpoint22 = "/v1/relations/jzufi-2-2";
        String endpoint23 = "/v1/relations/jzufi-2-3";
        OpenApiSchemaValidator.validate(response22, endpoint22, "GET");
        OpenApiSchemaValidator.validate(response23, endpoint23, "GET");
    }

    @Test
    void checkSpecificJsonPaths(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Zustaendigkeit zs = file.getZustaendigkeitenOnlinedienst().getFirst();
        zs.setGueltigkeiten(
            List.of(
                Zeitraum.builder()
                    // Setze nur zwei Properties in der Gueltigkeit, um das Verhalten der Ausgabe von Null-Werte zu testen
                    .beginn(LocalDate.now().minusDays(1))
                    .ende(LocalDate.now())
                    .build()
            )
        );
        String ars = zs.getArss().getFirst();

        ImportUtil.pushAndWait23(file);

        // Validierung
        Response response23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);
        Response response22 = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);
        response23
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json")
            // Stellt sicher, dass Werte, die im PV_Schema nicht definiert (null) sind, nicht im JSON ausgeliefert werden
            .body("content[0].zustaendigkeit[0].gueltigkeit[0].size()", is(2))
            .body("content[0].zustaendigkeit[0].gueltigkeit[0].beginn", is(notNullValue()))
            .body("content[0].zustaendigkeit[0].gueltigkeit[0].ende", is(notNullValue()))
            .body("content[0].zustaendigkeit[0].gueltigkeit[0]", not(hasKey("zusatz")));
        response22
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json")
            // Stellt sicher, dass Werte, die im PV_Schema nicht definiert (null) sind, nicht im JSON ausgeliefert werden
            .body("content[0].zustaendigkeit[0].gueltigkeit[0].beginn", is(notNullValue()))
            .body("content[0].zustaendigkeit[0].gueltigkeit[0].ende", is(notNullValue()))
            .body("content[0].zustaendigkeit[0].gueltigkeit[0].size()", is(2))
            .body("content[0].zustaendigkeit[0].gueltigkeit[0]", not(hasKey("zusatz")));
    }

    /**
     * Übermittle einen Zustaendigkeiten in XZuFi 2.2 und 2.3. Die Schnittstelle muss beide Zuständigkeiten in der angeforderten (gleichen) XZuFi Version ausgeben.
     */
    @Test
    void testConverter(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file1 = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMax();
        Leistungsbeschreibung lb = file1.getLeistungsbeschreibungen().getFirst();
        Zustaendigkeit zs = file1.getZustaendigkeitenOrganisationseinheit().getFirst();
        String ars = zs.getArss().getFirst();

        // Daten Einspielen
        // Es werden Datensätze in XZuFi 2.2 und 2.3 eingespielt.
        ImportUtil.pushAndWait(file1);

        Organisationseinheit oe2 = OeCreator.getInstance().getMax(testcasename);
        Zustaendigkeit zs2 = ZsOrganisationseinheitCreator.getInstance().getMax(lb.getId(), oe2.getId());
        XzufiFile file2 = new XzufiFile();
        file2.addBusinessobjects(oe2, zs2);
        // XZuFi 2.3
        ImportUtil.pushAndWait23(file2);

        // Validate
        Response response22 = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);
        response22.then().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("content", hasSize(2));

        // Es wird erwartet, dass beide Zuständigkeiten in XZUFI 2.2 ausgeliefert werden.
        assertDoesNotThrow(
            () ->
                response22
                    .then()
                    .extract()
                    .jsonPath()
                    .getObject(
                        "content[0].zustaendigkeit[0]",
                        de.xoev.xzufi.v2_2_0.ZustaendigkeitOrganisationseinheit.class
                    ),
            "Übermittelte Zustaendigkeit passt nicht zum XZuFi 2.2 Datenmodell"
        );
        assertDoesNotThrow(
            () ->
                response22
                    .then()
                    .extract()
                    .jsonPath()
                    .getObject(
                        "content[1].zustaendigkeit[0]",
                        de.xoev.xzufi.v2_2_0.ZustaendigkeitOrganisationseinheit.class
                    ),
            "Übermittelte Zustaendigkeit passt nicht zum XZuFi 2.2 Datenmodell"
        );

        // Validate
        Response response23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);
        response23.then().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("content", hasSize(2));

        // Es wird erwartet, dass beide Zuständigkeiten in XZUFI 2.3 ausgeliefert werden.
        assertDoesNotThrow(
            () ->
                response23
                    .then()
                    .extract()
                    .jsonPath()
                    .getObject(
                        "content[0].zustaendigkeit[0]",
                        de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit.class
                    ),
            "Übermittelte Zustaendigkeit passt nicht zum XZuFi 2.2 Datenmodell"
        );
        assertDoesNotThrow(
            () ->
                response23
                    .then()
                    .extract()
                    .jsonPath()
                    .getObject(
                        "content[1].zustaendigkeit[0]",
                        de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit.class
                    ),
            "Übermittelte Zustaendigkeit passt nicht zum XZuFi 2.2 Datenmodell"
        );
    }

    @Test
    void relationOneZSOE(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait23(file);

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zs = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        String ars = zs.getArss().getFirst();

        // Validation
        Response response = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);
        RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
        validator
            .validateCount(1)
            .validateParentIdValue(0, oe.getId())
            .validateParentIdSchemeAgencyId(0, oe.getSchemeAgencyId())
            .validateIdValue(0, lb.getId() + "." + oe.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zs.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars);

        Response response23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);
        RelationV3ResultListValidator validator23 = new RelationV3ResultListValidator(response23);
        validator23
            .validateCount(1)
            .validateParentIdValue(0, oe.getId())
            .validateParentIdSchemeAgencyId(0, oe.getSchemeAgencyId())
            .validateParentClass(0, "004", "Organisationseinheit")
            .validateIdValue(0, lb.getId() + "." + oe.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zs.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars);
    }

    @Test
    void relationOneZSOD(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        ImportUtil.pushAndWait23(file);

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Onlinedienst od = file.getOnlinedienste().getFirst();
        Zustaendigkeit zs = file.getZustaendigkeitenOnlinedienst().getFirst();
        String ars = zs.getArss().getFirst();
        Response response = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);
        Response response23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);

        // Validation
        RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
        validator
            .validateCount(1)
            .validateParentIdValue(0, od.getId())
            .validateParentIdSchemeAgencyId(0, od.getSchemeAgencyId())
            .validateIdValue(0, lb.getId() + "." + od.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zs.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars);
        RelationV3ResultListValidator validator23 = new RelationV3ResultListValidator(response23);
        validator23
            .validateCount(1)
            .validateParentIdValue(0, od.getId())
            .validateParentIdSchemeAgencyId(0, od.getSchemeAgencyId())
            .validateParentClass(0, "003", "Onlinedienst")
            .validateIdValue(0, lb.getId() + "." + od.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zs.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars);
    }

    @Test
    void relationTwoZSOE(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(2).getMin();
        ImportUtil.pushAndWait23(file);

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit oe1 = file.getOrganisationseinheiten().get(0);
        Organisationseinheit oe2 = file.getOrganisationseinheiten().get(1);
        Zustaendigkeit zs1 = file.getZustaendigkeitenOrganisationseinheit().get(0);
        Zustaendigkeit zs2 = file.getZustaendigkeitenOrganisationseinheit().get(1);
        String ars = zs1.getArss().getFirst();
        Response response = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);
        Response response23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);

        // Validation
        RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
        validator
            .validateCount(2)
            .validateParentIdValue(0, oe1.getId())
            .validateParentIdSchemeAgencyId(0, oe1.getSchemeAgencyId())
            .validateIdValue(0, lb.getId() + "." + oe1.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zs1.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars)
            .validateParentIdValue(1, oe2.getId())
            .validateParentIdSchemeAgencyId(1, oe2.getSchemeAgencyId())
            .validateIdValue(1, lb.getId() + "." + oe2.getId() + "." + ars)
            .validateIdSchemeAgencyId(1, zs2.getSchemeAgencyId())
            .validateLeistungidValue(1, lb.getId())
            .validateLeistungidSchemeAgencyId(1, lb.getSchemeAgencyId())
            .validateGebietidValue(1, ars);

        RelationV3ResultListValidator validator23 = new RelationV3ResultListValidator(response23);
        validator23
            .validateCount(2)
            .validateParentIdValue(0, oe1.getId())
            .validateParentIdSchemeAgencyId(0, oe1.getSchemeAgencyId())
            .validateParentClass(0, "004", "Organisationseinheit")
            .validateIdValue(0, lb.getId() + "." + oe1.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zs1.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars)
            .validateParentIdValue(1, oe2.getId())
            .validateParentIdSchemeAgencyId(1, oe2.getSchemeAgencyId())
            .validateParentClass(1, "004", "Organisationseinheit")
            .validateIdValue(1, lb.getId() + "." + oe2.getId() + "." + ars)
            .validateIdSchemeAgencyId(1, zs2.getSchemeAgencyId())
            .validateLeistungidValue(1, lb.getId())
            .validateLeistungidSchemeAgencyId(1, lb.getSchemeAgencyId())
            .validateGebietidValue(1, ars);
    }

    @Test
    void relationTwoZSOD(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(2).getMin();
        ImportUtil.pushAndWait23(file);

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Onlinedienst od1 = file.getOnlinedienste().get(0);
        Onlinedienst od2 = file.getOnlinedienste().get(1);
        Zustaendigkeit zs1 = file.getZustaendigkeitenOnlinedienst().get(0);
        Zustaendigkeit zs2 = file.getZustaendigkeitenOnlinedienst().get(1);
        String ars = zs1.getArss().getFirst();
        Response response = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);
        Response response23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);

        // Validation
        RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
        validator
            .validateCount(2)
            .validateParentIdValue(0, od1.getId())
            .validateParentIdSchemeAgencyId(0, od1.getSchemeAgencyId())
            .validateIdValue(0, lb.getId() + "." + od1.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zs1.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars)
            .validateParentIdValue(1, od2.getId())
            .validateParentIdSchemeAgencyId(1, od2.getSchemeAgencyId())
            .validateIdValue(1, lb.getId() + "." + od2.getId() + "." + ars)
            .validateIdSchemeAgencyId(1, zs2.getSchemeAgencyId())
            .validateLeistungidValue(1, lb.getId())
            .validateLeistungidSchemeAgencyId(1, lb.getSchemeAgencyId())
            .validateGebietidValue(1, ars);
        RelationV3ResultListValidator validator23 = new RelationV3ResultListValidator(response23);
        validator23
            .validateCount(2)
            .validateParentIdValue(0, od1.getId())
            .validateParentIdSchemeAgencyId(0, od1.getSchemeAgencyId())
            .validateParentClass(0, "003", "Onlinedienst")
            .validateIdValue(0, lb.getId() + "." + od1.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zs1.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars)
            .validateParentIdValue(1, od2.getId())
            .validateParentIdSchemeAgencyId(1, od2.getSchemeAgencyId())
            .validateParentClass(1, "003", "Onlinedienst")
            .validateIdValue(1, lb.getId() + "." + od2.getId() + "." + ars)
            .validateIdSchemeAgencyId(1, zs2.getSchemeAgencyId())
            .validateLeistungidValue(1, lb.getId())
            .validateLeistungidSchemeAgencyId(1, lb.getSchemeAgencyId())
            .validateGebietidValue(1, ars);
    }

    @Test
    void relationZSOEandZSOD(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        ImportUtil.pushAndWait23(file);

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();
        Onlinedienst od = file.getOnlinedienste().getFirst();
        Zustaendigkeit zsoe = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        Zustaendigkeit zsod = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        String ars = zsoe.getArss().getFirst();

        Response response = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);
        Response response23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);

        //Validation
        RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
        validator
            .validateCount(2)
            .validateParentIdValue(0, od.getId())
            .validateParentIdSchemeAgencyId(0, od.getSchemeAgencyId())
            .validateIdValue(0, lb.getId() + "." + od.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zsod.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars)
            .validateParentIdValue(1, oe.getId())
            .validateParentIdSchemeAgencyId(1, oe.getSchemeAgencyId())
            .validateIdValue(1, lb.getId() + "." + oe.getId() + "." + ars)
            .validateIdSchemeAgencyId(1, zsoe.getSchemeAgencyId())
            .validateLeistungidValue(1, lb.getId())
            .validateLeistungidSchemeAgencyId(1, lb.getSchemeAgencyId())
            .validateGebietidValue(1, ars);

        RelationV3ResultListValidator validator23 = new RelationV3ResultListValidator(response23);
        validator23
            .validateCount(2)
            .validateParentIdValue(0, od.getId())
            .validateParentIdSchemeAgencyId(0, od.getSchemeAgencyId())
            .validateParentClass(0, "003", "Onlinedienst")
            .validateIdValue(0, lb.getId() + "." + od.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zsod.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars)
            .validateParentIdValue(1, oe.getId())
            .validateParentIdSchemeAgencyId(1, oe.getSchemeAgencyId())
            .validateParentClass(1, "004", "Organisationseinheit")
            .validateIdValue(1, lb.getId() + "." + oe.getId() + "." + ars)
            .validateIdSchemeAgencyId(1, zsoe.getSchemeAgencyId())
            .validateLeistungidValue(1, lb.getId())
            .validateLeistungidSchemeAgencyId(1, lb.getSchemeAgencyId())
            .validateGebietidValue(1, ars);
    }

    @Test
    void relationTwoLBinRequest(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(2).oe(1).od(1).getMin();
        ImportUtil.pushAndWait23(file);

        Leistungsbeschreibung lb1 = file.getLeistungsbeschreibungen().get(0);
        Leistungsbeschreibung lb2 = file.getLeistungsbeschreibungen().get(1);
        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();
        Onlinedienst od = file.getOnlinedienste().getFirst();
        Zustaendigkeit zsoe1 = file.getZustaendigkeitenOrganisationseinheit().get(0);
        Zustaendigkeit zsoe2 = file.getZustaendigkeitenOrganisationseinheit().get(1);
        Zustaendigkeit zsod1 = file.getZustaendigkeitenOrganisationseinheit().get(0);
        Zustaendigkeit zsod2 = file.getZustaendigkeitenOrganisationseinheit().get(1);
        String ars = zsoe1.getArss().getFirst();
        Response response = RelationsConnector.getRelationsLbIdsArs220V1(
            List.of(lb1.getFullId(), lb2.getFullId()),
            ars
        );
        Response response23 = RelationsConnector.getRelationsLbIdsArs231V1(
            List.of(lb1.getFullId(), lb2.getFullId()),
            ars
        );

        // Validation
        RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
        validator
            .validateCount(4)
            .validateParentIdValue(0, od.getId())
            .validateParentIdSchemeAgencyId(0, od.getSchemeAgencyId())
            .validateIdValue(0, lb1.getId() + "." + od.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zsod1.getSchemeAgencyId())
            .validateLeistungidValue(0, lb1.getId())
            .validateLeistungidSchemeAgencyId(0, lb1.getSchemeAgencyId())
            .validateGebietidValue(0, ars)
            .validateParentIdValue(1, od.getId())
            .validateParentIdSchemeAgencyId(1, od.getSchemeAgencyId())
            .validateIdValue(1, lb2.getId() + "." + od.getId() + "." + ars)
            .validateIdSchemeAgencyId(1, zsod2.getSchemeAgencyId())
            .validateLeistungidValue(1, lb2.getId())
            .validateLeistungidSchemeAgencyId(1, lb2.getSchemeAgencyId())
            .validateGebietidValue(1, ars)
            .validateParentIdValue(2, oe.getId())
            .validateParentIdSchemeAgencyId(2, oe.getSchemeAgencyId())
            .validateIdValue(2, lb1.getId() + "." + oe.getId() + "." + ars)
            .validateIdSchemeAgencyId(2, zsoe1.getSchemeAgencyId())
            .validateLeistungidValue(2, lb1.getId())
            .validateLeistungidSchemeAgencyId(2, lb1.getSchemeAgencyId())
            .validateGebietidValue(2, ars)
            .validateParentIdValue(3, oe.getId())
            .validateParentIdSchemeAgencyId(3, oe.getSchemeAgencyId())
            .validateIdValue(3, lb2.getId() + "." + oe.getId() + "." + ars)
            .validateIdSchemeAgencyId(3, zsoe2.getSchemeAgencyId())
            .validateLeistungidValue(3, lb2.getId())
            .validateLeistungidSchemeAgencyId(3, lb2.getSchemeAgencyId())
            .validateGebietidValue(3, ars);
        RelationV3ResultListValidator validator23 = new RelationV3ResultListValidator(response23);
        validator23
            .validateCount(4)
            .validateParentIdValue(0, od.getId())
            .validateParentIdSchemeAgencyId(0, od.getSchemeAgencyId())
            .validateParentClass(0, "003", "Onlinedienst")
            .validateIdValue(0, lb1.getId() + "." + od.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zsod1.getSchemeAgencyId())
            .validateLeistungidValue(0, lb1.getId())
            .validateLeistungidSchemeAgencyId(0, lb1.getSchemeAgencyId())
            .validateGebietidValue(0, ars)
            .validateParentIdValue(1, od.getId())
            .validateParentIdSchemeAgencyId(1, od.getSchemeAgencyId())
            .validateParentClass(1, "003", "Onlinedienst")
            .validateIdValue(1, lb2.getId() + "." + od.getId() + "." + ars)
            .validateIdSchemeAgencyId(1, zsod2.getSchemeAgencyId())
            .validateLeistungidValue(1, lb2.getId())
            .validateLeistungidSchemeAgencyId(1, lb2.getSchemeAgencyId())
            .validateGebietidValue(1, ars)
            .validateParentIdValue(2, oe.getId())
            .validateParentIdSchemeAgencyId(2, oe.getSchemeAgencyId())
            .validateParentClass(2, "004", "Organisationseinheit")
            .validateIdValue(2, lb1.getId() + "." + oe.getId() + "." + ars)
            .validateIdSchemeAgencyId(2, zsoe1.getSchemeAgencyId())
            .validateLeistungidValue(2, lb1.getId())
            .validateLeistungidSchemeAgencyId(2, lb1.getSchemeAgencyId())
            .validateGebietidValue(2, ars)
            .validateParentIdValue(3, oe.getId())
            .validateParentIdSchemeAgencyId(3, oe.getSchemeAgencyId())
            .validateParentClass(3, "004", "Organisationseinheit")
            .validateIdValue(3, lb2.getId() + "." + oe.getId() + "." + ars)
            .validateIdSchemeAgencyId(3, zsoe2.getSchemeAgencyId())
            .validateLeistungidValue(3, lb2.getId())
            .validateLeistungidSchemeAgencyId(3, lb2.getSchemeAgencyId())
            .validateGebietidValue(3, ars);
    }

    @Test
    void relationMaxZSOE(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMax();
        ImportUtil.pushAndWait23(file);

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zs = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        String ars = zs.getArss().getFirst();
        Response response = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);
        Response response23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);

        // Validation
        RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
        validator
            .validateCount(1)
            .validateParentIdValue(0, oe.getId())
            .validateParentIdSchemeAgencyId(0, oe.getSchemeAgencyId())
            .validateIdValue(0, lb.getId() + "." + oe.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zs.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars)
            .validateIdSekundaerValue(0, zs.getFitConnectId())
            .validateIdSekundaerSchemeAgencyId(0, "urn:de:fitko")
            .validateIdSekundaerSchemeId(0, "urn:de:fitko:fit-connect:xzufi:destination");
        RelationV3ResultListValidator validator23 = new RelationV3ResultListValidator(response23);
        validator23
            .validateCount(1)
            .validateParentIdValue(0, oe.getId())
            .validateParentIdSchemeAgencyId(0, oe.getSchemeAgencyId())
            .validateParentClass(0, "004", "Organisationseinheit")
            .validateIdValue(0, lb.getId() + "." + oe.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zs.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars)
            .validateKommunikationssystemDestinationSignatureValue(0, zs.getFitConnectId());
    }

    @Test
    void relationMaxZSOD(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMax();
        ImportUtil.pushAndWait23(file);

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Onlinedienst od = file.getOnlinedienste().getFirst();
        Zustaendigkeit zs = file.getZustaendigkeitenOnlinedienst().getFirst();
        String ars = zs.getArss().getFirst();
        Response response = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);
        Response response23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);

        // Validation
        RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
        validator
            .validateCount(1)
            .validateParentIdValue(0, od.getId())
            .validateParentIdSchemeAgencyId(0, od.getSchemeAgencyId())
            .validateIdValue(0, lb.getId() + "." + od.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zs.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars);
        RelationV3ResultListValidator validator23 = new RelationV3ResultListValidator(response23);
        validator23
            .validateCount(1)
            .validateParentIdValue(0, od.getId())
            .validateParentIdSchemeAgencyId(0, od.getSchemeAgencyId())
            .validateParentClass(0, "003", "Onlinedienst")
            .validateIdValue(0, lb.getId() + "." + od.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zs.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars);
    }

    @Test
    void relationNotMatchedLB(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung lbWithoutZs = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(lbWithoutZs);
        ImportUtil.pushAndWait23(file);

        Zustaendigkeit zs = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        String ars = zs.getArss().getFirst();
        Response response = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lbWithoutZs.getFullId()), ars);
        Response response23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lbWithoutZs.getFullId()), ars);

        // Validation
        RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
        validator.validateCount(0);
        RelationV3ResultListValidator validator23 = new RelationV3ResultListValidator(response23);
        validator23.validateCount(0);
    }

    @Test
    void relationNotMatchedARS(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait23(file);

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        String ars = CreatorConstants.DEFAULT_BUNDESLANDCONFIG.getArss().get(1);
        Response response = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);
        Response response23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);

        // Validation
        RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
        validator.validateCount(0);
        RelationV3ResultListValidator validator23 = new RelationV3ResultListValidator(response23);
        validator23.validateCount(0);
    }

    @Test
    void relationAddModifyDelete(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Add
        XzufiFile fileAdd = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        ImportUtil.pushAndWait23(fileAdd);

        Leistungsbeschreibung lb = fileAdd.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit oe = fileAdd.getOrganisationseinheiten().getFirst();
        Onlinedienst od = fileAdd.getOnlinedienste().getFirst();
        Zustaendigkeit zsoe = fileAdd.getZustaendigkeitenOrganisationseinheit().getFirst();
        Zustaendigkeit zsod = fileAdd.getZustaendigkeitenOnlinedienst().getFirst();
        String ars = zsoe.getArss().getFirst();
        Response responseAdd = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);
        Response responseAdd23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);

        // Add Validation
        RelationV3ResultListValidator validatorAdd = new RelationV3ResultListValidator(responseAdd);
        validatorAdd
            .validateCount(2)
            .validateParentIdValue(0, od.getId())
            .validateParentIdSchemeAgencyId(0, od.getSchemeAgencyId())
            .validateIdValue(0, lb.getId() + "." + od.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zsod.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars)
            .validateParentIdValue(1, oe.getId())
            .validateParentIdSchemeAgencyId(1, oe.getSchemeAgencyId())
            .validateIdValue(1, lb.getId() + "." + oe.getId() + "." + ars)
            .validateIdSchemeAgencyId(1, zsoe.getSchemeAgencyId())
            .validateLeistungidValue(1, lb.getId())
            .validateLeistungidSchemeAgencyId(1, lb.getSchemeAgencyId())
            .validateGebietidValue(1, ars);
        RelationV3ResultListValidator validatorAdd23 = new RelationV3ResultListValidator(responseAdd23);
        validatorAdd23
            .validateCount(2)
            .validateParentIdValue(0, od.getId())
            .validateParentIdSchemeAgencyId(0, od.getSchemeAgencyId())
            .validateParentClass(0, "003", "Onlinedienst")
            .validateIdValue(0, lb.getId() + "." + od.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zsod.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars)
            .validateParentIdValue(1, oe.getId())
            .validateParentIdSchemeAgencyId(1, oe.getSchemeAgencyId())
            .validateParentClass(1, "004", "Organisationseinheit")
            .validateIdValue(1, lb.getId() + "." + oe.getId() + "." + ars)
            .validateIdSchemeAgencyId(1, zsoe.getSchemeAgencyId())
            .validateLeistungidValue(1, lb.getId())
            .validateLeistungidSchemeAgencyId(1, lb.getSchemeAgencyId())
            .validateGebietidValue(1, ars);

        // Modify
        XzufiFile fileModify = new XzufiFile();
        zsoe.setFitConnectId("Id_zsoe");
        fileModify.getZustaendigkeitenOrganisationseinheit().add(zsoe);
        ImportUtil.pushAndWait23(fileModify);

        Response responseModify = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);
        Response responseModify23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);

        // Modify Validation
        RelationV3ResultListValidator validatorModify = new RelationV3ResultListValidator(responseModify);
        validatorModify
            .validateCount(2)
            .validateParentIdValue(0, od.getId())
            .validateParentIdSchemeAgencyId(0, od.getSchemeAgencyId())
            .validateIdValue(0, lb.getId() + "." + od.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zsod.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars)
            .validateParentIdValue(1, oe.getId())
            .validateParentIdSchemeAgencyId(1, oe.getSchemeAgencyId())
            .validateIdValue(1, lb.getId() + "." + oe.getId() + "." + ars)
            .validateIdSchemeAgencyId(1, zsoe.getSchemeAgencyId())
            .validateLeistungidValue(1, lb.getId())
            .validateLeistungidSchemeAgencyId(1, lb.getSchemeAgencyId())
            .validateGebietidValue(1, ars)
            .validateIdSekundaerValue(1, "Id_zsoe");
        RelationV3ResultListValidator validatorModify23 = new RelationV3ResultListValidator(responseModify23);
        validatorModify23
            .validateCount(2)
            .validateParentIdValue(0, od.getId())
            .validateParentIdSchemeAgencyId(0, od.getSchemeAgencyId())
            .validateParentClass(0, "003", "Onlinedienst")
            .validateIdValue(0, lb.getId() + "." + od.getId() + "." + ars)
            .validateIdSchemeAgencyId(0, zsod.getSchemeAgencyId())
            .validateLeistungidValue(0, lb.getId())
            .validateLeistungidSchemeAgencyId(0, lb.getSchemeAgencyId())
            .validateGebietidValue(0, ars)
            .validateParentIdValue(1, oe.getId())
            .validateParentIdSchemeAgencyId(1, oe.getSchemeAgencyId())
            .validateParentClass(1, "004", "Organisationseinheit")
            .validateIdValue(1, lb.getId() + "." + oe.getId() + "." + ars)
            .validateIdSchemeAgencyId(1, zsoe.getSchemeAgencyId())
            .validateLeistungidValue(1, lb.getId())
            .validateLeistungidSchemeAgencyId(1, lb.getSchemeAgencyId())
            .validateGebietidValue(1, ars)
            .validateKommunikationssystemDestinationSignatureValue(1, "Id_zsoe");

        // Delete
        XzufiFile fileDelete = new XzufiFile();
        lb.setGeloescht(true);
        fileDelete.getLeistungsbeschreibungen().add(lb);
        oe.setGeloescht(true);
        fileDelete.getOrganisationseinheiten().add(oe);
        zsoe.setGeloescht(true);
        fileDelete.getZustaendigkeitenOrganisationseinheit().add(zsoe);
        od.setGeloescht(true);
        fileDelete.getOnlinedienste().add(od);
        zsod.setGeloescht(true);
        fileDelete.getZustaendigkeitenOnlinedienst().add(zsod);
        ImportUtil.pushAndWait23(fileDelete);

        Response responseDelete = RelationsConnector.getRelationsLbIdsArs220V1(List.of(lb.getFullId()), ars);
        Response responseDelete23 = RelationsConnector.getRelationsLbIdsArs231V1(List.of(lb.getFullId()), ars);

        // Validation
        RelationV3ResultListValidator validatorDelete = new RelationV3ResultListValidator(responseDelete);
        validatorDelete.validateCount(0);
        RelationV3ResultListValidator validatorDelete23 = new RelationV3ResultListValidator(responseDelete23);
        validatorDelete23.validateCount(0);
    }
}
