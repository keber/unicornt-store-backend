package com.unicornt.store.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Group A: rules that already hold and protect what works. They are hard from P0 on.
 * A regression here fails the build immediately, with no freezing safety net.
 */
@AnalyzeClasses(packages = "com.unicornt.store", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureRulesTest {

    @ArchTest
    static final ArchRule rest_controllers_live_only_in_web_rest =
            classes().that().haveSimpleNameEndingWith("RestController")
                    .should().resideInAPackage("..infrastructure.web.rest..")
                    .andShould().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .as("*RestController classes reside in infrastructure.web.rest and are @RestController");

    @ArchTest
    static final ArchRule domain_does_not_depend_on_web =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.web..")
                    .as("the domain layer does not depend on the web layer");

    @ArchTest
    static final ArchRule web_does_not_reach_spring_data_repositories =
            noClasses().that().resideInAPackage("..infrastructure.web..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence.repository..")
                    .as("controllers and web mappers never touch Spring Data repositories directly");
}
