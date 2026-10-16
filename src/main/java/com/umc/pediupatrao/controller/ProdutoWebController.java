package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.service.ProdutoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/produtos")
public class ProdutoWebController {

    private final ProdutoService produtoService;

    public ProdutoWebController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public String produtos(Model model) {
        model.addAttribute("produtos", produtoService.listarProdutos());
        return "produtos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        Produto produto = new Produto();
        produto.setAtivo(true);
        model.addAttribute("produto", produto);
        return "produtos/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable String id, Model model, RedirectAttributes ra) {
        return produtoService.buscarPorId(id).map(p -> {
            model.addAttribute("produto", p);
            return "produtos/form";
        }).orElseGet(() -> {
            ra.addFlashAttribute("erro", "Produto não encontrado.");
            return "redirect:/produtos";
        });
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Produto produto, Model model, RedirectAttributes ra) {
        try {
            produtoService.salvar(produto);
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            model.addAttribute("erro", e.getMessage());
            return "produtos/form";
        }
        ra.addFlashAttribute("sucesso", "Produto salvo.");
        return "redirect:/produtos";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable String id, RedirectAttributes ra) {
        try {
            produtoService.excluir(id);
            ra.addFlashAttribute("sucesso", "Produto excluído.");
        } catch (RecursoNaoEncontradoException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/produtos";
    }
}
