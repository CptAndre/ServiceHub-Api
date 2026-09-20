package com.example.servicehub_api.repository;

import com.example.servicehub_api.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    // Busca todas as avaliações passando pelo contrato até chegar no montador
    List<Avaliacao> findByContratoMontadorId(Long montadorId);
}