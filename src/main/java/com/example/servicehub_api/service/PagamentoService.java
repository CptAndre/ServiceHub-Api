package com.example.servicehub_api.service;
import com.example.servicehub_api.dto.*;
import com.example.servicehub_api.model.Pagamento;
import com.example.servicehub_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service @RequiredArgsConstructor @Transactional
public class PagamentoService {
    private final PagamentoRepository pagamentos;
    private final ContratoRepository contratos;
    private final ResourceAccess access;
    public PagamentoResponseDTO salvar(PagamentoRequestDTO dto) {
        var contrato = contratos.findById(dto.getContratoId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado"));
        access.requireOwner(contrato.getCliente().getId());
        Pagamento pagamento = new Pagamento();
        pagamento.setValorPago(dto.getValorPago());
        pagamento.setMetodoPagamento(dto.getMetodoPagamento().trim());
        pagamento.setContrato(contrato);
        return PagamentoResponseDTO.from(pagamentos.save(pagamento));
    }
    @Transactional(readOnly = true)
    public Page<PagamentoResponseDTO> buscarPorContrato(Long id, Pageable pageable) {
        var contrato = contratos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado"));
        access.requireRequestAccess(contrato);
        return pagamentos.findByContratoId(id, pageable).map(PagamentoResponseDTO::from);
    }
}
