package com.bvmt.decision.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Cours journalier OHLCV d'un instrument, aligné sur la clôture officielle BVMT.
 *
 * Stocké dans une hypertable TimescaleDB (`price_daily`) partitionnée par mois.
 * La clé primaire composite (instrument_id, trade_date) est requise par Timescale
 * pour les hypertables.
 */
@Entity
@Table(name = "price_daily")
@IdClass(PriceDaily.PriceDailyId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceDaily {

    @Id
    @Column(name = "instrument_id")
    private Long instrumentId;

    @Id
    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", insertable = false, updatable = false)
    private Instrument instrument;

    @Column(name = "open_price", precision = 15, scale = 4)
    private BigDecimal openPrice;

    @Column(name = "high_price", precision = 15, scale = 4)
    private BigDecimal highPrice;

    @Column(name = "low_price", precision = 15, scale = 4)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal closePrice;

    @Column(name = "reference_price", precision = 15, scale = 4)
    private BigDecimal referencePrice;

    @Column(nullable = false)
    private long volume;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal turnover = BigDecimal.ZERO;

    @Column(name = "nb_trades")
    private int nbTrades;

    @Column(name = "variation_pct", precision = 7, scale = 4)
    private BigDecimal variationPct;

    @Column(length = 30, nullable = false)
    private String source = "BVMT_BULLETIN";

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt = Instant.now();

    // --------- Clé composite ---------
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class PriceDailyId implements Serializable {
        private Long instrumentId;
        private LocalDate tradeDate;
    }
}
