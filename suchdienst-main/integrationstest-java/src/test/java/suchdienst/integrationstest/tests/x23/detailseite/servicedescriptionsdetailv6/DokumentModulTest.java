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
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.Modul;
import de.dataport.pvog.testdatengenerator.model.modultypen.moduldokument.Dokument;
import de.dataport.pvog.testdatengenerator.model.modultypen.moduldokument.Dokumentmodul;
import io.restassured.response.Response;
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
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.DokumentModulValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.TextmodulValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class DokumentModulTest {

    @Test
    void modulInLB(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Dokumentmodul modul = Dokumentmodul.builder()
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
            .ausgehend(
                List.of(
                    Dokument.builder()
                        .artCode("004")
                        .bezeichnung("Ausgehendes Dokument Bezeichnung")
                        .beschreibung("Ausgehendes Dokument Beschreibung")
                        .referenzFormularId(List.of("Ausgehend_Dokument_Formular_ID"))
                        .signaturCode("004")
                        .build()
                )
            )
            .eingehend(
                List.of(
                    Dokument.builder()
                        .artCode("001")
                        .bezeichnung("Eingehendes Dokument Bezeichnung")
                        .beschreibung("Eingehendes Dokument Beschreibung")
                        .referenzFormularId(List.of("Eingehend_Dokument_Formular_ID"))
                        .signaturCode("004")
                        .build()
                )
            )
            .beschreibungEingehend("Beschreibung des DokumenteModules.")
            .links(
                List.of(
                    Link.builder()
                        .uri("http://www.dokument.de/")
                        .titel("Dokument_uri_Titel")
                        .beschreibung("Beschreibung des Links")
                        .build()
                )
            )
            .positionDarstellung(0)
            .build();
        leistung.setDokumentmodule(List.of(modul));

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        DokumentModulValidator validator = new DokumentModulValidator(response);
        validator
            .validateType("_moduleDetail")
            .validateTitle("Dokumente")
            .validateText(null)
            .validateCode("96")
            // beschreibungEingehend ist gefüllt → Links wandern ins Textmodul 08, Dokumentmodul hat keine Links
            .validateLinkCount(0)
            .validateDetailsCount(2)
            .validateDetailsTitle(0, "Beizubringendes Dokument Eingehendes Dokument Bezeichnung")
            .validateDetailsText(0, "Eingehendes Dokument Beschreibung")
            .validateDetailsLinkCount(0, 0)
            .validateDetailsAdditionalInformationCount(0, 2)
            .validateDetailsAdditionalInformationByKey(0, "Dokumententyp", "Antrag")
            .validateDetailsAdditionalInformationByKey(0, "Erforderliche Signatur", "Handschriftliche Signatur")
            .validateDetailsTitle(1, "Hervorgehendes Dokument Ausgehendes Dokument Bezeichnung")
            .validateDetailsText(1, "Ausgehendes Dokument Beschreibung")
            .validateDetailsLinkCount(1, 0)
            .validateDetailsAdditionalInformationCount(1, 2)
            .validateDetailsAdditionalInformationByKey(1, "Dokumententyp", "Bescheid")
            .validateDetailsAdditionalInformationByKey(1, "Erforderliche Signatur", "Handschriftliche Signatur")
            .validateAccessChannel(Modul.ZugangskanalCode.ONLINE.name());

        // Textmodul 08 (Erforderliche Unterlagen) – erzeugt aus beschreibungEingehend des Dokumentmoduls
        TextmodulValidator textmodulValidator = new TextmodulValidator(response);
        textmodulValidator
            .validateTextByCode("08", "Beschreibung des DokumenteModules.")
            .validateTitleByCode("08", "Erforderliche Unterlagen")
            .validateLinkCountByCode("08", 1)
            .validateLinkUriByCode("08", 0, "http://www.dokument.de/")
            .validateLinkTitleByCode("08", 0, "Dokument_uri_Titel")
            .validateLinkDescriptionByCode("08", 0, "Beschreibung des Links");
    }

    @Test
    void modulInLBEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).zsOe(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Dokumentmodul modul = Dokumentmodul.builder()
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE_OFFLINE)
            .ausgehend(
                List.of(
                    Dokument.builder()
                        .artCode("004")
                        .bezeichnung("Ausgehendes Dokument Bezeichnung")
                        .bezeichnungEnglisch("Outgoing Document EN")
                        .beschreibung("Ausgehendes Dokument Beschreibung")
                        .beschreibungEnglisch("Outgoing Document Description EN")
                        .referenzFormularId(List.of("Ausgehend_Dokument_Formular_ID"))
                        .signaturCode("004")
                        .build()
                )
            )
            .eingehend(
                List.of(
                    Dokument.builder()
                        .artCode("001")
                        .bezeichnung("Eingehendes Dokument Bezeichnung")
                        .bezeichnungEnglisch("Incoming Document EN")
                        .beschreibung("Eingehendes Dokument Beschreibung")
                        .beschreibungEnglisch("Incoming Document Description EN")
                        .referenzFormularId(List.of("Eingehend_Dokument_Formular_ID"))
                        .signaturCode("004")
                        .build()
                )
            )
            .beschreibungEingehend("Beschreibung des DokumenteModules.")
            .beschreibungEingehendEnglisch("Document Description")
            .links(
                List.of(
                    Link.builder()
                        .uri("http://www.dokument.de/")
                        .uriEnglisch("http://www.document.en/")
                        .titel("Dokument_uri_Titel")
                        .titelEnglisch("Document Link Titel EN")
                        .beschreibung("Beschreibung des Links")
                        .beschreibungEnglisch("Document Link Description EN")
                        .build()
                )
            )
            .positionDarstellung(0)
            .build();
        leistung.setDokumentmodule(List.of(modul));

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        DokumentModulValidator validator = new DokumentModulValidator(response);
        validator
            .validateType("_moduleDetail")
            .validateTitle("Documents")
            .validateText(null)
            .validateCode("96")
            // beschreibungEingehend ist gefüllt → Links wandern ins Textmodul 08, Dokumentmodul hat keine Links
            .validateLinkCount(0)
            .validateDetailsCount(2)
            .validateDetailsTitle(0, "Required document Incoming Document EN")
            .validateDetailsText(0, "Incoming Document Description EN")
            .validateDetailsLinkCount(0, 0)
            .validateDetailsAdditionalInformationCount(0, 2)
            // TODO: Die Dokumententypen kommen seit Xzufi 2.3 aus der Codeliste
            // urn:xoev-de:fim-datenfelder:codeliste:dokumentart
            // Diese liegt bislang nur in Deutsch vor. Darum hier "Antrag" statt "Request"
            .validateDetailsAdditionalInformationByKey(0, "Document type", "Antrag")
            .validateDetailsAdditionalInformationByKey(0, "Required signature", "handwritten signature")
            .validateDetailsTitle(1, "Resulting document Outgoing Document EN")
            .validateDetailsText(1, "Outgoing Document Description EN")
            .validateDetailsLinkCount(1, 0)
            .validateDetailsAdditionalInformationCount(1, 2)
            // TODO: Die Dokumententypen kommen seit Xzufi 2.3 aus der Codeliste
            // urn:xoev-de:fim-datenfelder:codeliste:dokumentart
            // Diese liegt bislang nur in Deutsch vor. Darum hier "Bescheid" statt "Notice"
            .validateDetailsAdditionalInformationByKey(1, "Document type", "Bescheid")
            .validateDetailsAdditionalInformationByKey(1, "Required signature", "handwritten signature")
            .validateAccessChannel(Modul.ZugangskanalCode.ONLINE_OFFLINE.name());

        // Textmodul 08 (Erforderliche Unterlagen) – erzeugt aus beschreibungEingehend des Dokumentmoduls
        TextmodulValidator textmodulValidator = new TextmodulValidator(response);
        textmodulValidator
            .validateTextByCode("08", "Document Description")
            .validateTitleByCode("08", "Required documents")
            .validateLinkCountByCode("08", 1)
            .validateLinkUriByCode("08", 0, "http://www.document.en/")
            .validateLinkTitleByCode("08", 0, "Document Link Titel EN")
            .validateLinkDescriptionByCode("08", 0, "Document Link Description EN");
    }

    @Test
    void modulInSp(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Dokumentmodul.builder()
                            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE_OFFLINE)
                            .ausgehend(
                                List.of(
                                    Dokument.builder()
                                        .artCode("004")
                                        .bezeichnung("Ausgehendes Dokument Bezeichnung spez")
                                        .beschreibung("Ausgehendes Dokument Beschreibung spez")
                                        .referenzFormularId(List.of("Ausgehend_Dokument_Formular_ID_spez"))
                                        .signaturCode("004")
                                        .build()
                                )
                            )
                            .eingehend(
                                List.of(
                                    Dokument.builder()
                                        .artCode("001")
                                        .bezeichnung("Eingehendes Dokument Bezeichnung spez")
                                        .beschreibung("Eingehendes Dokument Beschreibung spez")
                                        .referenzFormularId(List.of("Eingehend_Dokument_Formular_ID_spez"))
                                        .signaturCode("004")
                                        .build()
                                )
                            )
                            .beschreibungEingehend("Beschreibung des DokumenteModules spez.")
                            .links(
                                List.of(
                                    Link.builder()
                                        .uri("http://www.dokumentSpez.de/")
                                        .titel("Dokument_uri_Titel_spez")
                                        .beschreibung("Beschreibung des Links spez")
                                        .build()
                                )
                            )
                            .positionDarstellung(0)
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        DokumentModulValidator validator = new DokumentModulValidator(response);
        validator
            .validateType("_moduleDetail")
            .validateTitle("Dokumente")
            .validateText(null)
            .validateCode("96")
            // beschreibungEingehend ist gefüllt → Links wandern ins Textmodul 08, Dokumentmodul hat keine Links
            .validateLinkCount(0)
            .validateDetailsCount(2)
            .validateDetailsTitle(0, "Beizubringendes Dokument Eingehendes Dokument Bezeichnung spez")
            .validateDetailsText(0, "Eingehendes Dokument Beschreibung spez")
            .validateDetailsLinkCount(0, 0)
            .validateDetailsAdditionalInformationCount(0, 2)
            .validateDetailsAdditionalInformationByKey(0, "Dokumententyp", "Antrag")
            .validateDetailsAdditionalInformationByKey(0, "Erforderliche Signatur", "Handschriftliche Signatur")
            .validateDetailsTitle(1, "Hervorgehendes Dokument Ausgehendes Dokument Bezeichnung spez")
            .validateDetailsText(1, "Ausgehendes Dokument Beschreibung spez")
            .validateDetailsLinkCount(1, 0)
            .validateDetailsAdditionalInformationCount(1, 2)
            .validateDetailsAdditionalInformationByKey(1, "Dokumententyp", "Bescheid")
            .validateDetailsAdditionalInformationByKey(1, "Erforderliche Signatur", "Handschriftliche Signatur")
            .validateAccessChannel(Modul.ZugangskanalCode.ONLINE_OFFLINE.name());

        // Textmodul 08 (Erforderliche Unterlagen) – erzeugt aus beschreibungEingehend des Dokumentmoduls
        TextmodulValidator textmodulValidator = new TextmodulValidator(response);
        textmodulValidator
            .validateTextByCode("08", "Beschreibung des DokumenteModules spez.")
            .validateTitleByCode("08", "Erforderliche Unterlagen")
            .validateLinkCountByCode("08", 1)
            .validateLinkUriByCode("08", 0, "http://www.dokumentSpez.de/")
            .validateLinkTitleByCode("08", 0, "Dokument_uri_Titel_spez")
            .validateLinkDescriptionByCode("08", 0, "Beschreibung des Links spez");
    }

    @Test
    void modulInSPEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Dokumentmodul.builder()
                            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE_OFFLINE)
                            .ausgehend(
                                List.of(
                                    Dokument.builder()
                                        .artCode("004")
                                        .bezeichnung("Ausgehendes Dokument Bezeichnung")
                                        .bezeichnungEnglisch("Outgoing Document EN")
                                        .beschreibung("Ausgehendes Dokument Beschreibung")
                                        .beschreibungEnglisch("Outgoing Document Description EN")
                                        .referenzFormularId(List.of("Ausgehend_Dokument_Formular_ID"))
                                        .signaturCode("004")
                                        .build()
                                )
                            )
                            .eingehend(
                                List.of(
                                    Dokument.builder()
                                        .artCode("001")
                                        .bezeichnung("Eingehendes Dokument Bezeichnung")
                                        .bezeichnungEnglisch("Incoming Document EN")
                                        .beschreibung("Eingehendes Dokument Beschreibung")
                                        .beschreibungEnglisch("Incoming Document Description EN")
                                        .referenzFormularId(List.of("Eingehend_Dokument_Formular_ID"))
                                        .signaturCode("004")
                                        .build()
                                )
                            )
                            .beschreibungEingehend("Beschreibung des DokumenteModules.")
                            .beschreibungEingehendEnglisch("Document Description")
                            .links(
                                List.of(
                                    Link.builder()
                                        .uri("http://www.dokument.de/")
                                        .uriEnglisch("http://www.document.en/")
                                        .titel("Dokument_uri_Titel")
                                        .titelEnglisch("Document Link Titel EN")
                                        .beschreibung("Beschreibung des Links")
                                        .beschreibungEnglisch("Document Link Description EN")
                                        .build()
                                )
                            )
                            .positionDarstellung(0)
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        DokumentModulValidator validator = new DokumentModulValidator(response);
        validator
            .validateType("_moduleDetail")
            .validateTitle("Documents")
            .validateText(null)
            .validateCode("96")
            // beschreibungEingehend ist gefüllt → Links wandern ins Textmodul 08, Dokumentmodul hat keine Links
            .validateLinkCount(0)
            .validateDetailsCount(2)
            .validateDetailsTitle(0, "Required document Incoming Document EN")
            .validateDetailsText(0, "Incoming Document Description EN")
            .validateDetailsLinkCount(0, 0)
            .validateDetailsAdditionalInformationCount(0, 2)
            // TODO: Die Dokumententypen kommen seit Xzufi 2.3 aus der Codeliste
            // urn:xoev-de:fim-datenfelder:codeliste:dokumentart
            // Diese liegt bislang nur in Deutsch vor. Darum hier "Antrag" statt "Request"
            .validateDetailsAdditionalInformationByKey(0, "Document type", "Antrag")
            .validateDetailsAdditionalInformationByKey(0, "Required signature", "handwritten signature")
            .validateDetailsTitle(1, "Resulting document Outgoing Document EN")
            .validateDetailsText(1, "Outgoing Document Description EN")
            .validateDetailsLinkCount(1, 0)
            .validateDetailsAdditionalInformationCount(1, 2)
            // TODO: Die Dokumententypen kommen seit Xzufi 2.3 aus der Codeliste
            // urn:xoev-de:fim-datenfelder:codeliste:dokumentart
            // Diese liegt bislang nur in Deutsch vor. Darum hier "Bescheid" statt "Notice"
            .validateDetailsAdditionalInformationByKey(1, "Document type", "Bescheid")
            .validateDetailsAdditionalInformationByKey(1, "Required signature", "handwritten signature")
            .validateAccessChannel(Modul.ZugangskanalCode.ONLINE_OFFLINE.name());

        // Textmodul 08 (Erforderliche Unterlagen) – erzeugt aus beschreibungEingehend des Dokumentmoduls
        TextmodulValidator textmodulValidator = new TextmodulValidator(response);
        textmodulValidator
            .validateTextByCode("08", "Document Description")
            .validateTitleByCode("08", "Required documents")
            .validateLinkCountByCode("08", 1)
            .validateLinkUriByCode("08", 0, "http://www.document.en/")
            .validateLinkTitleByCode("08", 0, "Document Link Titel EN")
            .validateLinkDescriptionByCode("08", 0, "Document Link Description EN");
    }
}
