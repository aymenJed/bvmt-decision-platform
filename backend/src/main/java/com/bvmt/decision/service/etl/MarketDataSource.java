package com.bvmt.decision.service.etl;

import com.bvmt.decision.dto.PriceQuoteDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Abstraction d'une source de données de marché.
 *
 * Plusieurs implémentations coexistent :
 *   - {@link BvmtBulletinSource}      → Bulletin Officiel PDF (source primaire)
 *   - {@link IlboursaScrapingSource}  → Scraping Ilboursa (repli, à n'activer
 *                                        que si CGU le permettent)
 *   - {@link ExcelImportSource}       → Import manuel de fichiers Excel BVMT
 *
 * Le scheduler ETL essaie les sources dans l'ordre de priorité ;
 * la première qui réussit est utilisée.
 */
public interface MarketDataSource {

    /** Identifiant court (pour logs et champ `source` en base). */
    String sourceName();

    /** Priorité d'exécution (plus bas = prioritaire). */
    int priority();

    /** Source active ? (ex: désactivée en config pour éviter le scraping). */
    boolean isEnabled();

    /**
     * Récupère toutes les cotations de clôture pour une date de séance.
     *
     * @param session date de la séance de cotation (ouvré BVMT)
     * @return liste des cotations, potentiellement vide
     * @throws MarketDataException si la source échoue (réseau, parsing...)
     */
    List<PriceQuoteDto> fetchQuotesForSession(LocalDate session) throws MarketDataException;

    class MarketDataException extends Exception {
        public MarketDataException(String message, Throwable cause) { super(message, cause); }
        public MarketDataException(String message) { super(message); }
    }
}
