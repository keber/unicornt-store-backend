package com.unicornt.store.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Group C: the target state for tactical DDD (PLAN.md section 3.3). A rule is
 * enabled (its {@code @ArchIgnore} removed) when the slice it names lands on
 * {@code final-delivery}. As of P1 the <strong>catalog</strong>-scoped rules below
 * are active. The whole-codebase versions stay {@code @ArchIgnore} until P5, when
 * every slice has migrated and they become hard rules.
 */
@AnalyzeClasses(packages = "com.unicornt.store", importOptions = ImportOption.DoNotIncludeTests.class)
class TargetArchitectureRulesTest {

    private static final DescribedPredicate<JavaClass> A_JPA_ENTITY =
            DescribedPredicate.describe("a *JpaEntity", type -> type.getSimpleName().endsWith("JpaEntity"));

    // ---- catalog scope (active since P1) --------------------------------------

    @ArchTest
    static final ArchRule catalog_repository_ports_are_interfaces_in_domain =
            classes().that().haveSimpleNameEndingWith("Repository")
                    .and().haveSimpleNameNotStartingWith("SpringData")
                    .and().resideOutsideOfPackage("..infrastructure..")
                    .should().beInterfaces()
                    .andShould().resideInAPackage("..domain.repository..")
                    .as("catalog repository ports are interfaces under domain.repository");

    @ArchTest
    static final ArchRule persistence_adapters_implement_a_domain_port =
            classes().that().resideInAPackage("..infrastructure.persistence.adapter..")
                    .should().dependOnClassesThat().resideInAPackage("..domain.repository..")
                    .as("classes in persistence.adapter implement an interface from domain.repository")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule use_cases_do_not_depend_on_jpa_entities =
            classes().that().resideInAPackage("..application.usecase..")
                    .should().onlyDependOnClassesThat(DescribedPredicate.not(A_JPA_ENTITY))
                    .as("classes in application.usecase do not depend on *JpaEntity types")
                    .allowEmptyShould(true);

    // ---- whole codebase (enabled at P5) -------------------------------------

    @ArchTest
    @ArchIgnore(reason = "FINAL-DELIVERY: pending P5 — every Spring Data repository still lives in infrastructure")
    static final ArchRule every_repository_interface_lives_in_domain =
            classes().that().areInterfaces().and().haveSimpleNameEndingWith("Repository")
                    .and().haveSimpleNameNotStartingWith("SpringData")
                    .should().resideInAPackage("..domain.repository..")
                    .as("every repository port is an interface under domain.repository");
}
