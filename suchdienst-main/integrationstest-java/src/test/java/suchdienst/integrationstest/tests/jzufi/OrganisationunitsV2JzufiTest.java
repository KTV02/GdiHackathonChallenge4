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
package suchdienst.integrationstest.tests.jzufi;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static suchdienst.integrationstest.common.Constants.XzufiVersion.V2_2_0;
import static suchdienst.integrationstest.common.Constants.XzufiVersion.V2_3_1;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.FoCreator;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig22;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig23;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22FormularGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22OrganisationseinheitsGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22ZustaendigkeitsGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi23.Xzufi23FormularGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi23.Xzufi23OrganisationseinheitsGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi23.Xzufi23ZustaendigkeitsGenerator;
import de.dataport.pvog.testdatengenerator.model.Anschrift;
import de.dataport.pvog.testdatengenerator.model.Bankverbindung;
import de.dataport.pvog.testdatengenerator.model.Erreichbarkeit;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.Herausgeber;
import de.dataport.pvog.testdatengenerator.model.Kontaktperson;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.Versionsinformation;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zeitraum;
import de.dataport.pvog.testdatengenerator.model.Zeitserie;
import de.dataport.pvog.testdatengenerator.model.Zeitserien;
import de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.validator.ErrorValidator;
import suchdienst.integrationstest.validator.jzufi.AbstractJzufiValidatorOU;
import suchdienst.integrationstest.validator.jzufi.JzufiValidatorOUV2;
import suchdienst.integrationstest.validator.jzufi.JzufiValidatorOUV23;
import suchdienst.integrationstest.validator.jzufi.OpenApiSchemaValidator;

