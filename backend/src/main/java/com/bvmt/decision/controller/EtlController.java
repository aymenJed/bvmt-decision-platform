package com.bvmt.decision.controller;

import com.bvmt.decision.service.etl.ExcelImportSource;
import com.bvmt.decision.service.etl.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Endpoints d'administration du pipeline ETL.
 * Réservés au rôle ROLE_ADMIN.
 */
@RestController
@RequestMapping("/etl")
@RequiredArgsConstructor
@Tag(name = "ETL", description = "Imports de cours (admin)")
@PreAuthorize("hasRole('ADMIN')")
public class EtlController {

    private final ImportService      importService;
    private final ExcelImportSource  excelSource;

    @Operation(summary = "Déclenche un import automatique pour une session donnée")
    @PostMapping("/import/auto")
    public ImportService.ImportResult importAuto(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate session) {
        return importService.importSession(session);
    }

    @Operation(summary = "Import manuel depuis fichier Excel (.xlsx)")
    @PostMapping(value = "/import/excel", consumes = "multipart/form-data")
    public ResponseEntity<String> importExcel(@RequestParam("file") MultipartFile file)
            throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Fichier vide");
        }
        var quotes = excelSource.importFromStream(file.getInputStream());
        int loaded = importService.loadQuotes(quotes);
        return ResponseEntity.ok("Importé : " + loaded + " / " + quotes.size());
    }
}
