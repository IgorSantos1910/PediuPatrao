package com.umc.pediupatrao.entity;

import java.util.ArrayList;
import java.util.List;

// Status do pedido. Fluxo: RECEBIDO -> EM_PREPARACAO -> PRONTO -> SAIU_PARA_ENTREGA ou RETIRADO -> FINALIZADO
// CANCELADO só acontece pela opção de cancelamento.
public enum StatusPedido {

    RECEBIDO("Recebido"),
    EM_PREPARACAO("Em preparação"),
    PRONTO("Pronto"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    RETIRADO("Retirado"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    // Diz qual é o próximo status permitido a partir do status atual
    public List<StatusPedido> proximos(TipoEntrega tipoEntrega) {
        List<StatusPedido> lista = new ArrayList<>();
        if (this == RECEBIDO) {
            lista.add(EM_PREPARACAO);
        } else if (this == EM_PREPARACAO) {
            lista.add(PRONTO);
        } else if (this == PRONTO) {
            if (tipoEntrega == TipoEntrega.RETIRADA) {
                lista.add(RETIRADO);
            } else {
                lista.add(SAIU_PARA_ENTREGA);
            }
        } else if (this == SAIU_PARA_ENTREGA || this == RETIRADO) {
            lista.add(FINALIZADO);
        }
        // FINALIZADO e CANCELADO não têm próximo status
        return lista;
    }

    public boolean podeAvancarPara(StatusPedido novo, TipoEntrega tipoEntrega) {
        return proximos(tipoEntrega).contains(novo);
    }

    // Desconto e cancelamento só são permitidos nesses três status
    public boolean isAntesDaSaida() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }

    public boolean isEstadoFinal() {
        return this == FINALIZADO || this == CANCELADO;
    }
}
