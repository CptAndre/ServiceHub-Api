package com.example.servicehub_api.service;
import com.example.servicehub_api.dto.*;
import com.example.servicehub_api.model.*;
import com.example.servicehub_api.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ContratoServiceTest {
    private final ContratoRepository contratos = mock(ContratoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ServicoRepository servicos = mock(ServicoRepository.class);
    private final ResourceAccess access = mock(ResourceAccess.class);
    private final ContratoService service = new ContratoService(contratos, usuarios, servicos, access,
            mock(org.springframework.context.ApplicationEventPublisher.class));
    @Test void reservaCopiaPrecoSemRecodificarTexto() {
        Usuario client = new Usuario(); client.setId(6L);
        Usuario provider = new Usuario(); provider.setId(1L); provider.setPapel(UserRole.PROVIDER);
        Servico offering = new Servico(); offering.setId(15L); offering.setTitulo("Manutenção & suporte");
        offering.setPrestador(provider); offering.setPreco(new BigDecimal("150.00"));
        when(usuarios.findById(6L)).thenReturn(Optional.of(client));
        when(servicos.findById(15L)).thenReturn(Optional.of(offering));
        when(contratos.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.bookService(6L, 15L);
        offering.setPreco(new BigDecimal("999.00"));
        assertEquals(new BigDecimal("150.00"), result.getValorTotal());
        assertEquals("Manutenção & suporte", result.getDescricaoServico());
        assertEquals("PENDING", result.getStatus()); assertEquals(15L, result.getServicoId());
        verify(access).requireOwner(6L);
    }
    @Test void rejeitaClienteInexistenteSemPersistir() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.bookService(99L, 1L));
        assertEquals(404, error.getStatusCode().value()); verifyNoInteractions(servicos); verify(contratos, never()).save(any());
    }
    @Test void rejeitaReaberturaDeOrdemConcluida() {
        Usuario provider = new Usuario(); provider.setId(1L);
        Servico offering = new Servico(); offering.setPrestador(provider);
        Contrato request = new Contrato(); request.setServico(offering); request.setStatus(RequestStatus.COMPLETED);
        when(contratos.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        ContratoStatusRequestDTO dto = new ContratoStatusRequestDTO(); dto.setStatus(RequestStatus.PENDING);
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.atualizarStatus(1L, dto));
        assertEquals(409, error.getStatusCode().value()); verify(contratos, never()).save(any());
    }
}
