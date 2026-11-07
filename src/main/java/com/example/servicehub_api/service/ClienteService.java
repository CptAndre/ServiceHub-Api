package com.example.servicehub_api.service;
import com.example.servicehub_api.dto.*;
import com.example.servicehub_api.model.UserRole;
import com.example.servicehub_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional
public class ClienteService {
    private final UsuarioRepository usuarios;
    private final AccountService accounts;
    private final ResourceAccess access;
    public ClienteResponseDTO salvar(ClienteRequestDTO dto) { return ClienteResponseDTO.from(accounts.criar(dto, UserRole.CLIENT)); }
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> buscarTodos(Pageable pageable) {
        var current = access.currentUser();
        if (current.getPapel() != UserRole.ADMIN) {
            var own = ClienteResponseDTO.from(accounts.encontrar(current.getId(), UserRole.CLIENT));
            return new PageImpl<>(pageable.getPageNumber() == 0 ? List.of(own) : List.of(), pageable, 1);
        }
        return usuarios.findByPapelAndAtivoTrue(UserRole.CLIENT, pageable).map(ClienteResponseDTO::from);
    }
    public ClienteResponseDTO buscarPorId(Long id) { access.requireOwner(id); return ClienteResponseDTO.from(accounts.encontrar(id, UserRole.CLIENT)); }
    public ClienteResponseDTO atualizar(Long id, ClienteRequestDTO dto) { access.requireOwner(id); return ClienteResponseDTO.from(accounts.atualizar(id, dto, UserRole.CLIENT)); }
    public void deletar(Long id) { access.requireOwner(id); accounts.desativar(id, UserRole.CLIENT); }
}
