package com.example.servicehub_api.service;

import com.example.servicehub_api.dto.ContratoRequestDTO;
import com.example.servicehub_api.dto.ContratoResponseDTO;
import com.example.servicehub_api.dto.ContratoStatusRequestDTO;
import com.example.servicehub_api.model.Contrato;
import com.example.servicehub_api.model.Servico;
import com.example.servicehub_api.repository.ClienteRepository;
import com.example.servicehub_api.repository.ContratoRepository;
import com.example.servicehub_api.repository.PrestadorRepository;
import com.example.servicehub_api.repository.ServicoRepository;
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
    private final ServicoRepository servicoRepository;

    public ContratoService(ContratoRepository contratoRepository, ClienteRepository clienteRepository,
                           PrestadorRepository prestadorRepository, ServicoRepository servicoRepository) {
        this.contratoRepository = contratoRepository;
        this.clienteRepository = clienteRepository;
        this.prestadorRepository = prestadorRepository;
        this.servicoRepository = servicoRepository;
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

    public ContratoResponseDTO bookService(Long clienteId, Long servicoId) {
        var cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviço não encontrado"));

        Contrato contrato = new Contrato();
        contrato.setCliente(cliente);
        contrato.setPrestador(servico.getPrestador());
        contrato.setServico(servico);
        String descricao = servico.getDescricao() == null || servico.getDescricao().isBlank()
                ? servico.getTitulo()
                : servico.getTitulo() + ": " + servico.getDescricao();
        if (descricao.length() > 2000) {
            descricao = descricao.substring(0, 2000);
        }
        contrato.setDescricaoServico(StringEscapeUtils.escapeHtml4(descricao));
        contrato.setValorTotal(servico.getPreco());
        contrato.setStatus("AGENDADO");
        return ContratoResponseDTO.from(contratoRepository.save(contrato));
    }

    public Page<ContratoResponseDTO> buscarPorCliente(Long clienteId, Pageable pageable) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado");
        }
        return contratoRepository.findByClienteId(clienteId, pageable).map(ContratoResponseDTO::from);
    }

    public Page<ContratoResponseDTO> buscarPorPrestador(Long prestadorId, Pageable pageable) {
        if (!prestadorRepository.existsById(prestadorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestador não encontrado");
        }
        return contratoRepository.findByPrestadorId(prestadorId, pageable).map(ContratoResponseDTO::from);
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
