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
package de.dataport.suchdienst.aop;

import de.dataport.suchdienst.config.InstanceType;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.indexer.service.AntivirusScannerService;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.RequestUtils;
import de.dataport.suchdienst.util.model.MdcKeys;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

@Aspect
@Configuration
@Slf4j
public class ElasticAspect {

    private final MessageSource messageSource;
    private final SuchdienstProperties properties;
    private final AntivirusScannerService antivirusScanner;

    public ElasticAspect(
        SuchdienstProperties properties,
        AntivirusScannerService antivirusScanner,
        MessageSource messageSource
    ) {
        this.properties = properties;
        this.antivirusScanner = antivirusScanner;
        this.messageSource = messageSource;
    }

    @Around("@annotation(antivir)")
    public Object checkAntivirus(ProceedingJoinPoint joinPoint, AntivirusMethod antivir)
        throws Throwable {
        MultipartFile file = null;
        for (Object o : joinPoint.getArgs()) {
            if (o instanceof MultipartFile multipartFile) {
                file = multipartFile;
                break;
            }
        }

        try {
            assert file != null;
            boolean scan = antivirusScanner.scanFile(file);
            if (scan) {
                return joinPoint.proceed();
            } else {
                throw new SuchenException(
                    ErrorCodeEnum.VIRUS_DETECTED,
                    messageSource.getMessage(
                        "elastic_aspect.exception_virus_detected",
                        new String[] { RequestUtils.sanitizeUserInput(file.getName()) },
                        Locale.GERMAN
                    )
                );
            }
        } catch (IllegalStateException _) {
            //PV-8946 catch-rethrow
            throw new SuchenException(
                ErrorCodeEnum.VIRUS_SCANNER_ERROR,
                messageSource.getMessage(
                    "elastic_aspect.exception_virus_scanner_error",
                    new String[] { file.getName() },
                    Locale.GERMAN
                )
            );
        }
    }

    @Before("@annotation(master)")
    public void checkMaster(MasterMethod master) {
        if (properties.getInstanceType() == InstanceType.SLAVE) {
            SuchenException ex = new SuchenException(
                // "Slaves dürfen keine Veränderungen an ElasticSearch vornehmen.")
                ErrorCodeEnum.NO_MASTER,
                messageSource.getMessage(
                    "elastic_aspect.exception_no_master_error",
                    null,
                    Locale.GERMAN
                )
            );
            MdcTemplate.runWith(
                MdcKeys.ERROR_CODE,
                String.valueOf(ErrorCodeEnum.NO_MASTER.getNumber()),
                // PV-8945 Error ist hier gerechtfertigt, wenn dies passiert liegt eine produktionsrelevante Misskonfiguration vor
                () -> log.error(ex.getMessage(), ex)
            );
            throw ex;
        }
    }
}
