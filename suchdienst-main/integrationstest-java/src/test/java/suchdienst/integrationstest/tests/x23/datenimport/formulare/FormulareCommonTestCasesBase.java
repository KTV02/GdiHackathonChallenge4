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
package suchdienst.integrationstest.tests.x23.datenimport.formulare;

import de.dataport.pvog.testdatengenerator.creator.businessobject.OeCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOrganisationseinheitCreator;
import de.dataport.pvog.testdatengenerator.generator.distributor.XzufiFileDistributor;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import java.util.List;

/**
 * Die gemeinsamen Testfälle der Formulare für Push und Pull haben identischen Code und unterscheiden sich nur in der zu verwendenden
 * Instanz des XzufiFileDistributor. Diesen spezifizieren wir in den konkreten Subklassen über einen ParameterResolver.
 */
public abstract class FormulareCommonTestCasesBase {

    protected final XzufiFileDistributor distributor;

    public FormulareCommonTestCasesBase(XzufiFileDistributor distributor) {
        this.distributor = distributor;
    }

    void addGlobalOe(XzufiFile file, String testcasename) {
        String bundesLandArs = "090000000000";
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit oe = OeCreator.getInstance().getMin(testcasename);
        Zustaendigkeit zs = ZsOrganisationseinheitCreator.getInstance().getMin(lb.getId(), oe.getId());
        zs.setArss(List.of(bundesLandArs));
        file.getOrganisationseinheiten().add(oe);
        file.getZustaendigkeitenOrganisationseinheit().add(zs);
    }
}
