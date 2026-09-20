package com.example.servicehub_api.service;

import com.example.servicehub_api.model.Avaliacao;
import com.example.servicehub_api.repository.AvaliacaoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import com.example.servicehub_api.exception.ResourceNotFoundException;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
    }

    public Avaliacao salvar(Avaliacao avaliacao) {
        return avaliacaoRepository.save(avaliacao);
    }

    public List<Avaliacao> buscarPorMontador(Long montadorId) {
        return avaliacaoRepository.findByContratoMontadorId(montadorId);
    }
}