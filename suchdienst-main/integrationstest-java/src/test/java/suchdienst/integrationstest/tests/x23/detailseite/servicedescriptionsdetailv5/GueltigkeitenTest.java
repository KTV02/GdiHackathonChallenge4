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
package suchdienst.integrationstest.tests.x23.detailseite.servicedescriptionsdetailv5;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.modul.BearbeitungsdauermodulCreator;
import de.dataport.pvog.testdatengenerator.creator.modul.KostenmodulCreator;
import de.dataport.pvog.testdatengenerator.creator.modul.TextmodulCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zeitraum;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulbearbeitungsdauer.Bearbeitungsdauermodul;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.Kostenmodul;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.ErrorValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.BearbeitungsdauermodulValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.KostenmodulValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.TextmodulValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class GueltigkeitenTest {

    @Test
    void allesGueltig(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeuegn
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        leistung.getGueltigkeiten().clear();
        leistung.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2020, 8, 1))
                .ende(LocalDate.of(2100, 8, 1))
                .build()
        );
        leistung.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2150, 8, 1))
                .ende(LocalDate.of(2200, 8, 1))
                .build()
        );

        leistung.getTextmodule().clear();
        Textmodul textmodul03 = TextmodulCreator.getInstance().getMin(testcasename, TextmodulCreator.Code.C03);
        textmodul03.setInhalt("Textmodul03");
        textmodul03.getGueltigkeiten().clear();
        textmodul03.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2020, 8, 1))
                .ende(LocalDate.of(2040, 8, 1))
                .build()
        );
        leistung.getTextmodule().add(textmodul03);
        Textmodul textmodul06 = TextmodulCreator.getInstance().getMin(testcasename, TextmodulCreator.Code.C06);
        textmodul06.setInhalt("Textmodul06");
        textmodul06.getGueltigkeiten().clear();
        textmodul06.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2020, 8, 1))
                .ende(LocalDate.of(2050, 8, 1))
                .build()
        );
        leistung.getTextmodule().add(textmodul06);

        Bearbeitungsdauermodul bearbeitungsdauermodul = BearbeitungsdauermodulCreator.getInstance().getMin();
        bearbeitungsdauermodul.setBeschreibung("Beschreibung des Bearbeitungsdauermodules.");
        bearbeitungsdauermodul.getGueltigkeiten().clear();
        bearbeitungsdauermodul.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2020, 8, 1))
                .ende(LocalDate.of(2100, 8, 1))
                .build()
        );
        leistung.setBearbeitungsdauermodule(List.of(bearbeitungsdauermodul));

        Kostenmodul kostenmodul = KostenmodulCreator.getInstance().getMin();
        kostenmodul.setBeschreibung("Beschreibung des Kostenmodules.");
        kostenmodul.getGueltigkeiten().clear();
        kostenmodul.setGueltigkeiten(
            List.of(
                Zeitraum.builder()
                    .beginn(LocalDate.of(2020, 8, 1))
                    .ende(LocalDate.of(2100, 8, 1))
                    .build()
            )
        );
        leistung.setKostenmodule(List.of(kostenmodul));

        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.getGueltigkeiten().clear();
        onlinedienst.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2020, 8, 1))
                .ende(LocalDate.of(2080, 8, 1))
                .build()
        );
        onlinedienst.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2090, 8, 1))
                .ende(LocalDate.of(2200, 8, 1))
                .build()
        );

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            leistung.getFullId(),
            LocalDate.now(),
            Constants.LANGUAGE_DE
        );

        // Validierung
        ServiceDescriptionValidator serviceDescriptionValidator = new ServiceDescriptionValidator(response);
        serviceDescriptionValidator
            .validateId(leistung.getFullId())
            .validatePeriodCount(2)
            .validatePeriodValidFrom(0, "2020-08-01")
            .validatePeriodValidto(0, "2100-08-01")
            .validatePeriodValidFrom(1, "2150-08-01")
            .validatePeriodValidto(1, "2200-08-01")
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, file.getOrganisationseinheiten().getFirst().getFullId());

        TextmodulValidator textmodulValidator = new TextmodulValidator(response);
        textmodulValidator.validateCount(2).validateText(0, "Textmodul06").validateText(1, "Textmodul03");

        BearbeitungsdauermodulValidator bearbeitungsdauermodulValidator = new BearbeitungsdauermodulValidator(response);
        bearbeitungsdauermodulValidator.validateText("Beschreibung des Bearbeitungsdauermodules.");

        KostenmodulValidator kostenmodulValidator = new KostenmodulValidator(response);
        kostenmodulValidator.validateText("Beschreibung des Kostenmodules.");

        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator
            .validateCount(1)
            .validateId(0, onlinedienst.getFullId())
            .validateValidityPeriodsCount(0, 2)
            .validateValidityPeriodsValidFrom(0, 0, "2020-08-01")
            .validateValidityPeriodsValidTo(0, 0, "2080-08-01")
            .validateValidityPeriodsValidFrom(0, 1, "2090-08-01")
            .validateValidityPeriodsValidTo(0, 1, "2200-08-01");
    }

    @Test
    void teilweiseGueltig(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeuegn
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        leistung.getTextmodule().clear();
        Textmodul textmodul03 = TextmodulCreator.getInstance().getMin(testcasename, TextmodulCreator.Code.C03);
        textmodul03.setInhalt("Textmodul03");
        textmodul03.getGueltigkeiten().clear();
        textmodul03.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2020, 8, 1))
                .ende(LocalDate.of(2040, 8, 1))
                .build()
        );
        leistung.getTextmodule().add(textmodul03);
        Textmodul textmodul06 = TextmodulCreator.getInstance().getMin(testcasename, TextmodulCreator.Code.C06);
        textmodul06.setInhalt("Textmodul06");
        textmodul06.getGueltigkeiten().clear();
        textmodul06.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2100, 8, 1))
                .ende(LocalDate.of(2150, 8, 1))
                .build()
        );
        leistung.getTextmodule().add(textmodul06);

        Bearbeitungsdauermodul bearbeitungsdauermodul = BearbeitungsdauermodulCreator.getInstance().getMin();
        bearbeitungsdauermodul.setBeschreibung("Beschreibung des Bearbeitungsdauermodules.");
        bearbeitungsdauermodul.getGueltigkeiten().clear();
        bearbeitungsdauermodul.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2100, 8, 1))
                .ende(LocalDate.of(2150, 8, 1))
                .build()
        );
        leistung.setBearbeitungsdauermodule(List.of(bearbeitungsdauermodul));

        Kostenmodul kostenmodul = KostenmodulCreator.getInstance().getMin();
        kostenmodul.setBeschreibung("Beschreibung des Kostenmodules.");
        kostenmodul.getGueltigkeiten().clear();
        kostenmodul.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2020, 8, 1))
                .ende(LocalDate.of(2100, 8, 1))
                .build()
        );
        leistung.setKostenmodule(List.of(kostenmodul));

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            leistung.getFullId(),
            LocalDate.now(),
            Constants.LANGUAGE_DE
        );

        // Validierung
        ServiceDescriptionValidator serviceDescriptionValidator = new ServiceDescriptionValidator(response);
        serviceDescriptionValidator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, file.getOrganisationseinheiten().getFirst().getFullId());

        TextmodulValidator textmodulValidator = new TextmodulValidator(response);
        textmodulValidator.validateCount(1).validateText(0, "Textmodul03");

        BearbeitungsdauermodulValidator bearbeitungsdauermodulValidator = new BearbeitungsdauermodulValidator(response);
        bearbeitungsdauermodulValidator.validateCount(0);

        KostenmodulValidator kostenmodulValidator = new KostenmodulValidator(response);
        kostenmodulValidator.validateText("Beschreibung des Kostenmodules.");
    }

    @Test
    void lbUngueltig(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeuegn
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        leistung.getGueltigkeiten().clear();
        leistung.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2100, 8, 1))
                .ende(LocalDate.of(2150, 8, 1))
                .build()
        );

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            leistung.getFullId(),
            LocalDate.now(),
            Constants.LANGUAGE_DE
        );

        // Validierung
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
    }

    @Test
    void odUngueltig(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeuegn
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.getGueltigkeiten().clear();
        onlinedienst.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2010, 8, 1))
                .ende(LocalDate.of(2020, 8, 1))
                .build()
        );

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response responseService = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            leistung.getFullId(),
            LocalDate.now(),
            Constants.LANGUAGE_DE
        );

        // Validierung
        ServiceDescriptionValidator serviceDescriptionValidator = new ServiceDescriptionValidator(responseService);
        serviceDescriptionValidator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, file.getOrganisationseinheiten().getFirst().getFullId());

        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(responseService);
        onlineServiceLinkValidator.validateCount(0);
    }
}
