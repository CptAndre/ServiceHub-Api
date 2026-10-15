package com.example.servicehub_api.service;

import com.example.servicehub_api.dto.AvaliacaoRequestDTO;
import com.example.servicehub_api.dto.AvaliacaoResponseDTO;
import com.example.servicehub_api.model.Avaliacao;
import com.example.servicehub_api.repository.AvaliacaoRepository;
import com.example.servicehub_api.repository.ContratoRepository;
import com.example.servicehub_api.repository.PrestadorRepository;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final ContratoRepository contratoRepository;
    private final PrestadorRepository prestadorRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository, ContratoRepository contratoRepository,
                            PrestadorRepository prestadorRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.contratoRepository = contratoRepository;
        this.prestadorRepository = prestadorRepository;
    }

    public AvaliacaoResponseDTO salvar(AvaliacaoRequestDTO dto) {
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setNota(dto.getNota());
        avaliacao.setComentario(dto.getComentario() == null ? null :
                StringEscapeUtils.escapeHtml4(dto.getComentario().trim()));
        avaliacao.setContrato(contratoRepository.findById(dto.getContratoId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado")));
        return AvaliacaoResponseDTO.from(avaliacaoRepository.save(avaliacao));
    }

    public Page<AvaliacaoResponseDTO> buscarPorPrestador(Long prestadorId, Pageable pageable) {
        if (!prestadorRepository.existsById(prestadorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestador não encontrado");
        }
        return avaliacaoRepository.findByContratoPrestadorId(prestadorId, pageable)
                .map(AvaliacaoResponseDTO::from);
    }
}
