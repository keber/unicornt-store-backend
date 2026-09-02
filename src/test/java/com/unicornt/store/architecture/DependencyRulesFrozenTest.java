package com.unicornt.store.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Group B: the refactor backlog. Every rule below is the target dependency rule
 * (PLAN.md section 2.2); today they are violated. Each is wrapped in
 * {@link FreezingArchRule} so the current violations are recorded once in
 * src/test/resources/archunit_store and the build stays green, but a NEW violation
 * fails it. Each slice removes violations; the orchestrator regenerates the store.
 * P5 converts these to hard rules and deletes the store once the count is zero.
 */
@AnalyzeClasses(packages = "com.unicornt.store", importOptions = ImportOption.DoNotIncludeTests.class)
class DependencyRulesFrozenTest {

    @ArchTest
    static final ArchRule domain_is_free_of_spring = FreezingArchRule.freeze(
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .as("no classes in domain should depend on org.springframework.."));

    @ArchTest
    static final ArchRule domain_is_free_of_jpa = FreezingArchRule.freeze(
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                    .as("no classes in domain should depend on jakarta.persistence.."));

    @ArchTest
    static final ArchRule domain_is_free_of_bean_validation = FreezingArchRule.freeze(
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
                    .as("no classes in domain should depend on jakarta.validation.."));

    @ArchTest
    static final ArchRule domain_is_free_of_infrastructure = FreezingArchRule.freeze(
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .as("no classes in domain should depend on ..infrastructure.."));

    @ArchTest
    static final ArchRule domain_is_free_of_application = FreezingArchRule.freeze(
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..application..")
                    .as("no classes in domain should depend on ..application.."));

    @ArchTest
    static final ArchRule application_is_free_of_infrastructure = FreezingArchRule.freeze(
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .as("no classes in application should depend on ..infrastructure..")
                    .allowEmptyShould(true));

    @ArchTest
    static final ArchRule application_is_free_of_spring_data = FreezingArchRule.freeze(
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.data..")
                    .as("no classes in application should depend on org.springframework.data..")
                    .allowEmptyShould(true));

    @ArchTest
    static final ArchRule layers_respect_the_dependency_rule = FreezingArchRule.freeze(
            layeredArchitecture().consideringOnlyDependenciesInLayers()
                    .layer("Domain").definedBy("..domain..")
                    .layer("Application").definedBy("..application..")
                    .layer("Web").definedBy("..infrastructure.web..")
                    .layer("Persistence").definedBy("..infrastructure.persistence..")
                    .whereLayer("Web").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Persistence").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Web", "Persistence")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Web", "Persistence")
                    .as("web -> application -> domain; persistence -> application; domain depends on no layer"));

    @ArchTest
    static final ArchRule packages_are_free_of_cycles = FreezingArchRule.freeze(
            slices().matching("com.unicornt.store.(*)..")
                    .should().beFreeOfCycles()
                    .as("no package cycles between the top-level slices"));
}
