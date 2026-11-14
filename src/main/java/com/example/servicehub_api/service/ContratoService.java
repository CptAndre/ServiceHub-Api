package com.example.servicehub_api.service;
import com.example.servicehub_api.dto.*;
import com.example.servicehub_api.model.*;
import com.example.servicehub_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service @RequiredArgsConstructor @Transactional
public class ContratoService {
    private final ContratoRepository contratos;
    private final UsuarioRepository usuarios;
    private final ServicoRepository servicos;
    private final ResourceAccess access;
    private final ApplicationEventPublisher events;
    public ContratoResponseDTO salvar(ContratoRequestDTO dto) {
        return ContratoResponseDTO.from(criar(dto.getClienteId(), dto.getServicoId(), dto.getDescricaoServico(), dto.getScheduledAt()));
    }
    public ContratoResponseDTO bookService(Long clienteId, Long servicoId) {
        return ContratoResponseDTO.from(criar(clienteId, servicoId, null, null));
    }
    private Contrato criar(Long clienteId, Long servicoId, String notes, java.time.LocalDateTime scheduledAt) {
        access.requireOwner(clienteId);
        var cliente = usuarios.findById(clienteId).filter(u -> u.getPapel() == UserRole.CLIENT && u.isAtivo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente ativo não encontrado"));
        var servico = servicos.findById(servicoId).filter(s -> s.isAtivo() && s.getPrestador().isAtivo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviço ativo não encontrado"));
        Contrato contrato = new Contrato();
        contrato.setCliente(cliente);
        contrato.setServico(servico);
        contrato.setDescricaoServico(notes == null ? servico.getTitulo() : notes.trim());
        contrato.setScheduledAt(scheduledAt);
        contrato.setValorTotal(servico.getPreco());
        contrato.setStatus(RequestStatus.PENDING);
        Contrato saved = contratos.save(contrato);
        events.publishEvent(new RequestTransitioned(saved.getStatus()));
        return saved;
    }
    @Transactional(readOnly = true)
    public Page<ContratoResponseDTO> buscarPorCliente(Long id, Pageable pageable) {
        access.requireOwner(id);
        if (usuarios.findById(id).filter(u -> u.getPapel() == UserRole.CLIENT).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado");
        return contratos.findByClienteId(id, pageable).map(ContratoResponseDTO::from);
    }
    @Transactional(readOnly = true)
    public Page<ContratoResponseDTO> buscarPorPrestador(Long id, Pageable pageable) {
        access.requireOwner(id);
        if (usuarios.findById(id).filter(u -> u.getPapel() == UserRole.PROVIDER).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestador não encontrado");
        return contratos.findByServicoPrestadorId(id, pageable).map(ContratoResponseDTO::from);
    }
    public ContratoResponseDTO atualizarStatus(Long id, ContratoStatusRequestDTO dto) {
        // A leitura do estado e sua alteração precisam ocorrer sob o mesmo bloqueio.
        Contrato contrato = contratos.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado"));
        access.requireOwner(contrato.getPrestador().getId());
        RequestStatus next = dto.getStatus();
        boolean allowed = switch (contrato.getStatus()) {
            case PENDING -> next == RequestStatus.ACCEPTED || next == RequestStatus.CANCELLED;
            case ACCEPTED -> next == RequestStatus.IN_PROGRESS || next == RequestStatus.CANCELLED;
            case IN_PROGRESS -> next == RequestStatus.COMPLETED || next == RequestStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT, "Transição de status inválida");
        contrato.setStatus(next);
        Contrato saved = contratos.save(contrato);
        events.publishEvent(new RequestTransitioned(saved.getStatus()));
        return ContratoResponseDTO.from(saved);
    }
}
