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

import io.restassured.response.Response;

@SuppressWarnings("UnusedReturnValue")
public class SingleDetailValidator<T> {

    private final DetailValidator<T> detailValidator;

    protected SingleDetailValidator(Response response, String xzufiCategory) {
        detailValidator = new DetailValidator<>(response, xzufiCategory);
    }

    protected SingleDetailValidator(Response response, String xzufiCategory, boolean skipPlaceholder) {
        detailValidator = new DetailValidator<>(response, xzufiCategory, skipPlaceholder);
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T validateCount(int expected) {
        detailValidator.validateCount(expected);
        return self();
    }

    public T validateType(String expected) {
        detailValidator.validateType(0, expected);
        return self();
    }

    public T validateTitle(String expected) {
        detailValidator.validateTitle(0, expected);
        return self();
    }

    public T validateCode(String expected) {
        detailValidator.validateCode(0, expected);
        return self();
    }

    public T validateText(String expected) {
        detailValidator.validateText(0, expected);
        return self();
    }

    public T validateLinkCount(int expected) {
        detailValidator.validateLinkCount(0, expected);
        return self();
    }

    public T validateLinkUri(int index, String expected) {
        detailValidator.validateLinkUri(0, index, expected);
        return self();
    }

    public T validateLinkTitle(int index, String expected) {
        detailValidator.validateLinkTitle(0, index, expected);
        return self();
    }

    public T validateLinkDescription(int index, String expected) {
        detailValidator.validateLinkDescription(0, index, expected);
        return self();
    }

    public T validateAdditionalInformationCount(int expected) {
        detailValidator.validateAdditionalInformationCount(0, expected);
        return self();
    }

    public T validateDetailsCount(int expected) {
        detailValidator.validateDetailsCount(0, expected);
        return self();
    }

    public T validateDetailsTitle(int index, String expected) {
        detailValidator.validateDetailsTitle(0, index, expected);
        return self();
    }

    public T validateDetailsText(int index, String expected) {
        detailValidator.validateDetailsText(0, index, expected);
        return self();
    }

    public T validateDetailsLinkCount(int indexDetail, int expected) {
        detailValidator.validateDetailsLinkCount(0, indexDetail, expected);
        return self();
    }

    public T validateDetailsLinkUri(int indexDetail, int indexLink, String expected) {
        detailValidator.validateDetailsLinkUri(0, indexDetail, indexLink, expected);
        return self();
    }

    public T validateDetailsLinkTitle(int indexDetail, int indexLink, String expected) {
        detailValidator.validateDetailsLinkTitle(0, indexDetail, indexLink, expected);
        return self();
    }

    public T validateDetailsLinkDescription(int indexDetail, int indexLink, String expected) {
        detailValidator.validateDetailsLinkDescription(0, indexDetail, indexLink, expected);
        return self();
    }

    public T validateDetailsAdditionalInformationCount(int indexDetail, int expected) {
        detailValidator.validateDetailsAdditionalInformationCount(0, indexDetail, expected);
        return self();
    }

    public T validateDetailsAdditionalInformationByKey(int indexDetail, String key, String expected) {
        detailValidator.validateDetailsAdditionalInformationByKey(0, indexDetail, key, expected);
        return self();
    }

    public T validateDetailsAdditionalInformation(int indexDetail, int indexInfo, String expected) {
        detailValidator.validateDetailsAdditionalInformation(0, indexDetail, indexInfo, expected);
        return self();
    }

    public T validateAccessChannel(String expected) {
        detailValidator.validateAccessChannel(0, expected);
        return self();
    }

    public T validateDate(String expected) {
        detailValidator.validateDate(0, expected);
        return self();
    }
}
