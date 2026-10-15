package com.example.servicehub_api.service;

import com.example.servicehub_api.dto.ServicoRequestDTO;
import com.example.servicehub_api.dto.ServicoResponseDTO;
import com.example.servicehub_api.model.Servico;
import com.example.servicehub_api.repository.PrestadorRepository;
import com.example.servicehub_api.repository.ServicoRepository;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final PrestadorRepository prestadorRepository;

    public ServicoService(ServicoRepository servicoRepository, PrestadorRepository prestadorRepository) {
        this.servicoRepository = servicoRepository;
        this.prestadorRepository = prestadorRepository;
    }

    public ServicoResponseDTO criar(ServicoRequestDTO request) {
        Servico servico = new Servico();
        servico.setTitulo(StringEscapeUtils.escapeHtml4(request.getTitulo().trim()));
        servico.setDescricao(request.getDescricao() == null ? null
                : StringEscapeUtils.escapeHtml4(request.getDescricao().trim()));
        servico.setPreco(request.getPreco());
        servico.setPrestador(prestadorRepository.findById(request.getPrestadorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestador não encontrado")));
        return ServicoResponseDTO.from(servicoRepository.save(servico));
    }

    public Page<ServicoResponseDTO> listarPorPrestador(Long prestadorId, Pageable pageable) {
        if (!prestadorRepository.existsById(prestadorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestador não encontrado");
        }
        return servicoRepository.findByPrestadorId(prestadorId, pageable).map(ServicoResponseDTO::from);
    }
}
