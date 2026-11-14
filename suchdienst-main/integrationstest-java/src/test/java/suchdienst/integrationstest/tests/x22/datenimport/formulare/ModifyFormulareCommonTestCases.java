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
package suchdienst.integrationstest.tests.x22.datenimport.formulare;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.generator.distributor.XzufiFileDistributor;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulursprungsportal.UrsprungsportalModul;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.OrganisationValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.FormsValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Diese Klasse stellt Testfälle sowohl für das Pull- als auch für das Push-Szenario bereit. Die Szenarien unterscheiden sich hierbei
 * nur durch den verwendeten XzufiFileDistributor.
 * Die Subklassen zum jeweiligen Szenario stellen einen entsprechenden Distributor mithilfe eines ParameterResolvers für die Tests
 * bereit.
 * Sinn dieser Tests ist es, Situationen zu testen, in denen der Sammlerdienst Änderungsnachrichten auf Formulare oder deren Zuständigkeiten
 * sendet, und dabei die Auswirkungen auf die VLs und deren Formulare zu prüfen.
 * Dieser Test testet also zusätzlich zu den bereits bestehenden Tests in ModifyTest das Updaten von Formularen.
 * Hier werden explizit nicht alle Permutationen von Business-Objekten getestet (wie im ModifyTest), sondern nur eine sinnvolle Auswahl,
 * bezogen auf Formulare. So vermeiden wir einen weiteren exponentiellen Anstieg der Testfälle.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
public abstract class ModifyFormulareCommonTestCases extends FormulareCommonTestCasesBase {

    protected ModifyFormulareCommonTestCases(XzufiFileDistributor distributor) {
        super(distributor);
    }

    /**
     * Anlegen einer LB mit FO, Ändern von Attributen nur an dem Formular.
     */
    @Test
    void modifyFO(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).fo(1).zsFo(1).getMin();
        addGlobalOe(fileSetup, testcasename);

        String ars = fileSetup.getZustaendigkeitenFormular().getFirst().getArss().getFirst();
        String leistungFullId = fileSetup.getLeistungsbeschreibungen().getFirst().getFullId();
        Formular formular = fileSetup.getFormulare().getFirst();
        ImportUtil.pushAndWait(fileSetup, distributor);

        XzufiFile file2 = XzufiFileCreator.newInstance().name(testcasename).lb(0).fo(1).getMin();
        formular.setBezeichnung("update-bezeichnung");
        formular.setKurzbezeichnung("update-kurzbezeichnung");
        formular.setBeschreibung("update-beschreibung");
        formular.getLink().setUri("https://example.org/update-uri");
        formular.setMimeCode("application/pdf");
        file2.getFormulare().add(formular);
        ImportUtil.pushAndWait(file2, distributor);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistungFullId);
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistungFullId);
        FormsValidator formsValidator = new FormsValidator(response);
        formsValidator.validateCount(1);
        formsValidator.validateId(0, formular.getFullId());
        formsValidator.validateName(0, "update-bezeichnung");
        formsValidator.validateDescription(0, "update-beschreibung");
        formsValidator.validateUri(0, "https://example.org/update-uri");
        formsValidator.validateMimeCode(0, "application/pdf");
    }

    /**
     * Legt eine LB mit zwei FO an, ändert ein Formular, prüft, ob das andere unverändert bleibt.
     */
    @Test
    void modifyFO2(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).fo(2).getMin();
        addGlobalOe(fileSetup, testcasename);

        String ars = fileSetup.getZustaendigkeitenFormular().getFirst().getArss().getFirst();
        String leistungFullId = fileSetup.getLeistungsbeschreibungen().getFirst().getFullId();
        Formular formular1 = fileSetup.getFormulare().getFirst();
        Formular formular2 = fileSetup.getFormulare().get(1);
        formular2.setBezeichnung("formular2-bezeichnung");
        ImportUtil.pushAndWait(fileSetup, distributor);

        XzufiFile file2 = XzufiFileCreator.newInstance().name(testcasename).lb(0).fo(1).getMin();
        formular1.setBezeichnung("formular1-update-bezeichnung");
        file2.getFormulare().add(formular1);
        ImportUtil.pushAndWait(file2, distributor);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistungFullId);
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistungFullId);
        FormsValidator formsValidator = new FormsValidator(response);
        formsValidator.validateCount(2);
        formsValidator.validateId(0, formular1.getFullId());
        formsValidator.validateName(0, "formular1-update-bezeichnung");
        formsValidator.validateId(1, formular2.getFullId());
        formsValidator.validateName(1, "formular2-bezeichnung");
    }

    /**
     * Ändern von allen Business Objekten.
     */
    @Test
    void modifyLBandOEandODandFOandSP(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String spezTextmodulInhaltModify = spezTextmodulInhalt + "Modify";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).fo(1).od(1).oe(1).sp(1).getMin();

        Spezialisierung spezialisierung = fileSetup.getSpezialisierungen().getFirst();
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
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.getUrsprungsportale().clear();
        leistung.getUrsprungsportale().add(UrsprungsportalModul.builder().uri("www.newPortal.de").build());
        file.getLeistungsbeschreibungen().add(leistung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        organisationseinheit.setTitel("newTitle");
        file.getOrganisationseinheiten().add(organisationseinheit);
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        onlinedienst.getLinks().getFirst().setUri("www.newUri.de");
        file.getOnlinedienste().add(onlinedienst);
        Textmodul textmodul = (Textmodul) spezialisierung.getModulSpezialisierungen().getFirst().getModul();
        textmodul.setInhalt(spezTextmodulInhaltModify);
        file.getSpezialisierungen().add(spezialisierung);
        formular.setBezeichnung("update-bezeichnung");
        formular.setKurzbezeichnung("update-kurzbezeichnung");
        formular.setBeschreibung("update-beschreibung");
        formular.getLink().setUri("https://example.org/update-uri");
        formular.setMimeCode("application/pdf");
        file.getFormulare().add(formular);

        ImportUtil.pushAndWait(file, distributor);

        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhaltModify)
            .validateSourcePortalsCount(1)
            .validateSourcePortalUri(0, "www.newPortal.de")
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateUri(0, onlinedienst.getLinks().getFirst().getUri());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle("newTitle");
        FormsValidator formsValidator = new FormsValidator(response);
        formsValidator.validateCount(1);
        formsValidator.validateId(0, formular.getFullId());
        formsValidator.validateName(0, "update-bezeichnung");
        formsValidator.validateDescription(0, "update-beschreibung");
        formsValidator.validateUri(0, "https://example.org/update-uri");
        formsValidator.validateMimeCode(0, "application/pdf");

        // Prueft ob die Synonymliste aktualisiert wurde
        Response responseListModify = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            spezTextmodulInhaltModify
        );
        ServiceDescriptionResultListValidator validatorListModifySP = new ServiceDescriptionResultListValidator(
            responseListModify
        );
        validatorListModifySP.validateContains(leistung.getFullId());

        Response responseListDelete = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListDeleteSP = new ServiceDescriptionResultListValidator(
            responseListDelete
        );
        validatorListDeleteSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst in der Leistungsbeschreibung aktualisiert wurde.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }
}
