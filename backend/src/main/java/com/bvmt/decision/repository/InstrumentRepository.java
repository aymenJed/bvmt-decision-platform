package com.bvmt.decision.repository;

import com.bvmt.decision.entity.Instrument;
import com.bvmt.decision.entity.Instrument.InstrumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    Optional<Instrument> findByTicker(String ticker);

    Optional<Instrument> findByIsin(String isin);

    List<Instrument> findByInstrumentTypeAndActiveTrue(InstrumentType type);

    @Query("""
           SELECT i FROM Instrument i
           WHERE i.active = true
             AND (LOWER(i.ticker) LIKE LOWER(CONCAT('%', :q, '%'))
                  OR LOWER(i.name) LIKE LOWER(CONCAT('%', :q, '%')))
           ORDER BY i.ticker
           """)
    List<Instrument> search(@Param("q") String query);
}
