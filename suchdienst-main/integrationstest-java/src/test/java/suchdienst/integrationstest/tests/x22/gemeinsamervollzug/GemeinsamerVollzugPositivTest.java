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
package suchdienst.integrationstest.tests.x22.gemeinsamervollzug;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsFormularCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOnlinedienstCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOrganisationseinheitCreator;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.ErrorValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.FormsValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.TextmodulValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Eine Leistung kann in verschiedenen Bundesländern gemeinsam genutzt werden. Die Leistung wird von einem
 * Land gepflegt (hier SH) und die Organisationseinheit bzw. Onlinedienst kann jeweils eine weitere Zuständigkeit haben
 * in einem anderen Bundesland oder die Leistung hat jeweils eine OE oder OD in zwei verschiedenen Bundesländern.
 * Testfälle:
 * 1 lb mit 3 oe jeweils 1 zs eigenem und 1 zs HH und 1 zs HE
 * 1 lb mit 1 oe mit 3 zs eigenem und 1 zs HH und 1 zs HE
 * 1 lb mit 3 od jeweils 1 zs eigenem und 1 zs HH und 1 zs HE
 * 1 lb mit 1 od mit 3 zs eigenem und 1 zs HH und 1 zs HE
 * 1 lb mit 1 oe mit 1 zs nur für HH und 1 zs für HE - keine SH Zuordnung
 * 1 lb mit 1 od mit 1 zs nur für HH und 1 zs für HE - keine SH Zuordnun
 * 1 lb mit od + sp für HH und HE
 * 1 lb mit oe + sp für HH und HE
 * Getestet wird, dass die Leistung korrekt in den entsprechenden Gebieten Gefunden wird und mit entsprechenden
 * Zuständigkeiten versehen ist.
 */
class GemeinsamerVollzugPositivTest {

    private static final BundeslandConfig BUNDESLAND_CONFIG_SH = new BundeslandConfig(
        "SH",
        "L100012",
        "010000000000",
        List.of("010575755063", "010515163089")
    );

    private static final String BASIS_ARS_SH = BUNDESLAND_CONFIG_SH.getBasicArs();
    private static final String ARS_SH = "010575755063";
    // Stadtstaat Hamburg
    private static final String ARS_HAMBURG = "020000000000";
    // Flächenland Hessen - Gemeinde ARS
    private static final String ARS_HESSEN = "065310005005";
    private static final String BASIS_ARS_HESSEN = "060000000000";

    private static final List<String> ARS_FEDERAL_STATE_LIST = List.of("01", "02", "06");
    private static final List<String> ARS_LIST = List.of(ARS_SH, ARS_HAMBURG, ARS_HESSEN);

