package com.example.servicehub_api.controller;

import com.example.servicehub_api.dto.ContratoRequestDTO;
import com.example.servicehub_api.dto.ContratoResponseDTO;
import com.example.servicehub_api.dto.ContratoStatusRequestDTO;
import com.example.servicehub_api.dto.BookServiceRestRequestDTO;
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
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ContratoResponseDTO criar(@Valid @RequestBody ContratoRequestDTO dto) {
        return contratoService.salvar(dto);
    }

    @PostMapping("/api/contratos/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ContratoResponseDTO bookService(@Valid @RequestBody BookServiceRestRequestDTO request) {
        return contratoService.bookService(request.getClientId(), request.getServiceId());
    }

    @GetMapping("/api/clientes/{clienteId}/contratos")
    @PreAuthorize("hasAnyRole('CLIENT', 'PROVIDER', 'ADMIN')")
    public Page<ContratoResponseDTO> listarPorCliente(@PathVariable Long clienteId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return contratoService.buscarPorCliente(clienteId, Pagination.of(page, size));
    }

    @GetMapping("/api/prestadores/{prestadorId}/contratos")
    @PreAuthorize("hasAnyRole('CLIENT', 'PROVIDER', 'ADMIN')")
    public Page<ContratoResponseDTO> listarPorPrestador(@PathVariable Long prestadorId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return contratoService.buscarPorPrestador(prestadorId, Pagination.of(page, size));
    }

    @PatchMapping("/api/contratos/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public ContratoResponseDTO atualizarStatus(@PathVariable Long id,
                                                @Valid @RequestBody ContratoStatusRequestDTO dto) {
        return contratoService.atualizarStatus(id, dto);
    }
}
