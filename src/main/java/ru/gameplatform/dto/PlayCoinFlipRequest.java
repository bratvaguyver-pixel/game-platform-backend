package ru.gameplatform.dto;

import java.math.BigDecimal;

public record PlayCoinFlipRequest(BigDecimal betAmount, String choice) {
}