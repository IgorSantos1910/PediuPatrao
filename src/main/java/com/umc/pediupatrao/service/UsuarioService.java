package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Administração de usuários e perfis (somente ADMIN)
@Service
public class UsuarioService {

    public static final String ENTIDADE = "Usuario";
    private static final int TAMANHO_MINIMO_SENHA = 6;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final UsuarioLogado usuarioLogado;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            AuditoriaService auditoriaService, UsuarioLogado usuarioLogado) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.usuarioLogado = usuarioLogado;
    }

    // Busca um usuário por ID
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Lista todos os usuários
    @PreAuthorize("hasRole('ADMIN')")
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Cria um novo usuário (criptografando a senha)
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario criarUsuario(UsuarioRequest dados) {
        String username = validarUsername(dados.getUsername());
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new RegraNegocioException("Já existe um usuário com o login " + username + ".");
        }
        Perfil perfil = validarPerfil(dados.getRole());
        validarSenha(dados.getPassword());

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setRole(perfil.name());
        usuario.setPassword(passwordEncoder.encode(dados.getPassword()));
        Usuario salvo = usuarioRepository.save(usuario);

        auditoriaService.registrar(TipoOperacao.INCLUSAO_USUARIO, ENTIDADE, salvo.getId(),
                "Usuário criado: " + username,
                List.of(new CampoAlterado("username", null, username),
                        new CampoAlterado("perfil", null, perfil.name())),
                null);
        return salvo;
    }

    // Atualiza login, perfil e (opcionalmente) a senha
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario atualizarUsuario(String id, UsuarioRequest dados) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        String username = validarUsername(dados.getUsername());
        Perfil novoPerfil = validarPerfil(dados.getRole());
        Perfil perfilAtual = existente.getPerfil();

        Usuario outro = usuarioRepository.findByUsername(username).orElse(null);
        if (outro != null && !outro.getId().equals(id)) {
            throw new RegraNegocioException("Já existe um usuário com o login " + username + ".");
        }

        boolean ehOProprio = existente.getUsername().equals(usuarioLogado.username());
        if (ehOProprio && novoPerfil != perfilAtual) {
            throw new RegraNegocioException("Você não pode alterar o seu próprio perfil.");
        }
        if (ehOProprio && !username.equals(existente.getUsername())) {
            throw new RegraNegocioException("Você não pode alterar o seu próprio login.");
        }
        if (perfilAtual == Perfil.ADMIN && novoPerfil != Perfil.ADMIN && contarAdmins() <= 1) {
            throw new RegraNegocioException("Não é possível rebaixar o último administrador do sistema.");
        }

        Map<String, Object> antes = retrato(existente);
        existente.setUsername(username);
        existente.setRole(novoPerfil.name());
        List<CampoAlterado> alteracoes = new ArrayList<>(AuditoriaService.comparar(antes, retrato(existente)));

        // Só atualiza a senha se uma nova foi informada; a auditoria registra apenas que houve troca
        if (dados.getPassword() != null && !dados.getPassword().isBlank()) {
            validarSenha(dados.getPassword());
            existente.setPassword(passwordEncoder.encode(dados.getPassword()));
            alteracoes.add(new CampoAlterado("senha", "[protegido]", "[protegido]"));
        }
        if (alteracoes.isEmpty()) {
            return existente;
        }
        Usuario salvo = usuarioRepository.save(existente);
        auditoriaService.registrar(TipoOperacao.ALTERACAO_USUARIO, ENTIDADE, salvo.getId(),
                "Usuário alterado: " + salvo.getUsername(), alteracoes, null);
        return salvo;
    }

    // Deleta um usuário pelo ID
    @PreAuthorize("hasRole('ADMIN')")
    public void deletarUsuario(String id) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        if (existente.getUsername().equals(usuarioLogado.username())) {
            throw new RegraNegocioException("Você não pode excluir o seu próprio usuário.");
        }
        if (existente.getPerfil() == Perfil.ADMIN && contarAdmins() <= 1) {
            throw new RegraNegocioException("Não é possível excluir o último administrador do sistema.");
        }
        usuarioRepository.deleteById(id);
        auditoriaService.registrar(TipoOperacao.EXCLUSAO_USUARIO, ENTIDADE, id,
                "Usuário excluído: " + existente.getUsername(),
                AuditoriaService.comparar(retrato(existente), Map.of()), null);
    }

    private long contarAdmins() {
        long total = 0;
        for (Usuario u : usuarioRepository.findAll()) {
            if (u.getPerfil() == Perfil.ADMIN) {
                total++;
            }
        }
        return total;
    }

    private static String validarUsername(String username) {
        if (username == null || !username.trim().matches("[A-Za-z0-9._-]{3,30}")) {
            throw new RegraNegocioException("Login inválido: use de 3 a 30 letras, números, ponto, hífen ou sublinhado.");
        }
        return username.trim();
    }

    private static Perfil validarPerfil(String role) {
        Perfil perfil = Perfil.de(role);
        if (perfil == null) {
            throw new RegraNegocioException("Perfil inválido. Valores aceitos: ADMIN, GERENTE ou ATENDENTE.");
        }
        return perfil;
    }

    private static void validarSenha(String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new RegraNegocioException("A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
    }

    private static Map<String, Object> retrato(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", u.getUsername());
        m.put("perfil", u.getRole());
        return m;
    }
}
