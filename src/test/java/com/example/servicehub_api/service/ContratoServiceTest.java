package com.example.servicehub_api.service;

import com.example.servicehub_api.model.Cliente;
import com.example.servicehub_api.model.Contrato;
import com.example.servicehub_api.model.Prestador;
import com.example.servicehub_api.model.Servico;
import com.example.servicehub_api.repository.ClienteRepository;
import com.example.servicehub_api.repository.ContratoRepository;
import com.example.servicehub_api.repository.PrestadorRepository;
import com.example.servicehub_api.repository.ServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratoServiceTest {

    @Mock
    private ContratoRepository contratoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PrestadorRepository prestadorRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @InjectMocks
    private ContratoService service;

    @Test
    void bookServiceCriaContratoComDadosDoCatalogo() {
        Cliente cliente = cliente(6L);
        Prestador prestador = prestador(1L);
        Servico servico = servico(15L, prestador);
        when(clienteRepository.findById(6L)).thenReturn(Optional.of(cliente));
        when(servicoRepository.findById(15L)).thenReturn(Optional.of(servico));
        when(contratoRepository.save(any(Contrato.class))).thenAnswer(invocation -> {
            Contrato saved = invocation.getArgument(0);
            saved.setId(25L);
            return saved;
        });

        var result = service.bookService(6L, 15L);

        ArgumentCaptor<Contrato> saved = ArgumentCaptor.forClass(Contrato.class);
        verify(contratoRepository).save(saved.capture());
        Contrato contrato = saved.getValue();
        assertAll(
                () -> assertEquals(cliente, contrato.getCliente()),
                () -> assertEquals(prestador, contrato.getPrestador()),
                () -> assertEquals(servico, contrato.getServico()),
                () -> assertEquals("&lt;b&gt;Limpeza&lt;/b&gt;: &lt;script&gt;alert(1)&lt;/script&gt;",
                        contrato.getDescricaoServico()),
                () -> assertEquals(new BigDecimal("150.00"), contrato.getValorTotal()),
                () -> assertEquals("AGENDADO", contrato.getStatus()),
                () -> assertEquals(6L, result.getClienteId()),
                () -> assertEquals(1L, result.getPrestadorId()),
                () -> assertEquals(15L, result.getServicoId())
        );
    }

    @Test
    void bookServiceRetornaNotFoundQuandoClienteNaoExiste() {
        when(clienteRepository.findById(404L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.bookService(404L, 15L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(servicoRepository);
        verify(contratoRepository, never()).save(any(Contrato.class));
    }

    @Test
    void bookServiceRetornaNotFoundQuandoServicoNaoExiste() {
        when(clienteRepository.findById(6L)).thenReturn(Optional.of(cliente(6L)));
        when(servicoRepository.findById(404L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.bookService(6L, 404L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(contratoRepository, never()).save(any(Contrato.class));
    }

    private Cliente cliente(Long id) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setNome("Cliente de teste");
        return cliente;
    }

    private Prestador prestador(Long id) {
        Prestador prestador = new Prestador();
        prestador.setId(id);
        prestador.setNome("Prestador de teste");
        return prestador;
    }

    private Servico servico(Long id, Prestador prestador) {
        Servico servico = new Servico();
        servico.setId(id);
        servico.setTitulo("<b>Limpeza</b>");
        servico.setDescricao("<script>alert(1)</script>");
        servico.setPreco(new BigDecimal("150.00"));
        servico.setPrestador(prestador);
        return servico;
    }
}
