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
package suchdienst.integrationstest.tests.x23.trefferlisteleikaidv4;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsleikaidv4.LeikaIdResultListValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LeikaIdTest {

    private static String ersteLbId;
    private static String ars;
    private static String ersteBeschreibungTM02;
    private static String ersterTitelTM03;
    private static String ersterVolltextTM06;
    private static String ersterBegriffImKontext;
    private static String ersterOdBezeichnung;
    private static String ersterOdBeschreibung;
    private static String ersterUrsprungsportalTitel;
    private static String ersteLeikaId;

    private static String zweiteLbId;
    private static String zweiteBeschreibungTM02;
    private static String zweiterTitelTM03;
    private static String zweiteLeikaId;

    private static String ersteOeId;
    private static String zweiteOeId;
    private static String dritteOeId;

    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        ElasticsearchConnector.removeAllData();

        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        SimpleDateFormat dateFormat = new SimpleDateFormat("ddHHmm");
        ersteLeikaId = "99001007" + dateFormat.format(Timestamp.valueOf(LocalDateTime.now()));
        zweiteLeikaId = "TMP_12_99001006" + dateFormat.format(Timestamp.valueOf(LocalDateTime.now()));

        XzufiFile file = XzufiFileCreator.newInstance()
            .name(testclassname + "ERSTER")
            .lb(1)
            .oe(1)
            .od(1)
            .getMin();
        //set relevant values for trefferliste
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        lb.setReferenzLeiKa(ersteLeikaId);

        List<Textmodul> textmodule = new ArrayList<>();
        ersteBeschreibungTM02 = randomPrefix + "ersteleikaidBeschreibung02tm";
        Textmodul tm02 = Textmodul.builder()
            .inhalt(ersteBeschreibungTM02)
            .positionDarstellung(2)
            .leikaCode("02")
            .build();
        ersterTitelTM03 = randomPrefix + "ersteleikaidTitel03tm";
        Textmodul tm03 = Textmodul.builder().inhalt(ersterTitelTM03).positionDarstellung(3).leikaCode("03").build();
        ersterVolltextTM06 = randomPrefix + "ersteleikaidVolltext06tm";
        Textmodul tm06 = Textmodul.builder().inhalt(ersterVolltextTM06).positionDarstellung(6).leikaCode("06").build();
        textmodule.add(tm02);
        textmodule.add(tm03);
        textmodule.add(tm06);
        lb.setTextmodule(textmodule);

        List<Suchbegriff> suchbegriffe = new ArrayList<>();
        ersterBegriffImKontext = randomPrefix + "ersteleikaidBegriffImKontext";
        Suchbegriff suchbegriff = new Suchbegriff();
        suchbegriff.setBegriff(ersterBegriffImKontext);
        suchbegriffe.add(suchbegriff);
        lb.setSuchbegriffe(suchbegriffe);

        ersterUrsprungsportalTitel = randomPrefix + "ersteleikaidUrsprungsportalTitel";
        lb.getUrsprungsportale().getFirst().setTitle(ersterUrsprungsportalTitel);

        ersterOdBezeichnung = randomPrefix + "ersteleikaidODBezeichnung";
        ersterOdBeschreibung = randomPrefix + "ersteleikaidODBeschreibung";
        Onlinedienst od = file.getOnlinedienste().getFirst();
        od.setBeschreibung(ersterOdBeschreibung);
        od.setBezeichnung(ersterOdBezeichnung);

        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        ersteLbId = file.getLeistungsbeschreibungen().getFirst().getFullId();
        ersteOeId = file.getOrganisationseinheiten().getFirst().getFullId();

        ImportUtil.push(file);

        XzufiFile secondFile = XzufiFileCreator.newInstance()
            .name(testclassname + "ZWEITER")
            .lb(1)
            .oe(2)
            .getMin();

        //set relevant values for trefferliste
        Leistungsbeschreibung zweiteLB = secondFile.getLeistungsbeschreibungen().getFirst();

        zweiteLB.setReferenzLeiKa(zweiteLeikaId);

        List<Textmodul> zweitetextmodule = new ArrayList<>();
        zweiteBeschreibungTM02 = randomPrefix + "zweiteleikaidBeschreibung02tm";
        Textmodul zweitetm02 = Textmodul.builder()
            .inhalt(zweiteBeschreibungTM02)
            .positionDarstellung(2)
            .leikaCode("02")
            .build();
        zweiterTitelTM03 = randomPrefix + "zweiteleikaidTitel03tm";
        Textmodul zweitertm03 = Textmodul.builder()
            .inhalt(zweiterTitelTM03)
            .positionDarstellung(3)
            .leikaCode("03")
            .build();
        String zweiterVolltextTM06 = randomPrefix + "zweiterleikaidVolltext06tm";
        Textmodul zweitertm06 = Textmodul.builder()
            .inhalt(zweiterVolltextTM06)
            .positionDarstellung(6)
            .leikaCode("06")
            .build();
        zweitetextmodule.add(zweitetm02);
        zweitetextmodule.add(zweitertm03);
        zweitetextmodule.add(zweitertm06);
        zweiteLB.setTextmodule(zweitetextmodule);

        ImportUtil.pushAndWait23(secondFile);

        zweiteLbId = secondFile.getLeistungsbeschreibungen().getFirst().getFullId();
        zweiteOeId = secondFile.getOrganisationseinheiten().get(0).getFullId();
        dritteOeId = secondFile.getOrganisationseinheiten().get(1).getFullId();
    }

    @Test
    void matchBySingleLeikaId() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, ersteLeikaId);
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateCount(1).validateContains(ersteLbId).validateNotContains(zweiteLbId);

        response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, zweiteLeikaId);
        validator = new LeikaIdResultListValidator(response);
        validator.validateCount(1).validateContains(zweiteLbId).validateNotContains(ersteLbId);
    }

    @Test
    void matchByLeikaIdList() {
        List<String> leikaIds = Arrays.asList(ersteLeikaId, zweiteLeikaId);
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, leikaIds);
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);

        validator.validateCount(2).validateContains(ersteLbId).validateContains(zweiteLbId);
    }

    @Test
    void noMatchBySingleUnknownLeikaId() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, "99001333333333");
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateEmptyResultList();
    }

    @Test
    void matchByLeikaIdsAndDescription() {
        List<String> leikaIds = Arrays.asList(ersteLeikaId, zweiteLeikaId);
        List<String> singleQueryItem = Collections.singletonList(ersteBeschreibungTM02);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, leikaIds, singleQueryItem);
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateCount(1).validateContains(ersteLbId).validateNotContains(zweiteLbId);
    }

    @Test
    void matchByLeikaIdsAndTitle() {
        List<String> leikaIds = Arrays.asList(ersteLeikaId, zweiteLeikaId);
        List<String> singleQueryItem = Collections.singletonList(ersterTitelTM03);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, leikaIds, singleQueryItem);
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateCount(1).validateContains(ersteLbId).validateNotContains(zweiteLbId);
    }

    @Test
    void matchByLeikaIdsAndFulltext() {
        List<String> leikaIds = Arrays.asList(ersteLeikaId, zweiteLeikaId);
        List<String> singleQueryItem = Collections.singletonList(ersterVolltextTM06);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, leikaIds, singleQueryItem);
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateCount(1).validateContains(ersteLbId).validateNotContains(zweiteLbId);
    }

    @Test
    void matchByLeikaIdsSynonym() {
        List<String> leikaIds = Arrays.asList(ersteLeikaId, zweiteLeikaId);
        List<String> singleQueryItem = Collections.singletonList(ersterBegriffImKontext);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, leikaIds, singleQueryItem);
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateCount(1).validateContains(ersteLbId).validateNotContains(zweiteLbId);
    }

    @Test
    void matchByLeikaIdsAndSourceportalTitle() {
        List<String> leikaIds = Arrays.asList(ersteLeikaId, zweiteLeikaId);
        List<String> singleQueryItem = Collections.singletonList(ersterUrsprungsportalTitel);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, leikaIds, singleQueryItem);
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateCount(1).validateContains(ersteLbId).validateNotContains(zweiteLbId);
    }

    @Test
    void matchByLeikaIdsAndODdescription() {
        List<String> leikaIds = Arrays.asList(ersteLeikaId, zweiteLeikaId);
        List<String> singleQueryItem = Collections.singletonList(ersterOdBeschreibung);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, leikaIds, singleQueryItem);
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateCount(1).validateContains(ersteLbId).validateNotContains(zweiteLbId);
    }

    @Test
    void matchByLeikaIdsAndODdefinition() {
        List<String> leikaIds = Arrays.asList(ersteLeikaId, zweiteLeikaId);
        List<String> singleQueryItem = Collections.singletonList(ersterOdBezeichnung);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, leikaIds, singleQueryItem);
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateCount(1).validateContains(ersteLbId).validateNotContains(zweiteLbId);
    }

    @Test
    void matchByLeikaIdListQueryParamList() {
        List<String> leikaIds = Arrays.asList(ersteLeikaId, zweiteLeikaId);
        List<String> multipleQueryItemsErster = Arrays.asList(
            ersteBeschreibungTM02,
            ersterTitelTM03,
            ersterUrsprungsportalTitel,
            ersterBegriffImKontext,
            ersterVolltextTM06
        );

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(
            ars,
            leikaIds,
            multipleQueryItemsErster
        );
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateCount(1).validateContains(ersteLbId).validateNotContains(zweiteLbId);

        List<String> multipleQueryItemsZweiter = Arrays.asList(zweiteBeschreibungTM02, zweiterTitelTM03);

        response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(
            ars,
            leikaIds,
            multipleQueryItemsZweiter
        );
        validator = new LeikaIdResultListValidator(response);
        validator.validateCount(1).validateContains(zweiteLbId).validateNotContains(ersteLbId);
    }

    @Test
    void noMatchByLeikaIdsAndMixedParams() {
        List<String> leikaIds = Arrays.asList(ersteLeikaId, zweiteLeikaId);
        List<String> singleQueryItem = Arrays.asList(ersteBeschreibungTM02, zweiteBeschreibungTM02);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, leikaIds, singleQueryItem);
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateEmptyResultList();
    }

    @Test
    void noMatchByLeikaIdsAndUnknownArs() {
        List<String> leikaIds = Arrays.asList(ersteLeikaId, zweiteLeikaId);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4("090909090909", leikaIds);
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateEmptyResultList();
    }
}
