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
package suchdienst.integrationstest.validator.servicedescriptionsdetailv6;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpStatus;

@SuppressWarnings("UnusedReturnValue")
public class DetailValidator<T> {

    private final String basepath;

    private final ValidatableResponse then;

    protected DetailValidator(Response response, String xzufiCategory) {
        this(response, xzufiCategory, true);
    }

    protected DetailValidator(Response response, String xzufiCategory, boolean skipPlaceholder) {
        StringBuilder sb = new StringBuilder("details.findAll");
        sb.append("{");
        sb.append("it.xZufiCategory == '").append(xzufiCategory).append("'");
        if (skipPlaceholder) {
            sb.append(" && it.text != 'nicht angegeben' && it.text != 'not specified'");
        }
        sb.append("}");
        basepath = sb.toString();
        this.then = response.then();
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T validateCount(int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + ".size()", is(expected));
        return self();
    }

    public T validateType(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].@type", equalTo(expected));
        return self();
    }

    public T validateTitle(int index, String expected) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(json.getString(basepath + "[" + index + "].title"));
        assertThat(actual, equalTo(expected));
        return self();
    }

    public T validateTitleByCode(String code, String expected) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();

        String actual = StringEscapeUtils.unescapeHtml4(
            json.getString(basepath + ".find {a -> a.code == '" + code + "'}.title")
        );
        assertThat(actual, equalTo(expected));
        return self();
    }

    public T validateText(int index, String expected) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(json.getString(basepath + "[" + index + "].text"));
        assertThat(actual, equalTo(expected));
        return self();
    }

    public T validateTextByCode(String code, String expected) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();

        String actual = StringEscapeUtils.unescapeHtml4(
            json.getString(basepath + ".find {a -> a.code == '" + code + "'}.text")
        );
        assertThat(actual, equalTo(expected));
        return self();
    }

    public T validateContainsNotCode(String code) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + ".findAll {a -> a.code == '" + code + "'}.size()", is(0));
        return self();
    }

    public T validateCode(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].code", equalTo(expected));
        return self();
    }

    public T validateLinkCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].links.size()", is(expected));
        return self();
    }

    public T validateLinkCountByCode(String code, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + ".find {it.code == '" + code + "'}.links.size()", is(expected));

        return self();
    }

    public T validateLinkUri(int index, int indexLink, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].links[" + indexLink + "].uri", is(expected));
        return self();
    }

    public T validateLinkUriByCode(String code, int indexLink, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + ".find {it.code == '" + code + "'}.links[" + indexLink + "].uri", is(expected));
        return self();
    }

    public T validateLinkTitle(int index, int indexLink, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].links[" + indexLink + "].title", is(expected));
        return self();
    }

    public T validateLinkTitleByCode(String code, int indexLink, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + ".find {it.code == '" + code + "'}.links[" + indexLink + "].title", is(expected));
        return self();
    }

    public T validateLinkDescription(int index, int indexLink, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].links[" + indexLink + "].description", is(expected));
        return self();
    }

    public T validateLinkDescriptionByCode(String code, int indexLink, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + ".find {it.code == '" + code + "'}.links[" + indexLink + "].description", is(expected));
        return self();
    }

    public T validateDetailsCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].details.size()", is(expected));
        return self();
    }

    public T validateDetailsTitle(int index, int indexDetail, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].details[" + indexDetail + "].title", equalTo(expected));
        return self();
    }

    public T validateDetailsText(int index, int indexDetail, String expected) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(
            json.getString(basepath + "[" + index + "].details[" + indexDetail + "].text")
        );
        assertThat(actual, equalTo(expected));
        return self();
    }

    public T validateDetailsCode(int index, int indexDetail, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].details[" + indexDetail + "].code", equalTo(expected));
        return self();
    }

    public T validateDetailsType(int index, int indexDetail, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].details[" + indexDetail + "].@type", equalTo(expected));
        return self();
    }

    public T validateAdditionalInformationCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].additionalInformation.size()", is(expected));
        return self();
    }

    public T validateAdditionalInformationCountByCode(String code, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + ".find {it.code == '" + code + "'}.additionalInformation.size()", is(expected));
        return self();
    }

    public T validateDetailsLinkCount(int index, int indexDetail, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].details[" + indexDetail + "].links.size()", is(expected));
        return self();
    }

    public T validateDetailsLinkUri(int index, int indexDetail, int indexLink, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                basepath + "[" + index + "].details[" + indexDetail + "].links[" + indexLink + "].uri",
                equalTo(expected)
            );
        return self();
    }

    public T validateDetailsLinkTitle(int index, int indexDetail, int indexLink, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                basepath + "[" + index + "].details[" + indexDetail + "].links[" + indexLink + "].title",
                equalTo(expected)
            );
        return self();
    }

    public T validateDetailsLinkDescription(int index, int indexDetail, int indexLink, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                basepath + "[" + index + "].details[" + indexDetail + "].links[" + indexLink + "].description",
                equalTo(expected)
            );
        return self();
    }

    public T validateDetailsLinkDynamicParameterCount(int index, int indexDetail, int indexLink, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                basepath +
                    "[" +
                    index +
                    "].details[" +
                    indexDetail +
                    "].links[" +
                    indexLink +
                    "].dynamicParameters.size()",
                is(expected)
            );
        return self();
    }

    public T validateDetailsAdditionalInformationCount(int index, int indexDetail, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].details[" + indexDetail + "].additionalInformation.size()", is(expected));
        return self();
    }

    public T validateDetailsAdditionalInformationByKey(int index, int indexDetail, String key, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                basepath +
                    "[" +
                    index +
                    "].details[" +
                    indexDetail +
                    "].additionalInformation.find {it.textKey == '" +
                    key +
                    "'}.textValue",
                equalTo(expected)
            );
        return self();
    }

    public T validateDetailsAdditionalInformation(int index, int indexDetail, int indexInfo, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                basepath +
                    "[" +
                    index +
                    "].details[" +
                    indexDetail +
                    "].additionalInformation[" +
                    indexInfo +
                    "].textValue",
                equalTo(expected)
            );
        return self();
    }

    public T validateAccessChannel(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].accessChannel", equalTo(expected));
        return self();
    }

    public T validateDate(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].date", equalTo(expected));
        return self();
    }
}
