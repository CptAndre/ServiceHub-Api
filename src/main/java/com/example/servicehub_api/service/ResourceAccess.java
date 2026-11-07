package com.example.servicehub_api.service;
import com.example.servicehub_api.model.*;
import com.example.servicehub_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service @RequiredArgsConstructor
public class ResourceAccess {
    private final UsuarioRepository usuarios;
    public Usuario currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária");
        }
        return usuarios.findByEmail(authentication.getName()).filter(Usuario::isAtivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário inativo ou inexistente"));
    }
    public void requireOwner(Long userId) {
        Usuario current = currentUser();
        if (current.getPapel() != UserRole.ADMIN && !current.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O recurso pertence a outro usuário");
        }
    }
    public void requireRequestAccess(Contrato request) {
        Usuario current = currentUser();
        if (current.getPapel() != UserRole.ADMIN && !current.getId().equals(request.getCliente().getId())
                && !current.getId().equals(request.getPrestador().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O recurso pertence a outro usuário");
        }
    }
}
