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
package de.dataport.suchdienst.indexer.service.languagecheck;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import de.dataport.suchdienst.indexer.model.mapping.AdditionalInformation;
import de.dataport.suchdienst.indexer.model.mapping.AdditionalInformationType;
import de.dataport.suchdienst.indexer.model.mapping.BankAccount;
import de.dataport.suchdienst.indexer.model.mapping.Contact;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LanguageCompleteOrganisationUnitTest {

    private LanguageOeCompleteCheckService completeCheckService;

    private OrganisationseinheitContent content;
    private OrganisationseinheitVL oe;
    private Map<Locale, OrganisationseinheitContent> map;

    @BeforeEach
    void init() {
        // Initialisiere Service mit seiner Dependency
        LanguageFoCompleteCheckService foService = new LanguageFoCompleteCheckService();
        completeCheckService = new LanguageOeCompleteCheckService(foService);

        content = new OrganisationseinheitContent();
        oe = new OrganisationseinheitVL();

        map = new HashMap<>();
        map.put(Locale.ENGLISH, content);
    }

    @Test
    void testMainValuesComplete() {
        oe.setName("title");
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        content.setName("englishTitle");
        assertTrue(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        oe.setDescription("beschreibung");
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        content.setDescription("englishDescription");
        assertTrue(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        oe.setShortDescription("kurzbeschreibung");
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        content.setShortDescription("englishShortDescription");
        assertTrue(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));
    }

    @Test
    void testFormsComplete() {
        Link germanLink = new Link("uri", "title", "description");
        oe.setForms(Lists.newArrayList(germanLink));
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        content.setForms(Lists.newArrayList(Link.of(null, null, null)));
        assertTrue(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));
    }

    @Test
    void testAdditionalInformationComplete() {
        AdditionalInformation info1 = new AdditionalInformation(
            "title",
            "text",
            AdditionalInformationType.INFO_OTHER
        );
        AdditionalInformation info2 = new AdditionalInformation(
            "title",
            "text",
            AdditionalInformationType.INFO_TIMES
        );
        oe.getAdditionalInformation().add(info1);
        oe.getAdditionalInformation().add(info2);
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        AdditionalInformation englishInfo1 = new AdditionalInformation(
            "title",
            "text",
            AdditionalInformationType.INFO_OTHER
        );
        AdditionalInformation englishInfo2 = new AdditionalInformation(
            "title",
            "text",
            AdditionalInformationType.INFO_TIMES
        );
        content.getAdditionalInformation().add(englishInfo2);
        content.getAdditionalInformation().add(englishInfo1);
        assertTrue(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        content.getAdditionalInformation().clear();
        content.getAdditionalInformation().add(englishInfo1);
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        AdditionalInformation englishInfo3 = new AdditionalInformation(
            "title",
            null,
            AdditionalInformationType.INFO_TIMES
        );
        content.getAdditionalInformation().add(englishInfo3);
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));
    }

    @Test
    void testBankAccountsComplete() {
        BankAccount germanBankAccount1 = new BankAccount(
            "receiver",
            "iban",
            "bankname",
            "beschreibung",
            "bic"
        );
        BankAccount germanBankAccount2 = new BankAccount(
            "receiver2",
            "iban2",
            "bankname2",
            "beschreibung2",
            "bic2"
        );
        oe.setBankAccounts(List.of(germanBankAccount1, germanBankAccount2));
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        BankAccount englishBankAccount1 = new BankAccount(
            "receiver",
            "iban",
            "bankname",
            "description",
            "bic"
        );
        BankAccount englishBankAccount2 = new BankAccount(
            "receiver2",
            "iban2",
            "bankname2",
            "description2",
            "bic2"
        );
        content.setBankAccounts(List.of(englishBankAccount2, englishBankAccount1));
        assertTrue(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        BankAccount englishBankAccount3 = new BankAccount(
            "receiver3",
            "iban2",
            "bankname2",
            "description2",
            "bic2"
        );
        content.setBankAccounts(List.of(englishBankAccount3, englishBankAccount1));
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        BankAccount englishBankAccount4 = new BankAccount(
            "receiver2",
            "iban2",
            "bankname2",
            null,
            "bic2"
        );
        content.setBankAccounts(List.of(englishBankAccount4, englishBankAccount1));
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));
    }

    @Test
    void testEmployeesComplete() {
        Contact germanContact1 = new Contact();
        germanContact1.setLastname("lastname");
        germanContact1.setName("name");
        germanContact1.setSalutation("salutaion");
        germanContact1.setTitle("title");
        oe.setEmployees(List.of(germanContact1));
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        Contact englishContact1 = new Contact();
        content.setEmployees(List.of(englishContact1));
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        englishContact1.setLastname("lastname");
        englishContact1.setName("name");
        englishContact1.setSalutation("salutaion");
        englishContact1.setTitle("title");
        assertTrue(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        englishContact1.setLastname("lastnamex");
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        englishContact1.setLastname("lastname");
        englishContact1.setTitle(null);
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));

        englishContact1.setTitle("englishtitle");
        englishContact1.setSalutation(null);
        assertFalse(completeCheckService.checkLanguageComplete(map, oe, Locale.ENGLISH));
    }
}
