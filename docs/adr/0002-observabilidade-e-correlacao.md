# ADR 0002: Observabilidade com acesso restrito e correlação segura

Data: 10 de novembro de 2026.
Status: aceito e implementado localmente.

## Contexto

A Aula 15 pede uma métrica de negócio, correlação de requisições e logs JSON.
Precisamos investigar falhas sem guardar senhas, tokens, corpos de requisição ou
dados pessoais nos logs. Uma tentativa revertida no banco também não deve aparecer
como transição concluída na métrica.

## Decisão

- Expomos somente `health`, `info`, `metrics` e `prometheus`. Saúde e informações
  básicas são públicas; detalhes da saúde e métricas exigem o perfil ADMIN.
- O filtro `RequestIdFilter` executa antes da segurança. Aceita `X-Request-Id` com
  1 a 64 caracteres de `A-Z`, `a-z`, `0-9`, ponto, sublinhado ou hífen. Nos demais
  casos gera UUID, devolve o id na resposta e limpa o MDC no `finally`. Mantém o
  mesmo id nos despachos assíncronos e de erro. Um `ThreadLocalAccessor` propaga
  somente esse campo para a execução GraphQL.
- O perfil `prod` usa o formato ECS nativo do Spring Boot. O desenvolvimento usa
  texto com `requestId`. Os logs HTTP contêm método, template da rota e status;
  os logs de transição contêm somente o estado de destino.
  As categorias de detalhes JDBC e parâmetros ficam desligadas em produção, pois
  o driver pode incluir valores pessoais. O handler mantém diagnóstico pelo tipo
  da falha e pelo id, com respostas de erro sem detalhes internos.
- O contador `servicehub.service_requests.transitions` usa exclusivamente a tag
  `to`, com os cinco valores de `RequestStatus`. A criação conta como `PENDING`.
  O service publica um evento interno; um listener `AFTER_COMMIT` incrementa o
  contador somente depois da confirmação no banco, tanto no REST quanto no GraphQL.

## Consequências

Há cinco séries de negócio, sem uma série por usuário, ordem ou requisição.
Operações inválidas e rollbacks não aumentam os contadores. As verificações de
acesso e correlação são testes executados pelo mesmo `mvnw verify` do CI.

Os contadores pertencem ao processo e reiniciam com ele. Servem para monitorar
eventos; o banco continua como fonte do histórico e um coletor externo pode guardar
as séries ao longo do tempo. Não há garantia de contabilização durável se o processo
terminar entre o commit e o incremento. Não são um livro de auditoria.

Essa entrega funciona localmente. A publicação e a coleta contínua em um ambiente
hospedado permanecem etapas posteriores.

## Referências

- Aula 15, slides 10–18 e 31–34.
- [Métricas no Spring Boot](https://docs.spring.io/spring-boot/reference/actuator/metrics.html).
- [Eventos vinculados à transação](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html).
