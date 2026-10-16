package com.umc.pediupatrao.security;

import com.umc.pediupatrao.entity.Perfil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Pega o usuário que está logado e o perfil dele
@Component
public class UsuarioLogado {

    public String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "sistema";
        }
        return auth.getName();
    }

    // Retorna o perfil (ADMIN, GERENTE ou ATENDENTE) ou null se não tiver
    public Perfil perfil() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        for (GrantedAuthority autoridade : auth.getAuthorities()) {
            String nome = autoridade.getAuthority().replace("ROLE_", "");
            Perfil perfil = Perfil.de(nome);
            if (perfil != null) {
                return perfil;
            }
        }
        return null;
    }

    public String perfilTexto() {
        Perfil perfil = perfil();
        return perfil == null ? "SISTEMA" : perfil.name();
    }
}
