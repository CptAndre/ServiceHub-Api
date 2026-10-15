package com.example.servicehub_api.controller;

import com.example.servicehub_api.dto.PrestadorRequestDTO;
import com.example.servicehub_api.dto.PrestadorResponseDTO;
import com.example.servicehub_api.service.PrestadorService;
import com.example.servicehub_api.util.Pagination;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prestadores")
public class PrestadorController {

    private final PrestadorService prestadorService;

    public PrestadorController(PrestadorService prestadorService) {
        this.prestadorService = prestadorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public PrestadorResponseDTO criar(@Valid @RequestBody PrestadorRequestDTO dto) {
        return prestadorService.salvar(dto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'PROVIDER', 'ADMIN')")
    public Page<PrestadorResponseDTO> listarTodos(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return prestadorService.buscarTodos(Pagination.of(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'PROVIDER', 'ADMIN')")
    public PrestadorResponseDTO buscarPorId(@PathVariable Long id) {
        return prestadorService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public PrestadorResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody PrestadorRequestDTO dto) {
        return prestadorService.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public void deletar(@PathVariable Long id) {
        prestadorService.deletar(id);
    }
}
