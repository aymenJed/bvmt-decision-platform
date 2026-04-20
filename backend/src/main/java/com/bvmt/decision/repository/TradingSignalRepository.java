package com.bvmt.decision.repository;

import com.bvmt.decision.entity.TradingSignal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TradingSignalRepository extends JpaRepository<TradingSignal, Long> {

    @Query("""
           SELECT s FROM TradingSignal s
           JOIN FETCH s.instrument
           WHERE s.signalDate >= :from
           ORDER BY s.signalDate DESC, s.createdAt DESC
           """)
    Page<TradingSignal> findRecent(@Param("from") LocalDate from, Pageable pageable);

    List<TradingSignal> findByInstrumentIdAndSignalDateOrderByCreatedAtDesc(
            Long instrumentId, LocalDate signalDate);

    @Query("""
           SELECT s FROM TradingSignal s
           JOIN FETCH s.instrument
           WHERE s.signalDate = :date
           ORDER BY s.strength DESC
           """)
    List<TradingSignal> findByDate(@Param("date") LocalDate date);

    boolean existsByInstrumentIdAndSignalDateAndRuleCode(
            Long instrumentId, LocalDate signalDate, String ruleCode);
}
