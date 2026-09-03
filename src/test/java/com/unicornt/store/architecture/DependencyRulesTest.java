package com.unicornt.store.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The dependency rule (PLAN.md section 2.2), enforced hard. These were frozen with a
 * {@code FreezingArchViolationStore} through P0-P4 while the refactor removed the
 * backlog; P5 dropped the store once every count reached zero, so they are now plain
 * {@code @ArchTest} rules with no baseline.
 */
@AnalyzeClasses(packages = "com.unicornt.store", importOptions = ImportOption.DoNotIncludeTests.class)
class DependencyRulesTest {

    @ArchTest
    static final ArchRule domain_is_free_of_spring
            = noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .as("no classes in domain should depend on org.springframework..");

    @ArchTest
    static final ArchRule domain_is_free_of_jpa
            = noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                    .as("no classes in domain should depend on jakarta.persistence..");

    @ArchTest
    static final ArchRule domain_is_free_of_bean_validation
            = noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
                    .as("no classes in domain should depend on jakarta.validation..");

    @ArchTest
    static final ArchRule domain_is_free_of_infrastructure
            = noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .as("no classes in domain should depend on ..infrastructure..");

    @ArchTest
    static final ArchRule domain_is_free_of_application
            = noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..application..")
                    .as("no classes in domain should depend on ..application..");

    @ArchTest
    static final ArchRule application_is_free_of_infrastructure
            = noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .as("no classes in application should depend on ..infrastructure..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_is_free_of_spring_data
            = noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.data..")
                    .as("no classes in application should depend on org.springframework.data..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule layers_respect_the_dependency_rule
            = layeredArchitecture().consideringOnlyDependenciesInLayers()
                    .layer("Domain").definedBy("..domain..")
                    .layer("Application").definedBy("..application..")
                    .layer("Web").definedBy("..infrastructure.web..")
                    .layer("Persistence").definedBy("..infrastructure.persistence..")
                    .whereLayer("Web").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Persistence").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Web", "Persistence")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Web", "Persistence")
                    .as("web -> application -> domain; persistence -> application; domain depends on no layer");

    @ArchTest
    static final ArchRule packages_are_free_of_cycles
            = slices().matching("com.unicornt.store.(*)..")
                    .should().beFreeOfCycles()
                    .as("no package cycles between the top-level slices");
}
