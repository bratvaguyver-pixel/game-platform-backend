package ru.gameplatform.model;

import java.math.BigDecimal;

public record User(
        int id,
        String username,
        String email,
        String passwordHash,
        BigDecimal balance,
        String role,
        boolean active
) {
}