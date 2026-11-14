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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.jzufi.model.mapper.JzufiModelMapper;
import de.xoev.xzufi.v2_3_1.SchreibeObjekt;
import de.xoev.xzufi.v2_3_1.TransferOperation050502;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import java.beans.Introspector;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Testziel: Sichert das Mapping der internen PV-Schema-Modelle auf die eigenen JZuFi-Ausgabemodelle
 * für den neuen XZuFi-2.3-Vertrag ab. Werte, JSON-Feldbindungen und konkrete Untertypen sollen
 * erhalten bleiben; nur die bekannten lokalen Modelldiscriminatoren {@code @type} dürfen hinzukommen.
 * Der HTTP-JSON-Vertrag einschließlich XML-/Binärinhalten wird in {@link JzufiJsonContractTest} geprüft.
 * Verwendet reale XML-Fixtures und synthetische Testobjekte, ohne die internen Modelle
 * durch HTTP-spezifische Mix-ins anzupassen.
 */
class JzufiModelMapperTest {

    private final JsonMapper baseline = httpMapper();
    private final JsonMapper output = httpMapper();
    private final Set<String> localTypeNames = families()
        .values()
        .stream()
        .flatMap(Collection::stream)
        .map(Class::getSimpleName)
        .collect(Collectors.toSet());

    /**
     * Prüft alle erreichbaren Ausprägungen des neuen Vertrags auf Feldverluste
     * und den erwarteten lokalen Typ. Die automatische Befüllung verbreitert die Abdeckung,
     * garantiert wegen ihrer Tiefen- und Typbeschränkung aber keine Prüfung sämtlicher Felder.
     */
    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("variants")
    void mappingPreservesPopulatedFieldsAndConcreteType(Class<?> base, Class<?> variant)
        throws Exception {
        var source = populated(variant, 0);
        // Den Basisklassen-Overload aufrufen, damit MapStructs Untertyp-Dispatch tatsächlich geprüft wird.
        var mapped = JzufiModelMapper.class
            .getMethod("map23", base)
            .invoke(JzufiModelMapper.INSTANCE, source);
        assertEquals(local(variant), mapped.getClass());
        assertEquals(variant.getSimpleName(), output.valueToTree(mapped).path("@type").asString());
        assertJson(source, mapped);
    }

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

    @Test
    void mappingPreservesResponsibilityTransferFields() throws Exception {
        var source23 = (de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt) populated(
            de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt.class,
            0
        );
        assertJson(source23, JzufiModelMapper.INSTANCE.map23(source23));
    }

    /** Deckt historische Werte, extreme Offsets und offsetlose Daten an Zeitumstellungstagen ab. */
    @ParameterizedTest
    @ValueSource(
        strings = {
            "2018-08-01+02:00",
            "1699-12-31+01:00",
            "2026-01-01+14:00",
            "2026-03-29",
            "2026-10-25",
        }
    )
    void dateMappingPreservesTheXmlDateWithoutTimezoneConversion(String value) throws Exception {
        var date = DatatypeFactory.newInstance().newXMLGregorianCalendar(value);
        var period23 = new de.xoev.xzufi.v2_3_1.Zeitraum();
        period23.setBeginn(date);
        var source23 = new de.xoev.xzufi.v2_3_1.Zustaendigkeit();
        source23.getGueltigkeit().add(period23);
        var mapped23 = JzufiModelMapper.INSTANCE.map23(source23);
        assertEquals(value, mapped23.getGueltigkeit().getFirst().getBeginn().toXMLFormat());
        assertEquals(
            value,
            output
                .readTree(output.writeValueAsString(mapped23))
                .path("gueltigkeit")
                .get(0)
                .path("beginn")
                .asString()
        );
        assertJson(source23, mapped23);
    }

    @ParameterizedTest
    @ValueSource(strings = { "08:30:00", "00:15:30.123456+14:00", "23:59:59.987654-03:30" })
    void openingTimesKeepTheirTimeZoneAndFractionWithoutAnInventedDate(String value)
        throws Exception {
        var series = new de.xoev.xzufi.v2_3_1.Zeitserie();
        series.setBeginn(DatatypeFactory.newInstance().newXMLGregorianCalendar(value));
        var openingTimes = new de.xoev.xzufi.v2_3_1.Zeitserien();
        openingTimes.getRegulaereZeiten().add(series);
        var organisation = new de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert();
        var identifier = new de.xoev.xzufi.v2_3_1.Identifikator();
        identifier.setValue("organisation");
        identifier.setSchemeAgencyID("test");
        organisation.setId(identifier);
        organisation.getInfoOeffnungszeitenStrukturiert().add(openingTimes);
        var mapped23 = JzufiModelMapper.INSTANCE.map23(organisation);
        assertJson(organisation, mapped23);
        assertEquals(
            value,
            mapped23
                .getInfoOeffnungszeitenStrukturiert()
                .getFirst()
                .getRegulaereZeiten()
                .getFirst()
                .getBeginn()
                .toXMLFormat()
        );
        assertTrue(output.writeValueAsString(mapped23).contains("\"" + value + "\""));
    }

