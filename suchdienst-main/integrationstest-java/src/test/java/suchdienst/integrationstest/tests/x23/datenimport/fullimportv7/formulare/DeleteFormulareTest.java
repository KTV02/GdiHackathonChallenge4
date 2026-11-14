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
package suchdienst.integrationstest.tests.x23.datenimport.fullimportv7.formulare;

import de.dataport.pvog.testdatengenerator.generator.distributor.XzufiFileDistributor;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.tests.x23.datenimport.formulare.DeleteFormulareCommonTestCases;
import suchdienst.integrationstest.tests.x23.datenimport.formulare.FullImportDistributorParameterResolver;

/**
 * Sinn dieser Tests ist es, Situationen zu testen, in denen der Sammlerdienst Löschnachrichten auf Formulare oder deren Zuständigkeiten
 * sendet, und dabei die Auswirkungen auf die VLs und deren Formulare zu prüfen.
 * Dieser Test testet also zusätzlich zu den bereits bestehenden Tests in DeleteTest das Löschen von Formularen im Pull-Szenario.
 * Hier werden explizit nicht alle Permutationen von Business-Objekten getestet (wie im DeleteTest), sondern nur eine sinnvolle Auswahl,
 * bezogen auf Formulare. So vermeiden wir einen weiteren exponentiellen Anstieg der Testfälle.
 * Die Testcases befinden sich allesamt in der Superklasse und laufen mit einem FullImportDistributor.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class, FullImportDistributorParameterResolver.class })
class DeleteFormulareTest extends DeleteFormulareCommonTestCases {

    public DeleteFormulareTest(XzufiFileDistributor distributor) {
        super(distributor);
    }
}
