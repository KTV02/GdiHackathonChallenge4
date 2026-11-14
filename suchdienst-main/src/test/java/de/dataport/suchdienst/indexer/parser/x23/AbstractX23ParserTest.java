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
package de.dataport.suchdienst.indexer.parser.x23;

import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.dataport.suchdienst.importer.model.GenericTransferOperation;
import de.dataport.suchdienst.importer.service.AlwaysAllowedImportChecker;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.util.XzufiObjectProvider;
import de.xoev.xzufi.v2_3_1.Leistung;
import de.xoev.xzufi.v2_3_1.LeistungSpezialisierung;
import de.xoev.xzufi.v2_3_1.OnlinedienstErweitert;
import de.xoev.xzufi.v2_3_1.TransferOperation050502;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;

/**
 * Gemeinsame Basis für Parser-Unit-Tests (x23) ohne Spring Kontext.
 * Verantwortlichkeiten analog x22 Basis:
 *  - Lädt Test-XML und baut {@link XzufiObjectProvider}
 *  - Lädt produktive Properties in eine {@link StaticMessageSource}
 *  - Stellt ein gemocktes {@link CodelistService} bereit
 *  - Initialisiert Streams für Onlinedienst-Tests (unparsedOds)
 */
abstract class AbstractX23ParserTest {

    protected XzufiObjectProvider provider;
    protected MessageSource messageSource;
    protected CodelistService codelistService;
    protected String xZufiFile;

    protected Stream<OnlinedienstErweitert> unparsedOds;

    protected AbstractX23ParserTest() {
        this.xZufiFile = "x23/xzufi_parsertests.xml";
    }

    protected AbstractX23ParserTest(String xZufiFile) {
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

        // Streams analog ehemaligem ModulParserTest
        unparsedOds = provider.getOds().map(GenericOnlinedienst::getOrConvertXZuFi23);
    }

    protected Leistung firstLbRaw() {
        return provider.getLbs().findFirst().get().getOrConvertXZuFi23();
    }

    protected LeistungSpezialisierung firstSpRaw() {
        return provider.getSps().findFirst().get().getOrConvertXZuFi23();
    }

    protected OnlinedienstErweitert firstOdRaw() {
        return provider.getOds().findFirst().get().getOrConvertXZuFi23();
    }

    private TransferOperation050502 loadTransferOperation(String resource) throws JAXBException {
        InputStream xmlStream = this.getClass().getClassLoader().getResourceAsStream(resource);
        JAXBContext jaxbContext = JAXBContext.newInstance(TransferOperation050502.class);
        return (TransferOperation050502) jaxbContext.createUnmarshaller().unmarshal(xmlStream);
    }

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
                sms.addMessage(key, locale, props.getProperty(key));
            }
        }
    }

    protected XzufiObjectProvider loadIntoProvider(String resource) throws Exception {
        return new XzufiObjectProvider(
            new GenericTransferOperation(loadTransferOperation(resource)),
            new AlwaysAllowedImportChecker()
        );
    }
}
