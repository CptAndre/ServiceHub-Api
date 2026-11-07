package com.example.servicehub_api.service;
import com.example.servicehub_api.dto.ClienteRequestDTO;
import com.example.servicehub_api.model.*;
import com.example.servicehub_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
@Service @RequiredArgsConstructor @Transactional
public class AccountService {
    public static final List<RequestStatus> ACTIVE_REQUESTS = List.of(RequestStatus.PENDING, RequestStatus.ACCEPTED, RequestStatus.IN_PROGRESS);
    private final UsuarioRepository usuarios;
    private final ContratoRepository contratos;
    private final PasswordEncoder passwordEncoder;
    private final ResourceAccess access;
    public Usuario criar(ClienteRequestDTO dto, UserRole role) {
        Usuario user = new Usuario();
        user.setPapel(role);
        preencher(user, dto, true);
        return usuarios.save(user);
    }
    public Usuario encontrar(Long id, UserRole role) {
        return usuarios.findById(id).filter(u -> u.getPapel() == role)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }
    public Usuario atualizar(Long id, ClienteRequestDTO dto, UserRole role) {
        Usuario user = encontrar(id, role);
        if (dto.getAtivo() != null && dto.getAtivo() != user.isAtivo()) {
            if (access.currentUser().getPapel() != UserRole.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente ADMIN altera a ativação de contas");
            if (!dto.getAtivo()) desativar(id, role);
            user.setAtivo(dto.getAtivo());
        }
        preencher(user, dto, false);
        return usuarios.save(user);
    }
    public void desativar(Long id, UserRole role) {
        Usuario user = encontrar(id, role);
        boolean hasActive = role == UserRole.PROVIDER
                ? contratos.existsByServicoPrestadorIdAndStatusIn(id, ACTIVE_REQUESTS)
                : contratos.existsByClienteIdAndStatusIn(id, ACTIVE_REQUESTS);
        if (hasActive) throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário possui ordens ativas");
        user.setAtivo(false);
        usuarios.save(user);
    }
    private void preencher(Usuario user, ClienteRequestDTO dto, boolean creating) {
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        usuarios.findByEmail(email).filter(existing -> !existing.getId().equals(user.getId())).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        });
        String password = dto.getPassword();
        if (creating && (password == null || password.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha obrigatória no cadastro");
        }
        if (password != null) {
            int bytes = password.getBytes(StandardCharsets.UTF_8).length;
            if (password.length() < 12 || bytes > 72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha deve ter ao menos 12 caracteres e até 72 bytes");
            user.setPasswordHash(passwordEncoder.encode(password));
        }
        user.setNome(dto.getNome().trim());
        user.setEmail(email);
        user.setTelefone(dto.getTelefone() == null ? null : dto.getTelefone().trim());
        user.setBio(dto.getBio() == null ? null : dto.getBio().trim());
        user.setAvatarUrl(dto.getAvatarUrl() == null ? null : dto.getAvatarUrl().trim());
    }
}
