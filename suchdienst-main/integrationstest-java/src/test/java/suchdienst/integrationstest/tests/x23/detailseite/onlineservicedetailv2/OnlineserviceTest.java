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
package suchdienst.integrationstest.tests.x23.detailseite.onlineservicedetailv2;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.DynamischerParameter;
import de.dataport.pvog.testdatengenerator.model.Identifizierungsmittel;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Sprache;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OnlineserviceConnector;
import suchdienst.integrationstest.validator.onlineservicedetailv2.HelpLinkValidator;
import suchdienst.integrationstest.validator.onlineservicedetailv2.LinksValidator;
import suchdienst.integrationstest.validator.onlineservicedetailv2.OnlineserviceValidator;
import suchdienst.integrationstest.validator.onlineservicedetailv2.RelationsValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class OnlineserviceTest {

    @Test
    void dataV2(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Zustaendigkeit zustaendigkeit = file.getZustaendigkeitenOnlinedienst().getFirst();
        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung("SAMPLE Bezeichnung OD01");
        onlinedienst.setBezeichnungEnglisch("SAMPLE Definition OD01 EN");
        onlinedienst.setKurzbezeichnung("SAMPLE Kurzbezeichnung OD01");
        onlinedienst.setKurzbezeichnungEnglisch("Short description OD01 EN");
        onlinedienst.setBeschreibung("SAMPLE Beschreibung OD01");
        onlinedienst.setBeschreibungEnglisch("SAMPLE description OD01 EN");
        onlinedienst.setTeaser("SAMPLE Teaser OD01");
        onlinedienst.setTeaserEnglisch("SAMPLE Teaser OD01 EN");
        onlinedienst.setLogo("https://my-logo.org");
        onlinedienst.setLinks(
            List.of(
                Link.builder()
                    .uri("https://sample-od01/?q=OD_01_uri")
                    .uriEnglisch("http://od01.de/en")
                    .dynamischeParameter(
                        List.of(
                            DynamischerParameter.builder()
                                .typ("001")
                                .parameterName("ars")
                                .parameterNameEnglisch("ars_en")
                                .build()
                        )
                    )
                    .titel("SAMPLE Titel OD01")
                    .titelEnglisch("OD01_Title")
                    .beschreibung("SAMPLE Beschreibung OD01")
                    .beschreibungEnglisch("OD01_Description")
                    .positionDarstellung(1010)
                    .positionDarstellungEnglisch(1)
                    .build()
            )
        );
        onlinedienst.setDurchfuehrungStaatsangehoerigkeit(List.of("000", "126", "168"));
        onlinedienst.setDurchfuehrungSprachen(
            List.of(
                Sprache.builder().code("de").bezeichnungDeutsch("deutsch").bezeichnungNativ("Deutsch").build(),
                Sprache.builder().code("en").bezeichnungDeutsch("englisch").bezeichnungNativ("English").build()
            )
        );
        onlinedienst.setZahlungsweisen(List.of("1000000", "3040000"));
        onlinedienst.setVertrauensniveau("10");
        onlinedienst.setIdentifizierungsmittel(
            List.of(Identifizierungsmittel.builder().code(String.valueOf(1103)).build())
        );

        onlinedienst.setHilfeLinks(
            List.of(
                Link.builder()
                    .uri("http://www.hilfelink_od01.de/")
                    .uriEnglisch("http://www.helplink_od01.en/")
                    .titel("SAMPLE Hilfelink OD01")
                    .titelEnglisch("SAMPLE helplink OD01 EN")
                    .beschreibung("SAMPLE Beschreibung Hilfelink")
                    .beschreibungEnglisch("SAMPLE Description Hilfelink EN")
                    .positionDarstellung(0)
                    .positionDarstellungEnglisch(1)
                    .build()
            )
        );
        onlinedienst.setHilfeText("SAMPLE Hilfetext");
        onlinedienst.setHilfeTextEnglisch("SAMPLE Helptext EN");

        // Daten einspielen und vom SuD abrufen
        ImportUtil.pushAndWait23(file);
        Response response = OnlineserviceConnector.getOnlineserviceDetailV2(onlinedienst.getFullId());

        OnlineserviceValidator validator = new OnlineserviceValidator(response);
        validator
            .validateName("SAMPLE Bezeichnung OD01")
            .validateDescription("SAMPLE Beschreibung OD01")
            .validateHelpText("SAMPLE Hilfetext")
            .validatePaymentMethodCount(2)
            .validatePaymentMethodContains("Bargeldzahlung")
            .validatePaymentMethodContains("Rechnung")
            .validateLevelConfidence("10")
            .validateExecutionLanguagesCount(2)
            .validateExecutionLanguagesContains("de")
            .validateExecutionLanguagesContains("en")
            .validateLanguagesCount(2)
            .validateLanguagesContains("de")
            .validateLanguagesContains("en")
            .validateLanguageComplete("en", true)
            .validateExecutionNationalitiesCount(3)
            .validateExecutionNationalitiesContains("000")
            .validateExecutionNationalitiesContains("168")
            .validateExecutionNationalitiesContains("126")
            .validateIdentificationMethodsContains(
                "Elektronische Identifizierung mittels nationalen eID Mittel - Benutzername/Passwort"
            )
            .validateLogo("https://my-logo.org");

        HelpLinkValidator helpLinkValidator = new HelpLinkValidator(response);
        helpLinkValidator
            .validateCount(1)
            .validateUri(0, "http://www.hilfelink_od01.de/")
            .validateTitle(0, "SAMPLE Hilfelink OD01")
            .validateDescription(0, "SAMPLE Beschreibung Hilfelink")
            // known issue PV-13064 .validateReplace(0, false)
            .validateDynamicParametersCount(0, 0);

        LinksValidator linksValidator = new LinksValidator(response);
        linksValidator
            .validateCount(1)
            .validateUri(0, "https://sample-od01/?q=OD_01_uri")
            .validateTitle(0, "SAMPLE Titel OD01")
            .validateDescription(0, "SAMPLE Beschreibung OD01")
            // known issue PV-13064 .validateReplace(0, false)
            .validateDynamicParametersCount(0, 1)
            .validateDynamicParametersTypCode(0, 0, "001")
            .validateDynamicParametersParameterName(0, 0, "ars");
        RelationsValidator relationsValidator = new RelationsValidator(response);
        String zsid =
            onlinedienst.getSchemeAgencyId() +
            ".ZSOD." +
            leistung.getId() +
            "." +
            onlinedienst.getId() +
            "." +
            zustaendigkeit.getArss().getFirst();
        relationsValidator
            .validateCount(1)
            .validateRelationId(0, zsid)
            .validateLbId(0, leistung.getFullId())
            .validateArs(0, zustaendigkeit.getArss().getFirst())
            .validateParentId(0, onlinedienst.getFullId())
            .validateTyp(0, "ONLINESERVICE");
    }

    @Test
    void dataEnglischV2(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Zustaendigkeit zustaendigkeit = file.getZustaendigkeitenOnlinedienst().getFirst();
        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnungEnglisch("SAMPLE Definition OD01 EN");
        onlinedienst.setKurzbezeichnungEnglisch("Short description OD01 EN");
        onlinedienst.setBeschreibungEnglisch("SAMPLE description OD01 EN");
        onlinedienst.setTeaserEnglisch("SAMPLE Teaser OD01 EN");
        onlinedienst.setLinks(
            List.of(
                Link.builder()
                    .uri("https://sample-od01/?q=OD_01_uri")
                    .uriEnglisch("http://od01.de/en")
                    .dynamischeParameter(
                        List.of(
                            DynamischerParameter.builder()
                                .typ("001")
                                .parameterName("ars")
                                .parameterNameEnglisch("ars_en")
                                .build()
                        )
                    )
                    .titel("SAMPLE Titel OD01")
                    .titelEnglisch("OD01_Title")
                    .beschreibung("SAMPLE Beschreibung OD01")
                    .beschreibungEnglisch("OD01_Description")
                    .positionDarstellung(1010)
                    .positionDarstellungEnglisch(1)
                    .build()
            )
        );
        onlinedienst.setDurchfuehrungStaatsangehoerigkeit(List.of("000", "126", "168"));
        onlinedienst.setDurchfuehrungSprachen(
            List.of(
                Sprache.builder().code("de").bezeichnungDeutsch("deutsch").bezeichnungNativ("Deutsch").build(),
                Sprache.builder().code("en").bezeichnungDeutsch("englisch").bezeichnungNativ("English").build()
            )
        );
        onlinedienst.setZahlungsweisen(List.of("1000000", "3040000"));
        onlinedienst.setVertrauensniveau("10");
        onlinedienst.setIdentifizierungsmittel(
            List.of(Identifizierungsmittel.builder().code(String.valueOf(1103)).build())
        );
        onlinedienst.setHilfeLinks(
            List.of(
                Link.builder()
                    .uri("http://www.hilfelink_od01.de/")
                    .uriEnglisch("http://www.helplink_od01.en/")
                    .titel("SAMPLE Hilfelink OD01")
                    .titelEnglisch("SAMPLE helplink OD01 EN")
                    .beschreibung("SAMPLE Beschreibung Hilfelink")
                    .beschreibungEnglisch("SAMPLE Description Hilfelink EN")
                    .positionDarstellung(0)
                    .positionDarstellungEnglisch(1)
                    .build()
            )
        );
        onlinedienst.setHilfeText("SAMPLE Hilfetext");
        onlinedienst.setHilfeTextEnglisch("SAMPLE Helptext EN");

        // Daten einspielen und vom SuD abrufen
        ImportUtil.pushAndWait23(file);
        Response response = OnlineserviceConnector.getOnlineserviceDetailV2(
            onlinedienst.getFullId(),
            Constants.LANGUAGE_EN
        );

        OnlineserviceValidator validator = new OnlineserviceValidator(response);
        validator
            .validateName("SAMPLE Definition OD01 EN")
            .validateDescription("SAMPLE description OD01 EN")
            .validateHelpText("SAMPLE Helptext EN")
            .validatePaymentMethodCount(2)
            .validatePaymentMethodContains("Cash payment")
            .validatePaymentMethodContains("Bill")
            .validateLevelConfidence("10")
            .validateExecutionLanguagesCount(2)
            .validateExecutionLanguagesContains("de")
            .validateExecutionLanguagesContains("en")
            .validateLanguagesCount(2)
            .validateLanguagesContains("de")
            .validateLanguagesContains("en")
            .validateLanguageComplete("en", true)
            .validateExecutionNationalitiesCount(3)
            .validateExecutionNationalitiesContains("000")
            .validateExecutionNationalitiesContains("168")
            .validateExecutionNationalitiesContains("126")
            .validateIdentificationMethodsContains(
                "electronic identification using national eID means – user name/password"
            );

        HelpLinkValidator helpLinkValidator = new HelpLinkValidator(response);
        helpLinkValidator
            .validateCount(1)
            .validateUri(0, "http://www.helplink_od01.en/")
            .validateTitle(0, "SAMPLE helplink OD01 EN")
            .validateDescription(0, "SAMPLE Description Hilfelink EN")
            // known issue PV-13064 .validateReplace(0, false)
            .validateDynamicParametersCount(0, 0);

        LinksValidator linksValidator = new LinksValidator(response);
        linksValidator
            .validateCount(1)
            .validateUri(0, "http://od01.de/en")
            .validateTitle(0, "OD01_Title")
            .validateDescription(0, "OD01_Description")
            // known issue PV-13064 .validateReplace(0, false)
            .validateDynamicParametersCount(0, 1)
            .validateDynamicParametersTypCode(0, 0, "001")
            .validateDynamicParametersParameterName(0, 0, "ars_en");
        RelationsValidator relationsValidator = new RelationsValidator(response);
        String zsid =
            onlinedienst.getSchemeAgencyId() +
            ".ZSOD." +
            leistung.getId() +
            "." +
            onlinedienst.getId() +
            "." +
            zustaendigkeit.getArss().getFirst();
        relationsValidator
            .validateCount(1)
            .validateRelationId(0, zsid)
            .validateLbId(0, leistung.getFullId())
            .validateArs(0, zustaendigkeit.getArss().getFirst())
            .validateParentId(0, onlinedienst.getFullId())
            .validateTyp(0, "ONLINESERVICE");
    }

    @Test
    void detailRelations(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Erzeuge Testdaten: Ein Onlinedienst der Zuständig ist für zwei Leistungen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(2).od(1).getMax();
        Leistungsbeschreibung lb1 = file.getLeistungsbeschreibungen().get(0);
        Leistungsbeschreibung lb2 = file.getLeistungsbeschreibungen().get(1);

        Onlinedienst od = file.getOnlinedienste().getFirst();
        Zustaendigkeit zsOd1 = file.getZustaendigkeitenOnlinedienst().get(0);
        Zustaendigkeit zsOd2 = file.getZustaendigkeitenOnlinedienst().get(1);

        String zsId1 =
            od.getSchemeAgencyId() + ".ZSOD." + lb1.getId() + "." + od.getId() + "." + zsOd1.getArss().getFirst();
        String zsId2 =
            od.getSchemeAgencyId() + ".ZSOD." + lb2.getId() + "." + od.getId() + "." + zsOd2.getArss().getFirst();

        // Daten einspielen
        ImportUtil.pushAndWait23(file);

        Response response = OnlineserviceConnector.getOnlineserviceDetailV2(od.getFullId(), Constants.LANGUAGE_DE);

        RelationsValidator relationsValidator = new RelationsValidator(response);

        relationsValidator
            .validateCount(2)
            .validateRelationId(0, zsId1)
            .validateLbId(0, lb1.getFullId())
            .validateArs(0, zsOd1.getArss().getFirst())
            .validateParentId(0, od.getFullId())
            .validateTyp(0, "ONLINESERVICE")
            .validateRelationId(1, zsId2)
            .validateLbId(1, lb2.getFullId())
            .validateArs(1, zsOd2.getArss().getFirst())
            .validateParentId(1, od.getFullId())
            .validateTyp(1, "ONLINESERVICE");
    }
}
