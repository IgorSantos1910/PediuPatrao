package com.umc.pediupatrao.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class MongoInitConfig implements CommandLineRunner {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) throws Exception {
        if (!mongoTemplate.collectionExists("pedidos")) {
            mongoTemplate.createCollection("pedidos");
        }
        if (!mongoTemplate.collectionExists("clientes")) {
            mongoTemplate.createCollection("clientes");
        }
        if (!mongoTemplate.collectionExists("usuarios")) {
            mongoTemplate.createCollection("usuarios");
        }
        if (!mongoTemplate.collectionExists("produtos")) {
            mongoTemplate.createCollection("produtos");
        }
        if (!mongoTemplate.collectionExists("horariosAtendimento")) {
            mongoTemplate.createCollection("horariosAtendimento");
        }
        if (!mongoTemplate.collectionExists("auditoria")) {
            mongoTemplate.createCollection("auditoria");
        }

        // Índices: login único e consultas de auditoria por entidade e por data
        mongoTemplate.indexOps("usuarios").ensureIndex(new Index().on("username", Sort.Direction.ASC).unique());
        mongoTemplate.indexOps("auditoria").ensureIndex(new Index()
                .on("entidade", Sort.Direction.ASC).on("entidadeId", Sort.Direction.ASC).on("dataHora", Sort.Direction.DESC));
        mongoTemplate.indexOps("auditoria").ensureIndex(new Index().on("dataHora", Sort.Direction.DESC));
    }
}
