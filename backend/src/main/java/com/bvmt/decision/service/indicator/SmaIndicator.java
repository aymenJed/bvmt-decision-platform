package com.bvmt.decision.service.indicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Simple Moving Average : moyenne arithmétique des `period` dernières clôtures.
 * Pas annoté @Component : instancié dynamiquement avec des périodes différentes
 * (SMA20, SMA50, SMA200...) via la fabrique IndicatorFactory.
 */
public class SmaIndicator implements Indicator {

    private final int period;

    public SmaIndicator(int period) {
        if (period < 1) throw new IllegalArgumentException("SMA period must be >= 1");
        this.period = period;
    }

    @Override public String code() { return "SMA_" + period; }

    @Override public int minRequiredBars() { return period; }

    @Override
    public IndicatorResult compute(List<BigDecimal> closes) {
        if (closes == null || closes.size() < period) return null;

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = closes.size() - period; i < closes.size(); i++) {
            sum = sum.add(closes.get(i));
        }
        return IndicatorResult.of(
                sum.divide(BigDecimal.valueOf(period), 6, RoundingMode.HALF_UP));
    }

    public int getPeriod() { return period; }
}
