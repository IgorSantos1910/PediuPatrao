package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.security.UsuarioLogado;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Telas de pedidos
@Controller
@RequestMapping("/pedidos")
public class PedidoWebController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final AuditoriaService auditoriaService;
    private final UsuarioLogado usuarioLogado;

    public PedidoWebController(PedidoService pedidoService, ClienteService clienteService, ProdutoService produtoService,
            AuditoriaService auditoriaService, UsuarioLogado usuarioLogado) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.auditoriaService = auditoriaService;
        this.usuarioLogado = usuarioLogado;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) StatusPedido status, Model model) {
        List<Pedido> pedidos = pedidoService.listarPedidos();
        if (status != null) {
            List<Pedido> filtrados = new ArrayList<>();
            for (Pedido p : pedidos) {
                if (p.getStatus() == status) {
                    filtrados.add(p);
                }
            }
            pedidos = filtrados;
        }
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("statusFiltro", status);
        model.addAttribute("todosStatus", StatusPedido.values());
        return "pedidos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        PedidoRequest form = new PedidoRequest();
        form.getItens().add(new ItemPedidoRequest());
        return exibirFormulario(model, form, null);
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("pedido") PedidoRequest form, Model model, RedirectAttributes ra) {
        try {
            Pedido salvo = pedidoService.criarPedido(form);
            ra.addFlashAttribute("sucesso", "Pedido registrado com status RECEBIDO.");
            return "redirect:/pedidos/" + salvo.getId();
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return exibirFormulario(model, form, null);
        }
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable String id, Model model, RedirectAttributes ra) {
        Pedido pedido;
        try {
            pedido = pedidoService.buscarPorId(id);
        } catch (RecursoNaoEncontradoException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/pedidos";
        }
        model.addAttribute("pedido", pedido);
        model.addAttribute("proximos", pedido.getStatus().proximos(pedido.getTipoEntrega()));
        model.addAttribute("descontoMaximo", Pedido.DESCONTO_MAXIMO);
        Perfil perfil = usuarioLogado.perfil();
        if (perfil == Perfil.ADMIN || perfil == Perfil.GERENTE) {
            model.addAttribute("auditoria", auditoriaService.consultarPorEntidade(PedidoService.ENTIDADE, id));
        }
        return "pedidos/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String id, Model model, RedirectAttributes ra) {
        Pedido pedido = pedidoService.buscarPorId(id);
        if (pedido.getStatus() != StatusPedido.RECEBIDO) {
            ra.addFlashAttribute("erro", "Somente pedidos com status RECEBIDO podem ser alterados.");
            return "redirect:/pedidos/" + id;
        }
        PedidoRequest form = new PedidoRequest();
        form.setClienteId(pedido.getClienteId());
        form.setTipoEntrega(pedido.getTipoEntrega());
        form.setObservacao(pedido.getObservacao());
        List<ItemPedidoRequest> itens = new ArrayList<>();
        for (ItemPedido item : pedido.getItens()) {
            itens.add(new ItemPedidoRequest(item.getProdutoId(), item.getQuantidade()));
        }
        form.setItens(itens);
        return exibirFormulario(model, form, id);
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable String id, @ModelAttribute("pedido") PedidoRequest form, Model model,
            RedirectAttributes ra) {
        try {
            pedidoService.alterarPedido(id, form);
            ra.addFlashAttribute("sucesso", "Pedido alterado.");
            return "redirect:/pedidos/" + id;
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return exibirFormulario(model, form, id);
        }
    }

    @PostMapping("/{id}/status")
    public String alterarStatus(@PathVariable String id, @RequestParam StatusPedido status, RedirectAttributes ra) {
        try {
            Pedido p = pedidoService.atualizarStatus(id, status);
            ra.addFlashAttribute("sucesso", "Status alterado para " + p.getStatus().getDescricao() + ".");
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    @PostMapping("/{id}/desconto")
    public String aplicarDesconto(@PathVariable String id, @RequestParam(required = false) BigDecimal percentual,
            @RequestParam(required = false) String justificativa, RedirectAttributes ra) {
        try {
            Pedido p = pedidoService.aplicarDesconto(id, percentual, justificativa);
            ra.addFlashAttribute("sucesso", "Desconto de " + p.getPercentualDesconto() + "% aplicado.");
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable String id, @RequestParam(required = false) String justificativa,
            RedirectAttributes ra) {
        try {
            pedidoService.cancelarPedido(id, justificativa);
            ra.addFlashAttribute("sucesso", "Pedido cancelado.");
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    private String exibirFormulario(Model model, PedidoRequest form, String pedidoId) {
        if (form.getItens() == null || form.getItens().isEmpty()) {
            form.setItens(new ArrayList<>(List.of(new ItemPedidoRequest())));
        }
        model.addAttribute("pedido", form);
        model.addAttribute("pedidoId", pedidoId);
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("produtos", produtoService.listarAtivos());
        model.addAttribute("tiposEntrega", TipoEntrega.values());
        return "pedidos/form";
    }
}
