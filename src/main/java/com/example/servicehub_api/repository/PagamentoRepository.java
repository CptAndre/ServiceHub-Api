package com.example.servicehub_api.repository;

import com.example.servicehub_api.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    Page<Pagamento> findByContratoId(Long contratoId, Pageable pageable);
}