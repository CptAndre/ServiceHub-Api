package com.example.servicehub_api.service;

import com.example.servicehub_api.dto.ServicoRequestDTO;
import com.example.servicehub_api.model.Prestador;
import com.example.servicehub_api.model.Servico;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private PrestadorRepository prestadorRepository;

    @InjectMocks
    private ServicoService service;

    @Test
    void criarLimpaTextoEAssociaServicoAoPrestador() {
        Prestador prestador = prestador(7L);
        when(prestadorRepository.findById(7L)).thenReturn(Optional.of(prestador));
        when(servicoRepository.save(any(Servico.class))).thenAnswer(invocation -> {
            Servico saved = invocation.getArgument(0);
            saved.setId(12L);
            return saved;
        });

        ServicoRequestDTO request = new ServicoRequestDTO();
        request.setPrestadorId(7L);
        request.setTitulo("  <b>Limpeza</b>  ");
        request.setDescricao("  <script>alert(1)</script>  ");
        request.setPreco(new BigDecimal("150.00"));

        var result = service.criar(request);

        ArgumentCaptor<Servico> saved = ArgumentCaptor.forClass(Servico.class);
        verify(servicoRepository).save(saved.capture());
        assertEquals("&lt;b&gt;Limpeza&lt;/b&gt;", saved.getValue().getTitulo());
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", saved.getValue().getDescricao());
        assertEquals(prestador, saved.getValue().getPrestador());
        assertEquals(new BigDecimal("150.00"), result.getPreco());
        assertEquals(7L, result.getPrestadorId());
    }

    @Test
    void criarRetornaNotFoundEnaoSalvaQuandoPrestadorNaoExiste() {
        when(prestadorRepository.findById(404L)).thenReturn(Optional.empty());
        ServicoRequestDTO request = new ServicoRequestDTO();
        request.setPrestadorId(404L);
        request.setTitulo("Instalação");
        request.setPreco(new BigDecimal("100.00"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.criar(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(servicoRepository, never()).save(any(Servico.class));
    }

    private Prestador prestador(Long id) {
        Prestador prestador = new Prestador();
        prestador.setId(id);
        prestador.setNome("Prestador de teste");
        return prestador;
    }
}
