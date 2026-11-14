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
package suchdienst.integrationstest.tests.x23.gemeinsamervollzug;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.OdCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.OeCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsFormularCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOnlinedienstCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOrganisationseinheitCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.FormsValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.TextmodulValidator;
import suchdienst.integrationstest.validator.servicedescriptionsleikaidv4.LeikaIdResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Es wird geprüft, das die Erweiterung einer Bundesleistung zu Leistung im gemeinsamen Vollzug und
 * das Hinzufügen von OE, OD und SP aus verschiedenen Bundesländern korrekt verarbeitet werden kann
 */

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class GemeinsamerVollzugBundesleistungTest {

    private static final BundeslandConfig BC_BUND = new BundeslandConfig(
        "BUND",
        "B100019",
        "000000000000",
        List.of("000000000000")
    );

    private static final BundeslandConfig BC_SH = new BundeslandConfig(
        "SH",
        "L100012",
        "010000000000",
        List.of("010575755063")
    );

    private static final BundeslandConfig BC_NI = new BundeslandConfig(
        "NI",
        "L100040",
        "030000000000",
        List.of("033555401027")
    );

    private static final BundeslandConfig BC_HH = new BundeslandConfig(
        "HH",
        "S100002",
        "020000000000",
        List.of("020000000000")
    );
    private static final BundeslandConfig BC_NW = new BundeslandConfig(
        "NW",
        "L100002",
        "050000000000",
        List.of("055540036036")
    );

    private static final String SP_LAND_PREFIX = "SpezialisierungNullNeun";

    private static final List<BundeslandConfig> BUNDESLAENDER = List.of(BC_SH, BC_HH, BC_NI, BC_NW);

    @BeforeEach
    void setup() {
        ElasticsearchConnector.removeAllData();
    }

    @Test
    void addEveryCountry(TestInfo testInfo) {
        //***** Setup *****
        // LB (BUND)
        // OE BUND mit ZS BUND
        // OD BUND mit ZS BUND
        // Leikasuche findet alle Leikas die jemals eingespielt wurden
        String randomPrefix = TestUtil.generateRandomWord();
        String randomSpBundContent = randomPrefix + "SpezialisierungNullDrei";
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BC_BUND).name(testclassname).lb(1).oe(1).od(1).getMin();
        String lbId = fileSetup.getLeistungsbeschreibungen().getFirst().getFullId();
        String searchItem = fileSetup.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        ImportUtil.pushAndWait23(fileSetup);

        //***** Testcase *****
        // Hinzufügen SP Bund mit Gebiet BUND
        // Hinzufügen ZS SH, ZS HH, ZS NI, ZS NW zu OE BUND
        // Hinzufügen ZS SH, ZS HH, ZS NI, ZS NW zu OD BUND
        // Hinzufügen OE (SH) mit ZS SH
        // Hinzufügen OE (HH) mit ZS HH
        // Hinzufügen OE (NI) mit ZS NI
        // Hinzufügen OE (NW) mit ZS NW

        // Hinzufügen OD (SH) mit ZS SH
        // Hinzufügen OD (HH) mit ZS HH
        // Hinzufügen OD (NI) mit ZS NI
        // Hinzufügen OD (NW) mit ZS NW

        // Hinzufügen SP (SH) mit Gebiet SH
        // Hinzufügen SP (HH) mit Gebiet HH
        // Hinzufügen SP (NI) mit Gebiet NI
        // Hinzufügen SP (NW) mit Gebiet NW

        Map<BundeslandConfig, String> bcOeIdMap = new HashMap<>();
        Map<BundeslandConfig, String> bcOdIdMap = new HashMap<>();
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        BUNDESLAENDER.forEach(bc -> leistung.getGueltigkeitGebietIDs().add(bc.getBasicArs()));
        file.getLeistungsbeschreibungen().add(leistung);

        Spezialisierung spBund = createSpezialisierung(
            BC_BUND,
            leistung.getId(),
            randomPrefix + "SpezialisierungBund",
            randomSpBundContent,
            "03",
            BC_BUND.getSchemeAgencyId()
        );
        file.getSpezialisierungen().add(spBund);

        String oeBundId = fileSetup.getOrganisationseinheiten().getFirst().getId();
        String oeBundFullId = fileSetup.getOrganisationseinheiten().getFirst().getFullId();
        String odBundId = fileSetup.getOnlinedienste().getFirst().getId();
        String odBundFullId = fileSetup.getOnlinedienste().getFirst().getFullId();

        // Länder die eine Zuständigkeit der Ursprünglichen OE bekommen
        BUNDESLAENDER.forEach(bc -> {
            Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance(BC_BUND).getMin(
                leistung.getId(),
                oeBundId,
                1
            );
            zustaendigkeitOE.setArss(bc.getArss());
            file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
            Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance(BC_BUND).getMin(
                leistung.getId(),
                odBundId,
                1
            );
            zustaendigkeitOD.setArss(bc.getArss());
            file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
            file.getSpezialisierungen().getFirst().getArs().add(bc.getArss().getFirst());
        });

        BUNDESLAENDER.forEach(bc -> {
            Organisationseinheit organisationseinheit = OeCreator.getInstance(BC_BUND).getMin(
                testclassname + bc.getKuerzel()
            );
            file.getOrganisationseinheiten().add(organisationseinheit);
            Zustaendigkeit zustaendigkeit = ZsOrganisationseinheitCreator.getInstance(BC_BUND).getMin(
                leistung.getId(),
                organisationseinheit.getId(),
                1
            );
            zustaendigkeit.setArss(bc.getArss());
            file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
            bcOeIdMap.put(bc, organisationseinheit.getFullId());

            Onlinedienst onlinedienst = OdCreator.getInstance(BC_BUND).getMin(testclassname + bc.getKuerzel());
            file.getOnlinedienste().add(onlinedienst);
            Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance(BC_BUND).getMin(
                leistung.getId(),
                onlinedienst.getId(),
                1
            );
            zustaendigkeitOD.setArss(bc.getArss());
            file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
            bcOdIdMap.put(bc, onlinedienst.getFullId());

            Spezialisierung spezialisierung = createSpezialisierung(
                bc,
                leistung.getId(),
                testclassname + bc.getKuerzel(),
                SP_LAND_PREFIX + bc.getKuerzel(),
                "09",
                BC_BUND.getSchemeAgencyId()
            );
            file.getSpezialisierungen().add(spezialisierung);
        });
        ImportUtil.pushAndWait23(file);

        // Für jedes Bundesland wird die normale Stichwortsuche und Leikasuche ausgeführt und geprüft,
        // dass die entsprechende Detailseite dargestellt werden kann

        BUNDESLAENDER.forEach(bc -> {
            //Test Trefferliste
            Response responseSearch = ServiceDescriptionConnector.getServiceDescriptionsV8(
                bc.getArss().getFirst(),
                searchItem
            );
            ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(responseSearch);
            validator.validateContains(lbId).validateIdList(List.of(lbId));
            //Test Details - OE von Bund und zusätzliche OE werden gefunden
            Response responseDetail = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                bc.getArss().getFirst(),
                lbId
            );
            ServiceDescriptionValidator validatorDetail = new ServiceDescriptionValidator(responseDetail);
            validatorDetail
                .validateId(lbId)
                .validateArsFederalstateCount(5)
                .validateOrganisationIdsCount(2)
                .validateOrganisationIdsContains(oeBundFullId)
                .validateOrganisationIdsContains(bcOeIdMap.get(bc));
            //Test Details - OD von Bund und zusätzliche OD werden gefunden
            OnlineServiceLinkValidator validatorOd = new OnlineServiceLinkValidator(responseDetail);
            validatorOd.validateCount(2).validateHasId(odBundFullId).validateHasId(bcOdIdMap.get(bc));
            //validiere Spezialisierung
            TextmodulValidator validatorTextmodul = new TextmodulValidator(responseDetail);
            validatorTextmodul
                .validateTextByCode("03", randomSpBundContent)
                .validateTextByCode("09", SP_LAND_PREFIX + bc.getKuerzel());

            Response responseLeika = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(
                bc.getArss().getFirst(),
                leistung.getReferenzLeiKa()
            );
            LeikaIdResultListValidator validatorLeika = new LeikaIdResultListValidator(responseLeika);
            validatorLeika.validateIdList(List.of(lbId));
        });
    }

    /**
     * <p><b>Testziel:</b> Testet, dass bei Leistung im gemeinsamen Vollzug und Zuständigkeit des Bundes für OE und OD die Leistung mit
     * dieser OE, OD und FO auch in allen Bundesländern abgerufen werden kann.</p>
     * <p>Exemplarisch erfolgt dies für HH, NI und NW. OE und OD ohne die Bundes Zuständigkeit werden dort nicht angezeigt</p>
     */

    @Test
    void bundZS(TestInfo testInfo) {
        //***** Setup *****
        // LB (SH)
        // OE1 (SH) mit ZS SH,BUND
        // OE2 (SH) mit ZS SH
        // OD1 (SH)) mit SH,BUND
        // OD2 (SH) mit ZS SH
        // FO1 (SH)) mit SH,BUND
        // FO2 (SH) mit ZS SH
        // SP (SH)
        // Leikasuche findet alle Leikas die jemals eingespielt wurden

        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BC_SH).name(testclassname).lb(1).oe(2).od(2).fo(2).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        // ZS für OE, OD mit Bund ARS
        String leistungId = leistung.getId();
        Zustaendigkeit zustaendigkeitOEBund = ZsOrganisationseinheitCreator.getInstance(BC_SH).getMin(
            leistungId,
            fileSetup.getOrganisationseinheiten().getFirst().getId(),
            1
        );
        zustaendigkeitOEBund.setArss(List.of(BC_BUND.getBasicArs()));
        Zustaendigkeit zustaendigkeitODBund = ZsOnlinedienstCreator.getInstance(BC_SH).getMin(
            leistungId,
            fileSetup.getOnlinedienste().getFirst().getId(),
            1
        );
        zustaendigkeitODBund.setArss(List.of(BC_BUND.getBasicArs()));
        Zustaendigkeit zustaendigkeitFOBund = ZsFormularCreator.getInstance(BC_SH).getMin(
            leistungId,
            fileSetup.getFormulare().getFirst().getId(),
            1
        );
        zustaendigkeitFOBund.setArss(List.of(BC_BUND.getBasicArs()));

        fileSetup.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOEBund);
        fileSetup.getZustaendigkeitenOnlinedienst().add(zustaendigkeitODBund);
        fileSetup.getZustaendigkeitenFormular().add(zustaendigkeitFOBund);

        String odMitBundZS = fileSetup.getOnlinedienste().getFirst().getFullId();
        String oeMitBundZS = fileSetup.getOrganisationseinheiten().getFirst().getFullId();
        String foMitBundZS = fileSetup.getFormulare().getFirst().getFullId();

        String spezSearchItem = TestUtil.generateRandomWord() + "SpezialisierungSH";
        Spezialisierung spezialisierung = createSpezialisierung(
            BC_SH,
            leistungId,
            testclassname + BC_SH.getKuerzel(),
            spezSearchItem,
            "03",
            BC_SH.getSchemeAgencyId()
        );
        fileSetup.getSpezialisierungen().add(spezialisierung);

        String lbId = fileSetup.getLeistungsbeschreibungen().getFirst().getFullId();

        leistung.setGemeinsamerVollzug(true);
        leistung.getGueltigkeitGebietIDs().add(BC_BUND.getBasicArs());

        String searchItem = fileSetup.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        ImportUtil.pushAndWait23(fileSetup);

        List<BundeslandConfig> bundeslaenderOhneSH = new ArrayList<>();
        BUNDESLAENDER.forEach(land -> {
            if (!land.getKuerzel().equals(BC_SH.getKuerzel())) {
                bundeslaenderOhneSH.add(land);
            }
        });

        // ***** Testscase - Validierung ******
        // Leistung mit OE1 und OD1 und FO1 kann in anderen Bundesländern - exemplarisch NI, HH, NW abgerufen werden
        // Leistung mit OE1, OE2 und OD1, OD2 und FO1, FO2, SP kann in SH abgerufen werden
        bundeslaenderOhneSH.forEach(bc -> {
            //Test Trefferliste
            Response responseSearch = ServiceDescriptionConnector.getServiceDescriptionsV8(
                bc.getArss().getFirst(),
                searchItem
            );
            ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(responseSearch);
            validator.validateContains(lbId).validateIdList(List.of(lbId));
            //Test Spezialisierung für SH wird in anderen Ländern nicht gefunden
            Response responseSearchSpez = ServiceDescriptionConnector.getServiceDescriptionsV8(
                bc.getArss().getFirst(),
                spezSearchItem
            );
            ServiceDescriptionResultListValidator validatorSpez = new ServiceDescriptionResultListValidator(
                responseSearchSpez
            );
            validatorSpez.validateCount(0);
            //Test Details - OE, OD, FO von Bund und zusätzliche OE, OD, FO werden gefunden
            Response responseDetail = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                bc.getArss().getFirst(),
                lbId
            );
            ServiceDescriptionValidator validatorDetail = new ServiceDescriptionValidator(responseDetail);
            validatorDetail
                .validateId(lbId)
                .validateArsFederalstateCount(2)
                .validateOrganisationIdsCount(1)
                .validateOrganisationIdsContains(oeMitBundZS);
            OnlineServiceLinkValidator validatorOd = new OnlineServiceLinkValidator(responseDetail);
            validatorOd.validateCount(1).validateHasId(odMitBundZS);
            FormsValidator formsValidator = new FormsValidator(responseDetail);
            formsValidator.validateCount(1);
            formsValidator.validateIds(foMitBundZS);
            Response responseLeika = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(
                bc.getArss().getFirst(),
                leistung.getReferenzLeiKa()
            );
            LeikaIdResultListValidator validatorLeika = new LeikaIdResultListValidator(responseLeika);
            validatorLeika.validateIdList(List.of(lbId));
        });

        //Abfrage über SH liefert 2 OE und 2 OD und 2 FO
        Response responseSearch = ServiceDescriptionConnector.getServiceDescriptionsV8(
            BC_SH.getArss().getFirst(),
            searchItem
        );
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(responseSearch);
        validator.validateContains(lbId).validateIdList(List.of(lbId));
        //Spezialisierung wird in SH gefunden
        Response responseSearchSpez = ServiceDescriptionConnector.getServiceDescriptionsV8(
            BC_SH.getArss().getFirst(),
            spezSearchItem
        );
        ServiceDescriptionResultListValidator validatorSpez = new ServiceDescriptionResultListValidator(
            responseSearchSpez
        );
        validatorSpez.validateContains(lbId).validateIdList(List.of(lbId));
        //Test Details - OE von Bund und zusätzliche OE werden gefunden
        Response responseDetail = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            BC_SH.getArss().getFirst(),
            lbId
        );
        ServiceDescriptionValidator validatorDetail = new ServiceDescriptionValidator(responseDetail);
        validatorDetail
            .validateId(lbId)
            .validateArsFederalstateCount(2)
            .validateOrganisationIdsCount(2)
            .validateOrganisationIdsContains(oeMitBundZS);
        //Test Details - OD von Bund und zusätzliche OD werden gefunden
        OnlineServiceLinkValidator validatorOd = new OnlineServiceLinkValidator(responseDetail);
        validatorOd.validateCount(2).validateHasId(odMitBundZS);
        FormsValidator formsValidator = new FormsValidator(responseDetail);
        formsValidator.validateCount(2);
        formsValidator.validateHasId(foMitBundZS);
        Response responseLeika = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(
            BC_SH.getArss().getFirst(),
            leistung.getReferenzLeiKa()
        );
        LeikaIdResultListValidator validatorLeika = new LeikaIdResultListValidator(responseLeika);
        validatorLeika.validateIdList(List.of(lbId));
    }

    private Spezialisierung createSpezialisierung(
        BundeslandConfig bc,
        String lbId,
        String spId,
        String content,
        String leikaCode,
        String schemeAgencyId
    ) {
        Textmodul textmodul = new Textmodul();
        textmodul.setLeikaCode(leikaCode);
        textmodul.setInhalt(content);
        textmodul.setPositionDarstellung(4);
        ModulSpezialisierung modulSpezialisierung = new ModulSpezialisierung();
        modulSpezialisierung.setErsetzung(true);
        modulSpezialisierung.setModul(textmodul);

        Spezialisierung spezialisierung = new Spezialisierung();
        spezialisierung.setId(spId);
        spezialisierung.setSchemeAgencyId(schemeAgencyId);
        spezialisierung.setGeloescht(false);
        spezialisierung.getArs().add(bc.getArss().getFirst());
        spezialisierung.setLeistungsId(lbId);
        spezialisierung.getModulSpezialisierungen().add(modulSpezialisierung);
        return spezialisierung;
    }
}
