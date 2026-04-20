package com.bvmt.decision.controller;

import com.bvmt.decision.dto.InstrumentDto;
import com.bvmt.decision.entity.Instrument;
import com.bvmt.decision.repository.InstrumentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/instruments")
@RequiredArgsConstructor
@Tag(name = "Instruments", description = "Référentiel des titres cotés BVMT")
public class InstrumentController {

    private final InstrumentRepository repo;

    @Operation(summary = "Liste tous les instruments actifs")
    @GetMapping
    public List<InstrumentDto> list(@RequestParam(required = false) String type) {
        var all = repo.findAll();
        return all.stream()
                .filter(Instrument::isActive)
                .filter(i -> type == null || i.getInstrumentType().name().equalsIgnoreCase(type))
                .map(InstrumentDto::from)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Recherche plein texte par ticker ou nom")
    @GetMapping("/search")
    public List<InstrumentDto> search(@RequestParam String q) {
        return repo.search(q).stream().map(InstrumentDto::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstrumentDto> getOne(@PathVariable Long id) {
        return repo.findById(id)
                .map(InstrumentDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-ticker/{ticker}")
    public ResponseEntity<InstrumentDto> getByTicker(@PathVariable String ticker) {
        return repo.findByTicker(ticker)
                .map(InstrumentDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
