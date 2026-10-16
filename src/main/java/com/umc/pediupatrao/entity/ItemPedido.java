package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Item do pedido
public class ItemPedido {

    private String produtoId;
    private String nomeProduto;
    private int quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;

    public ItemPedido() {
    }

    public ItemPedido(String produtoId, String nomeProduto, int quantidade, BigDecimal precoUnitario) {
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario.setScale(2, RoundingMode.HALF_UP);
        this.subtotal = this.precoUnitario.multiply(BigDecimal.valueOf(quantidade)).setScale(2, RoundingMode.HALF_UP);
    }

    public String getProdutoId() {
        return produtoId;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    @Override
    public String toString() {
        return quantidade + "x " + nomeProduto + " (R$ " + precoUnitario + ")";
    }
}
