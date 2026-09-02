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
 * Group C: the target state for tactical DDD (PLAN.md section 3.3). Each rule is
 * enabled by the orchestrator when the slice it names has landed on
 * {@code final-delivery}:
 * <ul>
 *   <li>catalog  — P1</li>
 *   <li>cart     — P2a</li>
 *   <li>identity — P2b</li>
 *   <li>ordering — P3</li>
 * </ul>
 * Until then each carries {@link ArchIgnore} with a {@code FINAL-DELIVERY: pending slice}
 * reason (ArchUnit's own disable mechanism; JUnit's {@code @Disabled} is not honoured
 * by the {@code @ArchTest} engine) so a half-migrated tree does not fail the build.
 * To activate a rule, delete its {@code @ArchIgnore} line.
 */
@AnalyzeClasses(packages = "com.unicornt.store", importOptions = ImportOption.DoNotIncludeTests.class)
class TargetArchitectureRulesTest {

    @ArchTest
    @ArchIgnore(reason = "FINAL-DELIVERY: pending slice catalog (P1) — enable when every *Repository has moved to domain.repository")
    static final ArchRule repository_interfaces_live_in_domain =
            classes().that().areInterfaces().and().haveSimpleNameEndingWith("Repository")
                    .should().resideInAPackage("..domain.repository..")
                    .as("repository ports are interfaces under domain.repository");

    @ArchTest
    @ArchIgnore(reason = "FINAL-DELIVERY: pending slice catalog (P1) — enable when infrastructure.persistence.adapter exists")
    static final ArchRule persistence_adapters_implement_a_domain_port =
            classes().that().resideInAPackage("..infrastructure.persistence.adapter..")
                    .should().dependOnClassesThat().resideInAPackage("..domain.repository..")
                    .as("classes in persistence.adapter implement an interface from domain.repository")
                    .allowEmptyShould(true);

    @ArchTest
    @ArchIgnore(reason = "FINAL-DELIVERY: pending slice catalog (P1) — enable when use cases no longer reference JPA entities")
    static final ArchRule use_cases_do_not_depend_on_jpa_entities =
            classes().that().resideInAPackage("..application.usecase..")
                    .should().onlyDependOnClassesThat(
                            DescribedPredicate.not(
                                    JavaClass.Predicates.simpleNameEndingWith("JpaEntity")))
                    .as("classes in application.usecase do not depend on *JpaEntity types")
                    .allowEmptyShould(true);
}
