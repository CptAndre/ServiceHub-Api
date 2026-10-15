package com.example.servicehub_api.controller;

import com.example.servicehub_api.dto.ServicoRequestDTO;
import com.example.servicehub_api.dto.ServicoResponseDTO;
import com.example.servicehub_api.service.ServicoService;
import com.example.servicehub_api.util.Pagination;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class ServicoController {

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @PostMapping("/api/servicos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public ServicoResponseDTO criar(@Valid @RequestBody ServicoRequestDTO request) {
        return servicoService.criar(request);
    }

    @GetMapping("/api/prestadores/{prestadorId}/servicos")
    @PreAuthorize("hasAnyRole('CLIENT', 'PROVIDER', 'ADMIN')")
    public Page<ServicoResponseDTO> listarPorPrestador(@PathVariable Long prestadorId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return servicoService.listarPorPrestador(prestadorId, Pagination.of(page, size));
    }
}
