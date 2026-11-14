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
package suchdienst.integrationstest.tests.x23.sonstiges;

import static de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland.BUND;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.FoCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsFormularCreator;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.utils.WaitUtil;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.EmployeeValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.NestedFormsValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.OrganisationValidator;
import suchdienst.integrationstest.validator.organisationunitstitlesv2.TitlesValidator;
import suchdienst.integrationstest.validator.servicedescriptionleikahierarchyv3.LeikaHierarchyResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.FormsValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionsleikaidv4.LeikaIdResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionssuggestionv1.SuggestionResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Ziel des Tests ist es zu prüfen, dass eine Bundesleistung auch in den einzelenen Bundesländern gesucht und gefunden werden können.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class BundLeistungTest {

    private static final BundeslandConfig bundeslandConfig = new BundeslandConfig(BUND);

    private static Leistungsbeschreibung lb;
    private static String lbId;
    private static String beschreibungTM02;
    private static String oeFullId;
    private static String oeTitle;
    private static String odFullId;
    private static String foFullId;
    private static String kpName;
    private static String kpLastname;

    private static String foName;
    private static String foDescription;
    private static String foUri;

    @BeforeAll
    static void setup(TestInfo testInfo) {
        ElasticsearchConnector.removeAllData();
        //warte auf Löschen des Datenbestands
        WaitUtil.waitFor(1, TimeUnit.SECONDS);

        String randomPrefix = TestUtil.generateRandomWord();
        String testcasename = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(bundeslandConfig)
            .name(testcasename)
            .lb(1)
            .oe(1)
            .od(1)
            .fo(1)
            .getMax();

        lb = file.getLeistungsbeschreibungen().getFirst();
        lb.setSchemeAgencyId("B100019");
        lb.setGueltigkeitGebietIDs(List.of("000000000000"));

        Formular nestedForm = FoCreator.getInstance().getMin(testcasename);
        Zustaendigkeit zsNestedForm = ZsFormularCreator.getInstance(bundeslandConfig).getMin(
            lb.getId(),
            nestedForm.getId()
        );
        nestedForm.getZustaendigkeiten().add(zsNestedForm);
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        organisationseinheit.getFormulare().add(nestedForm);

        beschreibungTM02 = randomPrefix + "trefferlisteBeschreibungTM02";
        Textmodul tm02 = Textmodul.builder().inhalt(beschreibungTM02).positionDarstellung(2).leikaCode("02").build();
        lb.setTextmodule(List.of(tm02));

        oeFullId = organisationseinheit.getFullId();
        oeTitle = organisationseinheit.getTitel();

        kpName = organisationseinheit.getKontaktpersonen().getFirst().getVorname();
        kpLastname = organisationseinheit.getKontaktpersonen().getFirst().getFamilienname();

        foName = organisationseinheit.getFormulare().getFirst().getBezeichnung();
        foDescription = organisationseinheit.getFormulare().getFirst().getBeschreibung();
        foUri = organisationseinheit.getFormulare().getFirst().getLink().getUri();

        odFullId = file.getOnlinedienste().getFirst().getFullId();
        foFullId = file.getFormulare().getFirst().getFullId();
        lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(file);
    }

    @AfterAll
    static void cleanUp() {
        ElasticsearchConnector.removeAllData();
    }

    @ParameterizedTest
    @MethodSource("laenderArss")
    void bundLeistungServiceDescriptions(String ars) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, beschreibungTM02);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    @ParameterizedTest
    @MethodSource("laenderArss")
    void bundLeistungDetails(String ars) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, lbId);
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(lbId).validateOrganisationIdsContains(oeFullId);
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateId(0, odFullId);
        FormsValidator formsValidator = new FormsValidator(response);
        formsValidator.validateCount(1);
        formsValidator.validateId(0, foFullId);
    }

    @ParameterizedTest
    @MethodSource("laenderArss")
    void bundLeistungOrganisationUnit(String ars) {
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(oeFullId, ars, lbId);
        OrganisationValidator validatorOe = new OrganisationValidator(responseOE);
        validatorOe.validateTitle(oeTitle);
        EmployeeValidator validator1 = new EmployeeValidator(responseOE);
        validator1
            .validateCount(1)
            .validateName(0, kpName)
            .validateLastname(0, kpLastname)
            .validateRelationLbId(0, 0, lbId)
            .validateRelationArs(0, 0, bundeslandConfig.getBasicArs());
    }

    @ParameterizedTest
    @MethodSource("laenderArss")
    void bundLeistungOrganisationUnitV5(String ars) {
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(oeFullId, ars, lbId);
        OrganisationValidator validatorOe = new OrganisationValidator(responseOE);
        validatorOe.validateTitle(oeTitle);
        EmployeeValidator validator1 = new EmployeeValidator(responseOE);
        validator1
            .validateCount(1)
            .validateName(0, kpName)
            .validateLastname(0, kpLastname)
            .validateRelationLbId(0, 0, lbId)
            .validateRelationArs(0, 0, bundeslandConfig.getBasicArs());

        NestedFormsValidator validator2 = new NestedFormsValidator(responseOE);
        validator2.validateCount(1).validateName(0, foName).validateDescription(0, foDescription).validateUri(0, foUri);
    }

    @ParameterizedTest
    @MethodSource("laenderArss")
    void bundLeistungOrganisationTitles(String ars) {
        Response response = OrganisationunitsConnector.getOrganisationunitsTitlesV2(ars, lbId);
        TitlesValidator validator = new TitlesValidator(response);
        validator.validateTitle(0, oeTitle);
    }

    @ParameterizedTest
    @MethodSource("laenderArss")
    void bundLeistungLeikaSearch(String ars) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, lb.getReferenzLeiKa());
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateContains(lbId);
    }

    @ParameterizedTest
    @MethodSource("laenderArss")
    void bundLeistungLeikaHierarchy(String ars) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaHierarchyV3(
            ars,
            lb.getReferenzLeiKa()
        );
        LeikaHierarchyResultListValidator validator = new LeikaHierarchyResultListValidator(response);
        validator.validateContains(lbId);
    }

    @ParameterizedTest
    @MethodSource("laenderArss")
    void bundLeistungSdSuggestions(String ars) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            beschreibungTM02,
            false
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        List<String> expectedSuggestions = List.of(beschreibungTM02);
        validator.validateListContains(expectedSuggestions);
    }

    private static Stream<Arguments> laenderArss() {
        return Stream.of(
            Arguments.of("010535318029"),
            Arguments.of("022060218218"),
            Arguments.of("033615401013"),
            Arguments.of("040110424424"),
            Arguments.of("051660012012"),
            Arguments.of("064320012012"),
            Arguments.of("071325003019"),
            Arguments.of("083175010096"),
            Arguments.of("091855155123"),
            Arguments.of("100410512512"),
            Arguments.of("110050005005"),
            Arguments.of("120625205293"),
            Arguments.of("130715158173"),
            Arguments.of("145235125150"),
            Arguments.of("150810135135"),
            Arguments.of("160615013018")
        );
    }
}
