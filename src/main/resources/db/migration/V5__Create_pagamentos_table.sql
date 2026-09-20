CREATE TABLE pagamentos (
                            id BIGSERIAL PRIMARY KEY,
                            valor_pago NUMERIC(10, 2) NOT NULL,
                            metodo_pagamento VARCHAR(50) NOT NULL,
                            data TIMESTAMP NOT NULL DEFAULT NOW(),
                            contrato_id BIGINT NOT NULL,
                            CONSTRAINT fk_pagamento_contrato FOREIGN KEY (contrato_id) REFERENCES contratos(id)
);