package ru.pool.poolmanagement.dto.reservation;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateReservationRequest(
        @NotNull(message = "clientId обязателен")
        Long clientId,

        @NotNull(message = "datetime обязателен")
        LocalDateTime datetime
) {
}