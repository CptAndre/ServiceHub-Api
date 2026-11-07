package com.example.servicehub_api;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ClienteIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Test void cadastraAutenticaEConsultaContaPersistente() throws Exception {
        String email = "integracao-" + UUID.randomUUID() + "@example.com";
        String password = "Senha-de-teste-2026";
        String body = mapper.writeValueAsString(Map.of("nome","João & Silva", "email",email.toUpperCase(java.util.Locale.ROOT), "password",password));
        var created = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email",email,"password",password))))
                .andExpect(status().isOk()).andReturn();
        String token = mapper.readTree(login.getResponse().getContentAsString()).path("accessToken").asText();
        mvc.perform(get("/api/clientes/{id}",id).header("Authorization","Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("João & Silva"));
    }
    @Test void cadastroPublicoNaoPermitePrivilegioDeAdministrador() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("nome","Admin indevido","email",UUID.randomUUID()+"@example.com","password","Senha-de-teste-2026","role","ADMIN"))))
                .andExpect(status().isForbidden());
    }
}
