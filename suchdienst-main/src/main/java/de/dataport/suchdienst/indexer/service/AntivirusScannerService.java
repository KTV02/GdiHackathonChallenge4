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

import de.dataport.pv_onlinegw.pv.virusscan.AntivirusScanner;
import de.dataport.pv_onlinegw.pv.virusscan.dummy.DoNothingAntivirusScanner;
import de.dataport.pv_onlinegw.pv.virusscan.icap.IcapAntivirusScanner;
import de.dataport.pv_onlinegw.pv.virusscan.icap.IcapServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ThreadSafe
@Slf4j
public class AntivirusScannerService {

    private static final String DO_NOTHING_ANTIVIRUS_SCANNER = "DoNothingAntivirusScanner";
    private static final String ICAP_ANTIVIRUS_SCANNER = "IcapAntivirusScanner";

    private final AntivirusScanner antivirusScanner;

    @Autowired
    AntivirusScannerService(
        Environment environment,
        @Value(
            "${de.dataport.pv_onlinegw.pv.virusscan.antivirusscanner.name}"
        ) String antivirusScannerName
    ) {
        if (
            ObjectUtils.isEmpty(antivirusScannerName) ||
            DO_NOTHING_ANTIVIRUS_SCANNER.equalsIgnoreCase(antivirusScannerName)
        ) {
            log.warn("Es wurde kein Antivirusscanner konfiguriert.");
            antivirusScanner = new DoNothingAntivirusScanner();
        } else if (!ICAP_ANTIVIRUS_SCANNER.equalsIgnoreCase(antivirusScannerName)) {
            log.warn("Antivirusscanner konnte nicht gefunden werden: {}", antivirusScannerName);
            antivirusScanner = new DoNothingAntivirusScanner();
        } else {
            // set server values for icap antivirus scanner
            String host = environment.getRequiredProperty(
                IcapAntivirusScanner.class.getName() + "." + "host"
            );
            Integer port = environment.getRequiredProperty(
                IcapAntivirusScanner.class.getName() + "." + "port",
                Integer.class
            );
            String icapServiceName = environment.getRequiredProperty(
                IcapAntivirusScanner.class.getName() + "." + "icapServiceName"
            );
            this.antivirusScanner = new IcapAntivirusScanner(
                new IcapServer(host, port, icapServiceName)
            );
        }
    }

    public boolean scanFile(MultipartFile file) throws IOException {
        return antivirusScanner.scanFile(file.getInputStream(), file.getSize());
    }

    public boolean scanFileContent(String fileContent) throws IOException {
        return antivirusScanner.scanFile(
            new ByteArrayInputStream(fileContent.getBytes()),
            fileContent.getBytes().length
        );
    }
}
