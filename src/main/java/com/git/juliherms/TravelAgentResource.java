package com.git.juliherms;

import com.git.juliherms.security.SecurityContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/travel")
public class TravelAgentResource {

    @Inject
    PackageExpert expert;

    /**
     * Endpoint para interagir com o agente de viagem.
     * Recebe uma pergunta do usuário e retorna a resposta do agente.
     * O nome do usuário é passado no cabeçalho "X-User-Name" para identificar a sessão de chat.
     *
     * @param question Pergunta do usuário.
     * @param userName Nome do usuário (passado no cabeçalho).
     * @return Resposta do agente de viagem ou mensagem de erro se o usuário não estiver autenticado.
     */
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ask(String question, @HeaderParam("X-User-Name") String userName) {
       if (userName != null && !userName.isEmpty()) {
            try {
                SecurityContext.setCurrentUser(userName);
                return expert.chat(userName, question); // Usar userName como memoryId
            } finally {
                SecurityContext.clear();
            }
        } else {
            return "Usuário precisa estar autenticado!";
        }
    }
}