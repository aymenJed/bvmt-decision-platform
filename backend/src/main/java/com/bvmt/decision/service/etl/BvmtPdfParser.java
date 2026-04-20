package com.bvmt.decision.service.etl;

import com.bvmt.decision.dto.PriceQuoteDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser du Bulletin Officiel BVMT.
 *
 * Le bulletin est un PDF à structure tabulaire :
 *   | Code | Valeur | Ouverture | Plus-haut | Plus-bas | Clôture | Var% | Volume |
 *
 * STRATÉGIE D'EXTRACTION :
 *   - Conversion PDF → texte (PDFBox ou Apache POI pour PDF)
 *   - Regex ligne par ligne pour extraire les champs
 *   - Tolérance aux formats alternatifs (virgule/point décimal, espaces
 *     insécables, milliers formatés...)
 *
 * NOTE : on utilise ici Apache POI pour le texte (pas pour PDF). Pour une
 * vraie implémentation, ajouter la dépendance PDFBox :
 *   <groupId>org.apache.pdfbox</groupId><artifactId>pdfbox</artifactId>
 *
 * Ce parser est volontairement écrit en mode "défensif" : il log les
 * anomalies mais ne plante pas sur une ligne mal formatée.
 */
@Component
@Slf4j
public class BvmtPdfParser {

    // Regex approximative — à CALIBRER sur un vrai bulletin en phase d'intégration.
    // Format attendu : TICKER  VALEUR  OUV  HAUT  BAS  CLOTURE  VAR%  VOLUME
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^(?<ticker>[A-Z0-9]{2,6})\\s+"                                 // ticker
          + "(?<name>[A-Z .&'\\-]{2,40}?)\\s+"                              // libellé
          + "(?<open>[\\d,.]+)\\s+(?<high>[\\d,.]+)\\s+"                    // ouverture / haut
          + "(?<low>[\\d,.]+)\\s+(?<close>[\\d,.]+)\\s+"                    // bas / clôture
          + "(?<var>-?[\\d,.]+)\\s*%?\\s+"                                  // variation %
          + "(?<volume>[\\d\\s.]+)"                                         // volume
        );

    /**
     * Parse le PDF en entrée et retourne les cotations.
     *
     * IMPLÉMENTATION : pour garder ce squelette compilable sans ajouter
     * PDFBox en dépendance, on expose un hook {@link #extractText(byte[])}
     * que les tests peuvent mocker. En prod, remplacer l'implémentation par :
     *
     *   try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
     *       return new PDFTextStripper().getText(doc);
     *   }
     */
    public List<PriceQuoteDto> parse(byte[] pdfBytes, LocalDate session) {
        String text = extractText(pdfBytes);
        if (text == null || text.isBlank()) {
            log.warn("Texte PDF vide pour {}", session);
            return List.of();
        }

        List<PriceQuoteDto> quotes = new ArrayList<>();
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim().replace('\u00A0', ' '); // NBSP → espace
            if (line.isBlank()) continue;

            Matcher m = LINE_PATTERN.matcher(line);
            if (!m.find()) continue;

            try {
                quotes.add(PriceQuoteDto.builder()
                        .ticker(m.group("ticker"))
                        .name(m.group("name").trim())
                        .tradeDate(session)
                        .openPrice(parseNumber(m.group("open")))
                        .highPrice(parseNumber(m.group("high")))
                        .lowPrice(parseNumber(m.group("low")))
                        .closePrice(parseNumber(m.group("close")))
                        .variationPct(parseNumber(m.group("var")))
                        .volume(parseLong(m.group("volume")))
                        .source("BVMT_BULLETIN")
                        .build());
            } catch (Exception ex) {
                log.debug("Ligne ignorée (parsing KO) : {}", line);
            }
        }
        return quotes;
    }

    /**
     * Point d'extension : extraction texte du PDF.
     * Le code réel utilise PDFBox ; ce hook facilite les tests.
     */
    protected String extractText(byte[] pdfBytes) {
        // TODO : brancher PDFBox en prod
        // try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
        //     return new PDFTextStripper().getText(doc);
        // }
        return new String(pdfBytes);  // fallback dev (PDF textuel simulé)
    }

    /** Tolérant virgule ou point décimal + espaces milliers. */
    static BigDecimal parseNumber(String s) {
        if (s == null) return null;
        String normalized = s.replace(" ", "")
                             .replace("\u00A0", "")
                             .replace(",", ".");
        // Si plusieurs points : dernier = décimal, précédents = milliers
        int lastDot = normalized.lastIndexOf('.');
        if (normalized.indexOf('.') != lastDot) {
            normalized = normalized.substring(0, lastDot).replace(".", "")
                       + "." + normalized.substring(lastDot + 1);
        }
        return new BigDecimal(normalized);
    }

    static Long parseLong(String s) {
        if (s == null) return null;
        return Long.parseLong(s.replaceAll("[\\s\u00A0.]", ""));
    }
}
