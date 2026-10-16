package com.umc.pediupatrao.entity;

// Campo modificado em uma operação auditada, com o valor anterior e o novo.
public class CampoAlterado {

    private String campo;
    private String valorAnterior;
    private String valorNovo;

    public CampoAlterado() {
    }

    public CampoAlterado(String campo, String valorAnterior, String valorNovo) {
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
    }

    public String getCampo() {
        return campo;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNovo() {
        return valorNovo;
    }
}
