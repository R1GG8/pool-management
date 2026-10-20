package ru.pool.poolmanagement.dto.reservation;

import ru.pool.poolmanagement.dto.client.ClientDetailsResponse;

import java.util.List;

public record OccupiedSlotResponse(
        String time,
        int count,
        List<ClientDetailsResponse> clients
) {
}