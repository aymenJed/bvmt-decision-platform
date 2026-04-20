package com.bvmt.decision.service.indicator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MacdIndicatorTest {

    @Test
    void macd_producesAllSubComponents() {
        // Série suffisamment longue (26 + 9 = 35 mini, on prend 60)
        List<BigDecimal> closes = Stream.iterate(10.0, d -> d + (Math.sin(d) * 0.5 + 0.1))
                .limit(60)
                .map(BigDecimal::valueOf).toList();

        var res = new MacdIndicator(12, 26, 9).compute(closes);
        assertNotNull(res);
        assertNotNull(res.value(), "MACD line");
        assertTrue(res.meta().containsKey("signal"));
        assertTrue(res.meta().containsKey("histogram"));

        BigDecimal macd      = res.value();
        BigDecimal signal    = (BigDecimal) res.meta().get("signal");
        BigDecimal histogram = (BigDecimal) res.meta().get("histogram");
        assertEquals(macd.subtract(signal).doubleValue(),
                     histogram.doubleValue(), 1e-4,
                     "histogram = MACD - signal");
    }

    @Test
    void macd_insufficientDataReturnsNull() {
        List<BigDecimal> tooShort = Stream.of(1, 2, 3, 4, 5).map(BigDecimal::valueOf).toList();
        assertNull(new MacdIndicator().compute(tooShort));
    }

    @Test
    void macd_fastMustBeStrictlyLessThanSlow() {
        assertThrows(IllegalArgumentException.class, () -> new MacdIndicator(26, 12, 9));
        assertThrows(IllegalArgumentException.class, () -> new MacdIndicator(12, 12, 9));
    }
}
