ALTER TABLE montadores RENAME TO prestadores;

ALTER TABLE contratos
    RENAME COLUMN descricao_moveis TO descricao_servico;

ALTER TABLE contratos
    RENAME COLUMN montador_id TO prestador_id;

ALTER TABLE contratos
    DROP CONSTRAINT fk_contrato_montador;

ALTER TABLE contratos
    ADD CONSTRAINT fk_contrato_prestador
        FOREIGN KEY (prestador_id) REFERENCES prestadores(id);
