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
package suchdienst.integrationstest.tests.x22.detailseite.servicedescriptionsdetailv5;

import static suchdienst.integrationstest.common.Constants.LANGUAGE_DE;
import static suchdienst.integrationstest.common.Constants.LANGUAGE_EN;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.Kostenmodul;
import io.restassured.response.Response;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.FristmodulValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.KostenmodulValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.TextmodulValidator;

/**
 * Werden die Pflichtmodule nicht angegeben oder nur leer angegeben werden sie mit einem standart Text gefüllt.
 * Im Englischen wird um eine Deutsch - Englisch Vermischung zu vermeiden, das Modul komplett auf Deutsch ausgegeben.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class PlaceholderModulesTest {

    private static Response responseDE;
    private static Response responseEN;

    @BeforeAll
    static void setup(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Kostenmodul kostenmodul = Kostenmodul.builder().kosten(Collections.emptyList()).build();
        leistung.setKostenmodule(List.of(kostenmodul));

        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        String lbId = leistung.getFullId();
        responseDE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, lbId, LANGUAGE_DE);
        responseEN = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, lbId, LANGUAGE_EN);
    }

    @Test
    void leeresKostenmodulDE() {
        KostenmodulValidator validator = new KostenmodulValidator(responseDE, false);
        validator
            .validateCount(1)
            .validateType("_module")
            .validateTitle("Kosten & Gebühren")
            .validateText("nicht angegeben")
            .validateAdditionalInformationCount(0)
            .validateLinkCount(0)
            .validatePlaceholder(true);
    }

    @Test
    void leeresKostenmodulEN() {
        //Ohne englischen Inhalt wird es komplett auf deutsch ausgegeben
        KostenmodulValidator validator = new KostenmodulValidator(responseEN, false);
        validator
            .validateCount(1)
            .validateType("_module")
            .validateTitle("Kosten & Gebühren")
            .validateText("nicht angegeben")
            .validateAdditionalInformationCount(0)
            .validateLinkCount(0)
            .validatePlaceholder(true);
    }

    @Test
    void fristModulDE() {
        FristmodulValidator validator = new FristmodulValidator(responseDE, false);
        validator
            .validateCount(1)
            .validateType("_module")
            .validateTitle("Fristen")
            .validateText("nicht angegeben")
            .validateAdditionalInformationCount(0)
            .validateLinkCount(0)
            .validatePlaceholder(true);
    }

    @Test
    void fristModulEN() {
        //Ohne englischen Inhalt wird es komplett auf deutsch ausgegeben
        FristmodulValidator validator = new FristmodulValidator(responseEN, false);
        validator
            .validateCount(1)
            .validateType("_module")
            .validateTitle("Fristen")
            .validateText("nicht angegeben")
            .validateAdditionalInformationCount(0)
            .validateLinkCount(0)
            .validatePlaceholder(true);
    }

    @ParameterizedTest
    @CsvSource({
        "09, Voraussetzungen",
        "08, 'Erforderliche Unterlagen'",
        "23, Ansprechpunkt",
        "07, Rechtsgrundlage(n)",
        "18, Urheber",
        "22, 'Zuständige Stelle'",
    })
    void validateTextModulesDE(String code, String title) {
        TextmodulValidator validator = new TextmodulValidator(responseDE, false);
        validator
            .validateTitleByCode(code, title)
            .validateTextByCode(code, "nicht angegeben")
            .validateLinkCountByCode(code, 0)
            .validateAdditionalInformationCountByCode(code, 0)
            .validatePlaceholderByCode(code, true);
    }

    @ParameterizedTest
    @CsvSource({
        "09, Voraussetzungen",
        "08, 'Erforderliche Unterlagen'",
        "23, Ansprechpunkt",
        "07, Rechtsgrundlage(n)",
        "18, Urheber",
        "22, 'Zuständige Stelle'",
    })
    void validateTextModulesEN(String code, String title) {
        //Ohne englischen Inhalt wird es komplett auf deutsch ausgegeben
        TextmodulValidator validator = new TextmodulValidator(responseEN, false);
        validator
            .validateTitleByCode(code, title)
            .validateTextByCode(code, "nicht angegeben")
            .validateLinkCountByCode(code, 0)
            .validateAdditionalInformationCountByCode(code, 0)
            .validatePlaceholderByCode(code, true);
    }
}
