package com.bvmt.decision.websocket;

import com.bvmt.decision.entity.TradingSignal;
import com.bvmt.decision.dto.SignalNotificationDto;
import com.bvmt.decision.dto.RsiAlertDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Façade de publication WebSocket.
 *
 * Tous les composants (IndicatorService, SignalEngine, ETL...) passent
 * par cette façade plutôt que d'utiliser {@link SimpMessagingTemplate}
 * directement — garantit la cohérence du schéma des messages envoyés
 * et facilite les tests (on peut mocker un seul point).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Publie un nouveau signal de trading sur /topic/signals.
     * Tous les clients abonnés le reçoivent en direct.
     */
    public void publishSignal(TradingSignal signal) {
        SignalNotificationDto dto = new SignalNotificationDto(
                signal.getId(),
                signal.getInstrument().getTicker(),
                signal.getInstrument().getName(),
                signal.getSignalType().name(),
                signal.getStrength().name(),
                signal.getRuleCode(),
                signal.getReferencePrice(),
                signal.getRationale(),
                signal.getConfidence(),
                signal.getSignalDate(),
                Instant.now());
        messagingTemplate.convertAndSend("/topic/signals", dto);
        log.debug("Signal publié : {} {} @ {}", dto.type(), dto.ticker(), dto.price());
    }

    /**
     * Publie une alerte RSI (survente/surachat) sur /topic/alerts.
     */
    public void publishRsiAlert(Long instrumentId, String ticker, String name,
                                String level, BigDecimal rsi, BigDecimal closePrice,
                                LocalDate date) {
        RsiAlertDto dto = new RsiAlertDto(
                instrumentId, ticker, name, level, rsi, closePrice, date, Instant.now());
        messagingTemplate.convertAndSend("/topic/alerts", dto);
        log.debug("Alerte RSI publiée : {} {} = {}", level, ticker, rsi);
    }
}
