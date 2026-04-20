package com.bvmt.decision.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO unifié d'une cotation de clôture, quelle que soit la source
 * (Bulletin PDF, scraping Ilboursa, import Excel).
 *
 * Les sources sont responsables de peupler les champs qu'elles peuvent
 * extraire ; les champs manquants restent null (ex: open/high/low peuvent
 * ne pas être présents dans les sources "lite").
 */
@Builder
public record PriceQuoteDto(
        String     ticker,           // ex: "BH", "BT", "SFBT"
        String     isin,             // optionnel
        String     name,             // libellé court (utile pour resolver auto)
        LocalDate  tradeDate,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal referencePrice,
        Long       volume,
        BigDecimal turnover,
        Integer    nbTrades,
        BigDecimal variationPct,
        String     source            // BVMT_BULLETIN, ILBOURSA, EXCEL_IMPORT...
) {}
