# ServiceHub API

API REST e GraphQL com Java 21, Spring Boot, PostgreSQL e Flyway.

## Modelo de referência do professor

As migrações V1 a V8 foram mantidas. A V9 cria o schema oficial de `servicehub_schema.sql`:

- `users`: clientes, prestadores e administradores, e-mail normalizado e senha BCrypt.
- `services`: serviço, categoria, preço, prestador e exclusão lógica.
- `service_requests`: ordens com preço histórico, agendamento e máquina de estados.
- `reviews`: uma avaliação por ordem concluída, com autoria e nota de 1 a 5.

A V10 transfere os registros antigos e conserva as cinco tabelas de origem como
`legacy_*`. CPF e documento ficam nessas tabelas históricas, pois não existem no
schema oficial. `pagamentos` continua como extensão local e passa a referenciar
`service_requests`. O schema das quatro tabelas centrais é comparado com uma cópia
integral do SQL do professor nos testes de PostgreSQL.

Clientes mantêm seus IDs. Prestadores recebem IDs de usuário acima do maior ID de
cliente, evitando colisões. Contratos e avaliações mantêm os IDs. Serviços sem
categoria prévia entram como `OUTROS`. Contratos avulsos ganham um serviço histórico
inativo. Os instantes de criação desses registros novos correspondem à migração,
pois a estrutura anterior não registrava todos esses instantes.

Contas migradas ficam **inativas**, com hash de senha aleatória sem senha recuperável.
Um administrador deve atribuir e-mail e senha reais e informar `ativo: true` na
atualização do cliente/prestador para habilitar a conta. Prestadores sem e-mail antigo
recebem um endereço reservado sob `servicehub.invalid`. Nenhuma credencial antiga de
demonstração é mantida. E-mails duplicados, nomes incompatíveis, preços inválidos,
avaliações repetidas ou vínculos inconsistentes interrompem a migração com mensagem,
sem descartar dados. Corrija a origem e tente novamente.

Não execute o SQL de referência diretamente sobre o banco do projeto. Inicie a
aplicação para o Flyway aplicar as novas migrações. Faça uma cópia do banco antes de
migrar um ambiente com dados importantes.

## Configuração e execução

Requer Java 21 e um banco PostgreSQL vazio ou já gerenciado pelas migrações V1–V8.
Crie o banco `servicehub` e configure as variáveis na sessão PowerShell ou em `.env`:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/servicehub"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "<senha-local>"
$env:JWT_SECRET = "<chave-aleatoria-Base64-com-pelo-menos-32-bytes>"
.\mvnw.cmd spring-boot:run
```

Opcionalmente, defina `SERVICEHUB_ADMIN_EMAIL` e `SERVICEHUB_ADMIN_PASSWORD` para
criar o primeiro administrador. A senha deve ter pelo menos 12 caracteres e no
máximo 72 bytes UTF-8. O bootstrap não altera a senha de contas existentes nem
promove uma conta de outro perfil. Não versione valores reais.

Para Docker, copie `.env.example` para `.env`, preencha as variáveis e execute:

```powershell
docker compose up -d --build
docker compose logs -f api
```

O Compose mantém o PostgreSQL 15 usado anteriormente para não tornar um volume
existente incompatível por uma troca automática de versão principal. O schema de
referência foi escrito para PostgreSQL 16 e usa recursos disponíveis também no 15.
Atualizar a versão do servidor exige exportação/restauração ou upgrade apropriado
para o volume existente. Não remova o volume para atualizar o schema.

REST v1: `http://localhost:8080/api/v1/...`

Informações: `http://localhost:8080/api/v1/info`

Saúde: `http://localhost:8080/actuator/health`
Swagger: `http://localhost:8080/swagger-ui/index.html`
GraphiQL: `http://localhost:8080/graphiql`
GraphQL: `POST http://localhost:8080/graphql`

## Cadastro e login

`POST /api/v1/auth/register` é público e permite os perfis `CLIENT` e `PROVIDER`.
O cadastro público não permite criar `ADMIN`.

```json
{
  "nome": "Ana Silva",
  "email": "ana@example.com",
  "telefone": "11999999999",
  "password": "<senha-propria-de-pelo-menos-12-caracteres>",
  "role": "CLIENT"
}
```

