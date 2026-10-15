# ServiceHub API

API REST para gerenciar clientes, prestadores de serviço, contratos, avaliações e pagamentos.

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
```

Substitua `sua-senha-do-postgresql` pela senha criada para o usuário `postgres` na sua instalação. Não informe nem grave senhas reais no repositório. As migrações do Flyway são executadas automaticamente.

## Executar

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

A documentação da API fica disponível em `http://localhost:8080/swagger-ui/index.html`.

## Segurança e exemplos

As credenciais abaixo são somente usuários em memória para demonstração local. As senhas
são codificadas com BCrypt em memória; elas não representam autenticação persistente:

- `user / user123`: role `USER`
- `admin / admin123`: roles `USER` e `ADMIN`

Somente `/api/hello`, `/api-docs/**` e `/swagger-ui/**` são públicos. Os demais
endpoints da API usam HTTP Basic. Alterações administrativas e exclusões exigem
`ADMIN`; leituras e criações exigem autenticação. Respostas não expõem entidades
JPA diretamente e as entradas textuais livres são normalizadas e escapadas para
HTML na camada de serviço.

O CPF do prestador é validado com formato e dígitos verificadores. Esta API de
exemplo não tem cadastro de contas nem associação de cada registro ao usuário
autenticado; portanto, não implementa autorização por propriedade do recurso
(BOLA). JWT e autenticação persistida ficam para a próxima etapa.

### Testes rápidos (PowerShell)

```powershell
# Público
curl.exe http://localhost:8080/api/hello

# Sem autenticação: 401
curl.exe http://localhost:8080/api/clientes

# Leitura autenticada e paginação (máximo size=50)
curl.exe -u user:user123 "http://localhost:8080/api/clientes?page=0&size=20"

# Criar cliente como USER
curl.exe -u user:user123 -H "Content-Type: application/json" `
  -d '{"nome":"Ana","email":"ana@example.com","telefone":"11999999999","documento":"12345678900"}' `
  http://localhost:8080/api/clientes

# Validação: 400
curl.exe -u user:user123 -H "Content-Type: application/json" `
  -d '{"nome":"","email":"invalido","telefone":"1"}' `
  http://localhost:8080/api/clientes

# Recurso inexistente: 404
curl.exe -u user:user123 http://localhost:8080/api/clientes/999999

# USER tentando alteração administrativa: 403
curl.exe -u user:user123 -X DELETE http://localhost:8080/api/clientes/1

# ADMIN pode excluir
curl.exe -u admin:admin123 -X DELETE http://localhost:8080/api/clientes/1

# Método não permitido: 405 (PATCH é permitido pelo firewall, mas não há rota PATCH)
curl.exe -u user:user123 -X PATCH http://localhost:8080/api/clientes
```

Os testes automatizados cobrem autenticação, bloqueio de operações administrativas
para `USER`, acesso para `ADMIN`, validação, CPF e sanitização. Para rodar a suíte,
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
- Cadastro e consulta de contratos.
- Cadastro e consulta de avaliações.
- Cadastro e consulta de pagamentos.
- Documentação OpenAPI/Swagger.
- Migrações versionadas do banco de dados.

### Banco de dados

As migrações estão em `src/main/resources/db/migration`:

1. `V1__Create_clientes_table.sql`
2. `V2__Create_montadores_table.sql` (estrutura histórica)
3. `V3__Create_contratos_table.sql`
4. `V4__Create_avaliacoes_table.sql`
5. `V5__Create_pagamentos_table.sql`
6. `V6__Add_contact_fields_to_clientes.sql`
7. `V7__Generalize_service_provider_domain.sql`

O Hibernate está configurado com `ddl-auto=validate`, portanto o schema deve ser criado pelas migrações do Flyway. As credenciais do banco são fornecidas por variáveis de ambiente e não devem ser gravadas no repositório.

### Organização do código

```text
src/main/java/com/example/servicehub_api
├── config          Configurações da aplicação e OpenAPI
├── controller      Endpoints REST
├── dto             Objetos de entrada e saída validados
├── exception      Exceções da aplicação
├── model           Entidades JPA
├── repository      Repositórios Spring Data
└── service         Regras de negócio
```

### Qualidade e validação

- Os testes automatizados de segurança, validação e sanitização estão em `src/test`.
- O arquivo `pom.xml` e as migrações SQL foram verificados.
- A execução dos testes depende de uma instalação configurada do Java 21.
- O ambiente atual não possui `JAVA_HOME` configurado; configure essa variável antes de executar `.\mvnw.cmd test`.
