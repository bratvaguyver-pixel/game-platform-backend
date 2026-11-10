package ru.gameplatform.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlayMinesRequest(BigDecimal betAmount, List<Integer> picks) {
}