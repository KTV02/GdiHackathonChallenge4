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
package de.dataport.suchdienst.error;

import de.dataport.suchdienst.error.model.ApiErrorModel;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ApiErrorToOctetStreamConverter extends AbstractHttpMessageConverter<ApiErrorModel> {

    private final ObjectMapper mapper;

    public ApiErrorToOctetStreamConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return clazz == ApiErrorModel.class;
    }

    @Override
    protected ApiErrorModel readInternal(
        Class<? extends ApiErrorModel> clazz,
        HttpInputMessage inputMessage
    ) throws HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected void writeInternal(ApiErrorModel t, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException {
        outputMessage.getBody().write(mapper.writeValueAsBytes(t));
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return clazz == ApiErrorModel.class && MediaType.APPLICATION_OCTET_STREAM.equals(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return List.of(MediaType.APPLICATION_OCTET_STREAM);
    }
}
