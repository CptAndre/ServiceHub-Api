package com.example.servicehub_api.controller;

import com.example.servicehub_api.model.Pagamento;
import com.example.servicehub_api.service.PagamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;

@RestController
@Tag(name = "5. Pagamentos", description = "Endpoints para registro de transações financeiras")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping("/api/pagamentos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar pagamento", description = "Registra um novo pagamento vinculado a um contrato de serviço.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pagamento registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados do pagamento inválidos ou contrato inexistente")
    })
    public Pagamento criar(@RequestBody Pagamento pagamento) {
        return pagamentoService.salvar(pagamento);
    }

    // Endpoint aninhado documentado no seu diagrama
    @GetMapping("/api/contratos/{contratoId}/pagamentos")
    @Operation(summary = "Listar pagamentos do contrato", description = "Retorna todo o histórico de pagamentos atrelados a um contrato específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pagamentos retornada com sucesso")
    })
    public List<Pagamento> listarPorContrato(@PathVariable Long contratoId) {
        return pagamentoService.buscarPorContrato(contratoId);
    }
}