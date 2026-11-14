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
package de.dataport.suchdienst.jzufi.model;

import static de.dataport.suchdienst.testutil.JzufiModelTestSupport.families;
import static de.dataport.suchdienst.testutil.JzufiModelTestSupport.httpMapper;
import static de.dataport.suchdienst.testutil.JzufiModelTestSupport.local;
import static de.dataport.suchdienst.testutil.JzufiModelTestSupport.mapperBuilder;
import static de.dataport.suchdienst.testutil.JzufiModelTestSupport.xsdHierarchy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.pv_onlinegw.xzufi_konverter.XZuFiKonverter;
import de.dataport.suchdienst.config.Jackson3Configuration;
import de.dataport.suchdienst.jzufi.model.mapper.JzufiModelMapper;
import de.xoev.xzufi.v2_3_1.AnschriftOrganisationseinheit;
import de.xoev.xzufi.v2_3_1.CodeLeiKaLeistungsgruppierung;
import de.xoev.xzufi.v2_3_1.CodeWithAuthorityType;
import de.xoev.xzufi.v2_3_1.Formular;
import de.xoev.xzufi.v2_3_1.FormulardateiBinaer;
import de.xoev.xzufi.v2_3_1.FormulardateiLink;
import de.xoev.xzufi.v2_3_1.FormulardateiXML;
import de.xoev.xzufi.v2_3_1.Gebietsklasse;
import de.xoev.xzufi.v2_3_1.Geokodierung;
import de.xoev.xzufi.v2_3_1.Identifikator;
import de.xoev.xzufi.v2_3_1.Kategorie;
import de.xoev.xzufi.v2_3_1.KontaktpersonErweitert;
import de.xoev.xzufi.v2_3_1.Leistung;
import de.xoev.xzufi.v2_3_1.LeistungsstrukturObjektMitVerrichtungUndDetail;
import de.xoev.xzufi.v2_3_1.OnlinedienstVorlage;
import de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert;
import de.xoev.xzufi.v2_3_1.PointType;
import de.xoev.xzufi.v2_3_1.Sprachversion;
import de.xoev.xzufi.v2_3_1.StringLocalized;
import de.xoev.xzufi.v2_3_1.StringLocalizedHTML;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testziel: Der HTTP-JSON-Vertrag emittiert alle erreichbaren XSD-Ausprägungen mit logischem
 * Typnamen und erhält Untertypfelder auch in verschachtelten Ausgaben und XML-/Binärdateien.
 * PV-Schema-HTTP und bestehende Elasticsearch-Klassenwrapper bleiben vom lokalen
 * Ausgabevertrag unabhängig. Das Deserialisieren lokaler Ausgaben prüfen die generierten Clienttests.
 */
class JzufiJsonContractTest {

    static Stream<Arguments> variants() {
        return families()
            .entrySet()
            .stream()
            .flatMap(family ->
                family
                    .getValue()
                    .stream()
                    .map(variant -> Arguments.of(family.getKey(), variant))
            );
    }

    /**
     * Leere Instanzen verhindern, dass Untertypfelder die Typauswahl beeinflussen.
     * Serialisiert wird ausdrücklich über die deklarierte Basis wie bei polymorphen Antwortfeldern.
     */
    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("variants")
    void emitsEverySupportedVariantThroughItsDeclaredBase(Class<?> base, Class<?> variant)
        throws Exception {
        var mapper = httpMapper();
        var value = local(variant).getConstructor().newInstance();
        var json = mapper.readTree(mapper.writerFor(local(base)).writeValueAsString(value));
        assertEquals(variant.getSimpleName(), json.path("@type").asString());
    }

    /** Prüft Konverter und NON_EMPTY-Ausgabe gemeinsam, ohne fehlende Module im lokalen Mapper zu bereinigen. */
    @Test
    void omitsAbsentModulesAfterConvertingALegacyService() {
        var legacy = new de.xoev.xzufi.v2_2_0.Leistung();
        var identifier = new de.xoev.xzufi.v2_2_0.Identifikator();
        identifier.setValue("service-without-optional-modules");
        identifier.setSchemeAgencyID("test");
        legacy.setId(identifier);
        var source = XZuFiKonverter.konvertiere(legacy);
        for (var modules : List.of(
            source.getModulFrist(),
            source.getModulBearbeitungsdauer(),
            source.getModulKosten(),
            source.getModulFachlicheFreigabe(),
            source.getModulAuskunftshinweis()
        )) {
            assertTrue(modules.isEmpty());
        }
        var mapper = httpMapper();
        var json = mapper.readTree(
            mapper.writeValueAsString(JzufiModelMapper.INSTANCE.map23(source))
        );
        for (String module : List.of(
            "modulFrist",
            "modulBearbeitungsdauer",
            "modulKosten",
            "modulFachlicheFreigabe",
            "modulAuskunftshinweis"
        )) {
            assertFalse(json.has(module), module);
        }
    }

