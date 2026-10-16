package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

// Cria um usuário de cada perfil e alguns produtos quando o banco está vazio
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, ProdutoRepository produtoRepository, PasswordEncoder encoder) {
        return args -> {
            criarUsuario(repo, encoder, "admin", "teste123", Perfil.ADMIN);
            criarUsuario(repo, encoder, "gerente01", "gerente123", Perfil.GERENTE);
            criarUsuario(repo, encoder, "atendente01", "atendente123", Perfil.ATENDENTE);

            if (produtoRepository.count() == 0) {
                criarProduto(produtoRepository, "Pizza Calabresa", "Pizza", 49.90);
                criarProduto(produtoRepository, "Pizza Mussarela", "Pizza", 45.00);
                criarProduto(produtoRepository, "Pizza Portuguesa", "Pizza", 54.90);
                criarProduto(produtoRepository, "Esfiha de Carne", "Esfiha", 6.50);
                criarProduto(produtoRepository, "Refrigerante 2L", "Bebida", 14.00);
            }
        };
    }

    private void criarUsuario(UsuarioRepository repo, PasswordEncoder encoder, String username, String senha, Perfil perfil) {
        Usuario existente = repo.findByUsername(username).orElse(null);

        if (existente == null) {
            Usuario user = new Usuario();
            user.setUsername(username);
            user.setPassword(encoder.encode(senha));
            user.setRole(perfil.name());
            repo.save(user);
            System.out.println("Usuário " + username + " criado!");
        } else if (existente.getPerfil() == null && perfil == Perfil.ADMIN) {
            // o admin do projeto original era criado sem perfil
            existente.setRole(Perfil.ADMIN.name());
            repo.save(existente);
        }
    }

    private void criarProduto(ProdutoRepository repo, String nome, String tipo, double preco) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setTipo(tipo);
        p.setPreco(preco);
        p.setAtivo(true);
        repo.save(p);
    }
}
