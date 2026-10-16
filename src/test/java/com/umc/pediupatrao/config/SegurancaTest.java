package com.umc.pediupatrao.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.controller.AuditoriaController;
import com.umc.pediupatrao.controller.ClienteController;
import com.umc.pediupatrao.controller.PedidoController;
import com.umc.pediupatrao.controller.PedidoWebController;
import com.umc.pediupatrao.controller.UsuarioController;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.security.UsuarioLogado;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import com.umc.pediupatrao.service.UsuarioService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Comprova que as permissões são aplicadas no backend (e não apenas ocultando botões)
@WebMvcTest(controllers = {PedidoController.class, AuditoriaController.class, UsuarioController.class,
        ClienteController.class, PedidoWebController.class})
@Import(SecurityConfig.class)
class SegurancaTest {

    private static final String SENHA = "senha123";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;
    @MockitoBean
    private PedidoService pedidoService;
    @MockitoBean
    private AuditoriaService auditoriaService;
    @MockitoBean
    private UsuarioService usuarioService;
    @MockitoBean
    private ClienteService clienteService;
    @MockitoBean
    private ProdutoService produtoService;
    @MockitoBean
    private UsuarioLogado usuarioLogado;

    @BeforeEach
    void usuarios() {
        String hash = new BCryptPasswordEncoder(4).encode(SENHA);
        when(usuarioDetailsService.loadUserByUsername(anyString())).thenAnswer(inv -> {
            String login = inv.getArgument(0);
            String perfil = switch (login) {
                case "admin" -> "ADMIN";
                case "gerente01" -> "GERENTE";
                case "atendente01" -> "ATENDENTE";
                default -> throw new UsernameNotFoundException(login);
            };
            return User.withUsername(login).password(hash).roles(perfil).build();
        });
        when(pedidoService.aplicarDesconto(anyString(), any(), any())).thenReturn(new Pedido());
        when(pedidoService.cancelarPedido(anyString(), any())).thenReturn(new Pedido());
        when(pedidoService.listarPedidos()).thenReturn(List.of());
        when(auditoriaService.consultar(any())).thenReturn(List.of());
    }

    // API REST

    @Test
    void apiExigeAutenticacao() throws Exception {
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroDeUsuariosNaoEhMaisPublico() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hacker\",\"password\":\"123456\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized());
        verify(usuarioService, never()).criarUsuario(any());
    }

    @Test
    void atendenteNaoPodeCancelarPedido() throws Exception {
        mvc.perform(post("/api/pedidos/p1/cancelamento").with(httpBasic("atendente01", SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Cliente desistiu\"}"))
                .andExpect(status().isForbidden());
        verify(pedidoService, never()).cancelarPedido(anyString(), any());
    }

    @Test
    void adminNaoPodeAplicarDescontoNemCancelar() throws Exception {
        mvc.perform(post("/api/pedidos/p1/desconto").with(httpBasic("admin", SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"percentual\":10}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/pedidos/p1/cancelamento").with(httpBasic("admin", SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Cliente desistiu\"}"))
                .andExpect(status().isForbidden());
        verify(pedidoService, never()).aplicarDesconto(anyString(), any(), any());
    }

    @Test
    void gerentePodeAplicarDescontoECancelar() throws Exception {
        mvc.perform(post("/api/pedidos/p1/desconto").with(httpBasic("gerente01", SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"percentual\":10}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/pedidos/p1/cancelamento").with(httpBasic("gerente01", SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Cliente desistiu\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminApenasConsultaPedidos() throws Exception {
        mvc.perform(get("/api/pedidos").with(httpBasic("admin", SENHA))).andExpect(status().isOk());
        mvc.perform(post("/api/pedidos").with(httpBasic("admin", SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditoriaSomenteLeituraParaAdminEGerente() throws Exception {
        mvc.perform(get("/api/auditoria").with(httpBasic("gerente01", SENHA))).andExpect(status().isOk());
        mvc.perform(get("/api/auditoria").with(httpBasic("admin", SENHA))).andExpect(status().isOk());
        mvc.perform(get("/api/auditoria").with(httpBasic("atendente01", SENHA))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/auditoria/qualquer").with(httpBasic("admin", SENHA))).andExpect(status().isForbidden());
        mvc.perform(put("/api/auditoria/qualquer").with(httpBasic("gerente01", SENHA))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    void gerenteNaoAdministraUsuarios() throws Exception {
        mvc.perform(put("/api/usuarios/u1").with(httpBasic("gerente01", SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"gerente01\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        verify(usuarioService, never()).atualizarUsuario(anyString(), any());
    }

    @Test
    void adminNaoAcessaDadosDeClientes() throws Exception {
        mvc.perform(get("/api/clientes").with(httpBasic("admin", SENHA))).andExpect(status().isForbidden());
        mvc.perform(get("/api/clientes").with(httpBasic("gerente01", SENHA))).andExpect(status().isOk());
        mvc.perform(post("/api/clientes").with(httpBasic("gerente01", SENHA))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    // Telas (sessão + CSRF)

    @Test
    void formularioSemTokenCsrfEhRejeitado() throws Exception {
        mvc.perform(post("/pedidos/p1/cancelar").param("justificativa", "Cliente desistiu do pedido")
                        .with(user("gerente01").roles("GERENTE")))
                .andExpect(status().isForbidden());
        verify(pedidoService, never()).cancelarPedido(anyString(), any());
    }

    @Test
    void gerenteCancelaPelaTelaComCsrf() throws Exception {
        mvc.perform(post("/pedidos/p1/cancelar").param("justificativa", "Cliente desistiu do pedido")
                        .with(user("gerente01").roles("GERENTE")).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void atendenteNaoCancelaPelaTelaMesmoComCsrf() throws Exception {
        mvc.perform(post("/pedidos/p1/cancelar").param("justificativa", "Cliente desistiu do pedido")
                        .with(user("atendente01").roles("ATENDENTE")).with(csrf()))
                .andExpect(status().isForbidden());
        verify(pedidoService, never()).cancelarPedido(anyString(), any());
    }
}
