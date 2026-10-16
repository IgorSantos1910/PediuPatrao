package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.TipoOperacao;

// Filtros opcionais da consulta de auditoria.
public class AuditoriaFiltro {

    private String entidade;
    private String entidadeId;
    private String usuario;
    private TipoOperacao operacao;

    public String getEntidade() {
        return entidade;
    }

    public void setEntidade(String entidade) {
        this.entidade = entidade;
    }

    public String getEntidadeId() {
        return entidadeId;
    }

    public void setEntidadeId(String entidadeId) {
        this.entidadeId = entidadeId;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public TipoOperacao getOperacao() {
        return operacao;
    }

    public void setOperacao(TipoOperacao operacao) {
        this.operacao = operacao;
    }
}
