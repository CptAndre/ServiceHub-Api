package com.example.servicehub_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("postgres-test")
@EnabledIf("com.example.servicehub_api.PostgresTestDatabase#isEnabled")
class PostgresApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        PostgresTestDatabase.configure(p);
    }
    private record Account(long id,String email,String token) {}
    private Account register(String role) throws Exception {
        String email=UUID.randomUUID()+"@example.com";
        var created=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("nome","Usuário de teste","email",email,"password","Senha-de-teste-2026","role",role))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        long id=mapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();
        var logged=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email",email,"password","Senha-de-teste-2026"))))
                .andExpect(status().isOk()).andReturn();
        return new Account(id,email,mapper.readTree(logged.getResponse().getContentAsString()).path("accessToken").asText());
    }
    private long service(Account provider) throws Exception {
        var result=mvc.perform(post("/api/servicos").header("Authorization","Bearer "+provider.token())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("titulo","Manutenção & suporte","descricao","Atendimento técnico","preco",150.00,"categoria","SUPORTE_TECNICO","prestadorId",provider.id()))))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }
    @Test void fluxoCompletoValidaSchemaSnapshotEstadosAutoriaAvaliacaoUnicaESoftDelete() throws Exception {
        Account client=register("CLIENT"),provider=register("PROVIDER"),other=register("CLIENT");
        long serviceId=service(provider);
        String booking=mapper.writeValueAsString(Map.of("clientId",client.id(),"serviceId",serviceId));
        mvc.perform(post("/api/contratos/bookings").header("Authorization","Bearer "+other.token()).contentType(MediaType.APPLICATION_JSON).content(booking)).andExpect(status().isForbidden());
        var created=mvc.perform(post("/api/contratos/bookings").header("Authorization","Bearer "+client.token()).contentType(MediaType.APPLICATION_JSON).content(booking))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.descricaoServico").value("Manutenção & suporte")).andReturn();
        long requestId=mapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();
        jdbc.update("UPDATE services SET price=999 WHERE id=?",serviceId);
        var storedPrice = jdbc.queryForObject("SELECT total_price FROM service_requests WHERE id=?", java.math.BigDecimal.class, requestId);
        assertNotNull(storedPrice);
        assertEquals(0, new java.math.BigDecimal("150.00").compareTo(storedPrice));
        mvc.perform(patch("/api/contratos/{id}",requestId).header("Authorization","Bearer "+provider.token()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}")).andExpect(status().isConflict());
        for(String next:new String[]{"ACCEPTED","IN_PROGRESS","COMPLETED"}) {
            mvc.perform(patch("/api/contratos/{id}",requestId).header("Authorization","Bearer "+provider.token()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("status",next))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(next));
        }
        String review=mapper.writeValueAsString(Map.of("nota",5,"comentario","Ótimo & pontual","contratoId",requestId));
        mvc.perform(post("/api/avaliacoes").header("Authorization","Bearer "+other.token()).contentType(MediaType.APPLICATION_JSON).content(review)).andExpect(status().isForbidden());
        mvc.perform(post("/api/avaliacoes").header("Authorization","Bearer "+client.token()).contentType(MediaType.APPLICATION_JSON).content(review))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.reviewerId").value(client.id()));
        mvc.perform(post("/api/avaliacoes").header("Authorization","Bearer "+client.token()).contentType(MediaType.APPLICATION_JSON).content(review)).andExpect(status().isConflict());
        mvc.perform(post("/api/pagamentos").header("Authorization","Bearer "+client.token()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("valorPago",150,"metodoPagamento","PIX","contratoId",requestId)))).andExpect(status().isCreated());
        mvc.perform(delete("/api/servicos/{id}",serviceId).header("Authorization","Bearer "+provider.token())).andExpect(status().isNoContent());
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT is_active FROM services WHERE id=?", Boolean.class, serviceId));
        mvc.perform(post("/api/contratos/bookings").header("Authorization","Bearer "+client.token()).contentType(MediaType.APPLICATION_JSON).content(booking)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/prestadores/{id}",provider.id()).header("Authorization","Bearer "+provider.token())).andExpect(status().isNoContent());
        mvc.perform(get("/api/prestadores").header("Authorization","Bearer "+provider.token())).andExpect(status().isUnauthorized());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM users WHERE id=? AND NOT is_active",Integer.class,provider.id()));
    }
    @Test void graphqlUsaMesmoModeloEChecaDonoDaReserva() throws Exception {
        Account client=register("CLIENT"),provider=register("PROVIDER"),other=register("CLIENT");
        String createService="mutation { createService(input: {providerId: \""+provider.id()+"\", title: \"Suporte via GraphQL\", price: 150.00, category: SUPORTE_TECNICO}) { id category } }";
        var createdService=mvc.perform(post("/graphql").header("Authorization","Bearer "+provider.token()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("query",createService))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.errors").doesNotExist()).andExpect(jsonPath("$.data.createService.category").value("SUPORTE_TECNICO")).andReturn();
        long serviceId=mapper.readTree(createdService.getResponse().getContentAsString()).path("data").path("createService").path("id").asLong();
        String query="mutation { bookService(input: {clientId: \""+client.id()+"\", serviceId: \""+serviceId+"\"}) { id status serviceId } }";
        mvc.perform(post("/graphql").header("Authorization","Bearer "+client.token()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("query",query))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.errors").doesNotExist()).andExpect(jsonPath("$.data.bookService.status").value("PENDING"));
        mvc.perform(post("/graphql").header("Authorization","Bearer "+other.token()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("query",query))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"));
        String selection="query { servicesByProvider(providerId: \""+provider.id()+"\") { id category price } }";
        mvc.perform(post("/graphql").header("Authorization","Bearer "+client.token()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("query",selection))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.errors").doesNotExist()).andExpect(jsonPath("$.data.servicesByProvider[0].category").value("SUPORTE_TECNICO"));
    }

    @Test void restEGraphqlCompartilhamConsultaEFiltroDePrestadoresAtivos() throws Exception {
        Account client = register("CLIENT"), provider = register("PROVIDER");
        String specialty = "especialidade-" + UUID.randomUUID();
        mvc.perform(put("/api/v1/prestadores/{id}", provider.id()).header("Authorization", "Bearer " + provider.token())
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                                "nome", "Prestador de teste", "email", provider.email(), "especialidade", specialty))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/prestadores/{id}", provider.id()).header("Authorization", "Bearer " + client.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.especialidade").value(specialty));
        String query = "query { providers(specialty: \" " + specialty + " \") { id specialty } }";
        mvc.perform(post("/graphql").header("Authorization", "Bearer " + client.token())
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("query", query))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.providers.length()").value(1))
                .andExpect(jsonPath("$.data.providers[0].id").value(Long.toString(provider.id())));
        mvc.perform(delete("/api/v1/prestadores/{id}", provider.id()).header("Authorization", "Bearer " + provider.token()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/prestadores/{id}", provider.id()).header("Authorization", "Bearer " + client.token()))
                .andExpect(status().isNotFound());
        String byId = "query { provider(id: \"" + provider.id() + "\") { id name } }";
        mvc.perform(post("/graphql").header("Authorization", "Bearer " + client.token())
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("query", byId))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.errors[0].extensions.classification").value("NOT_FOUND"));
        mvc.perform(post("/graphql").header("Authorization", "Bearer " + client.token())
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("query", query))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.providers").isEmpty());
    }

    @Test void errosHttpDoSpringPreservamOsCodigosNaAplicacaoCompleta() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.TEXT_PLAIN).content("plain text"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.status").value(415));
        Account client = register("CLIENT");
        mvc.perform(get("/api/v1/rota-inexistente").header("Authorization", "Bearer " + client.token()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }
}
