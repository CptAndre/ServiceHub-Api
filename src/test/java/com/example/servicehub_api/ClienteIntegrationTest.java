package com.example.servicehub_api;

import com.example.servicehub_api.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClienteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void criaEConsultaClienteNoBancoH2() throws Exception {
        String email = "integracao-" + UUID.randomUUID() + "@example.com";
        String token = bearerToken("client", "CLIENT");

        MvcResult createResult = mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Cliente Integração\","
                                + "\"email\":\"" + email + "\","
                                + "\"telefone\":\"11999999999\","
                                + "\"documento\":\"52998224725\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        JsonNode createdClient = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long clientId = createdClient.path("id").asLong();
        assertTrue(clientId > 0);

        mockMvc.perform(get("/api/clientes/{id}", clientId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clientId))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.nome").value("Cliente Integra&ccedil;&atilde;o"));
    }

    private String bearerToken(String username, String role) {
        var user = User.withUsername(username)
                .password("unused-in-this-test")
                .roles(role)
                .build();
        return "Bearer " + jwtService.generateToken(user);
    }
}
