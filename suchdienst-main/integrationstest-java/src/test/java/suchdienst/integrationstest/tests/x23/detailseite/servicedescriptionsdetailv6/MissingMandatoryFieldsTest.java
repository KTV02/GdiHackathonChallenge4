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
package suchdienst.integrationstest.tests.x23.detailseite.servicedescriptionsdetailv6;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.ArrayList;
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
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.TextmodulValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class MissingMandatoryFieldsTest {

    /*
    Es gibt im SuD backend eine Sonderbehandlung für den Fall, dass der Name also Textmodul 02 bzw. 03 nicht englischsprachig
    geliefert wird, prinzipiell aber englische Inhalte einer Leistung vorhanden sind.
    In diesem Fall werden die englischsprachigen Inhalte nicht ausgegeben - availableLanguages ist lediglich de.
    Das wird implizit über die Validierung languageCompleteIsEmpty geprüft
    */

    @Test
    void detailsData(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        List<Textmodul> textmodule = new ArrayList<>();
        String beschreibungTM02DE = "MissingMandatoryBeschreibungDE";
        Textmodul tm02 = Textmodul.builder().inhalt(beschreibungTM02DE).positionDarstellung(2).leikaCode("02").build();

        String titelTM03DE = "MissingMandatoryTitelDE";
        Textmodul tm03 = Textmodul.builder().inhalt(titelTM03DE).positionDarstellung(3).leikaCode("03").build();

        String volltextTM06DE = "MissingMandatoryVolltextDE";
        Textmodul tm06 = Textmodul.builder().inhalt(volltextTM06DE).positionDarstellung(4).leikaCode("06").build();

        Textmodul tm07 = Textmodul.builder()
            .inhalt("Text07DE")
            .inhaltEnglisch("Text07EN")
            .positionDarstellung(5)
            .leikaCode("07")
            .build();

        textmodule.add(tm02);
        textmodule.add(tm03);
        textmodule.add(tm06);
        textmodule.add(tm07);
        lb.setTextmodule(textmodule);

        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();

        String onlineserviceURIDE = "https://missingmandatory.com/uri";
        String onlineServiceTitelDE = "MissingMandatoryOdLinkTitel";
        String onlineServiceBeschreibungDE = "MissingMandatoryOdLinkBeschreibung";

        onlinedienst.setKurzbezeichnungEnglisch("MissingMandatoryShortDescription");
        onlinedienst.setLinks(
            List.of(
                Link.builder()
                    .uri(onlineserviceURIDE)
                    .titel(onlineServiceTitelDE)
                    .beschreibung(onlineServiceBeschreibungDE)
                    .positionDarstellung(1010)
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            lbId,
            Constants.LANGUAGE_EN
        );

        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateName(titelTM03DE).valdateLanguageCompleteIsEmpty();

        TextmodulValidator textmodulValidator = new TextmodulValidator(response);
        textmodulValidator.validateTextByCode("07", "Text07DE");

        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator
            .validateUri(0, onlineserviceURIDE)
            .validateTitle(0, onlineServiceTitelDE)
            .validateDescription(0, onlineServiceBeschreibungDE)
            .validateCount(1);
    }

    @Test
    void detailsDataDescriptionNull(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        List<Textmodul> textmodule = new ArrayList<>();
        String beschreibungTM02EN = "MissingMandatoryBeschreibungEN";
        String beschreibungTM02DE = "MissingMandatoryBeschreibungDE";
        Textmodul tm02 = Textmodul.builder()
            .inhalt(beschreibungTM02DE)
            .inhaltEnglisch(beschreibungTM02EN)
            .positionDarstellung(2)
            .leikaCode("02")
            .build();

        String titelTM03DE = "MissingMandatoryTitelDE";
        Textmodul tm03 = Textmodul.builder().inhalt(titelTM03DE).positionDarstellung(3).leikaCode("03").build();

        String volltextTM06DE = "MissingMandatoryVolltextDE";
        Textmodul tm06 = Textmodul.builder().inhalt(volltextTM06DE).positionDarstellung(4).leikaCode("06").build();

        textmodule.add(tm02);
        textmodule.add(tm03);
        textmodule.add(tm06);
        lb.setTextmodule(textmodule);

        ImportUtil.pushAndWait23(file);

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            lbId,
            Constants.LANGUAGE_EN
        );

        new ServiceDescriptionValidator(response).validateName(beschreibungTM02EN).validateLanguageCompleteForEN(false);
    }
}
