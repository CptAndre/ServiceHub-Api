package com.example.servicehub_api;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.servicehub_api.config.RequestIdFilter;
import com.example.servicehub_api.dto.ClienteRequestDTO;
import com.example.servicehub_api.dto.ContratoStatusRequestDTO;
import com.example.servicehub_api.model.RequestStatus;
import com.example.servicehub_api.model.UserRole;
import com.example.servicehub_api.model.Usuario;
import com.example.servicehub_api.service.AccountService;
import com.example.servicehub_api.service.ContratoService;
import com.example.servicehub_api.service.ServiceRequestMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"management.prometheus.metrics.export.enabled=true", "management.metrics.use-global-registry=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AccountService accounts;
    @Autowired ContratoService contratos;
    @Autowired MeterRegistry registry;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;
    @Value("${app.version}") String version;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) { PostgresTestDatabase.configure(properties); }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void healthAndInfoArePublicWhileOnlyAdminCanReadMetrics() throws Exception {
        Account client = account(UserRole.CLIENT), provider = account(UserRole.PROVIDER), admin = account(UserRole.ADMIN);
        mvc.perform(get("/actuator/info")).andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("servicehub-api"))
                .andExpect(jsonPath("$.app.version").value(version));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/health").header("Authorization", bearer(client)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/health").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.components.db.status").value("UP"));
        for (String path : new String[]{"/actuator/metrics", "/actuator/prometheus"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(header().exists(RequestIdFilter.HEADER));
            for (Account user : new Account[]{client, provider}) {
                mvc.perform(get(path).header("Authorization", bearer(user)))
                        .andExpect(status().isForbidden()).andExpect(header().exists(RequestIdFilter.HEADER));
            }
            mvc.perform(get(path).header("Authorization", bearer(admin))).andExpect(status().isOk());
        }
        for (String path : new String[]{"/actuator/env", "/actuator/configprops", "/actuator/heapdump"}) {
            mvc.perform(get(path).header("Authorization", bearer(admin))).andExpect(status().isNotFound());
        }
    }

    @Test
    void committedRestAndGraphqlTransitionsAppearInTheAdminScrape() throws Exception {
        Account client = account(UserRole.CLIENT), provider = account(UserRole.PROVIDER), admin = account(UserRole.ADMIN);
        long service = offering(provider);
        double pending = count(RequestStatus.PENDING), cancelled = count(RequestStatus.CANCELLED);
        long id = booking(client, service);
        assertEquals(pending + 1, count(RequestStatus.PENDING));
        for (RequestStatus next : new RequestStatus[]{RequestStatus.ACCEPTED, RequestStatus.IN_PROGRESS, RequestStatus.COMPLETED}) {
            double before = count(next);
            mvc.perform(patch("/api/v1/contratos/{id}", id).header("Authorization", bearer(provider))
                            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("status", next.name()))))
                    .andExpect(status().isOk());
            assertEquals(before + 1, count(next));
        }
        mvc.perform(patch("/api/v1/contratos/{id}", id).header("Authorization", bearer(provider))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict());
        assertEquals(cancelled, count(RequestStatus.CANCELLED));

        Logger logger = (Logger) LoggerFactory.getLogger(ServiceRequestMetrics.class);
        var logs = capturingAppender();
        logger.addAppender(logs);
        try {
            String query = "mutation { bookService(input: {clientId: \"" + client.user().getId()
                    + "\", serviceId: \"" + service + "\"}) { id } }";
            var result = mvc.perform(post("/graphql").header("Authorization", bearer(client))
                    .header(RequestIdFilter.HEADER, "graphql-correlation")
                    .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("query", query)))).andReturn();
            if (result.getRequest().isAsyncStarted()) result = mvc.perform(asyncDispatch(result)).andReturn();
            assertEquals(200, result.getResponse().getStatus());
            var body = mapper.readTree(result.getResponse().getContentAsString());
            assertFalse(body.has("errors"), body.toString());
            long secondId = body.path("data").path("bookService").path("id").asLong();
            assertEquals(pending + 2, count(RequestStatus.PENDING));
            assertEquals("graphql-correlation", result.getResponse().getHeader(RequestIdFilter.HEADER));
            assertTrue(logs.list.stream().anyMatch(e -> "graphql-correlation".equals(e.getMDCPropertyMap().get(RequestIdFilter.MDC_KEY))));
            mvc.perform(patch("/api/v1/contratos/{id}", secondId).header("Authorization", bearer(provider))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                    .andExpect(status().isOk());
            assertEquals(cancelled + 1, count(RequestStatus.CANCELLED));
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
        mvc.perform(get("/actuator/prometheus").header("Authorization", bearer(admin)).accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("servicehub_service_requests_transitions_total{to=\"PENDING\"}")))
                .andExpect(content().string(containsString("servicehub_service_requests_transitions_total{to=\"CANCELLED\"}")));
        var meters = registry.find(ServiceRequestMetrics.TRANSITIONS_METRIC).meters();
        assertEquals(RequestStatus.values().length, meters.size());
        Set<String> states = Arrays.stream(RequestStatus.values()).map(Enum::name).collect(Collectors.toSet());
        meters.forEach(meter -> {
            assertEquals(1, meter.getId().getTags().size());
            assertEquals("to", meter.getId().getTags().getFirst().getKey());
            assertTrue(states.contains(meter.getId().getTag("to")));
        });
    }

    @Test
    void rollbackDoesNotCountCreationsOrTransitions() throws Exception {
        Account client = account(UserRole.CLIENT), provider = account(UserRole.PROVIDER);
        long service = offering(provider);
        as(client);
        double pending = count(RequestStatus.PENDING);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            contratos.bookService(client.user().getId(), service);
            tx.setRollbackOnly();
        });
        assertEquals(pending, count(RequestStatus.PENDING));
        assertEquals(Integer.valueOf(0), jdbc.queryForObject("SELECT count(*) FROM service_requests WHERE client_id=?", Integer.class, client.user().getId()));
        SecurityContextHolder.clearContext();
        long id = booking(client, service);
        double accepted = count(RequestStatus.ACCEPTED);
        as(provider);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var dto = new ContratoStatusRequestDTO(); dto.setStatus(RequestStatus.ACCEPTED);
            contratos.atualizarStatus(id, dto);
            tx.setRollbackOnly();
        });
        assertEquals(accepted, count(RequestStatus.ACCEPTED));
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM service_requests WHERE id=?", String.class, id));
    }

    @Test
    void everyProtectedBusinessMappingRejectsRequestsWithoutToken() throws Exception {
        Set<String> publicPaths = Set.of("/api/auth/login", "/api/auth/register", "/api/info", "/api/hello",
                "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/info", "/api/v1/hello");
        int checked = 0;
        for (var entry : mappings.getHandlerMethods().entrySet()) {
            if (!entry.getValue().getBeanType().getPackageName().equals("com.example.servicehub_api.controller")) continue;
            for (String path : entry.getKey().getPatternValues()) {
                if (publicPaths.contains(path)) continue;
                for (var method : entry.getKey().getMethodsCondition().getMethods()) {
                    String uri = path.replaceAll("\\{[^}]+}", "1");
                    mvc.perform(request(HttpMethod.valueOf(method.name()), uri))
                            .andExpect(status().isUnauthorized()).andExpect(header().exists(RequestIdFilter.HEADER));
                    checked++;
                }
            }
        }
        assertTrue(checked > 0, "Nenhuma rota de negócio foi verificada");
        mvc.perform(post("/graphql")).andExpect(status().isUnauthorized()).andExpect(header().exists(RequestIdFilter.HEADER));
    }

    @Test
    void responseAndLogsShareTheValidatedIdWithoutLoggingCredentials() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestIdFilter.class);
        var logs = capturingAppender();
        logger.addAppender(logs);
        try {
            mvc.perform(get("/api/v1/info").header(RequestIdFilter.HEADER, "report-safe-id")
                            .param("password", "sensitive-probe-password").header("Authorization", "Bearer sensitive-probe-token"))
                    .andExpect(status().isOk()).andExpect(header().string(RequestIdFilter.HEADER, "report-safe-id"));
            assertFalse(logs.list.isEmpty());
            logs.list.forEach(event -> {
                assertEquals("report-safe-id", event.getMDCPropertyMap().get(RequestIdFilter.MDC_KEY));
                assertFalse(event.getFormattedMessage().contains("sensitive-probe"));
            });
            assertNull(MDC.get(RequestIdFilter.MDC_KEY));
            mvc.perform(get("/api/v1/info").header(RequestIdFilter.HEADER, "unsafe id"))
                    .andExpect(status().isOk()).andExpect(header().string(RequestIdFilter.HEADER,
                            matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }

    @Test
    void corsAllowsClientsToSendAndReadRequestIds() throws Exception {
        mvc.perform(options("/api/v1/servicos").header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,X-Request-Id"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Headers", containsString("X-Request-Id")));
        mvc.perform(get("/api/v1/info").header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Expose-Headers", containsString("X-Request-Id")));
    }

    private ListAppender<ILoggingEvent> capturingAppender() {
        var appender = new ListAppender<ILoggingEvent>() {
            @Override protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        appender.start();
        return appender;
    }

    private Account account(UserRole role) throws Exception {
        var dto = new ClienteRequestDTO();
        dto.setNome("Teste de observabilidade"); dto.setEmail(UUID.randomUUID() + "@example.com"); dto.setPassword("Senha-de-observabilidade-2026");
        Usuario user = accounts.criar(dto, role);
        var login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", dto.getEmail(), "password", dto.getPassword()))))
                .andExpect(status().isOk()).andReturn();
        return new Account(user, mapper.readTree(login.getResponse().getContentAsString()).path("accessToken").asText());
    }

    private long offering(Account provider) throws Exception {
        var result = mvc.perform(post("/api/v1/servicos").header("Authorization", bearer(provider))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("titulo", "Suporte de teste", "preco", 150,
                                "categoria", "SUPORTE_TECNICO", "prestadorId", provider.user().getId()))))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private long booking(Account client, long service) throws Exception {
        var result = mvc.perform(post("/api/v1/contratos/bookings").header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("clientId", client.user().getId(), "serviceId", service))))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private double count(RequestStatus status) {
        return registry.get(ServiceRequestMetrics.TRANSITIONS_METRIC).tag("to", status.name()).counter().count();
    }

    private String bearer(Account user) { return "Bearer " + user.token(); }

    private void as(Account account) {
        var principal = User.withUsername(account.user().getEmail()).password("unused").roles(account.user().getPapel().name()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private record Account(Usuario user, String token) {}
}
