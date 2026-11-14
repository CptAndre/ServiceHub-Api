# ADR 0003: Transições de ordens com bloqueio no PostgreSQL

Data: 10 de novembro de 2026.
Status: aceito e implementado.

## Contexto

A leitura do status seguida de `save()` permitia que duas transações validassem
mudanças sobre o mesmo estado antigo. Reproduzimos uma ordem concluída, avaliada e
depois sobrescrita como cancelada por uma transação concorrente.

O schema das quatro tabelas centrais deve continuar igual ao SQL do professor.
Adicionar uma coluna de versionamento otimista mudaria esse contrato.

## Decisão

`ContratoService.atualizarStatus` inicia a leitura por
`ContratoRepository.findByIdForUpdate`, com `PESSIMISTIC_WRITE`, dentro da mesma
transação da alteração. O banco mantém o bloqueio até commit ou rollback. Uma
segunda transação aguarda e valida a transição sobre o estado então confirmado.

`COMPLETED` e `CANCELLED` são finais. Tentar sair deles retorna `409`, inclusive
quando a tentativa começou enquanto outra transação ainda estava em andamento.
A autorização por proprietário permanece no service.

## Consequências

As alterações da mesma ordem são serializadas pelo banco, inclusive com mais de
uma instância da API. Ordens diferentes continuam independentes. O schema de
referência permanece intacto.

Uma disputa pode aumentar o tempo de resposta. As transações devem ser curtas;
chamadas externas demoradas não devem ocorrer enquanto mantêm esse bloqueio.

O teste `ArchitecturePostgresIntegrationTest` abre duas transações reais, verifica
a espera no PostgreSQL e testa os dois estados finais como vencedores. A tentativa
concorrente recebe `409`, e a avaliação continua restrita a ordens concluídas.

## Referências

- Aula 15, slides 20–26: características, verificações automáticas e ADRs.
- [Bloqueios no Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html).
