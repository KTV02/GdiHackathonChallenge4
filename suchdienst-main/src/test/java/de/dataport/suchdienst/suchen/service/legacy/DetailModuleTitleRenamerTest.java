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
package de.dataport.suchdienst.suchen.service.legacy;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.suchen.model.helper.MustContain;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.AbstractServiceModule;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceModule;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DetailModuleTitleRenamerTest {

    AbstractServiceModule module;
    Locale locale;

    @BeforeEach
    void setUp() {
        module = mock(ServiceModule.class);
        locale = Locale.GERMAN;
    }

    @Test
    void setsTitleWhenCodeIsRecognized() {
        when(module.getCode()).thenReturn("02");
        try (
            var codeMocked = mockStatic(ServiceModuleCode.class);
            var mustContainMocked = mockStatic(MustContain.class);
        ) {
            var code = ServiceModuleCode.LEISTUNGSBEZEICHNUNG;
            codeMocked.when(() -> ServiceModuleCode.fromCode("02")).thenReturn(Optional.of(code));
            var mustContain = mock(MustContain.class);
            mustContainMocked.when(() -> MustContain.create(code)).thenReturn(mustContain);
            when(mustContain.getTitle(locale)).thenReturn("Expected Title");
            DetailModuleTitleRenamer.renameModuleTitle(module, locale);
            verify(module).setTitle("Expected Title");
        }
    }

    @Test
    void doesNotSetTitleWhenCodeIsUnknown() {
        when(module.getCode()).thenReturn("UNKNOWN_CODE");
        try (var mocked = mockStatic(ServiceModuleCode.class)) {
            mocked
                .when(() -> ServiceModuleCode.fromCode("UNKNOWN_CODE"))
                .thenReturn(Optional.empty());
            DetailModuleTitleRenamer.renameModuleTitle(module, locale);
            verify(module, never()).setTitle(any());
        }
    }
}
