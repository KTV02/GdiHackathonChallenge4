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
package de.dataport.suchdienst.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption.Predefined;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import de.dataport.suchdienst.Application;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stellt sicher, dass alle Handler-Methoden aller {@link RestController}-Klassen
 * entweder selbst oder über ihre Controller-Klasse mit {@link ApiAudienceMarker}
 * annotiert sind.
 *
 * <p>Damit wird dieselbe Invariante wie in {@code ApiAudienceStartupValidation}
 * bereits zur Build-Zeit geprüft, ohne Produktionscode zu benötigen.
 */
class ApiAudienceAnnotationTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOptions(
            List.of(
                Predefined.DO_NOT_INCLUDE_JARS,
                Predefined.DO_NOT_INCLUDE_TESTS,
                Predefined.DO_NOT_INCLUDE_ARCHIVES
            )
        )
        .importPackagesOf(Application.class);

    /**
     * Jede Handler-Methode eines {@link RestController}s muss mit {@link ApiAudienceMarker}
     * annotiert sein – entweder direkt an der Methode oder an der Controller-Klasse.
     */
    @Test
    void all_controller_endpoints_must_have_api_audience_marker() {
        methods()
            .that()
            .areMetaAnnotatedWith(RequestMapping.class)
            .and()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController.class)
            .should(haveApiAudienceMarkerAtMethodOrClassLevel())
            .check(CLASSES);
    }

    private static ArchCondition<JavaMethod> haveApiAudienceMarkerAtMethodOrClassLevel() {
        return new ArchCondition<>(
            "mit @ApiAudienceMarker an der Methode oder der Controller-Klasse annotiert sein"
        ) {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean annotatedAtMethod = method.isAnnotatedWith(ApiAudienceMarker.class);
                boolean annotatedAtClass = method
                    .getOwner()
                    .isAnnotatedWith(ApiAudienceMarker.class);
                if (!annotatedAtMethod && !annotatedAtClass) {
                    String message = String.format(
                        "Fehlende @ApiAudienceMarker-Annotation: %s#%s – weder Methode noch Controller-Klasse %s sind annotiert",
                        method.getOwner().getName(),
                        method.getName(),
                        method.getOwner().getSimpleName()
                    );
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        };
    }
}
