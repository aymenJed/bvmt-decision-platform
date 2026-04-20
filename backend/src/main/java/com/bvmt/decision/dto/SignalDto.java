package com.bvmt.decision.dto;

import com.bvmt.decision.entity.TradingSignal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SignalDto(
        Long       id,
        Long       instrumentId,
        String     ticker,
        String     name,
        String     type,
        String     strength,
        String     ruleCode,
        BigDecimal triggeringValue,
        BigDecimal referencePrice,
        String     rationale,
        BigDecimal confidence,
        LocalDate  signalDate,
        Instant    createdAt
) {
    public static SignalDto from(TradingSignal s) {
        return new SignalDto(
                s.getId(),
                s.getInstrument().getId(),
                s.getInstrument().getTicker(),
                s.getInstrument().getName(),
                s.getSignalType().name(),
                s.getStrength().name(),
                s.getRuleCode(),
                s.getTriggeringValue(),
                s.getReferencePrice(),
                s.getRationale(),
                s.getConfidence(),
                s.getSignalDate(),
                s.getCreatedAt());
    }
}
