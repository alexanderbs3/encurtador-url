CREATE TABLE urls (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      hash_curto VARCHAR(10) NOT NULL UNIQUE,
                      url_original TEXT NOT NULL,
                      data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      data_expiracao TIMESTAMP NULL
);

-- Criamos um índice no hash para acelerar a busca durante o redirecionamento
CREATE INDEX idx_hash_curto ON urls(hash_curto);