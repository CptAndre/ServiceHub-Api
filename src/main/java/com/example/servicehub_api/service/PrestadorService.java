package com.example.servicehub_api.service;
import com.example.servicehub_api.dto.*;
import com.example.servicehub_api.model.UserRole;
import com.example.servicehub_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional
public class PrestadorService {
    private final UsuarioRepository usuarios;
    private final AccountService accounts;
    private final ResourceAccess access;
    public PrestadorResponseDTO salvar(PrestadorRequestDTO dto) {
        if (dto.getEspecialidade() != null) dto.setBio(dto.getEspecialidade());
        return PrestadorResponseDTO.from(accounts.criar(dto, UserRole.PROVIDER));
    }
    @Transactional(readOnly = true)
    public Page<PrestadorResponseDTO> buscarTodos(Pageable pageable) { return usuarios.findByPapelAndAtivoTrue(UserRole.PROVIDER, pageable).map(PrestadorResponseDTO::from); }
    public PrestadorResponseDTO buscarPorId(Long id) { return PrestadorResponseDTO.from(accounts.encontrar(id, UserRole.PROVIDER)); }
    public PrestadorResponseDTO atualizar(Long id, PrestadorRequestDTO dto) {
        access.requireOwner(id);
        if (dto.getEspecialidade() != null) dto.setBio(dto.getEspecialidade());
        return PrestadorResponseDTO.from(accounts.atualizar(id, dto, UserRole.PROVIDER));
    }
    public void deletar(Long id) { access.requireOwner(id); accounts.desativar(id, UserRole.PROVIDER); }
}
