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
package de.dataport.suchdienst.suchen.dynamicparams;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Erweitert eine im Konstruktor angegebene URL mit den Werten einer dynamischen Parameterliste,
 * welche der Liste hinzugefügt werden können. Ist die Liste leer, wird die URL im Originalzustand zurückgegeben.
 * Beim Erweitern werden Unicode-Domains in ihre ASCII-kompatible Punycode-Darstellung umgewandelt.
 */
@Slf4j
public class BasicDynamicUrlBuilder {

    private final String onlineDienstUrl;
    private final List<DynamicParamPair> dynamicParamPairList = new ArrayList<>();

    public BasicDynamicUrlBuilder(String onlineDienstUrl) {
        this.onlineDienstUrl = onlineDienstUrl;
    }

    /**
     * Fügt einen Query Parameter der Liste hinzu
     *
     * @param dynamicParamPair Key-Value-Paar mit neuem Query Parameter
     */
    public void addDynamicParamPair(DynamicParamPair dynamicParamPair) {
        dynamicParamPairList.add(dynamicParamPair);
    }

    /**
     * Fügt alle Parameter zu onlineDienstUrl und erzeugt damit eine neue URL, welche um die gesetzten Query Parameter
     * erweitert wurde.
     *
     * @return Konkatenierte URL, bestehend aus einer URL und Parametern
     */
    public String build() {
        if (dynamicParamPairList.isEmpty()) {
            return onlineDienstUrl;
        }

        UriComponentsBuilder uriComponentsBuilder;
        try {
            URI onlineDienstUri = URI.create(onlineDienstUrl);
            // java.net.URI erkennt Unicode-Domains nur als Authority, nicht als Host. (PV-15275)
            if (onlineDienstUri.getRawAuthority() != null && onlineDienstUri.getHost() == null) {
                uriComponentsBuilder = UriComponentsBuilder.fromUriString(
                    onlineDienstUrl,
                    UriComponentsBuilder.ParserType.WHAT_WG
                );
            } else {
                uriComponentsBuilder = UriComponentsBuilder.newInstance().uri(onlineDienstUri);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Onlinedienst URL konnte nicht geparst werden: {}", e.getMessage());
            return onlineDienstUrl;
        }

        dynamicParamPairList.forEach(param ->
            uriComponentsBuilder.queryParam(param.parameterName(), param.parameterValue())
        );
        return uriComponentsBuilder.toUriString();
    }
}
