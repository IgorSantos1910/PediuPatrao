# PediuPatrão

Atividade Prática M1 (Parte 2) - Engenharia de Software
Aluno: Igor Santos

O PediuPatrão é um sistema de pedidos de pizzaria feito em Java com Spring Boot, Spring Security, Thymeleaf e MongoDB.
Nessa atividade eu adicionei os perfis de acesso, o controle do andamento dos pedidos, o desconto, o cancelamento e a auditoria.

## Como rodar o projeto

Você vai precisar do JDK 21 e de um MongoDB.

O projeto já vem configurado pra usar o MongoDB da disciplina. Se quiser usar um MongoDB na sua máquina, é só definir essas variáveis antes de rodar:

```
MONGODB_URI=mongodb://localhost:27017
MONGODB_DATABASE=pizzaria_igorsantos
```

Pra subir o sistema:

```
mvnw spring-boot:run
```

(no Linux ou Mac é `./mvnw spring-boot:run`)

Depois abre o navegador em http://localhost:8080

Pra rodar os testes:

```
mvnw test
```

## Usuários pra testar

Esses usuários são criados sozinhos na primeira vez que o sistema roda:

- admin / teste123 (ADMIN)
- gerente01 / gerente123 (GERENTE)
- atendente01 / atendente123 (ATENDENTE)

## O que eu fiz

### RF01 - Usuários e permissões

Criei o enum `Perfil` com os três perfis: ADMIN, GERENTE e ATENDENTE. Quando a pessoa faz login, o `UsuarioDetailsService` pega o perfil dela no banco e transforma em role do Spring Security (tipo ROLE_GERENTE).

As permissões são conferidas no backend em dois lugares: no `SecurityConfig`, que libera ou bloqueia cada rota, e nos services, usando `@PreAuthorize`. Então não adianta só esconder o botão, se alguém chamar a rota direto o servidor bloqueia.

O que cada um pode fazer:

- ADMIN: cuida dos usuários e perfis, consulta os pedidos e vê a auditoria.
- GERENTE: mexe nos pedidos, dá desconto, cancela pedido, consulta clientes e vê a auditoria.
- ATENDENTE: cadastra e edita clientes, registra pedidos e muda o status.

Desconto e cancelamento são só do gerente (`hasRole('GERENTE')`), então nem o admin consegue fazer.

Antes qualquer um conseguia cadastrar usuário pela API. Agora só o admin consegue. Ele também não pode mudar o próprio perfil, e o último admin do sistema não pode ser apagado.

Também liguei o CSRF nas telas. A API usa HTTP Basic sem sessão, por isso lá não precisa do token.

### RF02 - Pedido

Agora o pedido guarda os itens (produto, quantidade e preço), o subtotal, o desconto, o total, quem registrou e a data/hora que entrou.

O total é calculado no servidor. Quem faz o pedido só manda o produto e a quantidade, o preço vem do cadastro de produtos. Assim ninguém consegue mandar um total diferente.

### RF03 - Status do pedido

O pedido segue essa ordem: RECEBIDO -> EM_PREPARACAO -> PRONTO -> SAIU_PARA_ENTREGA ou RETIRADO -> FINALIZADO.

Quem decide qual é o próximo status é o enum `StatusPedido`, no método `proximos()`. Não dá pra pular etapa e nem mexer num pedido que foi cancelado. Quando o pedido sai, fica gravado a data/hora e quem fez.

### RF04 - Desconto e cancelamento

Os dois só o gerente pode fazer, e só antes do pedido sair.

- Desconto: vai de 0% até 20%, e o total é calculado de novo.
- Cancelamento: precisa escrever uma justificativa (pelo menos 10 letras). Os dados do pedido continuam salvos, mas depois de cancelado ele não pode mais ser alterado.

### RF05 - Auditoria

Pra auditoria criei as classes `RegistroAuditoria`, `AuditoriaRepository` e `AuditoriaService`. Os registros ficam na coleção `auditoria` do MongoDB.

Cada registro guarda quem fez, o perfil, a data/hora, o tipo de operação, o que foi alterado (com o id), os campos que mudaram com o valor antigo e o novo, e a justificativa quando tem.

Fica registrado: pedido novo, alteração de pedido, mudança de status, desconto, cancelamento e alteração de cliente (endereço, telefone etc.). Também coloquei pra registrar mudanças em produtos e usuários.

O `AuditoriaRepository` só tem o `insert` e as consultas, então não tem como editar ou apagar um registro. A API da auditoria só aceita GET. Só o admin e o gerente conseguem ver.

A senha nunca vai pra auditoria. E quando o admin olha a auditoria, o telefone e o endereço do cliente aparecem escondidos (tipo 11*******77).

## API

A API usa HTTP Basic, ou seja, manda usuário e senha em cada requisição. Alguns testes que dá pra fazer no cmd:

```
# atendente tentando cancelar -> 403
curl -i -u atendente01:atendente123 -X POST http://localhost:8080/api/pedidos/ID/cancelamento -H "Content-Type: application/json" -d "{\"justificativa\":\"teste de acesso\"}"

# admin tentando dar desconto -> 403
curl -i -u admin:teste123 -X POST http://localhost:8080/api/pedidos/ID/desconto -H "Content-Type: application/json" -d "{\"percentual\":10}"

# gerente dando mais de 20% de desconto -> 422
curl -i -u gerente01:gerente123 -X POST http://localhost:8080/api/pedidos/ID/desconto -H "Content-Type: application/json" -d "{\"percentual\":30}"

# gerente tentando criar um admin -> 403
curl -i -u gerente01:gerente123 -X POST http://localhost:8080/api/usuarios -H "Content-Type: application/json" -d "{\"username\":\"novo\",\"password\":\"123456\",\"role\":\"ADMIN\"}"

# tentando apagar a auditoria -> 403
curl -i -u admin:teste123 -X DELETE http://localhost:8080/api/auditoria/qualquer
```

No lugar de ID coloca o código do pedido, que aparece na tela do pedido.

## Testes

Os testes ficam em `src/test`:

- `StatusPedidoTest`: testa a ordem dos status.
- `PedidoServiceTest`: testa o cálculo do total, a troca de status, o desconto e o cancelamento.
- `AuditoriaServiceTest`: testa a comparação do antes e depois e o mascaramento.
- `ClienteServiceTest`: testa se a mudança de endereço e telefone vai pra auditoria com o valor antigo e o novo.
- `UsuarioServiceTest`: testa as travas dos perfis (não mudar o próprio perfil, não rebaixar o último admin, perfil inválido) e se a senha não vai pra auditoria.
- `SegurancaTest`: testa se as tentativas de acesso sem permissão são bloqueadas, tanto na API quanto nas telas.
