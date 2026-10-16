package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.service.ClienteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteWebController {

    private final ClienteService clienteService;

    public ClienteWebController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public String clientes(Model model) {
        model.addAttribute("clientes", clienteService.listarClientes());
        return "clientes/lista";
    }

    @GetMapping("/novo")
    public String novoClienteForm(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "clientes/form";
    }

    @PostMapping("/salvar")
    public String salvarCliente(@ModelAttribute Cliente cliente, Model model, RedirectAttributes redirectAttributes) {
        try {
            cliente.setId(null);
            clienteService.criar(cliente);
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "clientes/form";
        }
        redirectAttributes.addFlashAttribute("sucesso", "Cliente criado com sucesso!");
        return "redirect:/clientes";
    }

    @GetMapping("/editar/{id}")
    public String editarClienteForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        return clienteService.buscarPorId(id).map(c -> {
            model.addAttribute("cliente", c);
            return "clientes/form";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("erro", "Cliente não encontrado.");
            return "redirect:/clientes";
        });
    }

    @PostMapping("/editar/{id}")
    public String atualizarCliente(@PathVariable String id, @ModelAttribute Cliente cliente, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            clienteService.atualizar(id, cliente);
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            cliente.setId(id);
            model.addAttribute("erro", e.getMessage());
            return "clientes/form";
        }
        redirectAttributes.addFlashAttribute("sucesso", "Cliente atualizado com sucesso!");
        return "redirect:/clientes";
    }

    @PostMapping("/{id}/excluir")
    public String excluirCliente(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            clienteService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Cliente excluído.");
        } catch (RecursoNaoEncontradoException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/clientes";
    }
}
