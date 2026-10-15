package com.example.servicehub_api.graphql;

import com.example.servicehub_api.dto.ContratoResponseDTO;
import com.example.servicehub_api.dto.PrestadorResponseDTO;
import com.example.servicehub_api.dto.ServicoRequestDTO;
import com.example.servicehub_api.dto.ServicoResponseDTO;
import com.example.servicehub_api.model.Prestador;
import com.example.servicehub_api.repository.PrestadorRepository;
import com.example.servicehub_api.service.ContratoService;
import com.example.servicehub_api.service.ServicoService;
import com.example.servicehub_api.util.Pagination;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Controller
public class ServiceHubGraphQlController {

    private final PrestadorRepository prestadorRepository;
    private final ContratoService contratoService;
    private final ServicoService servicoService;
    private final Validator validator;

    public ServiceHubGraphQlController(PrestadorRepository prestadorRepository,
                                       ContratoService contratoService,
                                       ServicoService servicoService,
                                       Validator validator) {
        this.prestadorRepository = prestadorRepository;
        this.contratoService = contratoService;
        this.servicoService = servicoService;
        this.validator = validator;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'PROVIDER', 'ADMIN')")
    public GraphQlProvider provider(@Argument Long id) {
        Prestador prestador = prestadorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Prestador não encontrado com o ID: " + id));
        return GraphQlProvider.from(PrestadorResponseDTO.from(prestador));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'PROVIDER', 'ADMIN')")
    public List<GraphQlProvider> providers(@Argument String specialty,
                                          @Argument Integer page,
                                          @Argument Integer size) {
        Pageable pageable = Pagination.of(page == null ? 0 : page, size == null ? 20 : size);
        Page<Prestador> results = specialty == null || specialty.isBlank()
                ? prestadorRepository.findAll(pageable)
                : prestadorRepository.findByEspecialidadeContainingIgnoreCase(specialty.trim(), pageable);
        return results.getContent().stream()
                .map(PrestadorResponseDTO::from)
                .map(GraphQlProvider::from)
                .toList();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'PROVIDER', 'ADMIN')")
    public List<GraphQlService> servicesByProvider(@Argument Long providerId,
                                                   @Argument Integer page,
                                                   @Argument Integer size) {
        Pageable pageable = Pagination.of(page == null ? 0 : page, size == null ? 20 : size);
        return servicoService.listarPorPrestador(providerId, pageable).getContent().stream()
                .map(GraphQlService::from)
                .toList();
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public GraphQlService createService(@Argument("input") CreateServiceInput input) {
        ServicoRequestDTO request = new ServicoRequestDTO();
        request.setPrestadorId(input.providerId());
        request.setTitulo(input.title());
        request.setDescricao(input.description());
        request.setPreco(input.price());

        Set<ConstraintViolation<ServicoRequestDTO>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return GraphQlService.from(servicoService.criar(request));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public GraphQlBooking bookService(@Argument("input") BookServiceInput input) {
        Set<ConstraintViolation<BookServiceInput>> violations = validator.validate(input);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return GraphQlBooking.from(contratoService.bookService(input.clientId(), input.serviceId()));
    }

    @SchemaMapping(typeName = "Provider", field = "services")
    @PreAuthorize("hasAnyRole('CLIENT', 'PROVIDER', 'ADMIN')")
    public List<GraphQlService> providerServices(GraphQlProvider provider,
                                                 @Argument Integer page,
                                                 @Argument Integer size) {
        Pageable pageable = Pagination.of(page == null ? 0 : page, size == null ? 20 : size);
        return servicoService.listarPorPrestador(provider.getId(), pageable).getContent().stream()
                .map(GraphQlService::from)
                .toList();
    }

    public record CreateServiceInput(Long providerId, String title, String description, BigDecimal price) {
    }

    public record BookServiceInput(@NotNull @Positive Long clientId,
                                   @NotNull @Positive Long serviceId) {
    }

    @Getter
    @RequiredArgsConstructor
    public static class GraphQlProvider {
        private final Long id;
        private final String name;
        private final String specialty;
        private final String phone;

        static GraphQlProvider from(PrestadorResponseDTO prestador) {
            return new GraphQlProvider(prestador.getId(), prestador.getNome(),
                    prestador.getEspecialidade(), prestador.getTelefone());
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class GraphQlBooking {
        private final Long id;
        private final String description;
        private final BigDecimal totalValue;
        private final String status;
        private final Long clientId;
        private final Long providerId;
        private final Long serviceId;

        static GraphQlBooking from(ContratoResponseDTO contrato) {
            return new GraphQlBooking(contrato.getId(), contrato.getDescricaoServico(), contrato.getValorTotal(),
                    contrato.getStatus(), contrato.getClienteId(), contrato.getPrestadorId(), contrato.getServicoId());
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class GraphQlService {
        private final Long id;
        private final String title;
        private final String description;
        private final BigDecimal price;
        private final Long providerId;

        static GraphQlService from(ServicoResponseDTO servico) {
            return new GraphQlService(servico.getId(), servico.getTitulo(), servico.getDescricao(),
                    servico.getPreco(), servico.getPrestadorId());
        }
    }
}
