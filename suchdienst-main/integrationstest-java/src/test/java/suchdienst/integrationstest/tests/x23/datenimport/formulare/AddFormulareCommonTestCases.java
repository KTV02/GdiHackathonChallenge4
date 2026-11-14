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
package suchdienst.integrationstest.tests.x23.datenimport.formulare;

import static org.hamcrest.Matchers.is;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.SpCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsFormularCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOnlinedienstCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOrganisationseinheitCreator;
import de.dataport.pvog.testdatengenerator.generator.distributor.XzufiFileDistributor;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.FormsValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Diese Klasse stellt Testfälle sowohl für das Pull- als auch für das Push-Szeneario bereit. Die Szenarien unterscheiden sich hierbei
 * nur durch den verwendeten XzufiFileDistributor.
 * Die Subklassen zum jeweiligen Szenario stellen einen entsprechenden Distributor mithilfe eines ParameterResolvers für die Tests
 * bereit.
 * Sinn dieser Tests ist es, Situationen zu testen, in denen der Sammlerdienst Nachrichten mit neuen Entitäten sendet, und dabei
 * die Auswirkungen auf die VLs und deren Formulare zu prüfen.
 * Dieser Test testet also zusätzlich zu den bereits bestehenden Tests in AddTest den Import von Formularen.
 * Hier werden explizit nicht alle Permutationen von Business-Objekten getestet (wie im AddTest), sondern nur eine sinnvolle Auswahl,
 * bezogen auf Formulare. So vermeiden wir einen weiteren exponentiellen Anstieg der Testfälle.
 */
public abstract class AddFormulareCommonTestCases extends FormulareCommonTestCasesBase {

    protected AddFormulareCommonTestCases(XzufiFileDistributor distributor) {
        super(distributor);
    }

    /**
     * Einfachster Fall: LB und FO in leeren Bestand importieren.
     */
    @Test
    void addLBandFOandZSFO(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);

