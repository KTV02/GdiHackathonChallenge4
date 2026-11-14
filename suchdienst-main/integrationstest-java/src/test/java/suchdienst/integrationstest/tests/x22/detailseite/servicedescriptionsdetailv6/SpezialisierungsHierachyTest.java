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
package suchdienst.integrationstest.tests.x22.detailseite.servicedescriptionsdetailv6;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.SpCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.FristStichtag;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulbearbeitungsdauer.Bearbeitungsdauermodul;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.Amount;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.KostenVariabel;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.Kostenmodul;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.BearbeitungsdauermodulValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.KostenmodulValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.TextmodulValidator;

@SuppressWarnings({ "FieldMayBeFinal", "CanBeFinal" })
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class SpezialisierungsHierachyTest {

    private static Map<List<String>, XzufiFile> data_Ersetzen_EmptyLb = new HashMap<>();
    private static Map<List<String>, XzufiFile> data_Ersetzen_NotEmptyLb = new HashMap<>();
    private static Map<List<String>, XzufiFile> data_NotErsetzen_EmptyLb = new HashMap<>();
    private static Map<List<String>, XzufiFile> data_NotErsetzen_NotEmptyLb = new HashMap<>();

    @BeforeAll
    static void setup(TestInfo testInfo) {
        BundeslandConfig bundeslandConfig = new BundeslandConfig(
            Bundesland.BY,
            "L100042",
            "090000000000",
            List.of("090000000000")
        );
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        Set<List<String>> arsCombinations = getArsCombinations(
            List.of("093775347141", "093775347000", "093770000000", "093000000000", "090000000000")
        );
        XzufiFile importFile = new XzufiFile();

        int counter = 0;
        for (List<String> arss : arsCombinations) {
            counter++;

            XzufiFile file1 = createTestdata(
                testclassname + "_Ersetzen_EmptyLb_Ars" + counter,
                bundeslandConfig,
                arss,
                true,
                true
            );
            data_Ersetzen_EmptyLb.put(arss, file1);
            importFile.getLeistungsbeschreibungen().addAll(file1.getLeistungsbeschreibungen());
            importFile.getOrganisationseinheiten().addAll(file1.getOrganisationseinheiten());
            importFile.getOnlinedienste().addAll(file1.getOnlinedienste());
            importFile.getSpezialisierungen().addAll(file1.getSpezialisierungen());
            importFile.getZustaendigkeitenOnlinedienst().addAll(file1.getZustaendigkeitenOnlinedienst());
            importFile
                .getZustaendigkeitenOrganisationseinheit()
                .addAll(file1.getZustaendigkeitenOrganisationseinheit());
            importFile.getZustaendigkeitenKontaktperson().addAll(file1.getZustaendigkeitenKontaktperson());

            XzufiFile file2 = createTestdata(
                testclassname + "_Ersetzen_NotEmptyLb_Ars" + counter,
                bundeslandConfig,
                arss,
                true,
                false
            );
            data_Ersetzen_NotEmptyLb.put(arss, file2);
            importFile.getLeistungsbeschreibungen().addAll(file2.getLeistungsbeschreibungen());
            importFile.getOrganisationseinheiten().addAll(file2.getOrganisationseinheiten());
            importFile.getOnlinedienste().addAll(file2.getOnlinedienste());
            importFile.getSpezialisierungen().addAll(file2.getSpezialisierungen());
            importFile.getZustaendigkeitenOnlinedienst().addAll(file2.getZustaendigkeitenOnlinedienst());
            importFile
                .getZustaendigkeitenOrganisationseinheit()
                .addAll(file2.getZustaendigkeitenOrganisationseinheit());
            importFile.getZustaendigkeitenKontaktperson().addAll(file2.getZustaendigkeitenKontaktperson());

            XzufiFile file3 = createTestdata(
                testclassname + "_NotErsetzen_EmptyLb_Ars" + counter,
                bundeslandConfig,
                arss,
                false,
                true
            );
            data_NotErsetzen_EmptyLb.put(arss, file3);
            importFile.getLeistungsbeschreibungen().addAll(file3.getLeistungsbeschreibungen());
            importFile.getOrganisationseinheiten().addAll(file3.getOrganisationseinheiten());
            importFile.getOnlinedienste().addAll(file3.getOnlinedienste());
            importFile.getSpezialisierungen().addAll(file3.getSpezialisierungen());
            importFile.getZustaendigkeitenOnlinedienst().addAll(file3.getZustaendigkeitenOnlinedienst());
            importFile
                .getZustaendigkeitenOrganisationseinheit()
                .addAll(file3.getZustaendigkeitenOrganisationseinheit());
            importFile.getZustaendigkeitenKontaktperson().addAll(file3.getZustaendigkeitenKontaktperson());

            XzufiFile file4 = createTestdata(
                testclassname + "_NotErsetzen_NotEmptyLb_Ars" + counter,
                bundeslandConfig,
                arss,
                false,
                false
            );
            data_NotErsetzen_NotEmptyLb.put(arss, file4);
            importFile.getLeistungsbeschreibungen().addAll(file4.getLeistungsbeschreibungen());
            importFile.getOrganisationseinheiten().addAll(file4.getOrganisationseinheiten());
            importFile.getOnlinedienste().addAll(file4.getOnlinedienste());
            importFile.getSpezialisierungen().addAll(file4.getSpezialisierungen());
            importFile.getZustaendigkeitenOnlinedienst().addAll(file4.getZustaendigkeitenOnlinedienst());
            importFile
                .getZustaendigkeitenOrganisationseinheit()
                .addAll(file4.getZustaendigkeitenOrganisationseinheit());
            importFile.getZustaendigkeitenKontaktperson().addAll(file4.getZustaendigkeitenKontaktperson());
        }

        ImportUtil.pushAndWait(importFile);
    }

    @DisplayName("textmodul")
    @ParameterizedTest(name = "{displayName} {index}")
    @MethodSource("provideHierachyData")
    void textmodul(XzufiFile file, boolean ersetzung, boolean emptyLB) {
        // Daten von der SuD API abfragen
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            "093775347141",
            leistung.getFullId()
        );

        // Validierung
        StringBuilder expectedInhalt = new StringBuilder();
        if (ersetzung) {
            Spezialisierung spezialisierung = file.getSpezialisierungen().getLast();
            Textmodul textmodul = (Textmodul) spezialisierung.getModulSpezialisierungen().getFirst().getModul();
            expectedInhalt.append(textmodul.getInhalt());
        } else {
            if (!emptyLB) {
                expectedInhalt.append("Textmodul14");
            }
            for (Spezialisierung spezialisierung : file.getSpezialisierungen()) {
                Textmodul textmodul = (Textmodul) spezialisierung.getModulSpezialisierungen().getFirst().getModul();
                expectedInhalt.append(textmodul.getInhalt());
            }
        }
        TextmodulValidator validator = new TextmodulValidator(response);
        validator
            .validateCount(3)
            .validateCode(0, "06")
            .validateTitle(0, "Beschreibung")
            .validateText(0, "Textmodul06")
            .validateCode(1, "14")
            .validateTitle(1, "Formulare")
            .validateText(1, expectedInhalt.toString())
            .validateCode(2, "03")
            .validateTitle(2, "Leistungsbezeichnung II (Leistungsname)")
            .validateText(2, "Textmodul03");
    }

    @DisplayName("bearbeitungsmodul")
    @ParameterizedTest(name = "{displayName} {index}")
    @MethodSource("provideHierachyData")
    void bearbeitungsmodul(XzufiFile file, boolean ersetzung, boolean emptyLB) {
        // Daten von der SuD API abfragen
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            "093775347141",
            leistung.getFullId()
        );

        // Validierung
        StringBuilder expectedBeschreibungmodul = new StringBuilder();
        List<String> expectedBeschreibung = new ArrayList<>();
        if (ersetzung) {
            Spezialisierung spezialisierung = file.getSpezialisierungen().getLast();
            Bearbeitungsdauermodul bearbeitungsdauermodul = (Bearbeitungsdauermodul) spezialisierung
                .getModulSpezialisierungen()
                .get(1)
                .getModul();
            expectedBeschreibungmodul.append(bearbeitungsdauermodul.getBeschreibung());
            expectedBeschreibung.add(bearbeitungsdauermodul.getBearbeitungsdauer().getFirst().getBeschreibungen());
        } else {
            if (!emptyLB) {
                expectedBeschreibungmodul.append("Beschreibung Bearbeitungsdauermodul");
                expectedBeschreibung.add("Beschreibung Bearbeitungsdauer");
            }
            for (Spezialisierung spezialisierung : file.getSpezialisierungen()) {
                Bearbeitungsdauermodul bearbeitungsdauermodul = (Bearbeitungsdauermodul) spezialisierung
                    .getModulSpezialisierungen()
                    .get(1)
                    .getModul();
                expectedBeschreibungmodul.append(bearbeitungsdauermodul.getBeschreibung());
                expectedBeschreibung.add(bearbeitungsdauermodul.getBearbeitungsdauer().getFirst().getBeschreibungen());
            }
        }

        BearbeitungsdauermodulValidator validator = new BearbeitungsdauermodulValidator(response);
        validator
            .validateCount(1)
            .validateTitle("Bearbeitungsdauer")
            .validateType("_moduleDetail")
            .validateText(expectedBeschreibungmodul.toString())
            .validateLinkCount(0)
            .validateCode("95")
            .validateDetailsCount(expectedBeschreibung.size());

        int index = 0;
        for (String beschreibung : expectedBeschreibung) {
            validator
                .validateDetailsTitle(index, beschreibung)
                .validateDetailsText(index, "26.10.2001 - 26.10.2021")
                .validateDetailsLinkCount(index, 0)
                // known issue PV-13064 .validateDetailsCode(index, "95")
                .validateDetailsAdditionalInformationCount(index, 0);
            index++;
        }
    }

    @DisplayName("kostenmodul")
    @ParameterizedTest(name = "{displayName} {index}")
    @MethodSource("provideHierachyData")
    void kostenmodul(XzufiFile file, boolean ersetzung, boolean emptyLB) {
        // Daten von der SuD API abfragen
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            "093775347141",
            leistung.getFullId()
        );

        // Validierung
        StringBuilder expectedBeschreibungmodul = new StringBuilder();
        List<String> expectedBeschreibung = new ArrayList<>();
        if (ersetzung) {
            Spezialisierung spezialisierung = file.getSpezialisierungen().getLast();
            Kostenmodul kostenmodul = (Kostenmodul) spezialisierung.getModulSpezialisierungen().get(2).getModul();
            expectedBeschreibungmodul.append(kostenmodul.getBeschreibung());
            KostenVariabel kosten = (KostenVariabel) kostenmodul.getKosten().getFirst();
            expectedBeschreibung.add(kosten.getBeschreibungenVariabilitaet());
        } else {
            if (!emptyLB) {
                expectedBeschreibungmodul.append("Beschreibung Kostenmodul");
                expectedBeschreibung.add("Beschreibung Kosten");
            }
            for (Spezialisierung spezialisierung : file.getSpezialisierungen()) {
                Kostenmodul kostenmodul = (Kostenmodul) spezialisierung.getModulSpezialisierungen().get(2).getModul();
                expectedBeschreibungmodul.append(kostenmodul.getBeschreibung());
                KostenVariabel kosten = (KostenVariabel) kostenmodul.getKosten().getFirst();
                expectedBeschreibung.add(kosten.getBeschreibungenVariabilitaet());
            }
        }

        KostenmodulValidator validator = new KostenmodulValidator(response, false);
        validator
            .validateCount(1)
            .validateTitle("Kosten & Gebühren")
            .validateType("_moduleDetail")
            .validateText(expectedBeschreibungmodul.toString())
            .validateLinkCount(0)
            .validateCode("98")
            .validateDetailsCount(expectedBeschreibung.size());

        int index = 0;
        for (String beschreibung : expectedBeschreibung) {
            validator
                .validateDetailsTitle(index, "Abgabe: 42,00 Euro - 420,00 Euro")
                .validateDetailsLinkCount(index, 0)
                // known issue PV-13064 .validateDetailsCode(index, "001")
                .validateDetailsAdditionalInformationCount(index, 1)
                .validateDetailsAdditionalInformation(index, 0, beschreibung);
            index++;
        }
    }

    private static Stream<Arguments> provideHierachyData() {
        List<Arguments> result = new ArrayList<>();

        for (List<String> arss : data_Ersetzen_EmptyLb.keySet()) {
            result.add(Arguments.of(data_Ersetzen_EmptyLb.get(arss), true, true));
        }
        for (List<String> arss : data_Ersetzen_NotEmptyLb.keySet()) {
            result.add(Arguments.of(data_Ersetzen_NotEmptyLb.get(arss), true, false));
        }
        for (List<String> arss : data_NotErsetzen_EmptyLb.keySet()) {
            result.add(Arguments.of(data_NotErsetzen_EmptyLb.get(arss), false, true));
        }
        for (List<String> arss : data_NotErsetzen_NotEmptyLb.keySet()) {
            result.add(Arguments.of(data_NotErsetzen_NotEmptyLb.get(arss), false, false));
        }

        return result.stream();
    }

    private static Set<List<String>> getArsCombinations(List<String> arss) {
        Set<List<String>> result = new HashSet<>();

        result.add(new ArrayList<>(arss));

        if (arss.size() > 1) {
            for (String ars : arss) {
                List<String> subArss = new ArrayList<>(arss);
                subArss.remove(ars);
                result.addAll(getArsCombinations(subArss));
            }
        }

        return result;
    }

    private static XzufiFile createTestdata(
        String name,
        BundeslandConfig bundeslandConfig,
        List<String> arss,
        boolean ersetzung,
        boolean emptyLB
    ) {
        XzufiFile file = XzufiFileCreator.newInstance(bundeslandConfig).name(name).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        List<Textmodul> textmodule = leistung.getTextmodule();
        textmodule.clear();
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(2).build());

        if (!emptyLB) {
            textmodule.add(Textmodul.builder().leikaCode("14").inhalt("Textmodul14").positionDarstellung(145).build());
            leistung.setBearbeitungsdauermodule(
                List.of(
                    Bearbeitungsdauermodul.builder()
                        .positionDarstellung(4)
                        .beschreibung("Beschreibung Bearbeitungsdauermodul")
                        .bearbeitungsdauer(
                            List.of(
                                FristStichtag.builder()
                                    .beschreibungen("Beschreibung Bearbeitungsdauer")
                                    .positionDarstellung(8)
                                    .vonJahr(2001)
                                    .vonMonat(10)
                                    .vonTag(26)
                                    .bisJahr(2021)
                                    .bisMonat(10)
                                    .bisTag(26)
                                    .build()
                            )
                        )
                        .build()
                )
            );
            leistung.setKostenmodule(
                List.of(
                    Kostenmodul.builder()
                        .positionDarstellung(4)
                        .beschreibung("Beschreibung Kostenmodul")
                        .kosten(
                            List.of(
                                KostenVariabel.builder()
                                    .codeTyp("001")
                                    .betragUntergrenze(
                                        Amount.builder().currencyCode("EUR").value(new BigDecimal(42)).build()
                                    )
                                    .betragObergrenze(
                                        Amount.builder().currencyCode("EUR").value(new BigDecimal(420)).build()
                                    )
                                    .beschreibungenVariabilitaet("Beschreibung Kosten")
                                    .build()
                            )
                        )
                        .build()
                )
            );
        }

        String leistungsId = file.getLeistungsbeschreibungen().getFirst().getId();
        for (String ars : arss) {
            Spezialisierung spezialisierung = SpCreator.getInstance(bundeslandConfig).getMin(name, leistungsId);
            spezialisierung.setArs(List.of(ars));
            List<ModulSpezialisierung> module = new ArrayList<>();
            module.add(
                ModulSpezialisierung.builder()
                    .ersetzung(ersetzung)
                    .modul(Textmodul.builder().leikaCode("14").inhalt("Textmodul14Spez").positionDarstellung(1).build())
                    .build()
            );
            module.add(
                ModulSpezialisierung.builder()
                    .ersetzung(ersetzung)
                    .modul(
                        Bearbeitungsdauermodul.builder()
                            .positionDarstellung(4)
                            .beschreibung("Beschreibung Bearbeitungsdauermodul Spez")
                            .bearbeitungsdauer(
                                List.of(
                                    FristStichtag.builder()
                                        .beschreibungen("Beschreibung Bearbeitungsdauer Spez")
                                        .positionDarstellung(8)
                                        .vonJahr(2001)
                                        .vonMonat(10)
                                        .vonTag(26)
                                        .bisJahr(2021)
                                        .bisMonat(10)
                                        .bisTag(26)
                                        .build()
                                )
                            )
                            .build()
                    )
                    .build()
            );
            module.add(
                ModulSpezialisierung.builder()
                    .ersetzung(ersetzung)
                    .modul(
                        Kostenmodul.builder()
                            .positionDarstellung(4)
                            .beschreibung("Beschreibung Kostenmodul Spez")
                            .kosten(
                                List.of(
                                    KostenVariabel.builder()
                                        .codeTyp("001")
                                        .betragUntergrenze(
                                            Amount.builder().currencyCode("EUR").value(new BigDecimal(42)).build()
                                        )
                                        .betragObergrenze(
                                            Amount.builder().currencyCode("EUR").value(new BigDecimal(420)).build()
                                        )
                                        .beschreibungenVariabilitaet("Beschreibung Kosten Spez")
                                        .build()
                                )
                            )
                            .build()
                    )
                    .build()
            );
            spezialisierung.setModulSpezialisierungen(module);
            file.getSpezialisierungen().add(spezialisierung);
        }

        return file;
    }
}
