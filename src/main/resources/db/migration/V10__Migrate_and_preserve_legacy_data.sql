-- Conserva os dados antigos e passa a usar as quatro tabelas oficiais.
-- Contas históricas ficam inativas: o sistema antigo não possuía suas senhas.
-- Os perfis e documentos originais permanecem nas tabelas legacy_*.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM clientes WHERE length(nome) > 100)
       OR EXISTS (SELECT 1 FROM prestadores WHERE length(nome) > 100) THEN
        RAISE EXCEPTION 'Há nomes com mais de 100 caracteres. Ajuste os nomes antes de migrar; nenhum dado foi truncado.';
    END IF;
    IF EXISTS (
        SELECT email FROM (
            SELECT COALESCE(NULLIF(lower(btrim(email)), ''), 'legacy-client-' || id || '@servicehub.invalid') AS email FROM clientes
            UNION ALL
            SELECT 'legacy-provider-' || id || '@servicehub.invalid' FROM prestadores
        ) emails GROUP BY email HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'E-mails duplicados após normalização. Resolva os duplicados antes da migração.';
    END IF;
    IF EXISTS (SELECT 1 FROM servicos WHERE preco <= 0)
       OR EXISTS (SELECT 1 FROM contratos WHERE valor_total <= 0) THEN
        RAISE EXCEPTION 'Serviços ou contratos com preço inválido. Corrija os valores antes da migração.';
    END IF;
    IF EXISTS (SELECT 1 FROM contratos WHERE status NOT IN ('AGENDADO','EM_ANDAMENTO','CONCLUIDO','CANCELADO')) THEN
        RAISE EXCEPTION 'Status legado desconhecido. Corrija-o antes da migração.';
    END IF;
    IF EXISTS (SELECT contrato_id FROM avaliacoes GROUP BY contrato_id HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Há múltiplas avaliações por contrato. Resolva as duplicidades antes da migração.';
    END IF;
    IF EXISTS (SELECT 1 FROM contratos c JOIN servicos s ON s.id = c.servico_id WHERE s.prestador_id <> c.prestador_id) THEN
        RAISE EXCEPTION 'Contrato com prestador diferente do serviço. Corrija os vínculos antes da migração.';
    END IF;
END $$;

INSERT INTO users (id, full_name, email, password_hash, role, phone, is_active)
SELECT id, nome, COALESCE(NULLIF(lower(btrim(email)), ''), 'legacy-client-' || id || '@servicehub.invalid'),
       '$2a$10$LKWXLoqfSvuFzjaH/Q2JLe.4f6tG/nrtENcqlnNqbK8rYJ1SGniTy', 'CLIENT', telefone, FALSE
FROM clientes;

INSERT INTO users (id, full_name, email, password_hash, role, phone, bio, is_active)
SELECT (SELECT COALESCE(MAX(id), 0) FROM clientes) + id, nome,
       'legacy-provider-' || id || '@servicehub.invalid',
       '$2a$10$LKWXLoqfSvuFzjaH/Q2JLe.4f6tG/nrtENcqlnNqbK8rYJ1SGniTy', 'PROVIDER', telefone, especialidade, FALSE
FROM prestadores;

INSERT INTO services (id, title, description, price, category, provider_id)
SELECT id, titulo, descricao, preco, 'OUTROS', (SELECT COALESCE(MAX(id), 0) FROM clientes) + prestador_id
FROM servicos;

-- Contratos avulsos ganham um serviço histórico inativo para satisfazer service_id NOT NULL.
-- O texto integral continua em description e notes. O título é apenas um rótulo do registro.
INSERT INTO services (id, title, description, price, category, provider_id, is_active)
SELECT (SELECT COALESCE(MAX(id), 0) FROM servicos) + id, 'Contrato legado #' || id,
       descricao_servico, valor_total, 'OUTROS',
       (SELECT COALESCE(MAX(id), 0) FROM clientes) + prestador_id, FALSE
FROM contratos WHERE servico_id IS NULL;

INSERT INTO service_requests (id, service_id, client_id, status, notes, total_price)
SELECT id, COALESCE(servico_id, (SELECT COALESCE(MAX(id), 0) FROM servicos) + id), cliente_id,
       CASE status WHEN 'AGENDADO' THEN 'PENDING' WHEN 'EM_ANDAMENTO' THEN 'IN_PROGRESS'
                   WHEN 'CONCLUIDO' THEN 'COMPLETED' WHEN 'CANCELADO' THEN 'CANCELLED' END,
       descricao_servico, valor_total
FROM contratos;

INSERT INTO reviews (id, request_id, reviewer_id, rating, comment)
SELECT a.id, a.contrato_id, c.cliente_id, a.nota, a.comentario
FROM avaliacoes a JOIN contratos c ON c.id = a.contrato_id;

SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM users;
SELECT setval(pg_get_serial_sequence('services', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM services;
SELECT setval(pg_get_serial_sequence('service_requests', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM service_requests;
SELECT setval(pg_get_serial_sequence('reviews', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM reviews;

-- Pagamentos são uma extensão local; a referência oficial não oferece tabela equivalente.
ALTER TABLE pagamentos DROP CONSTRAINT fk_pagamento_contrato;
ALTER TABLE pagamentos ADD CONSTRAINT fk_pagamento_request FOREIGN KEY (contrato_id) REFERENCES service_requests(id) ON DELETE RESTRICT;

ALTER TABLE clientes RENAME TO legacy_clientes;
ALTER TABLE prestadores RENAME TO legacy_prestadores;
ALTER TABLE servicos RENAME TO legacy_servicos;
ALTER TABLE contratos RENAME TO legacy_contratos;
ALTER TABLE avaliacoes RENAME TO legacy_avaliacoes;
