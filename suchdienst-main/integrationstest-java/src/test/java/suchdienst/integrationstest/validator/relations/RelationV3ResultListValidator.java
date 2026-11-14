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
package suchdienst.integrationstest.validator.relations;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

@SuppressWarnings("UnusedReturnValue")
public class RelationV3ResultListValidator {

    private final ValidatableResponse then;

    public RelationV3ResultListValidator(Response response) {
        this.then = response.then();
    }

    public RelationV3ResultListValidator validateCount(int expectedCount) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("content.size()", is(expectedCount));
        return this;
    }

    public RelationV3ResultListValidator validateParentClass(int index, String expectedCode, String expectedName) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("content[" + index + "].uebergeordnetesObjektKlasse.code", equalTo(expectedCode))
            .body("content[" + index + "].uebergeordnetesObjektKlasse.name", equalTo(expectedName))
            .body(
                "content[" + index + "].uebergeordnetesObjektKlasse.listURI",
                equalTo("urn:xoev-de:fim:codeliste:xzufi.transferzustaendigkeittyp")
            )
            .body("content[" + index + "].uebergeordnetesObjektKlasse.listVersionID", equalTo("1"));
        return this;
    }

    public RelationV3ResultListValidator validateParentIdValue(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("content[" + index + "].uebergeordnetesObjektID.value", equalTo(expected));
        return this;
    }

    public RelationV3ResultListValidator validateParentIdSchemeAgencyId(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("content[" + index + "].uebergeordnetesObjektID.schemeAgencyID", equalTo(expected));
        return this;
    }

    public RelationV3ResultListValidator validateIdValue(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("content[" + index + "].zustaendigkeit[0].id.value", equalTo(expected));
        return this;
    }

    public RelationV3ResultListValidator validateIdSchemeAgencyId(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("content[" + index + "].zustaendigkeit[0].id.schemeAgencyID", equalTo(expected));
        return this;
    }

    public RelationV3ResultListValidator validateLeistungidValue(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("content[" + index + "].zustaendigkeit[0].leistungID[0].value", equalTo(expected));
        return this;
    }

    public RelationV3ResultListValidator validateLeistungidSchemeAgencyId(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("content[" + index + "].zustaendigkeit[0].leistungID[0].schemeAgencyID", equalTo(expected));
        return this;
    }

    public RelationV3ResultListValidator validateGebietidValue(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("content[" + index + "].zustaendigkeit[0].gebietID[0].value", equalTo(expected));
        return this;
    }

    public RelationV3ResultListValidator validateIdSekundaerValue(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("content[" + index + "].zustaendigkeit[0].idSekundaer[0].value", equalTo(expected));
        return this;
    }

    public RelationV3ResultListValidator validateKommunikationssystemDestinationSignatureValue(
        int index,
        String expected
    ) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("content[" + index + "].zustaendigkeit[0].kommunikationssystem[0].kennung", equalTo(expected));
        return this;
    }

    public RelationV3ResultListValidator validateIdSekundaerSchemeAgencyId(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("content[" + index + "].zustaendigkeit[0].idSekundaer[0].schemeAgencyID", equalTo(expected));
        return this;
    }

    public RelationV3ResultListValidator validateIdSekundaerSchemeId(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("content[" + index + "].zustaendigkeit[0].idSekundaer[0].schemeID", equalTo(expected));
        return this;
    }
}
