package ru.pool.poolmanagement.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
        @NotBlank(message = "Имя обязательно для заполнения")
        String name,

        @NotBlank(message = "Телефон обязателен для заполнения")
        String phone,

        @NotBlank(message = "Email обязателен для заполнения")
        @Email(message = "Некорректный формат email")
        String email
) {
}