package com.example.servicehub_api.config;

import io.micrometer.context.ContextSnapshotFactory;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RequestIdFilterTest {
    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() { MDC.clear(); }

    @ParameterizedTest
    @ValueSource(strings = {"cliente-123", "A.b_9", "0123456789012345678901234567890123456789012345678901234567890123"})
    void acceptsSafeIdsAndCleansTheThread(String id) throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, id);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> assertEquals(id, MDC.get(RequestIdFilter.MDC_KEY)));
        assertEquals(id, response.getHeader(RequestIdFilter.HEADER));
        assertNull(MDC.get(RequestIdFilter.MDC_KEY));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"bad id", "bad\nid", "bad\rid", "<script>",
            "01234567890123456789012345678901234567890123456789012345678901234"})
    void replacesMissingOrUnsafeIdsWithUuids(String received) throws Exception {
        var request = new MockHttpServletRequest();
        if (received != null) request.addHeader(RequestIdFilter.HEADER, received);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            String id = response.getHeader(RequestIdFilter.HEADER);
            assertNotNull(id);
            assertDoesNotThrow(() -> UUID.fromString(id));
            assertEquals(id, MDC.get(RequestIdFilter.MDC_KEY));
        });
        assertNull(MDC.get(RequestIdFilter.MDC_KEY));
    }

    @Test
    void cleansMdcEvenWhenTheChainFails() {
        assertThrows(ServletException.class, () -> filter.doFilter(new MockHttpServletRequest(),
                new MockHttpServletResponse(), (req, res) -> { throw new ServletException("test failure"); }));
        assertNull(MDC.get(RequestIdFilter.MDC_KEY));
    }

    @Test
    void separateRequestsOnTheSameThreadReceiveDifferentIds() throws Exception {
        var first = new MockHttpServletResponse();
        var second = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), first, (req, res) -> {});
        filter.doFilter(new MockHttpServletRequest(), second, (req, res) -> {});
        assertNotEquals(first.getHeader(RequestIdFilter.HEADER), second.getHeader(RequestIdFilter.HEADER));
        assertNull(MDC.get(RequestIdFilter.MDC_KEY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASYNC", "ERROR"})
    void redispatchKeepsTheOriginalId(String type) throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {});
        String original = response.getHeader(RequestIdFilter.HEADER);
        response.reset();
        request.setDispatcherType(DispatcherType.valueOf(type));
        filter.doFilter(request, response, (req, res) -> assertEquals(original, MDC.get(RequestIdFilter.MDC_KEY)));
        assertEquals(original, response.getHeader(RequestIdFilter.HEADER));
        assertNull(MDC.get(RequestIdFilter.MDC_KEY));
    }

    @Test
    void contextPropagationRestoresAndCleansRequestIdInAnotherThread() throws Exception {
        MDC.put(RequestIdFilter.MDC_KEY, "graphql-safe-id");
        var snapshot = ContextSnapshotFactory.builder().build().captureAll();
        MDC.remove(RequestIdFilter.MDC_KEY);
        try (var worker = Executors.newSingleThreadExecutor()) {
            try {
                worker.submit(() -> {
                    assertNull(MDC.get(RequestIdFilter.MDC_KEY));
                    try (var ignored = snapshot.setThreadLocals()) {
                        assertEquals("graphql-safe-id", MDC.get(RequestIdFilter.MDC_KEY));
                    }
                    assertNull(MDC.get(RequestIdFilter.MDC_KEY));
                }).get(5, TimeUnit.SECONDS);
            } finally {
                worker.shutdownNow();
            }
        }
    }
}
