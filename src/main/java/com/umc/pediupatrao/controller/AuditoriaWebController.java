package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.AuditoriaFiltro;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.service.AuditoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

// Tela de consulta da auditoria (ADMIN e GERENTE). Somente leitura.
@Controller
@RequestMapping("/auditoria")
public class AuditoriaWebController {

    private final AuditoriaService auditoriaService;

    public AuditoriaWebController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String consultar(@ModelAttribute("filtro") AuditoriaFiltro filtro, Model model) {
        model.addAttribute("registros", auditoriaService.consultar(filtro));
        model.addAttribute("operacoes", TipoOperacao.values());
        return "auditoria/lista";
    }
}
