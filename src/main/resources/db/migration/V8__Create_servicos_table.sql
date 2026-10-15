CREATE TABLE servicos (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    descricao VARCHAR(2000),
    preco NUMERIC(10, 2) NOT NULL,
    prestador_id BIGINT NOT NULL,
    CONSTRAINT fk_servico_prestador FOREIGN KEY (prestador_id) REFERENCES prestadores(id)
);

CREATE INDEX idx_servicos_prestador_id ON servicos(prestador_id);

ALTER TABLE contratos
    ADD COLUMN servico_id BIGINT;

ALTER TABLE contratos
    ADD CONSTRAINT fk_contrato_servico FOREIGN KEY (servico_id) REFERENCES servicos(id);

CREATE INDEX idx_contratos_servico_id ON contratos(servico_id);
