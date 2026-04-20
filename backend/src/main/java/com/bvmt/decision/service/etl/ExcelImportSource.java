package com.bvmt.decision.service.etl;

import com.bvmt.decision.dto.PriceQuoteDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Import manuel d'un fichier Excel (.xlsx) de cotations.
 *
 * Format attendu (colonnes) :
 *   A: Date (format date Excel ou texte ISO)
 *   B: Ticker
 *   C: Nom
 *   D: Ouverture
 *   E: Plus haut
 *   F: Plus bas
 *   G: Clôture
 *   H: Volume
 *   I: Variation %
 *
 * Déclenché via endpoint REST /api/etl/import/excel (POST multipart).
 * Utile pour le bootstrap (historique long) ou en cas d'indisponibilité
 * des sources automatisées.
 */
@Component
@Slf4j
public class ExcelImportSource {

    public List<PriceQuoteDto> importFromStream(InputStream excelStream)
            throws IOException {
        List<PriceQuoteDto> quotes = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(excelStream)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int lastRow = sheet.getLastRowNum();

            // On saute la ligne 0 (header)
            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                try {
                    LocalDate date = readDate(row.getCell(0), formatter);
                    String    ticker = formatter.formatCellValue(row.getCell(1)).trim();
                    if (ticker.isEmpty() || date == null) continue;

                    quotes.add(PriceQuoteDto.builder()
                            .tradeDate(date)
                            .ticker(ticker)
                            .name(formatter.formatCellValue(row.getCell(2)).trim())
                            .openPrice(readDecimal(row.getCell(3)))
                            .highPrice(readDecimal(row.getCell(4)))
                            .lowPrice(readDecimal(row.getCell(5)))
                            .closePrice(readDecimal(row.getCell(6)))
                            .volume(readLong(row.getCell(7)))
                            .variationPct(readDecimal(row.getCell(8)))
                            .source("EXCEL_IMPORT")
                            .build());
                } catch (Exception ex) {
                    log.warn("Ligne Excel {} ignorée : {}", r, ex.getMessage());
                }
            }
        }
        log.info("Import Excel : {} cotations chargées", quotes.size());
        return quotes;
    }

    private static LocalDate readDate(Cell cell, DataFormatter fmt) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = fmt.formatCellValue(cell).trim();
        if (text.isEmpty()) return null;
        // Accepte ISO yyyy-MM-dd ou dd/MM/yyyy
        if (text.matches("\\d{4}-\\d{2}-\\d{2}")) return LocalDate.parse(text);
        if (text.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            String[] p = text.split("/");
            return LocalDate.of(Integer.parseInt(p[2]),
                                Integer.parseInt(p[1]),
                                Integer.parseInt(p[0]));
        }
        return null;
    }

    private static BigDecimal readDecimal(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING  -> {
                String s = cell.getStringCellValue().replace(",", ".").trim();
                yield s.isEmpty() ? null : new BigDecimal(s);
            }
            default -> null;
        };
    }

    private static Long readLong(Cell cell) {
        if (cell == null) return 0L;
        if (cell.getCellType() == CellType.NUMERIC) return (long) cell.getNumericCellValue();
        String s = cell.getStringCellValue().replaceAll("[\\s.,]", "");
        return s.isEmpty() ? 0L : Long.parseLong(s);
    }
}
