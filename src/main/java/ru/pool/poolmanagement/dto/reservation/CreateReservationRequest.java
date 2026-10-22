package ru.pool.poolmanagement.dto.reservation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateReservationRequest(
        @NotNull(message = "clientId обязателен")
        Long clientId,

        @NotNull(message = "datetime обязателен")
        LocalDateTime datetime,

        @Min(value = 1, message = "Минимальная длительность записи - 1 час")
        @Max(value = 4, message = "Нельзя записаться более чем на 4 часа подряд")
        Integer durationHours
) {
}