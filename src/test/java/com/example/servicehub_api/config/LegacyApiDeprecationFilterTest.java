package com.example.servicehub_api.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class LegacyApiDeprecationFilterTest {
    @Test void sunsetIsOnlyAdvertisedWhenExplicitlyConfigured() throws Exception {
        var filter = new LegacyApiDeprecationFilter("2026-11-07T00:00:00Z", "2027-03-01T00:00:00Z");
        var request = new MockHttpServletRequest("GET", "/api/clientes");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals("@" + Instant.parse("2026-11-07T00:00:00Z").getEpochSecond(), response.getHeader("Deprecation"));
        assertEquals("Mon, 01 Mar 2027 00:00:00 GMT", response.getHeader("Sunset"));
    }

    @Test void preventsAnnouncingRemovalBeforeTheMinimumNotice() {
        assertThrows(IllegalArgumentException.class, () -> new LegacyApiDeprecationFilter(
                "2026-11-07T00:00:00Z", "2026-12-01T00:00:00Z"));
    }
}
