package ru.pool.poolmanagement.dto.reservation;

public record AvailableSlotResponse(
        String time,
        int count
) {
}