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
package de.dataport.suchdienst.validator;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.ResultActions;

public abstract class ResultListValidator<T> {

    protected final ResultActions then;
    protected final String basepath;

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public ResultListValidator(ResultActions response) {
        this(response, "");
    }

    public ResultListValidator(ResultActions response, String base) {
        this.then = response;
        basepath = base;
    }

    public T validateContains(String expectedId) throws Exception {
        then.andExpectAll(
            status().isOk(),
            jsonPath("$." + basepath + "content[0].id").value(expectedId)
        );
        return self();
    }

    public T validateNoContent() throws Exception {
        then.andExpectAll(status().isOk(), jsonPath("$." + basepath + "content").isEmpty());
        return self();
    }
}
