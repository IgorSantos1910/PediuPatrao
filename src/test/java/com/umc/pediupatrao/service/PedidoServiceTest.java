package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PedidoServiceTest {

    private final Map<String, Pedido> banco = new HashMap<>();
    private PedidoRepository pedidoRepository;
    private AuditoriaService auditoriaService;
    private UsuarioLogado usuarioLogado;
    private PedidoService service;

    @BeforeEach
    void configurar() {
        pedidoRepository = mock(PedidoRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        ProdutoRepository produtoRepository = mock(ProdutoRepository.class);
        auditoriaService = mock(AuditoriaService.class);
        usuarioLogado = mock(UsuarioLogado.class);

        Cliente cliente = new Cliente();
        cliente.setId("c1");
        cliente.setNome("Maria");
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente));
        when(produtoRepository.findById("pizza")).thenReturn(Optional.of(produto("pizza", "Pizza Calabresa", 50.0)));
        when(produtoRepository.findById("refri")).thenReturn(Optional.of(produto("refri", "Refrigerante", 10.0)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId("p" + (banco.size() + 1));
            }
            banco.put(p.getId(), p);
            return p;
        });
        when(pedidoRepository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(banco.get(inv.<String>getArgument(0))));
        when(usuarioLogado.username()).thenReturn("atendente01");

        service = new PedidoService(pedidoRepository, clienteRepository, produtoRepository, auditoriaService, usuarioLogado);
    }

    private static Produto produto(String id, String nome, double preco) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setPreco(preco);
        p.setAtivo(true);
        return p;
    }

    private static PedidoRequest request(TipoEntrega tipo) {
        PedidoRequest r = new PedidoRequest();
        r.setClienteId("c1");
        r.setTipoEntrega(tipo);
        r.setItens(new java.util.ArrayList<>(List.of(new ItemPedidoRequest("pizza", 2), new ItemPedidoRequest("refri", 1))));
        return r;
    }

    private Pedido pedidoEm(StatusPedido alvo, TipoEntrega tipo) {
        Pedido p = service.criarPedido(request(tipo));
        StatusPedido atual = p.getStatus();
        while (atual != alvo) {
            atual = atual.proximos(tipo).iterator().next();
            p = service.atualizarStatus(p.getId(), atual);
        }
        return p;
    }

    // RF02

    @Test
    void criarPedidoCalculaTotalNoServidorERegistraEntrada() {
        Pedido p = service.criarPedido(request(TipoEntrega.ENTREGA));

        assertThat(p.getSubtotal()).isEqualByComparingTo("110.00");
        assertThat(p.getValorTotal()).isEqualByComparingTo("110.00");
        assertThat(p.getItens()).hasSize(2);
        assertThat(p.getItens().get(0).getPrecoUnitario()).isEqualByComparingTo("50.00");
        assertThat(p.getStatus()).isEqualTo(StatusPedido.RECEBIDO);
        assertThat(p.getResponsavelEntrada()).isEqualTo("atendente01");
        assertThat(p.getDataHoraEntrada()).isNotNull();
        assertThat(p.getClienteNome()).isEqualTo("Maria");
        verify(auditoriaService).registrar(eq(TipoOperacao.INCLUSAO_PEDIDO), eq("Pedido"), eq(p.getId()), anyString(), anyList(), isNull());
    }

    @Test
    void pedidoSemItensEhRejeitado() {
        PedidoRequest r = request(TipoEntrega.ENTREGA);
        r.setItens(new java.util.ArrayList<>());
        assertThatThrownBy(() -> service.criarPedido(r)).isInstanceOf(RegraNegocioException.class);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void quantidadeInvalidaEhRejeitada() {
        PedidoRequest r = request(TipoEntrega.ENTREGA);
        r.getItens().get(0).setQuantidade(0);
        assertThatThrownBy(() -> service.criarPedido(r)).isInstanceOf(RegraNegocioException.class);
    }

    // RF03

    @Test
    void naoPermiteFinalizarPedidoEmPreparacao() {
        Pedido p = pedidoEm(StatusPedido.EM_PREPARACAO, TipoEntrega.ENTREGA);
        assertThatThrownBy(() -> service.atualizarStatus(p.getId(), StatusPedido.FINALIZADO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Transição inválida");
    }

    @Test
    void saidaRegistraDataHoraEResponsavel() {
        Pedido p = pedidoEm(StatusPedido.PRONTO, TipoEntrega.ENTREGA);
        when(usuarioLogado.username()).thenReturn("gerente01");

        Pedido saiu = service.atualizarStatus(p.getId(), StatusPedido.SAIU_PARA_ENTREGA);

        assertThat(saiu.getDataHoraSaida()).isNotNull();
        assertThat(saiu.getResponsavelSaida()).isEqualTo("gerente01");
        assertThat(saiu.getHistorico()).extracting("statusNovo")
                .containsExactly(StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO, StatusPedido.SAIU_PARA_ENTREGA);
    }

    @Test
    void pedidoDeRetiradaNaoPodeSairParaEntrega() {
        Pedido p = pedidoEm(StatusPedido.PRONTO, TipoEntrega.RETIRADA);
        assertThatThrownBy(() -> service.atualizarStatus(p.getId(), StatusPedido.SAIU_PARA_ENTREGA))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(service.atualizarStatus(p.getId(), StatusPedido.RETIRADO).getResponsavelSaida()).isNotNull();
    }

    @Test
    void statusCanceladoNaoPodeSerDefinidoPelaTransicaoComum() {
        Pedido p = pedidoEm(StatusPedido.RECEBIDO, TipoEntrega.ENTREGA);
        assertThatThrownBy(() -> service.atualizarStatus(p.getId(), StatusPedido.CANCELADO))
                .isInstanceOf(RegraNegocioException.class);
    }

    // RF04 - cancelamento

    @Test
    void cancelamentoExigeJustificativa() {
        Pedido p = pedidoEm(StatusPedido.RECEBIDO, TipoEntrega.ENTREGA);
        assertThatThrownBy(() -> service.cancelarPedido(p.getId(), "  ")).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.cancelarPedido(p.getId(), "curta")).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void cancelamentoPreservaDadosEBloqueiaAlteracoesPosteriores() {
        Pedido p = pedidoEm(StatusPedido.EM_PREPARACAO, TipoEntrega.ENTREGA);
        when(usuarioLogado.username()).thenReturn("gerente01");

        Pedido cancelado = service.cancelarPedido(p.getId(), "Cliente desistiu por telefone");

        assertThat(cancelado.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(cancelado.getStatusAntesCancelamento()).isEqualTo(StatusPedido.EM_PREPARACAO);
        assertThat(cancelado.getResponsavelCancelamento()).isEqualTo("gerente01");
        assertThat(cancelado.getItens()).hasSize(2);
        assertThat(cancelado.getValorTotal()).isEqualByComparingTo("110.00");
        verify(auditoriaService).registrar(eq(TipoOperacao.CANCELAMENTO_PEDIDO), eq("Pedido"), eq(p.getId()),
                anyString(), anyList(), eq("Cliente desistiu por telefone"));

        assertThatThrownBy(() -> service.atualizarStatus(p.getId(), StatusPedido.PRONTO)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.aplicarDesconto(p.getId(), BigDecimal.TEN, null)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.cancelarPedido(p.getId(), "Segunda tentativa de cancelar")).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void naoCancelaPedidoQueJaSaiu() {
        Pedido p = pedidoEm(StatusPedido.SAIU_PARA_ENTREGA, TipoEntrega.ENTREGA);
        assertThatThrownBy(() -> service.cancelarPedido(p.getId(), "Cliente desistiu da entrega"))
                .isInstanceOf(RegraNegocioException.class);
    }

    // RF04 - desconto

    @Test
    void descontoPercentualRecalculaTotal() {
        Pedido p = pedidoEm(StatusPedido.PRONTO, TipoEntrega.ENTREGA);
        Pedido comDesconto = service.aplicarDesconto(p.getId(), new BigDecimal("20"), "Atraso na entrega");

        assertThat(comDesconto.getValorDesconto()).isEqualByComparingTo("22.00");
        assertThat(comDesconto.getValorTotal()).isEqualByComparingTo("88.00");
        verify(auditoriaService).registrar(eq(TipoOperacao.DESCONTO_PEDIDO), eq("Pedido"), eq(p.getId()),
                anyString(), anyList(), eq("Atraso na entrega"));
    }

    @Test
    void descontoAcimaDe20PorCentoEhRejeitado() {
        Pedido p = pedidoEm(StatusPedido.RECEBIDO, TipoEntrega.ENTREGA);
        assertThatThrownBy(() -> service.aplicarDesconto(p.getId(), new BigDecimal("20.01"), null))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.aplicarDesconto(p.getId(), new BigDecimal("-1"), null))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void descontoAposSaidaEhRejeitado() {
        Pedido p = pedidoEm(StatusPedido.RETIRADO, TipoEntrega.RETIRADA);
        assertThatThrownBy(() -> service.aplicarDesconto(p.getId(), BigDecimal.TEN, null))
                .isInstanceOf(RegraNegocioException.class);
    }
}
