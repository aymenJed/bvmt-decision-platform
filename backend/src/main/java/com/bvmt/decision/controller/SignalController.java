package com.bvmt.decision.controller;

import com.bvmt.decision.dto.SignalDto;
import com.bvmt.decision.entity.TradingSignal;
import com.bvmt.decision.repository.TradingSignalRepository;
import com.bvmt.decision.service.signal.SignalEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/signals")
@RequiredArgsConstructor
@Tag(name = "Signaux", description = "Signaux générés par le moteur de règles")
public class SignalController {

    private final TradingSignalRepository signalRepo;
    private final SignalEngine            engine;

    @Operation(summary = "Signaux récents, paginés")
    @GetMapping
    public List<SignalDto> recent(@RequestParam(defaultValue = "30") int days,
                                  @RequestParam(defaultValue = "50") int limit) {
        LocalDate from = LocalDate.now().minusDays(days);
        return signalRepo.findRecent(from, PageRequest.of(0, limit))
                .getContent().stream()
                .map(SignalDto::from)
                .toList();
    }

    @Operation(summary = "Signaux d'un instrument à une date donnée")
    @GetMapping("/instrument/{id}")
    public List<SignalDto> byInstrument(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return signalRepo
                .findByInstrumentIdAndSignalDateOrderByCreatedAtDesc(id, date)
                .stream().map(SignalDto::from).toList();
    }

    @Operation(summary = "Déclenche un scan manuel pour une date (admin)")
    @PostMapping("/scan")
    public String manualScan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        int n = engine.scanMarket(date);
        return "Scan terminé : " + n + " signaux générés pour " + date;
    }
}
