package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// Evento de auditoria persistido no MongoDB
@Document(collection = "auditoria")
public class RegistroAuditoria {

    @Id
    private String id;
    private String usuario;
    private String perfil;
    private LocalDateTime dataHora;
    private TipoOperacao operacao;
    private String entidade;
    private String entidadeId;
    private String descricao;
    private List<CampoAlterado> alteracoes;
    private String justificativa;

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(String usuario, String perfil, LocalDateTime dataHora, TipoOperacao operacao,
            String entidade, String entidadeId, String descricao, List<CampoAlterado> alteracoes, String justificativa) {
        this.usuario = usuario;
        this.perfil = perfil;
        this.dataHora = dataHora;
        this.operacao = operacao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.descricao = descricao;
        this.alteracoes = alteracoes;
        this.justificativa = justificativa;
    }

    // Cópia somente para exibição (ex.: com dados pessoais mascarados); nunca é persistida.
    public RegistroAuditoria copiaParaExibicao(List<CampoAlterado> alteracoesExibidas) {
        RegistroAuditoria copia = new RegistroAuditoria(usuario, perfil, dataHora, operacao, entidade, entidadeId,
                descricao, alteracoesExibidas, justificativa);
        copia.id = this.id;
        return copia;
    }

    public String getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getPerfil() {
        return perfil;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public TipoOperacao getOperacao() {
        return operacao;
    }

    public String getEntidade() {
        return entidade;
    }

    public String getEntidadeId() {
        return entidadeId;
    }

    public String getDescricao() {
        return descricao;
    }

    public List<CampoAlterado> getAlteracoes() {
        return alteracoes;
    }

    public String getJustificativa() {
        return justificativa;
    }
}
