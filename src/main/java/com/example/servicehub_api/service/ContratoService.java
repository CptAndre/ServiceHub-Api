package com.example.servicehub_api.service;

import com.example.servicehub_api.model.Contrato;
import com.example.servicehub_api.repository.ContratoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import com.example.servicehub_api.exception.ResourceNotFoundException;

@Service
public class ContratoService {

    private final ContratoRepository contratoRepository;

    public ContratoService(ContratoRepository contratoRepository) {
        this.contratoRepository = contratoRepository;
    }

    public Contrato salvar(Contrato contrato) {
        return contratoRepository.save(contrato);
    }

    public List<Contrato> buscarPorCliente(Long clienteId) {
        return contratoRepository.findByClienteId(clienteId);
    }

    public Contrato buscarPorId(Long id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado com o ID: " + id));
    }
}