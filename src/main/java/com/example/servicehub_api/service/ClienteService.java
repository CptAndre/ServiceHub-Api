package com.example.servicehub_api.service;

import com.example.servicehub_api.model.Cliente;
import com.example.servicehub_api.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import com.example.servicehub_api.exception.ResourceNotFoundException;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    // Injeção de dependência via construtor
    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    // CREATE / UPDATE
    public Cliente salvar(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    // READ (Todos)
    public List<Cliente> buscarTodos() {
        return clienteRepository.findAll();
    }

    // READ (Por ID)
    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com o ID: " + id));
    }

    // DELETE
    public void deletar(Long id) {
        Cliente cliente = buscarPorId(id);
        clienteRepository.delete(cliente);
    }


}