package com.git.juliherms.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Configuração para a memória de chat.
 * Produz um bean de ChatMemory para cada nova sessão de chat.
 */
@ApplicationScoped
public class ChatMemmoryConfig {

    // você também pode utilizar um RedisChatMemoryStore ou um JdbcChatMemoryStore para persistir a memória de chat em um banco de dados ou cache distribuído, se necessário.
    // Produz um bean de ChatMemory para cada nova sessão de chat.
    @Produces
    public ChatMemory chatMemory() {
        // O MessageWindowChatMemory mantém um número limitado de mensagens na memória, permitindo que o assistente virtual tenha contexto das últimas interações com o usuário.
        return MessageWindowChatMemory.builder()
                .maxMessages(20) // Mantém as últimas 20 mensagens na memória
                .chatMemoryStore(new InMemoryChatMemoryStore()) // Armazena a memória de chat em memória (não persistente)
                .build();
    }
}
