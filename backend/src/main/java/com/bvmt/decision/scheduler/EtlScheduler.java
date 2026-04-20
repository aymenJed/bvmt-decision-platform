package com.bvmt.decision.scheduler;

import com.bvmt.decision.entity.Instrument;
import com.bvmt.decision.repository.InstrumentRepository;
import com.bvmt.decision.service.IndicatorService;
import com.bvmt.decision.service.etl.ImportService;
import com.bvmt.decision.service.signal.SignalEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Orchestration temporelle de la plateforme :
 *
 *   Cron                   Action
 *   ───────────────────────────────────────────────────────────────
 *   0 0 18 * * MON-FRI     Import auto du bulletin BVMT du jour
 *                          → puis calcul indicateurs sur tous les instruments
 *                          → puis scan du moteur de règles
 *
 *   0 */5 10-14 * * MON-FRI  (Optionnel) Refresh intraday scraping
 *
 * Désactivable via `bvmt.etl.enabled=false`.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EtlScheduler {

    private static final ZoneId TUNIS_TZ = ZoneId.of("Africa/Tunis");

    private final ImportService       importService;
    private final IndicatorService    indicatorService;
    private final SignalEngine        signalEngine;
    private final InstrumentRepository instrumentRepo;

    @Value("${bvmt.etl.enabled:true}") private boolean enabled;

    /**
     * Import quotidien après clôture.
     * BVMT : clôture fixing à 14h05, bulletin disponible vers 17-18h.
     */
    @Scheduled(cron = "${bvmt.etl.daily-import-cron}", zone = "Africa/Tunis")
    public void dailyImport() {
        if (!enabled) return;

        LocalDate today = LocalDate.now(TUNIS_TZ);
        log.info(">>> Job ETL quotidien démarré pour {}", today);

        // 1) Import des cours
        var result = importService.importSession(today);
        log.info("Import : source={}, fetched={}, loaded={}",
                 result.source(), result.fetched(), result.loaded());
        if (result.loaded() == 0) {
            log.warn("Aucune donnée chargée — arrêt du pipeline pour {}", today);
            return;
        }

        // 2) Calcul des indicateurs sur tous les instruments actifs
        int indicCount = 0;
        for (Instrument inst : instrumentRepo.findAll()) {
            if (!inst.isActive()) continue;
            indicCount += indicatorService.computeAllFor(inst.getId(), today).size();
        }
        log.info("Indicateurs calculés : {} lignes", indicCount);

        // 3) Moteur de règles → signaux
        int signalCount = signalEngine.scanMarket(today);
        log.info("<<< Job ETL terminé pour {} ({} signaux générés)", today, signalCount);
    }

    /**
     * Rafraîchissement intraday (optionnel).
     * Toutes les 5 min entre 10h et 14h, pour alimenter price_tick.
     * À implémenter si une source intraday est disponible.
     */
    @Scheduled(cron = "${bvmt.etl.intraday-refresh-cron}", zone = "Africa/Tunis")
    public void intradayRefresh() {
        if (!enabled) return;
        // Stub : à implémenter quand une source de ticks intraday sera branchée
        log.debug("Intraday refresh tick (non implémenté)");
    }
}
