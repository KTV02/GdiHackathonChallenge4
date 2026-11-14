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

import static org.hamcrest.Matchers.equalTo;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.DynamischerParameter;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.Identifizierungsmittel;
import de.dataport.pvog.testdatengenerator.model.Kriterium;
import de.dataport.pvog.testdatengenerator.model.LeiKaTypisierung;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Rolle;
import de.dataport.pvog.testdatengenerator.model.Sprache;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.Versionsinformation;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.FormsValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.ServiceDescriptionValidator;

/**
 * Testziel: Hier soll geprüft werden ob die allgemeinen Daten der Leistung für die Detailsseite korrekt ausgegeben werden.
 */
@ExtendWith({ EnvironmentManagerExtension.class })
class ServiceDescriptionTest {

    @Test
    void basisData(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        BundeslandConfig bundeslandConfig = new BundeslandConfig(Bundesland.BY);

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        leistung.setReferenzLeiKa("99007005017000");
        leistung.setVersionsinformation(
            Versionsinformation.builder()
                .erstelldatum(LocalDateTime.of(2021, Month.MARCH, 11, 10, 59, 55))
                .bearbeitungsdatum(LocalDateTime.of(2021, Month.MARCH, 12, 10, 59, 55))
                .build()
        );
        leistung.setSdgInformationen(List.of("1030000"));
        leistung.setTypisierung(
            List.of(
                LeiKaTypisierung.builder().code("4").name("Regelungskompetenz auf Landesebene").build(),
                LeiKaTypisierung.builder().code("5").name("Regelungskompetenz auf kommunaler Ebene").build()
            )
        );

        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(3).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(6).build());
        leistung.setTextmodule(textmodule);

        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();
        oe.setTitel("Titel deutsch");
        Zustaendigkeit zustaendigkeit = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        zustaendigkeit.setRolle(List.of(Rolle.builder().code("01").build()));
        zustaendigkeit.setKriterien(List.of(Kriterium.builder().code("051").auspraegungen(List.of("b", "c")).build()));

        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName("Textmodul03")
            .validateExternalLastUpdate("2021-03-12T09:59:55Z")
            .validateLastUpdateNotEmpty()
            .validateLanguageCode("de")
            .validateOrganisationsCount(1)
            .validateOrganisationsId(0, file.getOrganisationseinheiten().getFirst().getFullId())
            .validateOrganisationsTitle(0, "Titel deutsch")
            .validateOrganisationsRole(0, "01", "Zuständige Stelle und Ansprechpunkt")
            .validateOrganisationsCriteria(
                0,
                List.of(
                    ServiceDescriptionValidator.Criterion.builder()
                        .code("051")
                        .name("Postleitzahl")
                        .auspraegungen(List.of("b", "c"))
                        .build()
                )
            )
            .validateArsCount(1)
            .validateArs(0, bundeslandConfig.getBasicArs())
            .validateLeikakeysCount(1)
            .validateLeikakeys(0, "99007005017000");
    }

    @Test
    void forms(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).fo(2).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Formular formular1 = file.getFormulare().getFirst();
        formular1.setBezeichnung("formular-1-bezeichnung");
        formular1.setBeschreibung("formular-1-beschreibung");
        formular1.getLink().setUri("formular-1-uri");
        Formular formular2 = file.getFormulare().get(1);
        formular2.setBezeichnung("formular-2-bezeichnung");
        formular2.setBeschreibung("formular-2-beschreibung");
        formular2.getLink().setUri("formular-2-uri");
        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenFormular().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId());
        FormsValidator formsValidator = new FormsValidator(response);
        formsValidator
            .validateCount(2)
            .validateUri(0, "formular-1-uri")
            .validateName(0, "formular-1-bezeichnung")
            .validateDescription(0, "formular-1-beschreibung")
            .validateUri(1, "formular-2-uri")
            .validateName(1, "formular-2-bezeichnung")
            .validateDescription(1, "formular-2-beschreibung");
    }

    @Test
    void onlineservices(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung("SAMPLE Bezeichnung OD01");
        onlinedienst.setBeschreibung("SAMPLE Beschreibung OD01");
        onlinedienst.setLinks(
            List.of(
                Link.builder()
                    .uri("https://sample-od01/?q=OD_01_uri")
                    .dynamischeParameter(
                        List.of(DynamischerParameter.builder().typ("001").parameterName("leistung").build())
                    )
                    .titel("SAMPLE Titel OD01")
                    .beschreibung("SAMPLE Beschreibung OD01")
                    .positionDarstellung(1010)
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
        onlinedienst.setVertrauensniveau("10");
        onlinedienst.setZahlungsweisen(List.of("1000000", "3040000"));
        onlinedienst.setIdentifizierungsmittel(
            List.of(Identifizierungsmittel.builder().code(String.valueOf(1103)).build())
        );
        onlinedienst.setHilfeText("SAMPLE Hilfetext");
        onlinedienst.setHilfeLinks(
            List.of(
                Link.builder()
                    .uri("http://www.hilfelink_od01.de/")
                    .titel("SAMPLE Hilfelink OD01")
                    .beschreibung("SAMPLE Beschreibung Hilfelink")
                    .positionDarstellung(0)
                    .build()
            )
        );

        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());
        OnlineServiceLinkValidator validator = new OnlineServiceLinkValidator(response);
        validator
            .validateCount(1)
            .validateId(0, onlinedienst.getFullId())
            .validateTitle(0, "SAMPLE Titel OD01")
            .validateDescription(0, "SAMPLE Beschreibung OD01")
            .validateUri(0, "https://sample-od01/?q=OD_01_uri&leistung=" + leistung.getId())
            .validateStatus(0, "UNDEFINED")
            .validateExecutionLanguagesCount(0, 2)
            .validateExecutionLanguagesContains(0, "de")
            .validateExecutionLanguagesContains(0, "en")
            .validateExecutionNationalitiesCount(0, 3)
            .validateExecutionNationalitiesContains(0, "000")
            .validateExecutionNationalitiesContains(0, "168")
            .validateExecutionNationalitiesContains(0, "126");
    }

    @Test
    void dynamischerLink(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();

        String onlinedienstUrl = "https://sample-od01/?gebiet_id=null";
        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.setLinks(
            List.of(
                Link.builder()
                    .uri(onlinedienstUrl)
                    .dynamischeParameter(
                        List.of(
                            DynamischerParameter.builder().typ("001").parameterName("anliegen_id").build(),
                            DynamischerParameter.builder().typ("002").parameterName("gebiet_id").build(),
                            DynamischerParameter.builder().typ("003").parameterName("oe_id_dynamisch").build()
                        )
                    )
                    .titel("SAMPLE Titel OD01")
                    .beschreibung("SAMPLE Beschreibung OD01")
                    .positionDarstellung(1010)
                    .build()
            )
        );

        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());
        OnlineServiceLinkValidator validator = new OnlineServiceLinkValidator(response);
        validator
            .validateCount(1)
            .validateId(0, onlinedienst.getFullId())
            .validateTitle(0, "SAMPLE Titel OD01")
            .validateDescription(0, "SAMPLE Beschreibung OD01")
            .validateUri(
                0,
                onlinedienstUrl +
                    "&gebiet_id=" +
                    ars +
                    "&anliegen_id=" +
                    leistung.getId() +
                    "&oe_id_dynamisch=" +
                    oe.getId()
            );
        // expected: https://sample-od01/?gebiet_id=nullgebiet_id095640000000oe_id_dynamischL100042.OE.OE_202405022153-dynamischerLink_0001"
        // actual:   https://sample-od01/?gebiet_id=null&gebiet_id=095640000000&anliegen_id=L100042.LB.LB_202405022153-dynamischerLink_0001&oe_id_dynamisch=L100042.OE.OE_202405022153-dynamischerLink_0001
        // e: anliegen_idL100042.LB.LB_202405022153-dynamischerLink_0001
        // a: anliegen_id=L100042.LB.LB_202405022153-dynamischerLink_0001
    }

    @Test
    void dataV2TitleToDescFallback(TestInfo testInfo) {
        String testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        var file = XzufiFileCreator.newInstance().name(testcaseName).lb(1).od(1).getMin();
        var leistung = file.getLeistungsbeschreibungen().getFirst();
        var ars = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();

        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung("SAMPLE Beschreibung OD01");
        onlinedienst.setLinks(
            List.of(Link.builder().uri("https://sample-od01/?q=OD_01_uri").positionDarstellung(0).build())
        );

        ImportUtil.pushAndWait(file);
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        new OnlineServiceLinkValidator(response)
            .validateCount(1)
            .validateUri(0, "https://sample-od01/?q=OD_01_uri")
            .validateTitle(0, "SAMPLE Beschreibung OD01")
            .validateContainsNoDescription(0);
    }

    @Test
    void dataV2TitleToUriFallback(TestInfo testInfo) {
        String testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        var file = XzufiFileCreator.newInstance().name(testcaseName).lb(1).od(1).getMin();
        var leistung = file.getLeistungsbeschreibungen().getFirst();
        var ars = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();

        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        //URI wird genommen, wenn Titel vom Onlinedienstlink und Bezeichnung leer ist
        onlinedienst.setBezeichnung("");
        onlinedienst.setLinks(
            List.of(Link.builder().uri("https://sample-od01/?q=OD_01_uri").positionDarstellung(0).build())
        );

        ImportUtil.pushAndWait(file);
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        new OnlineServiceLinkValidator(response)
            .validateCount(1)
            .validateUri(0, "https://sample-od01/?q=OD_01_uri")
            .validateTitle(0, "https://sample-od01/?q=OD_01_uri")
            .validateContainsNoDescription(0);
    }

    @Test
    void onlineservicesEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnungEnglisch("SAMPLE Definition OD01 EN");
        onlinedienst.setBeschreibungEnglisch("SAMPLE description OD01 EN");
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
        onlinedienst.setVertrauensniveau("10");
        onlinedienst.setZahlungsweisen(List.of("1000000", "3040000"));
        onlinedienst.setIdentifizierungsmittel(
            List.of(Identifizierungsmittel.builder().code(String.valueOf(1103)).build())
        );
        onlinedienst.setHilfeText("SAMPLE Hilfetext");
        onlinedienst.setHilfeTextEnglisch("SAMPLE Helptext EN");
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

        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        OnlineServiceLinkValidator validator = new OnlineServiceLinkValidator(response);
        validator
            .validateCount(1)
            .validateId(0, onlinedienst.getFullId())
            .validateTitle(0, "OD01_Title")
            .validateDescription(0, "OD01_Description")
            .validateUri(0, "http://od01.de/en?ars_en=" + leistung.getId())
            .validateExecutionLanguagesCount(0, 2)
            .validateExecutionLanguagesContains(0, "de")
            .validateExecutionLanguagesContains(0, "en")
            .validateExecutionNationalitiesCount(0, 3)
            .validateExecutionNationalitiesContains(0, "000")
            .validateExecutionNationalitiesContains(0, "168")
            .validateExecutionNationalitiesContains(0, "126");
    }

    @Test
    void dataV2TitleToDescFallbackEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnungEnglisch("SAMPLE Definition OD01 EN");
        onlinedienst.setBeschreibungEnglisch("SAMPLE description OD01 EN");
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
        onlinedienst.setVertrauensniveau("10");
        onlinedienst.setZahlungsweisen(List.of("1000000", "3040000"));
        onlinedienst.setIdentifizierungsmittel(
            List.of(Identifizierungsmittel.builder().code(String.valueOf(1103)).build())
        );
        onlinedienst.setHilfeText("SAMPLE Hilfetext");
        onlinedienst.setHilfeTextEnglisch("SAMPLE Helptext EN");
        onlinedienst.setHilfeLinks(
            List.of(
                Link.builder()
                    .uri("http://www.hilfelink_od01.de/")
                    .uriEnglisch("http://www.helplink_od01.en/")
                    .beschreibung("SAMPLE Beschreibung Hilfelink")
                    .beschreibungEnglisch("SAMPLE Description Hilfelink EN")
                    .positionDarstellung(0)
                    .positionDarstellungEnglisch(1)
                    .build()
            )
        );

        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        OnlineServiceLinkValidator validator = new OnlineServiceLinkValidator(response);
        validator
            .validateCount(1)
            .validateId(0, onlinedienst.getFullId())
            .validateTitle(0, onlinedienst.getBezeichnungEnglisch())
            .validateDescription(0, "OD01_Description");
    }

    @Disabled("Für EN noch nicht umgesetzt")
    @Test
    void dataV2TitleToUriFallbackEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.setBeschreibungEnglisch(null);
        onlinedienst.setBezeichnungEnglisch(null);
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
                    .beschreibung("SAMPLE Beschreibung OD01")
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
        onlinedienst.setVertrauensniveau("10");
        onlinedienst.setZahlungsweisen(List.of("1000000", "3040000"));
        onlinedienst.setIdentifizierungsmittel(
            List.of(Identifizierungsmittel.builder().code(String.valueOf(1103)).build())
        );
        onlinedienst.setHilfeText("SAMPLE Hilfetext");
        onlinedienst.setHilfeTextEnglisch("SAMPLE Helptext EN");
        onlinedienst.setHilfeLinks(
            List.of(
                Link.builder()
                    .uri("http://www.hilfelink_od01.de/")
                    .uriEnglisch("http://www.helplink_od01.en/")
                    .beschreibung("SAMPLE Beschreibung Hilfelink")
                    .beschreibungEnglisch("SAMPLE Description Hilfelink EN")
                    .positionDarstellung(0)
                    .positionDarstellungEnglisch(1)
                    .build()
            )
        );

        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        OnlineServiceLinkValidator validator = new OnlineServiceLinkValidator(response);
        validator
            .validateCount(1)
            .validateId(0, onlinedienst.getFullId())
            .validateTitle(0, "http://od01.de/en")
            .validateContainsNoDescription(0);
    }

    /* Leistung darf nur ausgegeben werden, wenn sie einer OD/OE zugeordnet ist */
    @Test
    void keineAusgabeVonLBohneOEoderOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        BundeslandConfig bundesland = new BundeslandConfig(Bundesland.BY);
        String arsErding = "091770117117"; // Erding
        String arsDorfen = "091770115115"; // Dorfen

        XzufiFile file = XzufiFileCreator.newInstance(bundesland).name(testcasename).lb(1).oe(1).sp(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        // ARS setzen
        file.getZustaendigkeitenOrganisationseinheit()
            .getFirst()
            .setArss(new ArrayList<>(List.of(arsErding)));
        file.getSpezialisierungen().getFirst().setArs(List.of(arsErding, arsDorfen));

        ImportUtil.pushAndWait(file);

        Response responseErding = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            arsErding,
            leistung.getFullId()
        );
        Response responseDorfen = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            arsDorfen,
            leistung.getFullId()
        );

        // Validieren: Spezialisierte Leistung wird nicht ausgegeben
        ServiceDescriptionValidator validatorErding = new ServiceDescriptionValidator(responseErding);
        validatorErding.validateArsCount(1).validateId(leistung.getFullId());
        responseDorfen
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND)
            .contentType(ContentType.JSON)
            .body("message", equalTo("Die angegebene Resource konnte nicht gefunden werden."));

        // Treatment: OE-Zuständigkeit erweitern mit ARS
        XzufiFile updateOE = new XzufiFile();
        updateOE.getOrganisationseinheiten().add(file.getOrganisationseinheiten().getFirst());
        Zustaendigkeit zsoe = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        zsoe.getArss().add(arsDorfen);
        updateOE.addBusinessobjects(zsoe);

        ImportUtil.pushAndWait(updateOE);

        // Validieren: Spezialisierte Leistung wird ausgegeben
        responseDorfen = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(arsDorfen, leistung.getFullId());
        ServiceDescriptionValidator validatorDorfen = new ServiceDescriptionValidator(responseDorfen);
        validatorDorfen.validateArsCount(1).validateId(leistung.getFullId());
    }
}