    /** Kombiniert verschiedene Basis-Slots in einer Ausgabe, einschließlich GML-Namen und Onlinedienstvorlagen. */
    @Test
    void preservesSubtypeFieldsThroughNestedOrganisationSlotsAndHttpJson() {
        var source = new OrganisationseinheitErweitert();
        var person = new KontaktpersonErweitert();
        var identifier = new Identifikator();
        identifier.setValue("organisation-reference");
        person.getOrganisationseinheitID().add(identifier);
        var address = new AnschriftOrganisationseinheit();
        var parking = new StringLocalizedHTML();
        parking.setValue("parking-details");
        address.getInfoParkplatz().add(parking);
        var point = new PointType();
        var code = new CodeWithAuthorityType();
        code.setValue("point-name");
        code.setCodeSpace("urn:point-names");
        point.getName().add(code);
        var location = new Geokodierung();
        location.setPoint(point);
        address.getGeokodierung().add(location);
        person.getAnschrift().add(address);
        source.getKontaktperson().add(person);
        var category = new Kategorie();
        var areaClass = new Gebietsklasse();
        var shortName = new StringLocalized();
        shortName.setValue("area-short-name");
        areaClass.getKurzbezeichnung().add(shortName);
        category.setKlasse(areaClass);
        source.getKategorie().add(category);
        var template = new OnlinedienstVorlage();
        template.setInfoEinfuehrung("template-introduction");
        source.getTerminvereinbarungsdienst().add(template);
        var mapper = httpMapper();
        var json = mapper.readTree(
            mapper.writeValueAsString(JzufiModelMapper.INSTANCE.map23(source))
        );
        assertEquals(
            "organisation-reference",
            json.at("/kontaktperson/0/organisationseinheitID/0/value").asString()
        );
        assertEquals(
            "parking-details",
            json.at("/kontaktperson/0/anschrift/0/infoParkplatz/0/value").asString()
        );
        assertEquals(
            "CodeWithAuthorityType",
            json.at("/kontaktperson/0/anschrift/0/geokodierung/0/Point/name/0/@type").asString()
        );
        assertEquals(
            "point-name",
            json.at("/kontaktperson/0/anschrift/0/geokodierung/0/Point/name/0/value").asString()
        );
        assertEquals(
            "area-short-name",
            json.at("/kategorie/0/klasse/kurzbezeichnung/0/value").asString()
        );
        assertEquals(
            "template-introduction",
            json.at("/terminvereinbarungsdienst/0/infoEinfuehrung").asString()
        );
    }

    @Test
    void preservesFieldsFromEveryLevelOfTheNestedServiceStructure() {
        var source = new Leistung();
        var structure = new LeistungsstrukturObjektMitVerrichtungUndDetail();
        var grouping = new CodeLeiKaLeistungsgruppierung();
        grouping.setCode("100");
        structure.setLeistungsgruppierungLeiKa(grouping);
        var objectId = new Identifikator();
        objectId.setValue("service-object");
        structure.setLeistungsobjektID(objectId);
        var detail = new StringLocalized();
        detail.setValue("activity-detail");
        structure.getVerrichtungsdetail().add(detail);
        source.setStruktur(structure);
        var mapper = httpMapper();
        var json = mapper.readTree(
            mapper.writeValueAsString(JzufiModelMapper.INSTANCE.map23(source))
        );
        assertEquals(
            "LeistungsstrukturObjektMitVerrichtungUndDetail",
            json.at("/struktur/@type").asString()
        );
        assertEquals("100", json.at("/struktur/leistungsgruppierungLeiKa/code").asString());
        assertEquals("service-object", json.at("/struktur/leistungsobjektID/value").asString());
        assertEquals("activity-detail", json.at("/struktur/verrichtungsdetail/0/value").asString());
    }

