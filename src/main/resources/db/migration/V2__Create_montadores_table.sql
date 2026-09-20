CREATE TABLE montadores (
                            id BIGSERIAL PRIMARY KEY,
                            nome VARCHAR(150) NOT NULL,
                            cpf VARCHAR(14) NOT NULL UNIQUE,
                            especialidade VARCHAR(100),
                            telefone VARCHAR(20)
);