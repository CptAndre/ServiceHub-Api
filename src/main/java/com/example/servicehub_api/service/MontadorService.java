package com.example.servicehub_api.service;

import com.example.servicehub_api.model.Montador;
import com.example.servicehub_api.repository.MontadorRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import com.example.servicehub_api.exception.ResourceNotFoundException;

@Service
public class MontadorService {

    private final MontadorRepository montadorRepository;

    public MontadorService(MontadorRepository montadorRepository) {
        this.montadorRepository = montadorRepository;
    }

    public Montador salvar(Montador montador) {
        return montadorRepository.save(montador);
    }

    public List<Montador> buscarTodos() {
        return montadorRepository.findAll();
    }

    public Montador buscarPorId(Long id) {
        return montadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Montador não encontrado com o ID: " + id));
    }

    public void deletar(Long id) {
        Montador montador = buscarPorId(id);
        montadorRepository.delete(montador);
    }
}