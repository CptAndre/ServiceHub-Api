package com.example.servicehub_api.controller;

import com.example.servicehub_api.model.Montador;
import com.example.servicehub_api.service.MontadorService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;

@RestController
@RequestMapping("/api/montadores")
@Tag(name = "2. Montadores", description = "Endpoints para gerenciamento do cadastro de prestadores de serviço")
public class MontadorController {

    private final MontadorService montadorService;

    public MontadorController(MontadorService montadorService) {
        this.montadorService = montadorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar novo montador", description = "Cria um novo registro de montador na plataforma e retorna os dados com o ID gerado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos ou malformados")
    })
    public Montador criar(@RequestBody Montador montador) {
        return montadorService.salvar(montador);
    }

    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de montadores retornada com sucesso")
    })
    @Operation(summary = "Listar todos os montador", description = "Retorna uma lista completa com todos os montadores cadastrados no sistema.")
    public List<Montador> listarTodos() {
        return montadorService.buscarTodos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar montador por ID", description = "Retorna os detalhes de um montador específico a partir do seu identificador único.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado com o ID fornecido")
    })
    public Montador buscarPorId(@PathVariable Long id) {
        return montadorService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do montador", description = "Sobrescreve as informações de um montador existente. Requer o envio de todos os dados do objeto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado para atualização")
    })
    public Montador atualizar(@PathVariable Long id, @RequestBody Montador montadorAtualizado) {
        montadorAtualizado.setId(id);
        return montadorService.salvar(montadorAtualizado);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir montador", description = "Remove o registro do montador do sistema. Esta operação não pode ser desfeita.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Excluído com sucesso (sem conteúdo de retorno)"),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado para exclusão")
    })
    public void deletar(@PathVariable Long id) {
        montadorService.deletar(id);
    }
}