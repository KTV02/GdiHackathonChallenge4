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
package suchdienst.integrationstest.tests.x22.detailseite.servicedescriptionsdetailv6;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.FormsValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.ServiceDescriptionValidator;

/**
 * Tests für mehrsprachige Formulare in ServiceDescriptions-Detail V6 API.
 *
 * Die Sprachauswahl erfolgt strikt: Nur Formulare, die Inhalte in der angefragten Sprache
 * haben, werden zurückgegeben. Es gibt keine Fallbacks.
 *
 * In sich "vollständige" XZuFi Formulare (Normalfall): Ein Formular-Element enthält alle Sprachversionen
 * in entsprechenden Elementen. Das {@code languages}-Set gibt an, welche Sprachen verfügbar sind.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class FormulareMehrsprachigkeitTest {

    private static final String FORM_URI = "https://portal.example.org/forms/antrag-001";
    private static final String FORM_URI_2 = "https://portal.example.org/forms/antrag-002";

    @Test
    @DisplayName(
        "Bilinguale Formulare - Deutsche Anfrage liefert alle deutschen Inhalte, auch von gemischten Formularen"
    )
    void bilingualFormsDeutscheAnfrage(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).fo(2).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        // Formular 1 - bilingual
        Formular formular1 = file.getFormulare().get(0);
        formular1.setBezeichnung("Formular A Deutsch");
        formular1.setBeschreibung("Beschreibung A Deutsch");
        formular1.setBezeichnungEnglisch("Form A English");
        formular1.setBeschreibungEnglisch("Description A English");
        formular1.getLink().setUri(FORM_URI);

        // Formular 2 - nur Deutsch
        Formular formular2 = file.getFormulare().get(1);
        formular2.setBezeichnung("Formular B Deutsch");
        formular2.setBeschreibung("Beschreibung B Deutsch");
        formular2.setBezeichnungEnglisch(null);
        formular2.setBeschreibungEnglisch(null);
        formular2.getLink().setUri(FORM_URI_2);

        String ars = file.getZustaendigkeitenFormular().getFirst().getArss().getFirst();
        ImportUtil.pushAndWait(file);

        Response response1 = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_DE
        );

        ServiceDescriptionValidator sdValidator1 = new ServiceDescriptionValidator(response1);
        sdValidator1.validateId(leistung.getFullId()).validateLanguageCode("de");

        FormsValidator formsValidator1 = new FormsValidator(response1);
        formsValidator1
            .validateCount(2)
            .validateId(0, formular1.getFullId())
            .validateName(0, "Formular A Deutsch")
            .validateDescription(0, "Beschreibung A Deutsch")
            .validateUri(0, FORM_URI)
            .validateId(1, formular2.getFullId())
            .validateName(1, "Formular B Deutsch")
            .validateDescription(1, "Beschreibung B Deutsch")
            .validateUri(1, FORM_URI_2);

        Response response2 = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        ServiceDescriptionValidator sdValidator2 = new ServiceDescriptionValidator(response2);
        sdValidator2.validateId(leistung.getFullId()).validateLanguageCode("en");

        FormsValidator formsValidator2 = new FormsValidator(response2);
        formsValidator2
            .validateCount(1)
            .validateId(0, formular1.getFullId())
            .validateName(0, "Form A English")
            .validateDescription(0, "Description A English")
            .validateUri(0, FORM_URI);
    }

    @Test
    @DisplayName("Bilinguales Formular - Beide Sprachen liefern korrekten Inhalt")
    void bilingualFormBeideSprachen(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).fo(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Formular formular = file.getFormulare().getFirst();
        formular.setBezeichnung("Antrag auf Förderung");
        formular.setBeschreibung("Hier beantragen Sie eine Förderung.");
        formular.setBezeichnungEnglisch("Application for Funding");
        formular.setBeschreibungEnglisch("Apply for funding here.");
        formular.getLink().setUri(FORM_URI);

        String ars = file.getZustaendigkeitenFormular().getFirst().getArss().getFirst();
        ImportUtil.pushAndWait(file);

        // Deutsche Anfrage
        Response responseDe = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_DE
        );

        ServiceDescriptionValidator sdValidatorDe = new ServiceDescriptionValidator(responseDe);
        sdValidatorDe.validateId(leistung.getFullId()).validateLanguageCode("de");

        FormsValidator formsValidatorDe = new FormsValidator(responseDe);
        formsValidatorDe
            .validateCount(1)
            .validateId(0, formular.getFullId())
            .validateName(0, "Antrag auf Förderung")
            .validateDescription(0, "Hier beantragen Sie eine Förderung.")
            .validateUri(0, FORM_URI);

        // Englische Anfrage
        Response responseEn = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        ServiceDescriptionValidator sdValidatorEn = new ServiceDescriptionValidator(responseEn);
        sdValidatorEn.validateId(leistung.getFullId()).validateLanguageCode("en");

        FormsValidator formsValidatorEn = new FormsValidator(responseEn);
        formsValidatorEn
            .validateCount(1)
            .validateId(0, formular.getFullId())
            .validateName(0, "Application for Funding")
            .validateDescription(0, "Apply for funding here.")
            .validateUri(0, FORM_URI);
    }

    @Test
    @DisplayName("Nur deutsches Formular - Englische Anfrage liefert KEIN Formular (strikter Modus)")
    void nurDeutschKeinFallback(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).fo(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        // Nur deutsches Formular (kein Englisch)
        Formular formular = file.getFormulare().getFirst();
        formular.setBezeichnung("Meldebescheinigung");
        formular.getLink().setUri(FORM_URI);

        String ars = file.getZustaendigkeitenFormular().getFirst().getArss().getFirst();
        ImportUtil.pushAndWait(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        FormsValidator formsValidator = new FormsValidator(response);
        // Strikter Modus: Kein Fallback, also 0 Formulare
        formsValidator.validateCount(0);
    }

    @Test
    @DisplayName("Mehrere bilinguale Formulare - Alle in jeweiliger Sprache verfügbar")
    void mehrereBilingualeForms(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).fo(2).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        // Formular 1 - bilingual
        Formular formular1 = file.getFormulare().get(0);
        formular1.setBezeichnung("Formular A Deutsch");
        formular1.setBezeichnungEnglisch("Form A English");
        formular1.getLink().setUri(FORM_URI);

        // Formular 2 - bilingual
        Formular formular2 = file.getFormulare().get(1);
        formular2.setBezeichnung("Formular B Deutsch");
        formular2.setBezeichnungEnglisch("Form B English");
        formular2.getLink().setUri(FORM_URI_2);

        String ars = file.getZustaendigkeitenFormular().getFirst().getArss().getFirst();
        ImportUtil.pushAndWait(file);

        // Englische Anfrage
        Response responseEn = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        FormsValidator formsValidatorEn = new FormsValidator(responseEn);
        formsValidatorEn.validateCount(2).validateHasId(formular1.getFullId()).validateHasId(formular2.getFullId());

        // Deutsche Anfrage
        Response responseDe = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_DE
        );

        FormsValidator formsValidatorDe = new FormsValidator(responseDe);
        formsValidatorDe.validateCount(2).validateHasId(formular1.getFullId()).validateHasId(formular2.getFullId());
    }

    @Test
    @DisplayName("Gemischte Formulare - Nur Formulare mit angefragter Sprache werden geliefert")
    void gemischteFormulareNurPassendeSprache(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).fo(2).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        // Formular 1 - nur Deutsch
        Formular formularNurDeutsch = file.getFormulare().get(0);
        formularNurDeutsch.setBezeichnung("Nur Deutsch");
        formularNurDeutsch.getLink().setUri(FORM_URI);

        // Formular 2 - bilingual
        Formular formularBilingual = file.getFormulare().get(1);
        formularBilingual.setBezeichnung("Auch Englisch Deutsch");
        formularBilingual.setBezeichnungEnglisch("Also English");
        formularBilingual.getLink().setUri(FORM_URI_2);

        String ars = file.getZustaendigkeitenFormular().getFirst().getArss().getFirst();
        ImportUtil.pushAndWait(file);

        // Englische Anfrage: Nur das bilinguale Formular sollte zurückkommen
        Response responseEn = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        FormsValidator formsValidatorEn = new FormsValidator(responseEn);
        formsValidatorEn.validateCount(1).validateId(0, formularBilingual.getFullId()).validateName(0, "Also English");

        // Deutsche Anfrage: Beide Formulare sollten zurückkommen
        Response responseDe = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_DE
        );

        FormsValidator formsValidatorDe = new FormsValidator(responseDe);
        formsValidatorDe
            .validateCount(2)
            .validateHasId(formularNurDeutsch.getFullId())
            .validateHasId(formularBilingual.getFullId());
    }
}