/**
 * Mit den Tests soll die Funktionalität des Endpunkts: /v2/organisationunits/jzufi sichergestellt werden.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class OrganisationunitsV2JzufiTest {

    @Test
    void checkJsonSchemaTest(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).getMin();

        ImportUtil.pushAndWait23(file);
        Response response23 = OrganisationunitsConnector.getOrganisationunitsJzufiV2(
            file.getOrganisationseinheiten().getFirst().getFullId(),
            V2_3_1
        );
        Response response22 = OrganisationunitsConnector.getOrganisationunitsJzufiV2(
            file.getOrganisationseinheiten().getFirst().getFullId(),
            V2_2_0
        );

        // Validate
        String endpoint = "/v2/organisationunits/jzufi";
        OpenApiSchemaValidator.validate(response22, endpoint, "GET");
        OpenApiSchemaValidator.validate(response23, endpoint, "GET");
    }

    @Test
    void compareOrganisationseinheit(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).getMin();
        CodelistConfig22 codelistConfig22 = new CodelistConfig22();
        CodelistConfig23 codelistConfig23 = new CodelistConfig23();

        OrganisationseinheitErweitert expected22 = new Xzufi22OrganisationseinheitsGenerator(
            codelistConfig22,
            new Xzufi22FormularGenerator(codelistConfig22, new Xzufi22ZustaendigkeitsGenerator(codelistConfig22))
        ).generateOrganisationseinheit(file.getOrganisationseinheiten().getFirst());

        de.xoev.xzufi.v2_3_1.Organisationseinheit expected23 = new Xzufi23OrganisationseinheitsGenerator(
            codelistConfig23,
            new Xzufi23FormularGenerator(codelistConfig23, new Xzufi23ZustaendigkeitsGenerator(codelistConfig23))
        ).generateOrganisationseinheit(file.getOrganisationseinheiten().getFirst());

        // Daten Einspielen
        ImportUtil.pushAndWait23(file, codelistConfig23);
        Response response23 = OrganisationunitsConnector.getOrganisationunitsJzufiV2(
            file.getOrganisationseinheiten().getFirst().getFullId(),
            V2_3_1
        );
        Response response22 = OrganisationunitsConnector.getOrganisationunitsJzufiV2(
            file.getOrganisationseinheiten().getFirst().getFullId(),
            V2_2_0
        );

        // Validierung
        de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert actual23 = response23
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json;xzufi-version=2.3.1")
            .extract()
            .body()
            .as(de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert.class);
        assertEquals(expected23, actual23);

        OrganisationseinheitErweitert actuall22 = response22
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json;xzufi-version=2.2.0")
            .extract()
            .body()
            .as(OrganisationseinheitErweitert.class);
        assertEquals(expected22, actuall22);
    }

    @Test
    void checkSpecificJsonPaths(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).getMin();

        // Bankverbindungen
        Bankverbindung verbindugn1 = Bankverbindung.builder()
            // Setze nur zwei Properties in der Bankverbindung, um das Verhalten der Ausgabe von Null-Werte zu testen
            .empfaenger("Timon Testreich")
            .iban("DE81700500000008393621")
            .build();
        file.getOrganisationseinheiten().getFirst().setBankverbindungen(List.of(verbindugn1));

        ImportUtil.pushAndWait23(file);
        Response response23 = OrganisationunitsConnector.getOrganisationunitsJzufiV2(
            file.getOrganisationseinheiten().getFirst().getFullId(),
            V2_3_1
        );
        Response response22 = OrganisationunitsConnector.getOrganisationunitsJzufiV2(
            file.getOrganisationseinheiten().getFirst().getFullId(),
            V2_2_0
        );

        // Validate
        response22
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json;xzufi-version=2.2.0")
            .body("id.value", equalTo(file.getOrganisationseinheiten().getFirst().getId()))
            .body("id.schemeAgencyID", equalTo(file.getOrganisationseinheiten().getFirst().getSchemeAgencyId()))
            // Stellt sicher, dass Werte, die im PV_Schema nicht definiert (null) sind, nicht im JSON ausgeliefert werden
            .body("bankverbindung[0]", not(hasKey("bic")))
            .body("bankverbindung[0]", not(hasKey("bankinstitut")))
            .body("bankverbindung[0]", not(hasKey("verwendungszweck")))
            .body("bankverbindung[0].size()", is(2))
            .body("bankverbindung[0].empfaenger", equalTo(verbindugn1.getEmpfaenger()))
            .body("bankverbindung[0].iban", equalTo(verbindugn1.getIban()));

        response23
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json;xzufi-version=2.3.1")
            .body("id.value", equalTo(file.getOrganisationseinheiten().getFirst().getId()))
            .body("id.schemeAgencyID", equalTo(file.getOrganisationseinheiten().getFirst().getSchemeAgencyId()))
            .body("bankverbindung[0].size()", is(2))
            .body("bankverbindung[0].empfaenger", equalTo(verbindugn1.getEmpfaenger()))
            .body("bankverbindung[0].iban", equalTo(verbindugn1.getIban()))
            // Stellt sicher, dass Werte, die im PV_Schema nicht definiert (null) sind, nicht im JSON ausgeliefert werden
            .body("bankverbindung[0]", not(hasKey("bic")))
            .body("bankverbindung[0]", not(hasKey("bankinstitut")))
            .body("bankverbindung[0]", not(hasKey("verwendungszweck")));
    }

    /**
     * Übermittle einen XZuFi 2.3 Datensatz und erwarte, dass er in XZuFi 2.2 Datensatz umgewandelt wird
     */
    @Test
    void testConverterTo22(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).getMax();

        // Daten als XZuFi 2.3 Senden
        ImportUtil.pushAndWait23(file);
        // Daten als XZuFi 2.2 Abrufen
        Response response = OrganisationunitsConnector.getOrganisationunitsJzufiV2(
            file.getOrganisationseinheiten().getFirst().getFullId(),
            V2_2_0
        );

        // Validate
        assertDoesNotThrow(
            () ->
                response
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .contentType(ContentType.JSON)
                    .extract()
                    .as(de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert.class),
            "Übermittelte Organisationseinheit passt nicht zum XZuFi 2.2 Datenmodell"
        );
    }

    /**
     * Übermittle einen XZuFi 2.2 Datensatz und erwarte, dass er in XZuFi 2.3 Datensatz umgewandelt wird
     */
    @Test
    void testConverterTo23(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        // getMax wird benötigt, da die gefüllten Felder aus getMin von XZuFi 2.3.1 abwärtskompatibel zu XZuFi 2.2 sind.
        // Nur durch getMax entsteht ein Unterschied, der einen Fehlerfall provozieren würde.
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).getMax();

        // Daten als XZuFi 2.2 Senden
        ImportUtil.pushAndWait(file);
        // Daten als XZuFi 2.3 Abrufen
        Response response = OrganisationunitsConnector.getOrganisationunitsJzufiV2(
            file.getOrganisationseinheiten().getFirst().getFullId(),
            V2_3_1
        );

        // Validate
        assertDoesNotThrow(
            () ->
                response
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .contentType(ContentType.JSON)
                    .extract()
                    .as(de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert.class),
            "Übermittelte Organisationseinheit passt nicht zum XZuFi 2.3 Datenmodell"
        );
    }

    // Negativ-Tests
    @Test
    void blankParameter() {
        Response response = OrganisationunitsConnector.getOrganisationunitsJzufiV2("", V2_3_1);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest();
    }

    @Test
    void unknownId() {
        Response response = OrganisationunitsConnector.getOrganisationunitsJzufiV2("unknowId", V2_3_1);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
    }

    @Test
    void detailedTest(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = createDetailedTestFile(testname);

        ImportUtil.pushAndWait(file);
        Response response = OrganisationunitsConnector.getOrganisationunitsJzufiV2(
            file.getOrganisationseinheiten().getFirst().getFullId(),
            V2_2_0
        );

        // Validate
        validateDetailedResponse(new JzufiValidatorOUV2(response), file);
    }

    @Test
    void detailedTest23(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = createDetailedTestFile(testname);
        CodelistConfig23 codelistConfig23 = new CodelistConfig23();

        // Daten als XZuFi 2.3 einspielen und als XZuFi 2.3 abrufen
        ImportUtil.pushAndWait23(file, codelistConfig23);
        Response response = OrganisationunitsConnector.getOrganisationunitsJzufiV2(
            file.getOrganisationseinheiten().getFirst().getFullId(),
            V2_3_1
        );

        // Validate
        validateDetailedResponse(new JzufiValidatorOUV23(response), file);
    }

    private void validateDetailedResponse(AbstractJzufiValidatorOU<?> validator, XzufiFile file) {
        var oe = file.getOrganisationseinheiten().getFirst();
        validator
            .validateId(oe.getId(), file.getSchemeAgencyId())
            .validateTitle(oe.getTitel(), "de")
            .validateTitle(oe.getTitelEnglisch(), "en")
            .validateBeschreibung(oe.getBeschreibung(), "de")
            .validateBeschreibung(oe.getBeschreibungEnglisch(), "en")
            .validateKurzbeschreibung(oe.getKurzbeschreibung(), "de")
            .validateKurzbeschreibung(oe.getKurzbeschreibungEnglisch(), "en")
            .validateInternetadressen(oe.getInternetadressen())
            .validateInfoSonstige(List.of(oe.getSonstiges(), oe.getSonstigesEnglisch()))
            .validateHerausgeber(oe.getHerausgeber())
            .validateKontaktperson(oe.getKontaktpersonen().getFirst())
            .validateAnschriften(oe.getAnschriften())
            .validateInfoOeffnungszeitenText(oe.getInfoOeffnungszeitenText())
            .validateInfoOeffnungszeitenText(oe.getInfoOeffnungszeitenTextEnglisch())
            .validateInfoOeffnungszeitenStrukturiert(oe.getInfoOeffnungszeitenStrukturiert())
            .validateBankverbindungen(oe.getBankverbindungen())
            .validateGueltigkeiten(oe.getGueltigkeiten())
            .validateVersionsinformation(oe.getVersionsinformation())
            .validateFormulare(oe.getFormulare());
    }

    private XzufiFile createDetailedTestFile(String testname) {
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).getMin();

        // Titel
        file.getOrganisationseinheiten()
            .getFirst()
            .setTitel("titel_" + testname);
        file.getOrganisationseinheiten().getFirst().setTitelEnglisch("titel_englisch");

        // Beschreibung
        file.getOrganisationseinheiten()
            .getFirst()
            .setBeschreibung("Beschreibung_" + testname);
        file.getOrganisationseinheiten()
            .getFirst()
            .setBeschreibungEnglisch("Beschreibung_EN_" + testname);

        // Kurzbeschreibung
        file.getOrganisationseinheiten()
            .getFirst()
            .setKurzbeschreibung("Kurzbeschreibung_" + testname);
        file.getOrganisationseinheiten()
            .getFirst()
            .setKurzbeschreibungEnglisch("Kurzbeschreibung_EN_" + testname);

        // Internetadressen
        file.getOrganisationseinheiten()
            .getFirst()
            .setInternetadressen(
                Link.builder()
                    .uri("https://orga.com/internetadressen")
                    .uriEnglisch("https://orga.com/internetadressen#en")
                    .build()
            );

        // Sonstiges
        file.getOrganisationseinheiten()
            .getFirst()
            .setSonstiges("Sonstiges_" + testname);
        file.getOrganisationseinheiten()
            .getFirst()
            .setSonstigesEnglisch("Sonstiges_EN_" + testname);

        // Herausgeber
        file.getOrganisationseinheiten()
            .getFirst()
            .setHerausgeber(
                Herausgeber.builder()
                    .id("Herausgeber_" + testname)
                    .bezeichnung("Bezeichnung_" + testname)
                    .kurzbezeichnung("Kurzbezeichnung_" + testname)
                    .beschreibung("Beschreibung_" + testname)
                    .beschreibungEnglisch("Beschreibung_EN_" + testname)
                    .build()
            );

        // Kontaktperson
        file.getOrganisationseinheiten()
            .getFirst()
            .setKontaktpersonen(
                List.of(
                    Kontaktperson.builder()
                        .id("kontaktperson_" + testname)
                        .anrede("Divers")
                        .titel("Titel_" + testname)
                        .vorname("Tim")
                        .familienname("Tester")
                        .raum("Raum_" + testname)
                        .anschriften(
                            List.of(Anschrift.builder().typ("001").postleitzahl("85435").ort("Erding").build())
                        )
                        .erreichbarkeiten(List.of(Erreichbarkeit.builder().code("03").kennung("08123 342342").build()))
                        .internetadressen(Link.builder().uri("https://amt.amtsgericht.de").build())
                        .build()
                )
            );

        // Anschriften
        file.getOrganisationseinheiten()
            .getFirst()
            .setAnschriften(
                List.of(
                    Anschrift.builder().typ("001").postleitzahl("22764").ort("Hamburg").build(),
                    Anschrift.builder().typ("005").postleitzahl("02131").ort("Teststadt").build()
                )
            );

        // Info Öffnungszeiten Text
        file.getOrganisationseinheiten()
            .getFirst()
            .setInfoOeffnungszeitenText(List.of("Info_1_" + testname, "Info_2_" + testname));
        file.getOrganisationseinheiten()
            .getFirst()
            .setInfoOeffnungszeitenTextEnglisch(List.of("Info_1_EN_" + testname, "Info_2_EN_" + testname));

        // Info Öffnungszeiten Text Strukturiert
        file.getOrganisationseinheiten()
            .getFirst()
            .setInfoOeffnungszeitenStrukturiert(
                List.of(
                    Zeitserien.builder()
                        .type("01")
                        .hinweistextEnglisch("Pay attention to different opening hours")
                        .regulaereZeiten(
                            List.of(
                                Zeitserie.builder()
                                    .beginn(LocalTime.of(1, 9, 0))
                                    .ende(LocalTime.of(1, 9, 0))
                                    .tagestyp("09")
                                    .tagesposition("40")
                                    .build()
                            )
                        )
                        .build()
                )
            );

        // Bankverbindungen
        file.getOrganisationseinheiten()
            .getFirst()
            .setBankverbindungen(
                List.of(
                    Bankverbindung.builder().empfaenger("Timon Testreich").iban("DE81700500000008393621").build(),
                    Bankverbindung.builder().empfaenger("Tina Testing").iban("AT667005000061184201").build()
                )
            );

        // Gueltigkeiten
        file.getOrganisationseinheiten()
            .getFirst()
            .setGueltigkeiten(
                List.of(
                    Zeitraum.builder()
                        .beginn(LocalDate.of(2025, 1, 1))
                        .ende(LocalDate.of(2025, 5, 1))
                        .build(),
                    Zeitraum.builder()
                        .beginn(LocalDate.of(2025, 8, 1))
                        .ende(LocalDate.of(2025, 12, 1))
                        .build()
                )
            );

        // Versionsinformation
        file.getOrganisationseinheiten()
            .getFirst()
            .setVersionsinformation(
                Versionsinformation.builder()
                    .erstelldatum(LocalDateTime.of(2025, 1, 1, 0, 0))
                    .ersteller("Miri Musterfrau")
                    .bearbeitungsdatum(LocalDateTime.of(2025, 3, 28, 0, 0))
                    .bearbeiter("Max Mustermann")
                    .version("1.3.3.7")
                    .build()
            );

        // Formulare
        file.getOrganisationseinheiten()
            .getFirst()
            .setFormulare(
                List.of(
                    FoCreator.getInstance().getMin(testname),
                    Formular.builder()
                        .id("FO_" + testname)
                        .bezeichnung("Formular_" + testname)
                        .bezeichnungEnglisch("Formular_englisch_" + testname)
                        .link(
                            Link.builder()
                                .uri("https://amt.amtsgericht.de")
                                .uriEnglisch("http://google.de/englisch")
                                .build()
                        )
                        .mimeCode("text/html")
                        .build()
                )
            );

        return file;
    }
}
