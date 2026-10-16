package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.TipoOperacao;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditoriaServiceTest {

    @Test
    void compararRetornaSomenteCamposAlteradosComValoresAnterioresENovos() {
        Map<String, Object> antes = new LinkedHashMap<>();
        antes.put("nome", "Maria");
        antes.put("logradouro", "Rua das Flores");
        antes.put("telefone", "11999990000");
        Map<String, Object> depois = new LinkedHashMap<>(antes);
        depois.put("logradouro", "Av. Brasil");

        List<CampoAlterado> alteracoes = AuditoriaService.comparar(antes, depois);

        assertThat(alteracoes).hasSize(1);
        assertThat(alteracoes.get(0).getCampo()).isEqualTo("logradouro");
        assertThat(alteracoes.get(0).getValorAnterior()).isEqualTo("Rua das Flores");
        assertThat(alteracoes.get(0).getValorNovo()).isEqualTo("Av. Brasil");
    }

    @Test
    void mascararMantemSoOInicioEOFim() {
        assertThat(AuditoriaService.mascarar("11988887777")).isEqualTo("11*******77");
        assertThat(AuditoriaService.mascarar("10")).isEqualTo("**");
        assertThat(AuditoriaService.mascarar(null)).isNull();
    }

    @Test
    void adminVeDadosPessoaisMascaradosEGerenteVeCompletos() {
        RegistroAuditoria r = new RegistroAuditoria("atendente01", "ATENDENTE", LocalDateTime.now(),
                TipoOperacao.ALTERACAO_CLIENTE, "Cliente", "c1", "Cliente alterado",
                List.of(new CampoAlterado("telefone", "11999990000", "11988887777"),
                        new CampoAlterado("nome", "Maria", "Maria Silva")), null);

        RegistroAuditoria visaoAdmin = AuditoriaService.paraExibicao(List.of(r), Perfil.ADMIN).get(0);
        RegistroAuditoria visaoGerente = AuditoriaService.paraExibicao(List.of(r), Perfil.GERENTE).get(0);

        assertThat(visaoAdmin.getAlteracoes().get(0).getValorNovo()).isEqualTo("11*******77");
        assertThat(visaoAdmin.getAlteracoes().get(1).getValorNovo()).isEqualTo("Maria Silva");
        assertThat(visaoGerente.getAlteracoes().get(0).getValorNovo()).isEqualTo("11988887777");
    }
}
