package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.AuditoriaFiltro;
import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    // Campos de cliente que aparecem mascarados para o ADMIN
    private static final List<String> CAMPOS_PESSOAIS = List.of("telefone", "cep", "logradouro", "numero", "complemento", "endereco");

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioLogado usuarioLogado;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, UsuarioLogado usuarioLogado) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioLogado = usuarioLogado;
    }

    // Grava um novo registro (somente insert)
    public void registrar(TipoOperacao operacao, String entidade, String entidadeId,
            String descricao, List<CampoAlterado> alteracoes, String justificativa) {
        RegistroAuditoria registro = new RegistroAuditoria(
                usuarioLogado.username(),
                usuarioLogado.perfilTexto(),
                LocalDateTime.now(),
                operacao,
                entidade,
                entidadeId,
                descricao,
                alteracoes,
                justificativa);
        auditoriaRepository.insert(registro);
    }

    // Compara o "antes" e o "depois" e devolve só os campos que mudaram
    public static List<CampoAlterado> comparar(Map<String, ?> antes, Map<String, ?> depois) {
        List<String> campos = new ArrayList<>(antes.keySet());
        for (String campo : depois.keySet()) {
            if (!campos.contains(campo)) {
                campos.add(campo);
            }
        }

        List<CampoAlterado> alteracoes = new ArrayList<>();
        for (String campo : campos) {
            String valorAntes = texto(antes.get(campo));
            String valorDepois = texto(depois.get(campo));
            if (!Objects.equals(valorAntes, valorDepois)) {
                alteracoes.add(new CampoAlterado(campo, valorAntes, valorDepois));
            }
        }
        return alteracoes;
    }

    private static String texto(Object valor) {
        if (valor == null || valor.toString().isBlank()) {
            return null;
        }
        if (valor instanceof LocalDateTime data) {
            return data.format(FORMATO_DATA);
        }
        return valor.toString().trim();
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public List<RegistroAuditoria> consultar(AuditoriaFiltro filtro) {
        // Busca todos (mais recentes primeiro) e aplica os filtros que foram preenchidos
        List<RegistroAuditoria> registros = new ArrayList<>();
        for (RegistroAuditoria r : auditoriaRepository.findAllByOrderByDataHoraDesc()) {
            if (!vazio(filtro.getEntidade()) && !r.getEntidade().equals(filtro.getEntidade())) {
                continue;
            }
            if (!vazio(filtro.getEntidadeId()) && !r.getEntidadeId().equals(filtro.getEntidadeId().trim())) {
                continue;
            }
            if (!vazio(filtro.getUsuario()) && !r.getUsuario().equals(filtro.getUsuario().trim())) {
                continue;
            }
            if (filtro.getOperacao() != null && r.getOperacao() != filtro.getOperacao()) {
                continue;
            }
            registros.add(r);
        }
        return paraExibicao(registros, usuarioLogado.perfil());
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public List<RegistroAuditoria> consultarPorEntidade(String entidade, String entidadeId) {
        List<RegistroAuditoria> registros = auditoriaRepository.findByEntidadeAndEntidadeIdOrderByDataHoraDesc(entidade, entidadeId);
        return paraExibicao(registros, usuarioLogado.perfil());
    }

    // GERENTE vê os dados completos; ADMIN vê telefone e endereço mascarados
    static List<RegistroAuditoria> paraExibicao(List<RegistroAuditoria> registros, Perfil perfil) {
        if (perfil == Perfil.GERENTE) {
            return registros;
        }
        List<RegistroAuditoria> resultado = new ArrayList<>();
        for (RegistroAuditoria r : registros) {
            List<CampoAlterado> alteracoes = new ArrayList<>();
            for (CampoAlterado c : r.getAlteracoes()) {
                if (CAMPOS_PESSOAIS.contains(c.getCampo())) {
                    alteracoes.add(new CampoAlterado(c.getCampo(), mascarar(c.getValorAnterior()), mascarar(c.getValorNovo())));
                } else {
                    alteracoes.add(c);
                }
            }
            resultado.add(r.copiaParaExibicao(alteracoes));
        }
        return resultado;
    }

    // Mostra só os 2 primeiros e os 2 últimos caracteres. Ex.: 11988887777 -> 11*******77
    static String mascarar(String valor) {
        if (valor == null) {
            return null;
        }
        if (valor.length() <= 4) {
            return "*".repeat(valor.length());
        }
        return valor.substring(0, 2) + "*".repeat(valor.length() - 4) + valor.substring(valor.length() - 2);
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }
}
