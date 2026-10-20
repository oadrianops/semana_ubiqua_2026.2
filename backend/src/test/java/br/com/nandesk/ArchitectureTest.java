package br.com.nandesk;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Regras de arquitetura do monólito modular (especificação 9.3, ADR-0003, RNF-09). */
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("br.com.nandesk");
    }

    @Test
    @DisplayName("9.3: não há ciclos entre módulos")
    void noCyclesBetweenModules() {
        slices().matching("br.com.nandesk.(*)..").should().beFreeOfCycles().check(classes);
    }

    @Test
    @DisplayName("9.3: api não acessa repositórios (infra) diretamente")
    void apiDoesNotUseRepositories() {
        noClasses()
                .that()
                .resideInAPackage("..api..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infra..")
                .check(classes);
    }

    @Test
    @DisplayName("9.3: domain não depende de api, application nem de Spring Web")
    void domainIsIndependent() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..api..", "..application..", "org.springframework.web..")
                .check(classes);
    }

    @Test
    @DisplayName("9.3: um módulo não acessa o infra de outro")
    void noCrossModuleInfra() {
        for (String module : new String[] {
            "admin",
            "audit",
            "auth",
            "catalog",
            "commission",
            "file",
            "lgpd",
            "market",
            "notification",
            "report",
            "search",
            "stall",
            "submission",
            "task",
            "user"
        }) {
            noClasses()
                    .that()
                    .resideInAPackage("br.com.nandesk." + module + "..")
                    .should()
                    .dependOnClassesThat(DescribedPredicate.describe(
                            "infra de outro módulo",
                            c -> c.getPackageName().contains(".infra")
                                    && c.getPackageName().startsWith("br.com.nandesk.")
                                    && !c.getPackageName().startsWith("br.com.nandesk." + module + ".")))
                    .check(classes);
        }
    }

    @Test
    @DisplayName("9.3: controllers ficam em api e nunca devolvem entidades")
    void controllersDoNotReturnEntities() {
        classes()
                .that()
                .areAnnotatedWith(RestController.class)
                .should()
                .resideInAPackage("..api..")
                .check(classes);
        methods()
                .that()
                .areDeclaredInClassesThat()
                .areAnnotatedWith(RestController.class)
                .and()
                .areMetaAnnotatedWith(RequestMapping.class)
                .should()
                .notHaveRawReturnType(DescribedPredicate.describe("entidade JPA", c -> c.isAnnotatedWith(Entity.class)))
                .check(classes);
    }

    @Test
    @DisplayName("9.3: @Transactional nunca em controllers")
    void noTransactionalControllers() {
        noClasses()
                .that()
                .areAnnotatedWith(RestController.class)
                .should()
                .beAnnotatedWith(Transactional.class)
                .check(classes);
        methods()
                .that()
                .areDeclaredInClassesThat()
                .areAnnotatedWith(RestController.class)
                .should()
                .notBeAnnotatedWith(Transactional.class)
                .check(classes);
    }

    @Test
    @DisplayName("9.3: injeção por construtor, sem @Autowired em campo")
    void noFieldInjection() {
        fields().should().notBeAnnotatedWith(Autowired.class).check(classes);
    }

    @Test
    @DisplayName("9.3: o tempo vem do bean Clock (nunca Instant.now() direto)")
    void noDirectInstantNow() {
        noClasses().should().callMethod(Instant.class, "now").check(classes);
    }

    @Test
    @DisplayName("9.3: entidades ficam em domain")
    void entitiesInDomain() {
        classes()
                .that()
                .areAnnotatedWith(Entity.class)
                .should()
                .resideInAPackage("..domain..")
                .check(classes);
    }
}
