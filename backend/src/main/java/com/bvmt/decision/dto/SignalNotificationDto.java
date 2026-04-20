package com.bvmt.decision.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Notification temps réel d'un signal de trading (poussée via WebSocket).
 */
public record SignalNotificationDto(
        Long       id,
        String     ticker,
        String     name,
        String     type,          // BUY / SELL / HOLD
        String     strength,      // WEAK / MEDIUM / STRONG
        String     ruleCode,
        BigDecimal price,
        String     rationale,
        BigDecimal confidence,
        LocalDate  signalDate,
        Instant    publishedAt
) {}
