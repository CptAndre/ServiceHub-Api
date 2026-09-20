package com.example.servicehub_api.controller;

import com.example.servicehub_api.model.Contrato;
import com.example.servicehub_api.service.ContratoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;

@RestController
@Tag(name = "3. Contratos", description = "Endpoints para gerenciamento de vínculos entre clientes e montadores")
public class ContratoController {

    private final ContratoService contratoService;

    public ContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    // Endpoint normal para criar
    @PostMapping("/api/contratos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar novo contrato de serviço", description = "Gera um novo contrato de montagem vinculando um cliente e um montador através de seus respectivos IDs.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos ou malformados")
    })
    public Contrato criar(@RequestBody Contrato contrato) {
        return contratoService.salvar(contrato);
    }

    // Endpoint aninhado para buscar contratos de um cliente específico
    @GetMapping("/api/clientes/{clienteId}/contratos")
    @Operation(summary = "Listar contratos por cliente", description = "Retorna o histórico de todos os contratos associados a um cliente específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de contratos retornada com sucesso")
    })
    public List<Contrato> listarPorCliente(@PathVariable Long clienteId) {
        return contratoService.buscarPorCliente(clienteId);
    }

    // PATCH para atualizar apenas o status (Atualização parcial)
    @PatchMapping("/api/contratos/{id}")
    @Operation(summary = "Atualizar status do contrato", description = "Modifica apenas a situação atual do contrato (ex: de AGENDADO para CONCLUIDO) sem alterar os demais dados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado para atualização")
    })
    public Contrato atualizarStatus(@PathVariable Long id, @RequestBody Contrato contratoAtualizacao) {
        Contrato contratoExistente = contratoService.buscarPorId(id);
        contratoExistente.setStatus(contratoAtualizacao.getStatus());
        return contratoService.salvar(contratoExistente);
    }
}