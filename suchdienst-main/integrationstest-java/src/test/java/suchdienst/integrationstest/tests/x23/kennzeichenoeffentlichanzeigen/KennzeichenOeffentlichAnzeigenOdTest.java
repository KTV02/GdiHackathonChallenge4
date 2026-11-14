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
package suchdienst.integrationstest.tests.x23.kennzeichenoeffentlichanzeigen;

import static org.hamcrest.Matchers.equalTo;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.OnlineserviceConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionleikahierarchyv3.LeikaHierarchyResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionsleikaidv4.LeikaIdResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class KennzeichenOeffentlichAnzeigenOdTest {

    private String odId;
    private String zsOdArs;

    @BeforeAll
    static void setup() {
        ElasticsearchConnector.removeAllData();
    }

    @AfterAll
    static void cleanup() {
        ElasticsearchConnector.removeAllData();
    }

    /** OD mit {@code kennzeichenOeffentlichAnzeigen=true} wird als öffentlich ausgegeben. */
    @Test
    void lbMitOeUndOdOeffentlich(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        XzufiFile file = importLbMitOd(testname, testname, true);
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(zsOdArs, lb.getFullId());
        new ServiceDescriptionValidator(response).validateId(lb.getFullId());
        new OnlineServiceLinkValidator(response).validateHasId(odId).validatePublicDisplayAllowed(odId, true);
    }

    /** Prüft die Ausgabe von {@code isPublicDisplayAllowed=false} für {@code /v8beta1/servicedescriptions}. */
    @Test
    void v8LiefertNichtOeffentlichenOdMitPublicDisplayAllowed(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        String suchbegriff = "schniposchranke";
        XzufiFile file = importLbMitOd(testname, suchbegriff, false);
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(zsOdArs, suchbegriff);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lb.getFullId());
        validator
            .serviceDescriptionsContainsOdId(odId)
            .validateOnlineServicePublicDisplayAllowed(lb.getFullId(), odId, false);
    }

    /** Prüft die Ausgabe von {@code isPublicDisplayAllowed=false} für die V6-Detailantwort. */
    @Test
    void v6DetailLiefertNichtOeffentlichenOdMitPublicDisplayAllowed(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        XzufiFile file = importLbMitOd(testname, testname, false);
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(zsOdArs, lb.getFullId());
        new ServiceDescriptionValidator(response).validateId(lb.getFullId());
        new OnlineServiceLinkValidator(response).validateHasId(odId).validatePublicDisplayAllowed(odId, false);
    }

    /** Prüft die Ausgabe von {@code isPublicDisplayAllowed=false} für die V4-LeiKa-Suche. */
    @Test
    void v4LeikaIdLiefertNichtOeffentlichenOdMitPublicDisplayAllowed(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        XzufiFile file = importLbMitOd(testname, testname, false);
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(zsOdArs, lb.getReferenzLeiKa());
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateContains(lb.getFullId());
        validator
            .validateOnlineServiceLinksContainsId(lb.getFullId(), odId)
            .validateOnlineServicePublicDisplayAllowed(lb.getFullId(), odId, false);
    }

    /** Prüft die Ausgabe von {@code isPublicDisplayAllowed=false} für die V3-LeiKa-Hierarchie. */
    @Test
    void v3LeikaHierarchyLiefertNichtOeffentlichenOdMitPublicDisplayAllowed(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        XzufiFile file = importLbMitOd(testname, testname, false);
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaHierarchyV3(
            zsOdArs,
            lb.getReferenzLeiKa()
        );
        LeikaHierarchyResultListValidator validator = new LeikaHierarchyResultListValidator(response);
        validator.validateContains(lb.getFullId());
        validator
            .validateOnlineServiceLinksContainsId(lb.getFullId(), odId)
            .validateOnlineServicePublicDisplayAllowed(lb.getFullId(), odId, false);
    }

    @Test
    void v3OnlineserviceDetailLiefertPublicDisplayAllowed(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        importLbMitOd(testname, testname, false);

        OnlineserviceConnector.getOnlineserviceDetailV3(odId)
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", equalTo(odId))
            .body("isPublicDisplayAllowed", equalTo(false));
    }

    /**
     * Legt eine Leistung mit einem OE und einem OD an, dessen {@code kennzeichenOeffentlichAnzeigen}
     * auf den übergebenen Wert gesetzt ist, und importiert die Daten. Setzt dabei {@code suchbegriff},
     * {@code odId} und {@code zsOdArs} auf die statischen Felder der Testklasse.
     */
    private XzufiFile importLbMitOd(String testcaseName, String suchtext, boolean kennzeichenOeffentlichAnzeigen) {
        XzufiFile file = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(1).od(1).getMin();

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        Textmodul tm03 = Textmodul.builder().inhalt(suchtext).positionDarstellung(3).leikaCode("03").build();
        textmodule.add(tm03);
        lb.setTextmodule(textmodule);

        Onlinedienst od = file.getOnlinedienste().getFirst();
        od.setKennzeichenOeffentlichAnzeigen(kennzeichenOeffentlichAnzeigen);
        odId = od.getFullId();

        zsOdArs = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();

        ImportUtil.pushAndWait23(file);
        return file;
    }
}
