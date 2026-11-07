package com.example.servicehub_api.service;
import com.example.servicehub_api.dto.ClienteRequestDTO;
import com.example.servicehub_api.model.*;
import com.example.servicehub_api.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ClienteServiceTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ContratoRepository contratos = mock(ContratoRepository.class);
    private final ResourceAccess access = mock(ResourceAccess.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AccountService service = new AccountService(usuarios, contratos, encoder, access);
    @Test void normalizaEmailGuardaBcryptEPreservaTextoSemCodificacaoHtml() {
        ClienteRequestDTO dto = new ClienteRequestDTO();
        dto.setNome(" João & Silva "); dto.setEmail(" ANA@EXAMPLE.COM "); dto.setPassword("Senha-de-teste-2026");
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));
        Usuario user = service.criar(dto, UserRole.CLIENT);
        assertEquals("ana@example.com", user.getEmail());
        assertEquals("João & Silva", user.getNome());
        assertNotEquals(dto.getPassword(), user.getPasswordHash());
        assertTrue(encoder.matches(dto.getPassword(), user.getPasswordHash()));
        assertEquals(UserRole.CLIENT, user.getPapel());
    }
    @Test void rejeitaEmailJaCadastrado() {
        Usuario existing = new Usuario(); existing.setId(1L);
        when(usuarios.findByEmail("ana@example.com")).thenReturn(Optional.of(existing));
        ClienteRequestDTO dto = new ClienteRequestDTO(); dto.setEmail("ANA@example.com");
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.criar(dto, UserRole.CLIENT));
        assertEquals(409, error.getStatusCode().value());
        verify(usuarios, never()).save(any());
    }
}
