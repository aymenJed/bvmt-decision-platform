package com.bvmt.decision.dto;

import com.bvmt.decision.entity.PriceDaily;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceBarDto(
        LocalDate  date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long       volume,
        BigDecimal variationPct
) {
    public static PriceBarDto from(PriceDaily p) {
        return new PriceBarDto(
                p.getTradeDate(),
                p.getOpenPrice(),
                p.getHighPrice(),
                p.getLowPrice(),
                p.getClosePrice(),
                p.getVolume(),
                p.getVariationPct());
    }
}
