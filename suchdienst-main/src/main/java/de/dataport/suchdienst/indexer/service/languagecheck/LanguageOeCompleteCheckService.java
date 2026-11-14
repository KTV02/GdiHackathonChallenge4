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

import de.dataport.suchdienst.indexer.model.mapping.AdditionalInformation;
import de.dataport.suchdienst.indexer.model.mapping.BankAccount;
import de.dataport.suchdienst.indexer.model.mapping.Contact;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

@Service
public class LanguageOeCompleteCheckService implements LanguageCompleteCheck {

    private final LanguageFoCompleteCheckService languageFoCompleteCheckService;

    public LanguageOeCompleteCheckService(
        LanguageFoCompleteCheckService languageFoCompleteCheckService
    ) {
        this.languageFoCompleteCheckService = languageFoCompleteCheckService;
    }

    public boolean checkLanguageComplete(
        Map<Locale, OrganisationseinheitContent> languageContent,
        OrganisationseinheitVL oe,
        Locale language
    ) {
        OrganisationseinheitContent content = languageContent.get(language);

        if (content == null) {
            return false;
        }

        return (
            stringFieldComplete(oe.getName(), content.getName()) &&
            stringFieldComplete(oe.getDescription(), content.getDescription()) &&
            stringFieldComplete(oe.getShortDescription(), content.getShortDescription()) &&
            isBankAccountsComplete(oe, content) &&
            isAdditionalInformationComplete(oe, content) &&
            isEmployeesComplete(oe, content) &&
            isNestedFormsComplete(oe, language) &&
            oe.getForms().size() <= content.getForms().size()
        );
    }

    private boolean isEmployeesComplete(
        OrganisationseinheitVL oe,
        OrganisationseinheitContent content
    ) {
        return oe
            .getEmployees()
            .stream()
            .allMatch(employee -> {
                Optional<Contact> contentContact = content
                    .getEmployees()
                    .stream()
                    .filter(
                        contentEmployee ->
                            Strings.CS.equals(
                                employee.getLastname(),
                                contentEmployee.getLastname()
                            ) &&
                            Strings.CS.equals(employee.getName(), contentEmployee.getName()) &&
                            Strings.CS.equals(employee.getRoom(), contentEmployee.getRoom())
                    )
                    .findFirst();
                return (
                    contentContact.isPresent() &&
                    stringFieldComplete(
                        employee.getSalutation(),
                        contentContact.get().getSalutation()
                    ) &&
                    stringFieldComplete(employee.getTitle(), contentContact.get().getTitle())
                );
            });
    }

    private boolean isAdditionalInformationComplete(
        OrganisationseinheitVL oe,
        OrganisationseinheitContent content
    ) {
        return oe
            .getAdditionalInformation()
            .stream()
            .allMatch(info -> {
                Optional<AdditionalInformation> contentInfo = content
                    .getAdditionalInformation()
                    .stream()
                    .filter(infoContent -> info.getType() == infoContent.getType())
                    .findFirst();
                return (
                    contentInfo.isPresent() &&
                    stringFieldComplete(info.getText(), contentInfo.get().getText()) &&
                    info.getAdditionalInformation().size() <=
                        contentInfo.get().getAdditionalInformation().size()
                );
            });
    }

    private boolean isBankAccountsComplete(
        OrganisationseinheitVL oe,
        OrganisationseinheitContent content
    ) {
        return oe
            .getBankAccounts()
            .stream()
            .allMatch(bankAccount -> {
                Optional<BankAccount> contentBankAccount = content
                    .getBankAccounts()
                    .stream()
                    .filter(
                        contentAccount ->
                            Strings.CS.equals(
                                contentAccount.getBankName(),
                                bankAccount.getBankName()
                            ) &&
                            Strings.CS.equals(
                                contentAccount.getReceiver(),
                                bankAccount.getReceiver()
                            ) &&
                            Strings.CS.equals(contentAccount.getIban(), bankAccount.getIban())
                    )
                    .findFirst();

                return (
                    contentBankAccount.isPresent() &&
                    stringFieldComplete(
                        bankAccount.getDescription(),
                        contentBankAccount.get().getDescription()
                    )
                );
            });
    }

    private boolean isNestedFormsComplete(OrganisationseinheitVL oe, Locale language) {
        return oe
            .getNestedForms()
            .stream()
            .allMatch(fo ->
                languageFoCompleteCheckService.checkLanguageComplete(
                    fo.getOtherLanguages(),
                    fo,
                    language
                )
            );
    }
}
