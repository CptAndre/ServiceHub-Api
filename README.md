# ServiceHub API

API REST e GraphQL para gerenciar clientes, prestadores de serviço, contratos, avaliações e pagamentos.

## Requisitos

- Java 21
- PostgreSQL

## Configuração

Crie o banco de dados `servicehub` no PostgreSQL antes de iniciar a aplicação:

```sql
CREATE DATABASE servicehub;
```

Configure as variáveis de ambiente usando o usuário e a senha definidos na instalação local do PostgreSQL. A senha abaixo é apenas um exemplo e deve ser substituída pela senha do PostgreSQL da sua máquina:

```text
DB_URL=jdbc:postgresql://localhost:5432/servicehub
DB_USERNAME=postgres
DB_PASSWORD=sua-senha-do-postgresql
```

No Windows PowerShell, as variáveis podem ser configuradas para a sessão atual com:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/servicehub"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "sua-senha-do-postgresql"
$env:JWT_SECRET = "<chave-aleatoria-em-base64-com-no-minimo-32-bytes>"
```

Substitua `sua-senha-do-postgresql` pela senha criada para o usuário `postgres` na sua instalação. Não informe nem grave senhas reais no repositório. As migrações do Flyway são executadas automaticamente.

## Executar

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

A documentação REST fica disponível em `http://localhost:8080/swagger-ui/index.html`.
O playground GraphiQL fica em `http://localhost:8080/graphiql`; as operações GraphQL
são servidas em `POST http://localhost:8080/graphql` e exigem token JWT.

## Segurança e exemplos

O login em `POST /api/auth/login` valida as credenciais e retorna um JWT Bearer.
Os usuários abaixo são contas de demonstração em memória (senhas codificadas com
BCrypt); não representam autenticação persistente nem devem ser usadas em produção:

- `admin / admin123`: role `ADMIN`
- `provider / provider123`: role `PROVIDER`
- `client / client123`: role `CLIENT`

Somente `/api/auth/login`, `/api/hello`, `/api-docs/**`, `/swagger-ui/**` e a
interface `/graphiql` são públicos. As operações em `/graphql` e os demais
endpoints protegidos exigem `Authorization: Bearer <token>` e aplicam regras de
acesso por role. A chave JWT local é apenas para demonstração. Configure
`JWT_SECRET` com uma chave aleatória em Base64 de pelo menos 256 bits e ajuste
`JWT_EXPIRATION_MS` no ambiente; não use o valor de exemplo em ambientes
compartilhados ou de produção.

Respostas não expõem entidades JPA diretamente e as entradas textuais livres são
normalizadas e escapadas para HTML na camada de serviço. O CPF do prestador é
validado com formato e dígitos verificadores. O projeto ainda usa usuários em
memória e não associa cada registro ao usuário autenticado, portanto a autorização
por propriedade do recurso (BOLA) não está implementada.

### Testes rápidos (PowerShell)

```powershell
# Público
curl.exe http://localhost:8080/api/hello

# Sem autenticação: 401
curl.exe http://localhost:8080/api/clientes

# Obter token de cliente
$login = curl.exe -s -H "Content-Type: application/json" `
  -d '{"username":"client","password":"client123"}' `
  http://localhost:8080/api/auth/login | ConvertFrom-Json
$token = $login.accessToken

# Leitura autenticada e paginação (máximo size=50)
curl.exe -H "Authorization: Bearer $token" "http://localhost:8080/api/prestadores?page=0&size=20"

# Criar cliente como CLIENT
curl.exe -H "Authorization: Bearer $token" -H "Content-Type: application/json" `
  -d '{"nome":"Ana","email":"ana@example.com","telefone":"11999999999","documento":"12345678900"}' `
  http://localhost:8080/api/clientes

# Validação: 400
curl.exe -H "Authorization: Bearer $token" -H "Content-Type: application/json" `
  -d '{"nome":"","email":"invalido","telefone":"1"}' `
  http://localhost:8080/api/clientes

# Recurso inexistente: 404
curl.exe -H "Authorization: Bearer $token" http://localhost:8080/api/clientes/999999

# CLIENT tentando alteração administrativa: 403
curl.exe -H "Authorization: Bearer $token" -X DELETE http://localhost:8080/api/clientes/1

# GraphQL: prestadores por especialidade
$graphqlBody = @{ query = 'query { providers(specialty: "Limpeza") { id name specialty } }' } | ConvertTo-Json -Compress
curl.exe -H "Authorization: Bearer $token" -H "Content-Type: application/json" `
  -d $graphqlBody http://localhost:8080/graphql

