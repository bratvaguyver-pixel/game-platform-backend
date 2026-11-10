package ru.gameplatform.model;

import java.math.BigDecimal;

public record Game(
        int id,
        String name,
        String type,
        String description,
        BigDecimal minBet,
        BigDecimal maxBet,
        boolean isActive
) {
}