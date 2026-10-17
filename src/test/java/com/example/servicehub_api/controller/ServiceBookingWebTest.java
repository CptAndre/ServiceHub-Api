package com.example.servicehub_api.controller;

import com.example.servicehub_api.config.SecurityConfig;
import com.example.servicehub_api.dto.ContratoResponseDTO;
import com.example.servicehub_api.dto.ServicoResponseDTO;
import com.example.servicehub_api.security.JwtAuthenticationFilter;
import com.example.servicehub_api.service.ContratoService;
import com.example.servicehub_api.service.JwtService;
import com.example.servicehub_api.service.ServicoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ServicoController.class, ContratoController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=U2VydmljZUh1Yl9kZXZfb25seV9rZXlfMzJfYnl0ZXNfbWluaW11bQ==",
        "jwt.expiration-ms=60000"
})
class ServiceBookingWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ServicoService servicoService;

    @MockitoBean
    private ContratoService contratoService;

    @Test
    void providerCanCreateService() throws Exception {
        when(servicoService.criar(any())).thenReturn(
                new ServicoResponseDTO(15L, "Limpeza", "Residencial", new BigDecimal("150.00"), 1L));

        mockMvc.perform(post("/api/servicos")
                        .header("Authorization", bearerToken("provider", "PROVIDER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Limpeza\",\"descricao\":\"Residencial\","
                                + "\"preco\":150.00,\"prestadorId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.prestadorId").value(1));

        verify(servicoService).criar(any());
    }

    @Test
    void clientCannotCreateService() throws Exception {
        mockMvc.perform(post("/api/servicos")
                        .header("Authorization", bearerToken("client", "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Limpeza\",\"preco\":150.00,\"prestadorId\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(servicoService);
    }

    @Test
    void clientCanBookService() throws Exception {
        when(contratoService.bookService(6L, 15L)).thenReturn(
                new ContratoResponseDTO(25L, "Limpeza residencial", new BigDecimal("150.00"),
                        "AGENDADO", 6L, 1L, 15L));

        mockMvc.perform(post("/api/contratos/bookings")
                        .header("Authorization", bearerToken("client", "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":6,\"serviceId\":15}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andExpect(jsonPath("$.servicoId").value(15));

        verify(contratoService).bookService(6L, 15L);
    }

    @Test
    void providerCannotBookService() throws Exception {
        mockMvc.perform(post("/api/contratos/bookings")
                        .header("Authorization", bearerToken("provider", "PROVIDER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":6,\"serviceId\":15}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(contratoService);
    }

    @Test
    void invalidBookingReturnsBadRequestAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/contratos/bookings")
                        .header("Authorization", bearerToken("client", "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":0,\"serviceId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(contratoService);
    }

    private String bearerToken(String username, String role) {
        var user = User.withUsername(username)
                .password("unused-in-this-test")
                .roles(role)
                .build();
        return "Bearer " + jwtService.generateToken(user);
    }
}
