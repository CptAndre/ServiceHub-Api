# ADR 0001: CI, publicação e versionamento do contrato

Data: 7 de novembro de 2026.
Status: decisões implementadas no repositório; publicação aguardando conta e ação do titular.

## Contexto

A Aula 14 exige CI em push e pull request, PostgreSQL real, imagem validada, saúde
consistente, hospedagem e política de breaking changes. A branch atual chama-se
`master`; o material usa `main`. Os consumidores existentes ainda usam `/api/...`.

## Decisões

1. O GitHub Actions atende `main` e `master`. Java 21 executa `./mvnw -B verify`.
   O CI liga Testcontainers com PostgreSQL 16 e exige banco real em todos os testes
   de integração. Não troca PostgreSQL por H2 quando Docker falha.
2. O JAR aprovado passa ao job de containers. O estágio `prebuilt` do Dockerfile
   empacota esse mesmo JAR. O Compose espera os estados de saúde, e uma bancada
   HTTP testa a imagem em execução. Só depois essa imagem pode ser publicada no
   GHCR, identificada pelo SHA do commit. PRs não publicam pacotes.
3. A preparação usa Render como PaaS, com Postgres gerenciado na mesma região.
   A primeira publicação será manual, a partir da imagem validada. Assim, a
   entrega contínua deixa o artefato pronto e o titular decide quando promovê-lo.
   Não reconstruímos a aplicação na PaaS. Preferimos digest ao selecionar uma
   imagem para produção; tags SHA também identificam a revisão de origem.
4. `/actuator/health` é público e inclui a saúde do banco sem mostrar detalhes.
   Docker, Compose e PaaS consultam essa mesma rota. Prontidão fica disponível em
   `/actuator/health/readiness`.
5. O perfil `prod` requer segredo JWT próprio e credenciais do PostgreSQL, valida
   origens CORS explícitas e usa logs JSON. O `.env` local não entra nesse perfil.
6. O contrato REST está em `/api/v1`, documentado em `/api-docs/v1`. GraphQL mantém
   `/graphql`, com evolução compatível de schema. Os aliases REST `/api/...`
   continuam disponíveis e recebem aviso de depreciação. Não há data de remoção
   anunciada; `Sunset` só é enviado quando uma data for explicitamente definida.

## Breaking changes

Exigem nova versão principal do contrato: remover ou renomear campo/rota, alterar
tipo, tornar obrigatório um campo antes opcional, mudar status HTTP ou restringir
uma regra de uso aceita pelo contrato. Acrescentar valores a enums requer revisão,
pois clientes podem usar listas fechadas.

Campos opcionais e novas operações compatíveis podem entrar na v1. Correções de
defeitos compatíveis entram como PATCH. A versão completa MAJOR.MINOR.PATCH fica no
POM, no OpenAPI, em `/api/v1/info` e nos metadados da imagem. A URI carrega o MAJOR.

Antes de remover uma rota, registrar changelog, guia de migração e prazo de ao
menos 90 dias, configurar `LEGACY_API_SUNSET_DATE`, manter as versões durante o
prazo e avaliar os consumidores. A configuração não remove rotas automaticamente.

## Consequências e limites

O build falha antes da publicação se teste, health check ou bancada HTTP falhar.
A imagem é promovida sem recompilação. O estágio padrão do Dockerfile continua
permitindo build local a partir do código.

Mudanças destrutivas no banco podem impedir o rollback da aplicação. Migrações
aplicadas permanecem imutáveis; usar mudanças compatíveis em etapas quando duas
versões precisarem coexistir. A migração de domínio V10 deve terminar antes de
publicar esta versão sobre um banco antigo. Não usar blue-green entre a aplicação
anterior à V10 e a atual, pois a V10 renomeia suas tabelas.

A conta da PaaS, custos, credenciais, URL pública e execução real do pipeline no
GitHub ainda dependem do titular. O template não cria recursos. Planos gratuitos
servem à demonstração e têm limites; revisar o plano antes de criar o banco.

## Referências

- Aula 14, slides 11–13, 19–20, 24–31.
- [Blueprints Render](https://render.com/docs/blueprint-spec).
- [Promoção de imagens no Render](https://render.com/docs/deploying-an-image).
- [Limites do plano gratuito](https://render.com/docs/free).
- [Testcontainers PostgreSQL](https://java.testcontainers.org/modules/databases/postgres/).
- [Deprecation, RFC 9745](https://www.rfc-editor.org/rfc/rfc9745.html).
