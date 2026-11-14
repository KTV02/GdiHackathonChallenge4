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
import de.dataport.pvog.testdatengenerator.creator.businessobject.FoCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsFormularCreator;
import de.dataport.pvog.testdatengenerator.generator.distributor.XzufiFileDistributor;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.NestedFormsValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.FormsValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;

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
 * <br>
 * TODO: Diese Tests sollten besser in das Package detailseite/organisationunitdetetailsv5 verschoben werden.
 * Es geht ja hier mehr um die Fachlichkeit, die OE auf der Detailseite darzustellen.
 * Im Gegensatz zu den "normalen" Formularen geht es ja nicht um die CRUD-Operationen von Formularen bei der Übernahme der Daten aus dem SaD.
 * Es fehlt auch eine größere Variabilität der Testdaten, bzgl. Kardinalität, Fremdsprachlichkeit etc. Siehe z.b. KontaktpersonenTest.
 * Siehe Kommentare an diesem MR: https://git.dataport.de/pvog/suchdienst/backend/-/merge_requests/2679
 **/
public abstract class AddGeschachtelteFormulareCommonTestCases extends FormulareCommonTestCasesBase {

    protected AddGeschachtelteFormulareCommonTestCases(XzufiFileDistributor distributor) {
        super(distributor);
    }

    /**
     * Testet den Import einer LB mit OE mit "normalen" und geschachtelten Formularen (an der OE, siehe PV-10975).
     * Da die geschachtelten Formulare selbst keine managed Entities sind (in dem Sinne, dass sie nur Unter-Elemente der OE sind und nicht
     * einzeln vom SaD geschickt werden wie die LB-Formulare), brauchen wir keine umfänglichen Tests, die auch das Updaten und Löschen
     * testen.
     */
    @Test
    void addLBandFOandZSFOandOEandNestedForms(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);

        //***** Testcase *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).fo(1).zsFo(1).oe(1).zsOe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        String lbId = leistung.getId();
        Formular formular = file.getFormulare().getFirst();
        formular.setBezeichnung("Formular-Bezeichnung");
        formular.setBeschreibung("Formular-Beschreibung");
        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zsfo = file.getZustaendigkeitenFormular().getFirst();
        Zustaendigkeit zsoe = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        zsoe.setArss(zsfo.getArss()); // gleichen ARS garantieren, sonst werden die Formulare evtl auf mehrere VLS verteilt
        String ars = zsoe.getArss().getFirst();
        FoCreator foCreator = FoCreator.getInstance();
        Formular nestedForm1 = foCreator.getMin("geschachteltes Formular1");
        Formular nestedForm2 = foCreator.getMin("geschachteltes Formular2");
        ZsFormularCreator.getInstance().getMin(lbId, nestedForm1.getId());
        Zustaendigkeit zsNestedForm1 = ZsFormularCreator.getInstance().getMin(lbId, nestedForm1.getId());
        Zustaendigkeit zsNestedForm2 = ZsFormularCreator.getInstance().getMin(lbId, nestedForm2.getId());
        nestedForm1.getZustaendigkeiten().add(zsNestedForm1);
        nestedForm2.getZustaendigkeiten().add(zsNestedForm2);
        nestedForm1.setBezeichnung("geschachteltes Formular1 Bezeichnung");
        nestedForm1.setBeschreibung("geschachteltes Formular1 Beschreibung");
        nestedForm2.setBezeichnung("geschachteltes Formular2 Bezeichnung");
        nestedForm2.setBeschreibung("geschachteltes Formular2 Beschreibung");

        oe.getFormulare().addAll(List.of(nestedForm1, nestedForm2));
        ImportUtil.pushAndWait(file, distributor);

        // An der LB soll es nun ein Formular geben:
        Response lbResponse = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator lbValidator = new ServiceDescriptionValidator(lbResponse);
        lbValidator.validateId(leistung.getFullId());
        FormsValidator formsValidator = new FormsValidator(lbResponse);
        formsValidator.validateCount(1);
        formsValidator.validateId(0, formular.getFullId());
        formsValidator.validateName(0, "Formular-Bezeichnung");
        formsValidator.validateDescription(0, "Formular-Beschreibung");
        formsValidator.validateUri(0, formular.getLink().getUri());

