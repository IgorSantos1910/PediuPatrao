package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Administração de usuários e perfis: somente ADMIN (SecurityConfig + @PreAuthorize no serviço).
@Controller
@RequestMapping("/usuarios")
public class UsuarioWebController {

    private final UsuarioService usuarioService;

    public UsuarioWebController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "usuarios/lista";
    }

    @GetMapping("/novo")
    public String novoUsuarioForm(Model model) {
        return formulario(model, new UsuarioRequest(), null);
    }

    @GetMapping("/editar/{id}")
    public String editarUsuarioForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        return usuarioService.buscarPorId(id).map(u -> {
            UsuarioRequest form = new UsuarioRequest();
            form.setUsername(u.getUsername());
            form.setRole(u.getRole());
            return formulario(model, form, id);
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("erro", "Usuário não encontrado.");
            return "redirect:/usuarios";
        });
    }

    @PostMapping("/salvar")
    public String salvarUsuario(@ModelAttribute("usuario") UsuarioRequest usuario, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            usuarioService.criarUsuario(usuario);
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            usuario.setPassword(null);
            return formulario(model, usuario, null);
        }
        redirectAttributes.addFlashAttribute("sucesso", "Usuário criado com sucesso!");
        return "redirect:/usuarios";
    }

    @PostMapping("/editar/{id}")
    public String atualizarUsuario(@PathVariable String id, @ModelAttribute("usuario") UsuarioRequest usuario,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.atualizarUsuario(id, usuario);
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            model.addAttribute("erro", e.getMessage());
            usuario.setPassword(null);
            return formulario(model, usuario, id);
        }
        redirectAttributes.addFlashAttribute("sucesso", "Usuário atualizado com sucesso!");
        return "redirect:/usuarios";
    }

    @PostMapping("/deletar/{id}")
    public String deletarUsuario(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.deletarUsuario(id);
            redirectAttributes.addFlashAttribute("sucesso", "Usuário removido com sucesso!");
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    private String formulario(Model model, UsuarioRequest form, String id) {
        model.addAttribute("usuario", form);
        model.addAttribute("usuarioId", id);
        model.addAttribute("perfis", Perfil.values());
        return "usuarios/form";
    }
}
