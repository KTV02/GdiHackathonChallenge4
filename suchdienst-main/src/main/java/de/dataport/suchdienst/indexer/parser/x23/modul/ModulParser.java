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
package de.dataport.suchdienst.indexer.parser.x23.modul;

import de.dataport.suchdienst.indexer.model.AccessChannel;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.util.TypeUtil;
import de.xoev.xzufi.v2_3_1.Leistungsmodul;
import java.util.Optional;

public abstract class ModulParser {

    protected final CodelistService codelistService;

    protected ModulParser(CodelistService codelistService) {
        this.codelistService = codelistService;
    }

    protected AccessChannel getAccessChannel(Leistungsmodul modul) {
        return TypeUtil.accessChannelFromCode(
            parseZugangskanal(modul).map(CodelistEntry::getCode).orElse(null)
        );
    }

    protected Optional<CodelistEntry> parseZugangskanal(Leistungsmodul modul) {
        return Optional.ofNullable(modul)
            .flatMap(m -> Optional.ofNullable(m.getZugangskanal()))
            .map(zugangskanal ->
                codelistService.getCodelistEntry(
                    zugangskanal.getListURI(),
                    zugangskanal.getListVersionID(),
                    zugangskanal.getCode()
                )
            );
    }
}
