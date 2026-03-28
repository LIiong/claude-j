package com.claudej.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 架构守护测试 — 自动化验证 DDD + 六边形架构的依赖方向。
 *
 * 依赖方向: adapter -> application -> domain <- infrastructure
 *
 * 这些测试替代人工 review 中的"依赖方向检查"，
 * 每次 mvn test 自动执行，违规即失败。
 */
class HexagonalArchitectureTest {

    private static JavaClasses allClasses;

    @BeforeAll
    static void importClasses() {
        allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.claudej");
    }

    // ==================== 依赖方向守护 ====================

    @Nested
    @DisplayName("依赖方向: adapter -> application -> domain <- infrastructure")
    class DependencyDirection {

        @Test
        @DisplayName("domain 不依赖 application")
        void domain_should_not_depend_on_application() {
            noClasses()
                    .that().resideInAPackage("com.claudej.domain..")
                    .should().dependOnClassesThat().resideInAPackage("com.claudej.application..")
                    .check(allClasses);
        }

        @Test
        @DisplayName("domain 不依赖 infrastructure")
        void domain_should_not_depend_on_infrastructure() {
            noClasses()
                    .that().resideInAPackage("com.claudej.domain..")
                    .should().dependOnClassesThat().resideInAPackage("com.claudej.infrastructure..")
                    .check(allClasses);
        }

        @Test
        @DisplayName("domain 不依赖 adapter")
        void domain_should_not_depend_on_adapter() {
            noClasses()
                    .that().resideInAPackage("com.claudej.domain..")
                    .should().dependOnClassesThat().resideInAPackage("com.claudej.adapter..")
                    .check(allClasses);
        }

        @Test
        @DisplayName("application 不依赖 infrastructure")
        void application_should_not_depend_on_infrastructure() {
            noClasses()
                    .that().resideInAPackage("com.claudej.application..")
                    .should().dependOnClassesThat().resideInAPackage("com.claudej.infrastructure..")
                    .check(allClasses);
        }

        @Test
        @DisplayName("application 不依赖 adapter")
        void application_should_not_depend_on_adapter() {
            noClasses()
                    .that().resideInAPackage("com.claudej.application..")
                    .should().dependOnClassesThat().resideInAPackage("com.claudej.adapter..")
                    .check(allClasses);
        }

        @Test
        @DisplayName("adapter 不依赖 infrastructure")
        void adapter_should_not_depend_on_infrastructure() {
            noClasses()
                    .that().resideInAPackage("com.claudej.adapter..")
                    .should().dependOnClassesThat().resideInAPackage("com.claudej.infrastructure..")
                    .check(allClasses);
        }

        @Test
        @DisplayName("infrastructure 不依赖 adapter")
        void infrastructure_should_not_depend_on_adapter() {
            noClasses()
                    .that().resideInAPackage("com.claudej.infrastructure..")
                    .should().dependOnClassesThat().resideInAPackage("com.claudej.adapter..")
                    .check(allClasses);
        }
    }

    // ==================== domain 层纯净性 ====================

    @Nested
    @DisplayName("domain 层纯净性: 禁止框架依赖")
    class DomainPurity {

        @Test
        @DisplayName("domain 不依赖 Spring")
        void domain_should_not_use_spring() {
            noClasses()
                    .that().resideInAPackage("com.claudej.domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .check(allClasses);
        }

        @Test
        @DisplayName("domain 不依赖 MyBatis-Plus")
        void domain_should_not_use_mybatis() {
            noClasses()
                    .that().resideInAPackage("com.claudej.domain..")
                    .should().dependOnClassesThat().resideInAPackage("com.baomidou..")
                    .check(allClasses);
        }

        @Test
        @DisplayName("domain 不依赖 javax.persistence")
        void domain_should_not_use_jpa() {
            noClasses()
                    .that().resideInAPackage("com.claudej.domain..")
                    .should().dependOnClassesThat().resideInAPackage("javax.persistence..")
                    .check(allClasses);
        }
    }

    // ==================== 命名规范守护 ====================

    @Nested
    @DisplayName("命名规范")
    class NamingConventions {

        @Test
        @DisplayName("infrastructure 持久化对象以 DO 结尾")
        void data_objects_should_end_with_DO() {
            classes()
                    .that().resideInAPackage("..persistence.dataobject..")
                    .should().haveSimpleNameEndingWith("DO")
                    .check(allClasses);
        }

        @Test
        @DisplayName("application DTO 以 DTO 结尾")
        void dtos_should_end_with_DTO() {
            classes()
                    .that().resideInAPackage("..application..dto..")
                    .should().haveSimpleNameEndingWith("DTO")
                    .check(allClasses);
        }

        @Test
        @DisplayName("adapter Controller 以 Controller 结尾")
        void controllers_should_end_with_Controller() {
            classes()
                    .that().resideInAPackage("..adapter..web..")
                    .and().areAnnotatedWith(org.springframework.stereotype.Controller.class)
                    .or().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should().haveSimpleNameEndingWith("Controller")
                    .check(allClasses);
        }
    }
}
