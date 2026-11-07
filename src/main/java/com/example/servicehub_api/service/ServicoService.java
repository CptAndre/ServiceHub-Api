package com.example.servicehub_api.service;
import com.example.servicehub_api.dto.*;
import com.example.servicehub_api.model.*;
import com.example.servicehub_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service @RequiredArgsConstructor @Transactional
public class ServicoService {
    private final ServicoRepository servicos;
    private final UsuarioRepository usuarios;
    private final ContratoRepository contratos;
    private final ResourceAccess access;
    public ServicoResponseDTO criar(ServicoRequestDTO request) {
        access.requireOwner(request.getPrestadorId());
        Usuario provider = usuarios.findById(request.getPrestadorId())
                .filter(u -> u.getPapel() == UserRole.PROVIDER && u.isAtivo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestador ativo não encontrado"));
        Servico service = new Servico();
        service.setTitulo(request.getTitulo().trim());
        service.setDescricao(request.getDescricao() == null ? null : request.getDescricao().trim());
        service.setPreco(request.getPreco());
        service.setCategoria(request.getCategoria());
        service.setPrestador(provider);
        return ServicoResponseDTO.from(servicos.save(service));
    }
    @Transactional(readOnly = true)
    public Page<ServicoResponseDTO> listarPorPrestador(Long id, Pageable pageable) {
        if (usuarios.findById(id).filter(u -> u.getPapel() == UserRole.PROVIDER).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestador não encontrado");
        }
        return servicos.findByPrestadorIdAndAtivoTrue(id, pageable).map(ServicoResponseDTO::from);
    }
    public void desativar(Long id) {
        Servico service = servicos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviço não encontrado"));
        access.requireOwner(service.getPrestador().getId());
        if (contratos.existsByServicoIdAndStatusIn(id, AccountService.ACTIVE_REQUESTS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Serviço possui ordens ativas");
        }
        service.setAtivo(false);
        servicos.save(service);
    }
}
