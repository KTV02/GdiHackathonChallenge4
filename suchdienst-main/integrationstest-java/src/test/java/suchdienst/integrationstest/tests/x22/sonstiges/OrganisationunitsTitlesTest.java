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
package suchdienst.integrationstest.tests.x22.sonstiges;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Kriterium;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Rolle;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.validator.organisationunitstitlesv2.TitlesValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class OrganisationunitsTitlesTest {

    private static final int OE_COUNT = 5;
    private static final List<String> AUSPRAEGUNGEN = List.of("a", "b", "c", "d", "e");
    private static String lbId;
    private static String ars;
    private static final List<String> oeIds = new ArrayList<>();

    @BeforeAll
    static void setupData(TestInfo testInfo) {
        String testcase = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testcase).lb(1).oe(OE_COUNT).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        List<Organisationseinheit> oes = file.getOrganisationseinheiten();
        List<Zustaendigkeit> zustaendigkeiten = file.getZustaendigkeitenOrganisationseinheit();
        for (int i = 0; i < OE_COUNT; i++) {
            Organisationseinheit oe = oes.get(i);
            oe.setTitel("Titel deutsch " + (i + 1));
            Zustaendigkeit zustaendigkeit = zustaendigkeiten.get(i);
            zustaendigkeit.setRolle(
                i < 3
                    ? List.of(
                          Rolle.builder()
                              .code("0" + (i + 1))
                              .build()
                      )
                    : List.of()
            );
            zustaendigkeit.setKriterien(
                List.of(
                    Kriterium.builder()
                        .code("001")
                        .auspraegungen(AUSPRAEGUNGEN.subList(0, i + 1))
                        .build()
                )
            );
        }
        ImportUtil.pushAndWait(file);
        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        lbId = leistung.getFullId();
        oeIds.addAll(file.getOrganisationseinheiten().stream().map(Organisationseinheit::getFullId).toList());
    }

    @Test
    void titlesV2() {
        Response response = OrganisationunitsConnector.getOrganisationunitsTitlesV2(
            ars,
            lbId,
            null,
            Constants.LANGUAGE_DE
        );

        TitlesValidator validator = new TitlesValidator(response).validateCount(OE_COUNT);
        for (int i = 0; i < OE_COUNT; i++) {
            validator = validator
                .validateId(i, oeIds.get(i))
                .validateTitle(i, "Titel deutsch " + (i + 1))
                .validateCriteria(
                    i,
                    List.of(
                        TitlesValidator.Criterion.builder()
                            .name("AnfangsbuchstabeFamilienname")
                            .code("001")
                            .auspraegungen(AUSPRAEGUNGEN.subList(0, i + 1))
                            .build()
                    )
                );
        }
        validator
            .validateRole(0, "01", "Zuständige Stelle und Ansprechpunkt")
            .validateRole(1, "02", "Zuständige Stelle")
            .validateRole(2, "03", "Ansprechpunkt")
            .validateRole(3, "03", "Ansprechpunkt")
            .validateRole(4, "03", "Ansprechpunkt");
    }

    @Test
    void titlesV2FallbackKeinEnglisch() {
        Response response = OrganisationunitsConnector.getOrganisationunitsTitlesV2(
            ars,
            lbId,
            null,
            Constants.LANGUAGE_EN
        );

        TitlesValidator validator = new TitlesValidator(response).validateCount(OE_COUNT);
        for (int i = 0; i < OE_COUNT; i++) {
            validator = validator
                .validateId(i, oeIds.get(i))
                .validateTitle(i, "Titel deutsch " + (i + 1))
                .validateCriteria(
                    i,
                    List.of(
                        TitlesValidator.Criterion.builder()
                            .name("First letter family name")
                            .code("001")
                            .auspraegungen(AUSPRAEGUNGEN.subList(0, i + 1))
                            .build()
                    )
                );
        }
        validator
            .validateRole(0, "01", "Competent office and contact point")
            .validateRole(1, "02", "Competent office")
            .validateRole(2, "03", "Contact point")
            .validateRole(3, "03", "Contact point")
            .validateRole(4, "03", "Contact point");
    }

    @Test
    void oeWithMultipleLb(TestInfo testInfo) {
        XzufiFile file = XzufiFileCreator.newInstance()
            .name(TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName()))
            .lb(2)
            .oe(1)
            .zsOe(2)
            .getMin();

        String oeMultiLb1 = file.getLeistungsbeschreibungen().get(0).getFullId();
        String oeMultiLb2 = file.getLeistungsbeschreibungen().get(1).getFullId();
        String oeId = file.getOrganisationseinheiten().getFirst().getFullId();
        String ars1 = file.getZustaendigkeitenOrganisationseinheit().get(0).getArss().getFirst();
        String ars2 = file.getZustaendigkeitenOrganisationseinheit().get(1).getArss().getFirst();
        file.getOrganisationseinheiten().getFirst().setTitel("Titel von Oe mit mehreren Lbs");
        List<Zustaendigkeit> zustaendigkeiten = file.getZustaendigkeitenOrganisationseinheit();
        for (int i = 0; i < zustaendigkeiten.size(); i++) {
            file.getZustaendigkeitenOrganisationseinheit()
                .get(i)
                .setRolle(List.of(Rolle.builder().code("03").build()));
            zustaendigkeiten
                .get(i)
                .setKriterien(
                    List.of(Kriterium.builder().code("001").auspraegungen(AUSPRAEGUNGEN.subList(0, 1)).build())
                );
        }
        ImportUtil.pushAndWait(file);

        List<Response> responsesList = List.of(
            OrganisationunitsConnector.getOrganisationunitsTitlesV2(ars1, oeMultiLb1, null, Constants.LANGUAGE_DE),
            OrganisationunitsConnector.getOrganisationunitsTitlesV2(ars1, oeMultiLb2, null, Constants.LANGUAGE_DE),
            OrganisationunitsConnector.getOrganisationunitsTitlesV2(ars2, oeMultiLb1, null, Constants.LANGUAGE_DE),
            OrganisationunitsConnector.getOrganisationunitsTitlesV2(ars2, oeMultiLb2, null, Constants.LANGUAGE_DE)
        );

        for (Response response : responsesList) {
            TitlesValidator validator = new TitlesValidator(response);
            validator
                .validateCount(1)
                .validateId(0, oeId)
                .validateTitle(0, "Titel von Oe mit mehreren Lbs")
                .validateRole(0, "03", "Ansprechpunkt")
                .validateCriteria(
                    0,
                    List.of(
                        TitlesValidator.Criterion.builder()
                            .code("001")
                            .name("AnfangsbuchstabeFamilienname")
                            .auspraegungen(AUSPRAEGUNGEN.subList(0, 1))
                            .build()
                    )
                );
        }
    }
}
