package com.bvmt.decision.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Représente un instrument financier coté sur la BVMT (ou hors cote).
 *
 * Types supportés : actions, obligations (BTA/BTC), SICAV, FCP, indices.
 * Le champ `metadata` (JSONB) permet d'étendre le modèle sans migration :
 *   - secteur détaillé, site émetteur, rating, etc.
 */
@Entity
@Table(name = "instrument",
       indexes = {
           @Index(name = "idx_instrument_ticker", columnList = "ticker"),
           @Index(name = "idx_instrument_type",   columnList = "instrument_type")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String isin;

    @Column(nullable = false, unique = true, length = 20)
    private String ticker;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "instrument_type", nullable = false, length = 20)
    private InstrumentType instrumentType;

    private String sector;

    @Column(nullable = false, length = 50)
    private String market = "BVMT";

    @Column(nullable = false, length = 3)
    private String currency = "TND";

    @Column(name = "listing_date")
    private LocalDate listingDate;

    @Column(name = "nominal_value", precision = 15, scale = 4)
    private BigDecimal nominalValue;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Métadonnées libres (JSONB) : secteur détaillé, site web, rating... */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum InstrumentType {
        EQUITY, BOND, SICAV, FCP, INDEX
    }
}
