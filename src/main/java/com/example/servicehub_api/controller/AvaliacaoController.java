package com.example.servicehub_api.controller;

import com.example.servicehub_api.dto.AvaliacaoRequestDTO;
import com.example.servicehub_api.dto.AvaliacaoResponseDTO;
import com.example.servicehub_api.service.AvaliacaoService;
import com.example.servicehub_api.util.Pagination;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @PostMapping({"/api/v1/avaliacoes", "/api/avaliacoes"})
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public AvaliacaoResponseDTO criar(@Valid @RequestBody AvaliacaoRequestDTO dto) {
        return avaliacaoService.salvar(dto);
    }

    @GetMapping({"/api/v1/prestadores/{prestadorId}/avaliacoes", "/api/prestadores/{prestadorId}/avaliacoes"})
    @PreAuthorize("hasAnyRole('CLIENT', 'PROVIDER', 'ADMIN')")
    public Page<AvaliacaoResponseDTO> listarPorPrestador(@PathVariable Long prestadorId,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return avaliacaoService.buscarPorPrestador(prestadorId, Pagination.of(page, size));
    }
}
