package com.bvmt.decision.service.etl;

import com.bvmt.decision.dto.PriceQuoteDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Source primaire : Bulletin Officiel PDF téléchargé quotidiennement depuis
 * https://www.bvmt.com.tn/editions-statistique
 *
 * STRATÉGIE :
 *   - URL pattern connu : bulletin PDF du jour J publié à ~18h
 *   - Téléchargement via RestClient
 *   - Parsing PDF : la logique de parsing est confiée à {@link BvmtPdfParser}
 *     (séparée pour pouvoir la tester unitairement sans réseau)
 *
 * NOTE IMPORTANTE : le format exact du bulletin et l'URL doivent être
 * validés avec l'équipe ops au lancement du projet. Ce code implémente
 * le pattern observé publiquement, mais la BVMT peut légitimement
 * demander une licence officielle via ses "Distributeurs des bases
 * de données de marché" pour un usage commercial.
 */
@Component
@Slf4j
public class BvmtBulletinSource implements MarketDataSource {

    private static final DateTimeFormatter URL_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String baseUrl;
    private final boolean enabled;
    private final BvmtPdfParser parser;
    private final RestClient restClient;

    public BvmtBulletinSource(
            @Value("${bvmt.etl.sources.bvmt-bulletin-url}") String baseUrl,
            @Value("${bvmt.etl.enabled:true}") boolean enabled,
            BvmtPdfParser parser,
            RestClient.Builder restClientBuilder) {
        this.baseUrl  = baseUrl;
        this.enabled  = enabled;
        this.parser   = parser;
        this.restClient = restClientBuilder
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "BVMT-Decision-Platform/0.1 (contact: ops@example.com)")
                .build();
    }

    @Override public String sourceName() { return "BVMT_BULLETIN"; }
    @Override public int    priority()   { return 1; }
    @Override public boolean isEnabled() { return enabled; }

    @Override
    public List<PriceQuoteDto> fetchQuotesForSession(LocalDate session)
            throws MarketDataException {
        if (!enabled) return List.of();

        // Pattern typique (à adapter selon la structure réelle fournie par la BVMT) :
        //   https://www.bvmt.com.tn/sites/default/files/bulletin/pdf/bullYYYYMMDD.pdf
        String pdfUrl = "https://www.bvmt.com.tn/sites/default/files/bulletin/pdf/bull"
                      + session.format(URL_DATE_FMT) + ".pdf";
        log.info("Téléchargement bulletin BVMT : {}", pdfUrl);

        try {
            byte[] pdfBytes = restClient.get()
                    .uri(URI.create(pdfUrl))
                    .accept(MediaType.APPLICATION_PDF)
                    .retrieve()
                    .body(byte[].class);

            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new MarketDataException(
                        "Bulletin PDF vide ou inaccessible pour " + session);
            }

            List<PriceQuoteDto> quotes = parser.parse(pdfBytes, session);
            log.info("Bulletin {} parsé : {} cotations extraites", session, quotes.size());
            return quotes;

        } catch (MarketDataException e) {
            throw e;
        } catch (Exception e) {
            throw new MarketDataException(
                    "Échec téléchargement/parsing bulletin BVMT du " + session, e);
        }
    }
}
