package com.git.juliherms.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import com.git.juliherms.TravelAgentAssistant;

/**
 * Representa um endpoint resta para expor o método via API e processar
 * o texto da pergunta
 */
@Path("/travel")
public class TravelAgentResource {

    @Inject
    TravelAgentAssistant assistant;

    /**
     * Método responsável por processar uma pergunta chamando o 
     * agente travel
     */
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ask(String question) {
        return assistant.chat(question);
    }
}