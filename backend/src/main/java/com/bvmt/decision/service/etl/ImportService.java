package com.bvmt.decision.service.etl;

import com.bvmt.decision.dto.PriceQuoteDto;
import com.bvmt.decision.entity.Instrument;
import com.bvmt.decision.entity.PriceDaily;
import com.bvmt.decision.repository.InstrumentRepository;
import com.bvmt.decision.repository.PriceDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Pipeline ETL complet :
 *   EXTRACT   : demande les cotations à la 1ère {@link MarketDataSource} qui réussit
 *   TRANSFORM : résout les tickers en entités Instrument (upsert si besoin)
 *   LOAD      : persiste en base (upsert sur clé composite)
 *
 * Utilisé à la fois par le scheduler (import quotidien automatique) et
 * par le contrôleur /api/etl/import (déclenchement manuel + Excel).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final List<MarketDataSource>   sources;
    private final PriceDailyRepository     priceRepo;
    private final InstrumentRepository     instrumentRepo;

    /**
     * Import automatique pour une date de séance donnée.
     * Essaie les sources par ordre de priorité ; s'arrête à la première qui
     * retourne au moins une cotation.
     *
     * @return résultat avec le nombre de lignes importées + source utilisée
     */
    public ImportResult importSession(LocalDate session) {
        log.info("=== Import séance {} ===", session);

        List<MarketDataSource> activeSources = sources.stream()
                .filter(MarketDataSource::isEnabled)
                .sorted(Comparator.comparingInt(MarketDataSource::priority))
                .toList();

        for (MarketDataSource src : activeSources) {
            try {
                List<PriceQuoteDto> quotes = src.fetchQuotesForSession(session);
                if (quotes.isEmpty()) {
                    log.info("Source {} : 0 cotation, essai suivant", src.sourceName());
                    continue;
                }
                int loaded = loadQuotes(quotes);
                return new ImportResult(session, src.sourceName(), quotes.size(), loaded);
            } catch (MarketDataSource.MarketDataException e) {
                log.warn("Source {} KO pour {} : {}", src.sourceName(), session, e.getMessage());
            }
        }
        log.error("Aucune source disponible n'a pu fournir les données du {}", session);
        return new ImportResult(session, "NONE", 0, 0);
    }

    /**
     * Charge un lot de cotations (utilisé par l'import auto ET l'import Excel).
     * Upsert sur clé composite (instrument_id, trade_date).
     */
    @Transactional
    public int loadQuotes(List<PriceQuoteDto> quotes) {
        int loaded = 0;
        for (PriceQuoteDto q : quotes) {
            Instrument instrument = resolveInstrument(q);
            if (instrument == null) {
                log.warn("Instrument introuvable & non créable : ticker={}", q.ticker());
                continue;
            }

            PriceDaily.PriceDailyId key =
                    new PriceDaily.PriceDailyId(instrument.getId(), q.tradeDate());

            PriceDaily row = priceRepo.findById(key)
                    .orElse(PriceDaily.builder()
                            .instrumentId(instrument.getId())
                            .tradeDate(q.tradeDate())
                            .build());

            row.setOpenPrice(q.openPrice());
            row.setHighPrice(q.highPrice());
            row.setLowPrice(q.lowPrice());
            row.setClosePrice(q.closePrice());
            row.setReferencePrice(q.referencePrice());
            row.setVolume(q.volume() == null ? 0L : q.volume());
            row.setTurnover(q.turnover() == null ? java.math.BigDecimal.ZERO : q.turnover());
            row.setNbTrades(q.nbTrades() == null ? 0 : q.nbTrades());
            row.setVariationPct(q.variationPct());
            row.setSource(q.source());
            row.setIngestedAt(Instant.now());

            priceRepo.save(row);
            loaded++;
        }
        log.info("Chargé {} lignes en base", loaded);
        return loaded;
    }

    /**
     * Résolution d'un ticker vers une entité Instrument.
     * Stratégie : lookup ISIN → lookup ticker → création auto en mode
     * "observation" (EQUITY par défaut, à enrichir ensuite via UI admin).
     */
    private Instrument resolveInstrument(PriceQuoteDto q) {
        if (q.isin() != null && !q.isin().isBlank()) {
            Optional<Instrument> byIsin = instrumentRepo.findByIsin(q.isin());
            if (byIsin.isPresent()) return byIsin.get();
        }
        Optional<Instrument> byTicker = instrumentRepo.findByTicker(q.ticker());
        if (byTicker.isPresent()) return byTicker.get();

        // Création automatique (EQUITY par défaut, l'admin pourra requalifier)
        if (q.isin() == null || q.isin().isBlank()) {
            // Pas d'ISIN → on ne peut pas créer une entrée propre
            return null;
        }
        return instrumentRepo.save(Instrument.builder()
                .isin(q.isin())
                .ticker(q.ticker())
                .name(q.name() == null ? q.ticker() : q.name())
                .instrumentType(Instrument.InstrumentType.EQUITY)
                .market("BVMT")
                .currency("TND")
                .active(true)
                .build());
    }

    public record ImportResult(LocalDate session, String source, int fetched, int loaded) {}
}
