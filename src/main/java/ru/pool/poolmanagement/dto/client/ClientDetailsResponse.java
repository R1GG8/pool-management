package ru.pool.poolmanagement.dto.client;

public record ClientDetailsResponse(
        Long id,
        String name,
        String phone,
        String email
) {
}