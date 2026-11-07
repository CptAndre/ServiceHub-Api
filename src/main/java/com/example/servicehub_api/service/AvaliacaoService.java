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
public class AvaliacaoService {
    private final AvaliacaoRepository avaliacoes;
    private final ContratoRepository contratos;
    private final UsuarioRepository usuarios;
    private final ResourceAccess access;
    public AvaliacaoResponseDTO salvar(AvaliacaoRequestDTO dto) {
        Contrato request = contratos.findById(dto.getContratoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado"));
        access.requireOwner(request.getCliente().getId());
        if (request.getStatus() != RequestStatus.COMPLETED) throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente ordens concluídas podem ser avaliadas");
        if (avaliacoes.existsByContratoId(request.getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ordem já avaliada");
        Avaliacao review = new Avaliacao();
        review.setNota(dto.getNota().shortValue());
        review.setComentario(dto.getComentario() == null ? null : dto.getComentario().trim());
        review.setContrato(request);
        review.setReviewer(request.getCliente());
        return AvaliacaoResponseDTO.from(avaliacoes.save(review));
    }
    @Transactional(readOnly = true)
    public Page<AvaliacaoResponseDTO> buscarPorPrestador(Long id, Pageable pageable) {
        if (usuarios.findById(id).filter(u -> u.getPapel() == UserRole.PROVIDER).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestador não encontrado");
        return avaliacoes.findByContratoServicoPrestadorId(id, pageable).map(AvaliacaoResponseDTO::from);
    }
}
