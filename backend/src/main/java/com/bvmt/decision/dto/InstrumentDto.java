package com.bvmt.decision.dto;

import com.bvmt.decision.entity.Instrument;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstrumentDto(
        Long       id,
        String     isin,
        String     ticker,
        String     name,
        String     instrumentType,
        String     sector,
        String     market,
        String     currency,
        LocalDate  listingDate,
        BigDecimal nominalValue,
        boolean    active
) {
    public static InstrumentDto from(Instrument i) {
        return new InstrumentDto(
                i.getId(), i.getIsin(), i.getTicker(), i.getName(),
                i.getInstrumentType().name(), i.getSector(), i.getMarket(),
                i.getCurrency(), i.getListingDate(), i.getNominalValue(),
                i.isActive());
    }
}
