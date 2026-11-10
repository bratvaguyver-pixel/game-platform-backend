package ru.gameplatform.dto;

import java.math.BigDecimal;

public record AuthResponse(String token, String username, String role, BigDecimal balance) {
}