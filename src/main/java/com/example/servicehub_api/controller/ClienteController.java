package com.example.servicehub_api.controller;

import com.example.servicehub_api.model.Cliente;
import com.example.servicehub_api.service.ClienteService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "1. Clientes", description = "Endpoints para gerenciamento de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar novo cliente", description = "Cria um cliente e retorna os dados com o ID gerado no banco.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos ou malformados")
    })
    public Cliente criar(@RequestBody Cliente cliente) {
        return clienteService.salvar(cliente);
    }

    @GetMapping
    @Operation(summary = "Listar todos os clientes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de clientes retornada com sucesso")
    })
    public List<Cliente> listarTodos() {
        return clienteService.buscarTodos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID", description = "Retorna os detalhes de um cliente específico a partir do seu identificador único.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado com o ID fornecido")
    })
    public Cliente buscarPorId(@PathVariable Long id) {
        return clienteService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do cliente", description = "Sobrescreve as informações de um cliente existente. Requer o envio de todos os dados do objeto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado para atualização")
    })
    public Cliente atualizar(@PathVariable Long id, @RequestBody Cliente clienteAtualizado) {
        // Garante que estamos atualizando o cliente correto
        clienteAtualizado.setId(id);
        return clienteService.salvar(clienteAtualizado);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir cliente", description = "Remove o registro do cliente do sistema. Esta operação não pode ser desfeita.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Excluído com sucesso (sem conteúdo de retorno)"),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado para exclusão")
    })
    public void deletar(@PathVariable Long id) {
        clienteService.deletar(id);
    }
}