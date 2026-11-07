package com.example.servicehub_api.service;
import com.example.servicehub_api.dto.*;
import com.example.servicehub_api.model.*;
import com.example.servicehub_api.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ServicoServiceTest {
    private final ServicoRepository servicos = mock(ServicoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ContratoRepository contratos = mock(ContratoRepository.class);
    private final ResourceAccess access = mock(ResourceAccess.class);
    private final ServicoService service = new ServicoService(servicos, usuarios, contratos, access);
    @Test void criaServicoComCategoriaEPrestadorDoSchemaOficial() {
        Usuario provider = new Usuario(); provider.setId(1L); provider.setPapel(UserRole.PROVIDER);
        when(usuarios.findById(1L)).thenReturn(Optional.of(provider));
        when(servicos.save(any())).thenAnswer(i -> i.getArgument(0));
        ServicoRequestDTO dto = new ServicoRequestDTO(); dto.setPrestadorId(1L); dto.setTitulo(" Manutenção & suporte ");
        dto.setPreco(new BigDecimal("150.00")); dto.setCategoria(ServiceCategory.SUPORTE_TECNICO);
        var result = service.criar(dto);
        assertEquals("Manutenção & suporte", result.getTitulo());
        assertEquals(ServiceCategory.SUPORTE_TECNICO, result.getCategoria());
        verify(access).requireOwner(1L);
    }
    @Test void rejeitaClienteComoPrestador() {
        Usuario client = new Usuario(); client.setId(1L); client.setPapel(UserRole.CLIENT);
        when(usuarios.findById(1L)).thenReturn(Optional.of(client));
        ServicoRequestDTO dto = new ServicoRequestDTO(); dto.setPrestadorId(1L);
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.criar(dto));
        assertEquals(404, error.getStatusCode().value()); verify(servicos, never()).save(any());
    }
}
