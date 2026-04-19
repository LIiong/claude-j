package com.claudej.adapter.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置类
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    /**
     * 配置 OpenAPI 基础信息
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("claude-j API")
                        .description("DDD + Hexagonal Architecture Demo Project API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("claude-j Team")
                                .email("team@claudej.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, createSecurityScheme()));
    }

    /**
     * 配置 JWT Bearer Token 认证方案
     */
    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("请输入 JWT Token，格式: eyJhbGciOiJIUzI1NiIs...");
    }

    /**
     * 用户聚合 API 分组
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .displayName("用户服务")
                .pathsToMatch("/api/v1/users/**")
                .build();
    }

    /**
     * 订单聚合 API 分组
     */
    @Bean
    public GroupedOpenApi orderApi() {
        return GroupedOpenApi.builder()
                .group("order")
                .displayName("订单服务")
                .pathsToMatch("/api/v1/orders/**")
                .build();
    }

    /**
     * 购物车聚合 API 分组
     */
    @Bean
    public GroupedOpenApi cartApi() {
        return GroupedOpenApi.builder()
                .group("cart")
                .displayName("购物车服务")
                .pathsToMatch("/api/v1/cart/**")
                .build();
    }

    /**
     * 优惠券聚合 API 分组
     */
    @Bean
    public GroupedOpenApi couponApi() {
        return GroupedOpenApi.builder()
                .group("coupon")
                .displayName("优惠券服务")
                .pathsToMatch("/api/v1/coupons/**")
                .build();
    }

    /**
     * 短链聚合 API 分组
     */
    @Bean
    public GroupedOpenApi shortlinkApi() {
        return GroupedOpenApi.builder()
                .group("shortlink")
                .displayName("短链服务")
                .pathsToMatch("/api/v1/shortlinks/**", "/s/**")
                .build();
    }

    /**
     * 认证聚合 API 分组
     */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .displayName("认证服务")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    /**
     * 全部 API 分组
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .displayName("全部服务")
                .pathsToMatch("/**")
                .build();
    }
}
