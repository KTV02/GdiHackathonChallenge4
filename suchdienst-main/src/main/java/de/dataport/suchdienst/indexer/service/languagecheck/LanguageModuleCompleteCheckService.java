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
package de.dataport.suchdienst.indexer.service.languagecheck;

import de.dataport.suchdienst.indexer.model.AccessChannel;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

public class LanguageModuleCompleteCheckService implements LanguageCompleteCheck {

    public boolean checkModulesComplete(
        List<AbstractServiceModule> germanDetails,
        List<AbstractServiceModule> otherDetails
    ) {
        if (germanDetails == null) {
            // If germanDetails is null, consider it complete regardless of otherDetails
            return true;
        }
        if (otherDetails == null) {
            // If germanDetails is not null but otherDetails is null, not complete
            return false;
        }

        // Ein Modul ist vollständig übersetzt, wenn es gleich viel oder mehr Inhalte in der Fremdsprache gibt.
        if (germanDetails.size() > otherDetails.size()) {
            // Es gibt mehr deutsche Details als in der Fremdsprache => nicht vollständig übersetzt
            return false;
        }
        EnumMap<AccessChannel, AbstractServiceModule> germanDetailMap = groupByAccessChannel(
            germanDetails
        );
        EnumMap<AccessChannel, AbstractServiceModule> otherDetailMap = groupByAccessChannel(
            otherDetails
        );

        if (germanDetailMap.size() > otherDetailMap.size()) {
            return false;
        }

        return Arrays.stream(AccessChannel.values()).allMatch(channel ->
            moduleComplete(germanDetailMap.get(channel), otherDetailMap.get(channel))
        );
    }

    private boolean moduleComplete(
        AbstractServiceModule germanModule,
        AbstractServiceModule englishModule
    ) {
        if (germanModule == null && englishModule == null) {
            return true;
        }
        if (
            germanModule instanceof ServiceModule germanServiceModule &&
            englishModule instanceof ServiceModule englishServiceModule
        ) {
            return moduleComplete(germanServiceModule, englishServiceModule);
        }
        if (
            germanModule instanceof ServiceModuleDetail germanServiceModuleDetail &&
            englishModule instanceof ServiceModuleDetail englishServiceModuleDetail
        ) {
            return moduleComplete(germanServiceModuleDetail, englishServiceModuleDetail);
        }
        return false;
    }

    private boolean moduleComplete(ServiceModule germanModule, ServiceModule englishModule) {
        return (
            stringFieldComplete(germanModule.getText(), englishModule.getText()) &&
            stringFieldComplete(germanModule.getTitle(), englishModule.getTitle()) &&
            germanModule.getAdditionalInformation().size() <=
                englishModule.getAdditionalInformation().size() &&
            germanModule.getLinks().size() <= englishModule.getLinks().size()
        );
    }

    private boolean moduleComplete(
        ServiceModuleDetail germanModule,
        ServiceModuleDetail englishModule
    ) {
        return (
            stringFieldComplete(germanModule.getTitle(), englishModule.getTitle()) &&
            stringFieldComplete(germanModule.getText(), englishModule.getText()) &&
            germanModule.getDetails().size() == englishModule.getDetails().size() &&
            germanModule.getLinks().size() == englishModule.getLinks().size()
        );
    }

    EnumMap<AccessChannel, AbstractServiceModule> groupByAccessChannel(
        List<AbstractServiceModule> details
    ) {
        EnumMap<AccessChannel, AbstractServiceModule> map = new EnumMap<>(AccessChannel.class);
        for (AbstractServiceModule detail : details) {
            map.put(detail.getAccessChannel(), detail);
        }
        return map;
    }
}
