package com.sandbox.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.slices;

class ArchitectureRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages("com.sandbox");

    @Test
    void domainDoesNotDependOnSpring() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "jakarta.validation..")
                .because("the domain must remain framework-free so it can be extracted into microservices")
                .check(CLASSES);
    }

    @Test
    void controllersDoNotAccessRepositories() {
        noClasses()
                .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().accessClassesThat().haveNameMatching(".*Repository")
                .because("controllers must go through application use cases, never repositories")
                .check(CLASSES);
    }

    @Test
    void onlyInfrastructureImplementsPorts() {
        noClasses()
                .that().areNotInterfaces()
                .and().implement(com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage("..port.."))
                .should().resideOutsideOfPackage("..infrastructure..")
                .because("ports are owned by domain/application and only infrastructure may implement them")
                .check(CLASSES);
    }

    @Test
    void noCyclicDependenciesBetweenModules() {
        slices()
                .matching("com.sandbox.(*)..")
                .should().beFreeOfCycles()
                .as("module packages must form an acyclic graph")
                .check(CLASSES);
    }

    @Test
    void domainModulesDoNotDependOnEachOther() {
        noClasses()
                .that().resideInAPackage("..orders.domain..")
                .should().accessClassesThat().resideInAPackage("..payments.domain..")
                .because("bounded contexts communicate via shared-kernel and events, never directly")
                .check(CLASSES);
    }
}
