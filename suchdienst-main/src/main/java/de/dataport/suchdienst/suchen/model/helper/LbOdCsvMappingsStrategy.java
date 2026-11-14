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
package de.dataport.suchdienst.suchen.model.helper;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import de.dataport.suchdienst.suchen.model.out.csv.SdCsvModel;

public class LbOdCsvMappingsStrategy extends ColumnPositionMappingStrategy<SdCsvModel> {

    private static final String[] HEADERS = new String[] {
        "ID-LB",
        "Leikaschlüssel",
        "Name",
        "Name2",
        "Updatedatum",
        "ID-OD",
        "Bezeichnung OD",
        "Links",
    };

    public LbOdCsvMappingsStrategy() {
        setType(SdCsvModel.class);
    }

    @Override
    public String[] generateHeader(SdCsvModel bean) throws CsvRequiredFieldEmptyException {
        super.generateHeader(bean);
        return HEADERS;
    }
}
