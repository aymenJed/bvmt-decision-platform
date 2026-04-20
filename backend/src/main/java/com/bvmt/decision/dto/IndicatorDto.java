package com.bvmt.decision.dto;

import com.bvmt.decision.entity.IndicatorDaily;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record IndicatorDto(
        LocalDate  date,
        String     code,
        BigDecimal value,
        Map<String, Object> meta
) {
    public static IndicatorDto from(IndicatorDaily i) {
        return new IndicatorDto(i.getTradeDate(), i.getIndicatorCode(),
                                i.getValue(), i.getMeta());
    }
}
