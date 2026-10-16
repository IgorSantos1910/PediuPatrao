package com.umc.pediupatrao.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatusPedidoTest {

    @Test
    void fluxoDeEntregaSegueAOrdemObrigatoria() {
        assertThat(StatusPedido.RECEBIDO.proximos(TipoEntrega.ENTREGA)).containsExactly(StatusPedido.EM_PREPARACAO);
        assertThat(StatusPedido.EM_PREPARACAO.proximos(TipoEntrega.ENTREGA)).containsExactly(StatusPedido.PRONTO);
        assertThat(StatusPedido.PRONTO.proximos(TipoEntrega.ENTREGA)).containsExactly(StatusPedido.SAIU_PARA_ENTREGA);
        assertThat(StatusPedido.SAIU_PARA_ENTREGA.proximos(TipoEntrega.ENTREGA)).containsExactly(StatusPedido.FINALIZADO);
    }

    @Test
    void pedidoParaRetiradaSaiComoRetirado() {
        assertThat(StatusPedido.PRONTO.proximos(TipoEntrega.RETIRADA)).containsExactly(StatusPedido.RETIRADO);
        assertThat(StatusPedido.RETIRADO.proximos(TipoEntrega.RETIRADA)).containsExactly(StatusPedido.FINALIZADO);
    }

    @Test
    void naoPermiteSaltarEtapas() {
        assertThat(StatusPedido.EM_PREPARACAO.podeAvancarPara(StatusPedido.FINALIZADO, TipoEntrega.ENTREGA)).isFalse();
        assertThat(StatusPedido.RECEBIDO.podeAvancarPara(StatusPedido.PRONTO, TipoEntrega.ENTREGA)).isFalse();
        assertThat(StatusPedido.PRONTO.podeAvancarPara(StatusPedido.RETIRADO, TipoEntrega.ENTREGA)).isFalse();
    }

    @Test
    void estadosFinaisNaoTemProximos() {
        assertThat(StatusPedido.FINALIZADO.proximos(TipoEntrega.ENTREGA)).isEmpty();
        assertThat(StatusPedido.CANCELADO.proximos(TipoEntrega.ENTREGA)).isEmpty();
        assertThat(StatusPedido.CANCELADO.isEstadoFinal()).isTrue();
    }

    @Test
    void cancelamentoEDescontoSoAntesDaSaida() {
        assertThat(StatusPedido.PRONTO.isAntesDaSaida()).isTrue();
        assertThat(StatusPedido.SAIU_PARA_ENTREGA.isAntesDaSaida()).isFalse();
        assertThat(StatusPedido.RETIRADO.isAntesDaSaida()).isFalse();
        assertThat(StatusPedido.FINALIZADO.isAntesDaSaida()).isFalse();
    }
}
