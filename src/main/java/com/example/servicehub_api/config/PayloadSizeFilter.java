package com.example.servicehub_api.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PayloadSizeFilter extends OncePerRequestFilter {

    private final long maxPayloadBytes;

    public PayloadSizeFilter(@Value("${app.max-payload-size-bytes:1048576}") long maxPayloadBytes) {
        this.maxPayloadBytes = maxPayloadBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getContentLengthLong() > maxPayloadBytes) {
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":413,\"erro\":\"Payload muito grande\","
                    + "\"mensagens\":[\"O tamanho máximo permitido foi excedido\"]}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
