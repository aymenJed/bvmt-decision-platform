package com.bvmt.decision.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "trading_signal",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"instrument_id", "signal_date", "rule_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "signal_date", nullable = false)
    private LocalDate signalDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, length = 10)
    private SignalType signalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Strength strength;

    @Column(name = "rule_code", nullable = false, length = 50)
    private String ruleCode;

    @Column(name = "triggering_value", precision = 18, scale = 6)
    private BigDecimal triggeringValue;

    @Column(name = "reference_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal referencePrice;

    @Column(columnDefinition = "text")
    private String rationale;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public enum SignalType { BUY, SELL, HOLD }
    public enum Strength   { WEAK, MEDIUM, STRONG }
}
