package com.bvmt.decision.service.signal;

import com.bvmt.decision.entity.Instrument;
import com.bvmt.decision.entity.PriceDaily;
import com.bvmt.decision.entity.TradingSignal;
import com.bvmt.decision.repository.InstrumentRepository;
import com.bvmt.decision.repository.PriceDailyRepository;
import com.bvmt.decision.repository.TradingSignalRepository;
import com.bvmt.decision.websocket.AlertPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Moteur de règles : exécute toutes les {@link TradingRule} sur un instrument
 * (ou sur tout le marché) à une date donnée.
 *
 * Les règles sont découvertes via injection Spring : ajouter une nouvelle
 * {@code @Component} implémentant {@link TradingRule} suffit pour qu'elle
 * soit prise en compte par le moteur — pas de registre à maintenir.
 *
 * Signaux dédoublonnés via la contrainte unique (instrument, date, rule_code).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignalEngine {

    private final List<TradingRule>         rules;
    private final PriceDailyRepository      priceRepo;
    private final InstrumentRepository      instrumentRepo;
    private final TradingSignalRepository   signalRepo;
    private final AlertPublisher            alertPublisher;

    /**
     * Évalue toutes les règles pour un instrument à une date donnée.
     * Persiste les signaux nouveaux et les publie via WebSocket.
     */
    @Transactional
    public List<TradingSignal> evaluateFor(Long instrumentId, LocalDate asOf) {
        Instrument instrument = instrumentRepo.findById(instrumentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Instrument introuvable: " + instrumentId));

        List<PriceDaily> history = priceRepo.findLastNBeforeAsc(instrumentId, asOf, 250);
        if (history.isEmpty() || !history.get(history.size() - 1).getTradeDate().equals(asOf)) {
            log.debug("Pas de cours pour {} à {}", instrument.getTicker(), asOf);
            return List.of();
        }

        List<TradingSignal> produced = new ArrayList<>();
        for (TradingRule rule : rules) {
            try {
                rule.evaluate(instrument, asOf, history).ifPresent(signal -> {
                    // Idempotence : on ne duplique pas si déjà présent
                    if (signalRepo.existsByInstrumentIdAndSignalDateAndRuleCode(
                            instrumentId, asOf, signal.getRuleCode())) {
                        return;
                    }
                    TradingSignal saved = signalRepo.save(signal);
                    produced.add(saved);
                    alertPublisher.publishSignal(saved);
                    log.info("Signal {} pour {} @ {} (règle={}, force={})",
                             saved.getSignalType(), instrument.getTicker(), asOf,
                             saved.getRuleCode(), saved.getStrength());
                });
            } catch (Exception e) {
                log.error("Erreur règle {} sur {} @ {}",
                          rule.code(), instrument.getTicker(), asOf, e);
            }
        }
        return produced;
    }

    /**
     * Scan complet du marché : évalue toutes les règles sur tous les instruments
     * actifs pour une date donnée. Exécuté par le scheduler après l'ETL.
     */
    @Transactional
    public int scanMarket(LocalDate asOf) {
        List<Instrument> active = instrumentRepo.findAll().stream()
                .filter(Instrument::isActive)
                .toList();
        int total = 0;
        for (Instrument inst : active) {
            total += evaluateFor(inst.getId(), asOf).size();
        }
        log.info("Scan marché {} : {} signaux générés sur {} instruments",
                 asOf, total, active.size());
        return total;
    }
}
