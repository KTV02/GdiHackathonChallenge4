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
package suchdienst.integrationstest.tests.x22.datenimport.pushimportv7.formulare;

import de.dataport.pvog.testdatengenerator.generator.distributor.PushDistributor;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.tests.x22.datenimport.formulare.AddFormulareCommonTestCases;
import suchdienst.integrationstest.tests.x22.datenimport.formulare.NoDistributorParameterResolver;

/**
 * Sinn dieser Tests ist es, Situationen zu testen, in denen der Sammlerdienst Nachrichten mit neuen Entitäten sendet, und dabei
 * die Auswirkungen auf die VLs und deren Formulare zu prüfen.
 * Dieser Test testet also zusätzlich zu den bereits bestehenden Tests in AddTest den Import von Formularen im Push-Szenario.
 * Hier werden explizit nicht alle Permutationen von Business-Objekten getestet (wie im AddTest), sondern nur eine sinnvolle Auswahl,
 * bezogen auf Formulare. So vermeiden wir einen weiteren exponentiellen Anstieg der Testfälle.
 * Die Testcases befinden sich allesamt in der Superklasse und laufen mit einem NoDistributor.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class, NoDistributorParameterResolver.class })
class AddFormulareTest extends AddFormulareCommonTestCases {

    public AddFormulareTest() {
        super(new PushDistributor());
    }
}
