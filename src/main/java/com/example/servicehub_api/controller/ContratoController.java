package com.example.servicehub_api.controller;

import com.example.servicehub_api.dto.ContratoRequestDTO;
import com.example.servicehub_api.dto.ContratoResponseDTO;
import com.example.servicehub_api.dto.ContratoStatusRequestDTO;
import com.example.servicehub_api.service.ContratoService;
import com.example.servicehub_api.util.Pagination;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class ContratoController {

    private final ContratoService contratoService;

    public ContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    @PostMapping("/api/contratos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ContratoResponseDTO criar(@Valid @RequestBody ContratoRequestDTO dto) {
        return contratoService.salvar(dto);
    }

    @GetMapping("/api/clientes/{clienteId}/contratos")
    @PreAuthorize("isAuthenticated()")
    public Page<ContratoResponseDTO> listarPorCliente(@PathVariable Long clienteId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return contratoService.buscarPorCliente(clienteId, Pagination.of(page, size));
    }

    @PatchMapping("/api/contratos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ContratoResponseDTO atualizarStatus(@PathVariable Long id,
                                                @Valid @RequestBody ContratoStatusRequestDTO dto) {
        return contratoService.atualizarStatus(id, dto);
    }
}
