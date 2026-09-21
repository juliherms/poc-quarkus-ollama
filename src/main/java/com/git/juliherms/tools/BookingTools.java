package com.git.juliherms.tools;

import com.git.juliherms.model.Booking;
import com.git.juliherms.model.enums.CategoryEnum;
import com.git.juliherms.service.BookingService;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Classe de ferramentas para interagir com o serviço de reservas.
 */
@ApplicationScoped
public class BookingTools{

    @Inject
    BookingService bookingService;

    /**
     * Obtém os detalhes completos de uma reserva com base em seu número de identificação (bookingId).
     * @param bookingId O ID da reserva.
     * @return Uma string contendo os detalhes da reserva ou uma mensagem indicando que a reserva não foi encontrada.
     */
    @Tool("Obtém os detalhes completos de uma reserva com base em seu número de identificação (bookingId).")
    public String getBookingDetails(long bookingId) {
        return bookingService.getBookingDetails(bookingId)
                .map(Booking::toString)
                .orElse("Reserva com ID " + bookingId + " não encontrada.");
    }

    /**
     * Cancela uma reserva existente com base no seu ID (bookingId).
     * O usuário deve estar autenticado.
     * @param bookingId O ID da reserva a ser cancelada.
     * @return Uma mensagem indicando o sucesso ou falha do cancelamento.
     */
    @Tool("""
        Cancela uma reserva existente com base no seu ID (bookingId).
        O usuário deve estar autenticado.
    """)
    public String cancelBooking(long bookingId) {
        return bookingService.cancelBooking(bookingId)
                .map(b -> "Reserva " + b.id() + " cancelada com sucesso.")
                .orElse("Não foi possível cancelar a reserva. Verifique se o ID está correto e se você tem permissão.");
    }

    /**
     * Lista os pacotes de viagem disponíveis para uma determinada categoria (ex: ADVENTURE, TREASURES).
     * @param category A categoria dos pacotes de viagem a serem listados.
     * @return Uma string contendo os destinos dos pacotes encontrados ou uma mensagem indicando que nenhum pacote foi encontrado.
     */
    @Tool("Lista os pacotes de viagem disponíveis para uma determinada categoria (ex: ADVENTURE, TREASURES).")
    public String listPackagesByCategory(CategoryEnum category) {
        List<Booking> packages = bookingService.findPackagesByCategory(category);
        if (packages.isEmpty()) {
            return "Nenhum pacote encontrado para a categoria: " + category;
        }
        return "Pacotes encontrados para a categoria '" + category + "': " + packages.stream()
                .map(Booking::destination)
                .toList().toString();
    }
}