Faça login em `POST /api/v1/auth/login` com `email` (ou `username`) e `password`.
Use `Authorization: Bearer <accessToken>` nas operações protegidas. O token deixa
de ser aceito se a conta estiver inativa ou seu perfil tiver mudado.

As rotas portuguesas existentes foram mantidas como aliases depreciados; o contrato
principal usa `/api/v1`. Clientes e prestadores agora são
perfis da mesma tabela `users`. Os cadastros exigem e-mail e senha. Campos CPF e
documento saíram do contrato atual, acompanhando o schema do professor.

## Serviços, contratação e avaliação

Crie um serviço como seu prestador autenticado, em `POST /api/v1/servicos`:

```json
{
  "titulo": "Suporte técnico",
  "descricao": "Atendimento residencial",
  "preco": 150.00,
  "categoria": "SUPORTE_TECNICO",
  "prestadorId": 2
}
```

Os IDs são exemplos. Use os IDs retornados pelo cadastro. Consulte as 21 categorias
em `ServiceCategory`. Também são aceitos os aliases `title`, `description`, `price`,
`category` e `providerId`.

Contrate em `POST /api/v1/contratos/bookings` com `clientId` e `serviceId`. Para incluir
agendamento e observações, use `POST /api/v1/contratos` com `clienteId`, `servicoId`,
`scheduledAt` e `descricaoServico`. O cliente deve ser o autenticado, salvo ação de
ADMIN. O preço sempre vem do serviço e permanece na ordem após alterações futuras.

O prestador responsável altera o status com `PATCH /api/v1/contratos/{id}`:
`PENDING` → `ACCEPTED` → `IN_PROGRESS` → `COMPLETED`. Também pode cancelar uma ordem
não finalizada com `CANCELLED`. Estados finais não podem ser reabertos.

`POST /api/v1/avaliacoes` recebe `nota`, `comentario` e `contratoId`. Só o cliente da
ordem, ou ADMIN em seu nome, pode avaliar uma ordem concluída, uma única vez.

Exclusões de contas e serviços são lógicas. Ordens ativas bloqueiam a exclusão.
Os textos da API são JSON e permanecem em texto original, sem codificação HTML
persistida e sem escape repetido. Interfaces que exibem esses valores devem usar
renderização de texto ou codificação adequada ao contexto.

A criação GraphQL exige `category` no input `createService`. A mutation `bookService`
usará os mesmos usuários, serviços, verificações de acesso e regras do REST.

## Testes

```powershell
.\mvnw.cmd test
```

Os testes comuns usam H2. Para executar também a comparação do schema, as migrações
com dados legados e o fluxo completo da API com Flyway e `ddl-auto=validate`, indique
um **banco PostgreSQL exclusivo de teste e inicialmente vazio**:

```powershell
$env:SERVICEHUB_TEST_DB_URL = "jdbc:postgresql://localhost:55432/servicehub_test"
$env:SERVICEHUB_TEST_DB_USER = "servicehub_test"
$env:SERVICEHUB_TEST_DB_PASSWORD = "<senha-de-teste>"
.\mvnw.cmd clean verify
```

Esses testes criam tabelas no schema `public` e schemas temporários de validação.
Nunca aponte as variáveis `SERVICEHUB_TEST_DB_*` para um banco de uso real. Sem essas
variáveis nem `SERVICEHUB_TESTCONTAINERS=true`, os testes específicos de PostgreSQL
são ignorados. Os demais testes locais continuam disponíveis com H2. Docker não é necessário
quando há PostgreSQL local disponível.

## CI/CD, produção e versionamento — Aula 14

O pipeline está em `.github/workflows/ci.yml` e atende pushes/PRs em `main` e
`master`, preservando o nome atual da branch. Ele roda `./mvnw -B verify` com Java 21
e Testcontainers/PostgreSQL 16. Todos os testes de integração usam PostgreSQL no CI;
se Docker ou o banco falhar, o build falha em vez de usar H2.

