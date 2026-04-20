package com.bvmt.decision.service.signal;

import com.bvmt.decision.entity.Instrument;
import com.bvmt.decision.entity.PriceDaily;
import com.bvmt.decision.entity.TradingSignal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrat du moteur de règles.
 *
 * Une règle examine un instrument à une date donnée, en s'appuyant sur
 * l'historique de cours fourni, et retourne éventuellement un signal.
 *
 * Chaque règle est un {@code @Component} Spring ; elles sont toutes
 * injectées dans le {@link SignalEngine} via {@code List<TradingRule>}.
 */
public interface TradingRule {

    /** Code unique (ex: RSI_OVERSOLD, MACD_BULLISH_CROSS). */
    String code();

    /** Type de signal produit par cette règle. */
    TradingSignal.SignalType signalType();

    /**
     * Evalue la règle et retourne un signal si les conditions sont réunies.
     *
     * @param instrument  l'instrument concerné
     * @param asOf        date d'analyse (clôture)
     * @param priceSeries historique de cours ASC incluant asOf
     * @return Optional.empty() si la règle ne se déclenche pas
     */
    Optional<TradingSignal> evaluate(Instrument instrument,
                                     LocalDate asOf,
                                     List<PriceDaily> priceSeries);
}