# GraphQL: serviço do prestador (PROVIDER token necessário para criar)
$providerLogin = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json" -Body (@{ username = "provider"; password = "provider123" } | ConvertTo-Json)
$providerToken = $providerLogin.accessToken
$serviceBody = @{ query = 'mutation { createService(input: { providerId: "1", title: "Limpeza residencial", description: "Limpeza de apartamento", price: 150.00 }) { id title price providerId } }' } | ConvertTo-Json -Compress
curl.exe -H "Authorization: Bearer $providerToken" -H "Content-Type: application/json" `
  -d $serviceBody http://localhost:8080/graphql

# Reservar serviço como cliente
$bookingBody = @{ query = 'mutation { bookService(input: { clientId: "6", serviceId: "1" }) { id description totalValue status clientId providerId serviceId } }' } | ConvertTo-Json -Compress
curl.exe -H "Authorization: Bearer $token" -H "Content-Type: application/json" `
  -d $bookingBody http://localhost:8080/graphql

# ADMIN pode excluir (obtenha seu token com /api/auth/login)
$adminLogin = curl.exe -s -H "Content-Type: application/json" `
  -d '{"username":"admin","password":"admin123"}' `
  http://localhost:8080/api/auth/login | ConvertFrom-Json
curl.exe -H "Authorization: Bearer $($adminLogin.accessToken)" -X DELETE http://localhost:8080/api/clientes/1

# Método não permitido: 405 (PATCH é permitido pelo firewall, mas não há rota PATCH)
curl.exe -H "Authorization: Bearer $token" -X PATCH http://localhost:8080/api/clientes
```

Os testes automatizados existentes cobrem segurança, validação, CPF e sanitização.
Para rodar a suíte,
configure Java 21 e PostgreSQL conforme os requisitos acima e execute `.\mvnw.cmd test`.

Para testar sanitização, envie `"<script>alert(1)</script>"` em `nome`,
`descricaoServico` ou `comentario`; o valor persistido e retornado será escapado
(`&lt;script&gt;...`). Não use dados reais nos testes.

### CORS e limites

Defina uma lista explícita de origens, separada por vírgulas, sem `*`:

```powershell
$env:APP_CORS_ALLOWED_ORIGINS = "http://localhost:3000,https://app.exemplo.test"
$env:MAX_PAYLOAD_SIZE_BYTES = "1048576"
$env:MAX_REQUEST_SIZE = "1MB"
```

Credenciais CORS ficam habilitadas apenas para essas origens. O limite de página
é `size <= 50`; payloads JSON usam `MAX_PAYLOAD_SIZE_BYTES`, enquanto formulários
multipart usam `MAX_REQUEST_SIZE` e `MAX_FILE_SIZE`.

## Status técnico do projeto

### Situação atual

O projeto está estruturado como uma aplicação Spring Boot com Java 21, Maven, PostgreSQL, Spring Data JPA e Flyway. A API possui camadas separadas de controladores, serviços, repositórios, entidades e configurações.

As funcionalidades principais implementadas são:

- Cadastro, consulta, atualização e exclusão de clientes.
- Cadastro, consulta, atualização e exclusão de prestadores de serviço.
- Cadastro e consulta de serviços vinculados a prestadores.
- Cadastro e consulta de contratos.
- Cadastro e consulta de avaliações.
- Cadastro e consulta de pagamentos.
- Documentação OpenAPI/Swagger.
- Migrações versionadas do banco de dados.
- Login JWT stateless com roles `ADMIN`, `PROVIDER` e `CLIENT`.
- Endpoint GraphQL com consultas de prestadores e contratos e mutation de contratação.
- Queries e mutations GraphQL para serviços e reservas, além de campos de serviço em prestadores.

### Banco de dados

As migrações estão em `src/main/resources/db/migration`:

1. `V1__Create_clientes_table.sql`
2. `V2__Create_montadores_table.sql` (estrutura histórica)
3. `V3__Create_contratos_table.sql`
4. `V4__Create_avaliacoes_table.sql`
5. `V5__Create_pagamentos_table.sql`
6. `V6__Add_contact_fields_to_clientes.sql`
7. `V7__Generalize_service_provider_domain.sql`
8. `V8__Create_servicos_table.sql`
8. `V8__Create_servicos_table.sql`

O Hibernate está configurado com `ddl-auto=validate`, portanto o schema deve ser criado pelas migrações do Flyway. As credenciais do banco são fornecidas por variáveis de ambiente e não devem ser gravadas no repositório.

### Organização do código

```text
src/main/java/com/example/servicehub_api
├── config          Configurações da aplicação e OpenAPI
├── controller      Endpoints REST
├── dto             Objetos de entrada e saída validados
├── exception      Exceções da aplicação
├── graphql        Queries e mutations GraphQL
├── model           Entidades JPA
├── repository      Repositórios Spring Data
└── service         Regras de negócio
```

### Qualidade e validação

- Os testes automatizados de segurança, validação e sanitização estão em `src/test`.
- O arquivo `pom.xml` e as migrações SQL foram verificados.
- A execução dos testes depende de uma instalação configurada do Java 21.
- O ambiente atual não possui `JAVA_HOME` configurado; configure essa variável antes de executar `.\mvnw.cmd test`.
