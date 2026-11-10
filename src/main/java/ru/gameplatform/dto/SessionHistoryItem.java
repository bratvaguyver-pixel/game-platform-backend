package ru.gameplatform.dto;

import java.math.BigDecimal;

public record SessionHistoryItem(
        int id,
        String gameName,
        String gameType,
        BigDecimal betAmount,
        String result,
        BigDecimal winAmount,
        String createdAt
) {
}