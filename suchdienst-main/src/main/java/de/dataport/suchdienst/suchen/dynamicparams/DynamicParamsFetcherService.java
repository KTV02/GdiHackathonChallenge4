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

import de.dataport.suchdienst.indexer.model.mapping.DynamischerParameter;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DynamicParamsFetcherService {

    private final HashMap<String, DynamicParamFetcherStrategy> dynamicParamFetcherStrategies =
        new HashMap<>();

    /**
     * Konstruktor, welcher die Strategien zur Suche von dynamischen parametern hält
     */
    public DynamicParamsFetcherService() {
        dynamicParamFetcherStrategies.put(
            LeistungIdByCode.LEISTUNG_ID_CODE,
            new LeistungIdByCode()
        );
        dynamicParamFetcherStrategies.put(GebietIdByCode.GEBIET_ID_CODE, new GebietIdByCode());
        dynamicParamFetcherStrategies.put(OeIdByCode.OE_ID_CODE, new OeIdByCode());
        dynamicParamFetcherStrategies.put(LeikaByCode.LEIKA_ID_CODE, new LeikaByCode());
    }

    /**
     * Durchsucht alle Onlinedienste der Verwaltungsleistung und ersetzt sämtliche Onlinedienst URLs
     * durch Onlinedienst URLs mit dynamischen Query Parametern, sofern gefunden
     *
     * @param verwaltungsleistung Die Verwaltungsleistung
     * @param ars                 Der ARS, nachdem an der Schnittstelle gesucht wird. Wenn null, dann wird der erste Treffer aus
     *                            der ARS Liste in der Verwaltungsleistung genommen (PV-10317)
     */
    public void applyDynamicParamsToOnlinedienste(
        Verwaltungsleistung verwaltungsleistung,
        List<OnlinedienstVL> ods,
        String ars
    ) {
        DynamicParamContext dynamicParamContext = getDynamicParamContext(verwaltungsleistung, ars);

        /*
        Die Verwaltungsleistung ist stark verschachtelt und die notwendigen Informationen befinden sich
        sehr tief in den Daten (der Code), daher wird ein Kontext gebildet um jederzeit den Zustand
        beim Finden eines Codes wiederzugeben. Es wird also der aktuelle Stand der Daten in dem Kontext
        festgehalten und wenn ein Code gefunden wurde, verfügbar gemacht.
         */
        ods.forEach(od -> applyParams(dynamicParamContext, od));
    }

    private DynamicParamContext getDynamicParamContext(
        Verwaltungsleistung verwaltungsleistung,
        String ars
    ) {
        DynamicParamContext.DynamicParamContextBuilder builder = DynamicParamContext.builder();
        if (null != ars && !ars.isEmpty()) {
            builder.ars(ars);
        }

        if (null != verwaltungsleistung.getLeistungsbeschreibung()) {
            builder.lbId(verwaltungsleistung.getLeistungsbeschreibung().getId());
            if (!verwaltungsleistung.getLeistungsbeschreibung().getLeikakeys().isEmpty()) {
                String leikaId = verwaltungsleistung
                    .getLeistungsbeschreibung()
                    .getLeikakeys()
                    .iterator()
                    .next();
                builder.leikaId(leikaId);
            }
        }

        // Annahme für den Prototypen: die erste OE, da die Liste der OE parallel zu den OD besteht.
        if (!verwaltungsleistung.getOrganisationseinheiten().isEmpty()) {
            builder.oeId(verwaltungsleistung.getOrganisationseinheiten().getFirst().getId());
        }

        return builder.build();
    }

    private void applyParams(DynamicParamContext dynamicParamContext, OnlinedienstVL od) {
        List<LinkDynamic> updatedLinks = new ArrayList<>();

        for (LinkDynamic additionalLink : od.getLinks()) {
            BasicDynamicUrlBuilder basicDynamicUrlBuilder = new BasicDynamicUrlBuilder(
                additionalLink.uri()
            );
            for (DynamischerParameter dynamischerParameter : additionalLink.dynamicParameters()) {
                String code = dynamischerParameter.code();
                String parameterName = dynamischerParameter.parameterName();
                if (!dynamicParamFetcherStrategies.containsKey(code)) {
                    // Keine implementierte Strategie vorhanden!
                    continue;
                }

                DynamicParamFetcherStrategy strategy = dynamicParamFetcherStrategies.get(code);
                DynamicParamPair paramPair = strategy.fetch(dynamicParamContext, parameterName);

                if (null != paramPair) {
                    // Parameter-Paare sind entweder vollständig, oder Null
                    basicDynamicUrlBuilder.addDynamicParamPair(paramPair);
                }
            }

            // Erstelle ein neues HyperLinkDynamic Record mit der aktualisierten URI
            String updatedUri = basicDynamicUrlBuilder.build();
            LinkDynamic updatedLink = LinkDynamic.of(
                updatedUri,
                additionalLink.title(),
                additionalLink.description(),
                additionalLink.dynamicParameters()
            );
            updatedLinks.add(updatedLink);
        }

        // Ersetze die komplette Links-Liste mit der aktualisierten Version
        od.setLinks(updatedLinks);
    }
}
