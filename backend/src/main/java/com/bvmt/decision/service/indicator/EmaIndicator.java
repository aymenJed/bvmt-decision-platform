package com.bvmt.decision.service.indicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Exponential Moving Average : plus réactive que la SMA, pondère davantage
 * les données récentes.
 *
 *   alpha = 2 / (period + 1)
 *   EMA(t) = prix(t) * alpha + EMA(t-1) * (1 - alpha)
 *
 * Initialisation : SMA sur les `period` premières valeurs puis récurrence.
 */
public class EmaIndicator implements Indicator {

    private final int period;

    public EmaIndicator(int period) {
        if (period < 1) throw new IllegalArgumentException("EMA period must be >= 1");
        this.period = period;
    }

    @Override public String code() { return "EMA_" + period; }

    @Override public int minRequiredBars() { return period; }

    @Override
    public IndicatorResult compute(List<BigDecimal> closes) {
        double ema = computeDouble(closes);
        if (Double.isNaN(ema)) return null;
        return IndicatorResult.of(
                BigDecimal.valueOf(ema).setScale(6, RoundingMode.HALF_UP));
    }

    /** Exposé en package-private pour réutilisation par MACD. */
    double computeDouble(List<BigDecimal> closes) {
        if (closes == null || closes.size() < period) return Double.NaN;

        double alpha = 2d / (period + 1);

        // Seed = SMA des `period` premières valeurs
        double seed = 0d;
        for (int i = 0; i < period; i++) seed += closes.get(i).doubleValue();
        seed /= period;

        double ema = seed;
        for (int i = period; i < closes.size(); i++) {
            ema = closes.get(i).doubleValue() * alpha + ema * (1 - alpha);
        }
        return ema;
    }

    public int getPeriod() { return period; }
}
