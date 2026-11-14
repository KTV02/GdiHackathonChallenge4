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
package suchdienst.integrationstest.tests.sonstiges;

import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.FoCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsFormularCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsKontaktpersonCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOnlinedienstCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOrganisationseinheitCreator;
import de.dataport.pvog.testdatengenerator.model.Erreichbarkeit;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.Kontaktperson;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.connectors.RelationsConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.utils.WaitUtil;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.EmployeeValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.NestedFormsValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.OrganisationValidator;
import suchdienst.integrationstest.validator.organisationunitstitlesv2.TitlesValidator;
import suchdienst.integrationstest.validator.relations.RelationV3ResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionleikahierarchyv3.LeikaHierarchyResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionsleikaidv4.LeikaIdResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionssuggestionv1.SuggestionResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Testziel: Es wird validiert, dass Geschäftsobjekte mit einer Ars aus einer höheren Ars-Hierarchy Stufe gefunden werden, wenn man mit der Ars der Gemeinde sucht.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class ArsHierarchieTest {

    public record ArsLbValues(String ars, String lbId, String lbBeschreibung, String leika) {}

    public record ValidationValues(
        String bundeslandKuerzel,
        String lbId,
        String lbFullId,
        String lbBeschreibung,
        String leika,
        String oeId,
        String oeFullId,
        String oeTitle,
        String odId,
        String odFullId,
        String kpName,
        String kpLastname,
        String foId,
        String foUri,
        String foBeschreibung,
        String foBezeichnung,
        String arsGemeinde,
        String arsGemeindeverband,
        String arsKreis,
        String arsRegierungsbezirk,
        String arsLand,
        ArsLbValues arsLbValues
    ) {}

    public record ArsBundesland(
        Bundesland bundesland,
        String arsLand,
        String arsRegierungsbezirk,
        String arsKreis,
        String arsGemeindeverbund,
        String arsGemeinde
    ) {}

    private static final ArsBundesland arsBundeslandSH = new ArsBundesland(
        Bundesland.SH,
        "010000000000",
        "010000000000",
        "010600000000",
        "010605024000",
        "010605024012"
    );
    private static final ArsBundesland arsBundeslandHH = new ArsBundesland(
        Bundesland.HH,
        "020000000000",
        "020000000000",
        "020000000000",
        "020000000000",
        "020000000000"
    );
    private static final ArsBundesland arsBundeslandNI = new ArsBundesland(
        Bundesland.NI,
        "030000000000",
        "030000000000",
        "033510000000",
        "033510004000",
        "033510004004"
    );
    private static final ArsBundesland arsBundeslandHB = new ArsBundesland(
        Bundesland.HB,
        "040000000000",
        "040000000000",
        "040110000000",
        "040110000000",
        "040110000000"
    );
    private static final ArsBundesland arsBundeslandNW = new ArsBundesland(
        Bundesland.NW,
        "050000000000",
        "051000000000",
        "051660000000",
        "051660004000",
        "051660004004"
    );
    private static final ArsBundesland arsBundeslandHE = new ArsBundesland(
        Bundesland.HE,
        "060000000000",
        "064000000000",
        "064310000000",
        "064310017000",
        "064310017017"
    );
    private static final ArsBundesland arsBundeslandRP = new ArsBundesland(
        Bundesland.RP,
        "070000000000",
        "073000000000",
        "073310000000",
        "073315001000",
        "073315001001"
    );
    private static final ArsBundesland arsBundeslandBW = new ArsBundesland(
        Bundesland.BW,
        "080000000000",
        "081000000000",
        "081170000000",
        "081170010000",
        "081170010010"
    );
    private static final ArsBundesland arsBundeslandBY = new ArsBundesland(
        Bundesland.BY,
        "090000000000",
        "091000000000",
        "091740000000",
        "091740111000",
        "091740111111"
    );
    private static final ArsBundesland arsBundeslandSL = new ArsBundesland(
        Bundesland.SL,
        "100000000000",
        "100000000000",
        "100460000000",
        "100460111000",
        "100460111111"
    );
    private static final ArsBundesland arsBundeslandBE = new ArsBundesland(
        Bundesland.BE,
        "110000000000",
        "110000000000",
        "110000000000",
        "110000000000",
        "110000000000"
    );
    private static final ArsBundesland arsBundeslandBB = new ArsBundesland(
        Bundesland.BB,
        "120000000000",
        "120000000000",
        "120710000000",
        "120715101000",
        "120715101028"
    );
    private static final ArsBundesland arsBundeslandMV = new ArsBundesland(
        Bundesland.MV,
        "130000000000",
        "130000000000",
        "130760000000",
        "130765652000",
        "130765652009"
    );
    private static final ArsBundesland arsBundeslandSN = new ArsBundesland(
        Bundesland.SN,
        "140000000000",
        "146000000000",
        "146260000000",
        "146265220000",
        "146265220150"
    );
    private static final ArsBundesland arsBundeslandST = new ArsBundesland(
        Bundesland.ST,
        "150000000000",
        "150000000000",
        "150890000000",
        "150895051000",
        "150895051041"
    );
    private static final ArsBundesland arsBundeslandTH = new ArsBundesland(
        Bundesland.TH,
        "160000000000",
        "160000000000",
        "160740000000",
        "160745005000",
        "160745005012"
    );
    private static final List<ArsBundesland> arsBundeslaender = List.of(
        arsBundeslandSH,
        arsBundeslandHH,
        arsBundeslandNI,
        arsBundeslandHB,
        arsBundeslandNW,
        arsBundeslandHE,
        arsBundeslandRP,
        arsBundeslandBW,
        arsBundeslandBY,
        arsBundeslandSL,
        arsBundeslandBE,
        arsBundeslandBB,
        arsBundeslandMV,
        arsBundeslandSN,
        arsBundeslandST,
        arsBundeslandTH
    );

    private static List<ValidationValues> testData = new ArrayList<>();

    @BeforeAll
    static void setup(TestInfo testInfo) {
        ElasticsearchConnector.removeAllData();
        //warte auf Löschen des Datenbestands
        WaitUtil.waitFor(1, TimeUnit.SECONDS);

        Map.Entry<List<XzufiFile>, List<ValidationValues>> testDataEntry = createTestData(testInfo);
        testData = testDataEntry.getValue();
        importTestData(testDataEntry.getKey());
    }

    // Es wird validiert, dass mit der Ars der Gemeinde gesucht wird, aber auch die Daten aus den hoeheren Ars-Hierarchy Stufen gefunden werden. Dies wird für jede Stufe durch getestet.
    @DisplayName("ARS-Hierarchie - Trefferliste")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProviderHierarchy")
    void arsHierarchieTrefferliste(ValidationValues arsValidationValues) {
        // Für jede ARS werden die Testdaten in dem TestData Record gespeichert.
        String lbId = arsValidationValues.lbFullId();
        String lbBeschreibung = arsValidationValues.lbBeschreibung();
        String arsGemeinde = arsValidationValues.arsGemeinde();

        // V7 SD
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(arsGemeinde, lbBeschreibung);
        ServiceDescriptionResultListValidator validatorServiceDescriptions = new ServiceDescriptionResultListValidator(
            response
        );
        validatorServiceDescriptions.validateContains(lbId);
    }

    @DisplayName("ARS-Hierarchie - Exakte Suche - Trefferliste")
    @ParameterizedTest(name = "{displayName}: {0} {1}")
    @MethodSource("testDataProviderExact")
    void arsHierarchieTrefferlisteExakteSuche(List<String> searchArs, ValidationValues arsValidationValues) {
        // Für jede ARS werden die Testdaten in dem TestData Record gespeichert.

        //Testfall für das nicht finden der Lbs, wenn nicht die exakte Ars angegeben wird und die ArsHierarchy Suche deaktiviert wird.
        searchArs.forEach(ars -> {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(
                ars,
                arsValidationValues.arsLbValues().lbBeschreibung,
                false
            );
            ServiceDescriptionResultListValidator validatorServiceDescriptions =
                new ServiceDescriptionResultListValidator(response);
            validatorServiceDescriptions.validateCount(0);
        });
        //Testfall für das Finden der Lb, wenn die exakte Ars angegeben wird und die ArsHierarchy Suche deaktiviert wird.
        String exactArs = arsValidationValues.arsLbValues().ars();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(
            exactArs,
            arsValidationValues.arsLbValues().lbBeschreibung,
            false
        );
        ServiceDescriptionResultListValidator validatorServiceDescriptions = new ServiceDescriptionResultListValidator(
            response
        );
        validatorServiceDescriptions.validateCount(1);
    }

    @DisplayName("ARS-Hierarchie - Detailseite")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProviderHierarchy")
    void arsHierarchieDetailsuche(ValidationValues arsValidationValues) {
        // Für jede ARS werden die Testdaten in dem TestData Record gespeichert.
        String lbId = arsValidationValues.lbFullId();
        String oeFullId = arsValidationValues.oeFullId();
        String odFullId = arsValidationValues.odFullId();
        String arsGemeinde = arsValidationValues.arsGemeinde();
        // V5 Details
        Response responseDetail = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsGemeinde, lbId);
        ServiceDescriptionValidator validatorDetail = new ServiceDescriptionValidator(responseDetail);
        validatorDetail.validateId(lbId).validateOrganisationIdsContains(oeFullId);
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(responseDetail);
        onlineServiceLinkValidator.validateId(0, odFullId);
    }

    @DisplayName("ARS-Hierarchie - Exakte Suche - Detailseite LB")
    @ParameterizedTest(name = "{displayName}: {0} {1}")
    @MethodSource("testDataProviderExact")
    void arsHierarchieDetailsExakteSuche(List<String> searchArs, ValidationValues arsValidationValues) {
        //Testfall für das nicht finden der Lbs, wenn nicht die exakte Ars angegeben wird und die ArsHierarchy Suche deaktiviert wird.
        searchArs.forEach(ars -> {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                ars,
                arsValidationValues.arsLbValues().lbId(),
                false
            );
            ServiceDescriptionValidator validatorServiceDescriptions = new ServiceDescriptionValidator(response);
            validatorServiceDescriptions.validateNotFound();
        });
    }

    @DisplayName("ARS-Hierarchie - Detailseite OE")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProviderHierarchy")
    void arsHierarchieOrganisationUnitsDetailsV4(ValidationValues arsValidationValues) {
        // Für jede ARS werden die Testdaten in dem TestData Record gespeichert.
        String lbId = arsValidationValues.lbFullId();
        String oeFullId = arsValidationValues.oeFullId();
        String oeTitlte = arsValidationValues.oeTitle();
        String kpName = arsValidationValues.kpName();
        String kpLastName = arsValidationValues.kpLastname();
        String arsGemeinde = arsValidationValues.arsGemeinde();
        // V4 OE Details
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(oeFullId, arsGemeinde, lbId);
        OrganisationValidator validatorOe = new OrganisationValidator(responseOE);
        validatorOe.validateTitle(oeTitlte);
        EmployeeValidator validatorEmployee = new EmployeeValidator(responseOE);
        validatorEmployee
            .validateCount(1)
            .validateName(0, kpName)
            .validateLastname(0, kpLastName)
            .validateRelationLbId(0, 0, lbId);
    }

    @DisplayName("ARS-Hierarchie - Detailseite OE")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProviderHierarchy")
    void arsHierarchieOrganisationUnitsDetailsV5(ValidationValues arsValidationValues) {
        // Für jede ARS werden die Testdaten in dem TestData Record gespeichert.
        String lbId = arsValidationValues.lbFullId();
        String oeFullId = arsValidationValues.oeFullId();
        String oeTitlte = arsValidationValues.oeTitle();
        String kpName = arsValidationValues.kpName();
        String kpLastName = arsValidationValues.kpLastname();
        String arsGemeinde = arsValidationValues.arsGemeinde();
        String foFullID = arsValidationValues.foId();
        String foBeschreibung = arsValidationValues.foBeschreibung();
        String foBezeichnung = arsValidationValues.foBezeichnung();
        String foUri = arsValidationValues.foUri();
        // V4 OE Details
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(oeFullId, arsGemeinde, lbId);
        OrganisationValidator validatorOe = new OrganisationValidator(responseOE);
        validatorOe.validateTitle(oeTitlte);
        EmployeeValidator validatorEmployee = new EmployeeValidator(responseOE);
        validatorEmployee
            .validateCount(1)
            .validateName(0, kpName)
            .validateLastname(0, kpLastName)
            .validateRelationLbId(0, 0, lbId);
        NestedFormsValidator nestedFormsValidator = new NestedFormsValidator(responseOE);
        nestedFormsValidator
            .validateCount(1)
            .validateId(0, foFullID)
            .validateDescription(0, foBeschreibung)
            .validateName(0, foBezeichnung)
            .validateUri(0, foUri);
    }

    @DisplayName("ARS-Hierarchie - Detailseite OE Titel")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProviderHierarchy")
    void arsHierarchieOrganisationTitlesV2(ValidationValues arsValidationValues) {
        // Für jede ARS werden die Testdaten in dem TestData Record gespeichert.
        String lbId = arsValidationValues.lbFullId();
        String oeTitlte = arsValidationValues.oeTitle();
        String arsGemeinde = arsValidationValues.arsGemeinde();
        // V2 OE Titles
        Response responseTitles = OrganisationunitsConnector.getOrganisationunitsTitlesV2(arsGemeinde, lbId);
        TitlesValidator validatorTitles = new TitlesValidator(responseTitles);
        validatorTitles.validateTitle(0, oeTitlte);
    }

    @DisplayName("ARS-Hierarchie - Leika-ID Suche")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProviderHierarchy")
    void arsHierarchieServiceDescriptionsLeikaV3(ValidationValues arsValidationValues) {
        // Für jede ARS werden die Testdaten in dem TestData Record gespeichert.
        String lbId = arsValidationValues.lbFullId();
        String lbLeika = arsValidationValues.leika();
        String arsGemeinde = arsValidationValues.arsGemeinde();
        // V3 Leika ID
        Response responseLeika = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(arsGemeinde, lbLeika);
        LeikaIdResultListValidator validatorLeika = new LeikaIdResultListValidator(responseLeika);
        validatorLeika.validateContains(lbId);
    }

    @DisplayName("ARS-Hierarchie - Exakte Suche - Leika-ID Suche")
    @ParameterizedTest(name = "{displayName}: {0} {1}")
    @MethodSource("testDataProviderExact")
    void arsHierarchieServiceDescriptionsLeikaV3ExakteSuche(
        List<String> searchArs,
        ValidationValues arsValidationValues
    ) {
        //Testfall für das nicht finden der Lbs, wenn nicht die exakte Ars angegeben wird und die ArsHierarchy Suche deaktiviert wird.
        searchArs.forEach(ars -> {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(
                ars,
                arsValidationValues.arsLbValues().leika(),
                false
            );
            ServiceDescriptionValidator validatorServiceDescriptions = new ServiceDescriptionValidator(response);
            validatorServiceDescriptions.validateEmptyContent();
        });
    }

    @DisplayName("ARS-Hierarchie - Leika-ID Hierarchie")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProviderHierarchy")
    void arsHierarchieServiceDescriptionsLeikaHierarchyV2(ValidationValues arsValidationValues) {
        // Für jede ARS werden die Testdaten in dem TestData Record gespeichert.
        String lbId = arsValidationValues.lbFullId();
        String lbLeika = arsValidationValues.leika();
        String arsGemeinde = arsValidationValues.arsGemeinde();
        // V2 Leika Hierarchy
        Response responseLeikaHierarchy = ServiceDescriptionConnector.getServiceDescriptionsLeikaHierarchyV3(
            arsGemeinde,
            lbLeika
        );
        LeikaHierarchyResultListValidator validatorLeikaHierarchy = new LeikaHierarchyResultListValidator(
            responseLeikaHierarchy
        );
        validatorLeikaHierarchy.validateContains(lbId);
    }

    @DisplayName("ARS-Hierarchie - Autovervollständigung")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProviderHierarchy")
    void arsHierarchyServiceDescriptionsSuggestionsV1(ValidationValues arsValidationValues) {
        // Für jede ARS werden die Testdaten in dem TestData Record gespeichert.
        String lbBeschreibung = arsValidationValues.lbBeschreibung();
        String arsGemeinde = arsValidationValues.arsGemeinde();
        // V1 SD Suggestions
        Response responseSugg = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            arsGemeinde,
            lbBeschreibung,
            false
        );
        SuggestionResultListValidator validatorSugg = new SuggestionResultListValidator(responseSugg);
        List<String> expectedSuggestions = List.of(lbBeschreibung);
        validatorSugg.validateListContains(expectedSuggestions);
    }

    @DisplayName("ARS-Hierarchie - JZUFI-Relations")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProviderHierarchy")
    void relationsJzufiV3(ValidationValues arsValidationValues) {
        // Für jede ARS werden die Testdaten in dem TestData Record gespeichert.
        String lbFullId = arsValidationValues.lbFullId();
        String lbId = arsValidationValues.lbId;
        String arsGemeinde = arsValidationValues.arsGemeinde();
        Response response = RelationsConnector.getRelationsLbIdsArsV3(
            List.of(lbFullId),
            arsGemeinde,
            true,
            Constants.XzufiVersion.V2_2_0
        );

        RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
        validator.validateCount(2);
        validator.validateParentIdValue(0, arsValidationValues.odId);
        validator.validateIdValue(0, lbId + "." + arsValidationValues.odId + "." + arsValidationValues.arsLbValues.ars);
        validator.validateParentIdValue(1, arsValidationValues.oeId);
        validator.validateIdValue(1, lbId + "." + arsValidationValues.oeId + "." + arsValidationValues.arsLbValues.ars);
    }

    @DisplayName("ARS-Hierarchie - Exakte Suche - JZUFI-Relations")
    @ParameterizedTest(name = "{displayName}: {0} {1}")
    @MethodSource("testDataProviderExact")
    void relationsJzufiV3ExakteSuche(List<String> searchArs, ValidationValues arsValidationValues) {
        //Testfall für das nicht finden der Lbs, wenn nicht die exakte Ars angegeben wird und die ArsHierarchy Suche deaktiviert wird.
        IntStream.range(0, searchArs.size()).forEach(i -> {
            Response response = RelationsConnector.getRelationsLbIdsArsV3(
                List.of(arsValidationValues.lbFullId()),
                searchArs.get(i),
                false,
                Constants.XzufiVersion.V2_2_0
            );
            RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
            validator.validateCount(0);
        });
    }

    private static Map.Entry<List<XzufiFile>, List<ValidationValues>> createTestData(TestInfo testInfo) {
        // In der testData Map werden die ARS als Key und die jeweiligen genutzten Testdaten gespeichert.
        String testcasename = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        List<XzufiFile> files = new ArrayList<>();
        List<ValidationValues> testData = new ArrayList<>();
        for (ArsBundesland arsBundesland : arsBundeslaender) {
            BundeslandConfig bundeslandConfig = new BundeslandConfig(arsBundesland.bundesland);

            // 5 Leistungsbeschreibungen, 5 OEs und ihre Zuständigkeiten, 5 ODs und ihre Zuständigkeiten, 5 KPs und ihre Zuständigkeiten, damit man die ARS-Hierarchy nachbilden kann.
            // Jede LB hat genau eine OE & OD & KP sowie ZSOE & ZSOD & ZSKP mit der gleichen ARS. Dadurch entsteht die ARS-Hierarchie. um mit der Gemeinde-ARS alle GOs zu finden.
            XzufiFile file = XzufiFileCreator.newInstance(bundeslandConfig)
                .name(testcasename)
                .lb(5)
                .oe(5)
                .zsOe(1)
                .od(5)
                .zsOd(1)
                .getMin();

            // Alle Zuständigkeiten werden gelöscht, da der Testdatengenerator 25 dieser erstellt und es leichter ist, sie neu zu erstellen, als alle neu anzupassen.
            file.getZustaendigkeitenOrganisationseinheit().clear();
            file.getZustaendigkeitenOnlinedienst().clear();
            file.getZustaendigkeitenKontaktperson().clear();

            List<ValidationValues> validationValues = setTestDataAndReturnValidationValues(
                arsBundesland,
                bundeslandConfig,
                file
            );
            testData.addAll(validationValues);

            files.add(file);
        }
        return Map.entry(files, testData);
    }

    private static List<ValidationValues> setTestDataAndReturnValidationValues(
        ArsBundesland arsBundesland,
        BundeslandConfig bundeslandConfig,
        XzufiFile file
    ) {
        List<ValidationValues> validationValues = new ArrayList<>();

        // Liste aus allen Ars Hierarchien wird erstellt.
        List<String> arsList = List.of(
            arsBundesland.arsLand,
            arsBundesland.arsRegierungsbezirk,
            arsBundesland.arsKreis,
            arsBundesland.arsGemeindeverbund,
            arsBundesland.arsGemeinde
        );

        // In dem for loop werden nun die Daten der XzuFi gesetzt. Für jede Ars der Hierarchy werden die Zuständigkeiten angepasst.
        // Die Daten werden erstellt / angepasst, da sie relevant für die Validierung in den Endpunkten sind.
        int i = 0;
        int leikaCounter = 10;
        for (String ars : arsList) {
            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
            SimpleDateFormat dateFormat = new SimpleDateFormat("ddHHmm");
            String leikaId = "9900" + leikaCounter + "05" + dateFormat.format(timestamp);

            String randomPrefix = TestUtil.generateRandomWord();
            Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().get(i);
            Organisationseinheit oe = file.getOrganisationseinheiten().get(i);
            Onlinedienst od = file.getOnlinedienste().get(i);

            Zustaendigkeit zustaendigkeitOe = ZsOrganisationseinheitCreator.getInstance(bundeslandConfig).getMin(
                lb.getId(),
                oe.getId()
            );
            zustaendigkeitOe.getArss().clear();
            zustaendigkeitOe.getArss().add(ars);

            Zustaendigkeit zustaendigkeitOd = ZsOnlinedienstCreator.getInstance(bundeslandConfig).getMin(
                lb.getId(),
                od.getId()
            );
            zustaendigkeitOd.getArss().clear();
            zustaendigkeitOd.getArss().add(ars);

            String oeId = oe.getId();
            String odId = od.getId();

            file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOe);
            file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOd);

            String kpId = "KP_" + ars;
            oe.getKontaktpersonen().clear();
            oe.getKontaktpersonen().add(
                Kontaktperson.builder()
                    .id(kpId)
                    .vorname("Name" + ars)
                    .familienname("Familie" + ars)
                    .raum("001")
                    .erreichbarkeiten(
                        List.of(
                            Erreichbarkeit.builder().code("01").kennung("kontaktperson@person.de").build(),
                            Erreichbarkeit.builder().code("02").kennung("04913374201").build()
                        )
                    )
                    .schemeAgencyId(bundeslandConfig.getSchemeAgencyId())
                    .build()
            );

            Zustaendigkeit zustaendigkeitKp = ZsKontaktpersonCreator.getInstance(bundeslandConfig).getMin(
                lb.getId(),
                kpId
            );
            zustaendigkeitKp.getArss().clear();
            zustaendigkeitKp.getArss().add(ars);
            file.getZustaendigkeitenKontaktperson().add(zustaendigkeitKp);

            String beschreibungTM02 = randomPrefix + "trefferlisteBeschreibung" + ars;
            Textmodul tm02 = Textmodul.builder()
                .inhalt(beschreibungTM02)
                .positionDarstellung(2)
                .leikaCode("02")
                .build();
            lb.getTextmodule().clear();
            lb.getTextmodule().add(tm02);

            lb.setReferenzLeiKa(leikaId);
            String lbLeika = lb.getReferenzLeiKa();

            ArsLbValues arsLbValues = new ArsLbValues(ars, lb.getFullId(), beschreibungTM02, lb.getReferenzLeiKa());

            // Formulare Bauen
            Formular nestedForm = FoCreator.getInstance().getMin("Ars_Hierachie_Formular");
            nestedForm.getLink().setUri("https://gidf.de/fo_full_orga_data_" + ars);
            nestedForm.setBezeichnung("Name FO_full_orga_data " + ars);
            nestedForm.setBeschreibung("Beschreibung FO_full_orga_data " + ars);
            nestedForm.setMimeCode("MimeCode FO_full_orga_data " + ars);

            Zustaendigkeit zsNestedForm = ZsFormularCreator.getInstance(bundeslandConfig).getMin(
                lb.getId(),
                nestedForm.getId()
            );
            zsNestedForm.getArss().clear();
            zsNestedForm.getArss().add(ars);

            nestedForm.getZustaendigkeiten().add(zsNestedForm);
            oe.getFormulare().add(nestedForm);

            ValidationValues validationData = new ValidationValues(
                arsBundesland.bundesland().name(),
                lb.getId(),
                lb.getFullId(),
                beschreibungTM02,
                lbLeika,
                oeId,
                oe.getFullId(),
                oe.getTitel(),
                odId,
                od.getFullId(),
                oe.getKontaktpersonen().getFirst().getVorname(),
                oe.getKontaktpersonen().getFirst().getFamilienname(),
                oe.getFormulare().getFirst().getFullId(),
                oe.getFormulare().getFirst().getLink().getUri(),
                oe.getFormulare().getFirst().getBeschreibung(),
                oe.getFormulare().getFirst().getBezeichnung(),
                arsBundesland.arsGemeinde,
                arsBundesland.arsGemeindeverbund,
                arsBundesland.arsKreis,
                arsBundesland.arsRegierungsbezirk,
                arsBundesland.arsLand,
                arsLbValues
            );

            validationValues.add(validationData);
            leikaCounter++;
            i++;
        }
        return validationValues;
    }

    static Stream<Arguments> testDataProviderHierarchy() {
        return testData
            .stream()
            .map(t ->
                arguments(
                    named(
                        String.format(
                            "Bundesland: '%s', Land: '%s', Regierungsbezirk '%s', Kreis: '%s', Gemeindeverband '%s', Gemeinde: '%s'",
                            t.bundeslandKuerzel(),
                            t.arsLand(),
                            t.arsRegierungsbezirk(),
                            t.arsKreis(),
                            t.arsGemeindeverband(),
                            t.arsGemeinde()
                        ),
                        t
                    )
                )
            );
    }

    static Stream<Arguments> testDataProviderExact() {
        return testData
            .stream()
            .map(t ->
                arguments(
                    named(
                        String.format("Bundesland: '%s', Such-ARS: %s", t.bundeslandKuerzel(), createArsList(t)),
                        createArsList(t)
                    ),
                    named(
                        String.format(
                            "Testfall: Land: '%s', Regierungsbezirk '%s', Kreis: '%s', Gemeindeverband '%s', Gemeinde: '%s'",
                            t.arsLand(),
                            t.arsRegierungsbezirk(),
                            t.arsKreis(),
                            t.arsGemeindeverband(),
                            t.arsGemeinde()
                        ),
                        t
                    )
                )
            );
    }

    /**
     * In der Methode "createArsList" wird eine Liste aus den verschiedenen Ars Hierarchie Ebenen erstellt, nur die die in der Xzufi genutzt wird, wird ausgelassen.
     * Dadurch wird in den Testfällen validiert, dass bei einer exakten Suche die Informationen aus der Xzufi nicht gefunden werden, wenn man mit einer anderen Ars aus dem gleichen Bundesland sucht.
     */
    private static List<String> createArsList(ValidationValues arsValidationValues) {
        // Hier wird die Ars Liste für ein Redaktionssystem erstellt, die Liste wird genutzt
        String arsGemeinde = arsValidationValues.arsGemeinde();
        String arsGemeindeverband = arsValidationValues.arsGemeindeverband();
        String arsKreis = arsValidationValues.arsKreis();
        String arsRegierungsbezirk = arsValidationValues.arsRegierungsbezirk();
        String arsLand = arsValidationValues.arsLand();
        Set<String> searchArs = new LinkedHashSet<>();
        searchArs.add(arsGemeinde);
        searchArs.add(arsGemeindeverband);
        searchArs.add(arsKreis);
        searchArs.add(arsRegierungsbezirk);
        searchArs.add(arsLand);

        // Entferne die Ars aus der Ars Hierachie-Liste, die in der XzuFi vorhanden ist.
        searchArs.remove(arsValidationValues.arsLbValues().ars());

        return List.copyOf(searchArs);
    }

    private static void importTestData(List<XzufiFile> files) {
        // In der testData Map werden die ARS als Key und die jeweiligen genutzen Testdaten gespeichert.
        String importId = null;
        for (XzufiFile file : files) {
            importId = ImportUtil.push(file);
        }
        ImportUtil.waitForImport(importId);
    }
}
