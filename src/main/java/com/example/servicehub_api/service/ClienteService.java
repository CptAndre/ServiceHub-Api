package com.example.servicehub_api.service;

import com.example.servicehub_api.dto.ClienteRequestDTO;
import com.example.servicehub_api.dto.ClienteResponseDTO;
import com.example.servicehub_api.model.Cliente;
import com.example.servicehub_api.repository.ClienteRepository;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    // Injeção de dependência via construtor
    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    // CREATE / UPDATE
    public ClienteResponseDTO salvar(ClienteRequestDTO dto) {
        Cliente cliente = new Cliente();
        cliente.setNome(limparTexto(dto.getNome()));
        cliente.setDocumento(normalizar(dto.getDocumento()));
        cliente.setEmail(dto.getEmail().trim().toLowerCase());
        cliente.setTelefone(dto.getTelefone().trim());
        return ClienteResponseDTO.from(clienteRepository.save(cliente));
    }

    public Page<ClienteResponseDTO> buscarTodos(Pageable pageable) {
        return clienteRepository.findAll(pageable).map(ClienteResponseDTO::from);
    }

    public ClienteResponseDTO buscarPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cliente não encontrado com o ID: " + id));
        return ClienteResponseDTO.from(cliente);
    }

    public void deletar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cliente não encontrado com o ID: " + id));
        clienteRepository.delete(cliente);
    }

    public ClienteResponseDTO atualizar(Long id, ClienteRequestDTO dto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cliente não encontrado com o ID: " + id));
        cliente.setNome(limparTexto(dto.getNome()));
        cliente.setDocumento(normalizar(dto.getDocumento()));
        cliente.setEmail(dto.getEmail().trim().toLowerCase());
        cliente.setTelefone(dto.getTelefone().trim());
        return ClienteResponseDTO.from(clienteRepository.save(cliente));
    }

    private String limparTexto(String valor) {
        return StringEscapeUtils.escapeHtml4(valor.trim());
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }
}