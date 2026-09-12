package cl.duoc.xyzbank.coreservice.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("core-service's business modules")
class CoreServiceArchitectureTest {

    private static final String[] BUSINESS_PACKAGES = {
        "cl.duoc.xyzbank.coreservice.accounts..",
        "cl.duoc.xyzbank.coreservice.transactions..",
        "cl.duoc.xyzbank.coreservice.interests..",
        "cl.duoc.xyzbank.coreservice.withdrawals.."
    };

    @Test
    @DisplayName("never depend on HttpServletRequest directly; identity, scope, and ownership flow through "
            + "the enforcement filter and CallerContext, never ad hoc header reads or use-case-level checks")
    void neverDependOnHttpServletRequestDirectly() {
        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage(BUSINESS_PACKAGES)
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("jakarta.servlet.http.HttpServletRequest");

        rule.check(new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("cl.duoc.xyzbank.coreservice"));
    }
}
