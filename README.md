# PediuPatrão

Atividade Prática M1 (Parte 2) - Engenharia de Software
Aluno: Igor Santos

Sistema de pedidos de pizzaria feito com Java 21, Spring Boot, Spring Security, Thymeleaf e MongoDB.
Nesta atividade foram adicionados perfis de acesso, controle do andamento dos pedidos, desconto, cancelamento e auditoria.

## Como rodar

Precisa do JDK 21 e de um MongoDB.

Por padrão o projeto usa o MongoDB da disciplina. Para usar um MongoDB local, defina antes de rodar:

```
MONGODB_URI=mongodb://localhost:27017
MONGODB_DATABASE=pizzaria_igorsantos
```

Rodar a aplicação:

```
mvnw spring-boot:run
```

Depois é só acessar http://localhost:8080

Rodar os testes:

```
mvnw test
```

## Usuários de teste

Criados automaticamente na primeira execução:

- admin / teste123 (ADMIN)
- gerente01 / gerente123 (GERENTE)
- atendente01 / atendente123 (ATENDENTE)

## O que foi feito

### RF01 - Usuários e permissões

- Criei o enum `Perfil` com ADMIN, GERENTE e ATENDENTE.
- No login, o `UsuarioDetailsService` pega o perfil do usuário no banco e transforma em role (ex.: ROLE_GERENTE).
- As permissões são verificadas no backend em dois lugares:
  - no `SecurityConfig`, por rota;
  - nos services, com `@PreAuthorize`.
- O que cada perfil pode fazer:
  - ADMIN: usuários e perfis, consultar pedidos, ver auditoria.
  - GERENTE: pedidos, desconto, cancelamento, consultar clientes, ver auditoria.
  - ATENDENTE: cadastrar/editar clientes, registrar pedidos e mudar o status.
- Desconto e cancelamento usam `hasRole('GERENTE')`, então nem o ADMIN consegue.
- O cadastro de usuários pela API era público e agora só o ADMIN acessa. O ADMIN não pode mudar o próprio perfil, e o último ADMIN não pode ser removido.
- O CSRF foi ligado nas telas. A API usa HTTP Basic sem sessão, por isso não precisa do token.

### RF02 - Pedido

- O pedido agora tem itens (produto, quantidade, preço), subtotal, desconto, total, quem registrou e data/hora de entrada.
- O total é calculado no servidor. A requisição só manda o produto e a quantidade; o preço vem do cadastro de produtos.

### RF03 - Status do pedido

- Fluxo: RECEBIDO -> EM_PREPARACAO -> PRONTO -> SAIU_PARA_ENTREGA ou RETIRADO -> FINALIZADO.
- As transições permitidas estão no enum `StatusPedido`, método `proximos()`.
- Quando o pedido sai, é gravada a data/hora e o usuário responsável.
- Não dá para pular etapas nem mudar um pedido cancelado.

### RF04 - Desconto e cancelamento

- Os dois são só do GERENTE e só antes do pedido sair.
- Desconto: de 0% a 20%, e o total é recalculado.
- Cancelamento: precisa de justificativa (mínimo 10 caracteres). Os dados do pedido continuam salvos e depois disso o pedido não pode mais ser alterado.

### RF05 - Auditoria

- Classes: `RegistroAuditoria`, `AuditoriaRepository` e `AuditoriaService`, salvando na coleção `auditoria`.
- Cada registro guarda: usuário, perfil, data/hora, operação, entidade e id, campos alterados (valor anterior e novo) e justificativa.
- O que é auditado: inclusão e alteração de pedido, mudança de status, desconto, cancelamento e alterações de cliente (endereço, telefone etc.). Também alterações de produtos e usuários.
- O `AuditoriaRepository` só tem `insert` e consultas, então não tem como alterar ou apagar registro. A API da auditoria só aceita GET.
- Só ADMIN e GERENTE consultam.
- A senha nunca é gravada. Para o ADMIN, telefone e endereço aparecem mascarados (ex.: 11*******77).

## API

Usa HTTP Basic (usuário e senha em cada requisição). Exemplos:

```
# atendente tentando cancelar -> 403
curl -i -u atendente01:atendente123 -X POST http://localhost:8080/api/pedidos/ID/cancelamento -H "Content-Type: application/json" -d "{\"justificativa\":\"teste de acesso\"}"

# admin tentando dar desconto -> 403
curl -i -u admin:teste123 -X POST http://localhost:8080/api/pedidos/ID/desconto -H "Content-Type: application/json" -d "{\"percentual\":10}"

# gerente com desconto acima de 20% -> 422
curl -i -u gerente01:gerente123 -X POST http://localhost:8080/api/pedidos/ID/desconto -H "Content-Type: application/json" -d "{\"percentual\":30}"

# gerente tentando criar um admin -> 403
curl -i -u gerente01:gerente123 -X POST http://localhost:8080/api/usuarios -H "Content-Type: application/json" -d "{\"username\":\"novo\",\"password\":\"123456\",\"role\":\"ADMIN\"}"

# tentando apagar auditoria -> 403
curl -i -u admin:teste123 -X DELETE http://localhost:8080/api/auditoria/qualquer
```

O ID do pedido aparece na tela de detalhe do pedido (campo "Código").

## Testes

Em `src/test`:

- `StatusPedidoTest`: fluxo de status.
- `PedidoServiceTest`: cálculo do total, transições, desconto e cancelamento.
- `AuditoriaServiceTest`: comparação antes/depois e mascaramento.
- `SegurancaTest`: tentativas de acesso sem permissão na API e nas telas.
