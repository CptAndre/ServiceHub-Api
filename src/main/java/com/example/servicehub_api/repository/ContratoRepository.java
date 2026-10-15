package com.example.servicehub_api.repository;

import com.example.servicehub_api.model.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    Page<Contrato> findByClienteId(Long clienteId, Pageable pageable);
    Page<Contrato> findByPrestadorId(Long prestadorId, Pageable pageable);
}
