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
package de.dataport.suchdienst.util;

import de.dataport.suchdienst.util.model.MdcKeys;
import de.dataport.suchdienst.util.model.ProtocolType;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.MDC;

/**
 * Zentrale Anlaufstelle für den Umgang mit dem MDC (Mapped Diagnostic Context). Außerhalb dieser Klasse
 * soll {@link MDC} nicht direkt verwendet werden, damit MDC-Einträge am Ende eines Blocks immer auf ihren
 * vorherigen Wert zurückgesetzt werden und nicht versehentlich verloren gehen ("lost write").
 *
 * <p>Es gibt drei Anwendungsfälle:
 * <ol>
 *   <li><b>Block-Scope:</b> {@link #withProtokolltyp(ProtocolType)}, {@link #scoped(String, String)}
 *       bzw. {@link #scoped(Map)} in try-with-resources.</li>
 *   <li><b>Einzelnes Log-Statement:</b> {@link #runWith(String, String, Runnable)} bzw.
 *       {@link #runWith(Map, Runnable)}.</li>
 *   <li><b>Thread-Wechsel:</b> {@link #wrap(Runnable)} bzw. {@link #currentContext()} zusammen mit
 *       {@link #callWithContext(Map, Supplier)}.</li>
 * </ol>
 *
 * <p>Ein Schlüssel wird immer dort gesetzt, wo sein Gültigkeitsbereich beginnt. Steht sein Wert erst
 * später fest (z.B. der HTTP-Status einer externen Antwort), gehört er per {@link #runWith} an die
 * Log-Ausgabe, die ihn braucht — nicht in den MDC ohne zugehörigen Scope.
 *
 * <p>Soll ein Fehler-Log im catch-Block die MDC-Einträge noch tragen, muss der Scope das gesamte
 * try/catch umschließen, denn try-with-resources schließt die Ressource <em>vor</em> dem catch-Block:
 * <pre>{@code
 * try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.EXTERN)) {
 *     try {
 *         ...
 *     } catch (Exception e) {
 *         log.warn("EXTERN - FEHLER: ...", e); // trägt noch den Protokolltyp EXTERN
 *     }
 * }
 * }</pre>
 */
public final class MdcTemplate {

    private MdcTemplate() {}

    /**
     * Setzt den {@link MdcKeys#PROTOKOLLTYP} für die Dauer eines try-with-resources-Blocks. Ein zuvor
     * gesetzter Protokolltyp (z.B. {@link ProtocolType#ADMIN} aus dem RequestParameterFilter) wird
     * danach wiederhergestellt und geht nicht verloren.
     */
    public static MdcScope withProtokolltyp(ProtocolType protocolType) {
        return scoped(MdcKeys.PROTOKOLLTYP, protocolType.name());
    }

    /**
     * Setzt einen MDC-Eintrag für die Dauer eines try-with-resources-Blocks. Im Gegensatz zu
     * {@link MDC#putCloseable(String, String)} wird beim Schließen nicht der Schlüssel entfernt,
     * sondern der vorherige Wert wiederhergestellt ("lost write" vermeiden).
     * Ein {@code null}-Wert ist wie bei {@link MDC#put(String, String)} erlaubt.
     */
    public static MdcScope scoped(String mdcKey, String mdcValue) {
        return scoped(Collections.singletonMap(mdcKey, mdcValue));
    }

    /**
     * Setzt mehrere MDC-Einträge für die Dauer eines try-with-resources-Blocks, siehe
     * {@link #scoped(String, String)}.
     */
    public static MdcScope scoped(Map<String, String> mdcEntries) {
        return new MdcScope(mdcEntries);
    }

    /**
     * Führt die Aktion mit einem zusätzlichen MDC-Eintrag aus; danach gilt wieder der vorherige Zustand.
     * Gedacht für einzelne Log-Statements.
     */
    public static void runWith(String mdcKey, String mdcValue, Runnable action) {
        runWith(Collections.singletonMap(mdcKey, mdcValue), action);
    }

    /**
     * Führt die Aktion mit zusätzlichen MDC-Einträgen aus; danach gilt wieder der vorherige Zustand.
     * Die Einträge werden mit dem bestehenden MDC zusammengeführt, nicht ersetzt.
     */
    public static void runWith(Map<String, String> mdcEntries, Runnable action) {
        try (var _ = scoped(mdcEntries)) {
            action.run();
        }
    }

    /**
     * Kopie des MDC des aktuellen Threads, nie {@code null}. Zusammen mit
     * {@link #callWithContext(Map, Supplier)} für die Übergabe an einen anderen Thread.
     */
    public static Map<String, String> currentContext() {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return context == null ? Map.of() : context;
    }

    /**
     * Die Request-Id des laufenden Requests, oder {@code null} außerhalb eines Requests.
     */
    public static String currentRequestId() {
        return MDC.get(MdcKeys.REQUEST_ID);
    }

    /**
     * Führt den Supplier mit dem übergebenen MDC-Kontext aus und stellt danach den Kontext des
     * ausführenden Threads wieder her. Im Gegensatz zu {@link #runWith(Map, Runnable)} wird der MDC
     * vollständig <em>ersetzt</em>, gedacht für Tasks, die in einem fremden Thread laufen.
     */
    public static <T> T callWithContext(Map<String, String> mdcContext, Supplier<T> supplier) {
        Map<String, String> callerContext = currentContext();
        applyContext(mdcContext);
        try {
            return supplier.get();
        } finally {
            applyContext(callerContext);
        }
    }

    /**
     * Bindet den MDC des aufrufenden Threads an den Task, sodass er beim Ausführen in einem anderen
     * Thread erhalten bleibt und dieser Thread danach wieder seinen ursprünglichen MDC hat.
     * Für Executors, die keinen {@code TaskDecorator} unterstützen.
     */
    public static Runnable wrap(Runnable task) {
        Map<String, String> taskContext = currentContext();
        return () ->
            callWithContext(taskContext, () -> {
                task.run();
                return null;
            });
    }

    private static void applyContext(Map<String, String> context) {
        if (context == null || context.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }

    /**
     * Gültigkeitsbereich für MDC-Einträge, der beim Schließen den vorherigen Zustand wiederherstellt.
     * Wird über {@link MdcTemplate#scoped(Map)} bzw. {@link MdcTemplate#withProtokolltyp(ProtocolType)}
     * erzeugt und in try-with-resources verwendet.
     */
    public static final class MdcScope implements AutoCloseable {

        private final Map<String, String> previousValues = new HashMap<>();
        private final Set<String> previouslyUnsetKeys = new HashSet<>();

        private MdcScope(Map<String, String> mdcEntries) {
            if (mdcEntries == null) {
                return;
            }
            mdcEntries.forEach((key, value) -> {
                rememberPreviousValue(key);
                MDC.put(key, value);
            });
        }

        private void rememberPreviousValue(String key) {
            String previousValue = MDC.get(key);
            if (previousValue == null) {
                previouslyUnsetKeys.add(key);
            } else {
                previousValues.put(key, previousValue);
            }
        }

        @Override
        public void close() {
            previouslyUnsetKeys.forEach(MDC::remove);
            previousValues.forEach(MDC::put);
        }
    }
}
