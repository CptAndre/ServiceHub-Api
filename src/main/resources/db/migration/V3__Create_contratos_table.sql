CREATE TABLE contratos (
                           id BIGSERIAL PRIMARY KEY,
                           descricao_moveis TEXT NOT NULL,
                           valor_total NUMERIC(10, 2) NOT NULL,
                           status VARCHAR(20) NOT NULL DEFAULT 'AGENDADO',
                           cliente_id BIGINT NOT NULL,
                           montador_id BIGINT NOT NULL,
                           CONSTRAINT fk_contrato_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
                           CONSTRAINT fk_contrato_montador FOREIGN KEY (montador_id) REFERENCES montadores(id)
);