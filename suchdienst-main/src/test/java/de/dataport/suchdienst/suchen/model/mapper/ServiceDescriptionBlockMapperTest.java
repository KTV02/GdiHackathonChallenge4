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
package de.dataport.suchdienst.suchen.model.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.dataport.suchdienst.indexer.model.AccessChannel;
import de.dataport.suchdienst.indexer.model.mapping.XZufiCategory;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.util.ServiceModuleCode;
import org.junit.jupiter.api.Test;

class ServiceDescriptionBlockMapperTest {

    @Test
    void testMapSeDeBlockOnlineTitle() {
        // Arrange
        String title = "Test Title";
        ServiceModule module = ServiceModule.builder()
            .title(title)
            .text("Test Text")
            .accessChannel(AccessChannel.ONLINE)
            .build();

        // Act
        String result = ServiceDescriptionBlockMapper.INSTANCE.mapSeDeBlockTitle(module);

        // Assert
        assertThat(result, is(title + " (Online)"));
    }

    @Test
    void testMapSeDeBlockOfflineTitle() {
        // Arrange
        String title = "Test Title";
        ServiceModule module = ServiceModule.builder()
            .title(title)
            .text("Test Text")
            .accessChannel(AccessChannel.OFFLINE)
            .build();

        // Act
        String result = ServiceDescriptionBlockMapper.INSTANCE.mapSeDeBlockTitle(module);

        // Assert
        assertThat(result, is(title + " (Offline)"));
    }

    @Test
    void testMapSeDeBlockOnlineOfflineTitle() {
        // Arrange
        String title = "Test Title";
        ServiceModule module = ServiceModule.builder()
            .title(title)
            .text("Test Text")
            .accessChannel(AccessChannel.ONLINE_OFFLINE)
            .build();

        // Act
        String result = ServiceDescriptionBlockMapper.INSTANCE.mapSeDeBlockTitle(module);

        // Assert
        assertThat(result, is(title));
    }

    @Test
    void testMapSeDeBlockUnbekanntKanalTitle() {
        // Arrange
        String title = "Test Title";
        ServiceModule module = ServiceModule.builder()
            .title(title)
            .text("Test Text")
            .accessChannel(null)
            .build();

        // Act
        String result = ServiceDescriptionBlockMapper.INSTANCE.mapSeDeBlockTitle(module);

        // Assert
        assertThat(result, is(title));
    }

    @Test
    void testSeDeBlockModuleDetailOnlineTest() {
        ServiceModuleDetail block = ServiceModuleDetail.builder()
            .title("Test Title")
            .text("Test Text")
            .accessChannel(AccessChannel.ONLINE)
            .build();

        de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlockModuleDetail result =
            ServiceDescriptionBlockMapper.INSTANCE.map(
                block,
                ServiceModuleCode.LEISTUNGSNAME.getCode(),
                XZufiCategory.TEXT_MODUL
            );

        assertThat(result.getTitle(), is("Test Title (Online)"));
    }

    @Test
    void testSeDeBlockModuleDetailOnlineOfflineTest() {
        ServiceModuleDetail block = ServiceModuleDetail.builder()
            .title("Test Title")
            .text("Test Text")
            .accessChannel(AccessChannel.ONLINE_OFFLINE)
            .build();

        de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlockModuleDetail result =
            ServiceDescriptionBlockMapper.INSTANCE.map(
                block,
                ServiceModuleCode.LEISTUNGSNAME.getCode(),
                XZufiCategory.TEXT_MODUL
            );

        assertThat(result.getTitle(), is("Test Title"));
    }

    @Test
    void testSeDeBlockModuleDetailUnbekanntTest() {
        ServiceModuleDetail block = ServiceModuleDetail.builder()
            .title("Test Title")
            .text("Test Text")
            .build();

        de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlockModuleDetail result =
            ServiceDescriptionBlockMapper.INSTANCE.map(
                block,
                ServiceModuleCode.LEISTUNGSNAME.getCode(),
                XZufiCategory.TEXT_MODUL
            );

        assertThat(result.getTitle(), is("Test Title"));
    }

    @Test
    void testSeDeBlockModuleOfflineTest() {
        ServiceModule block = ServiceModule.builder()
            .title("Test Title")
            .text("Test Text")
            .accessChannel(AccessChannel.OFFLINE)
            .build();

        de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlockModule result =
            ServiceDescriptionBlockMapper.INSTANCE.map(
                block,
                ServiceModuleCode.LEISTUNGSNAME.getCode(),
                XZufiCategory.TEXT_MODUL
            );

        assertThat(result.getTitle(), is("Test Title (Offline)"));
    }

    @Test
    void testSeDeBlockModuleUnbekanntTest() {
        ServiceModule block = ServiceModule.builder().title("Test Title").text("Test Text").build();

        de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlockModule result =
            ServiceDescriptionBlockMapper.INSTANCE.map(
                block,
                ServiceModuleCode.LEISTUNGSNAME.getCode(),
                XZufiCategory.TEXT_MODUL
            );

        assertThat(result.getTitle(), is("Test Title"));
    }
}
