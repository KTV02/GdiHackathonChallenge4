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
package suchdienst.integrationstest.tests.arsimport;

import io.restassured.response.Response;
import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.AdminConnector;
import suchdienst.integrationstest.connectors.LocationsConnector;
import suchdienst.integrationstest.validator.locationsv2.LocationsValidator;

@SuppressWarnings("ConstantConditions")
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class ArsImportTest {

    private static final File fileAdded = new File(
        ArsImportTest.class.getClassLoader().getResource("./upload/ARS_added.txt").getFile()
    );
    private static final File fileModified = new File(
        ArsImportTest.class.getClassLoader().getResource("./upload/ARS_modified.txt").getFile()
    );
    private static final File fileDeleted = new File(
        ArsImportTest.class.getClassLoader().getResource("./upload/ARS_deleted.txt").getFile()
    );

    @AfterEach
    void tearDown() {
        //upload original ars file
        ImportUtil.resetArsData();
    }

    @Test
    void addLocation() {
        //initial test - "JesteburgZwei" is not available
        String locationToAdd = "JesteburgZwei";
        Response response = LocationsConnector.getLocationsV2(locationToAdd, 10);
        LocationsValidator validator = new LocationsValidator(response);
        validator.validateCount(0);

        AdminConnector.uploadArs(fileAdded);
        ImportUtil.waitForLocationCount(locationToAdd, 1);

        response = LocationsConnector.getLocationsV2(locationToAdd, 10);
        validator = new LocationsValidator(response);
        validator
            .validateCount(1)
            .validateName(0, locationToAdd)
            .validateArs(0, "033535405043")
            .validateZip(0, "21267");
    }

    @Test
    void modifyLocation() {
        //initial test - "KakenstorfModified" is not available
        String modifiedlocation = "KakenstorfModified";
        String originalLocation = "Kakenstorf";
        //should not contain modified name
        Response response = LocationsConnector.getLocationsV2(modifiedlocation, 10);
        LocationsValidator validator = new LocationsValidator(response);
        validator.validateCount(0);
        //should contain original name
        response = LocationsConnector.getLocationsV2(originalLocation, 10);
        validator = new LocationsValidator(response);
        validator.validateCount(1).validateName(0, originalLocation);

        //Upload modified file
        AdminConnector.uploadArs(fileModified);
        ImportUtil.waitForLocationCount(modifiedlocation, 1);

        //should contain modified name
        response = LocationsConnector.getLocationsV2(modifiedlocation, 10);
        validator = new LocationsValidator(response);
        validator.validateCount(1).validateName(0, modifiedlocation);
        //should not contain original name
        response = LocationsConnector.getLocationsV2(originalLocation, 10);
        validator = new LocationsValidator(response);
        validator.validateCount(0);
    }

    @Test
    void deleteLocation() {
        //initial test - "Regesbostel" is available
        String deletedlocation = "Regesbostel";
        //should contain modified name
        Response response = LocationsConnector.getLocationsV2(deletedlocation, 10);
        LocationsValidator validator = new LocationsValidator(response);
        validator.validateCount(1).validateName(0, deletedlocation);

        //Upload file with removed Location
        AdminConnector.uploadArs(fileDeleted);
        ImportUtil.waitForLocationCount(deletedlocation, 0);

        //should not contain removed name
        response = LocationsConnector.getLocationsV2(deletedlocation, 10);
        validator = new LocationsValidator(response);
        validator.validateCount(0);
    }
}