    /**
     * Prüft die gemischte Dateiausgabe einschließlich XML-String, Base64 und Metadaten.
     * Mapping und Serialisierung dürfen dabei weder Quelldaten verändern noch Dateivarianten ausfiltern.
     */
    @Test
    void preservesAllFormFilesOrderPayloadMetadataAndSource() throws Exception {
        var source = new OrganisationseinheitErweitert();
        var form = new Formular();
        var first = new FormulardateiLink();
        first.setFormulardokumentTypZusatz("first-link");
        var language = new Sprachversion();
        language.setVersion("language-version");
        language.setKennzeichenMaschinelleUebersetzung(true);
        first.getSprache().add(language);
        var second = new FormulardateiLink();
        second.setFormulardokumentTypZusatz("second-link");
        var xml = new FormulardateiXML();
        var content = new de.xoev.xzufi.v2_3_1.AnyXMLContent();
        var document = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .newDocument();
        var element = document.createElementNS("urn:test:form", "form");
        element.setAttributeNS(
            javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
            "xmlns",
            "urn:test:form"
        );
        element.setAttribute("mode", "example");
        element.setTextContent("XML payload");
        content.getAny().add(element);
        xml.setXmldaten(content);
        var binary = new FormulardateiBinaer();
        var data = new de.xoev.xzufi.v2_3_1.BinaryObject();
        data.setValue(new byte[] { 0, 1, 2, 127, -1 });
        data.setFilename("form.bin");
        data.setMimeCode("application/octet-stream");
        binary.setBinaerdaten(data);
        // Zwei durch XML getrennte Links machen ein unbeabsichtigtes Gruppieren nach Dateityp sichtbar.
        var files = List.of(first, xml, second, binary);
        form.getDatei().addAll(files);
        source.getFormular().add(form);
        var mapped = JzufiModelMapper.INSTANCE.map23(source);
        assertEquals(
            List.of(
                "FormulardateiLink",
                "FormulardateiXML",
                "FormulardateiLink",
                "FormulardateiBinaer"
            ),
            mapped
                .getFormular()
                .getFirst()
                .getDatei()
                .stream()
                .map(file -> file.getClass().getSimpleName())
                .toList()
        );
        var mapper = httpMapper();
        var json = mapper.readTree(mapper.writeValueAsString(mapped));
        assertEquals(
            "first-link",
            json.at("/formular/0/datei/0/formulardokumentTypZusatz").asString()
        );
        assertEquals(
            "second-link",
            json.at("/formular/0/datei/2/formulardokumentTypZusatz").asString()
        );
        assertTrue(
            json.at("/formular/0/datei/1/xmldaten/any/0").asString().contains("XML payload")
        );
        assertTrue(
            json.at("/formular/0/datei/1/xmldaten/any/0").asString().contains("mode=\"example\"")
        );
        assertEquals("AAECf/8=", json.at("/formular/0/datei/3/binaerdaten/value").asString());
        assertEquals("form.bin", json.at("/formular/0/datei/3/binaerdaten/filename").asString());
        // Erneutes Parsen erkennt Namespace-/Attributverluste, die reine Teilstringprüfungen übersehen könnten.
        var xmlFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        xmlFactory.setNamespaceAware(true);
        xmlFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var readElement = xmlFactory
            .newDocumentBuilder()
            .parse(
                new org.xml.sax.InputSource(
                    new java.io.StringReader(
                        json.at("/formular/0/datei/1/xmldaten/any/0").asString()
                    )
                )
            )
            .getDocumentElement();
        assertEquals("form", readElement.getTagName());
        assertEquals("urn:test:form", readElement.getAttribute("xmlns"));
        assertEquals("example", readElement.getAttribute("mode"));
        assertEquals(2, readElement.getAttributes().getLength());
        assertEquals("XML payload", readElement.getTextContent());
        assertEquals("urn:test:form", readElement.getNamespaceURI());
        assertEquals("Sprachversion", json.at("/formular/0/datei/0/sprache/0/@type").asString());
        assertEquals(
            "language-version",
            json.at("/formular/0/datei/0/sprache/0/version").asString()
        );
        assertEquals(
            true,
            json.at("/formular/0/datei/0/sprache/0/kennzeichenMaschinelleUebersetzung").asBoolean()
        );
        assertEquals(files, form.getDatei());
        // XML und Binärdaten müssen auch dann erhalten bleiben, wenn überhaupt keine Linkdatei vorhanden ist.
        form.getDatei().removeIf(FormulardateiLink.class::isInstance);
        var withoutLinks = JzufiModelMapper.INSTANCE.map23(source);
        assertEquals(2, withoutLinks.getFormular().getFirst().getDatei().size());
        assertEquals(2, form.getDatei().size());
    }

