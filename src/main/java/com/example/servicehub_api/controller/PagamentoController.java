package com.example.servicehub_api.controller;

import com.example.servicehub_api.dto.PagamentoRequestDTO;
import com.example.servicehub_api.dto.PagamentoResponseDTO;
import com.example.servicehub_api.service.PagamentoService;
import com.example.servicehub_api.util.Pagination;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping("/api/pagamentos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public PagamentoResponseDTO criar(@Valid @RequestBody PagamentoRequestDTO dto) {
        return pagamentoService.salvar(dto);
    }

    @GetMapping("/api/contratos/{contratoId}/pagamentos")
    @PreAuthorize("hasAnyRole('CLIENT', 'PROVIDER', 'ADMIN')")
    public Page<PagamentoResponseDTO> listarPorContrato(@PathVariable Long contratoId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return pagamentoService.buscarPorContrato(contratoId, Pagination.of(page, size));
    }
}
