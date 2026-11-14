package com.example.servicehub_api;

import com.example.servicehub_api.dto.AvaliacaoRequestDTO;
import com.example.servicehub_api.dto.ClienteRequestDTO;
import com.example.servicehub_api.dto.ContratoResponseDTO;
import com.example.servicehub_api.dto.ContratoStatusRequestDTO;
import com.example.servicehub_api.dto.ServicoRequestDTO;
import com.example.servicehub_api.model.RequestStatus;
import com.example.servicehub_api.model.ServiceCategory;
import com.example.servicehub_api.model.UserRole;
import com.example.servicehub_api.model.Usuario;
import com.example.servicehub_api.service.AccountService;
import com.example.servicehub_api.service.AvaliacaoService;
import com.example.servicehub_api.service.ContratoService;
import com.example.servicehub_api.service.ServicoService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres-test")
@EnabledIf("com.example.servicehub_api.PostgresTestDatabase#isEnabled")
class ArchitecturePostgresIntegrationTest {
    @Autowired AccountService accounts;
    @Autowired ContratoService contratos;
    @Autowired ServicoService servicos;
    @Autowired AvaliacaoService avaliacoes;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManagerFactory entityManagerFactory;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        PostgresTestDatabase.configure(properties);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"COMPLETED", "CANCELLED"})
    void concurrentTerminalTransitionsCannotOverwriteEachOther(RequestStatus winner) throws Exception {
        Fixture fixture = createBookings(1);
        long id = fixture.requests().getFirst();
        as(fixture.provider());
        contratos.atualizarStatus(id, status(RequestStatus.ACCEPTED));
        contratos.atualizarStatus(id, status(RequestStatus.IN_PROGRESS));
        RequestStatus competitor = winner == RequestStatus.COMPLETED ? RequestStatus.CANCELLED : RequestStatus.COMPLETED;
        var firstLocked = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var secondPid = new CompletableFuture<Integer>();
        try (var workers = Executors.newFixedThreadPool(2)) {
            try {
                var first = workers.submit(() -> {
                    as(fixture.provider());
                    try {
                        return new TransactionTemplate(transactionManager).execute(tx -> {
                            ContratoResponseDTO result = contratos.atualizarStatus(id, status(winner));
                            firstLocked.countDown();
                            awaitLatch(releaseFirst);
                            return result;
                        });
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
                assertTrue(firstLocked.await(15, TimeUnit.SECONDS), "A primeira transação não obteve o bloqueio");
                var second = workers.submit(() -> {
                    as(fixture.provider());
                    try {
                        return new TransactionTemplate(transactionManager).execute(tx -> {
                            Integer pid = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                            assertNotNull(pid);
                            secondPid.complete(pid);
                            contratos.atualizarStatus(id, status(competitor));
                            return 200;
                        });
                    } catch (ResponseStatusException ex) {
                        return ex.getStatusCode().value();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
                // Confirma a disputa real no banco antes de liberar o vencedor.
                awaitDatabaseLock(secondPid.get(15, TimeUnit.SECONDS));
                assertFalse(second.isDone());
                releaseFirst.countDown();
                ContratoResponseDTO result = first.get(15, TimeUnit.SECONDS);
                assertNotNull(result);
                assertEquals(winner.name(), result.getStatus());
                assertEquals(Integer.valueOf(409), second.get(15, TimeUnit.SECONDS));
                assertEquals(winner.name(), jdbc.queryForObject("SELECT status FROM service_requests WHERE id=?", String.class, id));

                as(fixture.client());
                var review = new AvaliacaoRequestDTO();
                review.setContratoId(id);
                review.setNota(5);
                if (winner == RequestStatus.COMPLETED) {
                    avaliacoes.salvar(review);
                } else {
                    var error = assertThrows(ResponseStatusException.class, () -> avaliacoes.salvar(review));
                    assertEquals(409, error.getStatusCode().value());
                }
                assertEquals(Integer.valueOf(0), jdbc.queryForObject("""
                        SELECT count(*) FROM reviews r JOIN service_requests s ON s.id=r.request_id
                        WHERE s.id=? AND s.status<>'COMPLETED'
                        """, Integer.class, id));
            } finally {
                releaseFirst.countDown();
                workers.shutdownNow();
                assertTrue(workers.awaitTermination(15, TimeUnit.SECONDS), "As transações de teste não terminaram");
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"CLIENT", "PROVIDER"})
    void paginatedBookingsDoNotQueryEachServiceSeparately(UserRole reader) {
        Fixture fixture = createBookings(10);
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean previouslyEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            as(reader == UserRole.CLIENT ? fixture.client() : fixture.provider());
            var pageable = PageRequest.of(0, 5, Sort.by("id"));
            var page = reader == UserRole.CLIENT
                    ? contratos.buscarPorCliente(fixture.client().getId(), pageable)
                    : contratos.buscarPorPrestador(fixture.provider().getId(), pageable);
            assertEquals(5, page.getNumberOfElements());
            assertEquals(10, page.getTotalElements());
            assertEquals(2, page.getTotalPages());
            assertEquals(fixture.requests().subList(0, 5), page.map(ContratoResponseDTO::getId).getContent());
            page.forEach(dto -> {
                assertEquals(fixture.provider().getId(), dto.getPrestadorId());
                assertEquals(fixture.client().getId(), dto.getClienteId());
                assertNotNull(dto.getServicoId());
            });
            long statements = statistics.getPrepareStatementCount();
            assertTrue(statements <= 4, () -> "N+1 na listagem: " + statements + " consultas para 5 contratos");
            System.out.printf("Listagem de contratos %s: %d consultas SQL, incluindo paginação%n", reader, statements);
        } finally {
            statistics.setStatisticsEnabled(previouslyEnabled);
        }
    }

    private void awaitDatabaseLock(int pid) {
        await().alias("A transição concorrente deve aguardar o bloqueio da ordem")
                .pollInSameThread()
                .pollInterval(Duration.ofMillis(25))
                .atMost(Duration.ofSeconds(15))
                .until(() -> Boolean.TRUE.equals(jdbc.queryForObject(
                        "SELECT cardinality(pg_blocking_pids(?)) > 0", Boolean.class, pid)));
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) throw new IllegalStateException("Tempo de espera da transação excedido");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Transação de teste interrompida", ex);
        }
    }

    private Fixture createBookings(int count) {
        Usuario client = account(UserRole.CLIENT), provider = account(UserRole.PROVIDER);
        List<Long> requests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            as(provider);
            var offering = new ServicoRequestDTO();
            offering.setTitulo("Serviço " + i);
            offering.setPreco(new BigDecimal("150.00"));
            offering.setCategoria(ServiceCategory.OUTROS);
            offering.setPrestadorId(provider.getId());
            long serviceId = servicos.criar(offering).getId();
            as(client);
            requests.add(contratos.bookService(client.getId(), serviceId).getId());
        }
        return new Fixture(client, provider, requests);
    }

    private Usuario account(UserRole role) {
        var dto = new ClienteRequestDTO();
        dto.setNome("Usuário de regressão");
        dto.setEmail(UUID.randomUUID() + "@example.com");
        dto.setPassword("Senha-de-regressao-2026");
        return accounts.criar(dto, role);
    }

    private void as(Usuario user) {
        var principal = User.withUsername(user.getEmail()).password("unused").roles(user.getPapel().name()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private ContratoStatusRequestDTO status(RequestStatus value) {
        var dto = new ContratoStatusRequestDTO();
        dto.setStatus(value);
        return dto;
    }

    private record Fixture(Usuario client, Usuario provider, List<Long> requests) {}
}
