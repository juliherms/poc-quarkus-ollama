package com.git.juliherms;

import io.quarkiverse.langchain4j.RegisterAiService;

// Esta anotação instrui o Quarkus a gerar uma implementação desta inerface se conectar ao ollama
@RegisterAiService
public interface TravelAgentAssistant {

    /**
     * O método 'chat' recebe a mensagem do usuário e retorna a resposta do LLM.
     * 
     */
    String chat(String userMessage);

}