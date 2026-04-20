package com.bvmt.decision.controller;

import com.bvmt.decision.dto.IndicatorDto;
import com.bvmt.decision.repository.IndicatorDailyRepository;
import com.bvmt.decision.service.IndicatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/indicators")
@RequiredArgsConstructor
@Tag(name = "Indicateurs", description = "Séries d'indicateurs techniques calculés")
public class IndicatorController {

    private final IndicatorDailyRepository repo;
    private final IndicatorService         service;

    @Operation(summary = "Série historique d'un indicateur (RSI_14, SMA_20, MACD...)")
    @GetMapping("/instrument/{id}")
    public List<IndicatorDto> series(
            @PathVariable Long id,
            @RequestParam String code,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        return repo.findSeries(id, code, from, effectiveTo).stream()
                .map(IndicatorDto::from)
                .toList();
    }

    @Operation(summary = "Recalcule tous les indicateurs d'un instrument à une date donnée")
    @PostMapping("/instrument/{id}/recompute")
    public List<IndicatorDto> recompute(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.computeAllFor(id, date).stream()
                .map(IndicatorDto::from)
                .toList();
    }
}
