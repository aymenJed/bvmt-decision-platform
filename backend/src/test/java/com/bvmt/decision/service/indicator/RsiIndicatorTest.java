package com.bvmt.decision.service.indicator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests du RSI avec le jeu de données classique de Wilder (1978)
 * et vérifications des cas limites.
 */
class RsiIndicatorTest {

    @Test
    @DisplayName("RSI standard : 15 valeurs de Wilder, attendu ≈ 70.53")
    void rsi_wilder_reference() {
        // Série de référence extraite de l'ouvrage de Welles Wilder
        List<BigDecimal> closes = Stream.of(
                44.34, 44.09, 44.15, 43.61, 44.33, 44.83, 45.10, 45.42, 45.84,
                46.08, 45.89, 46.03, 45.61, 46.28, 46.28)
                .map(BigDecimal::valueOf).toList();

        var rsi = new RsiIndicator(14).compute(closes);
        assertNotNull(rsi);
        // Attendu ~70.53 — tolérance 0.5 pour les arrondis inter-implémentations
        assertEquals(70.53, rsi.value().doubleValue(), 0.5);
    }

    @Test
    @DisplayName("RSI = 100 si pas de pertes sur la période")
    void rsi_noLoss_returns100() {
        List<BigDecimal> strictlyIncreasing = Stream.iterate(10.0, d -> d + 0.5)
                .limit(20)
                .map(BigDecimal::valueOf).toList();
        var res = new RsiIndicator(14).compute(strictlyIncreasing);
        assertEquals(100.0, res.value().doubleValue(), 0.0001);
    }

    @Test
    @DisplayName("RSI null si historique insuffisant")
    void rsi_insufficientHistory_returnsNull() {
        List<BigDecimal> tooShort = Stream.of(10, 11, 12)
                .map(BigDecimal::valueOf).toList();
        assertNull(new RsiIndicator(14).compute(tooShort));
    }

    @Test
    @DisplayName("Période invalide → exception")
    void rsi_invalidPeriod_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RsiIndicator(1));
        assertThrows(IllegalArgumentException.class, () -> new RsiIndicator(0));
    }

    @Test
    @DisplayName("Code = RSI_<period>")
    void rsi_code_includesPeriod() {
        assertEquals("RSI_14", new RsiIndicator(14).code());
        assertEquals("RSI_7",  new RsiIndicator(7).code());
    }
}
