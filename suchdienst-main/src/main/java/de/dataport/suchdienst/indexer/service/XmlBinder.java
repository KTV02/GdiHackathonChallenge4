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
package de.dataport.suchdienst.indexer.service;

import com.ctc.wstx.stax.WstxInputFactory;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.xoev.xzufi.v2_2_0.TransferOperation040502;
import de.xoev.xzufi.v2_3_1.TransferOperation050502;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

@Service
public class XmlBinder {

    // JAXBContext is thread safe, Unmarshaller is not
    private final Map<Class<?>, JAXBContext> contextByClass = new HashMap<>();

    private final XMLInputFactory xmlInputFactory = createXmlInputFactory();

    public XmlBinder() {
        try {
            initializeJaxBContexts();
        } catch (JAXBException e) {
            throw new ImportException(
                ErrorCodeEnum.XZUFI_IMPORT_ERROR,
                "Fehler beim Erstellen des Unmarshallers.",
                e
            );
        }
    }

    /**
     * Liefert einen JAXBContext zu einer Klasse. Wenn es keine Klasse ist, die bereits bekannt ist,
     * wird ein neuer JAXBContext erzeugt.
     *
     * @param clazz betreffende Klasse zu der ein Context erzeugt werden soll.
     * @return der JAXBContext zu der Klasse.
     */
    private JAXBContext getJaxbContext(Class<?> clazz) {
        var jaxbContext = contextByClass.get(clazz);
        if (jaxbContext == null) {
            throw new IllegalStateException(
                String.format(
                    "JAXB-Context für Klasse '%s' wurde nicht initialisiert",
                    clazz.getCanonicalName()
                )
            );
        }
        return jaxbContext;
    }

    /**
     * Generischer Unmarschaller
     *
     * @param dokument XzuFi-Dokument als ByteArray
     * @param clazz    die deklarierte Klasse
     * @return Das JAXB-Element
     * @throws JAXBException wenn ein Fehler beim Unmarshalling auftritt
     */
    private <T> T unmarshal(byte[] dokument, Class<T> clazz) throws XMLStreamException {
        XMLStreamReader xsr = null;
        try (var inputStream = new ByteArrayInputStream(dokument)) {
            xsr = xmlInputFactory.createXMLStreamReader(new StreamSource(inputStream));
            var jaxbContext = getJaxbContext(clazz);
            var jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            var jaxbElement = jaxbUnmarshaller.unmarshal(xsr, clazz);
            return jaxbElement.getValue();
        } catch (JAXBException | XMLStreamException | IOException e) {
            // Dieser Fall sollte nie auftreten, da es sich um ein ByteArray als Source handelt.
            throw new ImportException(
                ErrorCodeEnum.XZUFI_UNEXPECTED_FORMAT,
                String.format(
                    "Unmarshalling von Xzufi Byte-Array fehlgeschlagen. (Größe: %s)",
                    FileUtils.byteCountToDisplaySize(dokument.length)
                ),
                e
            );
        } finally {
            if (xsr != null) {
                xsr.close();
            }
        }
    }

    public TransferOperation040502 unmarshallXzufi(byte[] xZufi) {
        try {
            return unmarshal(xZufi, TransferOperation040502.class);
        } catch (XMLStreamException e) {
            throw new ImportException(
                ErrorCodeEnum.XZUFI_UNEXPECTED_FORMAT,
                "Schließen des XML-Streams fehlgeschlagen.",
                e
            );
        }
    }

    public TransferOperation050502 unmarshallXzufi23(byte[] xZufi) {
        try {
            return unmarshal(xZufi, TransferOperation050502.class);
        } catch (XMLStreamException e) {
            throw new ImportException(
                ErrorCodeEnum.XZUFI_UNEXPECTED_FORMAT,
                "Schließen des XML-Streams fehlgeschlagen.",
                e
            );
        }
    }

    private XMLInputFactory createXmlInputFactory() {
        // Erzwinge Woodstox als Implementierung:
        // Sofern die Lib im Classpath ist, könnte man auch XMLInputFactory.newFactory(...) verwenden.
        // Durch direkte Verwendung der WstxInputFactory wird eine harte Abhängigkeit geschaffen. Dies ist gewünscht,
        // da die Standard-Java-Parser Limits bzgl. der maximalen Anzahl von XML-Entities definieren, die für die
        // Woodstox-Implementierung nicht nötig sind.
        XMLInputFactory wstxInputFactory = new WstxInputFactory();
        // Zugriff auf externe Ressourcen unterbinden
        wstxInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        wstxInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        return wstxInputFactory;
    }

    private void initializeJaxBContexts() throws JAXBException {
        Class<?>[] classes = { TransferOperation040502.class, TransferOperation050502.class };
        for (Class<?> clazz : classes) {
            contextByClass.put(
                clazz,
                JAXBContext.newInstance(clazz.getPackageName(), clazz.getClassLoader())
            );
        }
    }
}
