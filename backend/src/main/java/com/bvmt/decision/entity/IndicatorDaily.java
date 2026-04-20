package com.bvmt.decision.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Valeur d'un indicateur technique (RSI, SMA, MACD, YTM...) à une date.
 * Le champ `meta` permet de stocker des sous-composantes :
 *   - MACD : { "signal": 0.23, "histogram": 0.02 }
 *   - BOLL : { "upper": 10.2, "middle": 9.8, "lower": 9.4 }
 */
@Entity
@Table(name = "indicator_daily")
@IdClass(IndicatorDaily.IndicatorDailyId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicatorDaily {

    @Id
    @Column(name = "instrument_id")
    private Long instrumentId;

    @Id
    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Id
    @Column(name = "indicator_code", length = 20)
    private String indicatorCode;    // RSI_14, SMA_20, MACD, YTM, EMA_12, ...

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal value;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> meta = new HashMap<>();

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt = Instant.now();

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class IndicatorDailyId implements Serializable {
        private Long instrumentId;
        private LocalDate tradeDate;
        private String indicatorCode;
    }
}
