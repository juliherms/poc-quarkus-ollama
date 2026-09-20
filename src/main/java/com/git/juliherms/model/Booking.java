package com.git.juliherms.model;

import com.git.juliherms.model.enums.BookingStatusEnum;

import java.time.LocalDate;

public record Booking(
        Long id,
        String customerName,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        BookingStatusEnum status
) {}
