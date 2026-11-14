/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2025 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.config;

import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.FromStringDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleDeserializers;
import tools.jackson.databind.module.SimpleSerializers;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

@Configuration
public class Jackson3Configuration {

    private static final DatatypeFactory FACTORY = initializeFactory();

    private static DatatypeFactory initializeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e); // NOSONAR
        }
    }

    /**
     * Default-Mapper für JSON über HTTP (externe Representationen).
     * Basierend auf dem durch Spring Boot initialisierten {@link JsonMapper}.
     * DefaultTyping bleibt deaktiviert.
     */
    @Bean(name = "httpJsonMapper")
    @Primary
    public JsonMapper httpJsonMapper(JsonMapper.Builder jsonBuilder) {
        return jsonBuilder
            .deactivateDefaultTyping()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .build();
    }

    /**
     * Mapper für Elasticsearch (interne Persistenz/Transport).
     * Basierend auf dem durch Spring Boot initialisierten {@link JsonMapper}.
     * DefaultTyping wird hier gezielt aktiviert (nur für unser XZuFi-Package).
     */
    @Bean(name = "elasticsearchJsonMapper")
    public JsonMapper elasticsearchJsonMapper(JsonMapper.Builder jsonBuilder) {
        return jsonBuilder
            .setDefaultTyping(
                new Jackson3CustomTypeResolverBuilder(DefaultTyping.OBJECT_AND_NON_CONCRETE)
            )
            .build();
    }

    @Bean
    public JacksonModule jaxbModuleJackson3() {
        return new JakartaXmlBindAnnotationModule().setPriority(
            JakartaXmlBindAnnotationModule.Priority.SECONDARY
        );
    }

    @Bean
    public JsonMapperBuilderCustomizer customizeJackson3() {
        return builder -> {
            builder.serializerFactory(
                builder.serializerFactory().withAdditionalSerializers(new CustomSerializers())
            );
            builder.deserializerFactory(
                builder.deserializerFactory().withAdditionalDeserializers(new CustomDeserializers())
            );

            // Wichtig: DefaultTyping nicht global erzwingen.
            // Wir setzen es gezielt nur auf dem ES-internen Mapper (siehe Bean elasticsearchJsonMapper()).

            // Boot/Spring 4.0.1 wendet @JsonView wieder strikter an.
            // Für Legacy-APIs wollen wir das frühere Verhalten: nicht-annotierte Felder sollen
            // weiterhin in allen Views enthalten sein; nur explizit annotierte (z.B. Internal)
            // werden je nach View ein-/ausgeblendet.
            builder.enable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        };
    }

    @Bean
    public JacksonConverterCustomizer jacksonConverterCustomizer(JsonMapper.Builder jsonBuilder) {
        JsonMapper jsonMapper = jsonBuilder.deactivateDefaultTyping().build();
        return new JacksonConverterCustomizer(jsonMapper);
    }

    // contribute a custom JSON converter to both client and server
    public static class JacksonConverterCustomizer
        implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer
    {

        private final JsonMapper jsonMapper;

        JacksonConverterCustomizer(JsonMapper jsonMapper) {
            this.jsonMapper = jsonMapper;
        }

        @Override
        public void customize(HttpMessageConverters.ClientBuilder builder) {
            builder.withJsonConverter(new JacksonJsonHttpMessageConverter(this.jsonMapper));
        }

        @Override
        public void customize(HttpMessageConverters.ServerBuilder builder) {
            builder.withJsonConverter(new JacksonJsonHttpMessageConverter(this.jsonMapper));
        }
    }

    static class CustomSerializers extends SimpleSerializers {

        CustomSerializers() {
            addSerializer(XMLGregorianCalendar.class, new CustomXmlCalendarSerializer());
            addSerializer(Calendar.class, new CustomCalendarSerializer());
        }
    }

    static class CustomDeserializers extends SimpleDeserializers {

        CustomDeserializers() {
            addDeserializer(XMLGregorianCalendar.class, new CustomXmlCalendarDeserializer());
            addDeserializer(Calendar.class, new CustomCalendarDeserializer());
        }
    }

    /**
     * Notwendig um {@link XMLGregorianCalendar} aus dem PV_Schema zu serialisieren.
     * Ohne Custom Serializer produziert Jackson bei bspw.Gueltigkeiten falsche Angaben,
     * aus 2018-08-01 wird dann z.B. 2018-07-31T22:00:00.000+0000.
     */
    static class CustomXmlCalendarSerializer extends StdSerializer<XMLGregorianCalendar> {

        CustomXmlCalendarSerializer() {
            super(XMLGregorianCalendar.class);
        }

        @Override
        public void serialize(
            XMLGregorianCalendar value,
            JsonGenerator gen,
            SerializationContext ctxt
        ) {
            gen.writeString(value.toString());
        }
    }

    static class CustomXmlCalendarDeserializer
        extends FromStringDeserializer<XMLGregorianCalendar>
    {

        CustomXmlCalendarDeserializer() {
            super(XMLGregorianCalendar.class);
        }

        @Override
        protected XMLGregorianCalendar _deserialize(String value, DeserializationContext ctxt)
            throws JacksonException {
            return FACTORY.newXMLGregorianCalendar(value);
        }
    }

    /**
     * Notwendig um {@link GregorianCalendar} aus dem PV_Schema zu serialisieren.
     * Ohne den Custom Serializer wird ein Calendar Datum ohne Uhrzeit serialisiert.
     */
    static class CustomCalendarSerializer extends StdSerializer<Calendar> {

        CustomCalendarSerializer() {
            super(Calendar.class);
        }

        @Override
        public void serialize(Calendar value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(value.toInstant().toString());
        }
    }

    static class CustomCalendarDeserializer extends FromStringDeserializer<Calendar> {

        CustomCalendarDeserializer() {
            super(Calendar.class);
        }

        @Override
        protected GregorianCalendar _deserialize(String value, DeserializationContext ctxt)
            throws JacksonException {
            return FACTORY.newXMLGregorianCalendar(value).toGregorianCalendar();
        }
    }
}
