package com.example.servicehub_api.service;

import com.example.servicehub_api.dto.PagamentoRequestDTO;
import com.example.servicehub_api.dto.PagamentoResponseDTO;
import com.example.servicehub_api.model.Pagamento;
import com.example.servicehub_api.repository.ContratoRepository;
import com.example.servicehub_api.repository.PagamentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final ContratoRepository contratoRepository;

    public PagamentoService(PagamentoRepository pagamentoRepository, ContratoRepository contratoRepository) {
        this.pagamentoRepository = pagamentoRepository;
        this.contratoRepository = contratoRepository;
    }

    public PagamentoResponseDTO salvar(PagamentoRequestDTO dto) {
        Pagamento pagamento = new Pagamento();
        pagamento.setValorPago(dto.getValorPago());
        pagamento.setMetodoPagamento(dto.getMetodoPagamento().trim());
        pagamento.setContrato(contratoRepository.findById(dto.getContratoId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado")));
        return PagamentoResponseDTO.from(pagamentoRepository.save(pagamento));
    }

    public Page<PagamentoResponseDTO> buscarPorContrato(Long contratoId, Pageable pageable) {
        if (!contratoRepository.existsById(contratoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado");
        }
        return pagamentoRepository.findByContratoId(contratoId, pageable).map(PagamentoResponseDTO::from);
    }
}
