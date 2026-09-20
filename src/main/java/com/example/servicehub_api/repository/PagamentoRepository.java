package com.example.servicehub_api.repository;

import com.example.servicehub_api.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findByContratoId(Long contratoId);
}