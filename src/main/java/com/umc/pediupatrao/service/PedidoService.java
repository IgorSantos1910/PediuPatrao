package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    public static final String ENTIDADE = "Pedido";

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoriaService;
    private final UsuarioLogado usuarioLogado;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository, AuditoriaService auditoriaService, UsuarioLogado usuarioLogado) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.auditoriaService = auditoriaService;
        this.usuarioLogado = usuarioLogado;
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll(Sort.by(Sort.Direction.DESC, "dataHoraEntrada"));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public Pedido buscarPorId(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }

    // Registro do pedido (entrada)
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido criarPedido(PedidoRequest request) {
        validar(request);
        Cliente cliente = buscarCliente(request.getClienteId());
        LocalDateTime agora = LocalDateTime.now();
        String usuario = usuarioLogado.username();

        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setClienteNome(cliente.getNome());
        pedido.setTipoEntrega(request.getTipoEntrega());
        pedido.setObservacao(request.getObservacao());
        pedido.substituirItens(montarItens(request.getItens())); // aqui o total é calculado
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.registrarEntrada(agora, usuario);
        pedido.registrarHistorico(null, StatusPedido.RECEBIDO, agora, usuario);

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.INCLUSAO_PEDIDO, ENTIDADE, salvo.getId(), "Pedido registrado",
                AuditoriaService.comparar(Map.of(), dadosDoPedido(salvo)), null);
        return salvo;
    }

    // Alteração dos itens (só enquanto RECEBIDO)
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido alterarPedido(String id, PedidoRequest request) {
        validar(request);
        Pedido pedido = buscarPorId(id);
        if (pedido.getStatus() != StatusPedido.RECEBIDO) {
            throw new RegraNegocioException("Só é possível alterar pedidos com status RECEBIDO.");
        }
        Map<String, Object> antes = dadosDoPedido(pedido);

        Cliente cliente = buscarCliente(request.getClienteId());
        pedido.setClienteId(cliente.getId());
        pedido.setClienteNome(cliente.getNome());
        pedido.setTipoEntrega(request.getTipoEntrega());
        pedido.setObservacao(request.getObservacao());
        pedido.substituirItens(montarItens(request.getItens()));

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.ALTERACAO_PEDIDO, ENTIDADE, id, "Pedido alterado",
                AuditoriaService.comparar(antes, dadosDoPedido(salvo)), null);
        return salvo;
    }

    // Mudança de status
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido atualizarStatus(String id, StatusPedido novoStatus) {
        Pedido pedido = buscarPorId(id);
        StatusPedido atual = pedido.getStatus();

        if (atual == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Pedido cancelado não pode ter o status alterado.");
        }
        if (novoStatus == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Para cancelar use a opção de cancelamento (somente gerente).");
        }
        if (!atual.podeAvancarPara(novoStatus, pedido.getTipoEntrega())) {
            throw new RegraNegocioException("Transição inválida: " + atual.getDescricao() + " -> " + novoStatus.getDescricao() + ".");
        }

        Map<String, Object> antes = dadosDoStatus(pedido);
        LocalDateTime agora = LocalDateTime.now();
        String usuario = usuarioLogado.username();

        pedido.setStatus(novoStatus);
        if (novoStatus == StatusPedido.SAIU_PARA_ENTREGA || novoStatus == StatusPedido.RETIRADO) {
            pedido.registrarSaida(agora, usuario);
        }
        if (novoStatus == StatusPedido.FINALIZADO) {
            pedido.registrarFinalizacao(agora, usuario);
        }
        pedido.registrarHistorico(atual, novoStatus, agora, usuario);

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.MUDANCA_STATUS_PEDIDO, ENTIDADE, id,
                atual.getDescricao() + " -> " + novoStatus.getDescricao(),
                AuditoriaService.comparar(antes, dadosDoStatus(salvo)), null);
        return salvo;
    }

    // Desconto (somente GERENTE)
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual, String justificativa) {
        if (percentual == null || percentual.compareTo(BigDecimal.ZERO) < 0 || percentual.compareTo(Pedido.DESCONTO_MAXIMO) > 0) {
            throw new RegraNegocioException("O desconto deve estar entre 0% e 20%.");
        }
        Pedido pedido = buscarPorId(id);
        if (!pedido.getStatus().isAntesDaSaida()) {
            throw new RegraNegocioException("Desconto só pode ser aplicado antes da saída do pedido (e em pedido não cancelado).");
        }

        Map<String, Object> antes = dadosDosValores(pedido);
        pedido.definirDesconto(percentual, usuarioLogado.username(), LocalDateTime.now(), justificativa);

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.DESCONTO_PEDIDO, ENTIDADE, id, "Desconto de " + percentual + "%",
                AuditoriaService.comparar(antes, dadosDosValores(salvo)), justificativa);
        return salvo;
    }

    // Cancelamento (somente GERENTE)
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelarPedido(String id, String justificativa) {
        if (justificativa == null || justificativa.trim().length() < 10) {
            throw new RegraNegocioException("Informe a justificativa do cancelamento (mínimo 10 caracteres).");
        }
        Pedido pedido = buscarPorId(id);
        if (!pedido.getStatus().isAntesDaSaida()) {
            throw new RegraNegocioException("Só é possível cancelar pedidos que ainda não saíram (e que não estejam cancelados).");
        }

        Map<String, Object> antes = dadosDoStatus(pedido);
        StatusPedido anterior = pedido.getStatus();
        LocalDateTime agora = LocalDateTime.now();
        String usuario = usuarioLogado.username();

        // Itens e valores continuam salvos; só o status muda
        pedido.registrarCancelamento(agora, usuario, justificativa.trim());
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.registrarHistorico(anterior, StatusPedido.CANCELADO, agora, usuario);

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.CANCELAMENTO_PEDIDO, ENTIDADE, id, "Pedido cancelado",
                AuditoriaService.comparar(antes, dadosDoStatus(salvo)), justificativa.trim());
        return salvo;
    }

    // Métodos auxiliares

    private void validar(PedidoRequest request) {
        if (request.getClienteId() == null || request.getClienteId().isBlank()) {
            throw new RegraNegocioException("Selecione o cliente.");
        }
        if (request.getTipoEntrega() == null) {
            throw new RegraNegocioException("Informe se é entrega ou retirada.");
        }
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new RegraNegocioException("O pedido precisa ter pelo menos um item.");
        }
        for (ItemPedidoRequest item : request.getItens()) {
            if (item.getProdutoId() == null || item.getProdutoId().isBlank()) {
                throw new RegraNegocioException("Selecione o produto de todos os itens.");
            }
            if (item.getQuantidade() < 1 || item.getQuantidade() > 50) {
                throw new RegraNegocioException("A quantidade de cada item deve ser de 1 a 50.");
            }
        }
    }

    private Cliente buscarCliente(String clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado."));
    }

    // O preço vem do cadastro de produtos, nunca da requisição
    private List<ItemPedido> montarItens(List<ItemPedidoRequest> itensRequest) {
        List<ItemPedido> itens = new ArrayList<>();
        for (ItemPedidoRequest req : itensRequest) {
            Produto produto = produtoRepository.findById(req.getProdutoId())
                    .orElseThrow(() -> new RegraNegocioException("Produto não encontrado."));
            if (Boolean.FALSE.equals(produto.getAtivo())) {
                throw new RegraNegocioException("O produto " + produto.getNome() + " está inativo.");
            }
            if (produto.getPreco() == null) {
                throw new RegraNegocioException("O produto " + produto.getNome() + " está sem preço.");
            }
            itens.add(new ItemPedido(produto.getId(), produto.getNome(), req.getQuantidade(),
                    BigDecimal.valueOf(produto.getPreco())));
        }
        return itens;
    }

    // "Fotos" do pedido usadas na auditoria para comparar antes e depois
    private Map<String, Object> dadosDoPedido(Pedido p) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("cliente", p.getClienteNome());
        dados.put("tipoEntrega", p.getTipoEntrega());
        dados.put("itens", p.resumoItens());
        dados.put("observacao", p.getObservacao());
        dados.put("status", p.getStatus());
        dados.put("dataHoraEntrada", p.getDataHoraEntrada());
        dados.put("responsavelEntrada", p.getResponsavelEntrada());
        dados.putAll(dadosDosValores(p));
        return dados;
    }

    private Map<String, Object> dadosDosValores(Pedido p) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("subtotal", p.getSubtotal());
        dados.put("percentualDesconto", p.getPercentualDesconto());
        dados.put("valorDesconto", p.getValorDesconto());
        dados.put("valorTotal", p.getValorTotal());
        return dados;
    }

    private Map<String, Object> dadosDoStatus(Pedido p) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("status", p.getStatus());
        dados.put("dataHoraSaida", p.getDataHoraSaida());
        dados.put("responsavelSaida", p.getResponsavelSaida());
        dados.put("dataHoraFinalizacao", p.getDataHoraFinalizacao());
        dados.put("responsavelFinalizacao", p.getResponsavelFinalizacao());
        dados.put("dataHoraCancelamento", p.getDataHoraCancelamento());
        dados.put("responsavelCancelamento", p.getResponsavelCancelamento());
        return dados;
    }
}
