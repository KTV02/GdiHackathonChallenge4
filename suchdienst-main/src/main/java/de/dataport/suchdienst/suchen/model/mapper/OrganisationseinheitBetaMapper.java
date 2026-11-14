// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Mapping der Organisationseinheiten auf das Beta-Detailformat.
package de.dataport.suchdienst.suchen.model.mapper;

import de.dataport.suchdienst.indexer.model.mapping.Communication;
import de.dataport.suchdienst.indexer.model.mapping.Contact;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.mapping.Location;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.suchen.model.out.common.HyperLink;
import de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole;
import de.dataport.suchdienst.suchen.model.out.organisation.CommunicationDetail;
import de.dataport.suchdienst.suchen.model.out.organisation.ContactDetail;
import de.dataport.suchdienst.suchen.model.out.organisation.LocationDetail;
import de.dataport.suchdienst.suchen.model.out.organisation.OrganisationUnitDetail;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { FormularMapper.class, CommonMapper.class, ResponsibilityMapper.class })
public interface OrganisationseinheitBetaMapper {
    OrganisationseinheitBetaMapper INSTANCE = Mappers.getMapper(
        OrganisationseinheitBetaMapper.class
    );

    @Mapping(source = "oe.name", target = "name")
    @Mapping(source = "oe.forms", target = "internetAddresses")
    @Mapping(source = "mergedRole", target = "role")
    @Mapping(source = "communicationSystemsForArs", target = "communicationSystems")
    @Mapping(source = "oe.oeffentlichAnzeigen", target = "publicDisplayAllowed")
    OrganisationUnitDetail mapV6Beta1(
        OrganisationseinheitVL oe,
        ResponsibilityRole mergedRole,
        List<Communication> communicationSystemsForArs
    );

    @Mapping(source = "oeffentlichAnzeigen", target = "publicDisplayAllowed")
    CommunicationDetail mapCommunication(Communication communication);

    @Mapping(source = "oeffentlichAnzeigen", target = "publicDisplayAllowed")
    ContactDetail mapContact(Contact contact);

    @Mapping(target = "urls", ignore = true)
    @Mapping(target = "information", ignore = true)
    LocationDetail mapLocation(Location location);

    HyperLink mapLink(Link link);
}
