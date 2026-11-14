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

import static com.tngtech.archunit.core.domain.properties.HasType.Predicates.rawType;
import static com.tngtech.archunit.lang.conditions.ArchConditions.haveRawReturnType;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption.Predefined;
import de.dataport.suchdienst.Application;
import de.dataport.suchdienst.archunit.util.ReturnTypeGenericsCondition;
import de.dataport.suchdienst.archunit.util.StandardJavaObjectPredicate;
import de.dataport.suchdienst.backoffice.controller.AdminController;
import de.dataport.suchdienst.importer.controller.PushImportController;
import de.dataport.suchdienst.indexer.controller.CodelistController;
import de.dataport.suchdienst.suchen.controller.VerwaltungsleistungController;
import de.dataport.suchdienst.util.ResultPage;
import de.dataport.suchdienst.util.ResultPageWithMeta;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sammlung von Arch-Unit Tests zur Einhaltung der Architekturvorgaben bezüglich der Separation von Datenmodellen.
 * Der Suchdienst soll an öffentlichen Schnittstellen stets eigene Modelle herausgeben.
 * Es darf keine Kopplung zwischen einem Objekt zur Persistenz in Elasticsearch und einer öffentlichen REST-API geben.
 * Eine Kopplung hätte zur Folge, dass technische Anpassungen nicht durchgeführt werden können, ohne die öffentliche API zu verändern.
 */
class ModelSeparationTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOptions(
            List.of(
                Predefined.DO_NOT_INCLUDE_JARS,
                Predefined.DO_NOT_INCLUDE_TESTS,
                Predefined.DO_NOT_INCLUDE_ARCHIVES
            )
        )
        .importPackagesOf(Application.class)
        .that(
            DescribedPredicate.not(
                // Ausschließen von Admin-Endpunkten, da wir hier keine harten Abhängigkeiten von Konsumenten haben.
                // Deshalb ist ein Entkoppeln der Datenmodelle nicht zwingend erforderlich.
                JavaClass.Predicates.belongToAnyOf(
                    AdminController.class,
                    VerwaltungsleistungController.class,
                    CodelistController.class,
                    PushImportController.class
                )
            )
        );

    /**
     * Das interne Datenmodell soll keine Abhängigkeiten zu XZuFi Klassen enthalten, damit eine kompatibilität zu verschiedenen XZUFi Standards sichergestellt ist.
     * Das JZuFi Paket ist davon zunächst ausgenommen. Stand jetzt kennen wir noch kein Konzept zur parallelen Datenhaltung verschiedener XZuFi Versionen.
     * Daher könnte in der Zukunft auch das JZuFi Paket auf diese Beziehungen verzichten, wenn geeignete Wrapper gefunden wurden.
     */
    @Test
    void xzufi_not_allowed_in_indexer_model() {
        noClasses()
            .that()
            .resideInAPackage("..indexer.model..")
            .and()
            .resideOutsideOfPackage("..indexer.model.mapper..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..xzufi..")
            .check(CLASSES);
    }

    /**
     * Ein Out-Model, erkennbar über den Namen, muss in einem entsprechenden Package liegen.
     */
    @Test
    void out_models_must_reside_in_a_out_package() {
        classes()
            .that()
            .haveNameMatching(".*[Oo]ut.*")
            .should()
            .resideInAPackage("..model.out..")
            .as("Out-Models should reside in a package '..out..'")
            .check(CLASSES);
    }

    /**
     * Eine Klasse aus dem Out-Model Package darf keine Felder aus dem Indexer enthalten.
     */
    @Test
    void out_models_do_not_have_indexer_fields() {
        fields()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage("..model.out..")
            .should()
            .notHaveRawType(JavaClass.Predicates.resideInAPackage("..indexer.."))
            .check(CLASSES);
    }

    /**
     * Eine Klasse aus dem Out-Model Package darf keine Importe aus dem PV_Schema enthalten.
     */
    @Test
    void out_models_do_not_have_xzufi_imports() {
        noClasses()
            .that()
            .resideInAPackage("..out..")
            .and()
            .resideOutsideOfPackage("..jzufi.model..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..xzufi..")
            .check(CLASSES);
    }

    /**
     * Eine Klasse aus dem Out-Model Package darf keine Importe aus dem Indexer enthalten.
     */
    @Test
    void out_models_do_not_have_indexer_imports() {
        noClasses()
            .that()
            .resideInAPackage("..out..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..indexer..")
            .check(CLASSES);
    }

    /**
     * Jeder Controller soll in einem entsprechenden Package liegen.
     */
    @Test
    void controllers_should_reside_in_a_controller_package() {
        classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .resideInAPackage("..controller..")
            .check(CLASSES);
    }

    /**
     * Alle Controller haben das Suffix "Controller".
     */
    @Test
    void controller_naming_convention() {
        classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .haveSimpleNameEndingWith("Controller")
            .check(CLASSES);
    }

    /**
     * Alle Controller geben ein Modell aus einem Out-Model Package zurück.
     * Es werden generische Datentypen mit geprüft.
     * Ausgenommen sind Jzufi Endpunkte. Diese sollen PV_Schema Modelle zurückgeben. Konsumenten verlassen sich auf den Kontrakt
     */
    @Test
    void controllers_should_only_return_out_model() {
        DescribedPredicate<JavaClass> outModelOrStandardJava =
            JavaClass.Predicates.resideInAPackage("..model.out..")
                .or(StandardJavaObjectPredicate.isStandardJavaObject())
                .or(
                    JavaClass.Predicates.belongToAnyOf(
                        Resource.class,
                        ByteArrayResource.class,
                        HttpStatus.class,
                        PagedModel.class
                    )
                );

        methods()
            .that()
            .areAnnotatedWith(rawType(GetMapping.class).or(rawType(PostMapping.class)))
            .and()
            .haveNameNotContaining("Jzufi")
            .and()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController.class)
            .should(haveRawReturnType(outModelOrStandardJava))
            .orShould(
                ReturnTypeGenericsCondition.haveGenericReturnType(
                    outModelOrStandardJava,
                    ResponseEntity.class,
                    ResultPage.class,
                    ResultPageWithMeta.class,
                    Page.class,
                    List.class,
                    Set.class,
                    Iterator.class,
                    Stream.class
                )
            )
            .check(CLASSES);
    }
}
