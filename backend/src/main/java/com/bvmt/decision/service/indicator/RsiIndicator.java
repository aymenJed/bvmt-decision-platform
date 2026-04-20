package com.bvmt.decision.service.indicator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Relative Strength Index (RSI) — J. Welles Wilder, 1978.
 *
 * Formule :
 *   RS  = Moyenne des gains / Moyenne des pertes (sur `period` jours)
 *   RSI = 100 − (100 / (1 + RS))
 *
 * On utilise le lissage de Wilder (SMMA) pour rester cohérent avec
 * les plateformes de trading (TradingView, MetaTrader) :
 *
 *   nouvelleMoyenne = ((ancienneMoyenne × (period − 1)) + valeurCourante) / period
 *
 * Période standard = 14. Interprétation courante :
 *   - RSI < 30 → survente  (signal d'achat potentiel)
 *   - RSI > 70 → surachat  (signal de vente potentiel)
 */
@Component
public class RsiIndicator implements Indicator {

    private static final int DEFAULT_PERIOD = 14;
    private final int period;

    public RsiIndicator() { this(DEFAULT_PERIOD); }

    public RsiIndicator(int period) {
        if (period < 2) throw new IllegalArgumentException("RSI period must be >= 2");
        this.period = period;
    }

    @Override public String code() { return "RSI_" + period; }

    @Override public int minRequiredBars() { return period + 1; }

    @Override
    public IndicatorResult compute(List<BigDecimal> closes) {
        if (closes == null || closes.size() < minRequiredBars()) return null;

        // 1) Initialisation : moyennes simples sur les `period` premières variations
        double avgGain = 0d, avgLoss = 0d;
        for (int i = 1; i <= period; i++) {
            double change = closes.get(i).doubleValue() - closes.get(i - 1).doubleValue();
            if (change > 0) avgGain += change;
            else            avgLoss -= change;   // valeur positive
        }
        avgGain /= period;
        avgLoss /= period;

        // 2) Lissage de Wilder sur le reste de la série
        for (int i = period + 1; i < closes.size(); i++) {
            double change = closes.get(i).doubleValue() - closes.get(i - 1).doubleValue();
            double gain   = change > 0 ?  change : 0d;
            double loss   = change < 0 ? -change : 0d;
            avgGain = ((avgGain * (period - 1)) + gain) / period;
            avgLoss = ((avgLoss * (period - 1)) + loss) / period;
        }

        // 3) Calcul final
        if (avgLoss == 0d) {
            // Pas de pertes sur la période → RSI = 100 (surachat extrême)
            return IndicatorResult.of(BigDecimal.valueOf(100));
        }
        double rs  = avgGain / avgLoss;
        double rsi = 100d - (100d / (1d + rs));

        return IndicatorResult.of(
                BigDecimal.valueOf(rsi).setScale(4, RoundingMode.HALF_UP));
    }

    public int getPeriod() { return period; }
}
