package com.bvmt.decision.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Alerte RSI (seuils survente/surachat franchis).
 */
public record RsiAlertDto(
        Long       instrumentId,
        String     ticker,
        String     name,
        String     level,         // OVERSOLD / OVERBOUGHT
        BigDecimal rsiValue,
        BigDecimal closePrice,
        LocalDate  tradeDate,
        Instant    publishedAt
) {}
