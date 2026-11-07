package com.example.servicehub_api.repository;
import com.example.servicehub_api.model.Contrato;
import com.example.servicehub_api.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    Page<Contrato> findByClienteId(Long clienteId, Pageable pageable);
    Page<Contrato> findByServicoPrestadorId(Long prestadorId, Pageable pageable);
    boolean existsByClienteIdAndStatusIn(Long id, Collection<RequestStatus> status);
    boolean existsByServicoPrestadorIdAndStatusIn(Long id, Collection<RequestStatus> status);
    boolean existsByServicoIdAndStatusIn(Long id, Collection<RequestStatus> status);
}
