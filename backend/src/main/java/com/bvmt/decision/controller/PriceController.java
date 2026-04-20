package com.bvmt.decision.controller;

import com.bvmt.decision.dto.PriceBarDto;
import com.bvmt.decision.entity.PriceDaily;
import com.bvmt.decision.repository.InstrumentRepository;
import com.bvmt.decision.repository.PriceDailyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints de lecture des cours. Utilisés par le frontend pour alimenter
 * les charts (Chart.js / ngx-charts) et tracer les indicateurs superposés.
 */
@RestController
@RequestMapping("/prices")
@RequiredArgsConstructor
@Tag(name = "Cours", description = "Historique OHLCV et dernières clôtures")
public class PriceController {

    private final PriceDailyRepository priceRepo;
    private final InstrumentRepository instrumentRepo;

    @Operation(summary = "Historique OHLCV d'un instrument sur une fenêtre")
    @GetMapping("/instrument/{id}")
    public ResponseEntity<List<PriceBarDto>> history(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (instrumentRepo.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        List<PriceDaily> rows = priceRepo.findHistory(id, from, effectiveTo);
        return ResponseEntity.ok(rows.stream().map(PriceBarDto::from).toList());
    }

    @Operation(summary = "Dernière clôture connue d'un instrument")
    @GetMapping("/instrument/{id}/latest")
    public ResponseEntity<PriceBarDto> latest(@PathVariable Long id) {
        return priceRepo.findTopByInstrumentIdOrderByTradeDateDesc(id)
                .map(PriceBarDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
