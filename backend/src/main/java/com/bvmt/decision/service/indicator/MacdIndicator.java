package com.bvmt.decision.service.indicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * MACD (Gerald Appel, 1979) — oscillateur basé sur deux EMAs.
 *
 *   MACD      = EMA(fast) − EMA(slow)         (défaut : 12 / 26)
 *   Signal    = EMA(signalPeriod) du MACD     (défaut : 9)
 *   Histogram = MACD − Signal
 *
 * Signaux courants :
 *   - MACD croise Signal vers le haut   → achat (bullish cross)
 *   - MACD croise Signal vers le bas    → vente (bearish cross)
 *   - Divergence prix/MACD              → affaiblissement de tendance
 */
public class MacdIndicator implements Indicator {

    private final int fastPeriod;
    private final int slowPeriod;
    private final int signalPeriod;

    public MacdIndicator() { this(12, 26, 9); }

    public MacdIndicator(int fast, int slow, int signal) {
        if (fast >= slow) throw new IllegalArgumentException("fast must be < slow");
        this.fastPeriod   = fast;
        this.slowPeriod   = slow;
        this.signalPeriod = signal;
    }

    @Override public String code() { return "MACD"; }

    @Override
    public int minRequiredBars() {
        // On a besoin du slowPeriod + une marge pour lisser la ligne signal
        return slowPeriod + signalPeriod;
    }

    @Override
    public IndicatorResult compute(List<BigDecimal> closes) {
        if (closes == null || closes.size() < minRequiredBars()) return null;

        // Calcul glissant des EMAs fast & slow sur TOUTE la série
        double[] emaFast = emaSeries(closes, fastPeriod);
        double[] emaSlow = emaSeries(closes, slowPeriod);

        // MACD line = EMA fast − EMA slow (aligné sur le slow)
        int n = closes.size();
        double[] macdLine = new double[n];
        for (int i = 0; i < n; i++) {
            if (Double.isNaN(emaFast[i]) || Double.isNaN(emaSlow[i])) {
                macdLine[i] = Double.NaN;
            } else {
                macdLine[i] = emaFast[i] - emaSlow[i];
            }
        }

        // Signal line = EMA signalPeriod du MACD (en sautant les NaN initiaux)
        double alpha = 2d / (signalPeriod + 1);
        double signal = Double.NaN;
        int startIdx = -1;
        for (int i = 0; i < n; i++) if (!Double.isNaN(macdLine[i])) { startIdx = i; break; }
        if (startIdx < 0 || n - startIdx < signalPeriod) return null;

        // Seed de la signal line = SMA des signalPeriod premières valeurs MACD valides
        double seed = 0;
        for (int i = startIdx; i < startIdx + signalPeriod; i++) seed += macdLine[i];
        seed /= signalPeriod;
        signal = seed;
        for (int i = startIdx + signalPeriod; i < n; i++) {
            signal = macdLine[i] * alpha + signal * (1 - alpha);
        }

        double macd      = macdLine[n - 1];
        double histogram = macd - signal;

        return new IndicatorResult(
                BigDecimal.valueOf(macd).setScale(6, RoundingMode.HALF_UP),
                Map.of(
                    "signal",    BigDecimal.valueOf(signal).setScale(6, RoundingMode.HALF_UP),
                    "histogram", BigDecimal.valueOf(histogram).setScale(6, RoundingMode.HALF_UP),
                    "fastPeriod",   fastPeriod,
                    "slowPeriod",   slowPeriod,
                    "signalPeriod", signalPeriod
                ),
                null);
    }

    /** Retourne toute la série d'EMA(period), avec NaN tant qu'on n'a pas assez de données. */
    private static double[] emaSeries(List<BigDecimal> closes, int period) {
        int n = closes.size();
        double[] out = new double[n];
        for (int i = 0; i < period - 1; i++) out[i] = Double.NaN;

        double seed = 0;
        for (int i = 0; i < period; i++) seed += closes.get(i).doubleValue();
        seed /= period;
        out[period - 1] = seed;

        double alpha = 2d / (period + 1);
        for (int i = period; i < n; i++) {
            out[i] = closes.get(i).doubleValue() * alpha + out[i - 1] * (1 - alpha);
        }
        return out;
    }
}
