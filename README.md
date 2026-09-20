# ServiceHub API

API REST para gerenciar clientes, montadores, contratos, avaliações e pagamentos.

## Requisitos

- Java 21
- PostgreSQL

## Configuração

Defina as variáveis de ambiente usadas pela aplicação:

```text
DB_URL=jdbc:postgresql://localhost:5432/servicehub
DB_USERNAME=postgres
DB_PASSWORD=sua-senha
```

O banco `servicehub` deve existir antes de iniciar a aplicação. As migrações do Flyway são executadas automaticamente.

## Executar

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

A documentação da API fica disponível em `http://localhost:8080/docs`.

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

### Controle de versão

O repositório Git foi inicializado na pasta `servicehub-api`. Os diretórios `target/` e `.idea/`, além de configurações locais e arquivos `.env`, estão excluídos pelo `.gitignore`. Antes do primeiro push, ainda é necessário configurar o remote, criar o commit inicial e enviar a branch para o GitHub.
