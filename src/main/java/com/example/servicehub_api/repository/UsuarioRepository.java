package com.example.servicehub_api.repository;
import com.example.servicehub_api.model.Usuario;
import com.example.servicehub_api.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByPapel(UserRole papel);
    Page<Usuario> findByPapelAndAtivoTrue(UserRole papel, Pageable pageable);
    Page<Usuario> findByPapelAndAtivoTrueAndBioContainingIgnoreCase(UserRole papel, String bio, Pageable pageable);
}
