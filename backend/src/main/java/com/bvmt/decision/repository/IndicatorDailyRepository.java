package com.bvmt.decision.repository;

import com.bvmt.decision.entity.IndicatorDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndicatorDailyRepository
        extends JpaRepository<IndicatorDaily, IndicatorDaily.IndicatorDailyId> {

    @Query("""
           SELECT i FROM IndicatorDaily i
           WHERE i.instrumentId = :instrumentId
             AND i.indicatorCode = :code
             AND i.tradeDate BETWEEN :from AND :to
           ORDER BY i.tradeDate ASC
           """)
    List<IndicatorDaily> findSeries(@Param("instrumentId") Long instrumentId,
                                    @Param("code") String indicatorCode,
                                    @Param("from") LocalDate from,
                                    @Param("to")   LocalDate to);

    Optional<IndicatorDaily> findByInstrumentIdAndTradeDateAndIndicatorCode(
            Long instrumentId, LocalDate tradeDate, String indicatorCode);
}
