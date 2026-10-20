package ru.pool.poolmanagement.dto.reservation;

import jakarta.validation.constraints.NotNull;

public record CancelReservationRequest(
        @NotNull(message = "clientId обязателен")
        Long clientId,
        String reason
) {
}