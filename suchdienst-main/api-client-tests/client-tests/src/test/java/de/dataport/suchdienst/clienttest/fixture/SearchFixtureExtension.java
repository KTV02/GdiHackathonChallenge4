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

import java.io.IOException;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/** Teilt vorbereitete Lesedaten pro JUnit-Lauf; einzelne Klassenläufe erhalten dieselbe Vorbereitung. */
public final class SearchFixtureExtension implements ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(
        SearchFixtureExtension.class
    );
    private final Supplier<SearchFixture> factory;

    public SearchFixtureExtension() {
        this(SearchFixture::forTestServer);
    }

    SearchFixtureExtension(Supplier<SearchFixture> factory) {
        this.factory = factory;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == SearchFixture.class;
    }

    @Override
    public SearchFixture resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        // Im Root-Store teilen alle Operationsklassen die Ressource; JUnit schließt sie erst am Laufende.
        return extensionContext
            .getRoot()
            .getStore(NAMESPACE)
            .computeIfAbsent(SharedFixture.class, _ -> new SharedFixture(factory.get()), SharedFixture.class)
            .readyFixture();
    }

    private static final class SharedFixture implements AutoCloseable {

        private final SearchFixture fixture;
        private final Throwable initializationFailure;

        private SharedFixture(SearchFixture fixture) {
            this.fixture = fixture;
            Throwable failure = null;
            try {
                fixture.importData();
                fixture.recalculateOnlineServiceRelations();
                fixture.awaitVisibility(true);
            } catch (IOException | RuntimeException | AssertionError exception) {
                // Erst im Store registrieren, dann den Fehler weitergeben: Auch Teilimporte brauchen close().
                failure = exception;
            }
            initializationFailure = failure;
        }

        private SearchFixture readyFixture() {
            if (initializationFailure != null) {
                throw new ParameterResolutionException(
                    "Gemeinsame Such-Fixture konnte nicht vorbereitet werden",
                    initializationFailure
                );
            }
            return fixture;
        }

        @Override
        public void close() throws IOException {
            fixture.close();
        }
    }
}
