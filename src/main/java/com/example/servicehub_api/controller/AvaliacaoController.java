package com.example.servicehub_api.controller;

import com.example.servicehub_api.model.Avaliacao;
import com.example.servicehub_api.service.AvaliacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;

@RestController
@Tag(name = "4. Avaliações", description = "Endpoints para registro e consulta de notas e avaliações")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @PostMapping("/api/avaliacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar avaliação", description = "Adiciona uma nota (de 1 a 5) e um comentário para um contrato de montagem específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos ou malformados")
    })
    public Avaliacao criar(@RequestBody Avaliacao avaliacao) {
        return avaliacaoService.salvar(avaliacao);
    }

    // Endpoint aninhado documentado no seu diagrama
    @GetMapping("/api/montadores/{montadorId}/avaliacoes")
    @Operation(summary = "Listar avaliações do montador", description = "Retorna o portfólio de notas e comentários que um montador específico recebeu em seus serviços.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de avaliações retornada com sucesso")
    })
    public List<Avaliacao> listarPorMontador(@PathVariable Long montadorId) {
        return avaliacaoService.buscarPorMontador(montadorId);
    }
}