package com.example.servicehub_api.repository;
import com.example.servicehub_api.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    boolean existsByContratoId(Long contratoId);
    Page<Avaliacao> findByContratoServicoPrestadorId(Long prestadorId, Pageable pageable);
}
