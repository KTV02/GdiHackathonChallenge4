/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.clienttest.fixture;

import de.dataport.pvog.testdatengenerator.validator.Xzufi23Validator;
import de.xoev.xzufi.v2_3_1.AnyXMLContent;
import de.xoev.xzufi.v2_3_1.BinaryObject;
import de.xoev.xzufi.v2_3_1.CodeLeiKaLeistungsgruppierung;
import de.xoev.xzufi.v2_3_1.CodeLeiKaVerrichtungskennung;
import de.xoev.xzufi.v2_3_1.FormulardateiBinaer;
import de.xoev.xzufi.v2_3_1.FormulardateiLink;
import de.xoev.xzufi.v2_3_1.FormulardateiXML;
import de.xoev.xzufi.v2_3_1.LeistungsstrukturObjektMitVerrichtungUndDetail;
import de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert;
import de.xoev.xzufi.v2_3_1.SchreibeObjekt;
import de.xoev.xzufi.v2_3_1.Sprachversion;
import de.xoev.xzufi.v2_3_1.StringLocalized;
import de.xoev.xzufi.v2_3_1.TransferOperation050502;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.SAXException;

/**
 * Ergänzt die bestehende Generatorfixture um Felder, die dessen DSL nicht anbietet.
 * PV-Schema dient nur als XML-Testeingabe; die HTTP-Antwort wird ausschließlich mit
 * dem unabhängig generierten OpenAPI-Client gelesen.
 */
public final class JzufiPolymorphismFixture {

    public static final String STRUCTURE_DETAIL = "SDK-Verrichtungsdetail";
    public static final String LANGUAGE_VERSION = "sdk-language-version";
    public static final String BINARY_BASE64 = "AAECf/8=";
    public static final String XML_PAYLOAD = "SDK-XML-Payload";

    private JzufiPolymorphismFixture() {}

    /**
     * Ändert nur Schreiboperationen und validiert das ergänzte XML erneut gegen XSD.
     * Transaktionsheader, Löschoperationen und die übrigen Generatorwerte bleiben erhalten.
     */
    static String enrich(String xml) throws IOException {
        try {
            var context = JAXBContext.newInstance(TransferOperation050502.class);
            var message = context
                .createUnmarshaller()
                .unmarshal(new StreamSource(new StringReader(xml)), TransferOperation050502.class)
                .getValue();
            for (var operation : message.getTransferOperation()) {
                if (!(operation.getValue() instanceof SchreibeObjekt write)) continue;
                if (write.getLeistung() != null) {
                    var structure = new LeistungsstrukturObjektMitVerrichtungUndDetail();
                    var grouping = new CodeLeiKaLeistungsgruppierung();
                    grouping.setCode("100");
                    grouping.setListVersionID("sdk-contract-1");
                    structure.setLeistungsgruppierungLeiKa(grouping);
                    structure.setLeistungsobjektID(write.getLeistung().getId());
                    var activity = new CodeLeiKaVerrichtungskennung();
                    activity.setCode("001");
                    activity.setListVersionID("sdk-contract-1");
                    structure.setVerrichtungLeiKa(activity);
                    var detail = new StringLocalized();
                    detail.setLanguageCode("de");
                    detail.setValue(STRUCTURE_DETAIL);
                    structure.getVerrichtungsdetail().add(detail);
                    write.getLeistung().setStruktur(structure);
                }
                if (write.getOrganisationseinheit() instanceof OrganisationseinheitErweitert organisation) {
                    for (var form : organisation.getFormular()) {
                        for (var file : form.getDatei()) {
                            if (file instanceof FormulardateiLink link) {
                                var language = new Sprachversion();
                                language.setLanguageCode("de");
                                language.setVersion(LANGUAGE_VERSION);
                                language.setKennzeichenMaschinelleUebersetzung(true);
                                link.getSprache().add(language);
                            }
                        }
                        var xmlFile = new FormulardateiXML();
                        var content = new AnyXMLContent();
                        var document = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder()
                            .newDocument();
                        var element = document.createElementNS("urn:sdk:form", "sdk:form");
                        element.setAttribute("version", "1");
                        element.setTextContent(XML_PAYLOAD);
                        content.getAny().add(element);
                        xmlFile.setXmldaten(content);
                        form.getDatei().add(xmlFile);
                        var binaryFile = new FormulardateiBinaer();
                        var binary = new BinaryObject();
                        binary.setValue(java.util.Base64.getDecoder().decode(BINARY_BASE64));
                        binary.setFilename("sdk-form.bin");
                        binary.setMimeCode("application/octet-stream");
                        binaryFile.setBinaerdaten(binary);
                        form.getDatei().add(binaryFile);
                    }
                }
            }
            var output = new StringWriter();
            context.createMarshaller().marshal(message, output);
            String result = output.toString();
            Xzufi23Validator.getInstance().validateSchema(result);
            return result;
        } catch (JAXBException | SAXException | javax.xml.parsers.ParserConfigurationException exception) {
            throw new IOException("Polymorphe XZuFi-Importfixture konnte nicht erstellt werden", exception);
        }
    }
}
