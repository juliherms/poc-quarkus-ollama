package com.git.juliherms.service;

import com.git.juliherms.model.Booking;
import com.git.juliherms.model.enums.BookingStatusEnum;
import com.git.juliherms.security.SecurityContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.*;

/**
 * Class responsável por gerenciar as reservas de quartos de hotel.
 */
@ApplicationScoped
public class BookingService {

    private final Map<Long, Booking> bookings = new HashMap<>();

    /**
     * Inicializa as lista de reservas com alguns dados de exemplo.
     */
    public BookingService() {
        bookings.put(12345L, new Booking(12345L, "John Doe", "Tesouros do Egito",
                LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(2).plusDays(10), BookingStatusEnum.CONFIRMED));
        bookings.put(67890L, new Booking(67890L, "Jane Smith", "Aventura Amazônia",
                LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(3).plusDays(7), BookingStatusEnum.CONFIRMED));
    }

    /**
     * Recupera os detalhes de uma reserva com base no ID fornecido.
     * @param bookingId
     * @return
     */
    public Optional<Booking> getBookingDetails(long bookingId) {
       return Optional.ofNullable(bookings.get(bookingId));
    }

    /**
     * Cancela uma reserva existente.
     * Para confirmar o cancelamento, é necessário fornecer o ID da reserva (bookingId)
     * e o último nome do cliente (customerLastName).
     * @param bookingId O ID da reserva a ser cancelada.
     * @return Um Optional contendo a reserva cancelada, se o cancelamento for bem-sucedido; caso contrário, um Optional vazio.
     */
    public Optional<Booking> cancelBooking(long bookingId) {

        // Obter o usuário atual do contexto de segurança
        String currentUser = SecurityContext.getCurrentUser();

        if (bookings.containsKey(bookingId)) {
            Booking booking = bookings.get(bookingId);
            // Validando o usuário "logado", e não apenas o informado
            if (booking.customerName().equals(currentUser)) {
                Booking cancelledBooking = new Booking(booking.id(), booking.customerName(), booking.destination(),
                        booking.startDate(), booking.endDate(), BookingStatusEnum.CANCELLED);
                bookings.put(bookingId, cancelledBooking);
                return Optional.of(cancelledBooking);
            }
        }
        return Optional.empty();
    }

}
