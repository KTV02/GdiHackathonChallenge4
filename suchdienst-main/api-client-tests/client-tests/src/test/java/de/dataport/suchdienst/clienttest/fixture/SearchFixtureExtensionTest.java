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
package de.dataport.suchdienst.clienttest.fixture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * Testziel: Die SearchFixtureExtension bereitet eine Fixture pro JUnit-Lauf genau einmal
 * für alle Consumer-Klassen vor und bereinigt sie erst am Laufende, auch bei Test-
 * oder Vorbereitungsfehlern. Fehlgeschlagene Vorbereitung wird nicht wiederholt;
 * getrennte JUnit-Läufe teilen keine Ressourcen.
 *
 * <p>Die statischen Consumer-Klassen sind Testprogramme für das {@link EngineTestKit}, keine
 * eigenständig zu startenden Surefire-Tests. Die explizite Auswahl mit {@code selectClass(...)}
 * benötigt kein Test-Suffix. Jeder {@code execute()}-Aufruf startet einen eigenen JUnit-Lauf
 * samt Root-Store und dessen Bereinigung. FailingConsumer schlägt darin absichtlich
 * fehl; der äußere Test prüft diesen erwarteten Fehler über die Engine-Events.
 */
class SearchFixtureExtensionTest {

    @Test
    void preparesOnceForMultipleClassesAndClosesAtRunEnd() throws IOException {
        var fixture = mock(SearchFixture.class);
        register(fixture);
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(FirstConsumer.class), selectClass(SecondConsumer.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(2).succeeded(2));
        verify(fixture).importData();
        verify(fixture).recalculateOnlineServiceRelations();
        verify(fixture).awaitVisibility(true);
        verify(fixture).close();
    }

    @Test
    void preparesAndClosesWhenOnlyOneClassIsSelected() throws IOException {
        var fixture = mock(SearchFixture.class);
        register(fixture);
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(SecondConsumer.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(1).succeeded(1));
        verify(fixture).importData();
        verify(fixture).close();
    }

    @Test
    void closesAtRunEndWhenATestFails() throws IOException {
        var fixture = mock(SearchFixture.class);
        register(fixture);
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(FailingConsumer.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(1).failed(1));
        verify(fixture).close();
    }

    @Test
    void doesNotRetryFailedPreparationAndStillCloses() throws IOException {
        var fixture = mock(SearchFixture.class);
        doThrow(new IOException("expected import failure")).when(fixture).importData();
        register(fixture);
        var result = EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(FirstConsumer.class), selectClass(SecondConsumer.class))
            .execute();
        // Die Parameterauflösung in @BeforeAll scheitert: Klassencontainer schlagen fehl, Testmethoden starten nicht.
        result.containerEvents().assertStatistics(stats -> stats.failed(2));
        result.testEvents().assertStatistics(stats -> stats.started(0));
        verify(fixture).importData();
        verify(fixture, never()).recalculateOnlineServiceRelations();
        verify(fixture).close();
    }

    @Test
    void doesNotShareResourcesBetweenRuns() throws IOException {
        var firstFixture = mock(SearchFixture.class);
        var secondFixture = mock(SearchFixture.class);
        var fixtures = new ArrayDeque<>(List.of(firstFixture, secondFixture));
        FirstConsumer.extension = new SearchFixtureExtension(fixtures::removeFirst);
        // Gleiche Consumer-Klasse, aber neue Root-Stores: Jeder Lauf muss die nächste Fixture anfordern.
        for (int run = 0; run < 2; run++) {
            EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(FirstConsumer.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1));
        }
        for (var fixture : List.of(firstFixture, secondFixture)) {
            verify(fixture).importData();
            verify(fixture).close();
        }
        assertTrue(fixtures.isEmpty());
    }

    /** Übergibt den Mock an die vom EngineTestKit separat instanziierten Consumer. */
    private static void register(SearchFixture fixture) {
        FirstConsumer.extension = new SearchFixtureExtension(() -> fixture);
        SecondConsumer.extension = new SearchFixtureExtension(() -> fixture);
        FailingConsumer.extension = new SearchFixtureExtension(() -> fixture);
    }

    /** Testziel: Die Fixture bleibt während der Ausführung der ersten Consumer-Klasse geöffnet. */
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    static class FirstConsumer {

        @RegisterExtension
        static SearchFixtureExtension extension;

        private SearchFixture fixture;

        @BeforeAll
        void prepare(SearchFixture sharedFixture) {
            fixture = sharedFixture;
        }

        @Test
        void fixtureIsNotClosedWhileAClassIsRunning() throws IOException {
            verify(fixture, never()).close();
        }
    }

    /** Testziel: Eine weitere Consumer-Klasse verwendet die gemeinsame Fixture ohne vorzeitige Bereinigung. */
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    static class SecondConsumer {

        @RegisterExtension
        static SearchFixtureExtension extension;

        private SearchFixture fixture;

        @BeforeAll
        void prepare(SearchFixture sharedFixture) {
            fixture = sharedFixture;
        }

        @Test
        void fixtureIsNotClosedByAnotherClass() throws IOException {
            verify(fixture, never()).close();
        }
    }

    /** Testziel: Ein absichtlicher Consumer-Testfehler verhindert die Bereinigung am Laufende nicht. */
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    static class FailingConsumer {

        @RegisterExtension
        static SearchFixtureExtension extension;

        /** Schon die Parameterauflösung bereitet die Fixture vor; der Methodenrumpf darf leer bleiben. */
        @BeforeAll
        void prepare(SearchFixture sharedFixture) {}

        @Test
        void failsDeliberately() {
            fail("expected test failure");
        }
    }
}
