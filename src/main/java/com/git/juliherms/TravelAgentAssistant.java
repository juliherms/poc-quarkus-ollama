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
@RegisterAiService(retriever = PgVectorEmbeddingStore.class)
public interface TravelAgentAssistant {

    @SystemMessage("""
            Você é um agente de viagens especializado.
            Responda sempre em português do Brasil, de forma clara e objetiva.
            Use o contexto recuperado para enriquecer suas respostas quando disponível.
            """)
    @UserMessage("{{userMessage}}")
    String chat(String userMessage);
}
