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
package suchdienst.integrationstest.tests.sonstiges;

import static de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland.BUND;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.OnlinedienstParam;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.OnlineserviceConnector;
import suchdienst.integrationstest.utils.WaitUtil;
import suchdienst.integrationstest.validator.onlineservicedetailv3.OnlineserviceValidator;

/**
 * Ziel des Tests ist es zu prüfen, dass Onlinedienst-Parameter HTML enthalten dürfen und diese
 * an der API im Klartext (unkodiert) ausgegeben werden.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class OnlineserviceParameterTest {

    private static final BundeslandConfig bundeslandConfig = new BundeslandConfig(BUND);

    @BeforeAll
    static void setup() {
        ElasticsearchConnector.removeAllData();
        //warte auf Löschen des Datenbestands
        WaitUtil.waitFor(1, TimeUnit.SECONDS);
    }

    @AfterAll
    static void cleanUp() {
        ElasticsearchConnector.removeAllData();
    }

    @Test
    void odParametersHtml(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(bundeslandConfig).name(testcasename).lb(1).oe(9).od(1).getMax();
        var onlinedienst = file.getOnlinedienste().get(0);
        addEfaParam(onlinedienst, "efa.custom.feedback.url", "https://address.local/feedback");
        addCustomParam(onlinedienst, "hello-<b>nice</b>-parameter-name", "hello-<b>nice</b>-parameter-value");
        ImportUtil.pushAndWait23(file);

        Response response = OnlineserviceConnector.getOnlineserviceDetailV3(onlinedienst.getFullId());

        OnlineserviceValidator validator = new OnlineserviceValidator(response);
        validator.validateParametersEfaNamesContains("efa.custom.feedback.url");
        validator.validateParametersEfaValuesContains("https://address.local/feedback");
        validator.validateParametersCustomNamesContains("hello-<b>nice</b>-parameter-name");
        validator.validateParametersCustomValuesContains("hello-<b>nice</b>-parameter-value");
    }

    @Test
    void odParametersEnglish(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance(bundeslandConfig).name(testcasename).lb(1).oe(9).od(1).getMax();
        var onlinedienst = file.getOnlinedienste().get(0);
        addEfaParam(
            onlinedienst,
            "efa.custom.feedback.url",
            "https://address.local/feedback",
            "https://address.local/feedback-en"
        );
        addCustomParam(
            onlinedienst,
            "hello-<b>nice</b>-parameter-name",
            "hello-<b>nice</b>-parameter-value",
            "hello-<b>nice</b>-parameter-value-en"
        );
        ImportUtil.pushAndWait23(file);

        Response response = OnlineserviceConnector.getOnlineserviceDetailV3(
            onlinedienst.getFullId(),
            Constants.LANGUAGE_EN
        );

        OnlineserviceValidator validator = new OnlineserviceValidator(response);
        validator.validateParametersEfaNamesContains("efa.custom.feedback.url");
        validator.validateParametersEfaValuesContains("https://address.local/feedback-en");
        validator.validateParametersCustomNamesContains("hello-<b>nice</b>-parameter-name");
        validator.validateParametersCustomValuesContains("hello-<b>nice</b>-parameter-value-en");
    }

    private void addEfaParam(Onlinedienst onlinedienst, String name, String wert) {
        addEfaParam(onlinedienst, name, wert, null);
    }

    private void addEfaParam(Onlinedienst onlinedienst, String name, String wert, String wertEnglisch) {
        OnlinedienstParam param = new OnlinedienstParam();
        param.setName(name);
        param.setWert(wert);
        param.setWertEnglisch(wertEnglisch);
        onlinedienst.getParameterEfa().add(param);
    }

    private void addCustomParam(Onlinedienst onlinedienst, String name, String wert) {
        addCustomParam(onlinedienst, name, wert, null);
    }

    private void addCustomParam(Onlinedienst onlinedienst, String name, String wert, String wertEnglisch) {
        OnlinedienstParam param = new OnlinedienstParam();
        param.setName(name);
        param.setWert(wert);
        param.setWertEnglisch(wertEnglisch);
        onlinedienst.getParameterIndividuell().add(param);
    }
}
