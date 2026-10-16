package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // No login, o perfil gravado no banco vira a role do Spring Security (ex.: ROLE_GERENTE).
    // Usuário sem perfil válido entra, mas sem nenhuma permissão.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        Perfil perfil = Perfil.de(usuario.getRole());
        String[] roles = perfil == null ? new String[0] : new String[]{perfil.name()};

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(roles)
                .build();
    }
}
