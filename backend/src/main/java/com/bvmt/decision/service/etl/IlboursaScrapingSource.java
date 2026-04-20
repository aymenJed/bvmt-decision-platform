package com.bvmt.decision.service.etl;

import com.bvmt.decision.dto.PriceQuoteDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Source de repli : scraping de la page A-Z d'Ilboursa
 * (https://www.ilboursa.com/marches/aaz).
 *
 * ⚠️ ATTENTION CGU :
 *   - À n'ACTIVER qu'après vérification des Conditions Générales d'Ilboursa
 *     et respect du fichier robots.txt.
 *   - Désactivée par défaut (`bvmt.etl.sources.ilboursa-enabled=false`).
 *   - Prévoir un User-Agent identifiable et un contact de courtoisie.
 *   - Respecter un délai entre requêtes (ici : 1 requête / session / jour).
 *
 * Cette classe sert de SOURCE SECONDAIRE : elle ne remplace pas le bulletin
 * officiel, mais peut combler un trou si le PDF est indisponible.
 */
@Component
@Slf4j
public class IlboursaScrapingSource implements MarketDataSource {

    private final String baseUrl;
    private final boolean enabled;

    public IlboursaScrapingSource(
            @Value("${bvmt.etl.sources.ilboursa-base-url}") String baseUrl,
            @Value("${bvmt.etl.sources.ilboursa-enabled:false}") boolean enabled) {
        this.baseUrl = baseUrl;
        this.enabled = enabled;
    }

    @Override public String sourceName() { return "ILBOURSA"; }
    @Override public int    priority()   { return 10; }   // secondaire
    @Override public boolean isEnabled() { return enabled; }

    @Override
    public List<PriceQuoteDto> fetchQuotesForSession(LocalDate session)
            throws MarketDataException {
        if (!enabled) return List.of();

        String url = baseUrl + "/marches/aaz";
        log.info("Scraping Ilboursa (session={}) : {}", session, url);

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("BVMT-Decision-Platform/0.1 (contact: ops@example.com)")
                    .timeout(15_000)
                    .get();

            // Structure cible : table des cotations (sélecteurs à AJUSTER selon
            // la vraie structure HTML d'Ilboursa au moment du déploiement).
            List<PriceQuoteDto> quotes = new ArrayList<>();
            for (Element row : doc.select("table.cotations tr[data-ticker]")) {
                try {
                    String ticker = row.attr("data-ticker").trim();
                    String name   = row.selectFirst("td.name").text();
                    BigDecimal close = parseDecimal(row.selectFirst("td.close").text());
                    BigDecimal variation = parseDecimal(
                            row.selectFirst("td.var").text().replace("%", ""));
                    Long volume = parseLong(row.selectFirst("td.volume").text());

                    quotes.add(PriceQuoteDto.builder()
                            .ticker(ticker)
                            .name(name)
                            .tradeDate(session)
                            .closePrice(close)
                            .variationPct(variation)
                            .volume(volume)
                            .source("ILBOURSA")
                            .build());
                } catch (Exception ignored) {
                    // Ligne malformée, on continue
                }
            }
            log.info("Ilboursa : {} cotations extraites", quotes.size());
            return quotes;

        } catch (IOException e) {
            throw new MarketDataException("Échec scraping Ilboursa", e);
        }
    }

    private static BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        return new BigDecimal(s.replace(" ", "")
                               .replace("\u00A0", "")
                               .replace(",", "."));
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) return 0L;
        return Long.parseLong(s.replaceAll("[\\s\u00A0.,]", ""));
    }
}
