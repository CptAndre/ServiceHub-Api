package com.example.servicehub_api.repository;
import com.example.servicehub_api.model.Contrato;
import com.example.servicehub_api.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    @EntityGraph(attributePaths = {"servico", "servico.prestador"})
    Page<Contrato> findByClienteId(Long clienteId, Pageable pageable);
    @EntityGraph(attributePaths = {"servico", "servico.prestador"})
    Page<Contrato> findByServicoPrestadorId(Long prestadorId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contrato c where c.id = :id")
    Optional<Contrato> findByIdForUpdate(@Param("id") Long id);
    boolean existsByClienteIdAndStatusIn(Long id, Collection<RequestStatus> status);
    boolean existsByServicoPrestadorIdAndStatusIn(Long id, Collection<RequestStatus> status);
    boolean existsByServicoIdAndStatusIn(Long id, Collection<RequestStatus> status);
}
