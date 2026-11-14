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

import static suchdienst.integrationstest.connectors.ServiceDescriptionConnector.getServiceDescriptionsDetailV5;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.FoCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.SpCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsFormularCreator;
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
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
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
 * Sinn dieser Tests ist es, Situationen zu testen, in denen der Sammlerdienst Löschnachrichten auf Formulare oder deren Zuständigkeiten
 * sendet, und dabei die Auswirkungen auf die VLs und deren Formulare zu prüfen.
 * Dieser Test testet also zusätzlich zu den bereits bestehenden Tests in DeleteTest das Löschen von Formularen im Pull-Szenario.
 * Hier werden explizit nicht alle Permutationen von Business-Objekten getestet (wie im DeleteTest), sondern nur eine sinnvolle Auswahl,
 * bezogen auf Formulare. So vermeiden wir einen weiteren exponentiellen Anstieg der Testfälle.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
public abstract class DeleteFormulareCommonTestCases extends FormulareCommonTestCasesBase {

    protected DeleteFormulareCommonTestCases(XzufiFileDistributor distributor) {
        super(distributor);
    }

    /**
     * Eine LB mit zwei Formularen und jeweils einer Zuständigkeit importieren, dann Formular und eine Zuständigkeit löschen.
     * Es verbleibt die LB mit dem anderen Formular.
     */
    @Test
    void deleteFOandZSFO(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).fo(2).zsFo(1).getMin();
        addGlobalOe(fileSetup, testcasename);

        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Formular formular0 = fileSetup.getFormulare().getFirst();
        formular0.setBezeichnung("formular-0");
        Formular formular1 = fileSetup.getFormulare().get(1);
        formular1.setBezeichnung("formular-1");
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenFormular().getFirst();
        ImportUtil.pushAndWait23(fileSetup, distributor);

        //***** Testcase *****
        // Formular0 samt Zuständigkeit löschen
        String ars = zustaendigkeit.getArss().getFirst();
        XzufiFile file = new XzufiFile();
        formular0.setGeloescht(true);
        Formular foToDelete = FoCreator.getInstance().getMin("formular-0");
        foToDelete.setId(formular0.getId());
        foToDelete.setGeloescht(true);
        file.getFormulare().add(foToDelete);
        var zsfoToDelete = ZsFormularCreator.getInstance().getMin(leistung.getId(), formular0.getId(), 1);
        zsfoToDelete.setArss(List.of(ars));
        zsfoToDelete.setGeloescht(true);
        file.getZustaendigkeitenFormular().add(zsfoToDelete);
        ImportUtil.pushAndWait23(file, distributor);

