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
package de.dataport.suchdienst.indexer.model.verwaltungsleistung;

import de.dataport.suchdienst.indexer.model.mapping.AdditionalInformation;
import de.dataport.suchdienst.indexer.model.mapping.BankAccount;
import de.dataport.suchdienst.indexer.model.mapping.Communication;
import de.dataport.suchdienst.indexer.model.mapping.Contact;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.mapping.Location;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class OrganisationseinheitContent {

    private String name;
    private String description;
    private String shortDescription;

    // Enthält die Links aus Organisationseinheit.internetAdresse. Der Name "forms" ist irritierend. Sollte umbenannt werden => Indexmigration
    private List<Link> forms = new ArrayList<>();

    private List<Contact> employees = new ArrayList<>();
    private List<AdditionalInformation> additionalInformation = new ArrayList<>();
    private List<BankAccount> bankAccounts = new ArrayList<>();
    private Location location;
    private Map<String, List<Communication>> communicationSystems = new HashMap<>();
    private Publisher publisher;
}
