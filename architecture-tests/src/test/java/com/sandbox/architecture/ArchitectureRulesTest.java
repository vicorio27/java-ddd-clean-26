package com.sandbox.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureRulesTest {

    private static final JavaClasses CLASSES = importSandboxClasses();

    /**
     * La version anterior usaba importPackages("com.sandbox") + DO_NOT_INCLUDE_JARS,
     * que bajo Gradle importaba CERO clases: los modulos llegan como jars y las reglas
     * pasaban en vacio. Aqui se importan explicitamente las rutas del propio repo.
     */
    private static JavaClasses importSandboxClasses() {
        var rootDir = Path.of(System.getProperty("sandbox.rootDir")).toAbsolutePath().normalize();
        var locations = Arrays.stream(System.getProperty("sandbox.classpath").split(File.pathSeparator))
                .map(entry -> Path.of(entry).toAbsolutePath().normalize())
                .filter(path -> path.startsWith(rootDir))
                .filter(Files::exists)
                // Los modulos llegan empaquetados; un jar necesita URI jar:...!/ para ser recorrido.
                .map(path -> path.toString().endsWith(".jar")
                        ? Location.of(URI.create("jar:" + path.toUri() + "!/"))
                        : Location.of(path))
                .toList();

        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importLocations(locations);

        var sandboxClasses = classes.that(JavaClass.Predicates.resideInAPackage("com.sandbox.."));
        if (!sandboxClasses.iterator().hasNext()) {
            throw new IllegalStateException(
                    "No se importo ninguna clase de com.sandbox desde " + locations
                            + ". Las reglas de arquitectura no estarian comprobando nada.");
        }
        return sandboxClasses;
    }

    private static final List<String> BOUNDED_CONTEXTS =
            List.of("orders", "payments", "inventory", "customers", "notifications");

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
    void applicationLayerDoesNotDependOnSpring() {
        noClasses()
                .that().resideInAPackage("com.sandbox.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "jakarta.validation..")
                .because("use cases orchestrate domain logic; transactions and IO reach them through ports "
                        + "(see UnitOfWork), never through framework annotations")
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
                .and().implement(JavaClass.Predicates.resideInAPackage("..port.."))
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

    /**
     * La version anterior de esta regla solo comprobaba orders -> payments, dejando
     * 19 de los 20 pares posibles sin proteger. Ahora se generan todos.
     */
    @TestFactory
    Stream<DynamicTest> domainModulesDoNotDependOnEachOther() {
        var tests = new ArrayList<DynamicTest>();
        for (var source : BOUNDED_CONTEXTS) {
            for (var target : BOUNDED_CONTEXTS) {
                if (source.equals(target)) {
                    continue;
                }
                tests.add(DynamicTest.dynamicTest(source + " -/-> " + target, () ->
                        noClasses()
                                .that().resideInAPackage("..%s.domain..".formatted(source))
                                .should().dependOnClassesThat().resideInAPackage("..%s.domain..".formatted(target))
                                .because("bounded contexts communicate via shared-kernel and events, never directly")
                                .check(CLASSES)));
            }
        }
        return tests.stream();
    }
}