O JAR aprovado é empacotado pelo estágio `prebuilt` do Dockerfile. O segundo job
sobe API e PostgreSQL, espera saúde com timeout e executa a bancada HTTP. A imagem
que passou é salva e, em push na branch principal, publicada no GHCR com o SHA do
commit. PRs não publicam imagens. O deploy promove essa imagem, sem recompilar.

Em máquina com Docker, executar a suíte completa com:

```powershell
$env:SERVICEHUB_TESTCONTAINERS = "true"
$env:SERVICEHUB_REQUIRE_POSTGRES = "true"
.\mvnw.cmd verify
```

Para verificar uma API em execução, a bancada usa somente a biblioteca padrão do
Python e funciona também contra a URL pública:

```powershell
python scripts/smoke_test.py --base-url http://localhost:8080 --expected-version 1.1.0
```

A opção `--write` demonstra cadastro, JWT, serviço, contratação, estados, avaliação
única e exclusão lógica. Ela cria dados fictícios e mantém registros históricos.

O perfil `prod` exige segredo JWT próprio, credenciais do PostgreSQL e origens CORS
explícitas, escreve logs JSON e não importa o `.env` local. `DB_USER`/`DB_PASS` e
`CORS_ALLOWED_ORIGINS` seguem os nomes da aula; os aliases anteriores continuam
aceitos. `SERVER_PORT` tem precedência sobre `PORT`.

O OpenAPI v1 está em `/api-docs/v1` e configura o Bearer JWT no Swagger. POM, OpenAPI
e `/api/v1/info` usam a mesma versão. Mudanças compatíveis entram na v1; alterações
incompatíveis exigem outra versão principal. A política está no
[ADR 0001](docs/adr/0001-ci-deploy-versionamento.md) e no [changelog](CHANGELOG.md).

As rotas antigas enviam `Deprecation` no formato de data da RFC 9745. `Sunset` só é
enviado se `LEGACY_API_SUNSET_DATE` for configurado, com ao menos 90 dias após
`LEGACY_API_DEPRECATION_DATE`. Nenhuma rota é removida automaticamente.

**Publicação pendente:** nenhuma URL pública foi criada. O próximo passo exige
criar a conta de hospedagem para a conta Render, variáveis, limites dos planos e evidência de CI vermelho/verde.
O arquivo `deploy/render.yaml.example` é um template, não uma contratação ativa.

## Governança e observabilidade — Aula 15

A entrega pode ser demonstrada localmente com a API e o PostgreSQL. O Actuator
expõe somente estes endpoints:

| Endpoint | Acesso | Conteúdo |
| --- | --- | --- |
| `/actuator/health` e subgrupos | Público | Estado de saúde; detalhes apenas para ADMIN |
| `/actuator/info` | Público | Nome e versão da aplicação |
| `/actuator/metrics` | ADMIN | Consulta das métricas pelo nome Micrometer |
| `/actuator/prometheus` | ADMIN | Séries no formato de coleta Prometheus |

Sem token, métricas retornam `401`; com CLIENT ou PROVIDER, retornam `403`.
Endpoints como `env`, `configprops` e `heapdump` permanecem fora da exposição web.
Use um token obtido pelo login da conta ADMIN criada pelo bootstrap:

```powershell
curl.exe -H "Authorization: Bearer <token-ADMIN>" http://localhost:8080/actuator/prometheus
```

A métrica de negócio é `servicehub.service_requests.transitions`. Na saída
Prometheus aparece como `servicehub_service_requests_transitions_total`, com a
única tag `to`: `PENDING`, `ACCEPTED`, `IN_PROGRESS`, `COMPLETED` ou `CANCELLED`.
A criação e as mudanças válidas contam somente após commit. Transições rejeitadas
e transações revertidas não contam. A métrica soma eventos, não a quantidade atual
de ordens em cada estado, e reinicia quando o processo reinicia.

Uma consulta para acompanhar a taxa por estado de destino é:

```promql
sum by (to) (rate(servicehub_service_requests_transitions_total[5m]))
```

Toda resposta processada pela aplicação recebe `X-Request-Id`, inclusive os erros
de autenticação e autorização. Um id enviado pelo cliente é aceito quando contém
de 1 a 64 letras ASCII, números, pontos, sublinhados ou hífens; caso contrário,
geramos um UUID. O mesmo id acompanha o MDC e os despachos assíncronos/de erro,
e o contexto é limpo ao final. O CORS permite enviar e ler esse header.

