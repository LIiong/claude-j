package com.claudej.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.security.cors")
public class CorsProperties {

    private boolean enabled;

    @NotNull
    private Boolean allowCredentials = Boolean.TRUE;

    @PositiveOrZero
    private long maxAge = 1800L;

    private List<String> allowedOrigins = new ArrayList<String>();

    private List<String> allowedMethods = new ArrayList<String>();

    private List<String> allowedHeaders = new ArrayList<String>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(Boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    @AssertTrue(message = "allowedOrigins must not be empty when cors is enabled")
    public boolean isAllowedOriginsValid() {
        return !enabled || (allowedOrigins != null && !allowedOrigins.isEmpty());
    }

    @AssertTrue(message = "allowedMethods must not be empty when cors is enabled")
    public boolean isAllowedMethodsValid() {
        return !enabled || (allowedMethods != null && !allowedMethods.isEmpty());
    }

    @AssertTrue(message = "allowedHeaders must not be empty when cors is enabled")
    public boolean isAllowedHeadersValid() {
        return !enabled || (allowedHeaders != null && !allowedHeaders.isEmpty());
    }
}