    /** Die doppelte lokale Uhrzeit beim Sommerzeitende bezeichnet je nach Offset einen anderen, zu erhaltenden Zeitpunkt. */
    @ParameterizedTest
    @ValueSource(
        strings = {
            "2026-01-01T00:15:30.123+14:00",
            "2026-10-25T02:30:00+02:00",
            "2026-10-25T02:30:00+01:00",
        }
    )
    void dateTimeMappingKeepsTheInstantAndTheXmlTimezone(String value) throws Exception {
        var expected = java.time.OffsetDateTime.parse(value).toInstant();
        var version23 = new de.xoev.xzufi.v2_3_1.Versionsinformation();
        version23.setErstelltDatumZeit(
            new de.dataport.suchdienst.jzufi.model.v23.Adapter1().unmarshal(value)
        );
        var source23 = new de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert();
        source23.setVersionsinformation(version23);
        var mapped23 = JzufiModelMapper.INSTANCE.map23(source23);
        assertEquals(
            expected,
            mapped23.getVersionsinformation().getErstelltDatumZeit().toInstant()
        );
        assertEquals(
            value,
            output
                .readTree(output.writeValueAsString(mapped23))
                .path("versionsinformation")
                .path("erstelltDatumZeit")
                .asString()
        );
        assertJson(source23, mapped23);
    }

    /** Ergänzt synthetische Objekte durch echte JAXB-Importfixtures; verglichen wird der ausgegebene JSON-Vertrag. */
    @ParameterizedTest
    @ValueSource(strings = { "leistung", "organisation", "onlinedienst" })
    void preservesFixtureJson(String resource) throws Exception {
        String filename = switch (resource) {
            case "leistung" -> "x23/xzufi-sh-00001.xml";
            case "organisation" -> "x23/xzufi-sh-organisation.xml";
            default -> "x23/onlinedienst.xzufi";
        };
        TransferOperation050502 transfer;
        try (var input = getClass().getClassLoader().getResourceAsStream(filename)) {
            assertNotNull(input, filename);
            transfer = (TransferOperation050502) JAXBContext.newInstance(
                TransferOperation050502.class
            )
                .createUnmarshaller()
                .unmarshal(input);
        }
        var source = transfer
            .getTransferOperation()
            .stream()
            .map(JAXBElement::getValue)
            .filter(SchreibeObjekt.class::isInstance)
            .map(SchreibeObjekt.class::cast)
            .map(
                write ->
                    switch (resource) {
                        case "leistung" -> write.getLeistung();
                        case "organisation" -> write.getOrganisationseinheit();
                        default -> write.getOnlinedienst();
                    }
            )
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow();
        var mapped = switch (resource) {
            case "leistung" -> JzufiModelMapper.INSTANCE.map23(
                (de.xoev.xzufi.v2_3_1.Leistung) source
            );
            case "organisation" -> JzufiModelMapper.INSTANCE.map23(
                (de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert) source
            );
            default -> JzufiModelMapper.INSTANCE.map23(
                (de.xoev.xzufi.v2_3_1.OnlinedienstErweitert) source
            );
        };
        assertJson(source, mapped);
    }

    @Test
    void preservesGmlMetadataFields() {
        var source23 = metadataSample();
        assertJson(source23, JzufiModelMapper.INSTANCE.map23(source23));
    }

    private static de.xoev.xzufi.v2_3_1.AbstractMetaDataType metadataSample() {
        // Für diese abstrakte GML-Basis existiert im PV-Schema keine instanziierbare XSD-Ausprägung.
        var metadata = new de.xoev.xzufi.v2_3_1.AbstractMetaDataType() {};
        metadata.setId("metadata23");
        metadata.getContent().add("description");
        return metadata;
    }

    /**
     * Vergleicht das serialisierte Ausgabemodell mit dem unveränderten internen Modell.
     * Entfernt vorher ausschließlich die geprüften lokalen Modelldiscriminatoren,
     * damit diese bewusste HTTP-Erweiterung keine übrigen JSON-Abweichungen verdeckt.
     */
    private void assertJson(Object source, Object mapped) {
        JsonNode actual = output.readTree(output.writeValueAsString(mapped));
        removeLocalTypes(actual);
        assertEquals(
            baseline.readTree(baseline.writeValueAsString(source)),
            actual,
            source.getClass().getName()
        );
    }

