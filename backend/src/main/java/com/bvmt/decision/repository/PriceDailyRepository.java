package com.bvmt.decision.repository;

import com.bvmt.decision.entity.PriceDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceDailyRepository
        extends JpaRepository<PriceDaily, PriceDaily.PriceDailyId> {

    /**
     * Historique complet d'un instrument sur une fenêtre temporelle.
     * Utilisé pour le calcul des indicateurs glissants.
     */
    @Query("""
           SELECT p FROM PriceDaily p
           WHERE p.instrumentId = :instrumentId
             AND p.tradeDate BETWEEN :from AND :to
           ORDER BY p.tradeDate ASC
           """)
    List<PriceDaily> findHistory(@Param("instrumentId") Long instrumentId,
                                 @Param("from") LocalDate from,
                                 @Param("to")   LocalDate to);

    /**
     * N dernières clôtures (utile pour RSI, SMA, MACD).
     * Ordonné ASC pour un calcul séquentiel.
     */
    @Query(value = """
           SELECT * FROM (
             SELECT * FROM price_daily
             WHERE instrument_id = :instrumentId
               AND trade_date <= :asOf
             ORDER BY trade_date DESC
             LIMIT :n
           ) t ORDER BY trade_date ASC
           """, nativeQuery = true)
    List<PriceDaily> findLastNBeforeAsc(@Param("instrumentId") Long instrumentId,
                                        @Param("asOf") LocalDate asOf,
                                        @Param("n") int n);

    Optional<PriceDaily> findTopByInstrumentIdOrderByTradeDateDesc(Long instrumentId);

    @Query("SELECT MAX(p.tradeDate) FROM PriceDaily p WHERE p.instrumentId = :instrumentId")
    Optional<LocalDate> findLastTradeDate(@Param("instrumentId") Long instrumentId);

    /**
     * Dernière clôture de tous les instruments actifs à une date donnée.
     * Pour le scan batch du moteur de règles.
     */
    @Query("""
           SELECT p FROM PriceDaily p
           WHERE p.tradeDate = :date
           """)
    List<PriceDaily> findAllByTradeDate(@Param("date") LocalDate date);
}
