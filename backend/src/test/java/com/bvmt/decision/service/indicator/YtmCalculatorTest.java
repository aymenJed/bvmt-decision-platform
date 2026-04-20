package com.bvmt.decision.service.indicator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class YtmCalculatorTest {

    @Test
    void ytm_convergesForParBond() {
        // Obligation au pair : YTM doit ≈ taux de coupon
        BigDecimal ytm = YtmCalculator.computeYtm(
                new BigDecimal("1000"),   // dirty price = face
                new BigDecimal("1000"),   // face value
                new BigDecimal("0.0750"), // coupon 7.5%
                1,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2031, 1, 1));
        assertNotNull(ytm);
        assertEquals(0.0750, ytm.doubleValue(), 0.0005);
    }

    @Test
    void ytm_higherWhenBondTradesAtDiscount() {
        // Obligation cotée sous le pair → YTM > coupon
        BigDecimal ytm = YtmCalculator.computeYtm(
                new BigDecimal("950"),
                new BigDecimal("1000"),
                new BigDecimal("0.0750"),
                1,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2031, 1, 1));
        assertNotNull(ytm);
        assertTrue(ytm.doubleValue() > 0.0750,
                   "YTM doit être > coupon pour une obligation en décote");
    }
}
