package ru.pool.poolmanagement.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ClientRequest(
        @NotBlank(message = "Имя обязательно для заполнения")
        String name,

        @NotBlank(message = "Телефон обязателен для заполнения")
        @Pattern(
                regexp = "^(\\+7|8)[0-9]{10}$",
                message = "Номер телефона должен начинаться с +7 или 8 и содержать 11 цифр"
        )
        String phone,

        @NotBlank(message = "Email обязателен для заполнения")
        @Email(message = "Некорректный формат email")
        String email
) {
}