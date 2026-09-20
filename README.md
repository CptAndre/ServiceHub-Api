# ServiceHub API

API REST para gerenciar clientes, montadores, contratos, avaliações e pagamentos.

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

A documentação da API fica disponível em `http://localhost:8080/index.html`.

## Status técnico do projeto

### Situação atual

O projeto está estruturado como uma aplicação Spring Boot com Java 21, Maven, PostgreSQL, Spring Data JPA e Flyway. A API possui camadas separadas de controladores, serviços, repositórios, entidades e configurações.

As funcionalidades principais implementadas são:

- Cadastro, consulta, atualização e exclusão de clientes.
- Cadastro, consulta, atualização e exclusão de montadores.
- Cadastro e consulta de contratos.
- Cadastro e consulta de avaliações.
- Cadastro e consulta de pagamentos.
- Documentação OpenAPI/Swagger.
- Migrações versionadas do banco de dados.

### Banco de dados

As migrações estão em `src/main/resources/db/migration`:

1. `V1__Create_clientes_table.sql`
2. `V2__Create_montadores_table.sql`
3. `V3__Create_contratos_table.sql`
4. `V4__Create_avaliacoes_table.sql`
5. `V5__Create_pagamentos_table.sql`

O Hibernate está configurado com `ddl-auto=validate`, portanto o schema deve ser criado pelas migrações do Flyway. As credenciais do banco são fornecidas por variáveis de ambiente e não devem ser gravadas no repositório.

### Organização do código

```text
src/main/java/com/example/servicehub_api
├── config          Configurações da aplicação e OpenAPI
├── controller      Endpoints REST
├── exception       Exceções da aplicação
├── model           Entidades JPA
├── repository      Repositórios Spring Data
└── service         Regras de negócio
```

### Qualidade e validação

- O teste de contexto está localizado em `src/test`.
- O arquivo `pom.xml` e as migrações SQL foram verificados.
- A execução dos testes depende de uma instalação configurada do Java 21.
- O ambiente atual não possui `JAVA_HOME` configurado; configure essa variável antes de executar `.\mvnw.cmd test`.
