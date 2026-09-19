# POC Ollama — Agente de Viagens com Quarkus + LangChain4j

> Apesar do nome do projeto (`poc-ollama`) e do `artifactId` (`code-with-quarkus`), a implementação atual **não usa Ollama**: o backend de LLM configurado é a **OpenAI** (`gpt-4o-mini` para chat e `text-embedding-3-small` para embeddings), integrada via extensões Quarkus LangChain4j.

## Visão geral

Este é um projeto **Quarkus** que expõe um **agente de viagens conversacional** (chatbot) via API REST, utilizando **LangChain4j** (através das extensões `quarkus-langchain4j`) para orquestrar chamadas a modelos de linguagem da OpenAI, com suporte a **RAG (Retrieval-Augmented Generation)** usando **PostgreSQL + pgvector** como banco vetorial.

Em resumo, o programa permite:

1. Receber uma pergunta em texto puro via HTTP (`POST /travel`).
2. Buscar automaticamente, no `pgvector`, os trechos de documentos previamente ingeridos que são semanticamente mais relevantes para a pergunta (recuperação/*retrieval*).
3. Enviar a pergunta + o contexto recuperado para o modelo `gpt-4o-mini` da OpenAI.
4. Retornar a resposta gerada, sempre em português do Brasil, no papel de um "agente de viagens especializado".

## Arquitetura e componentes

```
Cliente HTTP
     │  POST /travel  (texto puro)
     ▼
TravelAgentResource  (JAX-RS REST endpoint)
     │
     ▼
TravelAgentAssistant (interface @RegisterAiService — AI Service do LangChain4j)
     │                         │
     │ prompt (system+user)    │ retriever
     ▼                         ▼
  OpenAI Chat Model      PgVectorEmbeddingStore (PostgreSQL + pgvector)
  (gpt-4o-mini)                ▲
                                │ embeddings
                          DocumentIngestor
                          (text-embedding-3-small)
```

### [TravelAgentResource.java](../src/main/java/com/git/juliherms/TravelAgentResource.java)

Endpoint REST simples:

- `POST /travel`, consome `text/plain` e produz `text/plain`.
- Recebe a pergunta do usuário no corpo da requisição e delega diretamente para `TravelAgentAssistant.chat(question)`.

### [TravelAgentAssistant.java](../src/main/java/com/git/juliherms/TravelAgentAssistant.java)

É o coração da integração com LangChain4j:

- Anotada com `@RegisterAiService(retriever = PgVectorEmbeddingStore.class)`, o que faz o Quarkus LangChain4j gerar automaticamente uma implementação da interface em tempo de build, conectando o serviço de IA ao banco vetorial pgvector como fonte de recuperação de contexto (RAG).
- `@SystemMessage` define a persona do agente: um "agente de viagens especializado" que sempre responde em português do Brasil e usa o contexto recuperado (RAG) para enriquecer as respostas.
- `@UserMessage("{{userMessage}}")` injeta a pergunta do usuário no prompt.
- O método `chat(String userMessage)` é tudo que o restante da aplicação precisa chamar — toda a orquestração (montar prompt, buscar contexto no vetor store, chamar a API da OpenAI, parsear a resposta) é gerada pelo framework.

### [DocumentIngestor.java](../src/main/java/com/git/juliherms/DocumentIngestor.java)

Componente responsável por popular a base vetorial (ingestão para RAG):

- Injeta o `PgVectorEmbeddingStore` (armazenamento vetorial) e o `EmbeddingModel` (modelo de embeddings da OpenAI, `text-embedding-3-small`), ambos fornecidos automaticamente pelas extensões `quarkus-langchain4j-openai` e `quarkus-langchain4j-pgvector`.
- O método `ingest(List<Document> documents)` divide cada documento em *chunks* de até 500 tokens (sem sobreposição — `recursive(500, 50)`), gera os embeddings e grava tudo no pgvector através de um `EmbeddingStoreIngestor`.
- **Observação:** não há, no código atual, nenhum endpoint REST ou job que chame `DocumentIngestor.ingest(...)` — a ingestão precisa ser disparada manualmente (por exemplo, em testes, em um `@Startup`, ou via um endpoint auxiliar ainda não implementado).

### [GreetingResource.java](../src/main/java/com/git/juliherms/GreetingResource.java)

Endpoint de exemplo gerado pelo arquétipo padrão do Quarkus (`GET /hello`), sem relação com o agente de viagens. Serve como *health check*/exemplo básico e é coberto pelos testes [GreetingResourceTest.java](../src/test/java/com/git/juliherms/GreetingResourceTest.java) e [GreetingResourceIT.java](../src/test/java/com/git/juliherms/GreetingResourceIT.java).

## Configuração ([application.properties](../src/main/resources/application.properties))

| Propriedade | Valor | Descrição |
|---|---|---|
| `quarkus.langchain4j.openai.api-key` | `${OPENAI_API_KEY}` | Chave da API OpenAI, lida da variável de ambiente `OPENAI_API_KEY` |
| `quarkus.langchain4j.openai.chat-model.model-name` | `gpt-4o-mini` | Modelo usado para gerar as respostas do chat |
| `quarkus.langchain4j.openai.timeout` | `60s` | Timeout das chamadas à API da OpenAI |
| `quarkus.langchain4j.openai.embedding-model.model-name` | `text-embedding-3-small` | Modelo usado para gerar embeddings dos documentos (dimensão 1536) |
| `quarkus.datasource.db-kind` | `postgresql` | Banco de dados relacional usado como backend do pgvector |
| `quarkus.datasource.jdbc.url` | `jdbc:postgresql://localhost:5432/ragdb` | URL de conexão com o Postgres local |
| `quarkus.langchain4j.pgvector.dimension` | `1536` | Dimensão dos vetores, deve casar com o modelo de embedding |
| `quarkus.langchain4j.pgvector.create-table` | `true` | Cria a tabela do pgvector automaticamente na inicialização |
| `quarkus.langchain4j.pgvector.drop-table-first` | `false` | Não recria a tabela do zero a cada start |

## Dependências principais ([pom.xml](../pom.xml))

- `quarkus-rest` — endpoints REST (JAX-RS/RESTEasy Reactive).
- `quarkus-langchain4j-openai` — integração com a API da OpenAI (chat + embeddings) via LangChain4j.
- `quarkus-langchain4j-pgvector` — integração com PostgreSQL/pgvector como *embedding store*.
- `quarkus-jdbc-postgresql` — driver JDBC do PostgreSQL.
- Java 21, Quarkus 3.34.5.

## Pré-requisitos para rodar

1. **PostgreSQL com extensão `pgvector`** habilitada, rodando em `localhost:5432`, banco `ragdb`, usuário `pguser` / senha `pgpassword` (ou ajustar `application.properties`).
2. **Variável de ambiente `OPENAI_API_KEY`** com uma chave válida da OpenAI.
3. Java 21 e Maven (ou o wrapper `mvnw`/`mvnw.cmd` do projeto).

## Como executar

```bash
export OPENAI_API_KEY=sk-...
./mvnw quarkus:dev
```

Testar o agente de viagens:

```bash
curl -X POST http://localhost:8080/travel \
  -H "Content-Type: text/plain" \
  -d "Quais os melhores pontos turísticos em Lisboa?"
```

Testar o endpoint de exemplo:

```bash
curl http://localhost:8080/hello
```

## Limitações e pontos de atenção observados no código

- Não existe ainda um endpoint para acionar `DocumentIngestor`, então o RAG só retorna contexto se documentos forem ingeridos por algum outro meio (ex.: código de inicialização, teste manual ou classe adicional futura).
- Não há tratamento de erros explícito no `TravelAgentResource` (ex.: corpo vazio, falha na API da OpenAI, falha de conexão com o pgvector).
- Apesar do nome do projeto sugerir Ollama, a stack real de LLM é 100% OpenAI — para usar Ollama seria necessário trocar a extensão `quarkus-langchain4j-openai` por `quarkus-langchain4j-ollama` e ajustar as propriedades correspondentes.
