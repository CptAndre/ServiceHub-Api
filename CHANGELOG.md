# Changelog

## Em preparação — Correções de dependências e inspeções (2026-11-14)

- Spring Boot 4.1.2 e versões corrigidas de Logback, Jackson, Commons Codec e JSON Smart.
- Substituição de android-json pela implementação mantida org.json no escopo de testes.
- Healthcheck com expansão de porta simplificada e fechamento explícito dos executores dos testes.
- Espera por bloqueio PostgreSQL com Awaitility e indicação dos schemas dinâmicos para a inspeção SQL da IDE.
- Alerta YAML sem correção oficial na linha Jackson 2 registrado no ADR 0004, com análise do uso atual.

## Em preparação — Aula 15 (2026-11-10)

- Métrica de transições confirmadas no banco, com cinco estados e scrape Prometheus exclusivo para ADMIN.
- `X-Request-Id` validado, correlação por MDC, propagação GraphQL e logs JSON no perfil prod.
- Saúde com detalhes restritos, informações públicas e allowlist de endpoints Actuator.
- Testes de autorização, rollback, correlação e proteção dos mapeamentos REST.
- ADRs de observabilidade e concorrência, com política de depreciação detalhada no README.

## 1.1.0 — 2026-11-07

- Contrato REST `/api/v1` e documento OpenAPI `/api-docs/v1`.
- Aliases `/api/...` preservados e sinalizados como depreciados, sem remoção agendada.
- Health e readiness com Actuator e validação de produção.
- CI com PostgreSQL 16, teste da imagem e promoção do mesmo artefato por SHA.
- Preparação de publicação no Render e política de compatibilidade registrada em ADR.

A versão 1.1.0 está preparada no repositório. A publicação e a comprovação dos jobs
no GitHub dependem de commit/push e configuração da plataforma. Não há deploy
público registrado nesta etapa.
