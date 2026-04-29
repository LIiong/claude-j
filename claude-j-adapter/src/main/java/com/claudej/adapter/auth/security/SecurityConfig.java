package com.claudej.adapter.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Spring Security 配置
 * 仅在非测试环境生效
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Profile("!test")
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          JwtAccessDeniedHandler jwtAccessDeniedHandler,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 启用 CORS 协商，确保预检在认证前处理
            .cors().configurationSource(corsConfigurationSource)
            .and()

            // 禁用 CSRF（使用 JWT 无状态认证）
            .csrf().disable()

            // 无状态会话
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            // URL 级授权配置
            .and()
            .authorizeRequests()
                // 公开端点：认证相关接口
                .antMatchers("/api/v1/auth/**").permitAll()
                // 公开端点：短链跳转
                .antMatchers("/s/**").permitAll()
                // 公开端点：健康检查
                .antMatchers("/actuator/**").permitAll()
                // 公开端点：Swagger UI
                .antMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // 其余端点需要认证
                .anyRequest().authenticated()

            // 异常处理
            .and()
            .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)

            // 先执行 CORS 协商，再执行 JWT 认证
            .and()
            .addFilterBefore(new CorsFilter(corsConfigurationSource), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}