        Response response = getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        FormsValidator formsValidator = new FormsValidator(response);
        formsValidator.validateCount(1);
        formsValidator.validateName(0, "formular-1");
    }

    /**
     * Eine LB mit zwei Formularen und jeweils einer Zuständigkeit importieren, dann eine Zuständigkeit löschen.
     * Die API liefert dann das dazugehörige Formular nicht mehr aus.
     */
    @Test
    void deleteZSFO(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).fo(2).zsFo(1).getMin();
        addGlobalOe(fileSetup, testcasename);

        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Formular formular0 = fileSetup.getFormulare().getFirst();
        formular0.setBezeichnung("formular-0");
        Formular formular1 = fileSetup.getFormulare().get(1);
        formular1.setBezeichnung("formular-1");
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenFormular().getFirst();
        ImportUtil.pushAndWait23(fileSetup, distributor);

        //***** Testcase *****
        // Zuständigkeit löschen
        String ars = zustaendigkeit.getArss().getFirst();
        XzufiFile file = new XzufiFile();
        var zsfoToDelete = ZsFormularCreator.getInstance().getMin(leistung.getId(), formular0.getId(), 1);
        zsfoToDelete.setArss(List.of(ars));
        zsfoToDelete.setGeloescht(true);
        file.getZustaendigkeitenFormular().add(zsfoToDelete);
        ImportUtil.pushAndWait23(file, distributor);

        Response response = getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        FormsValidator formsValidator = new FormsValidator(response);
        formsValidator.validateCount(1);
        formsValidator.validateName(0, "formular-1");
    }

    /**
     * Eine LB mit allen anhängigen Entitäten anlegen und dann nur die Formularzuständigkeit löschen.
     * Dadurch bleiben alle anderen Entitäten an der VL stehen, nur das Formular verschwindet.
     */
    @Test
    void deleteZSFODoesNotAffectOtherBos(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance()
            .name(testcasename)
            .lb(1)
            .fo(1)
            .zsFo(1)
            .oe(1)
            .zsOe(1)
            .od(1)
            .zsOd(1)
            .sp(0)
            .getMin();

        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        fileSetup.getSpezialisierungen().add(spezialisierung);
        Formular formular = fileSetup.getFormulare().getFirst();
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenFormular().getFirst();
        ImportUtil.pushAndWait23(fileSetup, distributor);

        //***** Testcase *****
        // Zuständigkeit löschen
        String ars = zustaendigkeit.getArss().getFirst();
        XzufiFile file = new XzufiFile();
        var zsfoToDelete = ZsFormularCreator.getInstance().getMin(leistung.getId(), formular.getId(), 1);
        zsfoToDelete.setArss(List.of(ars));
        zsfoToDelete.setGeloescht(true);
        file.getZustaendigkeitenFormular().add(zsfoToDelete);
        ImportUtil.pushAndWait23(file, distributor);

        Response response = getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        FormsValidator formsValidator = new FormsValidator(response);
        formsValidator.validateCount(0);
        validator.validateOrganisationIdsCount(1);
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());
    }

    /**
     * Eine LB mit einem FO und ZS anlegen und dann die LB und ZSFO löschen.
     */
    @Test
    void deleteLBandZSFO(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).fo(1).zsFo(1).getMin();
        addGlobalOe(fileSetup, testcasename);

        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenFormular().getFirst();
        Formular formular = fileSetup.getFormulare().getFirst();

        ImportUtil.pushAndWait23(fileSetup, distributor);

        //***** Testcase *****
        // Leistung und Zuständigkeit löschen
        String ars = zustaendigkeit.getArss().getFirst();
        XzufiFile file = new XzufiFile();
        leistung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistung);
        var zsfoToDelete = ZsFormularCreator.getInstance().getMin(leistung.getId(), formular.getId(), 1);
        zsfoToDelete.setArss(List.of(ars));
        zsfoToDelete.setGeloescht(true);
        file.getZustaendigkeitenFormular().add(zsfoToDelete);

        ImportUtil.pushAndWait23(file, distributor);

        Response response = getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateNotFound();
    }

    /**
     * Dieser Test macht das Gegenteil von @link {@link AddFormulareCommonTestCases#addFOandZSFOSplitVL(TestInfo)}.
     * Legt also 3 VLS an und löscht dann das Formular samt Zustaendigkeit und prüft am Ende, ob nur noch eine VL existiert.
     * Siehe Kommentar dort.
     */
    @Test
    void deleteFOandZSFOJoinVL(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);
        String ars1 = "091710111111";
        String ars2 = "091710112112";
        String ars3 = "091710113113";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).fo(3).getMin();
        addGlobalOe(fileSetup, testcasename);

        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        String leistungId = leistung.getId();
        Formular formular1 = fileSetup.getFormulare().getFirst();
        formular1.setBezeichnung("formular1");
        Formular formular2 = fileSetup.getFormulare().get(1);
        formular2.setBezeichnung("formular2");
        Formular formular3 = fileSetup.getFormulare().get(2);
        formular3.setBezeichnung("formular3");
        Zustaendigkeit zustaendigkeitFormular3 = fileSetup.getZustaendigkeitenFormular().get(2);

        var zsfo1 = ZsFormularCreator.getInstance().getMin(leistungId, formular1.getId(), 1);
        zsfo1.setArss(List.of(ars1, ars2));
        var zsfo2 = ZsFormularCreator.getInstance().getMin(leistungId, formular2.getId(), 1);
        zsfo2.setArss(List.of(ars1, ars2));
        var zsfo3 = ZsFormularCreator.getInstance().getMin(leistungId, formular3.getId(), 1);
        zsfo3.setArss(List.of(ars2, ars3));
        fileSetup.setZustaendigkeitenFormular(List.of(zsfo1, zsfo2, zsfo3));
        ImportUtil.pushAndWait23(fileSetup, distributor);

        // Prüfen, ob 3 VLs sind:
        String leistungFullId = leistung.getFullId();
        new FormsValidator(getServiceDescriptionsDetailV5(ars1, leistungFullId)).validateCount(2);
        new FormsValidator(getServiceDescriptionsDetailV5(ars2, leistungFullId)).validateCount(3);
        new FormsValidator(getServiceDescriptionsDetailV5(ars3, leistungFullId)).validateCount(1);

        // Jetzt formular3 löschen
        XzufiFile file = new XzufiFile();
        formular3.setGeloescht(true);
        file.getFormulare().add(formular3);
        zustaendigkeitFormular3.setGeloescht(true);
        file.getZustaendigkeitenFormular().add(zustaendigkeitFormular3);
        ImportUtil.pushAndWait23(file, distributor);

        // Ergebnis prüfen, 1 VL mit ars1 und ars2 sollten vorliegen:
        // Prüfen, ob VL0 wie oben beschrieben entstanden ist:
        Response response1 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars1, leistungFullId);
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response1);
        validator.validateId(leistungFullId);
        FormsValidator formsValidator1 = new FormsValidator(response1);
        formsValidator1.validateCount(2);
        Response response2 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars2, leistungFullId);
        ServiceDescriptionValidator validator2 = new ServiceDescriptionValidator(response2);
        validator2.validateId(leistungFullId);
        FormsValidator formsValidator2 = new FormsValidator(response2);
        formsValidator2.validateCount(2);
    }
}
