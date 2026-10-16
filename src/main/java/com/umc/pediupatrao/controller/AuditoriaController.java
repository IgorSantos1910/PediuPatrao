package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.AuditoriaFiltro;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.service.AuditoriaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Consulta de auditoria (somente leitura). Não há endpoints de inclusão, alteração ou exclusão.
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    // Filtros opcionais: ?entidade=Pedido&entidadeId=...&usuario=...&operacao=CANCELAMENTO_PEDIDO
    @GetMapping
    public List<RegistroAuditoria> consultar(@ModelAttribute AuditoriaFiltro filtro) {
        return auditoriaService.consultar(filtro);
    }
}
