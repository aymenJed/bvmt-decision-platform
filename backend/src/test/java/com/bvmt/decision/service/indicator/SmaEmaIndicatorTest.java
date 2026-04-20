package com.bvmt.decision.service.indicator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SmaEmaIndicatorTest {

    @Test
    void sma_classic() {
        List<BigDecimal> closes = Stream.of(10, 12, 14, 16, 18)
                .map(BigDecimal::valueOf).toList();
        var res = new SmaIndicator(5).compute(closes);
        assertNotNull(res);
        assertEquals(14.0, res.value().doubleValue(), 1e-6);
    }

    @Test
    void sma_insufficient() {
        assertNull(new SmaIndicator(10)
                .compute(Stream.of(1, 2, 3).map(BigDecimal::valueOf).toList()));
    }

    @Test
    void ema_seedMatchesSmaForFirstValue() {
        // Avec exactement `period` valeurs, l'EMA = SMA (pas encore de lissage)
        List<BigDecimal> closes = Stream.of(10, 12, 14, 16, 18)
                .map(BigDecimal::valueOf).toList();
        var res = new EmaIndicator(5).compute(closes);
        assertNotNull(res);
        assertEquals(14.0, res.value().doubleValue(), 1e-6);
    }

    @Test
    void ema_reactsToNewValues() {
        List<BigDecimal> base   = Stream.of(10, 10, 10, 10, 10).map(BigDecimal::valueOf).toList();
        List<BigDecimal> spiked = Stream.of(10, 10, 10, 10, 10, 20).map(BigDecimal::valueOf).toList();
        double emaBase   = new EmaIndicator(5).compute(base).value().doubleValue();
        double emaSpiked = new EmaIndicator(5).compute(spiked).value().doubleValue();
        assertTrue(emaSpiked > emaBase, "L'EMA doit monter quand le dernier cours monte");
    }
}
