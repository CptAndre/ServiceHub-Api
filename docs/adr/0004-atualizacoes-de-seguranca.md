# ADR 0004: Atualizações de segurança das dependências

Data: 14 de novembro de 2026.
Status: implementado, com alerta YAML pendente no fornecedor.

## Contexto e decisão

Os avisos do Maven apontavam versões com vulnerabilidades publicadas, incluindo
bibliotecas restritas aos testes. Atualizamos o parent para Spring Boot 4.1.2,
preservando Java 21, o contrato da API e as migrações SQL do professor.

O BOM do Boot atualiza Tomcat para 11.0.27, pgJDBC para 42.7.14 e Hibernate
Validator para 9.1.5.Final. Usamos as propriedades do próprio BOM para aplicar
correções adicionais ainda ausentes dessa versão do Boot:

| Família | Versão resolvida | Escopo |
| --- | --- | --- |
| Logback core/classic | 1.6.6 | Aplicação |
| Jackson 3 | 3.1.8 | Aplicação |
| Jackson 2 | 2.21.8 | Aplicação |
| Commons Codec | 1.23.0 | Testes |
| JSON Smart / Accessors Smart | 2.6.1 | Testes |
| ASM, transitivo de Accessors Smart | 9.11.1 | Testes |

As duas famílias Jackson continuam alinhadas por seus respectivos BOMs: Jackson 3
atende o Spring Boot; Jackson 2 permanece necessário para JJWT e Swagger.
Mantemos JSONassert estável em 1.5.3, excluindo android-json e fornecendo org.json
20260814 somente para testes. A comparação JSON do MockMvc verifica essa integração.

## Alerta que permanece: WS-2026-0134

O aviso GHSA-255r-36wv-4qpr afeta `YAMLAnchorReplayingParser`, disponível desde
Jackson YAML 2.19.0. O fornecedor informa que `YAMLFactory`, a fábrica padrão,
não é afetada. A correção existe na família 3, mas não foi incorporada ao código
das versões 2.21.8, 2.22.4 e 2.23.0 consultadas nesta data.

O projeto não usa `YAMLAnchorReplayingFactory` nem recebe YAML nos endpoints de
negócio. Swagger utiliza a fábrica padrão para gerar a documentação. Mantemos
Jackson YAML 2 alinhado com seu BOM para preservar a documentação da API e
registramos a pendência sem ocultar o alerta do analisador de dependências.
Essa avaliação vale para o uso atual; introduzir leitura de YAML externo exige
reavaliá-la. Aplicar uma correção oficial na linha 2 ou uma migração compatível
do Swagger para Jackson 3 permitirá remover a pendência.

Os avisos de tabelas SQL não resolvidas nos testes são independentes desse alerta:
os schemas são criados dinamicamente pelo Flyway. Suprimimos apenas a inspeção
`SqlResolve` na classe que compara esses schemas; o banco continua validando o SQL.

## Validação local

- `clean verify`: 97 testes, sem falhas, erros ou testes ignorados, com PostgreSQL 18.6 temporário.
- JAR no perfil `prod`: autenticação, REST, GraphQL, métricas ADMIN, correlação e privacidade dos logs.
- Artefato empacotado: versões de execução conferidas; bibliotecas JSON, ASM e Codec de testes ausentes do JAR.
- Healthcheck: expansão de porta verificada em shell, incluindo variáveis vazias e falha do curl.
- SQL e migrações do professor preservados; comparação do schema executada pela suíte.

O Docker não está disponível neste ambiente. A construção e execução da imagem,
assim como o caminho de Testcontainers, continuam dependendo do CI ou de um
ambiente com Docker. A suíte local usou PostgreSQL real externo ao Testcontainers.
A lista de inspeções do IntelliJ precisa ser atualizada após recarregar o Maven.

## Referências

- [Correções e compatibilidade do Logback](https://logback.qos.ch/news.html).
- [Correção de polimorfismo no Jackson](https://github.com/FasterXML/jackson-databind/security/advisories/GHSA-m8xf-2cr7-77ph).
- [JSON Smart 2.6.1 e atualização de ASM](https://github.com/netplex/json-smart-v2/blob/v2.6.1/README.md).
- [Histórico de correções do Commons Codec](https://commons.apache.org/proper/commons-codec/changes.html).
- [Alerta YAML do fornecedor](https://github.com/FasterXML/jackson-dataformats-text/security/advisories/GHSA-255r-36wv-4qpr).
