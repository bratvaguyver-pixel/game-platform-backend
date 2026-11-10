package ru.gameplatform.dto;

import java.math.BigDecimal;

public record TransactionHistoryItem(
        int id,
        BigDecimal amount,
        String type,
        String description,
        String createdAt
) {
}