package com.example.servicehub_api.controller;

import com.example.servicehub_api.config.SecurityConfig;
import com.example.servicehub_api.dto.ClienteResponseDTO;
import com.example.servicehub_api.security.JwtAuthenticationFilter;
import com.example.servicehub_api.service.ClienteService;
import com.example.servicehub_api.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=U2VydmljZUh1Yl9kZXZfb25seV9rZXlfMzJfYnl0ZXNfbWluaW11bQ==",
        "jwt.expiration-ms=60000"
})
class ClienteSecurityWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ClienteService clienteService;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidJwtIsRejected() throws Exception {
        mockMvc.perform(get("/api/clientes")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void clientCannotDeleteClient() throws Exception {
        mockMvc.perform(delete("/api/clientes/1").header("Authorization", bearerToken("client", "CLIENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(clienteService);
    }

    @Test
    void adminCanDeleteClient() throws Exception {
        mockMvc.perform(delete("/api/clientes/1").header("Authorization", bearerToken("admin", "ADMIN")))
                .andExpect(status().isNoContent());

        verify(clienteService).deletar(1L);
    }

    @Test
    void clientCanCreateClient() throws Exception {
        when(clienteService.salvar(any())).thenReturn(
                new ClienteResponseDTO(1L, "Ana", "12345678900", "ana@example.com", "11999999999"));

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", bearerToken("client", "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@example.com\","
                                + "\"telefone\":\"11999999999\",\"documento\":\"12345678900\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(clienteService).salvar(any());
    }

    @Test
    void invalidInputReturnsStandardValidationErrorAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", bearerToken("client", "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"invalid\",\"telefone\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagens").isArray());

        verifyNoInteractions(clienteService);
    }

    private String bearerToken(String username, String role) {
        var user = User.withUsername(username)
                .password("unused-in-this-test")
                .roles(role)
                .build();
        return "Bearer " + jwtService.generateToken(user);
    }
}
