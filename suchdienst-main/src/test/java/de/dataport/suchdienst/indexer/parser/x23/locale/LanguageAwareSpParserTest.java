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
package de.dataport.suchdienst.indexer.parser.x23.locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceBlockSpecialization;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.parser.ParserSelector;
import de.xoev.xzufi.v2_3_1.CodeLeistungstextmodul;
import de.xoev.xzufi.v2_3_1.Identifikator;
import de.xoev.xzufi.v2_3_1.Kostenmodul;
import de.xoev.xzufi.v2_3_1.LeistungSpezialisierung;
import de.xoev.xzufi.v2_3_1.Leistungsmodul;
import de.xoev.xzufi.v2_3_1.LeistungsmodulSpezialisierung;
import de.xoev.xzufi.v2_3_1.StringLocalizedHTML;
import de.xoev.xzufi.v2_3_1.Textmodul;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LanguageAwareSpParserTest {

    @Test
    void testSorting() {
        ParserSelector parserSelectorMock = mock(ParserSelector.class);
        when(parserSelectorMock.acceptList(any(), any(), any()))
            .thenReturn(List.of(Map.entry("06", mock(ServiceModuleDetail.class))))
            .thenReturn(List.of(Map.entry("02", mock(ServiceModuleDetail.class))))
            .thenReturn(List.of(Map.entry("98", mock(ServiceModuleDetail.class))))
            .thenReturn(List.of(Map.entry("24", mock(ServiceModuleDetail.class))));

        LeistungSpezialisierung spezialisierung = new LeistungSpezialisierung();
        Identifikator id = new Identifikator();
        id.setValue("SP1");
        id.setSchemeAgencyID("schemeId");
        spezialisierung.setId(id);

        spezialisierung
            .getModulSpezialisiert()
            .add(wrapIntoModulSpezialisierung(new Kostenmodul()));
        spezialisierung
            .getModulSpezialisiert()
            .add(wrapIntoModulSpezialisierung(getTextmodul("06", "Inhalt 06")));
        spezialisierung
            .getModulSpezialisiert()
            .add(wrapIntoModulSpezialisierung(getTextmodul("02", "Inhalt 02")));
        spezialisierung
            .getModulSpezialisiert()
            .add(wrapIntoModulSpezialisierung(getTextmodul("24", "Inhalt 24")));

        LanguageAwareSpParser parser = new LanguageAwareSpParser(null, parserSelectorMock);

        Map<String, List<ServiceBlockSpecialization>> map = parser.parseSpecializedModules(
            spezialisierung,
            spezialisierung.getId().getValue(),
            Locale.GERMAN
        );

        assertThat(map, is(instanceOf(LinkedHashMap.class)));
        assertThat(map.keySet().stream().toList(), is(List.of("02", "24", "06", "98")));
    }

    private Textmodul getTextmodul(String code, String inhalt) {
        Textmodul textmodul = new Textmodul();
        CodeLeistungstextmodul leika = new CodeLeistungstextmodul();
        leika.setCode(code);
        textmodul.setLeikaTextmodul(leika);

        StringLocalizedHTML localizedHTML = new StringLocalizedHTML();
        localizedHTML.setValue(inhalt);
        localizedHTML.setLanguageCode("de");

        textmodul.getInhalt().add(localizedHTML);

        return textmodul;
    }

    private LeistungsmodulSpezialisierung wrapIntoModulSpezialisierung(Leistungsmodul modul) {
        LeistungsmodulSpezialisierung modulSpezialisierung = new LeistungsmodulSpezialisierung();
        modulSpezialisierung.setModul(modul);
        modulSpezialisierung.setErsetzung(true);
        return modulSpezialisierung;
    }
}
