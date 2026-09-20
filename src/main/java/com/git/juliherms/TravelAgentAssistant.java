package com.git.juliherms;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.pgvector.PgVectorEmbeddingStore;

/**
 * AI Service que usa o modelo GPT da OpenAI como backend de chat.
 * O retriever conecta ao PgVectorEmbeddingStore para RAG:
 * antes de responder, o assistente busca no pgvector os trechos
 * de documentos mais relevantes e os injeta no contexto da pergunta.
 */
// Esta anotação instrui o Quarkus a gerar uma implementação desta interface que se conecta ao LLM configurado.
@RegisterAiService
public interface TravelAgentAssistant {

    /**
     * O método 'chat' recebe a mensagem do usuário e retorna a resposta do LLM.
     * @param userMessage A mensagem do usuário.
     * @return A resposta gerada pelo modelo de linguagem.
     */
    String chat(String userMessage);
}
