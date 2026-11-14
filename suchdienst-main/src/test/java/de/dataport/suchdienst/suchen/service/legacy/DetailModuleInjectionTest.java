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
package de.dataport.suchdienst.suchen.service.legacy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelBase;
import de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlock;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DetailModuleInjectionTest {

    static class TestSdOutModel extends SdOutModelBase {

        private Set<String> availableLanguages = new HashSet<>();
        private List<SeDeBlock> details = new ArrayList<>();

        @Override
        public Set<String> getAvailableLanguages() {
            return availableLanguages;
        }

        @Override
        public List<SeDeBlock> getDetails() {
            return details;
        }

        @Override
        public void setDetails(List<SeDeBlock> details) {
            this.details = details;
        }
    }

    static class TestSeDeBlock extends SeDeBlock {

        private boolean filled = true;
        private boolean placeholder = false;

        @Override
        public boolean isFilled() {
            return filled;
        }

        @Override
        public boolean isPlaceholder() {
            return placeholder;
        }

        public void setFilled(boolean filled) {
            this.filled = filled;
        }

        public void setPlaceholder(boolean placeholder) {
            this.placeholder = placeholder;
        }
    }

    static SeDeBlock createBlock(String code, String title, String text) {
        TestSeDeBlock block = new TestSeDeBlock();
        block.setCode(code);
        block.setTitle(title);
        block.setText(text);
        return block;
    }

    @Test
    void injectsAllMandatoryModulesIfNonePresent() {
        var model = new TestSdOutModel();
        model.getAvailableLanguages().add(Locale.GERMAN.toLanguageTag());
        model.setDetails(new ArrayList<>());

        DetailModuleInjection.injectServiceModules(model, Locale.GERMAN);

        var codes = model.getDetails().stream().map(SeDeBlock::getCode).toList();
        for (ServiceModuleCode code : DetailModuleInjection.MODULES_MANDATORY) {
            assertThat(
                "Should contain mandatory code: " + code.getCode(),
                codes,
                hasItem(code.getCode())
            );
        }
        assertThat(model.getDetails(), hasSize(DetailModuleInjection.MODULES_MANDATORY.size()));
        assertThat(model.getDetails(), hasItem(allOf(hasProperty("text", is("nicht angegeben")))));
    }

    @Test
    void doesNotInjectIfMandatoryModulePresent() {
        var model = new TestSdOutModel();
        model.getAvailableLanguages().add(Locale.GERMAN.toLanguageTag());
        SeDeBlock existing = createBlock(
            ServiceModuleCode.FRIST.getCode(),
            "Custom Title",
            "Custom Text"
        );
        model.setDetails(new ArrayList<>(List.of(existing)));

        DetailModuleInjection.injectServiceModules(model, Locale.GERMAN);

        var details = model.getDetails();
        assertThat(details, hasSize(DetailModuleInjection.MODULES_MANDATORY.size()));
        var modules = details
            .stream()
            .filter(b -> ServiceModuleCode.FRIST.getCode().equals(b.getCode()))
            .toList();
        assertThat(modules.size(), is(1));
        var frist = modules.stream().findFirst();
        assertThat(frist.isPresent(), is(true));
        assertThat(frist.get().getTitle(), equalTo("Custom Title"));
        assertThat(frist.get().getText(), equalTo("Custom Text"));
    }

    @Test
    void fallsBackToGermanIfLocaleNotAvailable() {
        var model = new TestSdOutModel();
        model.getAvailableLanguages().add(Locale.GERMAN.toLanguageTag());
        model.setDetails(new ArrayList<>());

        DetailModuleInjection.injectServiceModules(model, Locale.ENGLISH);

        Optional<SeDeBlock> fristen = model
            .getDetails()
            .stream()
            .filter(b -> ServiceModuleCode.FRIST.getCode().equals(b.getCode()))
            .findFirst();
        assertThat(fristen.isPresent(), is(true));
        assertThat(fristen.get().getTitle(), equalTo("Fristen"));
        assertThat(fristen.get().getText(), equalTo("nicht angegeben"));
    }

    @Test
    void setsTextToNotSpecifiedForInjectedModules() {
        var model = new TestSdOutModel();
        model.getAvailableLanguages().add(Locale.ENGLISH.toLanguageTag());
        model.setDetails(new ArrayList<>());

        DetailModuleInjection.injectServiceModules(model, Locale.ENGLISH);

        for (SeDeBlock block : model.getDetails()) {
            assertThat(block.getText(), anyOf(equalTo("not specified"), equalTo("not specified")));
        }
    }
}
