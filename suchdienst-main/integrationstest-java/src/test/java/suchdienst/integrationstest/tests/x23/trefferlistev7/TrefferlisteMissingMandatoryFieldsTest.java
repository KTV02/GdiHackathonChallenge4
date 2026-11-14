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
package suchdienst.integrationstest.tests.x23.trefferlistev7;

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
import suchdienst.integrationstest.validator.servicedescriptionv7.ServiceDescriptionResultListValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class TrefferlisteMissingMandatoryFieldsTest {

    /*
    Der Test prüft, dass bei fehlenden englischsprachigen Pflichtfeldern und Abfrage mit LanguageCode en die
    deutschsprachigen Inhalte übertragen werden
*/

    @Test
    void contentDataDE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String randomPrefix = TestUtil.generateRandomWord();

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

        String odBezeichnungDE = randomPrefix + "MissingMandatoryODBezeichnungDE";
        String onlineserviceURIDE = "https://missingmandatory.com/uri";
        String onlineServiceTitelDE = "MissingMandatoryOdLinkTitel";
        String onlineServiceBeschreibungDE = "MissingMandatoryOdLinkBeschreibung";

        onlinedienst.setBezeichnung(odBezeichnungDE);
        onlinedienst.setKurzbezeichnungEnglisch("MissingMandatoryShortDescription");
        onlinedienst.setLinks(
            List.of(
                Link.builder()
                    .uri(onlineserviceURIDE)
                    .titel(onlineServiceTitelDE)
                    .beschreibung(onlineServiceBeschreibungDE)
                    .positionDarstellung(1010)
                    .positionDarstellungEnglisch(1)
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();

        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ars,
            odBezeichnungDE,
            Constants.LANGUAGE_EN
        );
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator
            .validateName(0, titelTM03DE)
            .validateDescription(0, volltextTM06DE)
            .validateLanguageCompleteIsEmpty(0)
            .validateCount(1)
            .validateOnlineServiceLinksCount(0, 1)
            .validateOnlineServiceLinksUri(0, 0, onlineserviceURIDE)
            .validateOnlineServiceLinksTitle(0, 0, onlineServiceTitelDE)
            .validateOnlineServiceLinksDescription(0, 0, onlineServiceBeschreibungDE);
    }
}