        //***** Testcase *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).fo(1).getMin();
        addGlobalOe(file, testcasename);

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Formular formular = file.getFormulare().getFirst();
        formular.setBezeichnung("Formular-Bezeichnung");
        formular.setBeschreibung("Formular-Beschreibung");
        ImportUtil.pushAndWait23(file, distributor);
        String ars = file.getZustaendigkeitenFormular().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId());
        FormsValidator formsValidator = new FormsValidator(response);
        formsValidator.validateCount(1);
        formsValidator.validateId(0, formular.getFullId());
        formsValidator.validateName(0, "Formular-Bezeichnung");
        formsValidator.validateDescription(0, "Formular-Beschreibung");
        formsValidator.validateUri(0, formular.getLink().getUri());
    }

    /**
     * Zu einer bestehenden LB mit Formular ein weiteres Formular samt Zuständigkeit im selben ARS hinzufügen.
     * Als Ergebnis entsteht eine VL mit zwei Formularen.
     */
    @Test
    void addFOandZSFOSameArs(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);

        //***** Setup *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).fo(1).getMin();
        addGlobalOe(file, testcasename);

        Formular formular1 = file.getFormulare().getFirst();
        ImportUtil.pushAndWait23(file, distributor);

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        String ars = file.getZustaendigkeitenFormular().getFirst().getArss().getFirst();

        //***** Testcase *****
        XzufiFile file2 = XzufiFileCreator.newInstance().name(testcasename).lb(0).fo(1).getMin();
        Formular formular2 = file2.getFormulare().getFirst();
        var zsfo = ZsFormularCreator.getInstance().getMin(leistung.getId(), formular2.getId(), 1);
        zsfo.setArss(List.of(ars));
        file2.getZustaendigkeitenFormular().add(zsfo);
        ImportUtil.pushAndWait23(file2, distributor);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId());
        FormsValidator formsValidator = new FormsValidator(response);
        formsValidator.validateCount(2);
        formsValidator.validateIds(formular1.getFullId(), formular2.getFullId());
    }

    /**
     * Zu einer bestehenden LB mit Formular ein weiteres Formular samt Zuständigkeit in einem anderen ARS hinzufügen.
     * Als Ergebnis entstehen zwei VLS mit jeweils einem der Formulare.
     */
    @Test
    void addFOandZSFOOtherArs(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);
        String ars1 = "091710111111";
        String ars2 = "091710112112";

        //***** Setup *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).fo(1).getMin();
        addGlobalOe(file, testcasename);

        file.getZustaendigkeitenFormular().getFirst().setArss(List.of(ars1));
        Formular formular1 = file.getFormulare().getFirst();
        ImportUtil.pushAndWait23(file, distributor);

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        //***** Testcase *****
        XzufiFile file2 = XzufiFileCreator.newInstance().name(testcasename).lb(0).fo(1).getMin();
        Formular formular2 = file2.getFormulare().getFirst();
        var zsfo = ZsFormularCreator.getInstance().getMin(leistung.getId(), formular2.getId(), 1);
        zsfo.setArss(List.of(ars2));
        file2.getZustaendigkeitenFormular().add(zsfo);
        ImportUtil.pushAndWait23(file2, distributor);

        Response response1 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars1, leistung.getFullId());
        ServiceDescriptionValidator validator1 = new ServiceDescriptionValidator(response1);
        validator1.validateId(leistung.getFullId());
        FormsValidator formsValidator1 = new FormsValidator(response1);
        formsValidator1.validateCount(1);
        formsValidator1.validateId(0, formular1.getFullId());

        Response response2 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars2, leistung.getFullId());
        ServiceDescriptionValidator validator2 = new ServiceDescriptionValidator(response2);
        validator2.validateId(leistung.getFullId());
        FormsValidator formsValidator2 = new FormsValidator(response2);
        formsValidator2.validateCount(1);
        formsValidator2.validateId(0, formular2.getFullId());

        response2.then().body("forms[0].id", is(formular2.getFullId()));
    }

    /**
     * Zu einer bestehenden LB mit Formular eine weitere Zuständigkeit in einen anderen ARS hinzufügen.
     * Als Ergebnis entsteht eine VL mit zwei ARS.
     */
    @Test
    void addZSFOOtherArs(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);

        //***** Setup *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).fo(1).getMin();
        addGlobalOe(file, testcasename);

        Formular formular = file.getFormulare().getFirst();
        ImportUtil.pushAndWait23(file, distributor);

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        String ars1 = file.getZustaendigkeitenFormular().getFirst().getArss().getFirst();

        //***** Testcase *****
        XzufiFile file2 = XzufiFileCreator.newInstance().name(testcasename).getMin();
        Zustaendigkeit zsfo = ZsFormularCreator.getInstance().getMin(leistung.getId(), formular.getId(), 2);
        String ars2 = zsfo.getArss().getFirst();
        file2.setZustaendigkeitenFormular(List.of(zsfo));
        ImportUtil.pushAndWait23(file2, distributor);

        // VL lässt sich mit beiden ARS finden:
        Response response1 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars1, leistung.getFullId());
        ServiceDescriptionValidator validator1 = new ServiceDescriptionValidator(response1);
        validator1.validateId(leistung.getFullId());
        FormsValidator formsValidator1 = new FormsValidator(response1);
        formsValidator1.validateCount(1);
        formsValidator1.validateId(0, formular.getFullId());

        Response response2 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars2, leistung.getFullId());
        ServiceDescriptionValidator validator2 = new ServiceDescriptionValidator(response2);
        validator2.validateId(leistung.getFullId());
        FormsValidator formsValidator2 = new FormsValidator(response2);
        formsValidator2.validateCount(1);
        formsValidator2.validateId(0, formular.getFullId());
    }

    /**
     * Von jeder Fachobjekt-Klasse Objekte zu einer bestehenden LB hinzufügen: 2 FOs, 3 OE1, 4 ODs und 1 SP.
     */
    @Test
    void addMultipleOEAndZSOEandODandZSODandFOandZSFOandSP(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).getMin();
        ImportUtil.pushAndWait23(file, distributor);

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        String leistungId = leistung.getId();

        //***** Testcase *****
        XzufiFile file2 = XzufiFileCreator.newInstance().name(testcasename).lb(0).fo(2).oe(3).od(4).getMin();

        List<Zustaendigkeit> zsfos = file2
            .getFormulare()
            .stream()
            .map(fo -> ZsFormularCreator.getInstance().getMin(leistungId, fo.getId(), 1))
            .toList();
        file2.setZustaendigkeitenFormular(zsfos);

        List<Zustaendigkeit> zsoes = file2
            .getOrganisationseinheiten()
            .stream()
            .map(oe -> ZsOrganisationseinheitCreator.getInstance().getMin(leistungId, oe.getId()))
            .toList();
        file2.setZustaendigkeitenOrganisationseinheit(zsoes);

        List<Zustaendigkeit> zsods = file2
            .getOnlinedienste()
            .stream()
            .map(od -> ZsOnlinedienstCreator.getInstance().getMin(leistungId, od.getId()))
            .toList();
        file2.setZustaendigkeitenOnlinedienst(zsods);

        String ars = file2.getZustaendigkeitenFormular().getFirst().getArss().getFirst();

        Spezialisierung sp = SpCreator.getInstance().getMin("name", leistungId);
        sp.setArs(List.of(ars));
        sp.getModulSpezialisierungen().clear();
        sp.getModulSpezialisierungen().add(
            ModulSpezialisierung.builder()
                .ersetzung(true)
                .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                .build()
        );

        file2.getSpezialisierungen().add(sp);
        ImportUtil.pushAndWait23(file2, distributor);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId());
        new FormsValidator(response).validateCount(2);
        validator.validateOrganisationIdsCount(3);
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(4);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());
    }

    /**
     * Dieser Test prüft, ob das Hinzufügen von Formularen mit neuem ARS eine bestehende VL in mehrere VLs "aufbricht".
     * Ausgangssituation ist eine VL0 mit zwei Formularen F1 und F2 jeweils in ARS A1 und A2.
     * Dann kommt ein Formular F3 mit ARS A2 und A3 hinzu.
     * Erwartet wird, dass dadurch 3 VLS entstehen: (A1, (F1, F2)) und (A2, (F1, F2, F3)) und (A3, (F3)).
     */
    @Test
    void addFOandZSFOSplitVL(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);
        String ars1 = "091710111111";
        String ars2 = "091710112112";
        String ars3 = "091710113113";

        //***** Setup *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).fo(2).getMin();
        addGlobalOe(file, testcasename);

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        String leistungId = leistung.getId();

        Formular formular1 = file.getFormulare().get(0);
        Formular formular2 = file.getFormulare().get(1);
        formular1.setBezeichnung("formular1");
        formular2.setBezeichnung("formular2");
        String formular1FullId = formular1.getFullId();
        String formular2FullId = formular2.getFullId();

        var zsfo1 = ZsFormularCreator.getInstance().getMin(leistungId, formular1.getId());
        var zsfo2 = ZsFormularCreator.getInstance().getMin(leistungId, formular2.getId());
        zsfo1.setArss(List.of(ars1, ars2));
        zsfo2.setArss(List.of(ars1, ars2));
        file.setZustaendigkeitenFormular(List.of(zsfo1, zsfo2));
        ImportUtil.pushAndWait23(file, distributor);

        // Prüfen, ob VL0 wie oben beschrieben entstanden ist:
        String leistungFullId = leistung.getFullId();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars1, leistungFullId);
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistungFullId);
        new FormsValidator(response).validateCount(2);
        Response response2 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars2, leistungFullId);
        ServiceDescriptionValidator validator2 = new ServiceDescriptionValidator(response2);
        validator2.validateId(leistungFullId);
        new FormsValidator(response2).validateCount(2);

        // Jetzt das neue Formular3 hinzufügen:
        XzufiFile file2 = XzufiFileCreator.newInstance().name(testcasename).lb(0).fo(1).getMin();
        Formular formular3 = file2.getFormulare().getFirst();
        formular3.setBezeichnung("formular3");
        String formular3FullId = formular3.getFullId();
        var zsfo3 = ZsFormularCreator.getInstance().getMin(leistungId, formular3.getId());
        zsfo3.setArss(List.of(ars2, ars3));
        file2.setZustaendigkeitenFormular(List.of(zsfo3));
        ImportUtil.pushAndWait23(file2, distributor);

        // Ergebnis prüfen, 3 VLs sollten vorliegen:
        Response response3 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars1, leistungFullId);
        ServiceDescriptionValidator validator3 = new ServiceDescriptionValidator(response3);
        validator3.validateId(leistungFullId);
        FormsValidator formsValidator3 = new FormsValidator(response3);
        formsValidator3.validateCount(2);
        formsValidator3.validateIds(formular1FullId, formular2FullId);

        Response response4 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars2, leistungFullId);
        ServiceDescriptionValidator validator4 = new ServiceDescriptionValidator(response4);
        validator4.validateId(leistungFullId);
        FormsValidator formsValidator4 = new FormsValidator(response4);
        formsValidator4.validateCount(3);
        formsValidator4.validateIds(formular1FullId, formular2FullId, formular3FullId);

        Response response5 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars3, leistungFullId);
        ServiceDescriptionValidator validator5 = new ServiceDescriptionValidator(response5);
        validator5.validateId(leistungFullId);
        FormsValidator formsValidator5 = new FormsValidator(response5);
        formsValidator5.validateCount(1);
        formsValidator5.validateId(0, formular3FullId);
    }
}
