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
package de.dataport.suchdienst.indexer.parser.x22;

import de.dataport.suchdienst.importer.model.GenericTransferOperation;
import de.dataport.suchdienst.importer.service.AlwaysAllowedImportChecker;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.util.XzufiObjectProvider;
import de.xoev.xzufi.v2_2_0.Leistung;
import de.xoev.xzufi.v2_2_0.LeistungSpezialisierung;
import de.xoev.xzufi.v2_2_0.OnlinedienstErweitert;
import de.xoev.xzufi.v2_2_0.TransferOperation040502;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;

/**
 * Gemeinsame Basis für Parser-Unit-Tests (x22) ohne Spring Kontext.
 * Verantwortlichkeiten:
 *  - Lädt einmalig eine Test-XML und stellt einen {@link XzufiObjectProvider}
 *  - Baut ein {@link MessageSource}, das produktive Properties-Dateien lädt
 *  - Stellt ein gemocktes {@link CodelistService} bereit (vereinfacht Codelist-Zugriff)
 */
abstract class AbstractX22ParserTest {

    protected XzufiObjectProvider provider;
    protected MessageSource messageSource;
    protected CodelistService codelistService;

    /** Pfad zur Standard-Testdatei analog zu bisherigen Tests */
    protected String xZufiFile;

    protected AbstractX22ParserTest() {
        this.xZufiFile = "x22/xzufi_parsertests.xml";
    }

    protected AbstractX22ParserTest(String xZufiFile) {
        this.xZufiFile = xZufiFile;
    }

    @BeforeEach
    void baseSetUp() throws Exception {
        provider = new XzufiObjectProvider(
            new GenericTransferOperation(loadTransferOperation(xZufiFile)),
            new AlwaysAllowedImportChecker()
        );
        messageSource = buildStaticMessageSource();
        codelistService = Mockito.mock(CodelistService.class);
        Mockito.when(
            codelistService.getCodelistEntry(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString()
            )
        ).thenAnswer(inv -> {
            CodelistEntry entry = new CodelistEntry();
            entry.setUri(inv.getArgument(0));
            entry.setCode(inv.getArgument(2));
            entry.setText(Locale.GERMAN, inv.getArgument(2));
            entry.setText(Locale.ENGLISH, inv.getArgument(2));
            return entry;
        });
    }

    /**
     * Liefert die erste Leistung als konvertiertes XZuFi22 Objekt.
     */
    protected Leistung firstLbRaw() {
        return provider.getLbs().findFirst().get().getOrConvertXZuFi22();
    }

    /**
     * Liefert die erste Spezialisierung als konvertiertes XZuFi22 Objekt.
     */
    protected LeistungSpezialisierung firstSpRaw() {
        return provider.getSps().findFirst().get().getOrConvertXZuFi22();
    }

    /**
     * Liefert den ersten Onlinedienst als konvertiertes XZuFi22 Objekt.
     */
    protected OnlinedienstErweitert firstOdRaw() {
        return provider.getOds().findFirst().get().getOrConvertXZuFi22();
    }

    private TransferOperation040502 loadTransferOperation(String resource) throws JAXBException {
        InputStream xmlStream = this.getClass().getClassLoader().getResourceAsStream(resource);
        JAXBContext jaxbContext = JAXBContext.newInstance(TransferOperation040502.class);
        return (TransferOperation040502) jaxbContext.createUnmarshaller().unmarshal(xmlStream);
    }

    /**
     * Baut eine StaticMessageSource, indem die produktiven Properties-Dateien geladen werden.
     * Dadurch vermeiden wir redundante manuelle Einträge in jedem Test.
     */
    private MessageSource buildStaticMessageSource() throws IOException {
        StaticMessageSource sms = new StaticMessageSource();
        loadPropertiesInto(sms, "static-text/static_text_resource.properties", Locale.GERMAN);
        loadPropertiesInto(sms, "static-text/static_text_resource_en.properties", Locale.ENGLISH);
        return sms;
    }

    private void loadPropertiesInto(StaticMessageSource sms, String path, Locale locale)
        throws IOException {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                return; // Datei nicht vorhanden -> ignorieren
            }
            Properties props = new Properties();
            props.load(is);
            for (String key : props.stringPropertyNames()) {
                // ResourceBundleMessageSource würde MessageFormat-Platzhalter nutzen; hier
                // belassen wir den Text direkt (Assertions in Tests nutzen nur konkrete Werte)
                sms.addMessage(key, locale, props.getProperty(key));
            }
        }
    }

    /**
     * Lädt eine beliebige XZuFi XML Datei in einen neuen Provider (für Negativtests etc.).
     */
    protected XzufiObjectProvider loadIntoProvider(String resource) throws Exception {
        return new XzufiObjectProvider(
            new GenericTransferOperation(loadTransferOperation(resource)),
            new AlwaysAllowedImportChecker()
        );
    }
}
