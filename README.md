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

REST: `http://localhost:8080/api/...`
Swagger: `http://localhost:8080/swagger-ui/index.html`
GraphiQL: `http://localhost:8080/graphiql`
GraphQL: `POST http://localhost:8080/graphql`

## Cadastro e login

`POST /api/auth/register` é público e permite os perfis `CLIENT` e `PROVIDER`.
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

Faça login em `POST /api/auth/login` com `email` (ou `username`) e `password`.
Use `Authorization: Bearer <accessToken>` nas operações protegidas. O token deixa
de ser aceito se a conta estiver inativa ou seu perfil tiver mudado.

As rotas portuguesas existentes foram mantidas. Clientes e prestadores agora são
perfis da mesma tabela `users`. Os cadastros exigem e-mail e senha. Campos CPF e
documento saíram do contrato atual, acompanhando o schema do professor.

## Serviços, contratação e avaliação

Crie um serviço como seu prestador autenticado, em `POST /api/servicos`:

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

Contrate em `POST /api/contratos/bookings` com `clientId` e `serviceId`. Para incluir
agendamento e observações, use `POST /api/contratos` com `clienteId`, `servicoId`,
`scheduledAt` e `descricaoServico`. O cliente deve ser o autenticado, salvo ação de
ADMIN. O preço sempre vem do serviço e permanece na ordem após alterações futuras.

O prestador responsável altera o status com `PATCH /api/contratos/{id}`:
`PENDING` → `ACCEPTED` → `IN_PROGRESS` → `COMPLETED`. Também pode cancelar uma ordem
não finalizada com `CANCELLED`. Estados finais não podem ser reabertos.

`POST /api/avaliacoes` recebe `nota`, `comentario` e `contratoId`. Só o cliente da
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
variáveis, os testes específicos de PostgreSQL são ignorados. Docker não é necessário
quando há PostgreSQL local disponível.
