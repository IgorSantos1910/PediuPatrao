package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// Pedido da pizzaria
@Document(collection = "pedidos")
public class Pedido {

    public static final BigDecimal DESCONTO_MAXIMO = new BigDecimal("20");

    @Id
    private String id;

    private String clienteId;
    private String clienteNome;
    private TipoEntrega tipoEntrega;
    private String observacao;

    private List<ItemPedido> itens = new ArrayList<>();

    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal percentualDesconto = BigDecimal.ZERO;
    private BigDecimal valorDesconto = BigDecimal.ZERO;
    private BigDecimal valorTotal = BigDecimal.ZERO;

    private StatusPedido status;

    // Entrada
    private LocalDateTime dataHoraEntrada;
    private String responsavelEntrada;

    // Saída (entrega ou retirada)
    private LocalDateTime dataHoraSaida;
    private String responsavelSaida;

    // Finalização
    private LocalDateTime dataHoraFinalizacao;
    private String responsavelFinalizacao;

    // Desconto
    private String responsavelDesconto;
    private LocalDateTime dataHoraDesconto;
    private String justificativaDesconto;

    // Cancelamento
    private LocalDateTime dataHoraCancelamento;
    private String responsavelCancelamento;
    private String justificativaCancelamento;
    private StatusPedido statusAntesCancelamento;

    private List<HistoricoStatus> historico = new ArrayList<>();

    // Calcula subtotal, desconto e total. Só o servidor chama este método.
    public void recalcularValores() {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.getSubtotal());
        }
        this.subtotal = soma.setScale(2, RoundingMode.HALF_UP);

        // desconto = subtotal * percentual / 100
        this.valorDesconto = subtotal.multiply(percentualDesconto).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        this.valorTotal = subtotal.subtract(valorDesconto);
    }

    public void registrarHistorico(StatusPedido anterior, StatusPedido novo, LocalDateTime quando, String responsavel) {
        historico.add(new HistoricoStatus(anterior, novo, quando, responsavel));
    }

    public String resumoItens() {
        String texto = "";
        for (ItemPedido item : itens) {
            if (!texto.isEmpty()) {
                texto += "; ";
            }
            texto += item.toString();
        }
        return texto;
    }

    // Getters e setters. Os valores (subtotal, desconto, total) não têm setter de propósito.

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public TipoEntrega getTipoEntrega() {
        return tipoEntrega;
    }

    public void setTipoEntrega(TipoEntrega tipoEntrega) {
        this.tipoEntrega = tipoEntrega;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void substituirItens(List<ItemPedido> novosItens) {
        this.itens = new ArrayList<>(novosItens);
        recalcularValores();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getPercentualDesconto() {
        return percentualDesconto;
    }

    public void definirDesconto(BigDecimal percentual, String responsavel, LocalDateTime quando, String justificativa) {
        this.percentualDesconto = percentual.setScale(2, RoundingMode.HALF_UP);
        this.responsavelDesconto = responsavel;
        this.dataHoraDesconto = quando;
        this.justificativaDesconto = justificativa;
        recalcularValores();
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public LocalDateTime getDataHoraEntrada() {
        return dataHoraEntrada;
    }

    public String getResponsavelEntrada() {
        return responsavelEntrada;
    }

    public void registrarEntrada(LocalDateTime quando, String responsavel) {
        this.dataHoraEntrada = quando;
        this.responsavelEntrada = responsavel;
    }

    public LocalDateTime getDataHoraSaida() {
        return dataHoraSaida;
    }

    public String getResponsavelSaida() {
        return responsavelSaida;
    }

    public void registrarSaida(LocalDateTime quando, String responsavel) {
        this.dataHoraSaida = quando;
        this.responsavelSaida = responsavel;
    }

    public LocalDateTime getDataHoraFinalizacao() {
        return dataHoraFinalizacao;
    }

    public String getResponsavelFinalizacao() {
        return responsavelFinalizacao;
    }

    public void registrarFinalizacao(LocalDateTime quando, String responsavel) {
        this.dataHoraFinalizacao = quando;
        this.responsavelFinalizacao = responsavel;
    }

    public String getResponsavelDesconto() {
        return responsavelDesconto;
    }

    public LocalDateTime getDataHoraDesconto() {
        return dataHoraDesconto;
    }

    public String getJustificativaDesconto() {
        return justificativaDesconto;
    }

    public LocalDateTime getDataHoraCancelamento() {
        return dataHoraCancelamento;
    }

    public String getResponsavelCancelamento() {
        return responsavelCancelamento;
    }

    public String getJustificativaCancelamento() {
        return justificativaCancelamento;
    }

    public StatusPedido getStatusAntesCancelamento() {
        return statusAntesCancelamento;
    }

    public void registrarCancelamento(LocalDateTime quando, String responsavel, String justificativa) {
        this.statusAntesCancelamento = this.status;
        this.dataHoraCancelamento = quando;
        this.responsavelCancelamento = responsavel;
        this.justificativaCancelamento = justificativa;
    }

    public List<HistoricoStatus> getHistorico() {
        return historico;
    }

    public boolean isCancelado() {
        return status == StatusPedido.CANCELADO;
    }
}
