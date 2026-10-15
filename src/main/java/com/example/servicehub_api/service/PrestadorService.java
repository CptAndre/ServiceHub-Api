package com.example.servicehub_api.service;

import com.example.servicehub_api.dto.PrestadorRequestDTO;
import com.example.servicehub_api.dto.PrestadorResponseDTO;
import com.example.servicehub_api.model.Prestador;
import com.example.servicehub_api.repository.PrestadorRepository;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PrestadorService {

    private final PrestadorRepository prestadorRepository;

    public PrestadorService(PrestadorRepository prestadorRepository) {
        this.prestadorRepository = prestadorRepository;
    }

    public PrestadorResponseDTO salvar(PrestadorRequestDTO dto) {
        Prestador prestador = new Prestador();
        preencher(prestador, dto);
        return PrestadorResponseDTO.from(prestadorRepository.save(prestador));
    }

    public Page<PrestadorResponseDTO> buscarTodos(Pageable pageable) {
        return prestadorRepository.findAll(pageable).map(PrestadorResponseDTO::from);
    }

    public PrestadorResponseDTO buscarPorId(Long id) {
        return PrestadorResponseDTO.from(encontrar(id));
    }

    public PrestadorResponseDTO atualizar(Long id, PrestadorRequestDTO dto) {
        Prestador prestador = encontrar(id);
        preencher(prestador, dto);
        return PrestadorResponseDTO.from(prestadorRepository.save(prestador));
    }

    public void deletar(Long id) {
        prestadorRepository.delete(encontrar(id));
    }

    private Prestador encontrar(Long id) {
        return prestadorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Prestador não encontrado com o ID: " + id));
    }

    private void preencher(Prestador prestador, PrestadorRequestDTO dto) {
        prestador.setNome(limparTexto(dto.getNome()));
        prestador.setCpf(dto.getCpf().trim());
        prestador.setEspecialidade(dto.getEspecialidade() == null ? null : limparTexto(dto.getEspecialidade()));
        prestador.setTelefone(dto.getTelefone().trim());
    }

    private String limparTexto(String valor) {
        return StringEscapeUtils.escapeHtml4(valor.trim());
    }
}
