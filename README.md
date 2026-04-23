# 🔗 URL Shortener

> Serviço de encurtamento de URLs de alta performance construído com **Spring Boot**, **MySQL** e **Redis**, seguindo boas práticas de engenharia de software com cache inteligente e rastreamento de cliques em tempo real.

---

## 📋 Índice

- [Sobre o Projeto](#sobre-o-projeto)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Funcionalidades](#funcionalidades)
- [Pré-requisitos](#pré-requisitos)
- [Como Executar](#como-executar)
- [Endpoints da API](#endpoints-da-api)
- [Estratégia de Cache](#estratégia-de-cache)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Banco de Dados](#banco-de-dados)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Melhorias Futuras](#melhorias-futuras)

---

## Sobre o Projeto

O **LeetJourney URL Shortener** é uma API RESTful que transforma URLs longas em links curtos e gerenciáveis. O sistema utiliza uma codificação **Base62** sobre um ID sequencial gerenciado pelo Redis para gerar hashes únicos e compactos, e combina persistência durável no MySQL com cache de baixíssima latência no Redis para suportar alto volume de redirecionamentos.

**Fluxo principal:**
1. O cliente envia uma URL longa via `POST /api/v1/urls`
2. O sistema gera um hash curto único (ex: `aB3xZ`) usando Base62
3. A URL é salva no MySQL e cacheada no Redis com TTL (Time-To-Live)
4. Ao acessar `GET /{hash}`, o sistema responde com um redirect `302 Found` para a URL original, priorizando o cache Redis

---

## Arquitetura

```
┌─────────────────────────────────────────────────────────┐
│                        Cliente                          │
└───────────────────────────┬─────────────────────────────┘
                            │ HTTP
                            ▼
┌─────────────────────────────────────────────────────────┐
│              Spring Boot Application                    │
│  ┌─────────────┐   ┌─────────────┐   ┌───────────────┐ │
│  │UrlController│──▶│  UrlService │──▶│  UrlRepository│ │
│  └─────────────┘   └──────┬──────┘   └───────┬───────┘ │
│                           │                  │         │
└───────────────────────────┼──────────────────┼─────────┘
                            │                  │
               ┌────────────▼────┐    ┌────────▼────────┐
               │   Redis Cache   │    │   MySQL (JPA)   │
               │  (Latência < 1ms│    │  (Persistência  │
               │   TTL por URL)  │    │   durável)      │
               └─────────────────┘    └─────────────────┘
```

**Padrão Cache-Aside (Lazy Loading):**
- **Cache HIT**: Redis retorna a URL original diretamente — sem tocar no banco
- **Cache MISS**: MySQL é consultado como fallback, a URL é repovoada no Redis para requisições futuras

---

## Tecnologias

| Tecnologia | Versão | Finalidade |
|---|---|---|
| Java | 17+ | Linguagem principal |
| Spring Boot | 3.x | Framework da aplicação |
| Spring Data JPA | — | Mapeamento objeto-relacional (ORM) |
| Spring Data Redis | — | Integração com Redis |
| MySQL | 8.0.44 | Persistência relacional |
| Redis | 7 (Alpine) | Cache e sequenciador de IDs |
| Flyway | — | Versionamento e migração do banco |
| Lombok | — | Redução de boilerplate |
| Bean Validation | — | Validação de entrada (JSR-380) |
| Docker & Compose | — | Containerização dos serviços |

---

## Funcionalidades

- ✅ **Encurtamento de URLs** com hash Base62 único
- ✅ **Redirecionamento** com HTTP `302 Found`
- ✅ **Expiração configurável** de URLs por data/hora
- ✅ **Cache inteligente** com Redis (Cache-Aside Pattern)
- ✅ **Contagem de cliques** em tempo real via Redis
- ✅ **Estatísticas por hash** (total de cliques, datas de criação e expiração)
- ✅ **Validação de entrada** com mensagens de erro descritivas
- ✅ **Tratamento global de exceções** com respostas JSON padronizadas
- ✅ **Migrações de banco versionadas** com Flyway

---

## Pré-requisitos

Antes de começar, certifique-se de ter instalado:

- [Docker](https://www.docker.com/) e [Docker Compose](https://docs.docker.com/compose/)
- [Java 17+](https://adoptium.net/)
- [Maven 3.8+](https://maven.apache.org/)

---

## Como Executar

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/encurtador.git
cd encurtador
```

### 2. Suba os serviços de infraestrutura

```bash
docker compose up -d
```

Isso irá inicializar:
- **MySQL** na porta `3306`
- **Redis** na porta `6379`

### 3. Execute a aplicação

```bash
./mvnw spring-boot:run
```

A aplicação estará disponível em: `http://localhost:8080`

> O Flyway criará automaticamente a tabela `urls` e seus índices na primeira execução.

---

## Endpoints da API

### `POST /api/v1/urls` — Encurtar URL

**Request Body:**
```json
{
  "urlOriginal": "https://www.exemplo.com/artigo/muito-longo",
  "dataExpiracao": "2025-12-31T23:59:59"
}
```

> `dataExpiracao` é opcional. Se omitido, a URL expira em 24 horas no cache (mas permanece no banco).

**Response `201 Created`:**
```json
{
  "urlOriginal": "https://www.exemplo.com/artigo/muito-longo",
  "urlEncurtada": "http://localhost:8080/aB3xZ",
  "dataExpiracao": "2025-12-31T23:59:59"
}
```

---

### `GET /{hash}` — Redirecionar

```
GET /aB3xZ
```

**Response `302 Found`:**
```
Location: https://www.exemplo.com/artigo/muito-longo
```

**Response `404 Not Found`** (hash inválido ou expirado):
```json
{
  "timestamp": "2025-04-19T10:30:00",
  "status": 404,
  "erro": "URL não encontrada"
}
```

---

### `GET /api/v1/urls/{hash}/estatisticas` — Estatísticas

```
GET /api/v1/urls/aB3xZ/estatisticas
```

**Response `200 OK`:**
```json
{
  "hash": "aB3xZ",
  "urlOriginal": "https://www.exemplo.com/artigo/muito-longo",
  "totalCliques": 142,
  "dataCriacao": "2025-04-01T09:00:00",
  "dataExpiracao": "2025-12-31T23:59:59"
}
```

---

## Estratégia de Cache

O sistema implementa o padrão **Cache-Aside** com Redis:

```
Requisição GET /{hash}
        │
        ▼
  Redis contém hash?
   ├── SIM  ──▶  Retorna URL + incrementa contador de cliques
   └── NÃO  ──▶  Consulta MySQL
                  ├── Não encontrado ──▶ 404 Not Found
                  ├── Expirado ──▶ 400 Bad Request
                  └── Encontrado ──▶ Salva no Redis (TTL: 24h)
                                      + incrementa contador
                                      + retorna URL
```

**Chaves Redis utilizadas:**

| Prefixo | Exemplo | Valor |
|---|---|---|
| `url:{hash}` | `url:aB3xZ` | URL original (String) |
| `url_clicks:{hash}` | `url_clicks:aB3xZ` | Contador de cliques (Integer) |
| `url_id_sequence` | — | Sequência global de IDs (Long) |

---

## Estrutura do Projeto

```
src/
└── main/
    ├── java/br/leetjourney/encurtador/
    │   ├── EncurtadorApplication.java    # Entry point
    │   ├── controller/
    │   │   └── UrlController.java        # Camada de apresentação (REST)
    │   ├── service/
    │   │   └── UrlService.java           # Regras de negócio e orquestração
    │   ├── domain/
    │   │   └── Url.java                  # Entidade JPA
    │   ├── repository/
    │   │   └── UrlRepository.java        # Acesso ao MySQL via Spring Data
    │   ├── dto/
    │   │   ├── request/
    │   │   │   └── UrlRequestDTO.java    # Payload de entrada
    │   │   └── response/
    │   │       ├── UrlResponseDTO.java   # Payload de criação
    │   │       └── UrlStatsResponseDTO.java # Payload de estatísticas
    │   ├── exception/
    │   │   └── GlobalExceptionHandler.java # Tratamento centralizado de erros
    │   └── util/
    │       └── Base62Util.java           # Algoritmo de codificação Base62
    └── resources/
        ├── application.yml               # Configurações da aplicação
        └── db/migration/
            └── V1__create_urls_table.sql # Migration inicial do Flyway
```

---

## Banco de Dados

### Schema

```sql
CREATE TABLE urls (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    hash_curto     VARCHAR(10)  NOT NULL UNIQUE,
    url_original   TEXT         NOT NULL,
    data_criacao   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    data_expiracao TIMESTAMP    NULL
);

-- Índice para otimizar buscas por hash durante o redirecionamento
CREATE INDEX idx_hash_curto ON urls(hash_curto);
```

### Algoritmo Base62

O hash é gerado convertendo um ID sequencial (gerado atomicamente pelo Redis com `INCR`) para Base62, usando o alfabeto:

```
abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789
```

Com 6 caracteres Base62, o sistema suporta até **56 bilhões de URLs únicas** sem colisões.

---

## Variáveis de Ambiente

As configurações padrão estão em `src/main/resources/application.yml`. Para sobrescrever em produção, use variáveis de ambiente:

| Variável | Padrão | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/encurtador_db` | URL de conexão MySQL |
| `SPRING_DATASOURCE_USERNAME` | `root` | Usuário do MySQL |
| `SPRING_DATASOURCE_PASSWORD` | `root` | Senha do MySQL |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host do Redis |
| `SPRING_DATA_REDIS_PORT` | `6379` | Porta do Redis |
| `SERVER_PORT` | `8080` | Porta da aplicação |

---

## Melhorias Futuras

- [ ] **Exceções customizadas** — Substituir `RuntimeException` genérica por hierarquia própria (`UrlNotFoundException`, `UrlExpiredException`)
- [ ] **Autenticação** — Proteger endpoints de criação com JWT (JSON Web Token)
- [ ] **Rate Limiting** — Limitar requisições por IP usando Bucket4j ou Redis
- [ ] **Testes automatizados** — Cobertura com JUnit 5, Mockito e Testcontainers
- [ ] **Paginação de estatísticas** — Histórico de acessos com timestamps
- [ ] **URLs customizadas** — Permitir que o usuário escolha o hash
- [ ] **Dashboard** — Interface web para gerenciar e visualizar estatísticas
- [ ] **Persistência de cliques no MySQL** — Job periódico para sincronizar contadores do Redis com o banco

---

## Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.