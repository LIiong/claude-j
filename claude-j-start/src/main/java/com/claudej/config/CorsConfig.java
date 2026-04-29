package com.claudej.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    private final CorsProperties corsProperties;

    public CorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (corsProperties.isEnabled()) {
            source.registerCorsConfiguration("/**", createConfiguration());
        }
        return source;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(CorsFilter corsFilter) {
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<CorsFilter>();
        registration.setFilter(corsFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public CorsFilter corsFilter(CorsConfigurationSource corsConfigurationSource) {
        return new CorsFilter(corsConfigurationSource);
    }

    private CorsConfiguration createConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(corsProperties.getAllowCredentials());
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setMaxAge(corsProperties.getMaxAge());
        return configuration;
    }
}
