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
package suchdienst.integrationstest.tests.x23.trefferlistev8;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.DynamischerParameter;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulursprungsportal.UrsprungsportalModul;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Testziel: Validieren des Inhalts der /servicedescriptions/ars Trefferliste
 * <p>
 * Die Such-API (V8) kennt keinen "applyDynamicParams"-Parameter mehr: Onlinedienst-Links enthalten immer
 * dynamische Parameter. Der v7-Test parametrisiert über applyDynamicParams true/false, hier wird
 * nur noch der fest einprogrammierte Fall (immer aktiv) geprüft.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
public class TrefferlisteInhaltTest {

    private static final String RANDOM_PREFIX = TestUtil.generateRandomWord();

    private static String lbFullId;
    private static String lbId;
    private static String ars;

    private static final String BESCHREIBUNG_TM02_DE = RANDOM_PREFIX + "BeschreibungTM02DE";
    private static final String TITEL_TM03_DE = RANDOM_PREFIX + "TitelTM03DE";
    private static final String VOLLTEXT_TM06_DE = RANDOM_PREFIX + "VolltextTM06DE";
    private static final String TEASER_TM24_DE = RANDOM_PREFIX + "TeaserTM24DE";
    private static final String BEGRIFF_IM_KONTEXT_DE = RANDOM_PREFIX + "BegriffImKontextDE";
    private static final String URSPRUNGSPORTAL_TITEL_DE = RANDOM_PREFIX + "UrsprungsportalTitelDE";

    private static String oeId;

    private static final String BESCHREIBUNG_TM02_EN = RANDOM_PREFIX + "DescriptionTM02EN";
    private static final String TITEL_TM03_EN = RANDOM_PREFIX + "TitleTM03EN";
    private static final String VOLLTEXT_TM06_EN = RANDOM_PREFIX + "FulltextTM06EN";
    private static final String TEASER_TM24_EN = RANDOM_PREFIX + "TeaserTM24EN";
    private static final String BEGRIFF_IM_KONTEXT_EN = RANDOM_PREFIX + "SynonymEN";
    private static final String URSPRUNGSPORTAL_TITEL_EN = RANDOM_PREFIX + "SourcePortalTitleEN";
    public static final ArrayList<Link> linksList = new ArrayList<>();

    @BeforeAll
    static void setUpTestclass(TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testclassname).lb(1).oe(1).od(2).getMax();
        //set relevant values for trefferliste
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        List<Textmodul> textmodule = new ArrayList<>();
        Textmodul tm02 = Textmodul.builder()
            .inhalt(BESCHREIBUNG_TM02_DE)
            .inhaltEnglisch(BESCHREIBUNG_TM02_EN)
            .positionDarstellung(2)
            .leikaCode("02")
            .build();

        Textmodul tm03 = Textmodul.builder()
            .inhalt(TITEL_TM03_DE)
            .inhaltEnglisch(TITEL_TM03_EN)
            .positionDarstellung(3)
            .leikaCode("03")
            .build();

        Textmodul tm06 = Textmodul.builder()
            .inhalt(VOLLTEXT_TM06_DE)
            .inhaltEnglisch(VOLLTEXT_TM06_EN)
            .positionDarstellung(6)
            .leikaCode("06")
            .build();

        Textmodul tm24 = Textmodul.builder()
            .inhalt(TEASER_TM24_DE)
            .inhaltEnglisch(TEASER_TM24_EN)
            .positionDarstellung(24)
            .leikaCode("24")
            .build();

        textmodule.add(tm02);
        textmodule.add(tm03);
        textmodule.add(tm06);
        textmodule.add(tm24);
        lb.setTextmodule(textmodule);

        List<Suchbegriff> suchbegriffe = new ArrayList<>();
        suchbegriffe.add(
            Suchbegriff.builder().begriff(BEGRIFF_IM_KONTEXT_DE).begriffEnglisch(BEGRIFF_IM_KONTEXT_EN).build()
        );
        lb.setSuchbegriffe(suchbegriffe);