        // Die OE soll zwei geschachtelte Formulare haben:
        Response oeResponse = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            oe.getFullId(),
            ars,
            leistung.getFullId()
        );
        NestedFormsValidator nestedFormsValidator = new NestedFormsValidator(oeResponse);
        nestedFormsValidator.validateCount(2);
        nestedFormsValidator.validateId(0, nestedForm1.getFullId());
        nestedFormsValidator.validateName(0, "geschachteltes Formular1 Bezeichnung");
        nestedFormsValidator.validateDescription(0, "geschachteltes Formular1 Beschreibung");
        nestedFormsValidator.validateUri(0, nestedForm1.getLink().getUri());
        nestedFormsValidator.validateId(1, nestedForm2.getFullId());
        nestedFormsValidator.validateName(1, "geschachteltes Formular2 Bezeichnung");
        nestedFormsValidator.validateDescription(1, "geschachteltes Formular2 Beschreibung");
        nestedFormsValidator.validateUri(1, nestedForm2.getLink().getUri());
    }

    /**
     * Testet den Import einer LB mit OE mit zwei geschachtelten Formularen, von denen aber nur eines für die LB zuständig ist.
     * Im Ergebnis soll dann nur ein geschachteltes Formular sein.
     */
    @Test
    void addLBandNestedFormsForDifferentLBs(TestInfo testInfo) {
        String testcasename = TestUtil.testcasename(testInfo, distributor);

        //***** Testcase *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).zsOe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        String lbId = leistung.getId();
        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zsoe = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        String ars = zsoe.getArss().getFirst();
        FoCreator foCreator = FoCreator.getInstance();
        Formular nestedForm1 = foCreator.getMin("geschachteltes Formular1");
        Formular nestedForm2 = foCreator.getMin("geschachteltes Formular2");
        Zustaendigkeit zsNestedForm1 = ZsFormularCreator.getInstance().getMin(lbId, nestedForm1.getId());
        Zustaendigkeit zsNestedForm2 = ZsFormularCreator.getInstance().getMin(lbId + "-other", nestedForm2.getId());
        nestedForm1.getZustaendigkeiten().add(zsNestedForm1);
        nestedForm2.getZustaendigkeiten().add(zsNestedForm2);
        nestedForm1.setBezeichnung("geschachteltes Formular1 Bezeichnung");
        nestedForm1.setBeschreibung("geschachteltes Formular1 Beschreibung");
        nestedForm2.setBezeichnung("geschachteltes Formular2 Bezeichnung");
        nestedForm2.setBeschreibung("geschachteltes Formular2 Beschreibung");

        oe.getFormulare().addAll(List.of(nestedForm1, nestedForm2));
        ImportUtil.pushAndWait(file, distributor);

        // An der LB soll es kein direktes Formular geben:
        Response lbResponse = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator lbValidator = new ServiceDescriptionValidator(lbResponse);
        lbValidator.validateId(leistung.getFullId());
        FormsValidator formsValidator = new FormsValidator(lbResponse);
        formsValidator.validateCount(0);

        // Die OE soll nur ein geschachteltes Formular haben, da das andere ja gar nicht für die Lb zuständig ist:
        Response oeResponse = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            oe.getFullId(),
            ars,
            leistung.getFullId()
        );
        NestedFormsValidator nestedFormsValidator = new NestedFormsValidator(oeResponse);
        nestedFormsValidator.validateCount(1);
        nestedFormsValidator.validateId(0, nestedForm1.getFullId());
        nestedFormsValidator.validateName(0, "geschachteltes Formular1 Bezeichnung");
        nestedFormsValidator.validateDescription(0, "geschachteltes Formular1 Beschreibung");
        nestedFormsValidator.validateUri(0, nestedForm1.getLink().getUri());
    }
}