    @Test
    void dreiLandFuerOe(TestInfo testInfo) {
        //1 lb mit 1 oe mit 3 zs (SH, HH, HE))
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .oe(1)
            .zsOe(1)
            .getMin();

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.setGemeinsamerVollzug(true);
        lb.setGueltigkeitGebietIDs(List.of(BASIS_ARS_SH, ARS_HAMBURG, BASIS_ARS_HESSEN));
        String searchItem = file.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        Organisationseinheit organisationseinheitSH = file.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeit = ZsOrganisationseinheitCreator.getInstance(BUNDESLAND_CONFIG_SH).getMin(
            lb.getId(),
            organisationseinheitSH.getId(),
            1
        );
        zustaendigkeit.setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        ImportUtil.pushAndWait(file);

        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();

        ARS_LIST.forEach(ars -> {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, searchItem);
            ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
            validator.validateContains(lbId).validateIdList(List.of(lbId));

            Response responseDetail = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, lbId);
            ServiceDescriptionValidator validatorDetail = new ServiceDescriptionValidator(responseDetail);
            validatorDetail
                .validateId(lbId)
                .validateOrganisationIdsCount(1)
                .validateArsFederalstateCount(3)
                .validateArsFederalstateListContains(ARS_FEDERAL_STATE_LIST)
                .validateOrganisationIds(0, organisationseinheitSH.getFullId());
        });
    }

    @Test
    void dreiOeFuerLand(TestInfo testInfo) {
        //1 lb mit 3 oe mit oe1 -> zsSH, oe2 -> zsHH, oe3 -> zsHE
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .oe(3)
            .zsOe(1)
            .getMin();

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.setGemeinsamerVollzug(true);
        lb.setGueltigkeitGebietIDs(List.of(BASIS_ARS_SH, ARS_HAMBURG, BASIS_ARS_HESSEN));
        file.getZustaendigkeitenOrganisationseinheit().get(1).setArss(List.of(ARS_HAMBURG));
        file.getZustaendigkeitenOrganisationseinheit().get(2).setArss(List.of(ARS_HESSEN));
        String searchItem = file.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        Organisationseinheit organisationseinheitSH = file.getOrganisationseinheiten().get(0);
        Organisationseinheit organisationseinheitHH = file.getOrganisationseinheiten().get(1);
        Organisationseinheit organisationseinheitHE = file.getOrganisationseinheiten().get(2);
        ImportUtil.pushAndWait(file);

        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();

        ARS_LIST.forEach(ars -> {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, searchItem);
            ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
            validator.validateContains(lbId).validateIdList(List.of(lbId));
        });

        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        ServiceDescriptionValidator validatorDetailSH = new ServiceDescriptionValidator(responseDetailSH);
        validatorDetailSH
            .validateId(lbId)
            .validateArsFederalstateCount(3)
            .validateArsFederalstateListContains(ARS_FEDERAL_STATE_LIST)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheitSH.getFullId());

        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        ServiceDescriptionValidator validatorDetailHH = new ServiceDescriptionValidator(responseDetailHH);
        validatorDetailHH
            .validateId(lbId)
            .validateOrganisationIdsCount(1)
            .validateArsFederalstateCount(3)
            .validateArsFederalstateListContains(ARS_FEDERAL_STATE_LIST)
            .validateOrganisationIds(0, organisationseinheitHH.getFullId());

        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        ServiceDescriptionValidator validatorDetailHE = new ServiceDescriptionValidator(responseDetailHE);
        validatorDetailHE
            .validateId(lbId)
            .validateOrganisationIdsCount(1)
            .validateArsFederalstateCount(3)
            .validateArsFederalstateListContains(ARS_FEDERAL_STATE_LIST)
            .validateOrganisationIds(0, organisationseinheitHE.getFullId());
    }

    @Test
    void oeFuerFremdesLand(TestInfo testInfo) {
        //1 lb mit 1 oe mit 1 zs für HH, HE und nicht für Basisland SH
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH).name(testclassname).lb(1).oe(1).getMin();

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.setGemeinsamerVollzug(true);
        lb.setGueltigkeitGebietIDs(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getZustaendigkeitenOrganisationseinheit().getFirst().setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        String searchItem = file.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        Organisationseinheit organisationseinheitHH = file.getOrganisationseinheiten().getFirst();
        ImportUtil.pushAndWait(file);

        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();

        Response responseSH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_SH, searchItem);
        ServiceDescriptionResultListValidator validatorSH = new ServiceDescriptionResultListValidator(responseSH);
        validatorSH.validateEmptyResultList();

        Response responseHH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, searchItem);
        ServiceDescriptionResultListValidator validatorHH = new ServiceDescriptionResultListValidator(responseHH);
        validatorHH.validateContains(lbId).validateIdList(List.of(lbId)).validateCount(1);

        Response responseHE = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, searchItem);
        ServiceDescriptionResultListValidator validatorHE = new ServiceDescriptionResultListValidator(responseHE);
        validatorHE.validateContains(lbId).validateIdList(List.of(lbId)).validateCount(1);

        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        ErrorValidator validatorDetailSH = new ErrorValidator(responseDetailSH);
        validatorDetailSH.validateNotFound();

        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        ServiceDescriptionValidator validatorDetailHH = new ServiceDescriptionValidator(responseDetailHH);
        validatorDetailHH
            .validateId(lbId)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheitHH.getFullId());

        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        ServiceDescriptionValidator validatorDetailHE = new ServiceDescriptionValidator(responseDetailHE);
        validatorDetailHE
            .validateId(lbId)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheitHH.getFullId());
    }

    @Test
    void dreiLandFuerOd(TestInfo testInfo) {
        //1 lb mit 1 od mit 3 zs (SH, HH, HE))
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .od(1)
            .zsOd(1)
            .getMin();

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.setGemeinsamerVollzug(true);
        lb.setGueltigkeitGebietIDs(List.of(BASIS_ARS_SH, ARS_HAMBURG, BASIS_ARS_HESSEN));
        String searchItem = file.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        Onlinedienst onlinedienstSH = file.getOnlinedienste().getFirst();
        Zustaendigkeit zustaendigkeit = ZsOnlinedienstCreator.getInstance(BUNDESLAND_CONFIG_SH).getMin(
            lb.getId(),
            onlinedienstSH.getId(),
            1
        );
        zustaendigkeit.setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);
        ImportUtil.pushAndWait(file);

        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();

        ARS_LIST.forEach(ars -> {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, searchItem);
            ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
            validator.validateContains(lbId).validateIdList(List.of(lbId));

            Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
            OnlineServiceLinkValidator validatorDetailSH = new OnlineServiceLinkValidator(responseDetailSH);
            validatorDetailSH
                .validateId(0, onlinedienstSH.getFullId())
                .validateCount(1)
                .validateUri(0, onlinedienstSH.getLinks().getFirst().getUri());
        });
    }

    @Test
    void dreiLandFuerFo(TestInfo testInfo) {
        //1 lb mit 1 fo mit 3 zs (SH, HH, HE))
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .oe(1)
            .fo(1)
            .getMin();

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.setGemeinsamerVollzug(true);
        lb.setGueltigkeitGebietIDs(List.of(BASIS_ARS_SH, ARS_HAMBURG, BASIS_ARS_HESSEN));
        String searchItem = file.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        Formular formularSH = file.getFormulare().getFirst();
        Zustaendigkeit zustaendigkeit = ZsFormularCreator.getInstance(BUNDESLAND_CONFIG_SH).getMin(
            lb.getId(),
            formularSH.getId()
        );
        zustaendigkeit.setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().add(ARS_HAMBURG);
        file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().add(ARS_HESSEN);
        file.getZustaendigkeitenFormular().add(zustaendigkeit);
        ImportUtil.pushAndWait(file);

        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();

        ARS_LIST.forEach(ars -> {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, searchItem);
            ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
            validator.validateContains(lbId).validateIdList(List.of(lbId));

            Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
            FormsValidator validatorDetailSH = new FormsValidator(responseDetailSH);
            validatorDetailSH
                .validateId(0, formularSH.getFullId())
                .validateCount(1)
                .validateUri(0, formularSH.getLink().getUri());
        });
    }

    @Test
    void dreiOdFuerLand(TestInfo testInfo) {
        //1 lb mit 3 od mit od1 -> zsSH, od2 -> zsHH, od3 -> zsHE
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .od(3)
            .zsOd(1)
            .getMin();

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.setGemeinsamerVollzug(true);
        lb.setGueltigkeitGebietIDs(List.of(BASIS_ARS_SH, ARS_HAMBURG, BASIS_ARS_HESSEN));
        file.getZustaendigkeitenOnlinedienst().get(1).setArss(List.of(ARS_HAMBURG));
        file.getZustaendigkeitenOnlinedienst().get(2).setArss(List.of(ARS_HESSEN));
        String searchItem = file.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        Onlinedienst onlinedienstSH = file.getOnlinedienste().get(0);
        Onlinedienst onlinedienstHH = file.getOnlinedienste().get(1);
        Onlinedienst onlinedienstHE = file.getOnlinedienste().get(2);
        ImportUtil.pushAndWait(file);

        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();

        ARS_LIST.forEach(ars -> {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, searchItem);
            ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
            validator.validateContains(lbId).validateIdList(List.of(lbId));
        });

        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        OnlineServiceLinkValidator validatorDetailSH = new OnlineServiceLinkValidator(responseDetailSH);
        validatorDetailSH
            .validateId(0, onlinedienstSH.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienstSH.getLinks().getFirst().getUri());

        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        OnlineServiceLinkValidator validatorOdHH = new OnlineServiceLinkValidator(responseDetailHH);
        validatorOdHH
            .validateId(0, onlinedienstHH.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienstHH.getLinks().getFirst().getUri());

        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        OnlineServiceLinkValidator validatorOdHE = new OnlineServiceLinkValidator(responseDetailHE);
        validatorOdHE
            .validateId(0, onlinedienstHE.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienstHE.getLinks().getFirst().getUri());
    }

    @Test
    void dreiFoFuerLand(TestInfo testInfo) {
        //1 lb mit 3 fo mit fo1 -> zsSH, fo2 -> zsHH, fo3 -> zsHE
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .oe(1)
            .fo(3)
            .getMin();

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.setGemeinsamerVollzug(true);
        lb.setGueltigkeitGebietIDs(List.of(BASIS_ARS_SH, ARS_HAMBURG, BASIS_ARS_HESSEN));
        file.getZustaendigkeitenFormular().get(1).setArss(List.of(ARS_HAMBURG));
        file.getZustaendigkeitenFormular().get(2).setArss(List.of(ARS_HESSEN));
        file.getZustaendigkeitenOrganisationseinheit().get(0).getArss().add(ARS_HAMBURG);
        file.getZustaendigkeitenOrganisationseinheit().get(0).getArss().add(ARS_HESSEN);
        String searchItem = file.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        Formular formularSH = file.getFormulare().get(0);
        Formular formularHH = file.getFormulare().get(1);
        Formular formularHE = file.getFormulare().get(2);
        ImportUtil.pushAndWait(file);

        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();

        ARS_LIST.forEach(ars -> {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, searchItem);
            ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
            validator.validateContains(lbId).validateIdList(List.of(lbId));

            Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
            FormsValidator validatorDetailSH = new FormsValidator(responseDetailSH);
            validatorDetailSH
                .validateId(0, formularSH.getFullId())
                .validateCount(1)
                .validateUri(0, formularSH.getLink().getUri());
        });

        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        FormsValidator validatorOdHH = new FormsValidator(responseDetailHH);
        validatorOdHH
            .validateId(0, formularHH.getFullId())
            .validateCount(1)
            .validateUri(0, formularHH.getLink().getUri());

        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        FormsValidator validatorOdHE = new FormsValidator(responseDetailHE);
        validatorOdHE
            .validateId(0, formularHE.getFullId())
            .validateCount(1)
            .validateUri(0, formularHE.getLink().getUri());
    }

    @Test
    void odFuerFremdesLand(TestInfo testInfo) {
        //1 lb mit 1 od mit 1 zs für HH, 1 zs für HE und nicht für Basisland SH
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH).name(testclassname).lb(1).od(1).getMin();

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.setGemeinsamerVollzug(true);
        lb.setGueltigkeitGebietIDs(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getZustaendigkeitenOnlinedienst().getFirst().setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        String searchItem = file.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        ImportUtil.pushAndWait(file);

        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();

        Response responseSH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_SH, searchItem);
        ServiceDescriptionResultListValidator validatorSH = new ServiceDescriptionResultListValidator(responseSH);
        validatorSH.validateEmptyResultList();

        Response responseHH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, searchItem);
        ServiceDescriptionResultListValidator validatorHH = new ServiceDescriptionResultListValidator(responseHH);
        validatorHH.validateContains(lbId).validateIdList(List.of(lbId)).validateCount(1);

        Response responseHE = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, searchItem);
        ServiceDescriptionResultListValidator validatorHE = new ServiceDescriptionResultListValidator(responseHE);
        validatorHE.validateContains(lbId).validateIdList(List.of(lbId)).validateCount(1);

        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        ErrorValidator validatorDetailSH = new ErrorValidator(responseDetailSH);
        validatorDetailSH.validateNotFound();

        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        OnlineServiceLinkValidator validatorOdHH = new OnlineServiceLinkValidator(responseDetailHH);
        validatorOdHH
            .validateId(0, onlinedienst.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienst.getLinks().getFirst().getUri());

        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        OnlineServiceLinkValidator validatorOdHE = new OnlineServiceLinkValidator(responseDetailHE);
        validatorOdHE
            .validateId(0, onlinedienst.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienst.getLinks().getFirst().getUri());
    }

    @Test
    void foFuerFremdesLand(TestInfo testInfo) {
        //1 lb mit 1 fo mit 1 zs für HH, 1 zs für HE und nicht für Basisland SH
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .fo(1)
            .oe(1)
            .getMin();

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.setGemeinsamerVollzug(true);
        lb.setGueltigkeitGebietIDs(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getZustaendigkeitenFormular().getFirst().setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getZustaendigkeitenOrganisationseinheit().getFirst().setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        String searchItem = file.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        Formular formular = file.getFormulare().getFirst();
        ImportUtil.pushAndWait(file);

        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();

        Response responseSH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_SH, searchItem);
        ServiceDescriptionResultListValidator validatorSH = new ServiceDescriptionResultListValidator(responseSH);
        validatorSH.validateEmptyResultList();

        Response responseHH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, searchItem);
        ServiceDescriptionResultListValidator validatorHH = new ServiceDescriptionResultListValidator(responseHH);
        validatorHH.validateContains(lbId).validateIdList(List.of(lbId)).validateCount(1);

        Response responseHE = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, searchItem);
        ServiceDescriptionResultListValidator validatorHE = new ServiceDescriptionResultListValidator(responseHE);
        validatorHE.validateContains(lbId).validateIdList(List.of(lbId)).validateCount(1);

        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        ErrorValidator validatorDetailSH = new ErrorValidator(responseDetailSH);
        validatorDetailSH.validateNotFound();

        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        FormsValidator validatorOdHH = new FormsValidator(responseDetailHH);
        validatorOdHH.validateId(0, formular.getFullId()).validateCount(1).validateUri(0, formular.getLink().getUri());

        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        FormsValidator validatorOdHE = new FormsValidator(responseDetailHE);
        validatorOdHE.validateId(0, formular.getFullId()).validateCount(1).validateUri(0, formular.getLink().getUri());
    }

    @Test
    void spOdFuerFremdesLand(TestInfo testInfo) {
        //1 lb mit od + sp für HH und HE - keine SH Zuordnung
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .od(1)
            .sp(1)
            .getMin();
        file.getSpezialisierungen().getFirst().setArs(List.of(ARS_HAMBURG, ARS_HESSEN));

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.setGemeinsamerVollzug(true);
        lb.setGueltigkeitGebietIDs(List.of(ARS_HAMBURG, BASIS_ARS_HESSEN));
        file.getZustaendigkeitenOnlinedienst().getFirst().setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        String searchItem = file.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        ImportUtil.pushAndWait(file);

        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();

        Response responseSH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_SH, searchItem);
        ServiceDescriptionResultListValidator validatorSH = new ServiceDescriptionResultListValidator(responseSH);
        validatorSH.validateEmptyResultList();

        Response responseHH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, searchItem);
        ServiceDescriptionResultListValidator validatorHH = new ServiceDescriptionResultListValidator(responseHH);
        validatorHH.validateContains(lbId).validateIdList(List.of(lbId)).validateCount(1);

        Response responseHE = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, searchItem);
        ServiceDescriptionResultListValidator validatorHE = new ServiceDescriptionResultListValidator(responseHE);
        validatorHE.validateContains(lbId).validateIdList(List.of(lbId)).validateCount(1);

        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        ErrorValidator validatorDetailSH = new ErrorValidator(responseDetailSH);
        validatorDetailSH.validateNotFound();

        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        OnlineServiceLinkValidator validatorOdHH = new OnlineServiceLinkValidator(responseDetailHH);
        validatorOdHH
            .validateId(0, onlinedienst.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienst.getLinks().getFirst().getUri());

        TextmodulValidator validatorTextmodulHH = new TextmodulValidator(responseDetailHH);
        validatorTextmodulHH.validateTextByCode("03", "spezialisierung 03");

        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        OnlineServiceLinkValidator validatorOdHE = new OnlineServiceLinkValidator(responseDetailHE);
        validatorOdHE
            .validateId(0, onlinedienst.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienst.getLinks().getFirst().getUri());

        TextmodulValidator validatorTextmodulHE = new TextmodulValidator(responseDetailHE);
        validatorTextmodulHE.validateTextByCode("03", "spezialisierung 03");
    }

    @Test
    void spFoFuerFremdesLand(TestInfo testInfo) {
        //1 lb mit fo + sp für HH und HE - keine SH Zuordnung
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .oe(1)
            .fo(1)
            .sp(1)
            .getMin();
        file.getSpezialisierungen().getFirst().setArs(List.of(ARS_HAMBURG, ARS_HESSEN));

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.setGemeinsamerVollzug(true);
        lb.setGueltigkeitGebietIDs(List.of(ARS_HAMBURG, BASIS_ARS_HESSEN));
        file.getZustaendigkeitenFormular().getFirst().setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getZustaendigkeitenOrganisationseinheit().getFirst().setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        Formular formular = file.getFormulare().getFirst();
        String searchItem = file.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        ImportUtil.pushAndWait(file);

        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();

        Response responseSH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_SH, searchItem);
        ServiceDescriptionResultListValidator validatorSH = new ServiceDescriptionResultListValidator(responseSH);
        validatorSH.validateEmptyResultList();

        Response responseHH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, searchItem);
        ServiceDescriptionResultListValidator validatorHH = new ServiceDescriptionResultListValidator(responseHH);
        validatorHH.validateContains(lbId).validateIdList(List.of(lbId)).validateCount(1);

        Response responseHE = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, searchItem);
        ServiceDescriptionResultListValidator validatorHE = new ServiceDescriptionResultListValidator(responseHE);
        validatorHE.validateContains(lbId).validateIdList(List.of(lbId)).validateCount(1);

        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        ErrorValidator validatorDetailSH = new ErrorValidator(responseDetailSH);
        validatorDetailSH.validateNotFound();

        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        FormsValidator validatorFoHH = new FormsValidator(responseDetailHH);
        validatorFoHH.validateId(0, formular.getFullId()).validateCount(1).validateUri(0, formular.getLink().getUri());

        TextmodulValidator validatorTextmodulHH = new TextmodulValidator(responseDetailHH);
        validatorTextmodulHH.validateTextByCode("03", "spezialisierung 03");

        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        FormsValidator validatorFoHE = new FormsValidator(responseDetailHE);
        validatorFoHE.validateId(0, formular.getFullId()).validateCount(1).validateUri(0, formular.getLink().getUri());

        TextmodulValidator validatorTextmodulHE = new TextmodulValidator(responseDetailHE);
        validatorTextmodulHE.validateTextByCode("03", "spezialisierung 03");
    }

    @Test
    void spOeFuerFremdesLand(TestInfo testInfo) {
        //1 lb mit oe + sp für HH und HE - keine SH Zuordnung
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .oe(1)
            .sp(1)
            .getMin();
        file.getSpezialisierungen().getFirst().setArs(List.of(ARS_HAMBURG, ARS_HESSEN));

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.setGemeinsamerVollzug(true);
        lb.setGueltigkeitGebietIDs(List.of(ARS_HAMBURG, BASIS_ARS_HESSEN));
        file.getZustaendigkeitenOrganisationseinheit().getFirst().setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        String searchItem = file.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        ImportUtil.pushAndWait(file);

        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();

        Response responseSH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_SH, searchItem);
        ServiceDescriptionResultListValidator validatorSH = new ServiceDescriptionResultListValidator(responseSH);
        validatorSH.validateEmptyResultList();

        Response responseHH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, searchItem);
        ServiceDescriptionResultListValidator validatorHH = new ServiceDescriptionResultListValidator(responseHH);
        validatorHH.validateContains(lbId).validateIdList(List.of(lbId)).validateCount(1);

        Response responseHE = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, searchItem);
        ServiceDescriptionResultListValidator validatorHE = new ServiceDescriptionResultListValidator(responseHE);
        validatorHE.validateContains(lbId).validateIdList(List.of(lbId)).validateCount(1);

        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        ErrorValidator validatorDetailSH = new ErrorValidator(responseDetailSH);
        validatorDetailSH.validateNotFound();

        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        TextmodulValidator validatorTextmodul = new TextmodulValidator(responseDetailHH);
        validatorTextmodul.validateTextByCode("03", "spezialisierung 03");

        ServiceDescriptionValidator validatorDetailHH = new ServiceDescriptionValidator(responseDetailHH);
        validatorDetailHH
            .validateId(lbId)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());

        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        TextmodulValidator validatorTextmodulHE = new TextmodulValidator(responseDetailHE);
        validatorTextmodulHE.validateTextByCode("03", "spezialisierung 03");

        ServiceDescriptionValidator validatorDetailHE = new ServiceDescriptionValidator(responseDetailHE);
        validatorDetailHE
            .validateId(lbId)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());
    }
}
