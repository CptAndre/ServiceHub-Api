package com.example.servicehub_api.service;

import com.example.servicehub_api.dto.ContratoRequestDTO;
import com.example.servicehub_api.dto.ContratoResponseDTO;
import com.example.servicehub_api.dto.ContratoStatusRequestDTO;
import com.example.servicehub_api.model.Contrato;
import com.example.servicehub_api.repository.ClienteRepository;
import com.example.servicehub_api.repository.ContratoRepository;
import com.example.servicehub_api.repository.PrestadorRepository;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final ClienteRepository clienteRepository;
    private final PrestadorRepository prestadorRepository;

    public ContratoService(ContratoRepository contratoRepository, ClienteRepository clienteRepository,
                           PrestadorRepository prestadorRepository) {
        this.contratoRepository = contratoRepository;
        this.clienteRepository = clienteRepository;
        this.prestadorRepository = prestadorRepository;
    }

    public ContratoResponseDTO salvar(ContratoRequestDTO dto) {
        Contrato contrato = new Contrato();
        contrato.setCliente(clienteRepository.findById(dto.getClienteId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado")));
        contrato.setPrestador(prestadorRepository.findById(dto.getPrestadorId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestador não encontrado")));
        contrato.setDescricaoServico(StringEscapeUtils.escapeHtml4(dto.getDescricaoServico().trim()));
        contrato.setValorTotal(dto.getValorTotal());
        contrato.setStatus("AGENDADO");
        return ContratoResponseDTO.from(contratoRepository.save(contrato));
    }

    public Page<ContratoResponseDTO> buscarPorCliente(Long clienteId, Pageable pageable) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado");
        }
        return contratoRepository.findByClienteId(clienteId, pageable).map(ContratoResponseDTO::from);
    }

    public ContratoResponseDTO atualizarStatus(Long id, ContratoStatusRequestDTO dto) {
        Contrato contrato = buscarEntidade(id);
        contrato.setStatus(dto.getStatus().trim());
        return ContratoResponseDTO.from(contratoRepository.save(contrato));
    }

    private Contrato buscarEntidade(Long id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contrato não encontrado com o ID: " + id));
    }
}
