package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    public static final String ENTIDADE = "Produto";

    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoriaService;

    public ProdutoService(ProdutoRepository produtoRepository, AuditoriaService auditoriaService) {
        this.produtoRepository = produtoRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Produto> listarProdutos() {
        return produtoRepository.findAll(Sort.by("tipo", "nome"));
    }

    @PreAuthorize("isAuthenticated()")
    public List<Produto> listarAtivos() {
        List<Produto> ativos = new ArrayList<>();
        for (Produto p : listarProdutos()) {
            if (!Boolean.FALSE.equals(p.getAtivo())) {
                ativos.add(p);
            }
        }
        return ativos;
    }

    @PreAuthorize("isAuthenticated()")
    public Optional<Produto> buscarPorId(String id) {
        return produtoRepository.findById(id);
    }

    // Preço é informação comercial: somente o GERENTE altera o cardápio, e toda mudança é auditada
    @PreAuthorize("hasRole('GERENTE')")
    public Produto salvar(Produto dados) {
        validar(dados);
        if (dados.getId() == null || dados.getId().isBlank()) {
            Produto novo = new Produto();
            copiarCampos(dados, novo);
            Produto salvo = produtoRepository.save(novo);
            auditoriaService.registrar(TipoOperacao.INCLUSAO_PRODUTO, ENTIDADE, salvo.getId(),
                    "Produto cadastrado: " + salvo.getNome(), AuditoriaService.comparar(Map.of(), retrato(salvo)), null);
            return salvo;
        }
        Produto existente = produtoRepository.findById(dados.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado para atualização."));
        Map<String, Object> antes = retrato(existente);
        copiarCampos(dados, existente);
        List<CampoAlterado> alteracoes = AuditoriaService.comparar(antes, retrato(existente));
        if (alteracoes.isEmpty()) {
            return existente;
        }
        Produto salvo = produtoRepository.save(existente);
        auditoriaService.registrar(TipoOperacao.ALTERACAO_PRODUTO, ENTIDADE, salvo.getId(),
                "Produto alterado: " + salvo.getNome(), alteracoes, null);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public void excluir(String id) {
        Produto existente = produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
        produtoRepository.deleteById(id);
        auditoriaService.registrar(TipoOperacao.EXCLUSAO_PRODUTO, ENTIDADE, id,
                "Produto excluído: " + existente.getNome(), AuditoriaService.comparar(retrato(existente), Map.of()), null);
    }

    private void validar(Produto p) {
        if (p == null || p.getNome() == null || p.getNome().isBlank()) {
            throw new RegraNegocioException("O nome do produto é obrigatório.");
        }
        if (p.getPreco() == null || p.getPreco() <= 0) {
            throw new RegraNegocioException("O preço do produto deve ser maior que zero.");
        }
    }

    private static void copiarCampos(Produto origem, Produto destino) {
        destino.setNome(origem.getNome().trim());
        destino.setTipo(origem.getTipo() == null ? null : origem.getTipo().trim());
        destino.setPreco(origem.getPreco());
        destino.setAtivo(origem.getAtivo() == null || origem.getAtivo());
    }

    private static Map<String, Object> retrato(Produto p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nome", p.getNome());
        m.put("tipo", p.getTipo());
        m.put("preco", p.getPreco());
        m.put("ativo", p.getAtivo());
        return m;
    }
}
