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
package de.dataport.suchdienst.indexer.model.mapper;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDate;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleLink;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE)
public interface BusinessObjectMapper {
    BusinessObjectMapper INSTANCE = Mappers.getMapper(BusinessObjectMapper.class);

    OrganisationseinheitVL shallowCopy(OrganisationseinheitVL source);
    OnlinedienstVL shallowCopy(OnlinedienstVL source);
    FormularVL shallowCopy(FormularVL source);
    SpezialisierungVL shallowCopy(SpezialisierungVL source);

    ServiceModule shallowCopy(ServiceModule source);
    ServiceModuleDetail shallowCopy(ServiceModuleDetail source);
    ServiceModuleDate shallowCopy(ServiceModuleDate source);
    ServiceModuleLink shallowCopy(ServiceModuleLink source);

    /**
     * Kopiert die Inhalte eines LeistungsbeschreibungContent-Objekts in ein bestehendes LeistungsbeschreibungContent-Objekt.
     * Dies ersetzt die BeanUtils.copyProperties-Funktionalität für die Mehrsprachigkeit.
     */
    void copyContent(
        LeistungsbeschreibungContent source,
        @MappingTarget LeistungsbeschreibungContent target
    );

    /**
     * Kopiert die Inhalte eines OrganisationseinheitContent-Objekts in ein bestehendes OrganisationseinheitContent-Objekt.
     * Dies ersetzt die BeanUtils.copyProperties-Funktionalität für die Mehrsprachigkeit.
     */
    void copyContent(
        OrganisationseinheitContent source,
        @MappingTarget OrganisationseinheitContent target
    );

    /**
     * Kopiert die Inhalte eines OnlinedienstContent-Objekts in ein bestehendes OnlinedienstContent-Objekt.
     * Dies ersetzt die BeanUtils.copyProperties-Funktionalität für die Mehrsprachigkeit.
     */
    void copyContent(OnlinedienstContent source, @MappingTarget OnlinedienstContent target);

    /**
     * Kopiert die Inhalte eines FormularContent-Objekts in ein bestehendes FormularContent-Objekt.
     * Dies ersetzt die BeanUtils.copyProperties-Funktionalität für die Mehrsprachigkeit.
     */
    void copyContent(FormularContent source, @MappingTarget FormularContent target);

    /**
     * Kopiert die Inhalte eines SpezialisierungContent-Objekts in ein bestehendes SpezialisierungContent-Objekt.
     * Dies ersetzt die BeanUtils.copyProperties-Funktionalität für die Mehrsprachigkeit.
     */
    void copyContent(SpezialisierungContent source, @MappingTarget SpezialisierungContent target);
}
