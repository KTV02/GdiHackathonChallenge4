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
package de.dataport.suchdienst.testutil;

import de.dataport.suchdienst.config.Jackson3Configuration;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

/** Produktive Mapper-Anpassungen und ein von den lokalen Typregistrierungen unabhängiges XSD-Inventar. */
public final class JzufiModelTestSupport {

    private static final String SOURCE = "de.xoev.xzufi.v2_3_1.";
    private static final String TARGET = "de.dataport.suchdienst.jzufi.model.v23.";

    private JzufiModelTestSupport() {}

    /** Baut den HTTP-Mapper mit produktiven Jackson-Anpassungen und JAXB-Metadaten ohne Spring-Kontext. */
    public static JsonMapper httpMapper() {
        var configuration = new Jackson3Configuration();
        var builder = mapperBuilder();
        configuration.customizeJackson3().customize(builder);
        return configuration.httpJsonMapper(builder);
    }

    /** Jackson-Annotationen bleiben vorrangig; JAXB liefert ergänzend die XML-basierten Feldbindungen. */
    public static JsonMapper.Builder mapperBuilder() {
        return JsonMapper.builder().addModule(
            new JakartaXmlBindAnnotationModule().setPriority(
                JakartaXmlBindAnnotationModule.Priority.SECONDARY
            )
        );
    }

    /** Lokale Antworttypen der vier v2-Routen ohne den zusätzlichen Relationsseiten-Wrapper. */
    public static List<Class<?>> outputRoots() {
        return List.of(
            de.dataport.suchdienst.jzufi.model.v23.Leistung.class,
            de.dataport.suchdienst.jzufi.model.v23.OrganisationseinheitErweitert.class,
            de.dataport.suchdienst.jzufi.model.v23.OnlinedienstErweitert.class,
            de.dataport.suchdienst.jzufi.model.v23.ZustaendigkeitTransferObjekt.class
        );
    }

    /**
     * Leitet Familien aus den unveränderten PV-Schema-Feldtypen und JAXB-Untertypen ab,
     * nicht aus den zu prüfenden Jackson-/OpenAPI-Registrierungen des Ausgabemodells.
     * Oberklassen liefern geerbte Felder, werden aber nicht selbst zu zusätzlichen Slots.
     *
     * @return deklarierter Basistyp auf konkrete XSD-Ausprägungen, einschließlich einer instanziierbaren Basis
     */
    public static Map<Class<?>, Set<Class<?>>> families() {
        var mapper = httpMapper();
        var pending = new ArrayDeque<Class<?>>(
            List.of(
                de.xoev.xzufi.v2_3_1.Leistung.class,
                de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert.class,
                de.xoev.xzufi.v2_3_1.OnlinedienstErweitert.class,
                de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt.class
            )
        );
        Set<Class<?>> visited = new LinkedHashSet<>();
        Set<Class<?>> slots = new LinkedHashSet<>(pending);
        Map<Class<?>, Set<Class<?>>> families = new LinkedHashMap<>();
        while (!pending.isEmpty()) {
            var type = pending.removeFirst();
            if (
                !type.getPackageName().equals("de.xoev.xzufi.v2_3_1") || !visited.add(type)
            ) continue;
            if (type.getDeclaredAnnotation(XmlSeeAlso.class) != null) {
                var variants = new LinkedHashSet<>(xsdHierarchy(type));
                variants.removeIf(variant -> Modifier.isAbstract(variant.getModifiers()));
                families.put(type, variants);
                pending.addAll(variants);
            }
            for (
                Class<?> current = type;
                current != null && current != Object.class;
                current = current.getSuperclass()
            ) {
                for (var field : current.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    var valueType = mapper.constructType(field.getGenericType());
                    while (valueType.isContainerType()) valueType = valueType.getContentType();
                    slots.add(valueType.getRawClass());
                    pending.add(valueType.getRawClass());
                }
            }
        }
        // Eine Vererbungsstufe allein ist noch kein polymorpher Slot; sie muss auch als Feldtyp oder Root vorkommen.
        families.keySet().retainAll(slots);
        return families;
    }

    /** Ermittelt die Basis und alle transitiven JAXB-Untertypen, zunächst einschließlich abstrakter Klassen. */
    public static Set<Class<?>> xsdHierarchy(Class<?> base) {
        Set<Class<?>> result = new LinkedHashSet<>();
        var pending = new ArrayDeque<Class<?>>(List.of(base));
        while (!pending.isEmpty()) {
            var current = pending.removeFirst();
            if (!result.add(current)) continue;
            var children = current.getDeclaredAnnotation(XmlSeeAlso.class);
            if (children != null) pending.addAll(List.of(children.value()));
        }
        return result;
    }

    /** Findet das namensgleiche lokale 2.3-Modell; führt selbst kein Objektmapping aus. */
    public static Class<?> local(Class<?> source) throws ClassNotFoundException {
        return Class.forName(TARGET + source.getName().substring(SOURCE.length()));
    }
}
