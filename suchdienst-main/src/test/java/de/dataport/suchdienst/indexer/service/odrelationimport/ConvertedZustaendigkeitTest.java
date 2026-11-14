/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.indexer.service.odrelationimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.importer.model.GenericZustaendigkeitTransferObjekt;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConvertedZustaendigkeitTest {

    @Test
    void xzufi23IsResolvedEagerlyAndReused() {
        GenericZustaendigkeitTransferObjekt generic = mock(
            GenericZustaendigkeitTransferObjekt.class
        );
        ZustaendigkeitTransferObjekt converted = new ZustaendigkeitTransferObjekt();
        when(generic.getId()).thenReturn("zs-1");
        when(generic.getParentId()).thenReturn("de.bund.OD.od-1");
        when(generic.getGebietIds()).thenReturn(List.of("091620000000"));
        when(generic.getLbIds()).thenReturn(List.of("de.bund.LB.lb-1"));
        when(generic.isXZuFi23()).thenReturn(true);
        when(generic.getOrConvertXZuFi23()).thenReturn(converted);

        ConvertedZustaendigkeit result = ConvertedZustaendigkeit.of(generic);

        assertThat(result.getXzufi23()).isSameAs(converted);
        assertThat(result.getXzufi23()).isSameAs(converted); // Wiederholter Zugriff darf keine erneute Konvertierung auslösen.
        verify(generic, times(1)).getOrConvertXZuFi23();
    }

    @Test
    void xzufi22IsConvertedLazilyOnlyOnFirstAccess() {
        GenericZustaendigkeitTransferObjekt generic = mock(
            GenericZustaendigkeitTransferObjekt.class
        );
        ZustaendigkeitTransferObjekt converted = new ZustaendigkeitTransferObjekt();
        when(generic.getId()).thenReturn("zs-2");
        when(generic.getParentId()).thenReturn("de.bund.OE.oe-1");
        when(generic.getGebietIds()).thenReturn(List.of("091620000000"));
        when(generic.getLbIds()).thenReturn(List.of("de.bund.LB.lb-2"));
        when(generic.isXZuFi23()).thenReturn(false);
        when(generic.getOrConvertXZuFi23()).thenReturn(converted);

        ConvertedZustaendigkeit result = ConvertedZustaendigkeit.of(generic);

        verify(generic, times(0)).getOrConvertXZuFi23();
        assertThat(result.getXzufi23()).isSameAs(converted);
        assertThat(result.getXzufi23()).isSameAs(converted); // Das Ergebnis der ersten Konvertierung muss wiederverwendet werden.
        verify(generic, times(1)).getOrConvertXZuFi23();
    }

    @Test
    void missingLeistungIdFailsFast() {
        GenericZustaendigkeitTransferObjekt generic = mock(
            GenericZustaendigkeitTransferObjekt.class
        );
        when(generic.getId()).thenReturn("zs-3");
        when(generic.getParentId()).thenReturn("de.bund.OD.od-2");
        when(generic.getGebietIds()).thenReturn(List.of("091620000000"));
        when(generic.getLbIds()).thenReturn(List.of());

        assertThatThrownBy(() -> ConvertedZustaendigkeit.of(generic))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Zustaendigkeit 'zs-3' hat keine Leistungs-ID");
    }
}
