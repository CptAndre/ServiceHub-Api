package com.example.servicehub_api.service;

import com.example.servicehub_api.dto.ClienteRequestDTO;
import com.example.servicehub_api.model.Cliente;
import com.example.servicehub_api.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClienteServiceTest {

    private final ClienteRepository repository = mock(ClienteRepository.class);
    private final ClienteService service = new ClienteService(repository);

    @Test
    void escapesFreeTextBeforeSaving() {
        ClienteRequestDTO request = new ClienteRequestDTO();
        request.setNome("<script>alert(1)</script>");
        request.setEmail(" ANA@example.com ");
        request.setTelefone("11999999999");
        request.setDocumento("12345678900");
        when(repository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.salvar(request);

        var saved = org.mockito.ArgumentCaptor.forClass(Cliente.class);
        verify(repository).save(saved.capture());
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", saved.getValue().getNome());
        assertEquals("ana@example.com", saved.getValue().getEmail());
    }
}
