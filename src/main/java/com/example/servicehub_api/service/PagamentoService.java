package com.example.servicehub_api.service;

import com.example.servicehub_api.model.Pagamento;
import com.example.servicehub_api.repository.PagamentoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import com.example.servicehub_api.exception.ResourceNotFoundException;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;

    public PagamentoService(PagamentoRepository pagamentoRepository) {
        this.pagamentoRepository = pagamentoRepository;
    }

    public Pagamento salvar(Pagamento pagamento) {
        return pagamentoRepository.save(pagamento);
    }

    public List<Pagamento> buscarPorContrato(Long contratoId) {
        return pagamentoRepository.findByContratoId(contratoId);
    }
}