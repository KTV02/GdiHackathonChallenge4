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
package de.dataport.suchdienst.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.dataport.suchdienst.util.model.MdcKeys;
import de.dataport.suchdienst.util.model.ProtocolType;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;

/**
 * Testet insbesondere, dass zuvor gesetzte MDC-Werte nicht verloren gehen ("lost write").
 */
@ExtendWith(MdcCleanupExtension.class)
class MdcTemplateTest {

    @Test
    void stellt_den_vorherigen_protokolltyp_nach_dem_scope_wieder_her() {
        MDC.put(MdcKeys.PROTOKOLLTYP, ProtocolType.ADMIN.name());

        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.EXTERN)) {
            assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.EXTERN.name());
        }

        assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.ADMIN.name());
    }

    @Test
    void entfernt_den_schluessel_wenn_er_vorher_nicht_gesetzt_war() {
        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.EXTERN)) {
            assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.EXTERN.name());
        }

        assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isNull();
    }

    @Test
    void stellt_einen_vorher_gesetzten_leeren_wert_wieder_her() {
        MDC.put(MdcKeys.PROTOKOLLTYP, "");

        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.EXTERN)) {
            assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.EXTERN.name());
        }

        assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEmpty();
    }

    @Test
    void stellt_den_vorherigen_zustand_auch_bei_einer_exception_wieder_her() {
        MDC.put(MdcKeys.PROTOKOLLTYP, ProtocolType.ADMIN.name());

        assertThatThrownBy(() -> {
            try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.EXTERN)) {
                throw new IllegalStateException("kaputt");
            }
        }).isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.ADMIN.name());
    }

    @Test
    void stellt_mehrere_schluessel_unabhaengig_voneinander_wieder_her() {
        MDC.put(MdcKeys.PROTOKOLLTYP, ProtocolType.ADMIN.name());

        try (
            var _ = MdcTemplate.scoped(
                Map.of(
                    MdcKeys.PROTOKOLLTYP,
                    ProtocolType.EXTERN.name(),
                    MdcKeys.EXTERNER_BEREICH,
                    "GEODATEN"
                )
            )
        ) {
            assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.EXTERN.name());
            assertThat(MDC.get(MdcKeys.EXTERNER_BEREICH)).isEqualTo("GEODATEN");
        }

        assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.ADMIN.name());
        assertThat(MDC.get(MdcKeys.EXTERNER_BEREICH)).isNull();
    }

    /**
     * Ziel: Ein {@code null}-Wert ist erlaubt, wie bei {@code MDC.put}. Aufrufer setzen Schluessel aus
     * Nutzdaten (z.B. die zuletzt verarbeitete LB-Id), die noch nicht bekannt sein koennen.
     */
    @Test
    void erlaubt_einen_null_wert() {
        MDC.put(MdcKeys.LBID, "lb-1");

        try (var _ = MdcTemplate.scoped(MdcKeys.LBID, null)) {
            assertThat(MDC.get(MdcKeys.LBID)).isNull();
        }
        assertThat(MDC.get(MdcKeys.LBID)).isEqualTo("lb-1");

        MdcTemplate.runWith(MdcKeys.LBID, null, () -> assertThat(MDC.get(MdcKeys.LBID)).isNull());
        assertThat(MDC.get(MdcKeys.LBID)).isEqualTo("lb-1");
    }

    @Test
    void runWith_stellt_den_vorherigen_zustand_wieder_her() {
        MDC.put(MdcKeys.PROTOKOLLTYP, ProtocolType.ADMIN.name());

        MdcTemplate.runWith(MdcKeys.PROTOKOLLTYP, ProtocolType.IMPORT.name(), () ->
            assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.IMPORT.name())
        );

        assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.ADMIN.name());
    }

    @Test
    void verschachtelte_scopes_auf_demselben_schluessel_bauen_sich_wieder_ab() {
        try (var _ = MdcTemplate.scoped(MdcKeys.OEID, "aussen")) {
            try (var _ = MdcTemplate.scoped(MdcKeys.OEID, "innen")) {
                assertThat(MDC.get(MdcKeys.OEID)).isEqualTo("innen");
            }
            assertThat(MDC.get(MdcKeys.OEID)).isEqualTo("aussen");
        }
        assertThat(MDC.get(MdcKeys.OEID)).isNull();
    }

    @Test
    void callWithContext_ersetzt_den_kontext_und_stellt_ihn_danach_wieder_her() {
        MDC.put(MdcKeys.PROTOKOLLTYP, ProtocolType.ADMIN.name());
        MDC.put(MdcKeys.REQUEST_ID, "request-1");

        String protokolltyp = MdcTemplate.callWithContext(
            Map.of(MdcKeys.PROTOKOLLTYP, ProtocolType.IMPORT.name()),
            () -> {
                assertThat(MDC.get(MdcKeys.REQUEST_ID)).isNull();
                return MDC.get(MdcKeys.PROTOKOLLTYP);
            }
        );

        assertThat(protokolltyp).isEqualTo(ProtocolType.IMPORT.name());
        assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.ADMIN.name());
        assertThat(MDC.get(MdcKeys.REQUEST_ID)).isEqualTo("request-1");
    }

    @Test
    void wrap_uebertraegt_den_mdc_in_den_anderen_thread_und_hinterlaesst_ihn_sauber()
        throws Exception {
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            MDC.put(MdcKeys.REQUEST_ID, "request-1");
            var requestIdImTask = new String[1];
            executor
                .submit(MdcTemplate.wrap(() -> requestIdImTask[0] = MdcTemplate.currentRequestId()))
                .get();
            assertThat(requestIdImTask[0]).isEqualTo("request-1");

            // Ein Folge-Task auf demselben Thread darf den Kontext des Vorgaengers nicht mehr sehen.
            MDC.clear();
            executor
                .submit(MdcTemplate.wrap(() -> requestIdImTask[0] = MdcTemplate.currentRequestId()))
                .get();
            assertThat(requestIdImTask[0]).isNull();
        }
    }

    /**
     * Dokumentiert, warum der MDC-Scope das gesamte try/catch umschließen muss: try-with-resources
     * schließt die Ressource vor dem catch-Block, ein Fehler-Log dort saehe die Eintraege sonst nicht.
     */
    @Test
    void scope_ist_im_catch_block_nur_bei_umschliessendem_scope_noch_gesetzt() {
        String protokolltypImInnerenCatch;
        String protokolltypImAeusserenCatch;

        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.EXTERN)) {
            try {
                throw new IllegalStateException("kaputt");
            } catch (IllegalStateException _) {
                protokolltypImInnerenCatch = MDC.get(MdcKeys.PROTOKOLLTYP);
            }
        }
        assertThat(protokolltypImInnerenCatch).isEqualTo(ProtocolType.EXTERN.name());

        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.EXTERN)) {
            throw new IllegalStateException("kaputt");
        } catch (IllegalStateException _) {
            protokolltypImAeusserenCatch = MDC.get(MdcKeys.PROTOKOLLTYP);
        }
        assertThat(protokolltypImAeusserenCatch).isNull();
    }
}
