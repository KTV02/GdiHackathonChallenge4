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
package suchdienst.integrationstest.tests.trefferlisteleikaidhierarchiev2;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.ResultPageValidator;
import suchdienst.integrationstest.validator.servicedescriptionleikahierarchyv2.LeikaHierarchyResultListValidator;

/**
 * Test zum Feature in PV-6601
 * <pre>
 * Instanz | Leistungsgruppierung | Leistungskennung | Verrichtungskennung | Verrichtungsdetail
 *                    ## Standardisierter Adressraum für Zentrale Nutzung
 *  99     | 001-899              | 001-899          | 001-899             | 001-899
 *                    ## Freier Adressraum für dezentrale Nutzung
 *  00-16  | 900-999              | 900-999          | 900-999             | 900-999
 * </pre>
 *
 * </li>
 * </ol>
 * <p>
 * Durch die vier Gruppen wird eine Hierarchie gebildet, die man sich als Baum vorstellen kann:
 *
 * <pre>
 * 99 008 000 000 000
 * ├───99 008 001 000 000
 * │   ├───99 008 001 001 000
 * │   │   ├───99 008 001 001 001
 * │   │   └───99 008 001 001 020
 * │   └───99 008 001 002 000
 * └───99 008 002 000 000
 * </pre>
 * </p>
 * <p>
 * Alle Knoten eines Teilbaums haben dementsprechend ein gemeinames Präfix, wenn man nur die ersten
 * X Stellen bis zur ersten 000er Gruppe der Wurzel betrachtet.
 * </p>
 * <p>
 * Testfälle:
 * <pre>
 *   99 008 000 000 000
 *   └───99 008 001 000 000
 *       ├───99 008 001 001 000
 *       │   ├───99 008 001 001 001   LB A
 *       │   └───99 008 001 001 020   LB B
 *       ├───99 008 001 002 000       LB C
 *       ├───99 008 001 003 000
 *       └───99 008 001 004 000       LB D
 *           └───99 008 001 004 020
 *   77 008 000 000 000
 *   └───77 008 001 000 000
 *        └───77 008 001 001 000
 *            └───77 008 001 001 001
 *   99 899 000 000 000
 *   └───99 899 899 000 000           LB E
 *        └───99 899 899 899 000
 *            └───99 899 899 899 899
 *   └───11 008 999 000 000
 *       └───11 008 999 999 000       LB X
 *            └───11 008 999 999 999
 * </pre>
 * - exakte Treffer für alle
 * - Suche nach 99 008 001 001 000 liefert A und B         (Hierarchie nach unten erweitern
 * - Suche nach 99 008 001 004 020 liefert D               (Hierarchie nach oben erweitern)
 * - Suche nach 99 008 001 000 000 liefert A, B, C und D   (Hierarchie mehrstufig nach unten)
 * - Suche nach 99 008 001 003 000 liefert nichts
 * - Suche nach 99 899 899 899 899 liefert E
 * - Suche nach 77 008 001 001 001 liefert A
 * - Suche nach 11 008 999 999 999 liefert LB X
 * - Suche nach 11 008 999 000 000 liefert LB X
 * - Suche nach 11 008 000 000 000 liefert A, B, C und E
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LeikaIdHierarchieTest {

    private static final String LEIKA_LB_A = "99008001001001";
    private static final String LEIKA_LB_B = "99008001001020";
    private static final String LEIKA_LB_C = "99008001002000";
    private static final String LEIKA_LB_D = "99008001004000";
    private static final String LEIKA_LB_E = "99899899000000";
    private static final String LEIKA_LB_X = "11008999999000";

    private static Leistungsbeschreibung lbAVerrichtungsdetail; // Besonderheit: LbA  liegt im Randbereich des standardisierten Adressbereichs (001)
    private static Leistungsbeschreibung lbBVerrichtungsdetail;
    private static Leistungsbeschreibung lbCVerrichtungskennung;
    private static Leistungsbeschreibung lbDVerrichtungskennung;
    private static Leistungsbeschreibung lbELeistungskennung; // Besonderheit: LbE liegt im Randbereich des standardisierten Adressbereichs (899)
    private static Leistungsbeschreibung lbXVerrichtungskennungFreieInstanz; // Besonderheit: LbX liegt in einem privaten Adressbereich (900-999 er Blöcke)
    private static final String TEASER_TM24 = "teaser text leikahierarchie test";

    private static String ars;

    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        ElasticsearchConnector.removeAllData();

        String testcasename = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(6).oe(1).getMin();
        //Setzen der LeikaIDs
        lbAVerrichtungsdetail = file.getLeistungsbeschreibungen().get(0);
        lbAVerrichtungsdetail.setReferenzLeiKa(LEIKA_LB_A);
        lbBVerrichtungsdetail = file.getLeistungsbeschreibungen().get(1);
        lbBVerrichtungsdetail.setReferenzLeiKa(LEIKA_LB_B);
        lbCVerrichtungskennung = file.getLeistungsbeschreibungen().get(2);
        lbCVerrichtungskennung.setReferenzLeiKa(LEIKA_LB_C);
        lbDVerrichtungskennung = file.getLeistungsbeschreibungen().get(3);
        lbDVerrichtungskennung.setReferenzLeiKa(LEIKA_LB_D);
        lbELeistungskennung = file.getLeistungsbeschreibungen().get(4);
        lbELeistungskennung.setReferenzLeiKa(LEIKA_LB_E);
        lbXVerrichtungskennungFreieInstanz = file.getLeistungsbeschreibungen().get(5);
        lbXVerrichtungskennungFreieInstanz.setReferenzLeiKa(LEIKA_LB_X);

        List<Textmodul> tm = lbDVerrichtungskennung.getTextmodule();
        tm.add(Textmodul.builder().inhalt(TEASER_TM24).positionDarstellung(24).leikaCode("24").build());
        lbDVerrichtungskennung.setTextmodule(tm);
        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        ImportUtil.pushAndWait23(file);
    }

    /**
     * Exakte Treffer fuer alle LeikaIDs
     */
    @DisplayName("matchByExactLeika")
    @ParameterizedTest(name = "{displayName} {0} {1}")
    @MethodSource("leikaLbPairs")
    void matchByExactLeika(String testcase, String leikaId, String lbId) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaHierarchyV2(ars, leikaId);
        LeikaHierarchyResultListValidator leikaHierarchyResultListValidator = new LeikaHierarchyResultListValidator(
            response
        );
        ResultPageValidator pageValidator = new ResultPageValidator(response);
        leikaHierarchyResultListValidator.validateContains(lbId).validateExactMatch(true);
        pageValidator
            .validateFirst(true)
            .validateLast(true)
            .validatePageableDefaults(0, 100, 0)
            .validateMeta(Map.of("exact", true));
    }

    private static Stream<Arguments> leikaLbPairs() {
        return Stream.of(
            Arguments.of("LbA", LEIKA_LB_A, lbAVerrichtungsdetail.getFullId()),
            Arguments.of("LbB", LEIKA_LB_B, lbBVerrichtungsdetail.getFullId()),
            Arguments.of("LbC", LEIKA_LB_C, lbCVerrichtungskennung.getFullId()),
            Arguments.of("LbD", LEIKA_LB_D, lbDVerrichtungskennung.getFullId()),
            Arguments.of("LbE", LEIKA_LB_E, lbELeistungskennung.getFullId()),
            Arguments.of("LbX", LEIKA_LB_X, lbXVerrichtungskennungFreieInstanz.getFullId())
        );
    }

    /**
     * Suche nach 99 008 001 001 000 liefert A und B (Hierarchie nach unten erweitern)
     * Da keine exakte übereinstimmung gefunden wird, wird die Suche erfolgreich nach unten erweitert.
     */
    @Test
    void hierarchyDownOneStep() {
        String leikaId = "99008001001000";
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaHierarchyV2(ars, leikaId);
        LeikaHierarchyResultListValidator validator = new LeikaHierarchyResultListValidator(response);
        List<String> lbIds = List.of(lbAVerrichtungsdetail.getFullId(), lbBVerrichtungsdetail.getFullId());
        validator
            .validateContains(lbIds)
            .validateNotContains(lbCVerrichtungskennung.getFullId())
            .validateNotContains(lbDVerrichtungskennung.getFullId())
            .validateNotContains(lbELeistungskennung.getFullId())
            .validateNotContains(lbXVerrichtungskennungFreieInstanz.getFullId());
    }

    /**
     * Suche nach 99 008 001 004 020 liefert D (Hierarchie nach oben erweitern)
     * Da keine exakte übereinstimmung gefunden wird, wird die Suche ohne Treffer nach unten erweitert.
     * Anschließend wird die Suche nach oben erweitert und Treffer gefunden.
     */
    @Test
    void hierarchyUpOneStep() {
        String leikaId = "99008001004020";
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaHierarchyV2(ars, leikaId);
        LeikaHierarchyResultListValidator validator = new LeikaHierarchyResultListValidator(response);
        validator
            .validateContains(lbDVerrichtungskennung.getFullId())
            .validateNotContains(lbAVerrichtungsdetail.getFullId())
            .validateNotContains(lbBVerrichtungsdetail.getFullId())
            .validateNotContains(lbCVerrichtungskennung.getFullId())
            .validateNotContains(lbELeistungskennung.getFullId())
            .validateNotContains(lbXVerrichtungskennungFreieInstanz.getFullId())
            .validateExactMatch(false);
    }

    /**
     * Suche nach 99 008 001 000 000 liefert A, B, C und D (Hierarchie mehrstufig nach unten)
     * Da keine exakte übereinstimmung gefunden wird, wird die Suche erfolgreich nach unten erweitert (2 Stufen).
     */
    @Test
    void hierarchyDownMultipleSteps() {
        String leikaId = "99008001000000";
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaHierarchyV2(ars, leikaId);
        LeikaHierarchyResultListValidator validator = new LeikaHierarchyResultListValidator(response);
        List<String> lbIds = List.of(
            lbAVerrichtungsdetail.getFullId(),
            lbBVerrichtungsdetail.getFullId(),
            lbCVerrichtungskennung.getFullId(),
            lbDVerrichtungskennung.getFullId()
        );
        validator
            .validateContains(lbIds)
            .validateNotContains(lbELeistungskennung.getFullId())
            .validateNotContains(lbXVerrichtungskennungFreieInstanz.getFullId())
            .validateExactMatch(false);
    }

    /**
     * Suche nach 99 899 899 899 899 liefert E (Hierarchie mehrstufig nach oben)
     * Da keine exakte übereinstimmung gefunden wird, wird die Suche erfolgreich nach oben erweitert (2 Stufen).
     */
    @Test
    void hierarchyUpMultipleSteps() {
        String leikaId = "99899899899899";
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaHierarchyV2(ars, leikaId);
        LeikaHierarchyResultListValidator validator = new LeikaHierarchyResultListValidator(response);
        validator
            .validateContains(lbELeistungskennung.getFullId())
            .validateNotContains(lbAVerrichtungsdetail.getFullId())
            .validateNotContains(lbBVerrichtungsdetail.getFullId())
            .validateNotContains(lbCVerrichtungskennung.getFullId())
            .validateNotContains(lbDVerrichtungskennung.getFullId())
            .validateNotContains(lbXVerrichtungskennungFreieInstanz.getFullId())
            .validateExactMatch(false);
    }

    /**
     * Suche nach 99 008 001 003 000 liefert nichts
     */
    @Test
    void noMatch() {
        String leikaId = "99008001003000";
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaHierarchyV2(ars, leikaId);
        LeikaHierarchyResultListValidator validator = new LeikaHierarchyResultListValidator(response);
        validator.validateNoContent();
    }

    /**
     * Suche nach 11 008 999 999 999 liefert LB X
     * (Hierarchie nach oben erweitern)
     */
    @Test
    void privateSpaceHierarchyUpOneStep() {
        String leikaId = "11008999999999";
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaHierarchyV2(ars, leikaId);
        LeikaHierarchyResultListValidator validator = new LeikaHierarchyResultListValidator(response);
        validator
            .validateContains(lbXVerrichtungskennungFreieInstanz.getFullId())
            .validateNotContains(lbAVerrichtungsdetail.getFullId())
            .validateNotContains(lbBVerrichtungsdetail.getFullId())
            .validateNotContains(lbCVerrichtungskennung.getFullId())
            .validateNotContains(lbDVerrichtungskennung.getFullId())
            .validateNotContains(lbELeistungskennung.getFullId());
    }

    /**
     * Suche nach 11 008 999 000 000 liefert LB X
     * (Hierarchie nach unten erweitern)
     */
    @Test
    void freeInstanceHierarchyDownOneStep() {
        String leikaId = "11008999000000";
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaHierarchyV2(ars, leikaId);
        LeikaHierarchyResultListValidator validator = new LeikaHierarchyResultListValidator(response);
        validator
            .validateContains(lbXVerrichtungskennungFreieInstanz.getFullId())
            .validateNotContains(lbAVerrichtungsdetail.getFullId())
            .validateNotContains(lbBVerrichtungsdetail.getFullId())
            .validateNotContains(lbCVerrichtungskennung.getFullId())
            .validateNotContains(lbDVerrichtungskennung.getFullId())
            .validateNotContains(lbELeistungskennung.getFullId());
    }

    /**
     * Suche nach 77 008 001 001 001 liefert A
     * Findet exakte Treffer in einem anderen Bundesland
     */
    @Test
    void ignoreInstanceMatchExact() {
        String leikaId = "77008001001001";
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaHierarchyV2(ars, leikaId);
        LeikaHierarchyResultListValidator validator = new LeikaHierarchyResultListValidator(response);
        validator
            .validateContains(lbAVerrichtungsdetail.getFullId())
            .validateNotContains(lbBVerrichtungsdetail.getFullId())
            .validateNotContains(lbCVerrichtungskennung.getFullId())
            .validateNotContains(lbDVerrichtungskennung.getFullId())
            .validateNotContains(lbELeistungskennung.getFullId())
            .validateNotContains(lbXVerrichtungskennungFreieInstanz.getFullId())
            .validateExactMatch(true);
    }

    /**
     * Suche nach 11 008 000 000 000 liefert A, B, C und D
     * Liefer KEIN X, da X in einem privaten Adressraum liegt
     */
    @Test
    void ignoreInstanceMatchDown() {
        String leikaId = "11008000000000";
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaHierarchyV2(ars, leikaId);
        LeikaHierarchyResultListValidator validator = new LeikaHierarchyResultListValidator(response);
        List<String> lbIds = List.of(
            lbAVerrichtungsdetail.getFullId(),
            lbBVerrichtungsdetail.getFullId(),
            lbCVerrichtungskennung.getFullId(),
            lbDVerrichtungskennung.getFullId()
        );
        validator
            .validateContains(lbIds)
            .validateNotContains(lbELeistungskennung.getFullId())
            .validateNotContains(lbXVerrichtungskennungFreieInstanz.getFullId());
    }

    @Test
    void teaser() {
        String leikaId = "99008001004020";
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaHierarchyV2(ars, leikaId);
        LeikaHierarchyResultListValidator validator = new LeikaHierarchyResultListValidator(response);
        validator.validateTeaser(0, TEASER_TM24);
    }
}