```powershell
curl.exe -i -H "X-Request-Id: demonstracao-aula15" http://localhost:8080/api/v1/info
```

No desenvolvimento, o id aparece entre colchetes no terminal. Com
`SPRING_PROFILES_ACTIVE=prod`, os logs são JSON ECS com `requestId` como campo.
No Compose, o perfil local continua sendo o padrão; para demonstrar JSON, configure
`SPRING_PROFILES_ACTIVE=prod` no ambiente ou no `.env`, com segredo JWT próprio e
as demais variáveis exigidas pelo perfil. O exemplo de segredo de desenvolvimento
não é aceito em `prod`.
O perfil pode ser executado localmente usando as variáveis de produção já
documentadas; publicar no Render não é pré-requisito. Os logs da aplicação não
registram corpos, query strings, credenciais ou tokens. Para depurar SQL localmente,
use o logger `org.hibernate.SQL`, mantendo `spring.jpa.show-sql=false` para preservar
a correlação e sem habilitar logs de parâmetros com dados pessoais.
Falhas inesperadas registram o tipo da exceção com o mesmo id, sem copiar sua
mensagem interna ou despejar valores de entrada no log.
Em `prod`, os detalhes brutos de erro/aviso JDBC e os logs de parâmetros também
ficam desativados; o handler registra conflitos e falhas com o identificador seguro.

Os testes validam o acesso ao Actuator, a série de negócio no scrape autenticado,
o rollback sem incremento, a correlação REST/GraphQL e a limpeza do MDC. Uma
verificação também percorre os mapeamentos REST protegidos e exige `401` sem token.

As decisões registradas são:

- [ADR 0001 — CI, publicação e versionamento](docs/adr/0001-ci-deploy-versionamento.md).
- [ADR 0002 — Observabilidade e correlação](docs/adr/0002-observabilidade-e-correlacao.md).
- [ADR 0003 — Concorrência das ordens](docs/adr/0003-concorrencia-das-ordens.md).
- [ADR 0004 — Atualizações de segurança e pendência YAML](docs/adr/0004-atualizacoes-de-seguranca.md).

O contrato de erro atual continua sendo `ApiError` (`timestamp`, `status`, `erro`,
`mensagens`). O handler usa a base de exceções HTTP do Spring e preserva status e
headers; uma eventual adoção de `ProblemDetail` exige revisão de compatibilidade.
Os ADRs descrevem estas escolhas do projeto, sem copiar decisões ainda não adotadas
da implementação de referência do professor.

## Política de depreciação

O contrato ativo é `/api/v1`. Os aliases sem versão, `/api/...`, encaminham para as
mesmas operações e já aparecem como depreciados no OpenAPI. A v1 não tem remoção
agendada e ainda não existe uma v2.

| Header dos aliases antigos | Significado |
| --- | --- |
| `Deprecation` | Data configurada em `LEGACY_API_DEPRECATION_DATE`, em segundos Unix precedidos por `@` |
| `Sunset` | Data HTTP de encerramento, enviada somente quando `LEGACY_API_SUNSET_DATE` estiver definida |
| `Link` | `/api-docs/v1` com `rel="describedby"`, apontando para o contrato de destino |

Uma remoção futura seguirá este processo:

1. Publicar no changelog e no README o motivo, as rotas afetadas e o guia de migração.
2. Comunicar o anúncio e conceder pelo menos 90 dias de convivência antes da retirada.
   Configurar as datas de forma coerente com esse prazo; o código também rejeita
   `Sunset` inferior a 90 dias após a data de depreciação.
3. Manter as rotas durante a convivência e verificar seu uso com as métricas HTTP
   por template de URI, sem colocar ids de consumidores nas tags.
4. Entregar a retirada explicitamente, com resposta `410 Gone` e referência ao guia
   de migração. Configurar `Sunset` sozinho não desliga endpoints automaticamente.

Enquanto não houver um anúncio de retirada, `Sunset` fica vazio. Mudanças
incompatíveis na v1 exigem outra versão principal e uma política de convivência.
