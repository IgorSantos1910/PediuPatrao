package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.security.UsuarioLogado;
import com.umc.pediupatrao.service.PedidoService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

    private final PedidoService pedidoService;
    private final UsuarioLogado usuarioLogado;

    public HomeController(PedidoService pedidoService, UsuarioLogado usuarioLogado) {
        this.pedidoService = pedidoService;
        this.usuarioLogado = usuarioLogado;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(Model model) {
        // Painel com a quantidade de pedidos por status (apenas para quem tem perfil válido)
        if (usuarioLogado.perfil() != null) {
            Map<StatusPedido, Integer> contagem = new LinkedHashMap<>();
            for (StatusPedido s : StatusPedido.values()) {
                contagem.put(s, 0);
            }
            List<Pedido> pedidos = pedidoService.listarPedidos();
            for (Pedido p : pedidos) {
                contagem.put(p.getStatus(), contagem.get(p.getStatus()) + 1);
            }
            model.addAttribute("contagem", contagem);
        }
        return "home";
    }

    @GetMapping("/configuracoes")
    public String configuracoes(Model model) {
        Perfil perfil = usuarioLogado.perfil();
        model.addAttribute("perfil", perfil == null ? "Sem perfil" : perfil.getDescricao());
        return "configuracoes";
    }

    @RequestMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso-negado";
    }
}
