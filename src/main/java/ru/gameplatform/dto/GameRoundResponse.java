package ru.gameplatform.dto;

import java.math.BigDecimal;

public record GameRoundResponse(
        String game,
        String result,
        BigDecimal betAmount,
        BigDecimal winAmount,
        BigDecimal balance,
        String detail
) {
}