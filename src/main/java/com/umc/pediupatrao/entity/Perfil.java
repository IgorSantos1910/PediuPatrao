package com.umc.pediupatrao.entity;

// Perfis de acesso. Não existe hierarquia: o ADMIN não herda as permissões do GERENTE.
public enum Perfil {

    ADMIN("Administrador"),
    GERENTE("Gerente"),
    ATENDENTE("Atendente");

    private final String descricao;

    Perfil(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    // Converte o texto gravado no banco para o perfil. Retorna null se for inválido.
    public static Perfil de(String valor) {
        for (Perfil p : values()) {
            if (p.name().equalsIgnoreCase(valor)) {
                return p;
            }
        }
        return null;
    }
}
