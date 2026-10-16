package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;

// Linha do tempo do pedido: cada mudança de status com data/hora e responsável.
public class HistoricoStatus {

    private StatusPedido statusAnterior;
    private StatusPedido statusNovo;
    private LocalDateTime dataHora;
    private String responsavel;

    public HistoricoStatus() {
    }

    public HistoricoStatus(StatusPedido statusAnterior, StatusPedido statusNovo, LocalDateTime dataHora, String responsavel) {
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.dataHora = dataHora;
        this.responsavel = responsavel;
    }

    public StatusPedido getStatusAnterior() {
        return statusAnterior;
    }

    public StatusPedido getStatusNovo() {
        return statusNovo;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getResponsavel() {
        return responsavel;
    }
}
