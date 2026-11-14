// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Tests des Beta-Mappings mit Sichtbarkeitskennzeichen.
package de.dataport.suchdienst.suchen.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.indexer.model.mapping.Communication;
import de.dataport.suchdienst.indexer.model.mapping.Contact;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.mapping.Location;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class OrganisationseinheitBetaMapperTest {

    @ParameterizedTest
    @CsvSource({ "true, false, true", "false, true, false" })
    void mapsPublicDisplayAllowedAtEveryLevel(
        boolean organisationAllowed,
        boolean contactAllowed,
        boolean communicationAllowed
    ) {
        OrganisationseinheitVL oe = organisation();
        oe.setOeffentlichAnzeigen(organisationAllowed);
        oe.getEmployees().getFirst().setOeffentlichAnzeigen(contactAllowed);
        Communication communication = oe.getLocation().getCommunications().getFirst();
        communication.setOeffentlichAnzeigen(communicationAllowed);

        var detail = OrganisationseinheitBetaMapper.INSTANCE.mapV6Beta1(
            oe,
            null,
            List.of(communication)
        );

        assertEquals(organisationAllowed, detail.getPublicDisplayAllowed());
        assertEquals(
            contactAllowed,
            detail.getEmployees().iterator().next().getPublicDisplayAllowed()
        );
        assertEquals(
            communicationAllowed,
            detail.getCommunicationSystems().iterator().next().getPublicDisplayAllowed()
        );
        assertEquals(
            communicationAllowed,
            detail.getLocation().getCommunications().getFirst().getPublicDisplayAllowed()
        );
        assertEquals(
            communicationAllowed,
            detail
                .getEmployees()
                .iterator()
                .next()
                .getLocation()
                .getCommunications()
                .getFirst()
                .getPublicDisplayAllowed()
        );
    }

    @Test
    void defaultsToPublicForDataWithoutFlags() {
        OrganisationseinheitVL oe = JsonMapper.builder()
            .build()
            .readValue(
                """
                {
                    "id": "oe-1",
                    "location": {"communications": [{"name": "E-Mail"}]},
                    "employees": [{
                        "name": "Erika",
                        "location": {"communications": [{"name": "Telefon"}]}
                    }]
                }
                """,
                OrganisationseinheitVL.class
            );
        var detail = OrganisationseinheitBetaMapper.INSTANCE.mapV6Beta1(
            oe,
            null,
            oe.getLocation().getCommunications()
        );

        assertTrue(detail.getPublicDisplayAllowed());
        assertTrue(detail.getEmployees().iterator().next().getPublicDisplayAllowed());
        assertTrue(detail.getCommunicationSystems().iterator().next().getPublicDisplayAllowed());
        assertTrue(detail.getLocation().getCommunications().getFirst().getPublicDisplayAllowed());
        assertTrue(
            detail
                .getEmployees()
                .iterator()
                .next()
                .getLocation()
                .getCommunications()
                .getFirst()
                .getPublicDisplayAllowed()
        );
    }

    @Test
    void preservesSharedFieldsWhileUsingCleanBetaModels() {
        OrganisationseinheitVL oe = organisation();
        var role = new ResponsibilityRole("Ansprechpunkt", "03", "uri", "1");
        List<Communication> communications = oe.getLocation().getCommunications();
        JsonMapper jsonMapper = JsonMapper.builder().build();
        JsonNode stable = jsonMapper.valueToTree(
            OrganisationseinheitMapper.INSTANCE.mapV5(oe, role, communications)
        );
        JsonNode beta = jsonMapper.valueToTree(
            OrganisationseinheitBetaMapper.INSTANCE.mapV6Beta1(oe, role, communications)
        );

        ObjectNode stableContact = (ObjectNode) stable.get("employees").get(0);
        stableContact.remove("relations");
        ObjectNode stableLink = (ObjectNode) stableContact.get("additionalLinks").get(0);
        stableLink.remove("replace");
        stableLink.remove("dynamicParameters");
        removePublicDisplayAllowed(beta);

        assertEquals(stable, beta);
    }

    private void removePublicDisplayAllowed(JsonNode node) {
        if (node instanceof ObjectNode object) {
            object.remove("isPublicDisplayAllowed");
        }
        node.forEach(this::removePublicDisplayAllowed);
    }

    private OrganisationseinheitVL organisation() {
        Communication communication = new Communication();
        communication.setCode("01");
        communication.setName("E-Mail");
        communication.setValue("kontakt@example.org");
        communication.setValueAdditional("Zusatz");
        communication.setAdditional("Hinweis");
        Location location = new Location();
        location.setCommunications(List.of(communication));
        Contact contact = new Contact();
        contact.setName("Erika");
        contact.setLastname("Mustermann");
        contact.setLocation(location);
        contact.setAdditionalLinks(
            List.of(Link.of("https://example.org/kontakt", "Kontakt", "Text"))
        );
        OrganisationseinheitVL oe = new OrganisationseinheitVL();
        oe.setId("oe-1");
        oe.setName("Musteramt");
        oe.setDescription("Beschreibung");
        oe.setShortDescription("Kurzbeschreibung");
        oe.setForms(List.of(Link.of("https://example.org", "Internet", "Text")));
        oe.setEmployees(List.of(contact));
        oe.setLocation(location);
        return oe;
    }
}
