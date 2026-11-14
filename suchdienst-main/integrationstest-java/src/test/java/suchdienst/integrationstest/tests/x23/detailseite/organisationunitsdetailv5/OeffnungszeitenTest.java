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

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zeitraum;
import de.dataport.pvog.testdatengenerator.model.Zeitserie;
import de.dataport.pvog.testdatengenerator.model.Zeitserien;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.OeffnungszeitValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class OeffnungszeitenTest {

    @Test
    void ohneType(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        organisationseinheit.getInfoOeffnungszeitenStrukturiert().clear();
        organisationseinheit.getInfoOeffnungszeitenStrukturiert().add(
            Zeitserien.builder()
                .hinweistext("Wenn wir kein Bock mehr haben machen wir auch früher zu.")
                .gueltigkeit(
                    List.of(
                        Zeitraum.builder()
                            .beginn(LocalDate.of(2016, 5, 28))
                            .ende(LocalDate.of(2016, 5, 28))
                            .zusatz("Nur in diesen Zeitraum")
                            .build()
                    )
                )
                .regulaereZeiten(
                    List.of(
                        Zeitserie.builder()
                            .tagesposition("01")
                            .tagestyp("09")
                            .beginn(LocalTime.of(17, 39, 0))
                            .ende(LocalTime.of(17, 39, 0))
                            .zusatz("Offen")
                            .build()
                    )
                )
                .abweichendeZeiten(
                    List.of(
                        Zeitraum.builder()
                            .beginn(LocalDate.of(2016, 5, 28))
                            .ende(LocalDate.of(2016, 5, 28))
                            .zusatz("Abweichungen")
                            .build()
                    )
                )
                .build()
        );

        ImportUtil.pushAndWait23(file);
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());

        OeffnungszeitValidator validator = new OeffnungszeitValidator(response);
        validator
            .validateCount(1)
            .validateTitle(0, "Öffnungszeiten")
            .validateAdditionalInformation(0, "Reguläre Zeiten", "Erster Arbeitstag (Mo-Fr) 17:39 - 17:39; Offen")
            .validateAdditionalInformation(0, "Abweichende Zeiten", "28.05.2016 - 28.05.2016; Abweichungen")
            .validateAdditionalInformation(0, "Hinweis", "Wenn wir kein Bock mehr haben machen wir auch früher zu.");
    }

    @Test
    void mitType(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        organisationseinheit.getInfoOeffnungszeitenStrukturiert().clear();
        organisationseinheit.getInfoOeffnungszeitenStrukturiert().add(
            Zeitserien.builder()
                .type("07")
                .hinweistext("Wenn wir kein Bock mehr haben machen wir auch früher zu.")
                .gueltigkeit(
                    List.of(
                        Zeitraum.builder()
                            .beginn(LocalDate.of(2016, 5, 28))
                            .ende(LocalDate.of(2016, 5, 28))
                            .zusatz("Nur in diesen Zeitraum")
                            .build()
                    )
                )
                .regulaereZeiten(
                    List.of(
                        Zeitserie.builder()
                            .tagesposition("01")
                            .tagestyp("09")
                            .beginn(LocalTime.of(17, 39, 0))
                            .ende(LocalTime.of(17, 39, 0))
                            .zusatz("Offen")
                            .build()
                    )
                )
                .abweichendeZeiten(
                    List.of(
                        Zeitraum.builder()
                            .beginn(LocalDate.of(2016, 5, 28))
                            .ende(LocalDate.of(2016, 5, 28))
                            .zusatz("Abweichungen")
                            .build()
                    )
                )
                .build()
        );

        ImportUtil.pushAndWait23(file);
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());

        OeffnungszeitValidator validator = new OeffnungszeitValidator(response);
        validator
            .validateCount(1)
            .validateTitle(0, "Öffnungszeiten")
            .validateAdditionalInformation(0, "Reguläre Zeiten", "Erster Arbeitstag (Mo-Fr) 17:39 - 17:39; Offen")
            .validateAdditionalInformation(0, "Abweichende Zeiten", "28.05.2016 - 28.05.2016; Abweichungen")
            .validateAdditionalInformation(0, "Hinweis", "Wenn wir kein Bock mehr haben machen wir auch früher zu.");
    }
}
