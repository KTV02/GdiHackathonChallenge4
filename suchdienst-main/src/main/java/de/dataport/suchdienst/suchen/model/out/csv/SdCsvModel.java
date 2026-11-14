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
package de.dataport.suchdienst.suchen.model.out.csv;

import com.opencsv.bean.CsvBindAndSplitByPosition;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class SdCsvModel {

    @CsvBindByPosition(position = 0)
    private String id;

    @CsvBindByPosition(position = 2)
    private String title;

    @CsvBindByPosition(position = 3)
    private String title2;

    @CsvBindAndSplitByPosition(position = 1, elementType = String.class, writeDelimiter = ";")
    private Set<String> leikaIds = new HashSet<>();

    @CsvBindByPosition(position = 4)
    @CsvDate("dd.MM.yyyy HH:mm")
    private Instant timestamp;

    @CsvBindByPosition(position = 5)
    private String odId;

    @CsvBindByPosition(position = 6)
    private String odName;

    @CsvBindAndSplitByPosition(position = 7, elementType = String.class, writeDelimiter = ";")
    private List<String> urls = new ArrayList<>();
}
