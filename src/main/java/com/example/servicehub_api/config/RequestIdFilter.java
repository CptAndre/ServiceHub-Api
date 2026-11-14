package com.example.servicehub_api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    private static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".id";
    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    @Override
    protected boolean shouldNotFilterAsyncDispatch() { return false; }

    @Override
    protected boolean shouldNotFilterErrorDispatch() { return false; }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String id = idFor(request);
        response.setHeader(HEADER, id);
        MDC.put(MDC_KEY, id);
        try {
            chain.doFilter(request, response);
            if (!request.isAsyncStarted()) {
                Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                // Somente o template configurado no servidor; sem URL, query, body ou credenciais.
                log.info("HTTP method={} route={} status={}", request.getMethod(),
                        route == null ? "<unmapped>" : route, response.getStatus());
            }
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    @Override
    protected void doFilterNestedErrorDispatch(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                               @NonNull FilterChain chain) throws ServletException, IOException {
        response.setHeader(HEADER, idFor(request));
        chain.doFilter(request, response);
    }

    private String idFor(HttpServletRequest request) {
        if (request.getAttribute(ATTRIBUTE) instanceof String id) return id;
        String received = request.getHeader(HEADER);
        String id = received != null && VALID_ID.matcher(received).matches() ? received : UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, id);
        return id;
    }
}
