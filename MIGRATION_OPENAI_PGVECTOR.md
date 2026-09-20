# Migração: Ollama → OpenAI GPT + pgvector (RAG)

Este documento descreve as alterações realizadas para migrar o projeto da integração local com **Ollama** para o uso da **API da OpenAI** (GPT), com suporte a **RAG (Retrieval-Augmented Generation)** via **pgvector** no PostgreSQL.

---

## Visão Geral da Arquitetura

```
Cliente HTTP
    │
    ▼
TravelAgentResource  (POST /travel)
    │
    ▼
TravelAgentAssistant  (@RegisterAiService)
    ├──► OpenAI GPT-4o-mini          (geração de texto)
    └──► PgVectorEmbeddingStore      (recuperação RAG)
              ▲
              │ (ingestão de documentos)
         DocumentIngestor
              │
              ▼
         text-embedding-3-small      (modelo de embeddings)
```

---

## Modelos OpenAI Utilizados

| Finalidade         | Modelo                    | Dimensões |
|--------------------|---------------------------|-----------|
| Chat / Geração     | `gpt-4o-mini`             | —         |
| Embeddings / RAG   | `text-embedding-3-small`  | 1536      |

> **Por que `text-embedding-3-small`?**  
> Este modelo substitui o `text-embedding-ada-002` (legado). Oferece melhor desempenho em benchmarks de recuperação, suporte a *Matryoshka embeddings* (truncamento de dimensão) e custo reduzido, mantendo as mesmas 1536 dimensões.

---

## Dependências Adicionadas (`pom.xml`)

| Dependência removida                         | Substituída por                              |
|----------------------------------------------|----------------------------------------------|
| `quarkus-langchain4j-ollama`                 | `quarkus-langchain4j-openai`                 |
| —                                            | `quarkus-langchain4j-pgvector`               |
| —                                            | `quarkus-jdbc-postgresql`                    |

As versões são gerenciadas automaticamente pelo BOM `quarkus-langchain4j-bom` já presente no projeto.

---

## Configuração (`application.properties`)

```properties
# OpenAI - Chat
quarkus.langchain4j.openai.api-key=${OPENAI_API_KEY}
quarkus.langchain4j.openai.chat-model.model-name=gpt-4o-mini
quarkus.langchain4j.openai.timeout=60s

# OpenAI - Embeddings
quarkus.langchain4j.openai.embedding-model.model-name=text-embedding-3-small

# PostgreSQL (pgvector)
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=pguser
quarkus.datasource.password=pgpassword
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/ragdb

# pgvector store
quarkus.langchain4j.pgvector.dimension=1536
quarkus.langchain4j.pgvector.create-table=true
```

---

## Infraestrutura com Docker Compose

O arquivo `docker-compose.yml` sobe um PostgreSQL com a extensão **pgvector** já instalada:

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg17
    container_name: poc-pgvector
    environment:
      POSTGRES_DB: ragdb
      POSTGRES_USER: pguser
      POSTGRES_PASSWORD: pgpassword
    ports:
      - "5432:5432"
    volumes:
      - pgvector_data:/var/lib/postgresql/data

volumes:
  pgvector_data:
```

Iniciar o banco:

```bash
docker compose up -d
```

---

## Como Funciona o RAG neste Projeto

### 1. Ingestão de Documentos

Use `DocumentIngestor` para vetorizar e persistir documentos no pgvector:

```java
@Inject
DocumentIngestor ingestor;

// Exemplo: ingerir um documento de texto simples
Document doc = Document.from("Paris é a capital da França e famosa pela Torre Eiffel.");
ingestor.ingest(List.of(doc));
```

Internamente, o ingestor:
1. Divide o documento em chunks de ~500 tokens (com sobreposição de 50)
2. Envia cada chunk para a API da OpenAI (`text-embedding-3-small`) para gerar o vetor
3. Persiste o vetor e o texto na tabela `embeddings` do PostgreSQL

### 2. Recuperação e Geração

Ao chamar `POST /travel` com uma pergunta:

1. O `TravelAgentAssistant` vetoriza a pergunta com `text-embedding-3-small`
2. Busca no pgvector os chunks mais similares (busca por similaridade de cosseno)
3. Injeta os chunks recuperados no prompt como contexto
4. Envia o prompt enriquecido ao `gpt-4o-mini`
5. Retorna a resposta gerada ao cliente

---

## Pré-requisitos

- **Java 21+**
- **Docker e Docker Compose** instalados
- **Chave de API da OpenAI** — obtenha em [platform.openai.com/api-keys](https://platform.openai.com/api-keys)

---

## Como Executar

### 1. Configurar a chave da OpenAI

**Windows (PowerShell):**
```powershell
$env:OPENAI_API_KEY="sk-..."
```

**Linux / macOS:**
```bash
export OPENAI_API_KEY="sk-..."
```

### 2. Subir o banco de dados

```bash
docker compose up -d
```

### 3. Iniciar a aplicação em modo dev

```bash
./mvnw quarkus:dev
```

### 4. Testar o endpoint

```bash
curl -X POST http://localhost:8080/travel \
  -H "Content-Type: text/plain" \
  -d "Quais são os melhores destinos turísticos da Europa?"
```

---

## Observações Importantes

- **Variável de ambiente obrigatória:** a aplicação não iniciará sem `OPENAI_API_KEY` definida.
- **Tabela criada automaticamente:** `quarkus.langchain4j.pgvector.create-table=true` cria a tabela `embeddings` na primeira execução.
- **Reindexação necessária:** se trocar o modelo de embedding, todos os documentos precisam ser reingeridos, pois os vetores ocupam espaços diferentes.
- **Dev mode:** em modo dev (`quarkus:dev`), o Quarkus pode subir um PostgreSQL + pgvector automaticamente via Dev Services se não houver datasource configurado — neste caso, o `docker-compose.yml` garante o controle explícito da instância.
