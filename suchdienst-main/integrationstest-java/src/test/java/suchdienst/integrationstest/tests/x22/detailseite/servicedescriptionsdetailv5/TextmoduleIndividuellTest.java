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

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.modultextindividuell.TextmodulIndividuell;
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
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.TextmodulIndividuellValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
@SuppressWarnings("java:S1192")
class TextmoduleIndividuellTest {

    @Test
    void dataInLb(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        List<TextmodulIndividuell> module = new ArrayList<>();
        module.add(
            TextmodulIndividuell.builder()
                .code("009")
                .codeName("Honorar")
                .codelist("urn:xoev-de:fim:codeliste:xzufi.kostentyp")
                .inhalt("Inhalt Textmodul Individuell")
                .positionDarstellung(0)
                .build()
        );
        module.add(
            TextmodulIndividuell.builder()
                .code("010")
                .codeName("CodeIstVorhanden")
                .codelist("urn:xoev-de:fim:codeliste:xzufi.kostentyp")
                .inhalt("TextmodulIndividuell_Code_vorhanden")
                .positionDarstellung(70)
                .build()
        );
        module.add(
            TextmodulIndividuell.builder()
                .code("UnbekannterCode")
                .codeName("MeinIndividuellerTextZuUnbekanntemCode")
                .codelist("xzufi:individuellesTextmodultyp")
                .inhalt("TextmodulIndividuell_Code_Nicht_vorhanden")
                .positionDarstellung(71)
                .build()
        );
        leistung.setTextmoduleIndividuell(module);

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        // Validierung
        TextmodulIndividuellValidator validator = new TextmodulIndividuellValidator(response);
        validator
            .validateType(0, "_module")
            .validateTitle(0, "Honorar")
            .validateText(0, "Inhalt Textmodul Individuell")
            .validateCode(0, "009")
            .validateLinkCount(0, 0)
            .validateAdditionalInformationCount(0, 0)
            // known issue PV-13064 .validateReplace(0, false)
            .validateFilled(0, true)
            .validatePlaceholder(0, false)
            .validateType(1, "_module")
            .validateTitle(1, "Lohn")
            .validateText(1, "TextmodulIndividuell_Code_vorhanden")
            .validateCode(1, "010")
            .validateLinkCount(1, 0)
            .validateAdditionalInformationCount(1, 0)
            // known issue PV-13064 .validateReplace(1, false)
            .validateFilled(1, true)
            .validatePlaceholder(1, false)
            .validateType(2, "_module")
            .validateTitle(2, "MeinIndividuellerTextZuUnbekanntemCode")
            .validateText(2, "TextmodulIndividuell_Code_Nicht_vorhanden")
            .validateCode(2, "UnbekannterCode")
            .validateLinkCount(2, 0)
            .validateAdditionalInformationCount(2, 0)
            // known issue PV-13064 .validateReplace(2, false)
            .validateFilled(2, true)
            .validatePlaceholder(2, false);
    }

