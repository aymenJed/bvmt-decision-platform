package com.bvmt.decision.service;

import com.bvmt.decision.entity.IndicatorDaily;
import com.bvmt.decision.entity.Instrument;
import com.bvmt.decision.entity.PriceDaily;
import com.bvmt.decision.repository.IndicatorDailyRepository;
import com.bvmt.decision.repository.InstrumentRepository;
import com.bvmt.decision.repository.PriceDailyRepository;
import com.bvmt.decision.service.indicator.*;
import com.bvmt.decision.websocket.AlertPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service central qui :
 *   1. récupère l'historique de cours d'un instrument
 *   2. exécute les indicateurs techniques configurés
 *   3. persiste les résultats en cache (table indicator_daily)
 *   4. déclenche les alertes (seuils RSI) via WebSocket
 *
 * Point d'entrée principal : {@link #computeAllFor(Long, LocalDate)}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IndicatorService {

    private final PriceDailyRepository       priceRepo;
    private final IndicatorDailyRepository   indicatorRepo;
    private final InstrumentRepository       instrumentRepo;
    private final AlertPublisher             alertPublisher;

    // --- Configuration injectée depuis application.yml ---
    @Value("${bvmt.indicator.rsi-period:14}")    private int rsiPeriod;
    @Value("${bvmt.indicator.rsi-oversold:30}")  private int rsiOversold;
    @Value("${bvmt.indicator.rsi-overbought:70}") private int rsiOverbought;
    @Value("${bvmt.indicator.sma-short:20}")     private int smaShort;
    @Value("${bvmt.indicator.sma-long:50}")      private int smaLong;
    @Value("${bvmt.indicator.ema-short:12}")     private int emaShort;
    @Value("${bvmt.indicator.ema-long:26}")      private int emaLong;
    @Value("${bvmt.indicator.macd-signal:9}")    private int macdSignalPeriod;

    /**
     * Calcule RSI, SMA courte/longue, EMA courte/longue et MACD pour un instrument
     * à une date donnée. Persiste dans indicator_daily et publie une alerte
     * RSI si le seuil est franchi.
     *
     * Annoté @Transactional pour garantir l'atomicité (tous les indicateurs
     * d'une date sont écrits ou aucun).
     *
     * @return liste des indicateurs calculés (vide si données insuffisantes)
     */
    @Transactional
    public List<IndicatorDaily> computeAllFor(Long instrumentId, LocalDate asOf) {
        // 1) Récupérer l'historique nécessaire (on prend large : 250 barres)
        List<PriceDaily> history = priceRepo.findLastNBeforeAsc(instrumentId, asOf, 250);
        if (history.size() < rsiPeriod + 1) {
            log.debug("Historique insuffisant pour {} ({} barres)", instrumentId, history.size());
            return List.of();
        }
        List<BigDecimal> closes = history.stream().map(PriceDaily::getClosePrice).toList();

        List<IndicatorDaily> results = new ArrayList<>();

        // --- RSI ---
        Indicator rsi = new RsiIndicator(rsiPeriod);
        Indicator.IndicatorResult rsiResult = rsi.compute(closes);
        if (rsiResult != null) {
            IndicatorDaily rsiEntity = persist(instrumentId, asOf, rsi.code(), rsiResult);
            results.add(rsiEntity);
            // Alerte : croisement de seuil
            checkAndPublishRsiAlert(instrumentId, asOf, rsiResult.value(),
                                    closes.get(closes.size() - 1));
        }

        // --- SMA courte & longue ---
        addIfPresent(results, new SmaIndicator(smaShort),  closes, instrumentId, asOf);
        addIfPresent(results, new SmaIndicator(smaLong),   closes, instrumentId, asOf);

        // --- EMA courte & longue ---
        addIfPresent(results, new EmaIndicator(emaShort),  closes, instrumentId, asOf);
        addIfPresent(results, new EmaIndicator(emaLong),   closes, instrumentId, asOf);

        // --- MACD ---
        addIfPresent(results, new MacdIndicator(emaShort, emaLong, macdSignalPeriod),
                     closes, instrumentId, asOf);

        log.info("Indicateurs calculés pour instrument={} date={} → {} lignes",
                 instrumentId, asOf, results.size());
        return results;
    }

    private void addIfPresent(List<IndicatorDaily> results, Indicator indicator,
                              List<BigDecimal> closes, Long instrumentId, LocalDate asOf) {
        Indicator.IndicatorResult r = indicator.compute(closes);
        if (r != null) {
            results.add(persist(instrumentId, asOf, indicator.code(), r));
        }
    }

    private IndicatorDaily persist(Long instrumentId, LocalDate asOf,
                                   String code, Indicator.IndicatorResult r) {
        IndicatorDaily entity = IndicatorDaily.builder()
                .instrumentId(instrumentId)
                .tradeDate(asOf)
                .indicatorCode(code)
                .value(r.value())
                .meta(r.meta() == null ? new java.util.HashMap<>() : new java.util.HashMap<>(r.meta()))
                .build();
        return indicatorRepo.save(entity);
    }

    /** Publie une alerte temps réel si RSI franchit les seuils 30/70. */
    private void checkAndPublishRsiAlert(Long instrumentId, LocalDate date,
                                         BigDecimal rsi, BigDecimal closePrice) {
        Instrument instrument = instrumentRepo.findById(instrumentId).orElse(null);
        if (instrument == null) return;

        double value = rsi.doubleValue();
        String level = null;
        if (value <= rsiOversold)   level = "OVERSOLD";
        if (value >= rsiOverbought) level = "OVERBOUGHT";
        if (level == null) return;

        log.info("ALERTE RSI {} [{}] = {} @ {} (prix={})",
                 level, instrument.getTicker(), rsi, date, closePrice);

        alertPublisher.publishRsiAlert(
                instrument.getId(),
                instrument.getTicker(),
                instrument.getName(),
                level,
                rsi,
                closePrice,
                date);
    }
}
