package com.claudej.config;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * TraceIdFilter generates a unique requestId for each HTTP request
 * and propagates it via MDC and X-Request-Id response header.
 */
public class TraceIdFilter implements Filter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization required
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String requestId = generateRequestId();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    @Override
    public void destroy() {
        // No cleanup required
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}