    @Test
    void dataInLbEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        List<TextmodulIndividuell> module = new ArrayList<>();
        module.add(
            TextmodulIndividuell.builder()
                .code("009")
                .codeName("Honorar")
                .codelist("urn:xoev-de:fim:codeliste:xzufi.kostentyp")
                .inhalt("Inhalt Textmodul Individuell")
                .inhaltEnglisch("Content Textmodul Individuell EN")
                .positionDarstellung(0)
                .build()
        );
        module.add(
            TextmodulIndividuell.builder()
                .code("010")
                .codeName("CodeIstVorhanden")
                .codelist("urn:xoev-de:fim:codeliste:xzufi.kostentyp")
                .inhalt("TextmodulIndividuell_Code_vorhanden")
                .inhaltEnglisch("TextmodulIndividuell_Code_exists")
                .positionDarstellung(70)
                .build()
        );
        module.add(
            TextmodulIndividuell.builder()
                .code("UnbekannterCode")
                .codeName("MeinIndividuellerTextZuUnbekanntemCode")
                .codelist("xzufi:individuellesTextmodultyp")
                .inhalt("TextmodulIndividuell_Code_Nicht_vorhanden")
                .inhaltEnglisch("TextmodulIndividuell_doesnot_Code_exist")
                .positionDarstellung(71)
                .build()
        );
        leistung.setTextmoduleIndividuell(module);

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        // Validierung
        TextmodulIndividuellValidator validator = new TextmodulIndividuellValidator(response);
        validator
            .validateType(0, "_module")
            .validateTitle(0, "charge")
            .validateText(0, "Content Textmodul Individuell EN")
            .validateCode(0, "009")
            .validateLinkCount(0, 0)
            .validateAdditionalInformationCount(0, 0)
            // known issue PV-13064 .validateReplace(0, false)
            .validateFilled(0, true)
            .validatePlaceholder(0, false)
            .validateType(1, "_module")
            .validateTitle(1, "wage")
            .validateText(1, "TextmodulIndividuell_Code_exists")
            .validateCode(1, "010")
            .validateLinkCount(1, 0)
            .validateAdditionalInformationCount(1, 0)
            // known issue PV-13064 .validateReplace(1, false)
            .validateFilled(1, true)
            .validatePlaceholder(1, false)
            .validateType(2, "_module")
            .validateTitle(2, "MeinIndividuellerTextZuUnbekanntemCode")
            .validateText(2, "TextmodulIndividuell_doesnot_Code_exist")
            .validateCode(2, "UnbekannterCode")
            .validateLinkCount(2, 0)
            .validateAdditionalInformationCount(2, 0)
            // known issue PV-13064 .validateReplace(2, false)
            .validateFilled(2, true)
            .validatePlaceholder(2, false);
    }

    @Test
    void dataInSp(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        TextmodulIndividuell.builder()
                            .code("009")
                            .codeName("Honorar")
                            .codelist("urn:xoev-de:fim:codeliste:xzufi.kostentyp")
                            .inhalt("Inhalt Textmodul Individuell spez")
                            .positionDarstellung(0)
                            .build()
                    )
                    .build()
            )
        );

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait(file);
        String ars = spezialisierung.getArs().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        // Validierung
        TextmodulIndividuellValidator validator = new TextmodulIndividuellValidator(response);
        validator
            .validateType(0, "_module")
            .validateTitle(0, "Honorar")
            .validateText(0, "Inhalt Textmodul Individuell spez")
            .validateCode(0, "009")
            .validateLinkCount(0, 0)
            .validateAdditionalInformationCount(0, 0)
            // known issue PV-13064 .validateReplace(0, true)
            .validateFilled(0, true)
            .validatePlaceholder(0, false);
    }

    @Test
    void dataInSpEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        leistung.getTextmoduleIndividuell().clear();
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        TextmodulIndividuell.builder()
                            .code("009")
                            .codeName("Honorar")
                            .codelist("urn:xoev-de:fim:codeliste:xzufi.kostentyp")
                            .inhalt("Inhalt Textmodul Individuell spez")
                            .inhaltEnglisch("Spec Content Textmodul Individuell EN")
                            .positionDarstellung(0)
                            .build()
                    )
                    .build()
            )
        );

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait(file);
        String ars = spezialisierung.getArs().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        // Validierung
        TextmodulIndividuellValidator validator = new TextmodulIndividuellValidator(response);
        validator
            .validateType(0, "_module")
            .validateTitle(0, "charge")
            .validateText(0, "Spec Content Textmodul Individuell EN")
            .validateCode(0, "009")
            .validateLinkCount(0, 0)
            .validateAdditionalInformationCount(0, 0)
            // known issue PV-13064 .validateReplace(0, true)
            .validateFilled(0, true)
            .validatePlaceholder(0, false);
    }
}
