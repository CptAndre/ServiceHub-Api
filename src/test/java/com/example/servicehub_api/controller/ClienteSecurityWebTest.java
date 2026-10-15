package com.example.servicehub_api.controller;

import com.example.servicehub_api.config.SecurityConfig;
import com.example.servicehub_api.dto.ClienteResponseDTO;
import com.example.servicehub_api.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
@Import(SecurityConfig.class)
class ClienteSecurityWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void userCannotDeleteClient() throws Exception {
        mockMvc.perform(delete("/api/clientes/1").header("Authorization", basic("user", "user123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(clienteService);
    }

    @Test
    void adminCanDeleteClient() throws Exception {
        mockMvc.perform(delete("/api/clientes/1").header("Authorization", basic("admin", "admin123")))
                .andExpect(status().isNoContent());

        verify(clienteService).deletar(1L);
    }

    @Test
    void userCanCreateClient() throws Exception {
        when(clienteService.salvar(any())).thenReturn(
                new ClienteResponseDTO(1L, "Ana", "12345678900", "ana@example.com", "11999999999"));

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", basic("user", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@example.com\","
                                + "\"telefone\":\"11999999999\",\"documento\":\"12345678900\"}"))
                .andExpect(status().isCreated());

        org.mockito.Mockito.verify(clienteService).salvar(any());
    }

    @Test
    void invalidInputReturnsStandardValidationErrorAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", basic("user", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"invalid\",\"telefone\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagens").isArray());

        verifyNoInteractions(clienteService);
    }

    private String basic(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