        UrsprungsportalModul ursprungsportal = lb.getUrsprungsportale().getFirst();

        ursprungsportal.setTitle(URSPRUNGSPORTAL_TITEL_DE);
        ursprungsportal.setTitleEnglisch(URSPRUNGSPORTAL_TITEL_EN);
        ursprungsportal.setUriEnglisch("www.ursprungsportalEnglish.en");

        Onlinedienst od = file.getOnlinedienste().get(0);

        Onlinedienst od2 = file.getOnlinedienste().get(1);

        modifyTestData(od, od2);

        linksList.add(od.getLinks().get(0));
        linksList.add(od.getLinks().get(1));
        linksList.add(od2.getLinks().get(0));
        linksList.add(od2.getLinks().get(1));
        ImportUtil.pushAndWait23(file);

        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Leistungsbeschreibung leistungsbeschreibung = file.getLeistungsbeschreibungen().getFirst();
        lbId = leistungsbeschreibung.getId();
        lbFullId = leistungsbeschreibung.getFullId();

        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        oeId = organisationseinheit.getId();
    }

    @DisplayName("trefferListeInhaltDE")
    @Test
    void trefferlisteInhaltDE() {
        Response responseDE = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, BESCHREIBUNG_TM02_DE);
        ServiceDescriptionResultListValidator validatorDE = new ServiceDescriptionResultListValidator(responseDE);
        validatorDE.validateContains(lbFullId);
        validatorDE.validateName(0, TITEL_TM03_DE);
        validatorDE.validateTeaser(0, TEASER_TM24_DE);
        validatorDE.validateOnlineServiceLinksCount(0, 4);

        for (int i = 0; i < linksList.size(); i++) {
            validatorDE.validateOnlineServiceLinksDescription(0, i, linksList.get(i).getBeschreibung());
            validatorDE.validateOnlineServiceLinksTitle(0, i, linksList.get(i).getTitel());
            // Onlinedienst-Links enthalten in der Beta-API immer dynamische Parameter:
            // - der Parameter-Typ 001 gibt an, dass der Parameter mit der lbId gefüllt werden soll,
            String uri = linksList.get(i).getUri() + "&lb=" + lbId;
            if (i < 2) {
                // In modifyTestData(...) wird den ersten zwei Links ein weiterer Parameter hinzugefügt:
                // - 002 gibt an, dass der ARS eingesetzt werden soll
                uri += "&ars=" + ars;
                uri += "&oe_id_dynamisch=" + oeId;
            }
            validatorDE.validateOnlineServiceLinksUri(0, i, uri);
        }
    }

    @DisplayName("trefferListeInhaltEN")
    @Test
    void trefferlisteInhaltEN() {
        Response responseEN = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            BESCHREIBUNG_TM02_EN,
            Constants.LANGUAGE_EN
        );
        ServiceDescriptionResultListValidator validatorEN = new ServiceDescriptionResultListValidator(responseEN);
        validatorEN.validateContains(lbFullId);
        validatorEN.validateName(0, TITEL_TM03_EN);
        validatorEN.validateTeaser(0, TEASER_TM24_EN);
        validatorEN.validateOnlineServiceLinksCount(0, 4);

        for (int i = 0; i < linksList.size(); i++) {
            Link link = linksList.get(i);
            validatorEN.validateOnlineServiceLinksDescription(0, i, link.getBeschreibungEnglisch());
            validatorEN.validateOnlineServiceLinksTitle(0, i, link.getTitelEnglisch());
            String uri = link.getUriEnglisch() + "?lb_en=" + lbId;
            if (i < 2) {
                // In modifyTestData(...) wird den ersten zwei Links ein weiterer Parameter hinzugefügt:
                // - 002 gibt an, dass der ARS eingesetzt werden soll
                uri += "&ars_en=" + ars;
                uri += "&oe_id_dynamisch_en=" + oeId;
            }
            validatorEN.validateOnlineServiceLinksUri(0, i, uri);
        }
    }

    private static void modifyTestData(Onlinedienst onlinedienst, Onlinedienst onlinedienst2) {
        onlinedienst.setLinks(
            List.of(
                Link.builder()
                    .uri("https://sample-od01/?q=OD_01_uri")
                    .uriEnglisch("http://od01.de/en")
                    .dynamischeParameter(
                        List.of(
                            DynamischerParameter.builder()
                                .typ("001")
                                .parameterName("lb")
                                .parameterNameEnglisch("lb_en")
                                .build(),
                            DynamischerParameter.builder()
                                .typ("002")
                                .parameterName("ars")
                                .parameterNameEnglisch("ars_en")
                                .build(),
                            DynamischerParameter.builder()
                                .typ("003")
                                .parameterName("oe_id_dynamisch")
                                .parameterNameEnglisch("oe_id_dynamisch_en")
                                .build()
                        )
                    )
                    .titel("SAMPLE Titel OD01")
                    .titelEnglisch("OD01_Title")
                    .beschreibung("SAMPLE Beschreibung OD01")
                    .beschreibungEnglisch("OD01_Description")
                    .positionDarstellung(1010)
                    .positionDarstellungEnglisch(1)
                    .build(),
                Link.builder()
                    .uri("https://sample-od01/?q=OD_02_uri")
                    .uriEnglisch("http://od02.de/en")
                    .dynamischeParameter(
                        List.of(
                            DynamischerParameter.builder()
                                .typ("001")
                                .parameterName("lb")
                                .parameterNameEnglisch("lb_en")
                                .build(),
                            DynamischerParameter.builder()
                                .typ("002")
                                .parameterName("ars")
                                .parameterNameEnglisch("ars_en")
                                .build(),
                            DynamischerParameter.builder()
                                .typ("003")
                                .parameterName("oe_id_dynamisch")
                                .parameterNameEnglisch("oe_id_dynamisch_en")
                                .build()
                        )
                    )
                    .titel("SAMPLE Titel OD02")
                    .titelEnglisch("OD02_Title")
                    .beschreibung("SAMPLE Beschreibung OD02")
                    .beschreibungEnglisch("OD02_Description")
                    .positionDarstellung(1010)
                    .positionDarstellungEnglisch(1)
                    .build()
            )
        );

        onlinedienst2.setLinks(
            List.of(
                Link.builder()
                    .uri("https://sample-od01/?q=OD_03_uri")
                    .uriEnglisch("http://od03.de/en")
                    .dynamischeParameter(
                        List.of(
                            DynamischerParameter.builder()
                                .typ("001")
                                .parameterName("lb")
                                .parameterNameEnglisch("lb_en")
                                .build()
                        )
                    )
                    .titel("SAMPLE Titel OD03")
                    .titelEnglisch("OD03_Title")
                    .beschreibung("SAMPLE Beschreibung OD03")
                    .beschreibungEnglisch("OD03_Description")
                    .positionDarstellung(1010)
                    .positionDarstellungEnglisch(1)
                    .build(),
                Link.builder()
                    .uri("https://sample-od01/?q=OD_04_uri")
                    .uriEnglisch("http://od04.de/en")
                    .dynamischeParameter(
                        List.of(
                            DynamischerParameter.builder()
                                .typ("001")
                                .parameterName("lb")
                                .parameterNameEnglisch("lb_en")
                                .build()
                        )
                    )
                    .titel("SAMPLE Titel OD04")
                    .titelEnglisch("OD04_Title")
                    .beschreibung("SAMPLE Beschreibung OD04")
                    .beschreibungEnglisch("OD04_Description")
                    .positionDarstellung(1010)
                    .positionDarstellungEnglisch(1)
                    .build()
            )
        );
    }
}
