package com.example.servicehub_api.config;
import com.example.servicehub_api.dto.ClienteRequestDTO;
import com.example.servicehub_api.model.UserRole;
import com.example.servicehub_api.repository.UsuarioRepository;
import com.example.servicehub_api.service.AccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.jspecify.annotations.NonNull;
import java.util.Locale;
@Component
public class AdminBootstrap implements ApplicationRunner {
    private final String email;
    private final String password;
    private final UsuarioRepository usuarios;
    private final AccountService accounts;
    public AdminBootstrap(@Value("${app.bootstrap-admin.email:}") String email,
                          @Value("${app.bootstrap-admin.password:}") String password,
                          UsuarioRepository usuarios, AccountService accounts) {
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
        this.usuarios = usuarios;
        this.accounts = accounts;
    }
    @Override public void run(@NonNull ApplicationArguments args) {
        if (email.isBlank()) return;
        var existing = usuarios.findByEmail(email);
        if (existing.isPresent()) {
            if (existing.get().getPapel() != UserRole.ADMIN) throw new IllegalStateException("E-mail de bootstrap já pertence a outro perfil");
            return;
        }
        if (usuarios.existsByPapel(UserRole.ADMIN)) return;
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalStateException("E-mail do administrador inválido");
        ClienteRequestDTO request = new ClienteRequestDTO();
        request.setNome("Administrador");
        request.setEmail(email);
        request.setPassword(password);
        accounts.criar(request, UserRole.ADMIN);
    }
}
