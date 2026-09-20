CREATE TABLE avaliacoes (
                            id BIGSERIAL PRIMARY KEY,
                            nota INT NOT NULL CHECK (nota BETWEEN 1 AND 5),
                            comentario TEXT,
                            contrato_id BIGINT NOT NULL,
                            CONSTRAINT fk_avaliacao_contrato FOREIGN KEY (contrato_id) REFERENCES contratos(id)
);