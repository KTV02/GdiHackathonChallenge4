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
package suchdienst.integrationstest.tests.x23.detailseite.organisationunitsdetailv5;

import de.dataport.pvog.testdatengenerator.creator.CreatorConstants;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.FoCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsFormularCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsKontaktpersonCreator;
import de.dataport.pvog.testdatengenerator.model.Anschrift;
import de.dataport.pvog.testdatengenerator.model.Bankverbindung;
import de.dataport.pvog.testdatengenerator.model.Erreichbarkeit;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.GeoPoint;
import de.dataport.pvog.testdatengenerator.model.Herausgeber;
import de.dataport.pvog.testdatengenerator.model.Kommunikationssystem;
import de.dataport.pvog.testdatengenerator.model.Kontaktperson;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zeitraum;
import de.dataport.pvog.testdatengenerator.model.Zeitserie;
import de.dataport.pvog.testdatengenerator.model.Zeitserien;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.BankAccountValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.CommunicationSytemsValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.EmployeeValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.InfoOtherValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.InternetAddressesValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.NestedFormsValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.OeffnungszeitValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.OrganisationValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.PublisherValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.location.AddressValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.location.CommunicationValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.location.InformationValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.location.UrlValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class OrganisationUnitTest {

    @Test
    void daten(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        organisationseinheit.setTitel("Title der Orga");
        organisationseinheit.setBeschreibung("Beschreibung der Orga");
        organisationseinheit.setKurzbeschreibung("OE Kurzbeschreibung");
        organisationseinheit.getInfoOeffnungszeitenText().add("Oeffnungszeitentest");
        organisationseinheit.setSonstiges("Sonstige Informationen");
        organisationseinheit.getInfoOeffnungszeitenStrukturiert().clear();
        organisationseinheit.getInfoOeffnungszeitenStrukturiert().add(
            Zeitserien.builder()
                .type("07")
                .hinweistext("Hinweistext")
                .gueltigkeit(
                    List.of(
                        Zeitraum.builder()
                            .beginn(LocalDate.of(2019, 8, 13))
                            .ende(LocalDate.of(2022, 8, 13))
                            .zusatz("GZusatz OE")
                            .build()
                    )
                )
                .regulaereZeiten(
                    List.of(
                        Zeitserie.builder()
                            .tagesposition("01")
                            .tagestyp("09")
                            .beginn(LocalTime.of(9, 30, 0))
                            .ende(LocalTime.of(17, 0, 0))
                            .zusatz("Regulaere Zeiten")
                            .build()
                    )
                )
                .abweichendeZeiten(
                    List.of(
                        Zeitraum.builder()
                            .beginn(LocalDate.of(2019, 8, 13))
                            .ende(LocalDate.of(2030, 8, 13))
                            .zusatz("Abweichende Zeiten")
                            .build()
                    )
                )
                .build()
        );
        organisationseinheit.setInternetadressen(
            Link.builder()
                .uri("http://www.oeadresse.de/")
                .titel("Titel OE internet Adresse")
                .beschreibung("Beschreibung OE Internetadresse")
                .positionDarstellung(0)
                .build()
        );

        organisationseinheit.getBankverbindungen().clear();
        organisationseinheit
            .getBankverbindungen()
            .add(
                Bankverbindung.builder()
                    .empfaenger("Der Gemeindevorstand")
                    .iban("DE25501900000300336027")
                    .bic("FFVBDEFFXXX")
                    .bankinstitut("Frankfurter Volksbank e.G.")
                    .build()
            );

        organisationseinheit.getAnschriften().clear();
        organisationseinheit.getAnschriften().add(
            Anschrift.builder()
                .typ("001")
                .strasse("Billstraße")
                .hausnummer("82")
                .postleitzahl("20539")
                .ort("Hamburg")
                .zusatz("Kein Zusatz")
                .geoPoints(
                    List.of(GeoPoint.builder().srsName("EPSG:31468").values(List.of(4458169.59, 5403691.43)).build())
                )
                .infoParkplatz("Wenig Parkplätze")
                .infoOEPNV("Gut erreichbar")
                .infoBarrierefreiheit("Barrierearm")
                .kennzeichenAufzug(true)
                .kennzeichenRollstuhlgerecht(true)
                .build()
        );
        var zsoe = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        zsoe.getKommunikationssysteme().add(
            Kommunikationssystem.builder()
                .code("002")
                .codeName("Email")
                .listUri("urn:xoev-de:fim:codeliste:xzufi.kommunikationssystemtyp")
                .kennung("bla@bla.de")
                .zusatz("Kennungszusatz Email")
                .gueltigkeit(
                    List.of(
                        Zeitraum.builder()
                            .beginn(LocalDate.of(2019, 8, 13))
                            .ende(LocalDate.of(2024, 8, 13))
                            .build()
                    )
                )
                .build()
        );

        organisationseinheit.setHerausgeber(
            Herausgeber.builder()
                .beschreibung("HerausgeberBeschreibung")
                .bezeichnung("HerausgeberBezeichnung")
                .kurzbezeichnung("HerausgeberKurzbezeichnung")
                .build()
        );

        Kontaktperson kontaktperson = Kontaktperson.builder()
            .id("KP_1")
            .schemeAgencyId(CreatorConstants.DEFAULT_BUNDESLANDCONFIG.getSchemeAgencyId())
            .vorname("Max")
            .familienname("Bendler")
            .raum("5")
            .herausgeber(
                Herausgeber.builder()
                    .id("HerausgeberID")
                    .beschreibung("HerausgeberBeschreibung")
                    .bezeichnung("HerausgeberBezeichnung")
                    .kurzbezeichnung("HerausgeberKurzbezeichnung")
                    .build()
            )
            .erreichbarkeiten(
                List.of(
                    Erreichbarkeit.builder().code("01").kennung("max.bendler@pvog.de").build(),
                    Erreichbarkeit.builder().code("02").kennung("111222333").build(),
                    Erreichbarkeit.builder().code("03").kennung("123456789").build(),
                    Erreichbarkeit.builder().code("04").kennung("987654321").build()
                )
            )
            .build();
        organisationseinheit.getKontaktpersonen().add(kontaktperson);
        Zustaendigkeit zustaendigkeit = ZsKontaktpersonCreator.getInstance().getMin(
            leistung.getId(),
            kontaktperson.getId()
        );
        file.getZustaendigkeitenKontaktperson().add(zustaendigkeit);

        // Formulare Bauen
        Formular nestedForm = FoCreator.getInstance().getMin(testcasename);
        nestedForm.setLink(Link.builder().uri("https://gidf.de/fo_full_orga_data").build());
        nestedForm.setBezeichnung("Name FO_full_orga_data");
        nestedForm.setBeschreibung("Beschreibung FO_full_orga_data");
        nestedForm.setMimeCode("MimeCode FO_full_orga_data");
        Zustaendigkeit zsNestedForm = ZsFormularCreator.getInstance().getMin(leistung.getId(), nestedForm.getId());
        nestedForm.getZustaendigkeiten().add(zsNestedForm);
        organisationseinheit.getFormulare().add(nestedForm);

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        String ars = zustaendigkeit.getArss().getFirst();
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            ars,
            leistung.getFullId()
        );

        NestedFormsValidator nestedFormsValidator = new NestedFormsValidator(response);
        nestedFormsValidator
            .validateCount(1)
            .validateId(0, nestedForm.getFullId())
            .validateDescription(0, "Beschreibung FO_full_orga_data")
            .validateName(0, "Name FO_full_orga_data")
            .validateUri(0, "https://gidf.de/fo_full_orga_data")
            .validateMimeCode(0, "MimeCode FO_full_orga_data");

        //Validierung
        OrganisationValidator organisationValidator = new OrganisationValidator(response);
        organisationValidator
            .validateTitle("Title der Orga")
            .validateDescription("Beschreibung der Orga")
            .validateShortDescription("OE Kurzbeschreibung");

        BankAccountValidator bankAccountValidator = new BankAccountValidator(response);
        bankAccountValidator
            .validateCount(1)
            .validateReceiver(0, "Der Gemeindevorstand")
            .validateIban(0, "DE25501900000300336027")
            .validateBankName(0, "Frankfurter Volksbank e.G.")
            .validateBic(0, "FFVBDEFFXXX");

        UrlValidator urlValidator = new UrlValidator(response);
        urlValidator.validateCount(0);

        InformationValidator informationValidator = new InformationValidator(response);
        informationValidator.validateCount(0);

        AddressValidator addressValidator = new AddressValidator(response);
        addressValidator
            .validateCount(1)
            .validateCode(0, "001")
            .validateCity(0, "Hamburg")
            .validateHouseNumber(0, "82")
            .validateType(0, "Hausanschrift")
            .validateZip(0, "20539")
            .validateStreet(0, "Billstraße")
            .validateAdditional(0, "Kein Zusatz")
            .validateAccessible(0, "Barrierearm")
            .validateOepnv(0, "Gut erreichbar")
            .validateParking(0, "Wenig Parkplätze")
            .validateWheelchairAccessible(0, true)
            .validateLift(0, true);

        CommunicationValidator communicationValidator = new CommunicationValidator(response);
        communicationValidator.validateCount(0);

        EmployeeValidator employeeValidator = new EmployeeValidator(response);
        employeeValidator
            .validateCount(1)
            .validateName(0, "Max")
            .validateLastname(0, "Bendler")
            .validateRoom(0, "5")
            .validateAdditionalLinksCount(0, 0)
            .validateLocationUrlCount(0, 0)
            .validateLocationInformationCount(0, 0)
            .validateLocationAddressCount(0, 0)
            .validateLocationCommunicationCount(0, 4)
            .validateLocationCommunicationCode(0, 0, "01")
            .validateLocationCommunicationName(0, 0, "E-Mail")
            .validateLocationCommunicationValue(0, 0, "max.bendler@pvog.de")
            .validateLocationCommunicationAdditional(0, 0, "")
            .validateLocationCommunicationCode(0, 1, "02")
            .validateLocationCommunicationName(0, 1, "Telefon Festnetz")
            .validateLocationCommunicationValue(0, 1, "111222333")
            .validateLocationCommunicationAdditional(0, 1, "")
            .validateLocationCommunicationCode(0, 2, "03")
            .validateLocationCommunicationName(0, 2, "Telefon mobil")
            .validateLocationCommunicationValue(0, 2, "123456789")
            .validateLocationCommunicationAdditional(0, 2, "")
            .validateLocationCommunicationCode(0, 3, "04")
            .validateLocationCommunicationName(0, 3, "Fax")
            .validateLocationCommunicationValue(0, 3, "987654321")
            .validateLocationCommunicationAdditional(0, 3, "")
            .validateRelationCount(0, 1)
            .validateRelationLbId(0, 0, leistung.getFullId())
            .validateRelationArs(0, 0, ars);

        InternetAddressesValidator formValidator = new InternetAddressesValidator(response);
        formValidator
            .validateCount(1)
            .validateUri(0, "http://www.oeadresse.de/")
            .validateTitle(0, "Titel OE internet Adresse")
            .validateDescription(0, "Beschreibung OE Internetadresse");

        OeffnungszeitValidator oeffnungszeitValidator = new OeffnungszeitValidator(response);
        oeffnungszeitValidator
            .validateTitle(0, "Öffnungszeiten")
            .validateText(0, "Oeffnungszeitentest")
            .validateAdditionalInformation(
                0,
                "Reguläre Zeiten",
                "Erster Arbeitstag (Mo-Fr) 09:30 - 17:00; Regulaere Zeiten"
            )
            .validateAdditionalInformation(0, "Abweichende Zeiten", "13.08.2019 - 13.08.2030; Abweichende Zeiten")
            .validateAdditionalInformation(0, "Hinweis", "Hinweistext");

        CommunicationSytemsValidator communicationSytemsValidator = new CommunicationSytemsValidator(response);
        communicationSytemsValidator
            .validateCount(1)
            .validateName(0, "E-Mail-Client")
            .validateValue(0, "bla@bla.de")
            .validateAdditional(0, "Kennungszusatz Email");

        PublisherValidator publisherValidator = new PublisherValidator(response);
        publisherValidator
            .validateHerausgeberBezeichnung("HerausgeberBezeichnung")
            .validateHerausgeberKurzbezeichnung("HerausgeberKurzbezeichnung")
            .validateHerausgeberBeschreibung("HerausgeberBeschreibung");

        InfoOtherValidator infoOtherValidator = new InfoOtherValidator(response);
        infoOtherValidator.validateTitle(0, "Sonstige Informationen").validateText(0, "Sonstige Informationen");
    }

    @Test
    void datenEnglish(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        organisationseinheit.setTitelEnglisch("FirstOE_EN");
        organisationseinheit.setBeschreibungEnglisch("OE Description EN");
        organisationseinheit.setKurzbeschreibungEnglisch("OE_Short_Description_EN");
        organisationseinheit.getInfoOeffnungszeitenText().add("Oeffnungszeitentest");
        organisationseinheit.setSonstigesEnglisch("Other Information EN");
        organisationseinheit.setInfoOeffnungszeitenTextEnglisch(List.of("Opening EN"));
        organisationseinheit.getInfoOeffnungszeitenStrukturiert().clear();
        organisationseinheit.getInfoOeffnungszeitenStrukturiert().add(
            Zeitserien.builder()
                .type("07")
                .hinweistext("Hinweistext")
                .hinweistextEnglisch("Note EN")
                .gueltigkeit(
                    List.of(
                        Zeitraum.builder()
                            .beginn(LocalDate.of(2019, 8, 13))
                            .ende(LocalDate.of(2022, 8, 13))
                            .zusatz("GZusatz OE")
                            .zusatzEnglisch("GZusatz OE EN")
                            .build()
                    )
                )
                .regulaereZeiten(
                    List.of(
                        Zeitserie.builder()
                            .tagesposition("01")
                            .tagestyp("09")
                            .beginn(LocalTime.of(9, 30, 0))
                            .ende(LocalTime.of(17, 0, 0))
                            .zusatz("Regulaere Zeiten")
                            .zusatzEnglisch("Regular Time EN")
                            .build()
                    )
                )
                .abweichendeZeiten(
                    List.of(
                        Zeitraum.builder()
                            .beginn(LocalDate.of(2019, 8, 13))
                            .ende(LocalDate.of(2030, 8, 13))
                            .zusatz("Abweichende Zeiten")
                            .zusatzEnglisch("Different tiem EN")
                            .build()
                    )
                )
                .build()
        );
        organisationseinheit.setInternetadressen(
            Link.builder()
                .uri("http://www.oeadresse.de/")
                .uriEnglisch("http://www.oeadresse.com/")
                .titel("Titel OE internet Adresse")
                .titelEnglisch("Titel OE internet Adresse Englisch")
                .beschreibung("Beschreibung OE Internetadresse")
                .beschreibungEnglisch("Beschreibung OE Internetadresse Englisch")
                .positionDarstellung(0)
                .build()
        );

        organisationseinheit.getBankverbindungen().clear();
        organisationseinheit
            .getBankverbindungen()
            .add(
                Bankverbindung.builder()
                    .empfaenger("Der Gemeindevorstand")
                    .iban("DE25501900000300336027")
                    .bic("FFVBDEFFXXX")
                    .bankinstitut("Frankfurter Volksbank e.G.")
                    .build()
            );

        organisationseinheit.getAnschriften().clear();
        organisationseinheit.getAnschriften().add(
            Anschrift.builder()
                .typ("001")
                .strasse("Billstraße")
                .hausnummer("82")
                .postleitzahl("20539")
                .ort("Hamburg")
                .zusatz("Kein Zusatz")
                .geoPoints(
                    List.of(GeoPoint.builder().srsName("EPSG:31468").values(List.of(4458169.59, 5403691.43)).build())
                )
                .infoParkplatzEnglisch("only few parking spaces")
                .infoOEPNVEnglisch("well accessible")
                .infoBarrierefreiheitEnglisch("accessible")
                .kennzeichenAufzug(true)
                .kennzeichenRollstuhlgerecht(true)
                .build()
        );
        var zsoe = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        zsoe.getKommunikationssysteme().clear();
        zsoe.getKommunikationssysteme().add(
            Kommunikationssystem.builder()
                .code("002")
                .codeName("Email")
                .listUri("urn:xoev-de:fim:codeliste:xzufi.kommunikationssystemtyp")
                .kennung("bla@bla.com")
                .zusatz("Kennungszusatz Email")
                .gueltigkeit(
                    List.of(
                        Zeitraum.builder()
                            .beginn(LocalDate.of(2019, 8, 13))
                            .ende(LocalDate.of(2024, 8, 13))
                            .build()
                    )
                )
                .build()
        );

        organisationseinheit.setHerausgeber(
            Herausgeber.builder()
                .beschreibung("HerausgeberBeschreibung")
                .beschreibungEnglisch("PublisherDescription")
                .bezeichnung("HerausgeberBezeichnungOnlyDE")
                .kurzbezeichnung("HerausgeberKurzbezeichnungOnlyDE")
                .build()
        );

        organisationseinheit.getKontaktpersonen().clear();
        file.getZustaendigkeitenKontaktperson().clear();
        Kontaktperson kontaktperson = Kontaktperson.builder()
            .id("KP_1")
            .schemeAgencyId(CreatorConstants.DEFAULT_BUNDESLANDCONFIG.getSchemeAgencyId())
            .vorname("Max")
            .familienname("Bendler")
            .raum("5")
            .erreichbarkeiten(
                List.of(
                    Erreichbarkeit.builder().code("01").kennung("max.bendler@pvog.de").build(),
                    Erreichbarkeit.builder().code("02").kennung("111222333").build(),
                    Erreichbarkeit.builder().code("03").kennung("123456789").build(),
                    Erreichbarkeit.builder().code("04").kennung("987654321").build()
                )
            )
            .build();
        organisationseinheit.getKontaktpersonen().add(kontaktperson);
        Zustaendigkeit zustaendigkeit = ZsKontaktpersonCreator.getInstance().getMin(
            leistung.getId(),
            kontaktperson.getId()
        );
        file.getZustaendigkeitenKontaktperson().add(zustaendigkeit);

        // Formulare Bauen
        Formular nestedForm = FoCreator.getInstance().getMin(testcasename);
        nestedForm.setLink(
            Link.builder()
                .uri("https://gidf.de/fo_full_orga_data")
                .uriEnglisch("https://gidf.de/fo_full_orga_data/en")
                .build()
        );
        nestedForm.setBezeichnung("Name FO_full_orga_data");
        nestedForm.setBezeichnungEnglisch("Name FO_full_orga_data Englisch");
        nestedForm.setBeschreibung("Beschreibung FO_full_orga_data");
        nestedForm.setBeschreibungEnglisch("Beschreibung FO_full_orga_data Englisch");
        nestedForm.setMimeCode("MimeCode FO_full_orga_data");
        Zustaendigkeit zsNestedForm = ZsFormularCreator.getInstance().getMin(leistung.getId(), nestedForm.getId());

        nestedForm.getZustaendigkeiten().add(zsNestedForm);
        organisationseinheit.getFormulare().add(nestedForm);

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        String ars = zustaendigkeit.getArss().getFirst();
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            ars,
            leistung.getFullId(),
            null,
            Constants.LANGUAGE_EN
        );

        NestedFormsValidator nestedFormsValidator = new NestedFormsValidator(response);
        nestedFormsValidator
            .validateCount(1)
            .validateDescription(0, "Beschreibung FO_full_orga_data Englisch")
            .validateName(0, "Name FO_full_orga_data Englisch")
            .validateUri(0, "https://gidf.de/fo_full_orga_data/en")
            .validateMimeCode(0, "MimeCode FO_full_orga_data");

        //Validierung
        OrganisationValidator organisationValidator = new OrganisationValidator(response);
        organisationValidator
            .validateTitle("FirstOE_EN")
            .validateDescription("OE Description EN")
            .validateShortDescription("OE_Short_Description_EN");

        BankAccountValidator bankAccountValidator = new BankAccountValidator(response);
        bankAccountValidator
            .validateCount(1)
            .validateReceiver(0, "Der Gemeindevorstand")
            .validateIban(0, "DE25501900000300336027")
            .validateBankName(0, "Frankfurter Volksbank e.G.")
            .validateBic(0, "FFVBDEFFXXX");

        UrlValidator urlValidator = new UrlValidator(response);
        urlValidator.validateCount(0);

        InformationValidator informationValidator = new InformationValidator(response);
        informationValidator.validateCount(0);

        AddressValidator addressValidator = new AddressValidator(response);
        addressValidator
            .validateCount(1)
            .validateCode(0, "001")
            .validateCity(0, "Hamburg")
            .validateHouseNumber(0, "82")
            .validateType(0, "Address")
            .validateZip(0, "20539")
            .validateStreet(0, "Billstraße")
            .validateAdditional(0, "Kein Zusatz")
            .validateAccessible(0, "accessible")
            .validateOepnv(0, "well accessible")
            .validateParking(0, "only few parking spaces")
            .validateWheelchairAccessible(0, true)
            .validateLift(0, true);

        CommunicationValidator communicationValidator = new CommunicationValidator(response);
        communicationValidator.validateCount(0);

        PublisherValidator publisherValidator = new PublisherValidator(response);
        publisherValidator
            .validateHerausgeberBezeichnung("HerausgeberBezeichnungOnlyDE")
            .validateHerausgeberKurzbezeichnung("HerausgeberKurzbezeichnungOnlyDE")
            .validateHerausgeberBeschreibung("PublisherDescription");

        EmployeeValidator employeeValidator = new EmployeeValidator(response);
        employeeValidator
            .validateCount(1)
            .validateName(0, "Max")
            .validateLastname(0, "Bendler")
            .validateRoom(0, "5")
            .validateAdditionalLinksCount(0, 0)
            .validateLocationUrlCount(0, 0)
            .validateLocationInformationCount(0, 0)
            .validateLocationAddressCount(0, 0)
            .validateLocationCommunicationCount(0, 4)
            .validateLocationCommunicationCode(0, 0, "01")
            .validateLocationCommunicationName(0, 0, "Email")
            .validateLocationCommunicationValue(0, 0, "max.bendler@pvog.de")
            .validateLocationCommunicationAdditional(0, 0, "")
            .validateLocationCommunicationCode(0, 1, "02")
            .validateLocationCommunicationName(0, 1, "Landline")
            .validateLocationCommunicationValue(0, 1, "111222333")
            .validateLocationCommunicationAdditional(0, 1, "")
            .validateLocationCommunicationCode(0, 2, "03")
            .validateLocationCommunicationName(0, 2, "Mobile phone")
            .validateLocationCommunicationValue(0, 2, "123456789")
            .validateLocationCommunicationAdditional(0, 2, "")
            .validateLocationCommunicationCode(0, 3, "04")
            .validateLocationCommunicationName(0, 3, "Fax")
            .validateLocationCommunicationValue(0, 3, "987654321")
            .validateLocationCommunicationAdditional(0, 3, "")
            .validateRelationCount(0, 1)
            .validateRelationLbId(0, 0, leistung.getFullId())
            .validateRelationArs(0, 0, ars);

        InternetAddressesValidator internetAddressesValidator = new InternetAddressesValidator(response);
        internetAddressesValidator
            .validateCount(1)
            .validateUri(0, "http://www.oeadresse.com/")
            .validateTitle(0, "Titel OE internet Adresse Englisch")
            .validateDescription(0, "Beschreibung OE Internetadresse Englisch");

        OeffnungszeitValidator oeffnungszeitValidator = new OeffnungszeitValidator(response);
        oeffnungszeitValidator
            .validateTitle(0, "Business hours")
            .validateText(0, "Opening EN")
            .validateAdditionalInformation(
                0,
                "Regular hours",
                "first Working day (Mo-Fr) 09:30AM - 05:00PM; Regular Time EN"
            )
            .validateAdditionalInformation(0, "Divergent hours", "2019-08-13 - 2030-08-13; Different tiem EN")
            .validateAdditionalInformation(0, "Note", "Note EN");

        CommunicationSytemsValidator communicationSytemsValidator = new CommunicationSytemsValidator(response);
        communicationSytemsValidator
            .validateCount(2)
            .validateName(0, "System with connection to FIT-Connect")
            .validateName(1, "E-mail client")
            .validateValue(1, "bla@bla.com")
            .validateAdditional(1, "Kennungszusatz Email");

        InfoOtherValidator infoOtherValidator = new InfoOtherValidator(response);
        infoOtherValidator.validateTitle(0, "Additional information").validateText(0, "Other Information EN");
    }

    @Test
    void datenEnglishIncomplete(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();

        String expectedTitelEN = "Englischer Titel"; //wird validiert
        String expectedBeschreibungEN = "Englische Beschreibung"; //wird validiert
        String expectedKurzBeschreibungDE = "Deutsche Kurzbeschreibung"; //EN Text fehlt
        String expectedOeffungszeitenTextDE = "Deutscher Öffnungszeitentext"; //EN Text fehlt
        String expectedSonstigesEN = "Englisch Sonstiges"; //wird validiert
        String expectedOeffungszeitStrukturiertRegulaerTextDE = "Deutsch Gültigkeit Regulär Text"; //EN Text fehlt
        String expectedOeffungszeitStrukturiertAbweichendTextEN = "Englisch Abweichend Text"; //wird validiert
        String expectedFormUriEN = "http://oelink.en"; //wird validiert
        String expectedFormTitelDE = "Deutscher Link Titel"; //EN Text fehlt
        String expectedFormBeschreibungDE = "Deutsche Link Beschreibung"; //EN Text fehlt
        String expectedInfoParkplatzDE = "Deutsch ausreichend Parkplätze"; //EN Text fehlt
        String expectedInfoOEPNVEN = "Englisch ÖPNV"; //wird validiert
        String expectedInfoBarrierefreiheitDE = "Deutsch Barrierearm"; //EN Text fehlt

        organisationseinheit.setTitelEnglisch(expectedTitelEN);
        organisationseinheit.setBeschreibung("Deutsche Beschreibung");
        organisationseinheit.setBeschreibungEnglisch(expectedBeschreibungEN);
        organisationseinheit.setKurzbeschreibung(expectedKurzBeschreibungDE);
        organisationseinheit.getInfoOeffnungszeitenText().add(expectedOeffungszeitenTextDE);
        organisationseinheit.setSonstiges("Deutsch Sonstiges");
        organisationseinheit.setSonstigesEnglisch(expectedSonstigesEN);
        organisationseinheit.getInfoOeffnungszeitenStrukturiert().clear();
        organisationseinheit.getInfoOeffnungszeitenStrukturiert().add(
            Zeitserien.builder()
                .type("07")
                .hinweistext("Deutsch Öffnungszeit Strukturiert Text")
                .gueltigkeit(
                    List.of(
                        Zeitraum.builder()
                            .beginn(LocalDate.of(2019, 8, 13))
                            .ende(LocalDate.of(2022, 8, 13))
                            .zusatz("Deutsch Gültigkeit Text")
                            .zusatzEnglisch("Englisch Gültigkeit Text")
                            .build()
                    )
                )
                .regulaereZeiten(
                    List.of(
                        Zeitserie.builder()
                            .tagesposition("01")
                            .tagestyp("09")
                            .beginn(LocalTime.of(9, 30, 0))
                            .ende(LocalTime.of(17, 0, 0))
                            .zusatz(expectedOeffungszeitStrukturiertRegulaerTextDE)
                            .build()
                    )
                )
                .abweichendeZeiten(
                    List.of(
                        Zeitraum.builder()
                            .beginn(LocalDate.of(2019, 8, 13))
                            .ende(LocalDate.of(2030, 8, 13))
                            .zusatz("Deutsch Abweichend Text")
                            .zusatzEnglisch(expectedOeffungszeitStrukturiertAbweichendTextEN)
                            .build()
                    )
                )
                .build()
        );
        organisationseinheit.setInternetadressen(
            Link.builder()
                .uri("http://oelink.de")
                .uriEnglisch(expectedFormUriEN)
                .titel(expectedFormTitelDE)
                .beschreibung(expectedFormBeschreibungDE)
                .positionDarstellung(0)
                .build()
        );

        organisationseinheit.setHerausgeber(
            Herausgeber.builder()
                .beschreibung("HerausgeberBeschreibung")
                .beschreibungEnglisch("PublisherDescription")
                .bezeichnung("HerausgeberBezeichnungOnlyDE")
                .kurzbezeichnung("HerausgeberKurzbezeichnungOnlyDE")
                .build()
        );

        organisationseinheit.getAnschriften().clear();
        organisationseinheit.getAnschriften().add(
            Anschrift.builder()
                .typ("001")
                .strasse("Billstraße")
                .hausnummer("82")
                .postleitzahl("20539")
                .ort("Hamburg")
                .zusatz("Kein Zusatz")
                .geoPoints(
                    List.of(GeoPoint.builder().srsName("EPSG:31468").values(List.of(4458169.59, 5403691.43)).build())
                )
                .infoParkplatz(expectedInfoParkplatzDE)
                .infoOEPNV("Deutsch ÖPNV")
                .infoOEPNVEnglisch(expectedInfoOEPNVEN)
                .infoBarrierefreiheit(expectedInfoBarrierefreiheitDE)
                .kennzeichenAufzug(true)
                .kennzeichenRollstuhlgerecht(true)
                .build()
        );

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            ars,
            leistung.getFullId(),
            null,
            Constants.LANGUAGE_EN
        );

        //Validierung
        OrganisationValidator organisationValidator = new OrganisationValidator(response);
        organisationValidator
            .validateTitle(expectedTitelEN)
            .validateDescription(expectedBeschreibungEN)
            .validateLanguageComplete("en", false)
            .validateShortDescriptionNotExists();

        UrlValidator urlValidator = new UrlValidator(response);
        urlValidator.validateCount(0);

        InformationValidator informationValidator = new InformationValidator(response);
        informationValidator.validateCount(0);

        AddressValidator addressValidator = new AddressValidator(response);
        addressValidator
            .validateCount(1)
            .validateCode(0, "001")
            .validateCity(0, "Hamburg")
            .validateHouseNumber(0, "82")
            .validateType(0, "Address")
            .validateZip(0, "20539")
            .validateStreet(0, "Billstraße")
            .validateAdditional(0, "Kein Zusatz")
            .validateOepnv(0, expectedInfoOEPNVEN)
            .validateWheelchairAccessible(0, true)
            .validateLift(0, true)
            .validateAccessibleNotExist(0)
            .validateParkingNotExist(0);

        CommunicationValidator communicationValidator = new CommunicationValidator(response);
        communicationValidator.validateCount(0);

        PublisherValidator publisherValidator = new PublisherValidator(response);
        publisherValidator
            .validateHerausgeberBezeichnung("HerausgeberBezeichnungOnlyDE")
            .validateHerausgeberKurzbezeichnung("HerausgeberKurzbezeichnungOnlyDE")
            .validateHerausgeberBeschreibung("PublisherDescription");

        InternetAddressesValidator formValidator = new InternetAddressesValidator(response);
        formValidator
            .validateCount(1)
            .validateUri(0, expectedFormUriEN)
            .validateTitleNotExist(0)
            .validateDescriptionNotExist(0);

        OeffnungszeitValidator oeffnungszeitValidator = new OeffnungszeitValidator(response);
        oeffnungszeitValidator
            .validateTitle(0, "Business hours")
            .validateAdditionalInformation(
                0,
                "Divergent hours",
                "2019-08-13 - 2030-08-13; " + expectedOeffungszeitStrukturiertAbweichendTextEN
            )
            .validateTextNotExist(0)
            .validateAdditionalInformationNotContains(0, "Regular hours")
            .validateAdditionalInformationNotContains(0, "Note");

        InfoOtherValidator infoOtherValidator = new InfoOtherValidator(response);
        infoOtherValidator.validateTitle(0, "Additional information").validateText(0, expectedSonstigesEN);
    }

    @Test
    void deutscheOrganisationseinheitOhneEnglischFallback(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        file.getOrganisationseinheiten().getFirst().setTitel("Titel deutsch");

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();

        ImportUtil.pushAndWait23(file);

        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            ars,
            leistung.getFullId(),
            null,
            Constants.LANGUAGE_EN
        );
        OrganisationValidator organisationValidator = new OrganisationValidator(response);
        organisationValidator.validateTitle("Titel deutsch");
        response.then().header("Content-Language", "en");
    }
}