    /**
     * Prüft jeden gefundenen {@code @type}-Wert gegen die unterstützten Modelltypen,
     * bevor er rekursiv aus dem Vergleichsbaum entfernt wird. Unerwartete Typkennungen
     * lassen den Test fehlschlagen, statt beim Normalisieren unbemerkt zu verschwinden.
     */
    private void removeLocalTypes(JsonNode node) {
        if (node instanceof ObjectNode object && object.has("@type")) {
            assertTrue(localTypeNames.contains(object.path("@type").asString()), object.toString());
            object.remove("@type");
        }
        node.valueStream().forEach(this::removeLocalTypes);
    }

    /**
     * Erzeugt per JavaBeans-Reflection synthetische PV-Schema-Objekte mit festen Beispielwerten.
     * Befüllt auch JAXB-Live-Collections ohne Setter mit je einem Element.
     * Nicht unterstützte Typen und Modellobjekte jenseits der Tiefe 6 bleiben unbefüllt.
     * Die Werte sind nicht zwingend fachlich gültig. Skalare String-Felder erhalten
     * feldspezifische Werte, um Vertauschungen zu erkennen.
     *
     * @param type zu erzeugender Modell- oder unterstützter Werttyp
     * @param depth aktuelle Rekursionstiefe, beginnend mit 0
     * @return befüllter Beispielwert oder {@code null} bei nicht unterstütztem Typ oder Tiefenlimit
     */
    private static Object populated(Class<?> type, int depth) throws Exception {
        if (type == String.class) return "sample";
        if (type == boolean.class || type == Boolean.class) return true;
        if (type == int.class || type == Integer.class) return 7;
        if (type == byte[].class) return new byte[] { 1, 2, 3 };
        if (type == java.math.BigInteger.class) return java.math.BigInteger.valueOf(7);
        if (type == java.math.BigDecimal.class) return java.math.BigDecimal.valueOf(7.5);
        if (
            type == XMLGregorianCalendar.class
        ) return DatatypeFactory.newInstance().newXMLGregorianCalendar("2026-10-14+02:00");
        if (Calendar.class.isAssignableFrom(type)) return new GregorianCalendar(
            2026,
            Calendar.OCTOBER,
            14
        );
        if (type.isEnum()) return type.getEnumConstants()[0];
        if (depth > 6 || !type.getPackageName().startsWith("de.xoev.xzufi.")) return null;
        if (type == de.xoev.xzufi.v2_3_1.AbstractMetaDataType.class) return metadataSample();
        if (java.lang.reflect.Modifier.isAbstract(type.getModifiers())) {
            var concrete = de.dataport.suchdienst.testutil.JzufiModelTestSupport.xsdHierarchy(type)
                .stream()
                .filter(
                    candidate -> !java.lang.reflect.Modifier.isAbstract(candidate.getModifiers())
                )
                .findFirst()
                .orElseThrow(() ->
                    new IllegalArgumentException("Kein konkreter XSD-Untertyp: " + type.getName())
                );
            return populated(concrete, depth);
        }
        var instance = type.getConstructor().newInstance();
        for (var property : Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors()) {
            var setter = property.getWriteMethod();
            // JAXB kombiniert teils primitive is-Getter mit Boolean-Settern; JavaBeans erkennt diese nicht.
            if (
                setter == null &&
                property.getPropertyType() == boolean.class &&
                property.getReadMethod() != null &&
                property.getReadMethod().getName().startsWith("is")
            ) {
                String name = "set" + property.getReadMethod().getName().substring(2);
                setter = Arrays.stream(type.getMethods())
                    .filter(
                        method ->
                            method.getName().equals(name) &&
                            method.getParameterCount() == 1 &&
                            method.getParameterTypes()[0] == Boolean.class
                    )
                    .findFirst()
                    .orElse(null);
            }
            if (setter != null) {
                Object value =
                    property.getPropertyType() == String.class
                        ? "sample-" + property.getName()
                        : populated(property.getPropertyType(), depth + 1);
                if (value != null) setter.invoke(instance, value);
            } else if (
                property.getReadMethod() != null &&
                Collection.class.isAssignableFrom(property.getPropertyType())
            ) {
                var generic = (ParameterizedType) property.getReadMethod().getGenericReturnType();
                Object value = populated((Class<?>) generic.getActualTypeArguments()[0], depth + 1);
                if (value != null) {
                    @SuppressWarnings("unchecked")
                    var values = (Collection<Object>) property.getReadMethod().invoke(instance);
                    values.add(value);
                }
            }
        }
        return instance;
    }
}
