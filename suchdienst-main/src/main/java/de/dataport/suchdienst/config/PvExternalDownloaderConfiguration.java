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

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Sorgt per component scan dafür, dass die Beans aus der Library pv_externaldownloader zur Verfügung stehen.
 */
@Configuration
@ComponentScan("de.dataport.pvog.pvexternaldownloader")
public class PvExternalDownloaderConfiguration {}