    @Test
    void transferKeepsMixedResponsibilityTypesAndOrder() throws Exception {
        var transfer = new de.dataport.suchdienst.jzufi.model.v23.ZustaendigkeitTransferObjekt();
        var variants = families().get(de.xoev.xzufi.v2_3_1.Zustaendigkeit.class);
        for (var variant : variants) {
            transfer
                .getZustaendigkeit()
                .add(
                    (de.dataport.suchdienst.jzufi.model.v23.Zustaendigkeit) local(variant)
                        .getConstructor()
                        .newInstance()
                );
        }
        var mapper = httpMapper();
        var json = mapper.readTree(mapper.writeValueAsString(transfer));
        assertEquals(
            variants.stream().map(Class::getSimpleName).toList(),
            json
                .path("zustaendigkeit")
                .valueStream()
                .map(node -> node.path("@type").asString())
                .toList()
        );
    }

    @Test
    void internalHttpModelsDoNotInheritTheLocalTypeContract() {
        var organisation = new OrganisationseinheitErweitert();
        organisation
            .getZustaendigkeit()
            .add(new de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit());
        assertFalse(
            httpMapper().valueToTree(organisation).path("zustaendigkeit").get(0).has("@type")
        );
    }

    /** Persistenz verwendet weiterhin Wrapper mit vollqualifiziertem Klassennamen statt des lokalen HTTP-Discriminators. */
    @ParameterizedTest
    @ValueSource(
        classes = {
            de.xoev.xzufi.v2_2_0.Zustaendigkeit.class,
            de.xoev.xzufi.v2_3_1.Zustaendigkeit.class,
        }
    )
    void elasticsearchMapperRetainsClassWrappersForEveryVariant(Class<?> base) throws Exception {
        var mapper = new Jackson3Configuration().elasticsearchJsonMapper(mapperBuilder());
        for (var subtype : xsdHierarchy(base)) {
            var value = subtype.getConstructor().newInstance();
            var json = mapper.writeValueAsString(value);
            assertFalse(json.contains("\"@type\""));
            var tree = mapper.readTree(json);
            assertTrue(tree.isArray());
            assertEquals(subtype.getName(), tree.get(0).asString());
            assertEquals(subtype, mapper.readValue(json, base).getClass());
        }
    }

    @Test
    void elasticsearchMapperKeepsTransferPersistenceFormat() {
        var transfer = new de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt();
        transfer
            .getZustaendigkeit()
            .add(new de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit());
        var mapper = new Jackson3Configuration().elasticsearchJsonMapper(mapperBuilder());
        var json = mapper.writeValueAsString(transfer);
        assertFalse(json.contains("\"@type\""));
        var read = mapper.readValue(json, transfer.getClass());
        assertInstanceOf(
            de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit.class,
            read.getZustaendigkeit().getFirst()
        );
    }

    @ParameterizedTest
    @ValueSource(
        classes = {
            de.xoev.xzufi.v2_2_0.Zustaendigkeit.class,
            de.xoev.xzufi.v2_3_1.Zustaendigkeit.class,
        }
    )
    void elasticsearchMapperReadsExistingClassWrappers(Class<?> base) {
        // Historisches JSON von Hand: Ein Roundtrip nur mit dem heutigen Mapper würde Formatänderungen übersehen.
        String prefix = base.getPackageName();
        String typeName = prefix + ".ZustaendigkeitOrganisationseinheit";
        String role = "[\"" + prefix + ".CodeZustaendigkeitsrolle\",{\"code\":\"02\"}]";
        // 2.2 hat eine einzelne Rolle, 2.3 eine Rollenliste; die inneren Arrays sind persistierte Klassenwrapper.
        if (!prefix.endsWith("v2_2_0")) role = "[" + role + "]";
        String json = """
        ["%s",{
            "leistungID":[["%s.Identifikator",{"value":"lb"}]],
            "gebietID":[["%s.Identifikator",{"value":"gebiet"}]],
            "rolle":%s
        }]
        """.formatted(typeName, prefix, prefix, role);
        var mapper = new Jackson3Configuration().elasticsearchJsonMapper(mapperBuilder());
        var read = mapper.readValue(json, base);
        assertEquals(typeName, read.getClass().getName());
        var serialized = mapper.readTree(mapper.writeValueAsString(read));
        assertEquals(typeName, serialized.get(0).asString());
        assertEquals(
            "lb",
            serialized.get(1).path("leistungID").get(0).get(1).path("value").asString()
        );
        assertFalse(serialized.get(1).has("@type"));
    }
}
