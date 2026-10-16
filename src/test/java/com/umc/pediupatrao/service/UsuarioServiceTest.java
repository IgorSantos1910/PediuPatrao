package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UsuarioServiceTest {

    private UsuarioRepository usuarioRepository;
    private AuditoriaService auditoriaService;
    private UsuarioLogado usuarioLogado;
    private UsuarioService service;

    private Usuario admin;
    private Usuario atendente;

    @BeforeEach
    void configurar() {
        usuarioRepository = mock(UsuarioRepository.class);
        auditoriaService = mock(AuditoriaService.class);
        usuarioLogado = mock(UsuarioLogado.class);
        service = new UsuarioService(usuarioRepository, new BCryptPasswordEncoder(4), auditoriaService, usuarioLogado);

        admin = usuario("u1", "admin", "ADMIN");
        atendente = usuario("u2", "atendente01", "ATENDENTE");
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById("u2")).thenReturn(Optional.of(atendente));
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findAll()).thenReturn(List.of(admin, atendente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioLogado.username()).thenReturn("admin");
    }

    private static Usuario usuario(String id, String username, String role) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setRole(role);
        u.setPassword("hash");
        return u;
    }

    private static UsuarioRequest request(String username, String role, String senha) {
        UsuarioRequest r = new UsuarioRequest();
        r.setUsername(username);
        r.setRole(role);
        r.setPassword(senha);
        return r;
    }

    @Test
    void adminNaoPodeAlterarOProprioPerfil() {
        assertThatThrownBy(() -> service.atualizarUsuario("u1", request("admin", "GERENTE", null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("próprio perfil");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoPodeRebaixarOUltimoAdmin() {
        when(usuarioLogado.username()).thenReturn("outro");
        assertThatThrownBy(() -> service.atualizarUsuario("u1", request("admin", "ATENDENTE", null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("último administrador");
    }

    @Test
    void naoPodeExcluirASiMesmo() {
        assertThatThrownBy(() -> service.deletarUsuario("u1"))
                .isInstanceOf(RegraNegocioException.class);
        verify(usuarioRepository, never()).deleteById(anyString());
    }

    @Test
    void perfilInexistenteEhRecusado() {
        assertThatThrownBy(() -> service.criarUsuario(request("novo", "SUPERADMIN", "123456")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Perfil inválido");
    }

    @Test
    void trocaDePerfilEAuditadaSemGravarASenha() {
        service.atualizarUsuario("u2", request("atendente01", "GERENTE", "novaSenha123"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CampoAlterado>> captor = ArgumentCaptor.forClass(List.class);
        verify(auditoriaService).registrar(eq(TipoOperacao.ALTERACAO_USUARIO), eq("Usuario"), eq("u2"),
                anyString(), captor.capture(), isNull());

        List<CampoAlterado> alteracoes = captor.getValue();
        assertThat(alteracoes).anySatisfy(c -> {
            assertThat(c.getCampo()).isEqualTo("perfil");
            assertThat(c.getValorAnterior()).isEqualTo("ATENDENTE");
            assertThat(c.getValorNovo()).isEqualTo("GERENTE");
        });
        assertThat(alteracoes).anySatisfy(c -> {
            assertThat(c.getCampo()).isEqualTo("senha");
            assertThat(c.getValorNovo()).isEqualTo("[protegido]");
        });
        assertThat(alteracoes).noneSatisfy(c -> assertThat(c.getValorNovo()).isEqualTo("novaSenha123"));
    }
}
