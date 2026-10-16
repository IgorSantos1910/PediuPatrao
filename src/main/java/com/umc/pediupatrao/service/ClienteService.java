package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    public static final String ENTIDADE = "Cliente";

    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    public ClienteService(ClienteRepository clienteRepository, AuditoriaService auditoriaService) {
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll(Sort.by("nome"));
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Método para salvar um novo cliente ou atualizar um cliente existente
    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente salvar(Cliente cliente) {
        if (cliente.getId() == null || cliente.getId().isBlank()) {
            return criar(cliente);
        }
        return atualizar(cliente.getId(), cliente);
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente criar(Cliente dados) {
        validar(dados);
        Cliente novo = new Cliente();
        copiarCampos(dados, novo);
        Cliente salvo = clienteRepository.save(novo);
        auditoriaService.registrar(TipoOperacao.INCLUSAO_CLIENTE, ENTIDADE, salvo.getId(),
                "Cliente cadastrado: " + salvo.getNome(), AuditoriaService.comparar(Map.of(), retrato(salvo)), null);
        return salvo;
    }

    // Atualiza apenas os campos cadastrais permitidos e audita cada campo alterado (com destaque para endereço e telefone), registrando o valor anterior e o novo
    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente atualizar(String id, Cliente dados) {
        validar(dados);
        Cliente existente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado para atualização."));
        Map<String, Object> antes = retrato(existente);
        copiarCampos(dados, existente);
        List<CampoAlterado> alteracoes = AuditoriaService.comparar(antes, retrato(existente));
        if (alteracoes.isEmpty()) {
            return existente;
        }
        Cliente salvo = clienteRepository.save(existente);
        auditoriaService.registrar(TipoOperacao.ALTERACAO_CLIENTE, ENTIDADE, salvo.getId(),
                "Cliente alterado: " + salvo.getNome(), alteracoes, null);
        return salvo;
    }

    // Método para excluir cliente
    @PreAuthorize("hasRole('GERENTE')")
    public void excluir(String id) {
        Cliente existente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
        clienteRepository.deleteById(id);
        auditoriaService.registrar(TipoOperacao.EXCLUSAO_CLIENTE, ENTIDADE, id,
                "Cliente excluído: " + existente.getNome(), AuditoriaService.comparar(retrato(existente), Map.of()), null);
    }

    private void validar(Cliente c) {
        if (c == null || vazio(c.getNome())) {
            throw new RegraNegocioException("O nome do cliente é obrigatório.");
        }
        if (vazio(c.getTelefone())) {
            throw new RegraNegocioException("O telefone do cliente é obrigatório.");
        }
        String digitos = c.getTelefone().replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 11) {
            throw new RegraNegocioException("Telefone inválido: informe DDD + número (10 ou 11 dígitos).");
        }
        if (!vazio(c.getEstado()) && c.getEstado().trim().length() != 2) {
            throw new RegraNegocioException("Informe a UF com 2 letras.");
        }
        if (!vazio(c.getCep()) && c.getCep().replaceAll("\\D", "").length() != 8) {
            throw new RegraNegocioException("CEP inválido: informe 8 dígitos.");
        }
    }

    // Copia somente os campos cadastrais (o id nunca vem da requisição).
    private static void copiarCampos(Cliente origem, Cliente destino) {
        destino.setNome(limpar(origem.getNome()));
        destino.setTelefone(limpar(origem.getTelefone()));
        destino.setCep(limpar(origem.getCep()));
        destino.setLogradouro(limpar(origem.getLogradouro()));
        destino.setNumero(limpar(origem.getNumero()));
        destino.setComplemento(limpar(origem.getComplemento()));
        destino.setBairro(limpar(origem.getBairro()));
        destino.setCidade(limpar(origem.getCidade()));
        destino.setEstado(origem.getEstado() == null ? null : limpar(origem.getEstado().toUpperCase()));
        if (origem.getEndereco() != null) {
            destino.setEndereco(limpar(origem.getEndereco()));
        }
    }

    static Map<String, Object> retrato(Cliente c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nome", c.getNome());
        m.put("telefone", c.getTelefone());
        m.put("cep", c.getCep());
        m.put("logradouro", c.getLogradouro());
        m.put("numero", c.getNumero());
        m.put("complemento", c.getComplemento());
        m.put("bairro", c.getBairro());
        m.put("cidade", c.getCidade());
        m.put("estado", c.getEstado());
        m.put("endereco", c.getEndereco());
        return m;
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private static String limpar(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
