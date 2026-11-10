package ru.gameplatform.dto;

import java.math.BigDecimal;

public record PlayRouletteRequest(BigDecimal betAmount, String betType, Integer number) {
}