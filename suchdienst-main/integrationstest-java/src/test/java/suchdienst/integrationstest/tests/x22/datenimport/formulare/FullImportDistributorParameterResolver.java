/*-
 * ---license-start
 * Suchdienst Integrationstests
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package suchdienst.integrationstest.tests.x22.datenimport.formulare;

import de.dataport.pvog.testdatengenerator.generator.distributor.FullImportDistributor;
import de.dataport.pvog.testdatengenerator.generator.distributor.XzufiFileDistributor;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Tests, die sowohl für Push als auch für Pull laufen, können über einen ParameterResolver mit dem zum Szenario passenden Distributor
 * versorgt werden.
 * Diese Klasse ist für Pull-Tests gedacht, bei denen eine Clusterung der Sammlerdienst-Nachrichten stattfindet gibt.
 */
public class FullImportDistributorParameterResolver implements ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == XzufiFileDistributor.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == XzufiFileDistributor.class
            ? new FullImportDistributor()
            : null;
    }
}
