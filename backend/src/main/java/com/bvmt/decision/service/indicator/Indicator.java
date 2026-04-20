package com.bvmt.decision.service.indicator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Contrat d'un indicateur technique.
 *
 * Toutes les implémentations doivent être :
 *   - sans état (stateless / thread-safe)
 *   - déterministes (mêmes inputs = mêmes outputs)
 *   - numériquement stables (privilégier BigDecimal pour le prix, double pour
 *     les sous-calculs internes où la précision arbitraire est superflue)
 */
public interface Indicator {

    /** Code unique de l'indicateur (ex: RSI_14, SMA_20, MACD). */
    String code();

    /**
     * Nombre minimum de points de prix nécessaires pour calculer la première
     * valeur fiable. Permet au service d'indicateurs de pré-charger le bon
     * historique et d'éviter les calculs sur données insuffisantes.
     */
    int minRequiredBars();

    /**
     * Calcule l'indicateur à partir d'une série ordonnée (ASC) de prix de clôture.
     *
     * @param closes prix de clôture triés par date croissante
     * @return valeur de l'indicateur à la dernière date, ou null si données insuffisantes
     */
    IndicatorResult compute(java.util.List<BigDecimal> closes);

    /** Résultat enrichi (valeur principale + sous-composantes + date effective). */
    record IndicatorResult(BigDecimal value, Map<String, Object> meta, LocalDate asOf) {
        public static IndicatorResult of(BigDecimal value) {
            return new IndicatorResult(value, Map.of(), null);
        }
        public static IndicatorResult of(BigDecimal value, Map<String, Object> meta) {
            return new IndicatorResult(value, meta, null);
        }
    }
}
